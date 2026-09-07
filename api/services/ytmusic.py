"""
Wraps ytmusicapi and converts its results into the flat track schema the
existing Spotifusion frontend already expects (same shape TrackRow.jsx /
PlayerContext.jsx use for YouTube-backed tracks — id/title/artist/duration/
thumbnail/source), so no frontend player logic needs to change.
"""
import logging
from typing import Any, Optional

from ytmusicapi import YTMusic

logger = logging.getLogger("spotifusion.ytmusic")

# No OAuth/login needed for search — this uses YTMusic's public,
# unauthenticated search, which is what keeps this free and key-less.
_yt: Optional[YTMusic] = None


def _client() -> YTMusic:
    global _yt
    if _yt is None:
        _yt = YTMusic()
    return _yt


def _best_thumbnail(thumbnails: Optional[list]) -> Optional[str]:
    if not thumbnails:
        return None
    # ytmusicapi returns thumbnails smallest-to-largest; take the largest.
    return thumbnails[-1].get("url")


def _artist_name(artists: Optional[list]) -> str:
    if not artists:
        return "Unknown artist"
    return ", ".join(a.get("name", "") for a in artists if a.get("name")) or "Unknown artist"


def _to_track(item: dict) -> Optional[dict]:
    """Normalize one ytmusicapi search result. Returns None for anything
    without a playable videoId (e.g. artist/album cards that can slip through
    depending on the query)."""
    video_id = item.get("videoId")
    if not video_id:
        return None

    album = item.get("album") or {}
    return {
        "id": video_id,
        "title": item.get("title") or "Untitled",
        "artist": _artist_name(item.get("artists")),
        "album": album.get("name") if isinstance(album, dict) else None,
        "duration": item.get("duration"),  # "M:SS" string, already display-ready
        "durationSeconds": item.get("duration_seconds"),
        "thumbnail": _best_thumbnail(item.get("thumbnails")),
        "source": "youtube",
    }


def search_songs(query: str, limit: int = 20) -> list[dict]:
    """Search YouTube Music for songs matching `query`. Never raises for
    "no results" — returns an empty list. Raises only for genuine upstream
    failures, which the API layer turns into a friendly error."""
    results = _client().search(query, filter="songs", limit=limit)
    tracks = []
    for item in results:
        try:
            track = _to_track(item)
        except Exception as exc:  # noqa: BLE001 - one malformed result shouldn't kill the whole search
            logger.warning("Skipping malformed search result: %s (%s)", item.get("videoId"), exc)
            continue
        if track:
            tracks.append(track)
    return tracks


def get_song(video_id: str) -> Optional[dict]:
    """Look up a single track's metadata by video id."""
    data: dict[str, Any] = _client().get_song(video_id)
    details = data.get("videoDetails") or {}
    if not details.get("videoId"):
        return None
    thumbnails = (details.get("thumbnail") or {}).get("thumbnails")
    length = details.get("lengthSeconds")
    return {
        "id": details.get("videoId"),
        "title": details.get("title") or "Untitled",
        "artist": details.get("author") or "Unknown artist",
        "album": None,
        "duration": _seconds_to_mmss(int(length)) if length else None,
        "durationSeconds": int(length) if length else None,
        "thumbnail": _best_thumbnail(thumbnails),
        "source": "youtube",
    }


def _seconds_to_mmss(total_seconds: int) -> str:
    m, s = divmod(max(0, total_seconds), 60)
    return f"{m}:{s:02d}"
