import { useEffect, useMemo, useRef, useState } from 'react'
import {
  X, Play, Pause, SkipBack, SkipForward, Shuffle, Repeat, Repeat1,
  Heart, ListMusic, Moon, Volume2, VolumeX, Plus, Share2, Clock3,
  ChevronDown, Music2, Radio
} from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'
import { useAuth } from '@/context/AuthContext'
import { likeSong, unlikeSong } from '@/lib/library'
import { useLikedSongs } from '@/hooks/useLibraryData'
import { formatTime } from '@/lib/timeFormat'
import { getLyrics } from '@/lib/musicApi'
import QueuePanel from '@/components/QueuePanel'

// Real lyrics fetched from the backend (ytmusicapi) — see useLyrics below.
// Local files never have lyrics available (no metadata source for them);
// YouTube tracks may or may not, depending on what YouTube Music has.
function useLyrics(track) {
  const [state, setState] = useState({ status: 'idle', data: null })

  useEffect(() => {
    if (!track || track.source !== 'youtube') {
      setState({ status: 'unavailable', data: null })
      return
    }
    let cancelled = false
    setState({ status: 'loading', data: null })
    getLyrics(track.id)
      .then((result) => {
        if (cancelled) return
        setState(
          result?.available
            ? { status: 'available', data: result }
            : { status: 'unavailable', data: null }
        )
      })
      .catch(() => {
        if (!cancelled) setState({ status: 'error', data: null })
      })
    return () => {
      cancelled = true
    }
  }, [track?.id, track?.source])

  return state
}

