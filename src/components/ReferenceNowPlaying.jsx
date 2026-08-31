import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronDown, MoreVertical, SlidersHorizontal, ListMusic, Music2, Share2, Shuffle, SkipBack, SkipForward, Repeat, Repeat1, Heart } from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'
import { useAuth } from '@/context/AuthContext'
import { useLikedSongs } from '@/hooks/useLibraryData'
import { likeSong, unlikeSong } from '@/lib/library'
import { getArtwork } from '@/lib/artwork'
import { formatTime } from '@/lib/timeFormat'
import CircularSeek from '@/components/CircularSeek'
import QueuePanel from '@/components/QueuePanel'

const WAVE = [8,18,12,28,20,34,15,25,10,30,17,38,21,13,32,18,27,11,35,16,29,20,12,31,17,25,9,34,15,28,18,11,30,22,14,35,19,27,10,24,17,31,13,29,20,12]

export default function ReferenceNowPlaying() {
  const player = usePlayer()
  const navigate = useNavigate()
  const { user } = useAuth()
  const liked = useLikedSongs(user?.uid)
  const [panel, setPanel] = useState(null)
  const current = player.currentTrack
  const isLiked = !!current && liked.some((t) => t.id === current.id)
  const artwork = current ? getArtwork(current, 'large') : ''
  const progress = Math.min(player.progress || 0, player.duration || 0)

  const share = async () => {
    if (!current) return
    const text = `${current.title} — ${current.artist}`
    try { if (navigator.share) await navigator.share({ title: current.title, text }); else await navigator.clipboard.writeText(text) } catch {}
  }
  const toggleLike = () => {
    if (!user || !current) return
    isLiked ? unlikeSong(user.uid, current.id) : likeSong(user.uid, current)
  }
  const wave = useMemo(() => WAVE, [])
  if (!current || !player.nowPlayingOpen) return null

  return (
    <div className="fixed inset-0 z-[95] md:hidden now-playing-reference overflow-y-auto">
      <div className="ref-header">
        <button className="ref-icon-button" onClick={player.closeNowPlaying} aria-label="Close now playing"><ChevronDown size={21}/></button>
        <div className="text-center"><div className="ref-title">Now Playing</div><div className="ref-subtitle">{current.artist}</div></div>
        <button className="ref-icon-button" onClick={() => setPanel(panel ? null : 'menu')} aria-label="More options"><MoreVertical size={20}/></button>
      </div>
      {panel === 'menu' && <div className="ml-auto w-48 p-2 rounded-2xl bg-white/90 border border-white shadow-xl text-sm text-slate-700">
        <button className="w-full text-left p-3 rounded-xl hover:bg-slate-100" onClick={share}>Share track</button>
        <button className="w-full text-left p-3 rounded-xl hover:bg-slate-100" onClick={() => setPanel('queue')}>Open queue</button>
      </div>}
      <div className="px-1">
        <CircularSeek progress={progress} duration={player.duration} artwork={artwork} onSeek={player.seekTo} />
        <div className="ref-time-row"><span>{formatTime(progress)}</span><span>{formatTime(player.duration)}</span></div>
        <div className="px-1">
          <div className="flex items-start gap-3">
            <div className="min-w-0 flex-1"><h1 className="text-[25px] leading-tight font-extrabold tracking-[-.03em] truncate">{current.title}</h1><p className="text-[14px] text-[#707487] mt-1 truncate">{current.artist}</p></div>
            <button className="ref-icon-button shrink-0" onClick={toggleLike} aria-label={isLiked ? 'Unlike' : 'Like'}><Heart size={19} className={isLiked ? 'fill-[#2f5cff] text-[#2f5cff]' : ''}/></button>
          </div>
        </div>
        <div className="ref-tool-row mt-5">
          <button className="ref-tool" onClick={() => navigate('/equalizer')} aria-label="Equalizer"><SlidersHorizontal size={18}/></button>
          <button className="ref-tool" onClick={() => setPanel('queue')} aria-label="Queue"><ListMusic size={18}/></button>
          <button className="ref-tool" onClick={() => setPanel('lyrics')} aria-label="Lyrics"><Music2 size={18}/></button>
          <button className="ref-tool" onClick={share} aria-label="Share"><Share2 size={18}/></button>
        </div>
        <div className="ref-wave" aria-hidden="true">{wave.map((h, i) => <i key={i} style={{height:`${h}px`}} />)}</div>
        <div className="ref-controls">
          <button className={`ref-control ${player.shuffle ? 'text-[#2f5cff]' : ''}`} onClick={player.toggleShuffle} aria-label="Shuffle"><Shuffle size={19}/></button>
          <button className="ref-control" onClick={player.playPrevious} aria-label="Previous"><SkipBack size={23}/></button>
          <button className="ref-control main" onClick={player.togglePlay} aria-label={player.isPlaying ? 'Pause' : 'Play'}>{player.isPlaying ? <span className="text-xl">Ⅱ</span> : <span className="text-xl ml-0.5">▶</span>}</button>
          <button className="ref-control" onClick={player.playNext} aria-label="Next"><SkipForward size={23}/></button>
          <button className={`ref-control ${player.repeatMode !== 'off' ? 'text-[#2f5cff]' : ''}`} onClick={player.cycleRepeat} aria-label="Repeat">{player.repeatMode === 'one' ? <Repeat1 size={19}/> : <Repeat size={19}/>}</button>
        </div>
        <div className="flex items-center justify-between text-[11px] text-[#85899a] px-1 pb-5">
          <span>{current.source === 'local' ? 'On this device' : 'YouTube Music'}</span>
          <span>{player.queueIndex >= 0 ? `${player.queueIndex + 1} of ${player.queue.length}` : ''}</span>
        </div>
        {panel === 'queue' && <div className="rounded-3xl bg-white/70 border border-white/80 p-3 shadow-xl mb-6"><QueuePanel embedded onClose={() => setPanel(null)} /></div>}
        {panel === 'lyrics' && <div className="rounded-3xl bg-white/70 border border-white/80 p-5 shadow-xl mb-6"><div className="font-bold mb-2">Lyrics</div><p className="text-sm text-[#707487]">Open the lyrics panel to view available lyrics for this track.</p></div>}
      </div>
    </div>
  )
}
