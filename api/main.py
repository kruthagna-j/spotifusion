"""Scalable Spotifusion music-search API."""
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
from yt_dlp_fallback import extract_audio

logging.basicConfig(level=logging.INFO)
logger=logging.getLogger("spotifusion.api")
RATE_LIMIT_PER_MINUTE=int(os.getenv("RATE_LIMIT_PER_MINUTE","100000"))
SEARCH_RATE_LIMIT=os.getenv("SEARCH_RATE_LIMIT",f"{RATE_LIMIT_PER_MINUTE}/minute")
SONG_RATE_LIMIT=os.getenv("SONG_RATE_LIMIT",f"{max(RATE_LIMIT_PER_MINUTE,1200)}/minute")
LYRICS_RATE_LIMIT=os.getenv("LYRICS_RATE_LIMIT","100000/minute")
STREAM_RATE_LIMIT=os.getenv("STREAM_RATE_LIMIT","300/minute")
UPSTREAM_CONCURRENCY=int(os.getenv("UPSTREAM_CONCURRENCY","16"))
ALLOWED_ORIGINS=[o.strip() for o in os.getenv("ALLOWED_ORIGINS","http://localhost:5173,http://127.0.0.1:5173,https://spotifusion.vercel.app").split(",") if o.strip()]

def _rate_key(request: Request)->str:
    authz=request.headers.get("authorization","")
    if authz.startswith("Bearer "):
        digest=hashlib.sha256(authz[7:].encode()).hexdigest()[:32]
        return f"user:{digest}"
    return f"anonymous:{request.client.host if request.client else 'unknown'}"

limiter=Limiter(key_func=_rate_key,default_limits=[f"{RATE_LIMIT_PER_MINUTE}/minute"])
app=FastAPI(title="Spotifusion Music API",version="2.1.0")
app.state.limiter=limiter
app.add_exception_handler(RateLimitExceeded,_rate_limit_exceeded_handler)
app.add_middleware(CORSMiddleware,allow_origins=ALLOWED_ORIGINS,allow_credentials=False,allow_methods=["GET","OPTIONS"],allow_headers=["*"])
_upstream_gate=threading.BoundedSemaphore(max(1,UPSTREAM_CONCURRENCY))
_inflight:dict[str,threading.Event]={}
_inflight_results:dict[str,tuple[object,bool,Exception|None]]={}
_inflight_guard=threading.Lock()
_MAX_INFLIGHT=8192

def _cached_upstream(key,loader,*,ttl=cache.CACHE_TTL):
    cached=cache.get_json(key)
    if cached is not None: return cached,True
    with _inflight_guard:
        event=_inflight.get(key)
        owner=event is None
        if owner:
            if len(_inflight)>=_MAX_INFLIGHT: event=None
            else:
                event=threading.Event(); _inflight[key]=event
    if not owner:
        event.wait(timeout=int(os.getenv("SINGLEFLIGHT_TIMEOUT","30")))
        cached=cache.get_json(key)
        if cached is not None: return cached,True
        with _inflight_guard: result=_inflight_results.pop(key,None)
        if result is not None:
            value,was_cached,error=result
            if error is not None: raise error
            return value,was_cached
        return _cached_upstream(key,loader,ttl=ttl)
    outcome=(None,False,None)
    try:
        with _upstream_gate: value=loader()
        cache.set_json(key,value,ttl=ttl); outcome=(value,False,None); return value,False
    except Exception as exc:
        outcome=(None,False,exc); raise
    finally:
        if event is not None:
            with _inflight_guard:
                _inflight_results[key]=outcome; _inflight.pop(key,None); event.set()
                if len(_inflight_results)>_MAX_INFLIGHT: _inflight_results.pop(next(iter(_inflight_results)),None)

@app.get("/")
def root(): return {"service":"spotifusion-music-api","status":"ok","version":"2.1.0","docs":"/docs"}
@app.get("/health")
def health(): return {"status":"ok","cache":"enabled" if cache._get_client() else "local-only"}

@app.get("/api/search")
@limiter.limit(SEARCH_RATE_LIMIT)
def search(request:Request,q:str=Query(...,min_length=2,max_length=120),category:str=Query("all",pattern="^(all|songs|albums|artists|playlists|jukebox)$"),batch:int=Query(1,ge=1,le=100000),_user=Depends(require_firebase_user)):
    query=" ".join(q.strip().split())
    page_size=max(10,min(int(os.getenv("SEARCH_PAGE_SIZE","25")),50)); category=category.lower()
    requested=min(batch*page_size,int(os.getenv("SEARCH_PROVIDER_MAX","5000")))
    key=cache.normalize_key(f"search:v7:{category}:{requested}",query)
    def load_search():
        try: return ytmusic.search_songs(query,limit=requested,category=category)
        except Exception as first_error:
            logger.warning("Primary search failed query=%r category=%s limit=%s: %s; retrying",query,category,requested,first_error)
            return ytmusic.search_songs(query,limit=min(page_size,10),category=category)
    try: all_results,cached=_cached_upstream(key,load_search,ttl=int(os.getenv("SEARCH_CACHE_TTL",str(6*3600))))
    except Exception as exc: raise HTTPException(502,"Music search is temporarily unavailable. Please retry in a moment.") from exc
    all_results=all_results if isinstance(all_results,list) else []
    start=(batch-1)*page_size; results=all_results[start:start+page_size]
    return {"query":query,"category":category,"batch":batch,"pageSize":page_size,"results":results,"hasMore":len(all_results)>start+len(results) or len(all_results)>=requested,"available":len(all_results),"cached":cached}

