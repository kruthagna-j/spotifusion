import { useState } from 'react'
import {
  Play,
  Pause,
  SkipBack,
  SkipForward,
  Shuffle,
  Repeat,
  Repeat1,
  Volume2,
  VolumeX,
  Heart,
  ChevronDown,
  ListMusic,
} from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'
import { useAuth } from '@/context/AuthContext'
import { likeSong, unlikeSong } from '@/lib/library'
import { useLikedSongs } from '@/hooks/useLibraryData'
import { formatTime } from '@/lib/youtube'
import QueuePanel from '@/components/QueuePanel'

export default function PlayerBar() {
  const player = usePlayer()
  const { user } = useAuth()
  const liked = useLikedSongs(user?.uid)
  const [expanded, setExpanded] = useState(false)
  const [queueOpen, setQueueOpen] = useState(false)
  const { currentTrack } = player

  const isLiked = !!currentTrack && liked.some((t) => t.id === currentTrack.id)

  if (!currentTrack) return null

  function toggleLike() {
    if (!user || !currentTrack) return
    isLiked ? unlikeSong(user.uid, currentTrack.id) : likeSong(user.uid, currentTrack)
  }

  return (
    <>
      {/* Mobile mini-player -> tap to expand */}
      <button
        onClick={() => setExpanded(true)}
        className="md:hidden flex items-center gap-3 px-3 py-2 bg-surface-elevated border-t border-border w-full text-left"
      >
        <img src={currentTrack.thumbnail} alt="" className="w-10 h-10 rounded object-cover" />
        <div className="min-w-0 flex-1">
          <p className="text-sm truncate">{currentTrack.title}</p>
          <p className="text-xs text-text-subdued truncate">{currentTrack.artist}</p>
        </div>
        <span
          onClick={(e) => {
            e.stopPropagation()
            player.togglePlay()
          }}
          className="p-2"
        >
          {player.isPlaying ? <Pause size={22} /> : <Play size={22} />}
        </span>
      </button>

      {/* Mobile full-screen now-playing sheet */}
      {expanded && (
        <div className="md:hidden fixed inset-0 z-50 bg-gradient-to-b from-surface-highlight to-black flex flex-col p-6">
          <button onClick={() => setExpanded(false)} className="self-start p-2 -ml-2 mb-6">
            <ChevronDown size={26} />
          </button>
          <img
            src={currentTrack.thumbnail}
            alt=""
            className="w-full aspect-square object-cover rounded-lg shadow-2xl mb-8"
          />
          <div className="flex items-center justify-between mb-4">
            <div className="min-w-0">
              <p className="text-xl font-bold truncate">{currentTrack.title}</p>
              <p className="text-text-muted truncate">{currentTrack.artist}</p>
            </div>
            <button onClick={toggleLike}>
              <Heart size={24} className={isLiked ? 'fill-brand text-brand' : 'text-text-muted'} />
            </button>
          </div>
          <SeekBar player={player} />
          <div className="flex items-center justify-center gap-8 mt-6">
            <Shuffle
              size={20}
              onClick={player.toggleShuffle}
              className={player.shuffle ? 'text-brand' : 'text-text-muted'}
            />
            <SkipBack size={28} onClick={player.playPrevious} />
            <button
              onClick={player.togglePlay}
              className="w-16 h-16 rounded-full bg-white text-black flex items-center justify-center"
            >
              {player.isPlaying ? <Pause size={28} /> : <Play size={28} className="ml-1" />}
            </button>
            <SkipForward size={28} onClick={player.playNext} />
            <RepeatIcon player={player} />
          </div>
        </div>
      )}

      {/* Desktop player bar */}
      <div className="hidden md:grid grid-cols-3 items-center px-4 h-[90px] bg-surface border-t border-border">
        <div className="flex items-center gap-3 min-w-0">
          <img src={currentTrack.thumbnail} alt="" className="w-14 h-14 rounded object-cover" />
          <div className="min-w-0">
            <p className="text-sm truncate hover:underline cursor-pointer">{currentTrack.title}</p>
            <p className="text-xs text-text-subdued truncate hover:underline cursor-pointer">
              {currentTrack.artist}
            </p>
          </div>
          <button onClick={toggleLike} className="ml-2 shrink-0">
            <Heart size={18} className={isLiked ? 'fill-brand text-brand' : 'text-text-muted hover:text-text'} />
          </button>
        </div>

        <div className="flex flex-col items-center gap-2 max-w-[560px] mx-auto w-full">
          <div className="flex items-center gap-5">
            <Shuffle
              size={16}
              onClick={player.toggleShuffle}
              className={`cursor-pointer ${player.shuffle ? 'text-brand' : 'text-text-muted hover:text-text'}`}
            />
            <SkipBack size={18} onClick={player.playPrevious} className="cursor-pointer text-text-muted hover:text-text" />
            <button
              onClick={player.togglePlay}
              className="w-8 h-8 rounded-full bg-white text-black flex items-center justify-center hover:scale-105 transition-transform"
            >
              {player.isPlaying ? <Pause size={16} /> : <Play size={16} className="ml-0.5" />}
            </button>
            <SkipForward size={18} onClick={player.playNext} className="cursor-pointer text-text-muted hover:text-text" />
            <RepeatIcon player={player} small />
          </div>
          <SeekBar player={player} compact />
        </div>

        <div className="relative flex items-center justify-end gap-2">
          <button
            onClick={() => setQueueOpen((o) => !o)}
            title="Queue"
            className={queueOpen ? 'text-brand' : 'text-text-muted hover:text-text'}
          >
            <ListMusic size={18} />
          </button>
          {queueOpen && <QueuePanel onClose={() => setQueueOpen(false)} />}
          <button onClick={player.toggleMute}>
            {player.muted || player.volume === 0 ? (
              <VolumeX size={18} className="text-text-muted" />
            ) : (
              <Volume2 size={18} className="text-text-muted" />
            )}
          </button>
          <input
            type="range"
            min={0}
            max={100}
            value={player.muted ? 0 : player.volume}
            onChange={(e) => player.changeVolume(Number(e.target.value))}
            className="w-24 accent-white"
          />
        </div>
      </div>
    </>
  )
}

function RepeatIcon({ player, small }) {
  const size = small ? 16 : 20
  const active = player.repeatMode !== 'off'
  const Icon = player.repeatMode === 'one' ? Repeat1 : Repeat
  return (
    <Icon
      size={size}
      onClick={player.cycleRepeat}
      className={`cursor-pointer ${active ? 'text-brand' : 'text-text-muted hover:text-text'}`}
    />
  )
}

function SeekBar({ player, compact }) {
  return (
    <div className="flex items-center gap-2 w-full">
      {compact && <span className="text-[11px] text-text-subdued w-9 text-right">{formatTime(player.progress)}</span>}
      <input
        type="range"
        min={0}
        max={player.duration || 0}
        value={player.progress}
        onChange={(e) => player.seekTo(Number(e.target.value))}
        className="w-full accent-white h-1"
      />
      {compact && <span className="text-[11px] text-text-subdued w-9">{formatTime(player.duration)}</span>}
      {!compact && (
        <div className="flex justify-between text-xs text-text-subdued mt-1 w-full">
          <span>{formatTime(player.progress)}</span>
          <span>{formatTime(player.duration)}</span>
        </div>
      )}
    </div>
  )
}
