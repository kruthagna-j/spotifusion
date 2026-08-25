// Client for the free, key-less search backend (see /api/main.py), which
// uses ytmusicapi instead of the official (quota-limited) YouTube Data API.
//
// This backend is a standalone FastAPI + Redis service, NOT a Vercel
// serverless function (Vercel serverless doesn't fit the Redis-backed,
// horizontally-scalable design — see api/README.md) — so it needs its own
// URL, configured via VITE_MUSIC_API_URL.
import { auth } from '@/lib/firebase'

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

// The backend requires a signed-in Spotifusion account for search/song
// lookups (enforced server-side in api/auth.py) — this attaches the
// caller's Firebase ID token so those requests succeed.
async function authHeaders() {
  const user = auth.currentUser
  if (!user) return {}
  const token = await user.getIdToken()
  return { Authorization: `Bearer ${token}` }
}

const SEARCH_CACHE_TTL = 5 * 60 * 1000
const searchCache = new Map()

/**
 * Search for songs via the Spotifusion backend (ytmusicapi under the hood).
 * Returns an array of track objects already shaped for the existing player:
 * { id, title, artist, album, duration, durationSeconds, thumbnail, source }
 *
 * Requires a signed-in user (see auth.py) — throws if called while signed
 * out, rather than silently sending an unauthenticated request that the
 * server would reject anyway.
 *
 * Never throws for "no results" (resolves to []). Throws only for genuine
 * request failures, with a message safe to show directly to the user.
 */
export async function searchMusic(query, { signal } = {}) {
  const q = query.trim()
  if (!q) return []
  if (!auth.currentUser) {
    throw new Error('Sign in to search and stream music.')
  }

  const key = q.toLocaleLowerCase()
  const cached = searchCache.get(key)
  if (cached && Date.now() - cached.time < SEARCH_CACHE_TTL) {
    return cached.results
  }
  if (cached) searchCache.delete(key)

  let res
  try {
    res = await fetch(`${API_BASE}/api/search?q=${encodeURIComponent(q)}`, {
      headers: await authHeaders(),
      signal,
    })
  } catch (err) {
    if (err?.name === 'AbortError') throw err
    throw new Error('Unable to search right now. Please try again.')
  }

  const body = await parseJsonSafe(res)

  if (!res.ok) {
    throw new Error(body?.detail || 'Unable to search right now. Please try again.')
  }
  if (!body || !Array.isArray(body.results)) {
    throw new Error('Unable to search right now. Please try again.')
  }

  searchCache.set(key, { time: Date.now(), results: body.results })
  return body.results
}

/**
 * Look up a single track's metadata by its YouTube video id. Requires a
 * signed-in user, same as searchMusic.
 */
export async function getSong(videoId) {
  if (!videoId || !auth.currentUser) return null
  let res
  try {
    res = await fetch(`${API_BASE}/api/song/${encodeURIComponent(videoId)}`, {
      headers: await authHeaders(),
    })
  } catch {
    return null
  }
  if (!res.ok) return null
  return parseJsonSafe(res)
}

/**
 * Look up lyrics for a track by its YouTube video id. Requires a signed-in
 * user, same as the other endpoints. Resolves to { available: false } for
 * "no lyrics found" (a normal, common outcome — not an error) and only
 * throws for genuine request failures.
 */
export async function getLyrics(videoId) {
  if (!videoId) return { available: false }
  if (!auth.currentUser) {
    throw new Error('Sign in to see lyrics.')
  }
  let res
  try {
    res = await fetch(`${API_BASE}/api/lyrics/${encodeURIComponent(videoId)}`, {
      headers: await authHeaders(),
    })
  } catch {
    throw new Error('Unable to fetch lyrics right now. Please try again.')
  }
  const body = await parseJsonSafe(res)
  if (!res.ok) {
    throw new Error(body?.detail || 'Unable to fetch lyrics right now. Please try again.')
  }
  return body || { available: false }
}