export default function NowPlaying() {
  const player = usePlayer()
  const { user } = useAuth()
  const liked = useLikedSongs(user?.uid)
  const [tab, setTab] = useState('lyrics')
  const [sleepOpen, setSleepOpen] = useState(false)
  const current = player.currentTrack
  const isLiked = !!current && liked.some((t) => t.id === current.id)
  const lyricsState = useLyrics(current)
  const syncedLines = useMemo(() => {
    if (lyricsState.status !== 'available' || !lyricsState.data?.synced) return []
    return lyricsState.data.lines.map((l) => ({ time: l.startTimeSeconds, text: l.text }))
  }, [lyricsState])
  const activeLyric = useMemo(() => {
    // Sorted timestamps allow O(log n) lookup instead of scanning every line.
    let lo = 0, hi = syncedLines.length - 1, idx = -1
    while (lo <= hi) {
      const mid = (lo + hi) >> 1
      if (syncedLines[mid].time <= player.progress) { idx = mid; lo = mid + 1 }
      else hi = mid - 1
    }
    return idx
  }, [syncedLines, player.progress])

  if (!current || !player.nowPlayingOpen) return null

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
    <div className="fixed inset-0 z-[80] bg-black/95 backdrop-blur-xl flex flex-col">
      <header className="h-16 shrink-0 flex items-center justify-between px-5 md:px-8 border-b border-white/10">
        <button onClick={player.closeNowPlaying} className="p-2 rounded-full hover:bg-white/10" aria-label="Close Now Playing">
          <ChevronDown size={26} />
        </button>
        <div className="text-center">
          <p className="text-[11px] uppercase tracking-[0.2em] text-text-subdued">Now Playing</p>
          <p className="text-xs text-text-muted truncate max-w-[240px]">{current.artist}</p>
        </div>
        <button onClick={share} className="p-2 rounded-full hover:bg-white/10" aria-label="Share song"><Share2 size={20} /></button>
      </header>

      <div className="flex-1 min-h-0 overflow-y-auto px-5 py-6 md:px-10">
        <div className="max-w-6xl mx-auto grid lg:grid-cols-[minmax(320px,520px)_1fr] gap-8 xl:gap-14 items-center min-h-full">
          <section className="flex flex-col items-center lg:items-start">
            <div className="relative w-full max-w-[460px] aspect-square rounded-2xl overflow-hidden shadow-[0_30px_90px_rgba(0,0,0,.65)]">
              {current.thumbnail ? <img src={current.thumbnail} alt="" className="w-full h-full object-cover" /> : <div className="w-full h-full bg-surface-elevated flex items-center justify-center"><Music2 size={80} /></div>}
            </div>
            <div className="w-full max-w-[460px] mt-6 flex items-start gap-4">
              <div className="min-w-0 flex-1">
                <h1 className="text-2xl md:text-3xl font-bold truncate">{current.title}</h1>
                <p className="text-text-muted text-lg truncate mt-1">{current.artist}</p>
              </div>
              <button onClick={toggleLike} className="p-2 mt-1" aria-label={isLiked ? 'Unlike' : 'Like'}>
                <Heart size={26} className={isLiked ? 'fill-brand text-brand' : 'text-text-muted'} />
              </button>
            </div>

            <div className="w-full max-w-[460px] mt-5">
              <input type="range" min="0" max={player.duration || 0} value={Math.min(player.progress, player.duration || 0)} onChange={(e) => player.seekTo(Number(e.target.value))} className="w-full accent-white" aria-label="Seek" />
              <div className="flex justify-between text-[11px] text-text-subdued mt-1"><span>{formatTime(player.progress)}</span><span>{formatTime(player.duration)}</span></div>
            </div>

            <div className="w-full max-w-[460px] flex items-center justify-center gap-7 md:gap-9 mt-5">
              <button onClick={player.toggleShuffle} className={player.shuffle ? 'text-brand' : 'text-text-muted'} aria-label="Shuffle"><Shuffle size={20}/></button>
              <button onClick={player.playPrevious} aria-label="Previous"><SkipBack size={28}/></button>
              <button onClick={player.togglePlay} aria-label={player.isPlaying ? 'Pause' : 'Play'} className="w-16 h-16 rounded-full bg-white text-black flex items-center justify-center hover:scale-105 transition-transform">
                {player.isPlaying ? <Pause size={27}/> : <Play size={27} className="ml-1"/>}
              </button>
              <button onClick={player.playNext} aria-label="Next"><SkipForward size={28}/></button>
              <button onClick={player.cycleRepeat} className={player.repeatMode !== 'off' ? 'text-brand' : 'text-text-muted'} aria-label="Repeat">
                {player.repeatMode === 'one' ? <Repeat1 size={20}/> : <Repeat size={20}/>} 
              </button>
            </div>

            <div className="w-full max-w-[460px] flex items-center gap-3 mt-6">
              <Volume2 size={16} className="text-text-muted shrink-0" />
              <input type="range" min="0" max="100" value={player.muted ? 0 : player.volume} onChange={(e) => player.changeVolume(Number(e.target.value))} className="flex-1 accent-white" aria-label="Volume" />
              <button onClick={player.toggleMute} aria-label="Mute"><VolumeX size={17} className="text-text-muted"/></button>
            </div>

            <div className="flex items-center gap-2 mt-5 flex-wrap justify-center">
              <button onClick={() => setTab('queue')} className="np-pill"><ListMusic size={15}/> Queue</button>
              <button onClick={() => setSleepOpen(v => !v)} className={`np-pill ${player.sleepTimerSeconds ? 'text-brand' : ''}`}><Moon size={15}/> Sleep</button>
              <button onClick={() => setTab('lyrics')} className="np-pill"><Music2 size={15}/> Lyrics</button>
              <button onClick={share} className="np-pill"><Share2 size={15}/> Share</button>
            </div>
            {sleepOpen && <div className="mt-3 bg-surface-elevated border border-border rounded-xl p-2 w-full max-w-[320px]">
              <p className="text-xs font-semibold text-text-muted px-3 py-2">Sleep timer</p>
              {[5,10,15,30,60].map(m => <button key={m} onClick={() => { player.setSleepTimer(m*60); setSleepOpen(false) }} className="w-full text-left px-3 py-2 rounded-lg hover:bg-surface-hover text-sm">{m} minutes</button>)}
              <button onClick={() => { player.setSleepTimer(Math.max(1, player.duration-player.progress)); setSleepOpen(false) }} className="w-full text-left px-3 py-2 rounded-lg hover:bg-surface-hover text-sm">End of track</button>
              {player.sleepTimerSeconds && <button onClick={() => {player.clearSleepTimer(); setSleepOpen(false)}} className="w-full text-left px-3 py-2 rounded-lg hover:bg-surface-hover text-sm text-red-300">Cancel timer</button>}
            </div>}
          </section>

          <section className="min-h-[420px] bg-white/[0.035] border border-white/10 rounded-2xl overflow-hidden flex flex-col">
            <div className="flex items-center border-b border-white/10 p-2 gap-1">
              {['lyrics','queue','about'].map(name => <button key={name} onClick={() => setTab(name)} className={`px-4 py-2 rounded-lg text-sm font-semibold capitalize ${tab === name ? 'bg-white/10 text-white' : 'text-text-muted hover:text-white'}`}>{name}</button>)}
            </div>
            <div className="flex-1 min-h-0 overflow-y-auto p-6">
              {tab === 'lyrics' && <>
                <div className="flex items-center gap-2 text-text-muted mb-5"><Radio size={16}/> Lyrics</div>
                {lyricsState.status === 'loading' && (
                  <div className="h-full min-h-[320px] flex flex-col items-center justify-center text-center">
                    <div className="w-8 h-8 border-2 border-text-subdued border-t-white rounded-full animate-spin mb-4" />
                    <p className="text-sm text-text-muted">Looking up lyrics…</p>
                  </div>
                )}
                {lyricsState.status === 'error' && (
                  <div className="h-full min-h-[320px] flex flex-col items-center justify-center text-center">
                    <Music2 size={42} className="text-text-subdued mb-4"/>
                    <h3 className="font-bold mb-2">Couldn't load lyrics</h3>
                    <p className="text-sm text-text-muted max-w-sm mb-4">Something went wrong reaching the lyrics service.</p>
                    <button onClick={() => setTab('lyrics')} className="np-action">Retry</button>
                  </div>
                )}
                {lyricsState.status === 'unavailable' && (
                  <div className="h-full min-h-[320px] flex flex-col items-center justify-center text-center">
                    <Music2 size={42} className="text-text-subdued mb-4"/>
                    <h3 className="font-bold mb-2">Lyrics unavailable</h3>
                    <p className="text-sm text-text-muted max-w-sm">
                      {current.source === 'local'
                        ? "Lyrics aren't available for local files."
                        : 'Lyrics unavailable for this track.'}
                    </p>
                  </div>
                )}
                {lyricsState.status === 'available' && lyricsState.data.synced && (
                  <div className="space-y-3 text-lg md:text-2xl font-bold leading-tight">
                    {syncedLines.map((line, i) => (
                      <button key={`${line.time}-${i}`} onClick={() => player.seekTo(line.time)} className={`block w-full text-left transition-all ${i === activeLyric ? 'text-white scale-[1.01]' : 'text-text-subdued hover:text-text'}`}>{line.text}</button>
                    ))}
                  </div>
                )}
                {lyricsState.status === 'available' && !lyricsState.data.synced && (
                  <p className="whitespace-pre-line text-lg md:text-xl font-semibold leading-relaxed text-text-muted">
                    {lyricsState.data.text}
                  </p>
                )}
              </>}
              {tab === 'queue' && <QueuePanel embedded onClose={() => setTab('lyrics')} />}
              {tab === 'about' && <div className="space-y-6"><div><p className="text-xs uppercase tracking-wider text-text-subdued">Track</p><h3 className="text-xl font-bold mt-1">{current.title}</h3><p className="text-text-muted">{current.artist}</p></div><div className="grid sm:grid-cols-2 gap-3"><Info label="Source" value={current.source === 'local' ? 'Local device' : 'YouTube Music'} /><Info label="Duration" value={formatTime(player.duration)} /><Info label="Queue position" value={player.queueIndex >= 0 ? `${player.queueIndex + 1} / ${player.queue.length}` : '—'} /><Info label="Repeat" value={player.repeatMode} /></div><button onClick={() => player.enqueue(current)} className="np-action"><Plus size={16}/> Add another copy to queue</button></div>}
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}

function Info({label,value}) { return <div className="bg-white/[0.04] rounded-xl p-4"><p className="text-xs text-text-subdued">{label}</p><p className="font-semibold mt-1 capitalize">{value}</p></div> }
