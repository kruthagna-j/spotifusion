import { useMemo, useRef, useEffect } from 'react'
import {
  X, Play, Pause, SkipBack, SkipForward, Shuffle, Repeat, Repeat1,
  Heart, ListMusic, Moon, Volume2, VolumeX, Music2, Radio, Share2, ChevronDown
} from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'
import { useAuth } from '@/context/AuthContext'
import { useAuxPane } from '@/context/AuxPaneContext'
import { likeSong, unlikeSong } from '@/lib/library'
import { useLikedSongs } from '@/hooks/useLibraryData'
import { formatTime } from '@/lib/timeFormat'
import QueuePanel from '@/components/QueuePanel'

function parseSyncedLyrics(raw) {
  if (!raw || typeof raw !== 'string') return []
  return raw.split(/\r?\n/).flatMap((line) => {
    const matches = [...line.matchAll(/\[(\d{1,2}):(\d{2})(?:\.(\d{1,3}))?\]/g)]
    const text = line.replace(/\[[^\]]+\]/g, '').trim()
    return matches.map((m) => ({
      time: Number(m[1]) * 60 + Number(m[2]) + Number(`0.${m[3] || '0'}`),
      text,
    }))
  }).filter((x) => x.text).sort((a, b) => a.time - b.time)
}

