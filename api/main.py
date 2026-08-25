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
from collections import defaultdict
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
RATE_LIMIT_PER_MINUTE=int(os.getenv("RATE_LIMIT_PER_MINUTE","300"))
SEARCH_RATE_LIMIT=os.getenv("SEARCH_RATE_LIMIT",f"{RATE_LIMIT_PER_MINUTE}/minute")
SONG_RATE_LIMIT=os.getenv("SONG_RATE_LIMIT",f"{max(RATE_LIMIT_PER_MINUTE,600)}/minute")
LYRICS_RATE_LIMIT=os.getenv("LYRICS_RATE_LIMIT","120/minute")
UPSTREAM_CONCURRENCY=int(os.getenv("UPSTREAM_CONCURRENCY","8"))
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
_key_locks:dict[str,threading.Lock]=defaultdict(threading.Lock)
_key_locks_guard=threading.Lock(); _MAX_KEY_LOCKS=4096

def _key_lock(key:str)->threading.Lock:
    with _key_locks_guard:
        if len(_key_locks)>=_MAX_KEY_LOCKS: return _key_locks.setdefault("__overflow__",threading.Lock())
        return _key_locks[key]

def _cached_upstream(key,loader,*,ttl=cache.CACHE_TTL):
    cached=cache.get_json(key)
    if cached is not None: return cached,True
    lock=_key_lock(key)
    with lock:
        cached=cache.get_json(key)
        if cached is not None: return cached,True
        with _upstream_gate: value=loader()
        cache.set_json(key,value,ttl=ttl)
        return value,False

@app.get("/")
def root(): return {"service":"spotifusion-music-api","status":"ok","version":"2.0.0","docs":"/docs"}

@app.get("/health")
def health(): return {"status":"ok","cache":"enabled" if cache._get_client() else "local-only"}

@app.get("/api/search")
@limiter.limit(SEARCH_RATE_LIMIT)
def search(request:Request,q:str=Query(...,min_length=2,max_length=120),_user=Depends(require_firebase_user)):
    query=" ".join(q.strip().split())
    if len(query)<2: raise HTTPException(400,"Search query is too short.")
    key=cache.normalize_key("search:v2",query)
    try:
        tracks,cached=_cached_upstream(key,lambda:ytmusic.search_songs(query,limit=20),ttl=int(os.getenv("SEARCH_CACHE_TTL",str(6*3600))))
    except Exception as exc:
        logger.error("Search failed for query=%r: %s",query,exc)
        raise HTTPException(502,"Unable to search right now. Please try again.") from exc
    return {"query":query,"results":tracks,"cached":cached}

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

@app.exception_handler(Exception)
def unhandled_exception_handler(request:Request,exc:Exception):
    logger.exception("Unhandled error on %s",request.url.path)
    return JSONResponse(status_code=500,content={"detail":"Internal server error."})
