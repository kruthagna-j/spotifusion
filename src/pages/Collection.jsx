import { useEffect, useState } from 'react'
import { useLocation, useParams, Link } from 'react-router-dom'
import { ArrowLeft, Play, Music2, Search as SearchIcon, Shuffle, Disc3, UserRound } from 'lucide-react'
import { searchMusic } from '@/lib/musicApi'
import { useAuth } from '@/context/AuthContext'
import { usePlayer } from '@/context/PlayerContext'
import TrackRow from '@/components/TrackRow'

export default function Collection({ type }) {
  const { value } = useParams()
  const location = useLocation()
  const { user } = useAuth()
  const player = usePlayer()
  const initial = location.state?.tracks || []
  const title = decodeURIComponent(value || location.state?.name || (type === 'artist' ? 'Artist' : 'Album'))
  const [tracks, setTracks] = useState(initial)
  const [loading, setLoading] = useState(!initial.length)
  const [error, setError] = useState(null)

  useEffect(() => {
    if (!user || initial.length) { setLoading(false); return }
    let cancelled = false
    setLoading(true)
    searchMusic(title).then(results => {
      if (cancelled) return
      const filtered = type === 'artist'
        ? results.filter(t => (t.artist || '').toLowerCase().includes(title.toLowerCase()))
        : results.filter(t => (t.album || '').toLowerCase().includes(title.toLowerCase()))
      setTracks(filtered.length ? filtered : results)
    }).catch(e => !cancelled && setError(e.message)).finally(() => !cancelled && setLoading(false))
    return () => { cancelled = true }
  }, [user, title, type])

  if (!user) return <div className="p-8 text-center"><h1 className="text-2xl font-black">Sign in to explore {type}s</h1><Link to="/search" className="inline-flex mt-5 bg-brand text-black px-5 py-2.5 rounded-full font-bold">Go to Search</Link></div>
  return <div className="p-4 md:p-7 max-w-6xl mx-auto">
    <Link to="/search" className="inline-flex items-center gap-2 text-sm text-text-muted hover:text-white mb-6"><ArrowLeft size={16}/> Back</Link>
    <div className="sf-panel overflow-hidden mb-7">
      <div className="p-6 md:p-9 bg-gradient-to-br from-white/10 via-white/5 to-transparent">
        <div className="flex items-center gap-2 text-xs uppercase tracking-[.2em] text-text-subdued font-bold">{type === 'artist' ? <UserRound size={14}/> : <Disc3 size={14}/>} {type}</div>
        <h1 className="text-4xl md:text-6xl font-black mt-2 break-words">{title}</h1>
        <p className="text-sm text-text-muted mt-3">{tracks.length ? `${tracks.length} tracks found` : 'Explore music from this collection.'}</p>
        {tracks.length > 0 && <div className="flex flex-wrap gap-2 mt-6"><button onClick={() => player.playTrack(tracks[0], tracks)} className="inline-flex items-center gap-2 bg-brand text-black font-black px-5 py-3 rounded-full"><Play size={18} fill="currentColor"/> Play</button><button onClick={() => { const shuffled=[...tracks].sort(() => Math.random()-0.5); player.playTrack(shuffled[0], shuffled) }} className="inline-flex items-center gap-2 bg-white/10 hover:bg-white/15 font-bold px-5 py-3 rounded-full"><Shuffle size={17}/> Shuffle</button></div>}
      </div>
    </div>
    {loading && <div className="space-y-2">{Array.from({length:6}).map((_,i)=><div className="h-14 rounded-xl bg-white/5 animate-pulse" key={i}/>)}</div>}
    {error && <div className="sf-panel p-6 text-red-300">{error}</div>}
    {!loading && !error && !tracks.length && <div className="text-center py-12 text-text-muted"><SearchIcon className="mx-auto mb-3" size={38}/><p>No matching tracks were found.</p></div>}
    {!loading && tracks.length > 0 && <div className="space-y-1">{tracks.map((t,i)=><TrackRow key={t.id} track={t} index={i} contextTracks={tracks}/>)}</div>}
  </div>
}
