import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams, Link } from 'react-router-dom'
import { ArrowLeft, Play, Shuffle, ListMusic } from 'lucide-react'
import { getPlaylist } from '@/lib/musicApi'
import { useAuth } from '@/context/AuthContext'
import { usePlayer } from '@/context/PlayerContext'
import TrackRow from '@/components/TrackRow'
import { getArtwork } from '@/lib/artwork'

export default function YouTubePlaylist() {
  const { id } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuth()
  const player = usePlayer()
  const state = location.state || {}
  const [data, setData] = useState({ name: state.name || 'Playlist', artwork: state.artwork || null, description: '', tracks: state.tracks || [] })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!user) { setLoading(false); return }
    let cancelled = false
    setLoading(true)
    getPlaylist(id).then((payload) => {
      if (cancelled) return
      if (!payload) throw new Error('Unable to load this playlist.')
      setData({
        name: payload.name || state.name || 'Playlist',
        artwork: getArtwork(payload, 'large') || state.artwork || null,
        description: payload.description || '',
        tracks: Array.isArray(payload.tracks) ? payload.tracks : [],
      })
    }).catch((e) => { if (!cancelled) setError(e?.message || 'Unable to load this playlist.') })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, id])

  if (!user) return <div className="p-6 text-center"><h1 className="text-xl font-black">Sign in to open playlists</h1><button onClick={() => navigate('/search')} className="mt-5 bg-brand text-black font-bold px-5 py-2.5 rounded-full">Back to Search</button></div>
  if (loading) return <div className="p-5 md:p-7"><div className="sf-panel h-52 animate-pulse"/></div>
  if (error) return <div className="p-5 md:p-7"><div className="sf-panel p-6 text-center text-red-300">{error}<div><Link to="/search" className="inline-flex mt-5 bg-brand text-black font-bold px-5 py-2.5 rounded-full">Back to Search</Link></div></div></div>

  const tracks = data.tracks || []
  return <div className="p-4 md:p-7 max-w-6xl mx-auto min-w-0 overflow-x-hidden">
    <button onClick={() => navigate(-1)} className="inline-flex items-center gap-2 text-sm text-text-muted hover:text-text mb-5"><ArrowLeft size={16}/> Back</button>
    <section className="sf-panel overflow-hidden mb-6">
      <div className="p-5 md:p-8 flex flex-col sm:flex-row sm:items-end gap-5 bg-gradient-to-br from-white/10 via-white/5 to-transparent">
        <div className="w-36 h-36 md:w-48 md:h-48 rounded-2xl overflow-hidden bg-surface-highlight shrink-0">
          {data.artwork ? <img src={data.artwork} alt="" className="w-full h-full object-cover" /> : <div className="w-full h-full grid place-items-center"><ListMusic size={48} className="text-text-subdued"/></div>}
        </div>
        <div className="min-w-0">
          <p className="text-xs uppercase tracking-[.2em] text-text-subdued font-bold">YouTube playlist</p>
          <h1 className="text-3xl md:text-6xl font-black mt-2 break-words">{data.name}</h1>
          {data.description && <p className="text-sm text-text-muted mt-2 line-clamp-3">{data.description}</p>}
          <p className="text-sm text-text-muted mt-3">{tracks.length} tracks</p>
          {tracks.length > 0 && <div className="flex flex-wrap gap-2 mt-5">
            <button onClick={() => player.playTrack(tracks[0], tracks)} className="inline-flex items-center gap-2 bg-brand text-black font-black px-5 py-3 rounded-full"><Play size={18} fill="currentColor"/> Play</button>
            <button onClick={() => { const shuffled=[...tracks].sort(() => Math.random()-0.5); player.playTrack(shuffled[0], shuffled) }} className="inline-flex items-center gap-2 bg-white/10 font-bold px-5 py-3 rounded-full"><Shuffle size={17}/> Shuffle</button>
          </div>}
        </div>
      </div>
    </section>
    {tracks.length ? <div className="space-y-1">{tracks.map((track, i) => <TrackRow key={track.id} track={track} index={i} contextTracks={tracks}/>)}</div> : <div className="sf-panel p-8 text-center text-text-muted">This playlist has no playable tracks.</div>}
  </div>
}
