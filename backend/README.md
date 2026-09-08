# Spotifusion Backend

Production API service for the Android and web clients.

## Run

```bash
npm install
npm start
```

The service exposes `/health` and the API contract used by the clients. YouTube search/stream and lyrics providers are intentionally configured separately so provider credentials and deployment behavior are not hard-coded into the client.

## Render

Set the service root directory to `backend` and the start command to `npm start`. Configure the environment variables from `.env.example`.
