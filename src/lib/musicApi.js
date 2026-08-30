import { auth } from '@/lib/firebase'

const CONFIGURED_API_BASE = (import.meta.env.VITE_MUSIC_API_URL || '').replace(/\/$/, '')
const SAME_ORIGIN_API_BASE = typeof window !== 'undefined' ? window.location.origin : ''
const API_BASES = CONFIGURED_API_BASE ? [CONFIGURED_API_BASE] : [SAME_ORIGIN_API_BASE].filter(Boolean)
if (import.meta.env.PROD && !CONFIGURED_API_BASE) console.warn('[Spotifusion] VITE_MUSIC_API_URL is not set; using same-origin API fallback.')

async function parseJsonSafe(res) { try { return await res.json() } catch { return null } }
async function authHeaders(forceRefresh = false) {
  const user = auth.currentUser
  if (!user) return {}
  return { Authorization: `Bearer ${await user.getIdToken(forceRefresh)}` }
}

const SEARCH_CACHE_TTL = 10 * 60 * 1000
const ENTITY_CACHE_TTL = 30 * 60 * 1000
const DISCOVER_CACHE_TTL = 15 * 60 * 1000
const MAX_SEARCH_CACHE = 128
const MAX_ENTITY_CACHE = 256
const MAX_DISCOVER_CACHE = 8
const TRANSIENT_STATUS = new Set([408, 425, 429, 500, 502, 503, 504])
const REQUEST_TIMEOUT_MS = 20000

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
  map.delete(key); map.set(key, { time: Date.now(), value })
  while (map.size > max) map.delete(map.keys().next().value)
}
function coalesce(key, loader) {
  const existing = inflight.get(key)
  if (existing) return existing
  const promise = Promise.resolve().then(loader).finally(() => inflight.delete(key))
  inflight.set(key, promise)
  return promise
}
function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)) }

async function fetchWithTimeout(url, options = {}, timeoutMs = REQUEST_TIMEOUT_MS) {
  const controller = new AbortController()
  const sourceSignal = options.signal
  if (sourceSignal?.aborted) throw new DOMException('Request aborted', 'AbortError')
  const onAbort = () => controller.abort()
  sourceSignal?.addEventListener('abort', onAbort, { once: true })
  const timer = window.setTimeout(() => controller.abort(), timeoutMs)
  try {
    return await fetch(url, { ...options, signal: controller.signal })
  } catch (err) {
    if (sourceSignal?.aborted) throw new DOMException('Request aborted', 'AbortError')
    if (controller.signal.aborted) throw new Error('Music service request timed out.')
    throw err
  } finally {
    window.clearTimeout(timer)
    sourceSignal?.removeEventListener('abort', onAbort)
  }
}

async function request(path, { signal } = {}) {
  if (!API_BASES.length) throw new Error('Music service is not configured.')
  let lastError = null
  let refreshedToken = false

  for (let attempt = 0; attempt < 4; attempt++) {
    for (const base of API_BASES) {
      try {
        const response = await fetchWithTimeout(`${base}${path}`, {
          headers: await authHeaders(refreshedToken),
          signal,
          cache: 'no-store',
        })
        const body = await parseJsonSafe(response)
        if (response.ok) return body

        if (response.status === 401 && !refreshedToken && auth.currentUser) {
          refreshedToken = true
          break
        }
        lastError = new Error(body?.detail || `Music service request failed (${response.status}).`)
        if (!TRANSIENT_STATUS.has(response.status)) throw lastError
      } catch (err) {
        if (err?.name === 'AbortError') throw err
        lastError = err
      }
    }
    if (refreshedToken && attempt === 0) continue
    if (attempt < 3) await sleep(500 * 2 ** attempt)
  }
  throw lastError || new Error('Unable to reach the music service right now. Please try again.')
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

export async function searchMusicPage(query, { category = 'all', batch = 1, signal } = {}) {
  const q = query.trim()
  if (q.length < 2) return { results: [], hasMore: false, batch, category }
  if (!auth.currentUser) throw new Error('Sign in to search and stream music.')
  const cacheKey = `page:${category}:${batch}:${normalizedKey(q)}`
  const cached = getCached(searchCache, cacheKey, SEARCH_CACHE_TTL)
  if (cached) return cached
  const body = await coalesce(`search-page:${cacheKey}`, () => request(`/api/search?q=${encodeURIComponent(q)}&category=${encodeURIComponent(category)}&batch=${batch}`, { signal }))
  const result = {
    results: Array.isArray(body?.results) ? body.results : [],
    hasMore: Boolean(body?.hasMore),
    available: Number.isFinite(body?.available) ? body.available : undefined,
    batch: Number(body?.batch || batch),
    pageSize: Number(body?.pageSize || 100),
    category,
  }
  setCached(searchCache, cacheKey, result, MAX_SEARCH_CACHE)
  return result
}

export async function getArtist(artistId) {
  if (!artistId || !auth.currentUser) return null
  const key = `artist:${artistId}`
  const cached = getCached(songCache, key, ENTITY_CACHE_TTL)
  if (cached) return cached
  const body = await coalesce(key, () => request(`/api/artist/${encodeURIComponent(artistId)}`))
  if (body) setCached(songCache, key, body, MAX_ENTITY_CACHE)
  return body
}

export async function getAlbum(albumId) {
  if (!albumId || !auth.currentUser) return null
  const key = `album:${albumId}`
  const cached = getCached(songCache, key, ENTITY_CACHE_TTL)
  if (cached) return cached
  const body = await coalesce(key, () => request(`/api/album/${encodeURIComponent(albumId)}`))
  if (body) setCached(songCache, key, body, MAX_ENTITY_CACHE)
  return body
}

export async function getPlaylist(playlistId) {
  if (!playlistId || !auth.currentUser) return null
  const key = `playlist:${playlistId}`
  const cached = getCached(songCache, key, ENTITY_CACHE_TTL)
  if (cached) return cached
  const body = await coalesce(key, () => request(`/api/playlist/${encodeURIComponent(playlistId)}`))
  if (body) setCached(songCache, key, body, MAX_ENTITY_CACHE)
  return body
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

export async function warmMusicService() {
  if (!API_BASES.length) return false
  for (const base of API_BASES) {
    try {
      const response = await fetchWithTimeout(`${base}/health`, { cache: 'no-store' }, 10000)
      if (response.ok) return true
    } catch {}
  }
  return false
}
