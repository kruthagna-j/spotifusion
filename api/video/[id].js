const YT_API_KEY = process.env.YOUTUBE_API_KEY
const YT_BASE = 'https://www.googleapis.com/youtube/v3'

async function ytFetch(path, params) {
  const url = new URL(`${YT_BASE}/${path}`)
  Object.entries({ ...params, key: YT_API_KEY }).forEach(([k, v]) => url.searchParams.set(k, v))
  const res = await fetch(url)
  if (!res.ok) throw new Error(`YouTube API ${path} failed: ${res.status}`)
  return res.json()
}

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
  if (!YT_API_KEY) return res.status(500).json({ error: 'YOUTUBE_API_KEY is not set.' })

  try {
    const { id } = req.query
    const data = await ytFetch('videos', { part: 'snippet,contentDetails', id })
    if (!data.items.length) return res.status(404).json({ error: 'Not found' })
    const video = data.items[0]
    const channelData = await ytFetch('channels', { part: 'snippet,statistics', id: video.snippet.channelId })
    res.status(200).json(toTrack(video, channelData.items[0]))
  } catch (err) {
    console.error(err)
    res.status(500).json({ error: 'Lookup failed' })
  }
}
