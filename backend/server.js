import express from "express";
import cors from "cors";
import { Innertube } from "youtubei.js";

const app = express();
const PORT = Number(process.env.PORT || 10000);
const ALLOWED_ORIGINS = (process.env.ALLOWED_ORIGINS || "*").split(",").map((v) => v.trim()).filter(Boolean);
const CACHE_TTL_MS = 5 * 60 * 1000;

app.disable("x-powered-by");
app.use(cors({ origin: ALLOWED_ORIGINS.includes("*") ? true : ALLOWED_ORIGINS }));
app.use(express.json({ limit: "256kb" }));

let youtubePromise;
const streamCache = new Map();

async function getYouTube() {
  if (!youtubePromise) {
    youtubePromise = Innertube.create({ lang: "en", location: "US" }).catch((error) => {
      youtubePromise = undefined;
      throw error;
    });
  }
  return youtubePromise;
}

function cleanText(value) {
  if (value == null) return "";
  if (typeof value === "string") return value;
  if (typeof value.toString === "function") return value.toString();
  return "";
}

function thumbnail(video) {
  const thumbs = video?.thumbnails || [];
  return thumbs.length ? thumbs[thumbs.length - 1]?.url || "" : "";
}

function mapSearchItem(item) {
  if (!item || item.type !== "Video") return null;
  return {
    id: item.id,
    videoId: item.id,
    title: cleanText(item.title),
    artist: cleanText(item.author?.name || item.author?.toString?.() || "Unknown Artist"),
    album: "Single",
    durationSeconds: Number(item.duration?.seconds || 0),
    thumbnail: thumbnail(item),
    genre: "Music"
  };
}

async function searchYouTube(query, limit = 20) {
  const yt = await getYouTube();
  const result = await yt.search(query, { type: "video", sort_by: "relevance" });
  return (result.items || []).map(mapSearchItem).filter(Boolean).slice(0, limit);
}

async function resolveStream(videoId, quality = "High (320kbps)") {
  const cached = streamCache.get(`${videoId}:${quality}`);
  if (cached && cached.expiresAt > Date.now()) return cached.value;

  const yt = await getYouTube();
  const info = await yt.getBasicInfo(videoId);
  const targetBitrate = quality.startsWith("Normal") ? 160000 : quality.startsWith("Maximum") ? 512000 : 320000;
  const format = info.chooseFormat({ type: "audio", quality: "best" });
  if (!format) throw new Error("No audio format available");
  const url = await format.decipher(yt.session.player);
  const value = {
    videoId,
    url,
    mimeType: format.mime_type || "audio/webm",
    bitrate: Number(format.bitrate || targetBitrate),
    durationSeconds: Number(info.basic_info?.duration || 0),
    title: cleanText(info.basic_info?.title),
    artist: cleanText(info.basic_info?.author?.name || info.basic_info?.channel?.name || "Unknown Artist"),
    thumbnail: thumbnail(info.basic_info)
  };
  streamCache.set(`${videoId}:${quality}`, { value, expiresAt: Date.now() + CACHE_TTL_MS });
  return value;
}

app.get("/health", (_req, res) => {
  res.json({ ok: true, service: "spotifusion-backend", version: "1.1.0", youtube: Boolean(youtubePromise) });
});

app.get("/api/health", (_req, res) => {
  res.json({ ok: true, service: "spotifusion-backend", version: "1.1.0", youtube: Boolean(youtubePromise) });
});

app.get("/api/search", async (req, res) => {
  const query = String(req.query.query || req.query.q || "").trim();
  const limit = Math.min(Math.max(Number(req.query.limit || 20), 1), 50);
  if (query.length < 2) return res.status(400).json({ error: "INVALID_QUERY", message: "Search query must contain at least 2 characters." });
  try {
    const results = await searchYouTube(query, limit);
    res.json({ results, query });
  } catch (error) {
    console.error("search", error);
    res.status(502).json({ error: "SEARCH_FAILED", message: "YouTube search is temporarily unavailable." });
  }
});

app.get("/api/stream/:videoId", async (req, res) => {
  const videoId = String(req.params.videoId || "").trim();
  const quality = String(req.query.quality || "High (320kbps)");
  if (!/^[A-Za-z0-9_-]{11}$/.test(videoId)) return res.status(400).json({ error: "INVALID_VIDEO_ID" });
  try {
    res.json(await resolveStream(videoId, quality));
  } catch (error) {
    console.error("stream", videoId, error);
    res.status(502).json({ error: "STREAM_FAILED", message: "Unable to resolve an audio stream for this track." });
  }
});

app.get("/api/song/:videoId", async (req, res) => {
  const videoId = String(req.params.videoId || "").trim();
  if (!/^[A-Za-z0-9_-]{11}$/.test(videoId)) return res.status(400).json({ error: "INVALID_VIDEO_ID" });
  try {
    const yt = await getYouTube();
    const info = await yt.getBasicInfo(videoId);
    res.json({
      id: videoId,
      videoId,
      title: cleanText(info.basic_info?.title),
      artist: cleanText(info.basic_info?.author?.name || info.basic_info?.channel?.name || "Unknown Artist"),
      durationSeconds: Number(info.basic_info?.duration || 0),
      thumbnail: thumbnail(info.basic_info)
    });
  } catch (error) {
    console.error("song", videoId, error);
    res.status(502).json({ error: "SONG_LOOKUP_FAILED" });
  }
});

app.get("/api/lyrics/:videoId", (_req, res) => {
  // Lyrics are optional metadata; the Android client already supports track lyrics.
  res.json({ lyrics: [], syncedLyrics: [] });
});

app.get("/api/discover", async (_req, res) => {
  try {
    const results = await searchYouTube("new music 2026", 20);
    res.json({ sections: [{ title: "Discover", tracks: results }] });
  } catch (error) {
    console.error("discover", error);
    res.json({ sections: [] });
  }
});

app.use((_req, res) => res.status(404).json({ error: "NOT_FOUND" }));

app.listen(PORT, "0.0.0.0", () => console.log(`Spotifusion backend listening on ${PORT}`));
