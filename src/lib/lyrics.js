const LRCLIB_API = 'https://lrclib.net/api/get'

export async function getLyrics({ title, artist, album, duration }) {
  if (!title || !artist) return null
  const params = new URLSearchParams({ track_name: title, artist_name: artist })
  if (album) params.set('album_name', album)
  if (Number.isFinite(duration) && duration > 0) params.set('duration', String(Math.round(duration)))

  const response = await fetch(`${LRCLIB_API}?${params.toString()}`, { cache: 'no-store' })
  if (response.status === 404) return null
  if (!response.ok) throw new Error(`Lyrics request failed (${response.status})`)
  const data = await response.json()
  if (!data?.plainLyrics && !data?.syncedLyrics) return null
  return { plainLyrics: data.plainLyrics || '', syncedLyrics: data.syncedLyrics || '' }
}

export function parseSyncedLyrics(lrc) {
  if (!lrc) return []
  return lrc.split(/\r?\n/).flatMap((line) => {
    const tags = [...line.matchAll(/\[(\d{1,3}):(\d{2})(?:\.(\d{1,3}))?\]/g)]
    const text = line.replace(/\[[^\]]+\]/g, '').trim()
    if (!text || !tags.length) return []
    return tags.map((match) => {
      const minutes = Number(match[1])
      const seconds = Number(match[2])
      const fraction = match[3] ? Number(`0.${match[3]}`) : 0
      return { time: minutes * 60 + seconds + fraction, text }
    })
  }).sort((a, b) => a.time - b.time)
}
