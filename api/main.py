"""Scalable Spotifusion music-search API.

Traffic controls are cache-first and per authenticated user. Repeated/hot
requests are served from memory/Redis; cold upstream requests are coalesced per
key and bounded by a small concurrency gate so a traffic spike does not fan
out into the same spike against YouTube Music.

No rate-limit setting can make upstream traffic literally zero for millions of
unique cold queries. It can, however, prevent duplicate upstream work and keep
upstream concurrency bounded while allowing generous customer request limits.
"""
from __future__ import annotations
import hashlib, logging, os, threading
from fastapi import Depends, FastAPI, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
import cache
from auth import require_firebase_user
from services import ytmusic

logging.basicConfig(level=logging.INFO)
logger=logging.getLogger("spotifusion.api")
RATE_LIMIT_PER_MINUTE=int(os.getenv("RATE_LIMIT_PER_MINUTE","100000"))
SEARCH_RATE_LIMIT=os.getenv("SEARCH_RATE_LIMIT",f"{RATE_LIMIT_PER_MINUTE}/minute")
SONG_RATE_LIMIT=os.getenv("SONG_RATE_LIMIT",f"{max(RATE_LIMIT_PER_MINUTE,1200)}/minute")
LYRICS_RATE_LIMIT=os.getenv("LYRICS_RATE_LIMIT","100000/minute")
UPSTREAM_CONCURRENCY=int(os.getenv("UPSTREAM_CONCURRENCY","16"))
ALLOWED_ORIGINS=[o.strip() for o in os.getenv("ALLOWED_ORIGINS","http://localhost:5173,http://127.0.0.1:5173,https://spotifusion.vercel.app").split(",") if o.strip()]

def _rate_key(request: Request)->str:
    authz=request.headers.get("authorization","")
    if authz.startswith("Bearer "):
        digest=hashlib.sha256(authz[7:].encode()).hexdigest()[:32]
        return f"user:{digest}"
    return f"anonymous:{request.client.host if request.client else 'unknown'}"

limiter=Limiter(key_func=_rate_key,default_limits=[f"{RATE_LIMIT_PER_MINUTE}/minute"])
app=FastAPI(title="Spotifusion Music API",version="2.0.0")
app.state.limiter=limiter
app.add_exception_handler(RateLimitExceeded,_rate_limit_exceeded_handler)
app.add_middleware(CORSMiddleware,allow_origins=ALLOWED_ORIGINS,allow_credentials=False,allow_methods=["GET","OPTIONS"],allow_headers=["*"])
_upstream_gate=threading.BoundedSemaphore(max(1,UPSTREAM_CONCURRENCY))
_inflight:dict[str,threading.Event]={}
_inflight_results:dict[str,tuple[object,bool,Exception|None]]={}
_inflight_guard=threading.Lock()
_MAX_INFLIGHT=8192

def _cached_upstream(key,loader,*,ttl=cache.CACHE_TTL):
    """Cache-first single-flight loader.

    Only the first request for a cold key reaches the upstream service.
    Concurrent callers wait for that result instead of creating duplicate
    upstream traffic. The semaphore additionally caps total upstream work.
    """
    cached=cache.get_json(key)
    if cached is not None:
        return cached,True

    with _inflight_guard:
        event=_inflight.get(key)
        owner=event is None
        if owner:
            if len(_inflight)>=_MAX_INFLIGHT:
                # Avoid unbounded memory growth under a malicious/high-cardinality burst.
                # This request becomes an owner without being registered.
                event=None
            else:
                event=threading.Event()
                _inflight[key]=event

    if not owner:
        event.wait(timeout=int(os.getenv("SINGLEFLIGHT_TIMEOUT","30")))
        cached=cache.get_json(key)
        if cached is not None:
            return cached,True
        with _inflight_guard:
            result=_inflight_results.pop(key,None)
        if result is not None:
            value,was_cached,error=result
            if error is not None:
                raise error
            return value,was_cached
        # Owner may have timed out/failed to publish; retry once as a new request.
        return _cached_upstream(key,loader,ttl=ttl)

    outcome=(None,False,None)
    try:
        with _upstream_gate:
            value=loader()
        cache.set_json(key,value,ttl=ttl)
        outcome=(value,False,None)
        return value,False
    except Exception as exc:
        outcome=(None,False,exc)
        raise
    finally:
        if event is not None:
            with _inflight_guard:
                _inflight_results[key]=outcome
                _inflight.pop(key,None)
                event.set()
                if len(_inflight_results)>_MAX_INFLIGHT:
                    _inflight_results.pop(next(iter(_inflight_results)),None)

