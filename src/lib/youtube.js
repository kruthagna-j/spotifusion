// Client for the /search backend (see functions/src/index.js). Never call
// the YouTube Data API directly from the browser — the key must stay server-side.
const BASE = import.meta.env.VITE_SEARCH_API_BASE

export async function searchTracks(query, { trustedOnly = true } = {}) {
  if (!query.trim()) return []
  const url = new URL(`${BASE}/search`)
  url.searchParams.set('q', query)
  url.searchParams.set('trustedOnly', String(trustedOnly))
  const res = await fetch(url)
  if (!res.ok) throw new Error('Search request failed')
  const data = await res.json()
  return data.tracks
}

// Parses ISO 8601 durations like "PT3M45S" -> seconds
export function parseDuration(iso) {
  if (!iso) return 0
  const m = iso.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/)
  if (!m) return 0
  const [, h, min, s] = m
  return (Number(h) || 0) * 3600 + (Number(min) || 0) * 60 + (Number(s) || 0)
}

export function formatTime(totalSeconds) {
  if (!Number.isFinite(totalSeconds)) return '0:00'
  const m = Math.floor(totalSeconds / 60)
  const s = Math.floor(totalSeconds % 60)
  return `${m}:${String(s).padStart(2, '0')}`
}