// ── Right-side Auxiliary Pane (desktop) ─────────────────────────────────────
// A collapsible 320px panel docked to the right that shows track details,
// synced lyrics, and queue — toggled via the button in the player bar.
// On mobile this isn't shown (NowPlaying fullscreen covers that role).
export default function AuxPane() {
  const player = usePlayer()
  const { user } = useAuth()
  const { auxOpen, closeAux } = useAuxPane()
  const liked = useLikedSongs(user?.uid)
  const lyricsRef = useRef(null)

  const current = player.currentTrack
  const isLiked = !!current && liked.some((t) => t.id === current.id)
  const lyrics = useMemo(() => parseSyncedLyrics(current?.lyrics || current?.syncedLyrics || ''), [current])
  const activeLyric = useMemo(() => {
    let idx = -1
    lyrics.forEach((line, i) => { if (line.time <= player.progress) idx = i })
    return idx
  }, [lyrics, player.progress])

  // Auto-scroll active lyric into view
  useEffect(() => {
    if (!lyricsRef.current || activeLyric < 0) return
    const el = lyricsRef.current.querySelector(`[data-lyric="${activeLyric}"]`)
    el?.scrollIntoView({ block: 'center', behavior: 'smooth' })
  }, [activeLyric])

  if (!auxOpen || !current) return null

  const toggleLike = () => {
    if (!user) return
    isLiked ? unlikeSong(user.uid, current.id) : likeSong(user.uid, current)
  }

  const share = async () => {
    const text = `${current.title} — ${current.artist}`
    try {
      if (navigator.share) await navigator.share({ title: current.title, text })
      else await navigator.clipboard.writeText(text)
    } catch { /* user cancelled */ }
  }

  return (
    <aside
      className="hidden md:flex flex-col w-[320px] shrink-0 h-full bg-surface border-l border-border overflow-hidden"
      aria-label="Now Playing panel"
    >
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-border shrink-0">
        <span className="text-xs font-bold uppercase tracking-widest text-text-muted">Now Playing</span>
        <button onClick={closeAux} aria-label="Close panel" className="text-text-muted hover:text-text p-1 rounded">
          <X size={16} />
        </button>
      </div>

      {/* Artwork */}
      <div className="px-5 pt-5 pb-3 shrink-0">
        <div className="relative w-full aspect-square rounded-xl overflow-hidden shadow-card bg-surface-elevated">
          {current.thumbnail
            ? <img src={current.thumbnail} alt="" className="w-full h-full object-cover" />
            : <div className="w-full h-full flex items-center justify-center"><Music2 size={64} className="text-text-subdued" /></div>
          }
        </div>
      </div>

      {/* Title + like */}
      <div className="px-5 pb-2 flex items-start gap-3 shrink-0">
        <div className="flex-1 min-w-0">
          <p className="text-base font-bold truncate">{current.title}</p>
          <p className="text-sm text-text-muted truncate mt-0.5">{current.artist}</p>
        </div>
        <button onClick={toggleLike} aria-label={isLiked ? 'Unlike' : 'Like'} className="mt-0.5 shrink-0">
          <Heart size={20} className={isLiked ? 'fill-brand text-brand' : 'text-text-muted hover:text-text'} />
        </button>
      </div>

      {/* Seek */}
      <div className="px-5 pb-2 shrink-0">
        <input type="range" min={0} max={player.duration || 0}
          value={Math.min(player.progress, player.duration || 0)}
          onChange={(e) => player.seekTo(Number(e.target.value))}
          className="w-full accent-brand h-1" aria-label="Seek" />
        <div className="flex justify-between text-[11px] text-text-subdued mt-1">
          <span>{formatTime(player.progress)}</span>
          <span>{formatTime(player.duration)}</span>
        </div>
      </div>

      {/* Controls */}
      <div className="px-5 pb-3 flex items-center justify-between shrink-0">
        <button onClick={player.toggleShuffle} className={player.shuffle ? 'text-brand' : 'text-text-muted hover:text-text'} aria-label="Shuffle">
          <Shuffle size={17} />
        </button>
        <button onClick={player.playPrevious} className="text-text-muted hover:text-text" aria-label="Previous">
          <SkipBack size={22} />
        </button>
        <button onClick={player.togglePlay}
          aria-label={player.isPlaying ? 'Pause' : 'Play'}
          className="w-11 h-11 rounded-full bg-white text-black flex items-center justify-center hover:scale-105 transition-transform">
          {player.isPlaying ? <Pause size={18} /> : <Play size={18} className="ml-0.5" />}
        </button>
        <button onClick={player.playNext} className="text-text-muted hover:text-text" aria-label="Next">
          <SkipForward size={22} />
        </button>
        <button onClick={player.cycleRepeat}
          className={player.repeatMode !== 'off' ? 'text-brand' : 'text-text-muted hover:text-text'}
          aria-label="Repeat">
          {player.repeatMode === 'one' ? <Repeat1 size={17} /> : <Repeat size={17} />}
        </button>
      </div>

      {/* Volume */}
      <div className="px-5 pb-3 flex items-center gap-2 shrink-0">
        <button onClick={player.toggleMute} aria-label={player.muted ? 'Unmute' : 'Mute'}>
          {player.muted || player.volume === 0 ? <VolumeX size={15} className="text-text-muted" /> : <Volume2 size={15} className="text-text-muted" />}
        </button>
        <input type="range" min={0} max={100} value={player.muted ? 0 : player.volume}
          onChange={(e) => player.changeVolume(Number(e.target.value))}
          className="flex-1 accent-brand" aria-label="Volume" />
      </div>

      {/* Divider */}
      <div className="h-px bg-border mx-4 shrink-0" />

      {/* Lyrics / scrollable area */}
      <div ref={lyricsRef} className="flex-1 overflow-y-auto px-5 py-4 scrollbar-none">
        {lyrics.length > 0 ? (
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-text-subdued mb-4">
              <Radio size={14} /><span className="text-xs font-semibold uppercase tracking-wide">Lyrics</span>
            </div>
            {lyrics.map((line, i) => (
              <button
                key={`${line.time}-${i}`}
                data-lyric={i}
                onClick={() => player.seekTo(line.time)}
                className={`block w-full text-left text-sm leading-relaxed transition-all ${
                  i === activeLyric ? 'text-white font-bold scale-[1.02]' : 'text-text-subdued hover:text-text-muted'
                }`}
              >
                {line.text}
              </button>
            ))}
          </div>
        ) : (
          <div className="flex flex-col items-center justify-center h-full min-h-[160px] text-center">
            <Music2 size={36} className="text-text-subdued mb-3" />
            <p className="text-sm font-semibold mb-1">Lyrics</p>
            <p className="text-xs text-text-subdued max-w-[220px]">
              Synchronized lyrics appear here when timestamped lyrics are available for this track.
            </p>
          </div>
        )}
      </div>

      {/* Footer actions */}
      <div className="flex items-center justify-around border-t border-border px-3 py-2 shrink-0">
        <button onClick={share} className="flex flex-col items-center gap-0.5 text-text-subdued hover:text-text p-2" aria-label="Share">
          <Share2 size={16} />
          <span className="text-[10px]">Share</span>
        </button>
        <button className="flex flex-col items-center gap-0.5 text-text-subdued hover:text-text p-2" aria-label="Queue">
          <ListMusic size={16} />
          <span className="text-[10px]">Queue</span>
        </button>
        <button
          onClick={() => player.openNowPlaying()}
          className="flex flex-col items-center gap-0.5 text-text-subdued hover:text-text p-2"
          aria-label="Expand Now Playing">
          <ChevronDown size={16} className="rotate-180" />
          <span className="text-[10px]">Expand</span>
        </button>
      </div>
    </aside>
  )
}
