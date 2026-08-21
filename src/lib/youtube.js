// Client for the /search backend (see functions/src/index.js). Used when the
// signed-in user hasn't supplied their own YouTube API key.
const BASE = import.meta.env.VITE_SEARCH_API_BASE
const YT_DIRECT_BASE = 'https://www.googleapis.com/youtube/v3'

export async function searchTracks(query, { trustedOnly = true, apiKey = null } = {}) {
  if (!query.trim()) return []
  if (apiKey) return searchTracksDirect(query, apiKey, trustedOnly)

  const url = new URL(`${BASE}/search`)
  url.searchParams.set('q', query)
  url.searchParams.set('trustedOnly', String(trustedOnly))
  const res = await fetch(url)
  if (!res.ok) throw new Error('Search request failed')
  const data = await res.json()
  return data.tracks
}

// Same "trusted official channel" heuristic as functions/src/index.js,
// but run entirely in the browser against the user's own YouTube Data API
// key — their own free quota, no shared backend needed.
function scoreChannel(channel) {
  const title = (channel?.snippet?.title || '').toLowerCase()
  const subs = Number(channel?.statistics?.subscriberCount || 0)
  let score = 0
  if (title.endsWith('- topic')) score += 100
  if (title.includes('vevo')) score += 90
  if (channel?.snippet?.customUrl) score += 5
  if (subs >= 1_000_000) score += 40
  else if (subs >= 100_000) score += 20
  else if (subs >= 10_000) score += 5
  return score
}

function toTrack(video, channel) {
  const sn = video.snippet
  const score = scoreChannel(channel)
  return {
    id: video.id,
    title: sn.title,
    artist: sn.channelTitle.replace(/\s*-\s*topic$/i, ''),
    channelId: sn.channelId,
    thumbnail: sn.thumbnails?.high?.url || sn.thumbnails?.default?.url,
    publishedAt: sn.publishedAt,
    duration: video.contentDetails?.duration || null,
    trustScore: score,
    trusted: score >= 20,
  }
}

async function ytDirectFetch(apiKey, path, params) {
  const url = new URL(`${YT_DIRECT_BASE}/${path}`)
  Object.entries({ ...params, key: apiKey }).forEach(([k, v]) => url.searchParams.set(k, v))
  const res = await fetch(url)
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    const reason = body?.error?.message || `HTTP ${res.status}`
    throw new Error(`YouTube API error: ${reason}`)
  }
  return res.json()
}

async function searchTracksDirect(q, apiKey, trustedOnly) {
  const searchData = await ytDirectFetch(apiKey, 'search', {
    part: 'snippet',
    q,
    type: 'video',
    videoCategoryId: '10',
    maxResults: '25',
    safeSearch: 'moderate',
  })

  const videoIds = searchData.items.map((it) => it.id.videoId).filter(Boolean)
  if (!videoIds.length) return []

  const channelIds = [...new Set(searchData.items.map((it) => it.snippet.channelId))]
  const [videosData, channelsData] = await Promise.all([
    ytDirectFetch(apiKey, 'videos', { part: 'snippet,contentDetails', id: videoIds.join(',') }),
    ytDirectFetch(apiKey, 'channels', { part: 'snippet,statistics', id: channelIds.join(',') }),
  ])
  const channelsById = Object.fromEntries(channelsData.items.map((c) => [c.id, c]))

  let tracks = videosData.items.map((v) => toTrack(v, channelsById[v.snippet.channelId]))
  tracks.sort((a, b) => b.trustScore - a.trustScore)

  if (trustedOnly) {
    const filtered = tracks.filter((t) => t.trusted)
    tracks = filtered.length ? filtered : tracks
  }
  return tracks
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
