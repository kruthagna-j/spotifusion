// Client for the free, key-less search backend (see /api/main.py), which
// uses ytmusicapi instead of the official (quota-limited) YouTube Data API.
//
// This backend is a standalone FastAPI + Redis service, NOT a Vercel
// serverless function (Vercel serverless doesn't fit the Redis-backed,
// horizontally-scalable design — see api/README.md) — so it needs its own
// URL. Deliberately does NOT default to same-origin '/api', since that path
// is already used by the existing Vercel function in api/search.js; the two
// must not collide.
const API_BASE = import.meta.env.VITE_MUSIC_API_URL || 'https://spotifusion.onrender.com'

if (import.meta.env.PROD && !import.meta.env.VITE_MUSIC_API_URL) {
  // Loud, not silent: a deployed build with no configured backend would
  // otherwise fail every search with an opaque network error, and someone
  // debugging it wouldn't know why. localhost is a dev-only fallback.
  console.error(
    '[Spotifusion] VITE_MUSIC_API_URL is not set in this production build — ' +
      'search will try to reach https://spotifusion.onrender.com, which does not exist ' +
      "for anyone but a developer's own machine. Set VITE_MUSIC_API_URL to " +
      'your deployed backend URL in your hosting provider\'s environment variables.'
  )
}

async function parseJsonSafe(res) {
  try {
    return await res.json()
  } catch {
    return null
  }
}

/**
 * Search for songs via the Spotifusion backend (ytmusicapi under the hood).
 * Returns an array of track objects already shaped for the existing player:
 * { id, title, artist, album, duration, durationSeconds, thumbnail, source }
 *
 * Never throws for "no results" (resolves to []). Throws only for genuine
 * request failures, with a message safe to show directly to the user.
 */
export async function searchMusic(query) {
  const q = query.trim()
  if (!q) return []

  let res
  try {
    res = await fetch(`${API_BASE}/api/search?q=${encodeURIComponent(q)}`)
  } catch {
    throw new Error('Unable to search right now. Please try again.')
  }

  const body = await parseJsonSafe(res)

  if (!res.ok) {
    throw new Error(body?.detail || 'Unable to search right now. Please try again.')
  }
  if (!body || !Array.isArray(body.results)) {
    throw new Error('Unable to search right now. Please try again.')
  }
  return body.results
}

/**
 * Look up a single track's metadata by its YouTube video id.
 */
export async function getSong(videoId) {
  if (!videoId) return null
  let res
  try {
    res = await fetch(`${API_BASE}/api/song/${encodeURIComponent(videoId)}`)
  } catch {
    return null
  }
  if (!res.ok) return null
  return parseJsonSafe(res)
}
