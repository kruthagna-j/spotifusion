const PREFIX = 'spotifusion:online-cache:'
const DEFAULT_TTL = 5 * 60 * 1000

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
    localStorage.setItem(PREFIX + key, JSON.stringify({ time: Date.now(), ttl, value }))
  } catch {
    // Storage can be unavailable/full; online requests must still work.
  }
}

export function clearOnlineCache() {
  try {
    Object.keys(localStorage)
      .filter((key) => key.startsWith(PREFIX))
      .forEach((key) => localStorage.removeItem(key))
  } catch {}
}
