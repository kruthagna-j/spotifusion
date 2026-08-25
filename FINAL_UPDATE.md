# Spotifusion Final Update

## Scalability / traffic protection
- Per-authenticated-user rate limits replace the old shared IP bucket.
- Search: 300 requests/minute per authenticated user by default.
- Song metadata: 600 requests/minute per authenticated user by default.
- Lyrics: 120 requests/minute per authenticated user by default.
- Two-tier cache: per-instance LRU/TTL cache in front of shared Redis.
- Normalized search keys collapse case/whitespace variants.
- Request coalescing prevents simultaneous identical cold requests from fanning out.
- Bounded upstream concurrency keeps each API instance from creating an unbounded burst.
- Longer metadata/lyrics TTLs reduce repeated upstream calls.

## Rendering / algorithm improvements
- Track rows use React.memo.
- Track rows use a dedicated low-frequency player context, so 500 ms progress updates no longer force every row to render.
- Firestore listeners are shared per user/resource instead of one listener per row/menu.
- Liked-song lookup uses a Set, changing row membership checks from linear scan to expected O(1).
- Synced-lyrics active-line lookup uses binary search, O(log n), instead of scanning all lyric lines on every progress update.
- Shuffle uses Fisher-Yates O(n) order generation instead of repeated random picks.
- Search uses a 500 ms debounce and ignores queries shorter than two characters.
- Client search/song/lyrics requests use bounded LRU caches and in-flight request coalescing.

## Deployment cleanup
- Removed the stale production backend fallback URL.
- `.env` is not included in the delivery ZIP; `.env.example` is provided.
- Updated backend/frontend deployment documentation.
- Added `/health` endpoint.
- API version bumped to 2.0.0.

## Scale note
A high customer rate limit cannot honestly guarantee zero upstream traffic for 10 million customers making 10 million unique cold queries. This build minimizes duplicate/hot traffic and bounds cold upstream concurrency. For very large production traffic, deploy multiple API instances behind a load balancer and use one managed Redis cluster shared by all instances.