@app.get("/")
def root(): return {"service":"spotifusion-music-api","status":"ok","version":"2.0.0","docs":"/docs"}

@app.get("/health")
def health(): return {"status":"ok","cache":"enabled" if cache._get_client() else "local-only"}

@app.get("/api/search")
@limiter.limit(SEARCH_RATE_LIMIT)
def search(
    request: Request,
    q: str = Query(..., min_length=2, max_length=120),
    category: str = Query("all", pattern="^(all|songs|albums|artists|playlists|jukebox)$"),
    batch: int = Query(1, ge=1, le=100000),
    _user=Depends(require_firebase_user),
):
    """Category-aware batched search.

    The UI asks for batch 1, then batch 2, etc. We intentionally do not put a
    20/100 UI cap on the results. YouTube Music/ytmusicapi decides how many
    continuations are actually available. The server deduplicates each batch
    and tells the client whether new items remain.
    """
    query = " ".join(q.strip().split())
    if len(query) < 2:
        raise HTTPException(400, "Search query is too short.")

    page_size = max(20, min(int(os.getenv("SEARCH_PAGE_SIZE", "100")), 100))
    category = category.lower()
    requested = min(batch * page_size, int(os.getenv("SEARCH_PROVIDER_MAX", "10000")))
    key = cache.normalize_key(f"search:v4:{category}:{requested}", query)
    try:
        all_results, cached = _cached_upstream(
            key,
            lambda: ytmusic.search_songs(query, limit=requested, category=category),
            ttl=int(os.getenv("SEARCH_CACHE_TTL", str(6 * 3600))),
        )
    except Exception as exc:
        logger.error("Search failed for query=%r category=%s batch=%s: %s", query, category, batch, exc)
        raise HTTPException(502, "Unable to search right now. Please try again.") from exc

    all_results = all_results if isinstance(all_results, list) else []
    start_index = (batch - 1) * page_size
    results = all_results[start_index:start_index + page_size]
    has_more = len(all_results) > start_index + len(results)
    # If the provider returned a full batch, a continuation may exist even if
    # the current implementation cannot expose it until the next request.
    # The next request is therefore allowed; duplicate results are filtered by
    # the frontend and has_more becomes false when no new items arrive.
    if len(results) == page_size and batch < 100000:
        has_more = True

    return {
        "query": query,
        "category": category,
        "batch": batch,
        "pageSize": page_size,
        "results": results,
        "hasMore": has_more,
        "available": len(all_results),
        "cached": cached,
    }


@app.get("/api/artist/{artist_id}")
@limiter.limit(SONG_RATE_LIMIT)
def artist(request: Request, artist_id: str, _user=Depends(require_firebase_user)):
    if not artist_id or len(artist_id) > 128:
        raise HTTPException(400, "Invalid artist id.")
    key = cache.normalize_key("artist:v1", artist_id)
    try:
        payload, _ = _cached_upstream(
            key,
            lambda: ytmusic.get_artist(artist_id),
            ttl=int(os.getenv("ENTITY_CACHE_TTL", str(6 * 3600))),
        )
    except Exception as exc:
        logger.error("Artist lookup failed for id=%r: %s", artist_id, exc)
        raise HTTPException(502, "Unable to fetch this artist right now. Please try again.") from exc
    return payload

