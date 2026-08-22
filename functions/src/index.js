/**
 * Spotifusion backend — proxies the YouTube Data API so the API key never
 * ships to the client, and applies a "trusted source" filter so search
 * results favor official artist channels / auto-generated "- Topic"
 * channels over random uploads, covers, and reaction videos.
 *
 * Deploy: firebase deploy --only functions
 * Requires: firebase functions:config:set youtube.key="YOUR_YT_DATA_API_KEY"
 *           (or, for Functions v2, set YOUTUBE_API_KEY as an env/secret)
 */
const functions = require('firebase-functions')
const admin = require('firebase-admin')
const express = require('express')
const cors = require('cors')

admin.initializeApp()
const app = express()
app.use(cors({ origin: true }))

const YT_API_KEY = process.env.YOUTUBE_API_KEY || functions.config()?.youtube?.key
const YT_BASE = 'https://www.googleapis.com/youtube/v3'

if (!YT_API_KEY) {
  console.warn('YOUTUBE_API_KEY is not set. Search requests will fail until it is configured.')
}

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

// Heuristic trust score. YouTube's public API does not expose the verified
// badge, so we approximate "official/trusted" using signals that correlate
// with official releases: auto-generated "- Topic" channels (created by
// YouTube for every artist on YouTube Music), VEVO, and subscriber count.
function scoreChannel(channel) {
  const title = (channel?.snippet?.title || '').toLowerCase()
  const subs = Number(channel?.statistics?.subscriberCount || 0)
  let score = 0
  if (title.endsWith('- topic')) score += 100 // canonical official-audio channel
  if (title.includes('vevo')) score += 90
  if (channel?.snippet?.customUrl) score += 5
  if (subs >= 1_000_000) score += 40
  else if (subs >= 100_000) score += 20
  else if (subs >= 10_000) score += 5
  return score
}

function toTrack(video, channel) {
  const sn = video.snippet
  return {
    id: video.id,
    title: sn.title,
    artist: sn.channelTitle.replace(/\s*-\s*topic$/i, ''),
    channelId: sn.channelId,
    thumbnail: sn.thumbnails?.high?.url || sn.thumbnails?.default?.url,
    publishedAt: sn.publishedAt,
    duration: video.contentDetails?.duration || null, // ISO 8601, e.g. PT3M45S
    trustScore: scoreChannel(channel),
    trusted: scoreChannel(channel) >= 20,
  }
}

// GET /search?q=...&trustedOnly=true
app.get('/search', async (req, res) => {
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

    const [videosData, channelIds] = await Promise.all([
      ytFetch('videos', { part: 'snippet,contentDetails', id: videoIds.join(',') }),
      Promise.resolve([...new Set(searchData.items.map((it) => it.snippet.channelId))]),
    ])

    const channelsData = await ytFetch('channels', {
      part: 'snippet,statistics',
      id: channelIds.join(','),
    })
    const channelsById = Object.fromEntries(channelsData.items.map((c) => [c.id, c]))

    let tracks = videosData.items.map((v) => toTrack(v, channelsById[v.snippet.channelId]))
    tracks.sort((a, b) => b.trustScore - a.trustScore)

    if (trustedOnly) {
      const filtered = tracks.filter((t) => t.trusted)
      tracks = filtered.length ? filtered : tracks // don't return empty results for niche/indie tracks
    }

    res.json({ tracks })
  } catch (err) {
    console.error(err)
    res.status(500).json({ error: 'Search failed' })
  }
})

// GET /video/:id  — single-track lookup, used to refresh metadata / duration
app.get('/video/:id', async (req, res) => {
  try {
    const data = await ytFetch('videos', {
      part: 'snippet,contentDetails',
      id: req.params.id,
    })
    if (!data.items.length) return res.status(404).json({ error: 'Not found' })
    const video = data.items[0]
    const channelData = await ytFetch('channels', {
      part: 'snippet,statistics',
      id: video.snippet.channelId,
    })
    res.json(toTrack(video, channelData.items[0]))
  } catch (err) {
    console.error(err)
    res.status(500).json({ error: 'Lookup failed' })
  }
})

exports.api = functions.https.onRequest(app)
