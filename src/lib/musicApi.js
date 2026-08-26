import { auth } from '@/lib/firebase'

const API_BASE = (import.meta.env.VITE_MUSIC_API_URL || '').replace(/\/$/, '')
if (import.meta.env.PROD && !API_BASE) console.error('[Spotifusion] VITE_MUSIC_API_URL is not set in this production build.')

async function parseJsonSafe(res) { try { return await res.json() } catch { return null } }
async function authHeaders() {
  const user = auth.currentUser
  if (!user) return {}
  return { Authorization: `Bearer ${await user.getIdToken()}` }
}

const SEARCH_CACHE_TTL = 10 * 60 * 1000
const ENTITY_CACHE_TTL = 30 * 60 * 1000
const DISCOVER_CACHE_TTL = 15 * 60 * 1000
const MAX_SEARCH_CACHE = 128
const MAX_ENTITY_CACHE = 256
const MAX_DISCOVER_CACHE = 8

const searchCache = new Map()
const songCache = new Map()
const lyricsCache = new Map()
const discoverCache = new Map()
const inflight = new Map()

function normalizedKey(v) { return v.trim().toLocaleLowerCase().replace(/\s+/g, ' ') }
function getCached(map, key, ttl) {
  const e = map.get(key)
  if (!e) return null
  if (Date.now() - e.time >= ttl) { map.delete(key); return null }
  map.delete(key); map.set(key, e)
  return e.value
}
function setCached(map, key, value, max) {
  map.delete(key)
  map.set(key, { time: Date.now(), value })
  while (map.size > max) map.delete(map.keys().next().value)
}
function coalesce(key, loader) {
  const existing = inflight.get(key)
  if (existing) return existing
  const promise = Promise.resolve().then(loader).finally(() => inflight.delete(key))
  inflight.set(key, promise)
  return promise
}

async function request(path, { signal } = {}) {
  if (!API_BASE) throw new Error('Music service is not configured.')
  let res
  try {
    res = await fetch(`${API_BASE}${path}`, { headers: await authHeaders(), signal })
  } catch (err) {
    if (err?.name === 'AbortError') throw err
    throw new Error('Unable to reach the music service right now. Please try again.')
  }
  const body = await parseJsonSafe(res)
  if (!res.ok) throw new Error(body?.detail || 'Music service request failed.')
  return body
}

export async function searchMusic(query, { signal } = {}) {
  const q = query.trim()
  if (q.length < 2) return []
  if (!auth.currentUser) throw new Error('Sign in to search and stream music.')
  const key = normalizedKey(q)
  const cached = getCached(searchCache, key, SEARCH_CACHE_TTL)
  if (cached) return cached
  const body = await coalesce(`search:${key}`, () => request(`/api/search?q=${encodeURIComponent(q)}`, { signal }))
  if (!body || !Array.isArray(body.results)) throw new Error('Unable to search right now. Please try again.')
  setCached(searchCache, key, body.results, MAX_SEARCH_CACHE)
  return body.results
}

export async function getSong(videoId) {
  if (!videoId || !auth.currentUser) return null
  const cached = getCached(songCache, videoId, ENTITY_CACHE_TTL)
  if (cached) return cached
  const body = await coalesce(`song:${videoId}`, () => request(`/api/song/${encodeURIComponent(videoId)}`))
  if (body) setCached(songCache, videoId, body, MAX_ENTITY_CACHE)
  return body
}

export async function getLyrics(videoId) {
  if (!videoId) return { available: false }
  if (!auth.currentUser) throw new Error('Sign in to see lyrics.')
  const cached = getCached(lyricsCache, videoId, ENTITY_CACHE_TTL)
  if (cached) return cached
  const body = await coalesce(`lyrics:${videoId}`, () => request(`/api/lyrics/${encodeURIComponent(videoId)}`))
  const result = body || { available: false }
  setCached(lyricsCache, videoId, result, MAX_ENTITY_CACHE)
  return result
}

export async function getDiscover({ signal } = {}) {
  if (!auth.currentUser) return { sections: [] }
  const key = 'global'
  const cached = getCached(discoverCache, key, DISCOVER_CACHE_TTL)
  if (cached) return cached
  const body = await coalesce('discover:global', () => request('/api/discover', { signal }))
  const result = body || { sections: [] }
  setCached(discoverCache, key, result, MAX_DISCOVER_CACHE)
  return result
}
