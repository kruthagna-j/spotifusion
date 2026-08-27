import { useEffect, useState } from 'react'
import { useLocation, useParams, Link } from 'react-router-dom'
import { ArrowLeft, Play, Search as SearchIcon, Shuffle, Disc3, UserRound } from 'lucide-react'
import { getArtist, getAlbum } from '@/lib/musicApi'
import { useAuth } from '@/context/AuthContext'
import { usePlayer } from '@/context/PlayerContext'
import TrackRow from '@/components/TrackRow'
import { getArtwork } from '@/lib/artwork'

export default function Collection({ type }) {
  const { value } = useParams()
  const location = useLocation()
  const { user } = useAuth()
  const player = usePlayer()
  const state = location.state || {}
  const [tracks, setTracks] = useState(state.tracks || [])
  const [title, setTitle] = useState(state.name || '')
  const [artwork, setArtwork] = useState(state.artwork || null)
  const [description, setDescription] = useState('')
  const [loading, setLoading] = useState(!(state.tracks?.length))
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    const initialTracks = Array.isArray(state.tracks) ? state.tracks : []
    if (initialTracks.length) {
      setTracks(initialTracks)
      setLoading(false)
    }
    if (!user) {
      setLoading(false)
      return () => { cancelled = true }
    }

    const browseId = state.browseId || value
    if (!browseId) {
      setError(`Invalid ${type} identifier.`)
      setLoading(false)
      return () => { cancelled = true }
    }

    // Even when search supplied some tracks, refresh the entity in the
    // background so direct links/reloads have the real title/artwork/tracks.
    setLoading(!initialTracks.length)
    setError(null)
    const loader = type === 'artist' ? getArtist(browseId) : getAlbum(browseId)
    loader.then((payload) => {
      if (cancelled) return
      if (!payload) throw new Error(`Unable to load this ${type}.`)
      const payloadTracks = Array.isArray(payload.tracks) ? payload.tracks : []
      setTracks(payloadTracks.length ? payloadTracks : initialTracks)
      setTitle(payload.name || state.name || (type === 'artist' ? 'Artist' : 'Album'))
      setDescription(payload.description || '')
      setArtwork(getArtwork(payload, 'large') || state.artwork || null)
    }).catch((e) => {
      if (!cancelled) setError(e?.message || `Unable to load this ${type}.`)
    }).finally(() => {
      if (!cancelled) setLoading(false)
    })
    return () => { cancelled = true }
  // state is the navigation snapshot; changing the URL/type should reload.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user, value, type])

  if (!user) return <div className="p-6 md:p-8 text-center"><h1 className="text-2xl font-black">Sign in to explore {type}s</h1><Link to="/search" className="inline-flex mt-5 bg-brand text-black px-5 py-2.5 rounded-full font-bold">Go to Search</Link></div>

  const heading = title || decodeURIComponent(value || '') || (type === 'artist' ? 'Artist' : 'Album')
  return <div className="p-4 md:p-7 max-w-6xl mx-auto overflow-x-hidden">
    <Link to="/search" className="inline-flex items-center gap-2 text-sm text-text-muted hover:text-white mb-5"><ArrowLeft size={16}/> Back to search</Link>
    <div className="sf-panel overflow-hidden mb-6">
      <div className="p-5 md:p-9 bg-gradient-to-br from-white/10 via-white/5 to-transparent">
        <div className="flex flex-col sm:flex-row sm:items-end gap-5">
          <div className="w-36 h-36 md:w-48 md:h-48 rounded-2xl overflow-hidden bg-surface-highlight shrink-0 shadow-card">
            {artwork ? <img src={artwork} alt="" className="w-full h-full object-cover" onError={(e) => { e.currentTarget.style.display='none' }} /> : <div className="w-full h-full grid place-items-center"><Disc3 size={48} className="text-text-subdued" /></div>}
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2 text-xs uppercase tracking-[.2em] text-text-subdued font-bold">{type === 'artist' ? <UserRound size={14}/> : <Disc3 size={14}/>} {type}</div>
            <h1 className="text-3xl md:text-6xl font-black mt-2 break-words">{heading}</h1>
            {description && <p className="text-sm text-text-muted mt-2 line-clamp-3">{description}</p>}
            <p className="text-sm text-text-muted mt-3">{tracks.length ? `${tracks.length} tracks` : 'No tracks found.'}</p>
            {tracks.length > 0 && <div className="flex flex-wrap gap-2 mt-5">
              <button onClick={() => player.playTrack(tracks[0], tracks)} className="inline-flex items-center gap-2 bg-brand text-black font-black px-5 py-3 rounded-full"><Play size={18} fill="currentColor"/> Play</button>
              <button onClick={() => { const shuffled=[...tracks].sort(() => Math.random()-0.5); player.playTrack(shuffled[0], shuffled) }} className="inline-flex items-center gap-2 bg-white/10 hover:bg-white/15 font-bold px-5 py-3 rounded-full"><Shuffle size={17}/> Shuffle</button>
            </div>}
          </div>
        </div>
      </div>
    </div>
    {loading && <div className="space-y-2">{Array.from({length:6}).map((_,i)=><div className="h-14 rounded-xl bg-white/5 animate-pulse" key={i}/>)}</div>}
    {error && <div className="sf-panel p-5 mb-4 text-red-300 flex items-start justify-between gap-3"><span>{error}</span><Link to="/search" className="text-brand font-bold shrink-0">Search again</Link></div>}
    {!loading && !error && !tracks.length && <div className="text-center py-12 text-text-muted"><SearchIcon className="mx-auto mb-3" size={38}/><p>No matching tracks were found.</p></div>}
    {!loading && tracks.length > 0 && <div className="space-y-1">{tracks.map((t,i)=><TrackRow key={t.id} track={t} index={i} contextTracks={tracks}/>)}</div>}
  </div>
}
