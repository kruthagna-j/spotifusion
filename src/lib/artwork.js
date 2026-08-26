const cache = new Map()

function upgradeYouTubeThumbnail(url, width, height = width) {
  if (!url || typeof url !== 'string') return url || null
  if (!/ytimg\.com|ggpht\.com/.test(url)) return url
  if (/i\.ytimg\.com\/vi\/[^/]+\/(?:default|mqdefault|hqdefault|sddefault)\.jpg/.test(url)) {
    return url.replace(/\/(?:default|mqdefault|hqdefault|sddefault)\.jpg$/, '/maxresdefault.jpg')
  }

  try {
    const parsed = new URL(url)
    if (parsed.searchParams.has('w')) parsed.searchParams.set('w', String(width))
    if (parsed.searchParams.has('h')) parsed.searchParams.set('h', String(height))
    if (parsed.searchParams.has('w') || parsed.searchParams.has('h')) return parsed.toString()
    return url.replace(/=w\d+-h\d+[^&]*/, `=w${width}-h${height}-l90-rj`)
  } catch {
    return url
  }
}

export function getArtwork(track, size = 'large') {
  if (!track) return null
  const base = track.artwork?.[size] || track.artwork?.large || track.thumbnail || track.thumbnailUrl
  if (!base) return null

  const width = size === 'small' ? 160 : size === 'medium' ? 640 : 1400
  const key = `${base}|${width}`
  if (cache.has(key)) return cache.get(key)

  const value = upgradeYouTubeThumbnail(base, width, width)
  cache.set(key, value)
  return value
}

export function artworkSrcSet(track) {
  const small = getArtwork(track, 'small')
  const medium = getArtwork(track, 'medium')
  const large = getArtwork(track, 'large')
  return [small && `${small} 320w`, medium && `${medium} 640w`, large && `${large} 1400w`].filter(Boolean).join(', ')
}
