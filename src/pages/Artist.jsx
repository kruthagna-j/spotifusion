import { useState, useEffect, useRef } from 'react'
import { useParams, Link } from 'react-router-dom'
import { Play, Shuffle, Music2, Disc3, ExternalLink } from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'
import { useAuth } from '@/context/AuthContext'
import { searchMusic } from '@/lib/musicApi'
import TrackRow from '@/components/TrackRow'
import { SkeletonRowList } from '@/components/Skeleton'

// Artist page — fetches artist's top tracks via our backend search
// (ytmusicapi doesn't expose a true "artist profile" endpoint in the free
// tier, so we do a smart search for the artist name and group top results)
export default function Artist() {
  const { name } = useParams()           // /artist/:name (URL-encoded)
  const decodedName = decodeURIComponent(name || '')
  const { user, signIn } = useAuth()
  const player = usePlayer()
  const [tracks, setTracks] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const abortRef = useRef(null)

  useEffect(() => {
    if (!user || !decodedName) return
    abortRef.current?.abort()
    const ctrl = new AbortController()
    abortRef.current = ctrl
    setLoading(true)
    setError(null)
    searchMusic(decodedName, { signal: ctrl.signal })
      .then(results => {
        if (ctrl.signal.aborted) return
        // Filter to tracks most likely from this artist
        const artistTracks = results.filter(t =>
          t.artist?.toLowerCase().includes(decodedName.toLowerCase()) ||
          decodedName.toLowerCase().includes(t.artist?.toLowerCase() || '')
        ).slice(0, 20)
        setTracks(artistTracks.length > 0 ? artistTracks : results.slice(0, 10))
        setLoading(false)
      })
      .catch(err => {
        if (err?.name === 'AbortError') return
        setError(err.message || 'Could not load artist info')
        setLoading(false)
      })
    return () => ctrl.abort()
  }, [user, decodedName])

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
        <p className="text-text-muted text-sm mb-4">Sign in to view artist pages.</p>
        <button onClick={signIn} className="bg-brand text-black font-bold px-6 py-2.5 rounded-full">
          Sign in with Google
        </button>
      </div>
    )
  }

  return (
    <div>
      {/* Hero header */}
      <div className="relative h-48 md:h-64 flex items-end p-6 bg-gradient-to-b from-surface-elevated to-transparent overflow-hidden">
        <div className="absolute inset-0 flex items-center justify-center opacity-5">
          <Disc3 size={320} />
        </div>
        <div className="relative z-10">
          <p className="text-xs font-bold uppercase tracking-wide mb-2">Artist</p>
          <h1 className="text-4xl md:text-7xl font-black truncate">{decodedName}</h1>
          {tracks.length > 0 && (
            <p className="text-text-muted text-sm mt-2">{tracks.length} top tracks</p>
          )}
        </div>
      </div>

      <div className="p-4 md:p-6">
        {tracks.length > 0 && (
          <div className="flex items-center gap-4 mb-6">
            <button
              onClick={() => player.playTrack(tracks[0], tracks)}
              aria-label={`Play ${decodedName}`}
              className="w-14 h-14 rounded-full bg-brand text-black flex items-center justify-center hover:scale-105 hover:bg-brand-hover transition-transform"
            >
              <Play size={24} className="ml-1" />
            </button>
            <button
              onClick={playShuffled}
              aria-label="Shuffle play"
              aria-pressed={player.shuffle}
              className={`w-10 h-10 rounded-full flex items-center justify-center transition-colors ${
                player.shuffle ? 'text-brand' : 'text-text-muted hover:text-text'
              }`}
            >
              <Shuffle size={22} />
            </button>
          </div>
        )}

        {loading && <SkeletonRowList count={8} />}

        {error && (
          <div className="bg-red-500/10 text-red-400 rounded-md px-4 py-3 text-sm mb-4">
            {error}
          </div>
        )}

        {!loading && !error && tracks.length === 0 && (
          <div className="text-center py-16">
            <Music2 size={40} className="mx-auto mb-3 text-text-subdued" />
            <p className="text-text-muted text-sm">No tracks found for this artist.</p>
          </div>
        )}

        {!loading && tracks.length > 0 && (
          <>
            <h2 className="text-sm font-bold uppercase tracking-wide text-text-muted mb-3">Popular tracks</h2>
            {tracks.map((track, i) => (
              <TrackRow key={track.id} track={track} index={i} contextTracks={tracks} />
            ))}
          </>
        )}
      </div>
    </div>
  )
}