@app.get("/api/album/{album_id}")
@limiter.limit(SONG_RATE_LIMIT)
def album(request: Request, album_id: str, _user=Depends(require_firebase_user)):
    if not album_id or len(album_id) > 128:
        raise HTTPException(400, "Invalid album id.")
    key = cache.normalize_key("album:v1", album_id)
    try:
        payload, _ = _cached_upstream(
            key,
            lambda: ytmusic.get_album(album_id),
            ttl=int(os.getenv("ENTITY_CACHE_TTL", str(6 * 3600))),
        )
    except Exception as exc:
        logger.error("Album lookup failed for id=%r: %s", album_id, exc)
        raise HTTPException(502, "Unable to fetch this album right now. Please try again.") from exc
    return payload

@app.get("/api/song/{video_id}")
@limiter.limit(SONG_RATE_LIMIT)
def song(request:Request,video_id:str,_user=Depends(require_firebase_user)):
    if not video_id or len(video_id)>32: raise HTTPException(400,"Invalid video id.")
    key=cache.normalize_key("song:v2",video_id)
    try:
        payload,_=_cached_upstream(key,lambda:{"found":(track:=ytmusic.get_song(video_id)) is not None,"track":track},ttl=int(os.getenv("SONG_CACHE_TTL",str(24*3600))))
    except Exception as exc:
        logger.error("Song lookup failed for id=%r: %s",video_id,exc)
        raise HTTPException(502,"Unable to fetch this track right now. Please try again.") from exc
    if not payload.get("found"): raise HTTPException(404,"Track not found.")
    return payload["track"]

@app.get("/api/lyrics/{video_id}")
@limiter.limit(LYRICS_RATE_LIMIT)
def lyrics(request:Request,video_id:str,_user=Depends(require_firebase_user)):
    if not video_id or len(video_id)>32: raise HTTPException(400,"Invalid video id.")
    key=cache.normalize_key("lyrics:v2",video_id)
    try:
        result,_=_cached_upstream(key,lambda:ytmusic.get_lyrics(video_id),ttl=int(os.getenv("LYRICS_CACHE_TTL",str(24*3600))))
    except Exception as exc:
        logger.error("Lyrics lookup failed for id=%r: %s",video_id,exc)
        raise HTTPException(502,"Unable to fetch lyrics right now. Please try again.") from exc
    return result

@app.get("/api/discover")
@limiter.limit(os.getenv("DISCOVER_RATE_LIMIT", "120/minute"))
def discover(request: Request, _user=Depends(require_firebase_user)):
    key = "discover:v1:IN"
    try:
        tracks, cached = _cached_upstream(
            key,
            lambda: ytmusic.get_discover("IN"),
            ttl=int(os.getenv("DISCOVER_CACHE_TTL", str(3 * 3600))),
        )
    except Exception as exc:
        logger.error("Discovery failed: %s", exc)
        raise HTTPException(502, "Discovery is temporarily unavailable.") from exc

    trending = tracks.get("trending", []) if isinstance(tracks, dict) else []
    fresh = tracks.get("fresh", []) if isinstance(tracks, dict) else []
    sections = []
    if trending:
        sections.append({"id": "india-trending", "label": "Trending", "title": "Trending in India", "subtitle": "Popular right now", "tracks": trending})
        sections.append({"id": "top-picks", "label": "Popular", "title": "Top picks", "subtitle": "Popular music to explore", "tracks": trending[:12]})
    if fresh:
        sections.append({"id": "new-releases", "label": "Fresh music", "title": "New and recently surfaced", "subtitle": "Fresh tracks from YouTube Music", "tracks": fresh})
    if trending:
        sections.append({"id": "discover", "label": "Discover", "title": "Discover something new", "subtitle": "A mix of popular music and fresh picks", "tracks": (fresh[:12] + trending[6:18])[:24]})
    return {"sections": sections, "cached": cached}

@app.exception_handler(Exception)
def unhandled_exception_handler(request:Request,exc:Exception):
    logger.exception("Unhandled error on %s",request.url.path)
    return JSONResponse(status_code=500,content={"detail":"Internal server error."})
