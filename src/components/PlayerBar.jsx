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
  Cast,
  Moon,
} from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'
import { useAuth } from '@/context/AuthContext'
import { likeSong, unlikeSong } from '@/lib/library'
import { useLikedSongs } from '@/hooks/useLibraryData'
import { formatTime } from '@/lib/timeFormat'
import QueuePanel from '@/components/QueuePanel'
import NowPlaying from '@/components/NowPlaying'

export default function PlayerBar() {
  const player = usePlayer()
  const { user } = useAuth()
  const liked = useLikedSongs(user?.uid)
  const [queueOpen, setQueueOpen] = useState(false)
  const [sleepOpen, setSleepOpen] = useState(false)
  const { currentTrack } = player

  const isLiked = !!currentTrack && liked.some((t) => t.id === currentTrack.id)

  if (!currentTrack) return null

  function toggleLike() {
    if (!user || !currentTrack) return
    isLiked ? unlikeSong(user.uid, currentTrack.id) : likeSong(user.uid, currentTrack)
  }

  return (
    <>
      {/* Mobile mini-player -> tap to expand. Floating rounded card docked
          just above the bottom nav, matching the Figma reference (NowPlaying
          node: rounded-[10px], margin from screen edges, sits right above
          the tab bar) rather than a flush full-width bar. */}
      <div className="md:hidden px-2 pt-2 bg-bg">
        <button
          onClick={() => player.openNowPlaying()}
          aria-label={`Now playing: ${currentTrack.title} by ${currentTrack.artist}. Tap to expand.`}
          className="flex items-center gap-3 px-2 py-2 bg-surface-elevated rounded-[10px] shadow-card w-full text-left"
        >
          <img src={currentTrack.thumbnail} alt="" className="w-10 h-10 rounded-[6px] object-cover" />
          <div className="min-w-0 flex-1">
            <p className="text-sm truncate">{currentTrack.title}</p>
            <p className="text-xs text-text-muted truncate">{currentTrack.artist}</p>
          </div>
          <span
            role="button"
            tabIndex={-1}
            aria-hidden="true"
            onClick={(e) => {
              e.stopPropagation()
              player.togglePlay()
            }}
            className="p-2 mr-1"
          >
            {player.isPlaying ? <Pause size={22} /> : <Play size={22} />}
          </span>
        </button>
      </div>

      <NowPlaying />

      {/* Desktop player bar */}
      <div className="hidden md:grid grid-cols-3 items-center px-4 h-[90px] bg-surface border-t border-border">
        <div className="flex items-center gap-3 min-w-0">
          <img src={currentTrack.thumbnail} alt="" className="w-14 h-14 rounded object-cover" />
          <button
            onClick={() => player.openNowPlaying()}
            className="min-w-0 text-left"
            aria-label="Open Now Playing"
          >
            <p className="text-sm truncate hover:underline cursor-pointer">{currentTrack.title}</p>
            <p className="text-xs text-text-subdued truncate hover:underline cursor-pointer">
              {currentTrack.artist}
            </p>
          </button>
          <button
            onClick={toggleLike}
            aria-label={isLiked ? 'Unlike this song' : 'Like this song'}
            aria-pressed={isLiked}
            className="ml-2 shrink-0"
          >
            <Heart size={18} className={isLiked ? 'fill-brand text-brand' : 'text-text-muted hover:text-text'} />
          </button>
        </div>

        <div className="flex flex-col items-center gap-2 max-w-[560px] mx-auto w-full">
          <div className="flex items-center gap-5">
            <button
              onClick={player.toggleShuffle}
              aria-label="Toggle shuffle"
              aria-pressed={player.shuffle}
              className={`cursor-pointer ${player.shuffle ? 'text-brand' : 'text-text-muted hover:text-text'}`}
            >
              <Shuffle size={16} />
            </button>
            <button
              onClick={player.playPrevious}
              aria-label="Previous track"
              className="cursor-pointer text-text-muted hover:text-text"
            >
              <SkipBack size={18} />
            </button>
            <button
              onClick={player.togglePlay}
              aria-label={player.isPlaying ? 'Pause' : 'Play'}
              className="w-8 h-8 rounded-full bg-white text-black flex items-center justify-center hover:scale-105 transition-transform"
            >
              {player.isPlaying ? <Pause size={16} /> : <Play size={16} className="ml-0.5" />}
            </button>
            <button
              onClick={player.playNext}
              aria-label="Next track"
              className="cursor-pointer text-text-muted hover:text-text"
            >
              <SkipForward size={18} />
            </button>
            <RepeatIcon player={player} small />
          </div>
          <SeekBar player={player} compact />
        </div>

        <div className="relative flex items-center justify-end gap-2">
          {player.outputSupported && (
            <button
              onClick={player.outputDeviceId ? player.resetOutputDevice : player.chooseOutputDevice}
              aria-label={
                player.outputDeviceId
                  ? `Connected to ${player.outputDeviceLabel}. Click to disconnect.`
                  : 'Connect to a device'
              }
              title={
                player.outputDeviceId ? `Connected to ${player.outputDeviceLabel}` : 'Connect to a device'
              }
              className={player.outputDeviceId ? 'text-brand' : 'text-text-muted hover:text-text'}
            >
              <Cast size={18} />
            </button>
          )}
          <div className="relative">
            <button
              onClick={() => setSleepOpen((o) => !o)}
              aria-label="Sleep timer"
              title="Sleep timer"
              className={player.sleepTimerSeconds ? 'text-brand' : 'text-text-muted hover:text-text'}
            >
              <Moon size={18} />
            </button>
            {sleepOpen && (
              <div className="absolute bottom-full right-0 mb-3 w-52 bg-surface-elevated border border-border rounded-lg shadow-2xl p-2 z-50">
                <p className="px-2 py-1.5 text-xs font-bold text-text-muted">Sleep timer</p>
                {[300, 600, 900, 1800, 3600].map((seconds) => (
                  <button
                    key={seconds}
                    onClick={() => { player.setSleepTimer(seconds); setSleepOpen(false) }}
                    className="w-full text-left px-2 py-2 text-sm rounded hover:bg-surface-hover"
                  >
                    {seconds < 3600 ? `${seconds / 60} minutes` : '1 hour'}
                  </button>
                ))}
                <button
                  onClick={() => { player.setSleepTimer(player.duration > player.progress ? player.duration - player.progress : 0); setSleepOpen(false) }}
                  className="w-full text-left px-2 py-2 text-sm rounded hover:bg-surface-hover"
                >
                  End of track
                </button>
                {player.sleepTimerSeconds && (
                  <button
                    onClick={() => { player.clearSleepTimer(); setSleepOpen(false) }}
                    className="w-full text-left px-2 py-2 text-sm rounded hover:bg-surface-hover text-red-300"
                  >
                    Cancel timer
                  </button>
                )}
              </div>
            )}
          </div>
          <button
            onClick={() => setQueueOpen((o) => !o)}
            aria-label="Toggle queue"
            aria-expanded={queueOpen}
            title="Queue"
            className={queueOpen ? 'text-brand' : 'text-text-muted hover:text-text'}
          >
            <ListMusic size={18} />
          </button>
          {queueOpen && <QueuePanel onClose={() => setQueueOpen(false)} />}
          <button
            onClick={player.toggleMute}
            aria-label={player.muted || player.volume === 0 ? 'Unmute' : 'Mute'}
          >
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
            aria-label="Volume"
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
  const label =
    player.repeatMode === 'off'
      ? 'Enable repeat'
      : player.repeatMode === 'all'
        ? 'Repeat all (tap for repeat one)'
        : 'Repeat one (tap to disable)'
  return (
    <button
      onClick={player.cycleRepeat}
      aria-label={label}
      aria-pressed={active}
      className={`cursor-pointer ${active ? 'text-brand' : 'text-text-muted hover:text-text'}`}
    >
      <Icon size={size} />
    </button>
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
        aria-label="Seek"
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
