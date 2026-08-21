// Free Vercel Serverless Function — replaces functions/src/index.js so no
// Firebase Blaze billing plan is required anywhere in this project.
// Deployed automatically by Vercel at /api/search whenever you push.
// Requires one env var in the Vercel project (Settings -> Environment
// Variables): YOUTUBE_API_KEY — a free key from Google Cloud Console
// (APIs & Services -> Library -> YouTube Data API v3 -> Enable -> Credentials).

const YT_API_KEY = process.env.YOUTUBE_API_KEY
const YT_BASE = 'https://www.googleapis.com/youtube/v3'

async function ytFetch(path, params) {
  const url = new URL(`${YT_BASE}/${path}`)
  Object.entries({ ...params, key: YT_API_KEY }).forEach(([k, v]) => url.searchParams.set(k, v))
  const res = await fetch(url)
  if (!res.ok) {
    const body = await res.text()
    throw new Error(`YouTube API ${path} failed: ${res.status} ${body}`)
  }
  return res.json()
}

// Same heuristic as before: YouTube's public API doesn't expose a verified
// badge, so official-ness is approximated from auto-generated "- Topic"
// channels, VEVO, and subscriber count.
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

export default async function handler(req, res) {
  res.setHeader('Access-Control-Allow-Origin', '*')
  if (req.method === 'OPTIONS') return res.status(204).end()

  if (!YT_API_KEY) {
    return res.status(500).json({
      error: 'YOUTUBE_API_KEY is not set in this Vercel project\'s environment variables.',
    })
  }

  try {
    const q = String(req.query.q || '').trim()
    if (!q) return res.status(400).json({ error: 'Missing q param' })
    const trustedOnly = req.query.trustedOnly !== 'false' // default: on

    const searchData = await ytFetch('search', {
      part: 'snippet',
      q,
      type: 'video',
      videoCategoryId: '10', // Music
      maxResults: '25',
      safeSearch: 'moderate',
    })

    const videoIds = searchData.items.map((it) => it.id.videoId).filter(Boolean)
    if (!videoIds.length) return res.json({ tracks: [] })

    const channelIds = [...new Set(searchData.items.map((it) => it.snippet.channelId))]
    const [videosData, channelsData] = await Promise.all([
      ytFetch('videos', { part: 'snippet,contentDetails', id: videoIds.join(',') }),
      ytFetch('channels', { part: 'snippet,statistics', id: channelIds.join(',') }),
    ])
    const channelsById = Object.fromEntries(channelsData.items.map((c) => [c.id, c]))

    let tracks = videosData.items.map((v) => toTrack(v, channelsById[v.snippet.channelId]))
    tracks.sort((a, b) => b.trustScore - a.trustScore)

    if (trustedOnly) {
      const filtered = tracks.filter((t) => t.trusted)
      tracks = filtered.length ? filtered : tracks
    }

    res.status(200).json({ tracks })
  } catch (err) {
    console.error(err)
    res.status(500).json({ error: 'Search failed' })
  }
}
