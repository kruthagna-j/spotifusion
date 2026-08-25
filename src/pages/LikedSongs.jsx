import { Heart, Play, Shuffle } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { useLikedSongsStatus } from '@/hooks/useLibraryData'
import { usePlayer } from '@/context/PlayerContext'
import TrackRow from '@/components/TrackRow'
import { SkeletonRowList } from '@/components/Skeleton'

export default function LikedSongs() {
  const { user } = useAuth()
  const { data: tracks, loading } = useLikedSongsStatus(user?.uid)
  const player = usePlayer()

  function playShuffled() {
    if (!tracks.length) return
    const shuffled = [...tracks].sort(() => Math.random() - 0.5)
    if (!player.shuffle) player.toggleShuffle()
    player.playTrack(shuffled[0], shuffled)
  }

  return (
    <div>
      <div className="flex items-end gap-6 p-6 bg-gradient-to-b from-indigo-700 to-transparent">
        <div className="w-32 h-32 md:w-48 md:h-48 rounded-md shadow-card bg-gradient-to-br from-indigo-400 to-white flex items-center justify-center shrink-0">
          <Heart size={48} fill="white" className="text-white" aria-hidden="true" />
        </div>
        <div>
          <p className="text-xs font-bold uppercase">Playlist</p>
          <h1 className="text-3xl md:text-6xl font-black my-2">Liked Songs</h1>
          <p className="text-text-muted text-sm">{tracks.length} songs</p>
        </div>
      </div>

      <div className="p-4 md:p-6">
        {tracks.length > 0 && (
          <div className="flex items-center gap-4 mb-6">
            <button
              onClick={() => player.playTrack(tracks[0], tracks)}
              aria-label="Play Liked Songs"
              className="w-14 h-14 rounded-full bg-brand text-black flex items-center justify-center hover:scale-105 hover:bg-brand-hover transition-transform"
            >
              <Play size={24} className="ml-1" />
            </button>
            <button
              onClick={playShuffled}
              aria-label="Shuffle play"
              aria-pressed={player.shuffle}
              title="Shuffle play"
              className={`w-10 h-10 rounded-full flex items-center justify-center transition-colors ${
                player.shuffle ? 'text-brand' : 'text-text-muted hover:text-text'
              }`}
            >
              <Shuffle size={22} />
            </button>
          </div>
        )}

        {!user && (
          <p className="text-text-muted text-sm">Sign in to see songs you've liked.</p>
        )}

        {user && loading && <SkeletonRowList count={6} />}

        {user && !loading && tracks.length === 0 && (
          <div className="text-center py-16">
            <Heart size={40} className="mx-auto mb-3 text-text-subdued" aria-hidden="true" />
            <p className="text-text-muted text-sm">Songs you like will appear here.</p>
          </div>
        )}

        {!loading &&
          tracks.map((track, i) => (
            <TrackRow key={track.id} track={track} index={i} contextTracks={tracks} />
          ))}
      </div>
    </div>
  )
}
