"""Fast two-tier cache used by the music API.

Redis is the shared cache for horizontally scaled instances. A small in-process
LRU/TTL cache sits in front of Redis so hot requests can be answered without a
network hop. Cache failures are intentionally non-fatal.
"""
from __future__ import annotations
import json, logging, os, threading, time
from collections import OrderedDict
from typing import Any, Optional

logger = logging.getLogger("spotifusion.cache")
REDIS_URL = os.getenv("REDIS_URL", "redis://localhost:6379")
CACHE_TTL = int(os.getenv("CACHE_TTL", "21600"))
LOCAL_CACHE_MAX = int(os.getenv("LOCAL_CACHE_MAX", "2048"))
LOCAL_CACHE_TTL = int(os.getenv("LOCAL_CACHE_TTL", "900"))
_client = None
_client_init_failed = False
_client_lock = threading.Lock()
_memory: OrderedDict[str, tuple[float, Any]] = OrderedDict()
_memory_lock = threading.Lock()

def _get_client():
    global _client, _client_init_failed
    if _client is not None: return _client
    if _client_init_failed: return None
    with _client_lock:
        if _client is not None: return _client
        if _client_init_failed: return None
        try:
            import redis
            client = redis.Redis.from_url(REDIS_URL, socket_connect_timeout=1, socket_timeout=1, health_check_interval=30, decode_responses=True)
            client.ping()
            _client = client
            logger.info("Connected to Redis at %s", REDIS_URL)
        except Exception as exc:
            _client_init_failed = True
            logger.warning("Redis unavailable (%s) — using local cache only.", exc)
    return _client

def normalize_key(prefix: str, value: str) -> str:
    normalized = " ".join(value.strip().lower().split())
    return f"{prefix}:{normalized}"

def _memory_get(key: str) -> Optional[Any]:
    now=time.monotonic()
    with _memory_lock:
        item=_memory.get(key)
        if item is None: return None
        expires,value=item
        if expires <= now:
            _memory.pop(key,None); return None
        _memory.move_to_end(key); return value

def _memory_set(key: str, value: Any, ttl: int) -> None:
    with _memory_lock:
        _memory[key]=(time.monotonic()+ttl,value); _memory.move_to_end(key)
        while len(_memory)>LOCAL_CACHE_MAX: _memory.popitem(last=False)

def get_json(key: str) -> Optional[Any]:
    local=_memory_get(key)
    if local is not None: return local
    client=_get_client()
    if client is None: return None
    try:
        raw=client.get(key)
        if not raw: return None
        value=json.loads(raw); _memory_set(key,value,LOCAL_CACHE_TTL); return value
    except Exception as exc:
        logger.warning("Redis GET failed for key=%s: %s",key,exc); return None

def set_json(key: str, value: Any, ttl: int=CACHE_TTL) -> None:
    _memory_set(key,value,min(ttl,LOCAL_CACHE_TTL))
    client=_get_client()
    if client is None: return
    try: client.setex(key,ttl,json.dumps(value,separators=(",",":")))
    except Exception as exc: logger.warning("Redis SET failed for key=%s: %s",key,exc)
