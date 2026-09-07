import { useState, useEffect, useRef } from 'react'
import { useParams } from 'react-router-dom'
import { Play, Shuffle, Music2, Disc } from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'
import { useAuth } from '@/context/AuthContext'
import { searchMusic } from '@/lib/musicApi'
import TrackRow from '@/components/TrackRow'
import { SkeletonRowList } from '@/components/Skeleton'

export default function Album() {
  const { name } = useParams()
  const decodedName = decodeURIComponent(name || '')
  const { user, signIn } = useAuth()
  const player = usePlayer()
  const [tracks, setTracks] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const abortRef = useRef(null)

  // Derive album/artist from the search param "album - artist" pattern
  const [albumTitle, artistName] = decodedName.includes(' by ')
    ? decodedName.split(' by ').map(s => s.trim())
    : [decodedName, '']

  useEffect(() => {
    if (!user || !decodedName) return
    abortRef.current?.abort()
    const ctrl = new AbortController()
    abortRef.current = ctrl
    setLoading(true)
    setError(null)
    const query = artistName ? `${albumTitle} ${artistName}` : albumTitle
    searchMusic(query, { signal: ctrl.signal })
      .then(results => {
        if (ctrl.signal.aborted) return
        // Filter tracks that match the album name or artist
        const albumTracks = results.filter(t =>
          (t.album && t.album.toLowerCase().includes(albumTitle.toLowerCase())) ||
          (artistName && t.artist?.toLowerCase().includes(artistName.toLowerCase()))
        ).slice(0, 25)
        setTracks(albumTracks.length > 0 ? albumTracks : results.slice(0, 15))
        setLoading(false)
      })
      .catch(err => {
        if (err?.name === 'AbortError') return
        setError(err.message || 'Could not load album')
        setLoading(false)
      })
    return () => ctrl.abort()
  }, [user, decodedName, albumTitle, artistName])

  // Derive cover from first track
  const cover = tracks[0]?.thumbnail || null

  function playShuffled() {
    if (!tracks.length) return
    const shuffled = [...tracks].sort(() => Math.random() - 0.5)
    if (!player.shuffle) player.toggleShuffle()
    player.playTrack(shuffled[0], shuffled)
  }

  if (!user) {
    return (
      <div className="p-6 text-center pt-16">
        <Music2 size={40} className="mx-auto mb-3 text-text-subdued" />
        <p className="text-text-muted text-sm mb-4">Sign in to view album pages.</p>
        <button onClick={signIn} className="bg-brand text-black font-bold px-6 py-2.5 rounded-full">
          Sign in with Google
        </button>
      </div>
    )
  }

  return (
    <div>
      {/* Header */}
      <div className="flex items-end gap-6 p-6 bg-gradient-to-b from-surface-elevated to-transparent">
        <div className="w-32 h-32 md:w-48 md:h-48 rounded-md shadow-card bg-surface-highlight flex items-center justify-center shrink-0 overflow-hidden">
          {cover
            ? <img src={cover} alt="" className="w-full h-full object-cover" />
            : <Disc size={64} className="text-text-subdued" />
          }
        </div>
        <div>
          <p className="text-xs font-bold uppercase">Album</p>
          <h1 className="text-3xl md:text-5xl font-black my-2">{albumTitle}</h1>
          {artistName && <p className="text-text-muted text-sm font-semibold">{artistName}</p>}
          {tracks.length > 0 && <p className="text-text-subdued text-sm mt-1">{tracks.length} tracks</p>}
        </div>
      </div>

      <div className="p-4 md:p-6">
        {tracks.length > 0 && (
          <div className="flex items-center gap-4 mb-6">
            <button
              onClick={() => player.playTrack(tracks[0], tracks)}
              aria-label={`Play ${albumTitle}`}
              className="w-14 h-14 rounded-full bg-brand text-black flex items-center justify-center hover:scale-105 hover:bg-brand-hover transition-transform"
            >
              <Play size={24} className="ml-1" />
            </button>
            <button onClick={playShuffled} aria-label="Shuffle play" aria-pressed={player.shuffle}
              className={`w-10 h-10 rounded-full flex items-center justify-center ${player.shuffle ? 'text-brand' : 'text-text-muted hover:text-text'}`}>
              <Shuffle size={22} />
            </button>
          </div>
        )}

        {loading && <SkeletonRowList count={8} />}
        {error && <div className="bg-red-500/10 text-red-400 rounded-md px-4 py-3 text-sm">{error}</div>}
        {!loading && !error && tracks.length === 0 && (
          <div className="text-center py-16">
            <Disc size={40} className="mx-auto mb-3 text-text-subdued" />
            <p className="text-text-muted text-sm">No tracks found for this album.</p>
          </div>
        )}
        {!loading && tracks.map((track, i) => (
          <TrackRow key={track.id} track={track} index={i} contextTracks={tracks} />
        ))}
      </div>
    </div>
  )
}
