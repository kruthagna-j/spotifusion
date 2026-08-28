import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { CheckCircle2, LoaderCircle } from 'lucide-react'
import { finishSpotifyAuth } from '@/lib/spotifyConnect'

export default function SpotifyCallback() {
  const navigate = useNavigate()
  const [error, setError] = useState('')

  useEffect(() => {
    let cancelled = false
    finishSpotifyAuth(window.location.search)
      .then(() => {
        if (!cancelled) navigate('/settings?spotify=connected', { replace: true })
      })
      .catch((e) => {
        if (!cancelled) setError(e?.message || 'Spotify connection failed.')
      })
    return () => { cancelled = true }
  }, [navigate])

  return <div className="min-h-screen bg-bg text-text grid place-items-center p-6">
    <div className="w-full max-w-md rounded-3xl border border-white/10 bg-white/[.04] p-7 text-center">
      {error ? <>
        <div className="text-red-300 font-bold">Spotify connection failed</div>
        <p className="text-sm text-text-muted mt-3">{error}</p>
        <button type="button" onClick={() => navigate('/settings', { replace: true })} className="sf-primary-button mt-6 w-full">Back to Settings</button>
      </> : <>
        <CheckCircle2 size={42} className="mx-auto text-brand" />
        <h1 className="text-xl font-black mt-4">Connecting Spotify…</h1>
        <p className="text-sm text-text-muted mt-2">Finishing your secure Spotify Connect session.</p>
        <LoaderCircle size={20} className="animate-spin mx-auto mt-5 text-text-subdued" />
      </>}
    </div>
  </div>
}
