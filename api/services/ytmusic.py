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


def _artwork(thumbnails: Optional[list]) -> dict:
    """Return stable artwork variants without forcing the frontend to stretch
    a tiny thumbnail. ytmusicapi normally supplies several sizes."""
    if not thumbnails:
        return {"small": None, "medium": None, "large": None}
    urls = [x.get("url") for x in thumbnails if isinstance(x, dict) and x.get("url")]
    if not urls:
        return {"small": None, "medium": None, "large": None}
    return {
        "small": urls[0],
        "medium": urls[min(len(urls) - 1, max(0, len(urls) // 2))],
        "large": urls[-1],
    }

def _best_thumbnail(thumbnails: Optional[list]) -> Optional[str]:
    return _artwork(thumbnails).get("large")


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
        "artwork": _artwork(item.get("thumbnails")),
        "source": "youtube",
    }


def _entity_artwork(item: dict) -> dict:
    return _artwork(item.get("thumbnails"))


def _to_search_entity(item: dict) -> Optional[dict]:
    """Normalize mixed YouTube Music search results.

    The search endpoint intentionally keeps songs playable while exposing
    non-playable discovery entities (artists, albums, playlists and mixes)
    separately so the UI can render them without pretending they are tracks.
    """
    result_type = str(item.get("resultType") or item.get("type") or "").lower()
    artwork = _entity_artwork(item)
    if result_type in {"song", "video"} or item.get("videoId"):
        track = _to_track(item)
        if not track:
            return None
        track["resultType"] = "song"
        return track

    if result_type == "artist" or item.get("artistId"):
        return {
            "id": item.get("browseId") or item.get("artistId"),
            "resultType": "artist",
            "title": item.get("artist") or item.get("title") or "Unknown artist",
            "artist": item.get("artist") or item.get("title") or "Unknown artist",
            "subtitle": item.get("description") or "Artist",
            "thumbnail": artwork.get("large"),
            "artwork": artwork,
            "browseId": item.get("browseId") or item.get("artistId"),
            "source": "youtube",
        }

    if result_type == "album" or item.get("albumId"):
        artists = item.get("artists") or []
        return {
            "id": item.get("browseId") or item.get("albumId"),
            "resultType": "album",
            "title": item.get("title") or "Unknown album",
            "artist": _artist_name(artists),
            "subtitle": item.get("year") or "Album",
            "thumbnail": artwork.get("large"),
            "artwork": artwork,
            "browseId": item.get("browseId") or item.get("albumId"),
            "source": "youtube",
        }

    if result_type in {"playlist", "community_playlist", "mix"} or item.get("playlistId"):
        playlist_id = item.get("playlistId") or item.get("browseId")
        title = item.get("title") or "Playlist"
        lower_title = title.lower()
        entity_kind = "jukebox" if any(word in lower_title for word in ("jukebox", "mix", "radio", "station")) or result_type == "mix" else "playlist"
        return {
            "id": playlist_id,
            "resultType": entity_kind,
            "title": title,
            "artist": item.get("author") or item.get("description") or "Playlist",
            "subtitle": item.get("description") or item.get("author") or ("Jukebox / Mix" if entity_kind == "jukebox" else "Playlist"),
            "thumbnail": artwork.get("large"),
            "artwork": artwork,
            "browseId": playlist_id,
            "source": "youtube",
        }

    return None


def search_songs(query: str, limit: int = 100, category: str = "all") -> list[dict]:
    """Search one category and normalize it for the frontend.

    ``ytmusicapi.search`` supports filtered searches and follows YouTube
    Music continuations up to the requested limit. The frontend uses batches
    so it never has to render a giant result set at once.
    """
    client = _client()
    category = (category or "all").lower()
    if category == "songs":
        filters = ["songs"]
    elif category == "albums":
        filters = ["albums"]
    elif category == "artists":
        filters = ["artists"]
    elif category == "playlists":
        filters = ["playlists", "community_playlists", "featured_playlists"]
    elif category in {"jukebox", "mixes", "mix"}:
        # Jukebox/radio/mix shelves are exposed by the default mixed search.
        filters = [None]
    else:
        filters = ["songs", "albums", "artists", "playlists"]

    entities: list[dict] = []
    seen: set[str] = set()
    for filter_name in filters:
        try:
            items = client.search(query, filter=filter_name, limit=max(20, int(limit)))
        except Exception as exc:
            logger.warning("Search failed for category=%s filter=%s: %s", category, filter_name, exc)
            continue
        for item in items or []:
            try:
                entity = _to_search_entity(item)
                if not entity or not entity.get("id"):
                    continue
                if category == "jukebox" and entity.get("resultType") != "jukebox":
                    continue
                if entity["id"] in seen:
                    continue
                seen.add(entity["id"])
                entities.append(entity)
            except Exception as exc:
                logger.warning("Skipping malformed search result: %s (%s)", item, exc)
    return entities


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
        "artwork": _artwork(thumbnails),
        "source": "youtube",
    }


def _seconds_to_mmss(total_seconds: int) -> str:
    m, s = divmod(max(0, total_seconds), 60)
    return f"{m}:{s:02d}"


def get_lyrics(video_id: str) -> dict:
    """Looks up lyrics for a track. ytmusicapi's real flow is two calls, not
    one: get_watch_playlist() gives the lyrics browseId for this video (if
    YouTube Music has one at all), then get_lyrics() fetches the actual
    lyrics for that browseId. Tries timestamped lyrics first (for
    synchronized scrolling) and falls back to plain lyrics — many tracks
    only have one or the other, or neither.

    Never fabricates: any failure at any step (no browseId, no lyrics,
    upstream error) returns {"available": False} rather than raising, since
    "no lyrics for this track" is an expected, common, non-error outcome.
    """
    try:
        watch_playlist = _client().get_watch_playlist(videoId=video_id, limit=1)
        browse_id = watch_playlist.get("lyrics")
        if not browse_id:
            return {"available": False}
    except Exception as exc:  # noqa: BLE001
        logger.info("No lyrics browseId for video=%s: %s", video_id, exc)
        return {"available": False}

    # Try timed (synchronized) lyrics first.
    try:
        timed = _client().get_lyrics(browse_id, timestamps=True)
        if timed and timed.get("lyrics"):
            lines = [
                {
                    "text": line["text"],
                    "startTimeSeconds": line["start_time"],
                    "endTimeSeconds": line["end_time"],
                }
                for line in timed["lyrics"]
            ]
            return {"available": True, "synced": True, "lines": lines, "source": timed.get("source")}
    except Exception as exc:  # noqa: BLE001 - timed lyrics are frequently unavailable; this is expected, not an error
        logger.info("Timed lyrics unavailable for video=%s: %s", video_id, exc)

    # Fall back to plain (unsynchronized) lyrics.
    try:
        plain = _client().get_lyrics(browse_id, timestamps=False)
        if plain and plain.get("lyrics"):
            return {"available": True, "synced": False, "text": plain["lyrics"], "source": plain.get("source")}
    except Exception as exc:  # noqa: BLE001
        logger.info("Plain lyrics unavailable for video=%s: %s", video_id, exc)

    return {"available": False}


def _chart_tracks(data: Any) -> list[dict]:
    tracks = []
    if not isinstance(data, dict):
        return tracks
    for item in data.get("videos") or data.get("tracks") or []:
        try:
            track = _to_track(item)
            if track:
                tracks.append(track)
        except Exception:
            continue
    return tracks

def _home_section_tracks(section: Any) -> list[dict]:
    if not isinstance(section, dict):
        return []
    found = []
    for item in section.get("contents") or []:
        if not isinstance(item, dict):
            continue
        try:
            track = _to_track(item)
            if track:
                found.append(track)
        except Exception:
            pass
    return found

def get_fresh_tracks() -> list[dict]:
    """Read YouTube Music home sections that are explicitly about new/fresh
    music when the installed ytmusicapi version exposes get_home()."""
    try:
        client = _client()
        if not hasattr(client, "get_home"):
            return []
        sections = client.get_home() or []
        for section in sections:
            title = str(section.get("title") or "").lower() if isinstance(section, dict) else ""
            if any(word in title for word in ("new", "release", "fresh")):
                tracks = _home_section_tracks(section)
                if tracks:
                    return tracks[:24]
    except Exception as exc:
        logger.info("Fresh releases unavailable: %s", exc)
    return []

def get_discover(country: str = "IN") -> dict:
    """Fetch a compact, cacheable discovery snapshot."""
    trending = []
    fresh = []
    try:
        data = _client().get_charts(country=country)
        trending = _chart_tracks(data)[:30]
    except Exception as exc:
        logger.info("Charts unavailable for %s: %s", country, exc)
    fresh = get_fresh_tracks()
    if not fresh:
        # Keep Home useful when a particular YT Music home section is absent.
        fresh = trending[:18]
    return {"trending": trending, "fresh": fresh}
