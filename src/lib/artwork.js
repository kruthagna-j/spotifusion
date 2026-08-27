const cache = new Map()

function validUrl(value) {
  return typeof value === 'string' && /^https?:\/\//i.test(value.trim())
}

function upgradeYouTubeThumbnail(url, width = 1400, height = 1400) {
  if (!validUrl(url)) return null
  const clean = url.trim()

  // YouTube video thumbnails: use the clean max-resolution endpoint.
  const videoMatch = clean.match(/(?:i\.)?ytimg\.com\/vi\/([^/]+)\//i)
  if (videoMatch?.[1]) {
    return `https://i.ytimg.com/vi/${videoMatch[1]}/maxresdefault.jpg`
  }

  // Google/YouTube image CDN: remove old resize descriptors and request a
  // large square. This prevents low-resolution 120px artwork from being
  // stretched in the player.
  if (/ggpht\.com|googleusercontent\.com/i.test(clean)) {
    const withoutSize = clean.replace(/=w\d+(?:-h\d+)?(?:-[^?&]+)?/i, '')
    const separator = withoutSize.includes('?') ? '&' : '?'
    return `${withoutSize}${separator}w=${width}&h=${height}`
  }

  return clean
}

export function getArtwork(track, size = 'large') {
  if (!track) return null
  const candidates = [
    track?.artwork?.[size],
    track?.artwork?.large,
    track?.artwork?.medium,
    track?.thumbnail,
    track?.thumbnailUrl,
    track?.image,
  ]
  const base = candidates.find(validUrl)
  if (!base) return null

  const width = size === 'small' ? 320 : size === 'medium' ? 800 : 1600
  const key = `${base}|${width}`
  if (cache.has(key)) return cache.get(key)

  const value = upgradeYouTubeThumbnail(base, width, width)
  cache.set(key, value)
  return value
}

export function artworkSrcSet() {
  // Disabled intentionally. Some YouTube/Google thumbnail URLs contain
  // provider-specific descriptors that browsers reject in srcset.
  // Spotifusion now uses one validated high-resolution URL instead.
  return undefined
}
