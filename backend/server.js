import express from "express";
import cors from "cors";

const app = express();
const PORT = Number(process.env.PORT || 10000);
const ALLOWED_ORIGINS = (process.env.ALLOWED_ORIGINS || "*").split(",").map((v) => v.trim()).filter(Boolean);

app.disable("x-powered-by");
app.use(cors({ origin: ALLOWED_ORIGINS.includes("*") ? true : ALLOWED_ORIGINS }));
app.use(express.json({ limit: "256kb" }));

app.get("/health", (_req, res) => {
  res.json({ ok: true, service: "spotifusion-backend", version: "1.0.0" });
});

app.get("/api/health", (_req, res) => {
  res.json({ ok: true, service: "spotifusion-backend", version: "1.0.0" });
});

app.get("/api/search", (_req, res) => {
  res.status(501).json({
    error: "SEARCH_PROVIDER_NOT_CONFIGURED",
    message: "The backend is online, but the YouTube search provider is not configured yet."
  });
});

app.get("/api/stream/:videoId", (_req, res) => {
  res.status(501).json({
    error: "STREAM_PROVIDER_NOT_CONFIGURED",
    message: "The backend is online, but the YouTube stream provider is not configured yet."
  });
});

app.get("/api/lyrics/:videoId", (_req, res) => {
  res.status(501).json({
    error: "LYRICS_PROVIDER_NOT_CONFIGURED",
    message: "The backend is online, but the lyrics provider is not configured yet."
  });
});

app.get("/api/discover", (_req, res) => {
  res.json({ tracks: [], playlists: [] });
});

app.use((_req, res) => {
  res.status(404).json({ error: "NOT_FOUND" });
});

app.listen(PORT, "0.0.0.0", () => {
  console.log(`Spotifusion backend listening on ${PORT}`);
});
