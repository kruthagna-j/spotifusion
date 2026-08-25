// Private Session: when active, playback is not recorded to "Recently
// Played" in Firestore. Deliberately NOT persisted to storage — matches
// real Spotify's behavior where a private session is temporary and resets
// next time you open the app, not a permanent account-wide setting.
let privateSession = false
const listeners = new Set()

export function isPrivateSession() {
  return privateSession
}

export function setPrivateSession(value) {
  privateSession = value
  listeners.forEach((fn) => fn(privateSession))
}

export function subscribePrivateSession(fn) {
  listeners.add(fn)
  return () => listeners.delete(fn)
}
