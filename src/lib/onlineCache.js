const PREFIX = 'spotifusion:online-cache:'
const DEFAULT_TTL = 5 * 60 * 1000
const MAX_ENTRIES = 40

function removeExpiredAndOldest() {
  const now = Date.now()
  const entries = []
  Object.keys(localStorage).forEach((key) => {
    if (!key.startsWith(PREFIX)) return
    try {
      const entry = JSON.parse(localStorage.getItem(key))
      if (!entry || now - entry.time > (entry.ttl || DEFAULT_TTL)) {
        localStorage.removeItem(key)
        return
      }
      entries.push({ key, time: entry.time })
    } catch {
      localStorage.removeItem(key)
    }
  })
  entries.sort((a, b) => a.time - b.time)
  while (entries.length >= MAX_ENTRIES) {
    const oldest = entries.shift()
    if (oldest) localStorage.removeItem(oldest.key)
  }
}

export function getCached(key) {
  try {
    const raw = localStorage.getItem(PREFIX + key)
    if (!raw) return null
    const entry = JSON.parse(raw)
    if (!entry || Date.now() - entry.time > (entry.ttl || DEFAULT_TTL)) {
      localStorage.removeItem(PREFIX + key)
      return null
    }
    return entry.value
  } catch {
    return null
  }
}

export function setCached(key, value, ttl = DEFAULT_TTL) {
  try {
    removeExpiredAndOldest()
    localStorage.setItem(PREFIX + key, JSON.stringify({ time: Date.now(), ttl, value }))
  } catch {
    // Storage can be unavailable/full. Evict our own cache and retry once;
    // online requests must never fail just because browser storage is full.
    try {
      removeExpiredAndOldest()
      localStorage.setItem(PREFIX + key, JSON.stringify({ time: Date.now(), ttl, value }))
    } catch {
      // Best effort only.
    }
  }
}

export function clearOnlineCache() {
  try {
    Object.keys(localStorage)
      .filter((key) => key.startsWith(PREFIX))
      .forEach((key) => localStorage.removeItem(key))
  } catch {}
}
