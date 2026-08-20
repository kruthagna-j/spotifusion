import { Heart, Play } from 'lucide-react'
import { useAuth } from '@/context/AuthContext'
import { useLikedSongs } from '@/hooks/useLibraryData'
import { usePlayer } from '@/context/PlayerContext'
import TrackRow from '@/components/TrackRow'

export default function LikedSongs() {
  const { user } = useAuth()
  const tracks = useLikedSongs(user?.uid)
  const player = usePlayer()

  return (
    <div>
      <div className="flex items-end gap-6 p-6 bg-gradient-to-b from-indigo-700 to-transparent">
        <div className="w-48 h-48 rounded-md shadow-2xl bg-gradient-to-br from-indigo-400 to-white flex items-center justify-center shrink-0">
          <Heart size={64} fill="white" className="text-white" />
        </div>
        <div>
          <p className="text-xs font-bold uppercase">Playlist</p>
          <h1 className="text-4xl md:text-6xl font-black my-2">Liked Songs</h1>
          <p className="text-text-muted text-sm">{tracks.length} songs</p>
        </div>
      </div>

      <div className="p-4 md:p-6">
        {tracks.length > 0 && (
          <button
            onClick={() => player.playTrack(tracks[0], tracks)}
            className="w-14 h-14 rounded-full bg-brand text-black flex items-center justify-center mb-6 hover:scale-105 transition-transform"
          >
            <Play size={24} className="ml-1" />
          </button>
        )}

        {!user && <p className="text-text-muted text-sm">Sign in to see songs you've liked.</p>}
        {user && tracks.length === 0 && (
          <p className="text-text-muted text-sm">Songs you like will appear here.</p>
        )}

        {tracks.map((track, i) => (
          <TrackRow key={track.id} track={track} index={i} contextTracks={tracks} />
        ))}
      </div>
    </div>
  )
}
