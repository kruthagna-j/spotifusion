# Spotifusion music-search backend

Free, key-less alternative to the official YouTube Data API. Uses
[ytmusicapi](https://github.com/sigma67/ytmusicapi) (unofficial, unauthenticated
access to YouTube Music's public search) so search has no Google API quota or
billing dependency at all.

```
api/
├── main.py              FastAPI app: routes, CORS, rate limiting
├── cache.py              Redis wrapper (degrades gracefully if Redis is down)
├── requirements.txt
├── Dockerfile
└── services/
    └── ytmusic.py        ytmusicapi wrapper, normalizes results
```

This is a **standalone service**, not a Vercel serverless function — it needs
a persistent Redis connection and is meant to scale horizontally (multiple
containers behind a load balancer), which doesn't fit Vercel's per-request
serverless model. `.vercelignore` keeps these files out of the frontend's
Vercel deploy so they don't collide with it.

## Run locally

```bash
cd api
python3 -m venv .venv
source .venv/bin/activate       # Windows: .venv\Scripts\activate
pip install -r requirements.txt
cp .env.example .env            # defaults are fine for local dev
uvicorn main:app --reload --port 8000
```

Then test it:

```bash
curl http://localhost:8000/
curl "http://localhost:8000/api/search?q=Blinding%20Lights"
```

Redis is optional locally — if it's not running, you'll see a logged warning
("Redis unavailable ... continuing without cache") and search still works,
just uncached.

To run Redis locally too: `docker run -p 6379:6379 redis:7-alpine`

## Deploy

Any host that runs a long-lived Python process works — Render, Railway,
Fly.io, a plain VPS, or the included `Dockerfile`:

```bash
docker build -t spotifusion-api .
docker run -p 8000:8000 --env-file .env spotifusion-api
```

For horizontal scaling, run several containers (or platform instances) all
pointed at the **same** `REDIS_URL` (e.g. a managed Redis add-on — Render,
Railway, and Upstash all have free tiers) behind a load balancer. The app has
no in-memory state that matters across requests, so any instance can serve
any request.

After deploying, set `VITE_MUSIC_API_URL` in the frontend's environment
(Vercel dashboard) to the deployed URL, e.g. `https://your-api.onrender.com`.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated list of origins allowed to call this API (CORS) |
| `REDIS_URL` | `redis://localhost:6379` | Redis connection string; safe to leave unset/wrong — falls back to no caching |
| `CACHE_TTL` | `3600` | Seconds a cached search/song result is kept |
| `RATE_LIMIT_PER_MINUTE` | `60` | Per-client (by IP) request budget |

## Endpoints

- `GET /` — health check
- `GET /api/search?q=...` — search songs, returns `{ query, results, cached }`
- `GET /api/song/{video_id}` — single track metadata lookup

Artist/album/playlist/lyrics endpoints were intentionally left out: they
aren't used anywhere in the current frontend, and ytmusicapi's lyrics
endpoint specifically is unreliable enough (frequent rate limits/missing
data) that shipping it wouldn't be worth the added surface area. Straightforward
to add later following the same pattern as `/api/song` if a page needs them.

## Limitations

- This is unofficial access to YouTube Music, not a licensed API — it can
  break if YouTube changes its internal endpoints, and is still subject to
  YouTube's own rate limiting/blocking of automated traffic at scale. Redis
  caching and the built-in rate limiter both help reduce how often you hit
  upstream, but neither guarantees unlimited or unbreakable access.
- No lyrics/artist/album pages are wired up (see above).
