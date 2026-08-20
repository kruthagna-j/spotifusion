import { Play, Pause, Heart, BadgeCheck } from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'
import { useAuth } from '@/context/AuthContext'
import { likeSong, unlikeSong } from '@/lib/library'
import { useLikedSongs } from '@/hooks/useLibraryData'
import { formatTime } from '@/lib/youtube'
import AddToPlaylistMenu from '@/components/AddToPlaylistMenu'

export default function TrackRow({ track, index, contextTracks }) {
  const player = usePlayer()
  const { user } = useAuth()
  const liked = useLikedSongs(user?.uid)
  const isCurrent = player.currentTrack?.id === track.id
  const isLiked = liked.some((t) => t.id === track.id)

  return (
    <div
      onDoubleClick={() => player.playTrack(track, contextTracks)}
      className={`group grid grid-cols-[24px_1fr_auto] items-center gap-4 px-4 py-2 rounded-md hover:bg-surface-hover ${
        isCurrent ? 'text-brand' : 'text-text-muted'
      }`}
    >
      <div className="flex items-center justify-center w-6">
        <span className="group-hover:hidden text-sm">{index + 1}</span>
        <button
          className="hidden group-hover:flex"
          onClick={() => (isCurrent ? player.togglePlay() : player.playTrack(track, contextTracks))}
        >
          {isCurrent && player.isPlaying ? (
            <Pause size={16} className="text-text" />
          ) : (
            <Play size={16} className="text-text" />
          )}
        </button>
      </div>

      <div className="flex items-center gap-3 min-w-0">
        <img src={track.thumbnail} alt="" className="w-10 h-10 rounded object-cover shrink-0" />
        <div className="min-w-0">
          <p className={`text-sm truncate ${isCurrent ? 'text-brand' : 'text-text'}`}>{track.title}</p>
          <p className="text-xs text-text-subdued truncate flex items-center gap-1">
            {track.artist}
            {track.trusted && <BadgeCheck size={12} className="text-brand shrink-0" />}
          </p>
        </div>
      </div>

      <div className="flex items-center gap-4">
        <button
          className="opacity-0 group-hover:opacity-100 transition-opacity"
          onClick={() => (user ? (isLiked ? unlikeSong(user.uid, track.id) : likeSong(user.uid, track)) : null)}
        >
          <Heart size={16} className={isLiked ? 'fill-brand text-brand opacity-100' : ''} />
        </button>
        <span className="text-sm w-10 text-right">
          {track.duration ? formatTime(parseDurationLocal(track.duration)) : '--:--'}
        </span>
        <AddToPlaylistMenu track={track} />
      </div>
    </div>
  )
}

function parseDurationLocal(iso) {
  const m = iso.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/)
  if (!m) return 0
  const [, h, min, s] = m
  return (Number(h) || 0) * 3600 + (Number(min) || 0) * 60 + (Number(s) || 0)
}
