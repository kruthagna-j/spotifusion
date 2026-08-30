import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate, useParams, Link } from 'react-router-dom'
import { ArrowLeft, Play, Search as SearchIcon, Shuffle, Disc3, UserRound, Music2 } from 'lucide-react'
import { getArtist, getAlbum } from '@/lib/musicApi'
import { useAuth } from '@/context/AuthContext'
import { usePlayer } from '@/context/PlayerContext'
import TrackRow from '@/components/TrackRow'
import { getArtwork } from '@/lib/artwork'

function AlbumCard({ item, navigate }) {
  const image = getArtwork(item, 'large')
  return <button type="button" onClick={() => navigate(`/album/${encodeURIComponent(item.id)}`, { state: { name: item.name || item.title, artwork: image, browseId: item.id } })} className="sf-collection-card">
    <div className="sf-collection-card-art">{image ? <img src={image} alt="" loading="lazy"/> : <Disc3 size={30}/>}</div>
    <strong className="truncate w-full text-left">{item.name || item.title || 'Album'}</strong>
    <span className="truncate w-full text-left">{item.subtitle || item.artist || 'Album'}</span>
  </button>
}

export default function Collection({ type }) {
  const { value } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const { user } = useAuth()
  const player = usePlayer()
  const state = location.state || {}
  const entityId = state.browseId || value
  const [tracks, setTracks] = useState(Array.isArray(state.tracks) ? state.tracks : [])
  const [title, setTitle] = useState(state.name || '')
  const [artwork, setArtwork] = useState(state.artwork || null)
  const [description, setDescription] = useState('')
  const [albums, setAlbums] = useState([])
  const [singles, setSingles] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [artworkFailed, setArtworkFailed] = useState(false)

  useEffect(() => {
    let cancelled = false
    if (!user || !entityId) { setLoading(false); return () => { cancelled = true } }
    setLoading(true)
    setError(null)
    setTracks(Array.isArray(state.tracks) ? state.tracks : [])
    setTitle(state.name || '')
    setArtwork(state.artwork || null)
    setDescription('')
    setAlbums([])
    setSingles([])
    setArtworkFailed(false)

    const loader = type === 'artist' ? getArtist(entityId) : getAlbum(entityId)
    loader.then((payload) => {
      if (cancelled) return
      if (!payload) throw new Error(`Unable to load this ${type}.`)
      const payloadTracks = Array.isArray(payload.tracks) ? payload.tracks : []
      setTracks(payloadTracks.length ? payloadTracks : (state.tracks || []))
      setTitle(payload.name || state.name || (type === 'artist' ? 'Artist' : 'Album'))
      setDescription(payload.description || '')
      setArtwork(getArtwork(payload, 'large') || state.artwork || null)
      if (type === 'artist') {
        setAlbums(Array.isArray(payload.albums) ? payload.albums : [])
        setSingles(Array.isArray(payload.singles) ? payload.singles : [])
      }
    }).catch((e) => { if (!cancelled) setError(e?.message || `Unable to load this ${type}.`) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [user, entityId, type, location.key])

  const heading = title || (type === 'artist' ? 'Artist' : 'Album')
  const uniqueTracks = useMemo(() => { const seen = new Set(); return tracks.filter((x) => x?.id && !seen.has(x.id) && seen.add(x.id)) }, [tracks])

  if (!user) return <div className="p-6 text-center"><h1 className="text-2xl font-black">Sign in to explore {type}s</h1><Link to="/login" className="inline-flex mt-5 bg-brand text-black px-5 py-2.5 rounded-full font-bold">Sign in</Link></div>

  return <div className="p-4 md:p-7 max-w-6xl mx-auto overflow-x-hidden min-w-0 pb-40">
    <button onClick={() => navigate(-1)} className="inline-flex items-center gap-2 text-sm text-text-muted hover:text-white mb-5"><ArrowLeft size={16}/> Back</button>
    <section className="sf-panel overflow-hidden mb-7">
      <div className="p-5 md:p-9 bg-gradient-to-br from-white/10 via-white/5 to-transparent">
        <div className="flex flex-col sm:flex-row sm:items-end gap-5">
          <div className={`w-36 h-36 md:w-52 md:h-52 overflow-hidden bg-surface-highlight shrink-0 shadow-card ${type === 'artist' ? 'rounded-full' : 'rounded-2xl'}`}>
            {artwork && !artworkFailed ? <img src={artwork} alt="" className="w-full h-full object-cover" onError={() => setArtworkFailed(true)}/> : <div className="w-full h-full grid place-items-center">{type === 'artist' ? <UserRound size={52} className="text-text-subdued"/> : <Disc3 size={52} className="text-text-subdued"/>}</div>}
          </div>
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2 text-xs uppercase tracking-[.2em] text-text-subdued font-bold">{type === 'artist' ? <UserRound size={14}/> : <Disc3 size={14}/>} {type}</div>
            <h1 className="text-3xl md:text-6xl font-black mt-2 break-words">{heading}</h1>
            {description && <p className="text-sm text-text-muted mt-2 line-clamp-4">{description}</p>}
            <p className="text-sm text-text-muted mt-3">{uniqueTracks.length} tracks{type === 'artist' && albums.length ? ` · ${albums.length} albums` : ''}</p>
            {uniqueTracks.length > 0 && <div className="flex flex-wrap gap-2 mt-5"><button onClick={() => player.playTrack(uniqueTracks[0], uniqueTracks)} className="sf-primary-button"><Play size={18} fill="currentColor"/> Play</button><button onClick={() => { const shuffled=[...uniqueTracks].sort(() => Math.random()-.5); player.playTrack(shuffled[0], shuffled) }} className="sf-secondary-button"><Shuffle size={17}/> Shuffle</button></div>}
          </div>
        </div>
      </div>
    </section>

    {loading && <div className="space-y-2">{Array.from({length:6}).map((_,i)=><div className="h-16 rounded-xl bg-white/5 animate-pulse" key={i}/>)}</div>}
    {error && <div className="sf-panel p-5 mb-5 text-red-300 flex items-center justify-between gap-3"><span>{error}</span><button onClick={() => navigate(-1)} className="text-brand font-bold">Go back</button></div>}

    {!loading && type === 'artist' && albums.length > 0 && <section className="mb-8"><div className="flex items-center justify-between mb-4"><h2 className="text-xl font-black">Albums</h2><span className="text-xs text-text-subdued">{albums.length}</span></div><div className="sf-collection-grid">{albums.map((item) => <AlbumCard key={item.id} item={item} navigate={navigate}/>)}</div></section>}
    {!loading && type === 'artist' && singles.length > 0 && <section className="mb-8"><h2 className="text-xl font-black mb-4">Singles & releases</h2><div className="sf-collection-grid">{singles.map((item) => <AlbumCard key={item.id} item={item} navigate={navigate}/>)}</div></section>}

    {!loading && uniqueTracks.length > 0 && <section><h2 className="text-xl font-black mb-4">{type === 'artist' ? 'Popular songs' : 'Tracks'}</h2><div className="space-y-1">{uniqueTracks.map((t,i)=><TrackRow key={t.id} track={t} index={i} contextTracks={uniqueTracks}/>)}</div></section>}
    {!loading && !error && !uniqueTracks.length && !albums.length && <div className="text-center py-14 text-text-muted"><SearchIcon className="mx-auto mb-3" size={38}/><p>No playable music was found.</p></div>}
  </div>
}
