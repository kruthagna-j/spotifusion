"""
Spotifusion music-search backend.

Free alternative to the official YouTube Data API: uses ytmusicapi (an
unofficial, key-less client for YouTube Music's public search) instead of a
quota-limited, key-required Google API. Results are normalized into the same
track shape (id/title/artist/duration/thumbnail/source) the existing
Spotifusion frontend and YouTube IFrame Player already use — no player
changes required.

/api/search and /api/song require a signed-in Spotifusion account (see
auth.py) — search results and streaming metadata are not available to
anonymous callers, enforced here at the API layer, not just hidden behind a
frontend sign-in screen.

Run locally (from this `api/` directory):
    pip install -r requirements.txt
    uvicorn main:app --reload --port 8000

See ../README.md and this repo's chat history for full setup/deploy notes.
"""
import logging
import os
from typing import Optional

from fastapi import Depends, FastAPI, HTTPException, Query, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address

import cache
from auth import require_firebase_user
from services import ytmusic

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("spotifusion.api")

RATE_LIMIT_PER_MINUTE = int(os.getenv("RATE_LIMIT_PER_MINUTE", "60"))
ALLOWED_ORIGINS = [
    origin.strip()
    for origin in os.getenv(
        "ALLOWED_ORIGINS", "http://localhost:5173,http://127.0.0.1:5173,http://10.10.140.86:5173,https://spotifusion.vercel.app"
    ).split(",")
    if origin.strip()
]

limiter = Limiter(key_func=get_remote_address, default_limits=[f"{RATE_LIMIT_PER_MINUTE}/minute"])

app = FastAPI(title="Spotifusion Music API", version="1.0.0")
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

app.add_middleware(
    CORSMiddleware,
    allow_origins=ALLOWED_ORIGINS,
    allow_credentials=False,
    allow_methods=["GET", "OPTIONS"],
    allow_headers=["*"],
)


@app.get("/")
def root():
    return {
        "service": "spotifusion-music-api",
        "status": "ok",
        "docs": "/docs",
    }


@app.get("/api/search")
@limiter.limit(f"{RATE_LIMIT_PER_MINUTE}/minute")
def search(
    request: Request,
    q: str = Query(..., min_length=1, max_length=200),
    _user=Depends(require_firebase_user),
):
    query = q.strip()
    if not query:
        raise HTTPException(status_code=400, detail="Query parameter 'q' must not be empty.")

    cache_key = cache.normalize_key("search", query)
    cached = cache.get_json(cache_key)
    if cached is not None:
        return {"query": query, "results": cached, "cached": True}

    try:
        tracks = ytmusic.search_songs(query, limit=20)
    except Exception as exc:  # noqa: BLE001 - upstream (ytmusicapi/YouTube Music) failure
        logger.error("Search failed for query=%r: %s", query, exc)
        raise HTTPException(
            status_code=502,
            detail="Unable to search right now. Please try again.",
        ) from exc

    cache.set_json(cache_key, tracks)
    return {"query": query, "results": tracks, "cached": False}


@app.get("/api/song/{video_id}")
@limiter.limit(f"{RATE_LIMIT_PER_MINUTE}/minute")
def song(request: Request, video_id: str, _user=Depends(require_firebase_user)):
    if not video_id or len(video_id) > 32:
        raise HTTPException(status_code=400, detail="Invalid video id.")

    cache_key = cache.normalize_key("song", video_id)
    cached = cache.get_json(cache_key)
    if cached is not None:
        return cached

    try:
        track = ytmusic.get_song(video_id)
    except Exception as exc:  # noqa: BLE001
        logger.error("Song lookup failed for id=%r: %s", video_id, exc)
        raise HTTPException(
            status_code=502,
            detail="Unable to fetch this track right now. Please try again.",
        ) from exc

    if track is None:
        raise HTTPException(status_code=404, detail="Track not found.")

    cache.set_json(cache_key, track)
    return track


@app.get("/api/lyrics/{video_id}")
@limiter.limit(f"{RATE_LIMIT_PER_MINUTE}/minute")
def lyrics(request: Request, video_id: str, _user=Depends(require_firebase_user)):
    if not video_id or len(video_id) > 32:
        raise HTTPException(status_code=400, detail="Invalid video id.")

    cache_key = cache.normalize_key("lyrics", video_id)
    cached = cache.get_json(cache_key)
    if cached is not None:
        return cached

    try:
        result = ytmusic.get_lyrics(video_id)
    except Exception as exc:  # noqa: BLE001 - genuine upstream failure, distinct from "no lyrics" (handled inside get_lyrics)
        logger.error("Lyrics lookup failed for id=%r: %s", video_id, exc)
        raise HTTPException(
            status_code=502,
            detail="Unable to fetch lyrics right now. Please try again.",
        ) from exc

    # Cache "unavailable" results too (shorter TTL) — a track without
    # lyrics doesn't suddenly get them, so this avoids re-querying upstream
    # for the same not-found result on every playback.
    if result.get("available"):
        cache.set_json(cache_key, result)  # default TTL (cache.CACHE_TTL)
    else:
        cache.set_json(cache_key, result, ttl=3600)
    return result


# Note: artist/album/playlist endpoints were deliberately left out.


@app.exception_handler(Exception)
def unhandled_exception_handler(request: Request, exc: Exception):
    logger.exception("Unhandled error on %s", request.url.path)
    return JSONResponse(status_code=500, content={"detail": "Internal server error."})
