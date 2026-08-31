"""Small yt-dlp fallback helpers for Spotifusion.

ytmusicapi remains the catalog/metadata provider. yt-dlp is used only when
YouTube Music metadata/playback extraction needs a fallback.
"""
from __future__ import annotations

import logging
from typing import Any, Optional

logger = logging.getLogger("spotifusion.ytdlp")


def _load_yt_dlp():
    try:
        import yt_dlp
        return yt_dlp
    except ImportError:
        return None


def extract_audio(video_id: str) -> Optional[dict[str, Any]]:
    """Return a direct audio URL and basic metadata without downloading media."""
    yt_dlp = _load_yt_dlp()
    if yt_dlp is None or not video_id:
        return None

    opts = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "noplaylist": True,
        "format": "bestaudio/best",
        "socket_timeout": 12,
        "retries": 1,
    }
    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(f"https://www.youtube.com/watch?v={video_id}", download=False)
            if not info or not info.get("url"):
                return None
            return {
                "id": info.get("id") or video_id,
                "url": info.get("url"),
                "title": info.get("title"),
                "durationSeconds": info.get("duration"),
                "thumbnail": info.get("thumbnail"),
                "source": "youtube-ytdlp",
            }
    except Exception as exc:
        logger.warning("yt-dlp extraction failed for %s: %s", video_id, exc)
        return None


def extract_playlist(playlist_id: str, limit: int = 100) -> list[dict[str, Any]]:
    """Extract playlist entries without downloading any media."""
    yt_dlp = _load_yt_dlp()
    if yt_dlp is None or not playlist_id:
        return []

    opts = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "extract_flat": True,
        "playlistend": max(1, int(limit)),
        "socket_timeout": 12,
        "retries": 1,
    }
    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(
                f"https://www.youtube.com/playlist?list={playlist_id}",
                download=False,
            )
            entries = info.get("entries") if info else None
            result: list[dict[str, Any]] = []
            for entry in entries or []:
                if not entry or not entry.get("id"):
                    continue
                result.append({
                    "id": entry.get("id"),
                    "title": entry.get("title") or "Untitled",
                    "durationSeconds": entry.get("duration"),
                    "thumbnail": entry.get("thumbnail"),
                    "source": "youtube-ytdlp",
                })
            return result
    except Exception as exc:
        logger.warning("yt-dlp playlist extraction failed for %s: %s", playlist_id, exc)
        return []