@app.get("/api/artist/{artist_id}")
@limiter.limit(SONG_RATE_LIMIT)
def artist(request:Request,artist_id:str,_user=Depends(require_firebase_user)):
    if not artist_id or len(artist_id)>128: raise HTTPException(400,"Invalid artist id.")
    try: payload,_=_cached_upstream(cache.normalize_key("artist:v1",artist_id),lambda:ytmusic.get_artist(artist_id),ttl=int(os.getenv("ENTITY_CACHE_TTL",str(6*3600))))
    except Exception as exc: raise HTTPException(502,"Unable to fetch this artist right now. Please try again.") from exc
    return payload

@app.get("/api/album/{album_id}")
@limiter.limit(SONG_RATE_LIMIT)
def album(request:Request,album_id:str,_user=Depends(require_firebase_user)):
    if not album_id or len(album_id)>128: raise HTTPException(400,"Invalid album id.")
    try: payload,_=_cached_upstream(cache.normalize_key("album:v1",album_id),lambda:ytmusic.get_album(album_id),ttl=int(os.getenv("ENTITY_CACHE_TTL",str(6*3600))))
    except Exception as exc: raise HTTPException(502,"Unable to fetch this album right now. Please try again.") from exc
    return payload

@app.get("/api/playlist/{playlist_id}")
@limiter.limit(SONG_RATE_LIMIT)
def playlist(request:Request,playlist_id:str,_user=Depends(require_firebase_user)):
    if not playlist_id or len(playlist_id)>128: raise HTTPException(400,"Invalid playlist id.")
    try: payload,_=_cached_upstream(cache.normalize_key("playlist:v1",playlist_id),lambda:ytmusic.get_playlist(playlist_id),ttl=int(os.getenv("ENTITY_CACHE_TTL",str(6*3600))))
    except Exception as exc: raise HTTPException(502,"Unable to fetch this playlist right now. Please try again.") from exc
    return payload

@app.get("/api/song/{video_id}")
@limiter.limit(SONG_RATE_LIMIT)
def song(request:Request,video_id:str,_user=Depends(require_firebase_user)):
    if not video_id or len(video_id)>32: raise HTTPException(400,"Invalid video id.")
    key=cache.normalize_key("song:v2",video_id)
    try: payload,_=_cached_upstream(key,lambda:{"found":(track:=ytmusic.get_song(video_id)) is not None,"track":track},ttl=int(os.getenv("SONG_CACHE_TTL",str(24*3600))))
    except Exception as exc: raise HTTPException(502,"Unable to fetch this track right now. Please try again.") from exc
    if not payload.get("found"): raise HTTPException(404,"Track not found.")
    return payload["track"]

@app.get("/api/stream/{video_id}")
@limiter.limit(STREAM_RATE_LIMIT)
def stream(request:Request,video_id:str,_user=Depends(require_firebase_user)):
    """Extract a playable audio URL on demand; never stores media on Render."""
    if not video_id or len(video_id)>32: raise HTTPException(400,"Invalid video id.")
    key=cache.normalize_key("stream:v1",video_id)
    try:
        payload,_=_cached_upstream(key,lambda:extract_audio(video_id),ttl=int(os.getenv("STREAM_CACHE_TTL","300")))
    except Exception as exc:
        logger.warning("Stream extraction failed for %s: %s",video_id,exc)
        raise HTTPException(502,"Audio stream is temporarily unavailable. Please try again.") from exc
    if not payload or not payload.get("url"): raise HTTPException(502,"No playable audio stream was found.")
    return payload

@app.get("/api/lyrics/{video_id}")
@limiter.limit(LYRICS_RATE_LIMIT)
def lyrics(request:Request,video_id:str,_user=Depends(require_firebase_user)):
    if not video_id or len(video_id)>32: raise HTTPException(400,"Invalid video id.")
    try: result,_=_cached_upstream(cache.normalize_key("lyrics:v2",video_id),lambda:ytmusic.get_lyrics(video_id),ttl=int(os.getenv("LYRICS_CACHE_TTL",str(24*3600))))
    except Exception as exc: raise HTTPException(502,"Unable to fetch lyrics right now. Please try again.") from exc
    return result

@app.get("/api/discover")
@limiter.limit(os.getenv("DISCOVER_RATE_LIMIT","120/minute"))
def discover(request:Request,_user=Depends(require_firebase_user)):
    try: tracks,cached=_cached_upstream("discover:v1:IN",lambda:ytmusic.get_discover("IN"),ttl=int(os.getenv("DISCOVER_CACHE_TTL",str(3*3600))))
    except Exception as exc: raise HTTPException(502,"Discovery is temporarily unavailable.") from exc
    trending=tracks.get("trending",[]) if isinstance(tracks,dict) else []; fresh=tracks.get("fresh",[]) if isinstance(tracks,dict) else []
    sections=[]
    if trending:
        sections.append({"id":"india-trending","label":"Trending","title":"Trending in India","subtitle":"Popular right now","tracks":trending})
        sections.append({"id":"top-picks","label":"Popular","title":"Top picks","subtitle":"Popular music to explore","tracks":trending[:12]})
    if fresh: sections.append({"id":"new-releases","label":"Fresh music","title":"New and recently surfaced","subtitle":"Fresh tracks from YouTube Music","tracks":fresh})
    if trending: sections.append({"id":"discover","label":"Discover","title":"Discover something new","subtitle":"A mix of popular music and fresh picks","tracks":(fresh[:12]+trending[6:18])[:24]})
    return {"sections":sections,"cached":cached}

@app.exception_handler(Exception)
def unhandled_exception_handler(request:Request,exc:Exception):
    logger.exception("Unhandled error on %s",request.url.path)
    return JSONResponse(status_code=500,content={"detail":"Internal server error."})
