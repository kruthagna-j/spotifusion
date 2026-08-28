const CLIENT_ID = import.meta.env.VITE_SPOTIFY_CLIENT_ID || ''
const TOKEN_KEY = 'spotifusion:spotify-token:v1'
const VERIFIER_KEY = 'spotifusion:spotify-pkce-verifier'
const STATE_KEY = 'spotifusion:spotify-oauth-state'
const REDIRECT_URI = `${window.location.origin}/spotify-callback`

export const SPOTIFY_SCOPES = [
  'user-read-private',
  'user-read-email',
  'user-read-playback-state',
  'user-modify-playback-state',
  'playlist-read-private',
  'playlist-read-collaborative',
  'user-library-read',
  'streaming',
].join(' ')

function base64Url(bytes) {
  let binary = ''
  bytes.forEach((b) => { binary += String.fromCharCode(b) })
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function randomString(length = 64) {
  const bytes = new Uint8Array(length)
  crypto.getRandomValues(bytes)
  return base64Url(bytes)
}

async function sha256(value) {
  return new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(value)))
}

export function spotifyConfigured() {
  return Boolean(CLIENT_ID)
}

export function getSpotifyToken() {
  try {
    const token = JSON.parse(localStorage.getItem(TOKEN_KEY) || 'null')
    if (!token?.access_token) return null
    if (token.expires_at && Date.now() > token.expires_at - 60_000) return null
    return token
  } catch { return null }
}

export function clearSpotifyToken() {
  localStorage.removeItem(TOKEN_KEY)
}

export async function startSpotifyAuth() {
  if (!CLIENT_ID) throw new Error('Spotify Connect is not configured. Add VITE_SPOTIFY_CLIENT_ID in Vercel environment variables.')
  const verifier = randomString(64)
  const challenge = base64Url(await sha256(verifier))
  const state = randomString(32)
  localStorage.setItem(VERIFIER_KEY, verifier)
  localStorage.setItem(STATE_KEY, state)

  const url = new URL('https://accounts.spotify.com/authorize')
  url.search = new URLSearchParams({
    client_id: CLIENT_ID,
    response_type: 'code',
    redirect_uri: REDIRECT_URI,
    scope: SPOTIFY_SCOPES,
    state,
    code_challenge_method: 'S256',
    code_challenge: challenge,
  }).toString()
  window.location.assign(url.toString())
}

export async function finishSpotifyAuth(search) {
  const params = new URLSearchParams(search)
  const error = params.get('error')
  if (error) throw new Error(`Spotify authorization failed: ${error}`)
  const code = params.get('code')
  const state = params.get('state')
  const expectedState = localStorage.getItem(STATE_KEY)
  const verifier = localStorage.getItem(VERIFIER_KEY)
  if (!code || !state || !expectedState || state !== expectedState || !verifier) {
    throw new Error('Spotify sign-in session expired or was invalid. Please connect again.')
  }

  const body = new URLSearchParams({
    grant_type: 'authorization_code',
    code,
    redirect_uri: REDIRECT_URI,
    client_id: CLIENT_ID,
    code_verifier: verifier,
  })
  const response = await fetch('https://accounts.spotify.com/api/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })
  const data = await response.json()
  if (!response.ok) throw new Error(data?.error_description || 'Spotify token exchange failed.')
  const token = { ...data, expires_at: Date.now() + (Number(data.expires_in || 3600) * 1000) }
  localStorage.setItem(TOKEN_KEY, JSON.stringify(token))
  localStorage.removeItem(VERIFIER_KEY)
  localStorage.removeItem(STATE_KEY)
  return token
}

export async function refreshSpotifyToken(token) {
  if (!CLIENT_ID || !token?.refresh_token) return null
  const response = await fetch('https://accounts.spotify.com/api/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: new URLSearchParams({
      grant_type: 'refresh_token',
      refresh_token: token.refresh_token,
      client_id: CLIENT_ID,
    }),
  })
  const data = await response.json()
  if (!response.ok) { clearSpotifyToken(); throw new Error(data?.error_description || 'Spotify session expired.') }
  const next = { ...token, ...data, refresh_token: data.refresh_token || token.refresh_token, expires_at: Date.now() + (Number(data.expires_in || 3600) * 1000) }
  localStorage.setItem(TOKEN_KEY, JSON.stringify(next))
  return next
}

async function authorizedFetch(path, options = {}) {
  let token = getSpotifyToken()
  if (!token) {
    try {
      const stored = JSON.parse(localStorage.getItem(TOKEN_KEY) || 'null')
      token = stored?.refresh_token ? await refreshSpotifyToken(stored) : null
    } catch { token = null }
  }
  if (!token?.access_token) throw new Error('Connect your Spotify account first.')
  const response = await fetch(`https://api.spotify.com/v1${path}`, {
    ...options,
    headers: { ...(options.headers || {}), Authorization: `Bearer ${token.access_token}` },
  })
  if (response.status === 401 && token.refresh_token) {
    const next = await refreshSpotifyToken(token)
    if (next?.access_token) return fetch(`https://api.spotify.com/v1${path}`, { ...options, headers: { ...(options.headers || {}), Authorization: `Bearer ${next.access_token}` } })
  }
  return response
}

export async function getSpotifyProfile() {
  const response = await authorizedFetch('/me')
  if (!response.ok) throw new Error('Unable to read Spotify profile.')
  return response.json()
}

export async function getSpotifyDevices() {
  const response = await authorizedFetch('/me/player/devices')
  if (!response.ok) {
    if (response.status === 403) throw new Error('Spotify requires Premium for Connect playback control.')
    throw new Error('Unable to load Spotify Connect devices.')
  }
  return response.json()
}

export async function transferSpotifyPlayback(deviceId, play = false) {
  const response = await authorizedFetch('/me/player', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ device_ids: [deviceId], play }),
  })
  if (!response.ok) throw new Error('Spotify could not transfer playback to that device.')
}
