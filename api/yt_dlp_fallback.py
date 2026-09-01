"""Small yt-dlp fallback helpers for Spotifusion.

ytmusicapi remains the catalog/metadata provider. yt-dlp is used only when
YouTube Music metadata/playback extraction needs a fallback.
"""
from __future__ import annotations

import base64
import logging
import os
import tempfile
from typing import Any, Optional

logger = logging.getLogger("spotifusion.ytdlp")


def _load_yt_dlp():
    try:
        import yt_dlp
        return yt_dlp
    except ImportError:
        return None


def _cookie_file() -> tuple[Optional[str], Optional[tempfile.NamedTemporaryFile]]:
    """Materialize optional Render-provided YouTube cookies for yt-dlp.

    The secret is supplied as base64 in YTDLP_COOKIES_B64 so it never needs to
    be committed to Git. The temporary file is deleted after each extraction.
    """
    encoded = os.getenv("YTDLP_COOKIES_B64", "").strip()
    if not encoded:
        return None, None
    try:
        data = base64.b64decode(encoded, validate=True)
        if not data:
            return None, None
        tmp = tempfile.NamedTemporaryFile(prefix="spotifusion-cookies-", suffix=".txt", delete=False)
        tmp.write(data)
        tmp.flush()
        tmp.close()
        return tmp.name, tmp
    except Exception as exc:
        logger.error("Could not decode YTDLP_COOKIES_B64: %s", exc)
        return None, None


def _pick_audio_url(info: dict[str, Any]) -> Optional[str]:
    """Pick an actual playable HTTP(S) URL from yt-dlp output."""
    direct = info.get("url")
    if direct and info.get("acodec") not in (None, "none"):
        return direct

    formats = info.get("formats") or []

    audio_only = [
        f for f in formats
        if f.get("url")
        and f.get("protocol") not in ("m3u8", "m3u8_native", "http_dash_segments", "dash")
        and f.get("vcodec") == "none"
        and f.get("acodec") not in (None, "none")
    ]
    if audio_only:
        audio_only.sort(
            key=lambda f: (f.get("abr") or 0, f.get("asr") or 0),
            reverse=True,
        )
        return audio_only[0].get("url")

    progressive = [
        f for f in formats
        if f.get("url")
        and f.get("protocol") not in ("m3u8", "m3u8_native", "http_dash_segments", "dash")
        and f.get("acodec") not in (None, "none")
    ]
    if progressive:
        progressive.sort(
            key=lambda f: (
                1 if f.get("format_id") == "18" else 0,
                1 if f.get("vcodec") != "none" else 0,
                f.get("abr") or 0,
                f.get("tbr") or 0,
                f.get("height") or 0,
            ),
            reverse=True,
        )
        return progressive[0].get("url")

    return None


def _extract_with_options(yt_dlp, video_id: str, opts: dict[str, Any]) -> Optional[dict[str, Any]]:
    cookie_path = None
    cookie_tmp = None
    try:
        cookie_path, cookie_tmp = _cookie_file()
        if cookie_path:
            opts = {**opts, "cookiefile": cookie_path}
            user_agent = os.getenv("YTDLP_USER_AGENT", "").strip()
            if user_agent:
                opts["http_headers"] = {"User-Agent": user_agent}

        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(
                f"https://www.youtube.com/watch?v={video_id}",
                download=False,
            )
            if not info:
                return None
            audio_url = _pick_audio_url(info)
            if not audio_url:
                logger.warning(
                    "yt-dlp returned no direct playable format for %s (formats=%s)",
                    video_id,
                    [f.get("format_id") for f in (info.get("formats") or [])],
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
    finally:
        if cookie_tmp and cookie_path:
            try:
                os.unlink(cookie_path)
            except OSError:
                pass


def extract_audio(video_id: str) -> Optional[dict[str, Any]]:
    """Return a direct playable URL without downloading or storing media."""
    yt_dlp = _load_yt_dlp()
    if yt_dlp is None or not video_id:
        return None

    common = {
        "quiet": True,
        "no_warnings": False,
        "skip_download": True,
        "noplaylist": True,
        "socket_timeout": 12,
        "retries": 1,
    }

    # If YTDLP_COOKIES_B64 is configured on Render, yt-dlp uses that browser
    # session to pass YouTube's bot/login check. Without it we still try the
    # no-cookie clients first, so public playback continues to work when the
    # Render IP is not challenged.
    android_vr = {
        **common,
        "format": "18/best",
        "extractor_args": {"youtube": {"player_client": ["android_vr"]}},
    }
    result = _extract_with_options(yt_dlp, video_id, android_vr)
    if result:
        logger.info("Using android_vr playback fallback for %s", video_id)
        return result

    default = {
        **common,
        "format": "bestaudio/best",
    }
    result = _extract_with_options(yt_dlp, video_id, default)
    if result:
        logger.info("Using default yt-dlp playback extraction for %s", video_id)
        return result

    logger.warning("No playable direct audio URL returned for %s", video_id)
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
