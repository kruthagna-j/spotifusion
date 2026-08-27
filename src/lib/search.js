import { auth } from '@/lib/firebase'

const API_BASE = (import.meta.env.VITE_MUSIC_API_URL || '').replace(/\/$/, '')
const CACHE_TTL = 10 * 60 * 1000
const MAX_CACHE = 256
const cache = new Map()
const inflight = new Map()

function keyFor(query, category, batch) {
  return `${query.trim().toLocaleLowerCase().replace(/\s+/g, ' ')}|${category}|${batch}`
}
function getCached(key) {
  const item = cache.get(key)
  if (!item) return null
  if (Date.now() - item.time >= CACHE_TTL) { cache.delete(key); return null }
  cache.delete(key); cache.set(key, item)
  return item.value
}
function setCached(key, value) {
  cache.delete(key); cache.set(key, { time: Date.now(), value })
  while (cache.size > MAX_CACHE) cache.delete(cache.keys().next().value)
}
async function authHeaders() {
  const user = auth.currentUser
  if (!user) return {}
  return { Authorization: `Bearer ${await user.getIdToken()}` }
}

export async function searchMusicPage(query, { category = 'all', batch = 1, signal } = {}) {
  const q = query.trim()
  if (q.length < 2) return { results: [], hasMore: false, batch, category }
  if (!auth.currentUser) throw new Error('Sign in to search and stream music.')
  if (!API_BASE) throw new Error('Music service is not configured.')
  const key = keyFor(q, category, batch)
  const cached = getCached(key)
  if (cached) return cached
  const existing = inflight.get(key)
  if (existing) return existing
  const promise = (async () => {
    const url = `${API_BASE}/api/search?q=${encodeURIComponent(q)}&category=${encodeURIComponent(category)}&batch=${batch}`
    let response
    try {
      response = await fetch(url, { headers: await authHeaders(), signal })
    } catch (err) {
      if (err?.name === 'AbortError') throw err
      throw new Error('Unable to reach the music service right now. Please try again.')
    }
    let body = null
    try { body = await response.json() } catch {}
    if (!response.ok) throw new Error(body?.detail || 'Music service request failed.')
    const result = {
      results: Array.isArray(body?.results) ? body.results : [],
      hasMore: Boolean(body?.hasMore),
      available: Number.isFinite(body?.available) ? body.available : undefined,
      batch: Number(body?.batch || batch),
      pageSize: Number(body?.pageSize || 100),
      category,
    }
    setCached(key, result)
    return result
  })().finally(() => inflight.delete(key))
  inflight.set(key, promise)
  return promise
}
