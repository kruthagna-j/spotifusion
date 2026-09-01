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


def _pick_audio_url(info: dict[str, Any]) -> Optional[str]:
    """Pick an actual playable URL from the formats returned by yt-dlp."""
    direct = info.get("url")
    if direct and info.get("acodec") not in (None, "none"):
        return direct

    formats = info.get("formats") or []

    # Prefer audio-only formats. YouTube may expose these through different
    # clients depending on current PO-token enforcement.
    audio_only = [
        f for f in formats
        if f.get("url")
        and f.get("vcodec") == "none"
        and f.get("acodec") not in (None, "none")
    ]
    if audio_only:
        audio_only.sort(
            key=lambda f: (f.get("abr") or 0, f.get("asr") or 0),
            reverse=True,
        )
        return audio_only[0].get("url")

    # Last resort: a progressive format containing both video and audio is
    # still playable by an HTML audio element. This is useful when YouTube
    # withholds standalone audio formats (for example format 18).
    audio_capable = [
        f for f in formats
        if f.get("url") and f.get("acodec") not in (None, "none")
    ]
    if audio_capable:
        audio_capable.sort(
            key=lambda f: (
                1 if f.get("vcodec") == "none" else 0,
                f.get("abr") or 0,
                f.get("tbr") or 0,
                f.get("height") or 0,
            ),
            reverse=True,
        )
        return audio_capable[0].get("url")

    return None


def extract_audio(video_id: str) -> Optional[dict[str, Any]]:
    """Return a direct playable URL without downloading or storing media."""
    yt_dlp = _load_yt_dlp()
    if yt_dlp is None or not video_id:
        return None

    opts = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "noplaylist": True,
        # Do not force web_embedded/web_safari here. YouTube's currently
        # changing PO-token rules can make those clients expose only SABR/HLS
        # formats without a direct URL. yt-dlp's maintained default client
        # selection is better able to fall back to a directly playable format.
        "format": "bestaudio/best",
        "socket_timeout": 12,
        "retries": 1,
    }

    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(
                f"https://www.youtube.com/watch?v={video_id}",
                download=False,
            )
            if not info:
                return None

            audio_url = _pick_audio_url(info)
            if not audio_url:
                formats = info.get("formats") or []
                logger.warning(
                    "yt-dlp returned no playable audio URL for %s (formats=%s, "
                    "format_ids=%s)",
                    video_id,
                    len(formats),
                    [f.get("format_id") for f in formats[-12:]],
                )
                return None

            return {
                "id": info.get("id") or video_id,
                "url": audio_url,
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
