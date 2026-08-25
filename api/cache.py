"""
Thin Redis cache wrapper for search results.

Design requirement from spec: the API must keep working if Redis is down —
connection/get/set errors are logged and swallowed, never raised, so a
missing/unreachable Redis degrades to "always cache miss" instead of crashing
requests.
"""
import json
import logging
import os
from typing import Any, Optional

logger = logging.getLogger("spotifusion.cache")

REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379")
CACHE_TTL = int(os.getenv("CACHE_TTL", "3600"))

_client = None
_client_init_failed = False


def _get_client():
    """Lazily create the Redis client. Returns None if Redis is unavailable."""
    global _client, _client_init_failed
    if _client is not None:
        return _client
    if _client_init_failed:
        return None
    try:
        import redis  # imported lazily so the app still starts without the package during local hacking

        client = redis.Redis.from_url(REDIS_URL, socket_connect_timeout=2, socket_timeout=2)
        client.ping()
        _client = client
        logger.info("Connected to Redis at %s", REDIS_URL)
        return _client
    except Exception as exc:  # noqa: BLE001 - intentionally broad: cache must never take the API down
        _client_init_failed = True
        logger.warning("Redis unavailable (%s) — continuing without cache.", exc)
        return None


def normalize_key(prefix: str, value: str) -> str:
    """e.g. normalize_key('search', 'Blinding Lights') -> 'search:blinding lights'"""
    return f"{prefix}:{value.strip().lower()}"


def get_json(key: str) -> Optional[Any]:
    client = _get_client()
    if client is None:
        return None
    try:
        raw = client.get(key)
        return json.loads(raw) if raw else None
    except Exception as exc:  # noqa: BLE001
        logger.warning("Redis GET failed for key=%s: %s", key, exc)
        return None


def set_json(key: str, value: Any, ttl: int = CACHE_TTL) -> None:
    client = _get_client()
    if client is None:
        return
    try:
        client.setex(key, ttl, json.dumps(value))
    except Exception as exc:  # noqa: BLE001
        logger.warning("Redis SET failed for key=%s: %s", key, exc)
