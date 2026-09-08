# SpotiFusion Backend

Production API service for the Android and web clients.

## Run

```bash
npm ci
npm start
```

The service uses `youtubei.js` to resolve YouTube search results and short-lived audio stream URLs. The Android client still keeps its own Invidious fallback if the backend is temporarily unavailable.

## Endpoints

- `GET /health`
- `GET /api/health`
- `GET /api/search?q=<query>`
- `GET /api/song/<videoId>`
- `GET /api/stream/<videoId>?quality=High%20(320kbps)`
- `GET /api/lyrics/<videoId>`
- `GET /api/discover`

## Render

Use `backend` as the service root directory and `npm start` as the start command. `render.yaml` is included for deployment configuration.

`ALLOWED_ORIGINS` should contain the production web origin, for example `https://spotifusion.vercel.app`.

Stream URLs are intentionally returned rather than proxied through the backend. They are short-lived and the Android Media3 player consumes them directly.
