import { auth } from '@/lib/firebase'

const API_BASE = (import.meta.env.VITE_MUSIC_API_URL || 'https://spotifusion.onrender.com').replace(/\/$/, '')

/** Resolve a YouTube video id through the authenticated Render/yt-dlp fallback. */
export async function getYouTubeStream(videoId, { signal } = {}) {
  if (!videoId || !auth.currentUser) throw new Error('Sign in to stream music.')
  const token = await auth.currentUser.getIdToken()
  const response = await fetch(`${API_BASE}/api/stream/${encodeURIComponent(videoId)}`, {
    headers: { Authorization: `Bearer ${token}` },
    signal,
    cache: 'no-store',
  })
  let body = null
  try { body = await response.json() } catch {}
  if (!response.ok || !body?.url) throw new Error(body?.detail || 'Unable to start this stream.')
  return body
}
