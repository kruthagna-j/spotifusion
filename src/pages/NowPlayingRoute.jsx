import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ChevronDown, Heart, ListMusic, Moon, Music2, Pause, Play, Repeat, Repeat1, Share2, Shuffle, SkipBack, SkipForward, Volume2, VolumeX, Plus } from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'
import { useAuth } from '@/context/AuthContext'
import { useLikedSongs } from '@/hooks/useLibraryData'
import { likeSong, unlikeSong } from '@/lib/library'
import { getLyrics } from '@/lib/musicApi'
import { formatTime } from '@/lib/timeFormat'
import QueuePanel from '@/components/QueuePanel'

function Lyrics({ track, player }) {
  const [state, setState] = useState({ status: 'loading', data: null })
  useEffect(() => {
    let cancelled = false
    if (!track || track.source !== 'youtube') return setState({ status: 'unavailable', data: null })
    setState({ status: 'loading', data: null })
    getLyrics(track.id).then(data => {
      if (!cancelled) setState(data?.available ? { status: 'ready', data } : { status: 'unavailable', data: null })
    }).catch(() => !cancelled && setState({ status: 'error', data: null }))
    return () => { cancelled = true }
  }, [track?.id, track?.source])
  const lines = useMemo(() => state.data?.synced ? state.data.lines : [], [state.data])
  const active = lines.reduce((a, l, i) => l.startTimeSeconds <= player.progress ? i : a, -1)
  if (state.status === 'loading') return <div className="h-full grid place-items-center text-text-muted">Loading lyrics…</div>
  if (state.status === 'error') return <div className="h-full grid place-items-center text-text-muted text-center"><div><Music2 size={38} className="mx-auto mb-3"/><p>Lyrics couldn't be loaded.</p><p className="text-xs mt-1">Try again later.</p></div></div>
  if (state.status === 'unavailable') return <div className="h-full grid place-items-center text-text-muted text-center"><div><Music2 size={38} className="mx-auto mb-3"/><p>Lyrics aren't available for this track.</p></div></div>
  if (lines.length) return <div className="space-y-4 py-4">{lines.map((line, i) => <p key={`${line.startTimeSeconds}-${i}`} className={`text-xl md:text-2xl font-black transition-all ${i === active ? 'text-white scale-[1.01]' : 'text-text-subdued'}`}>{line.text || '♪'}</p>)}</div>
  return <div className="whitespace-pre-wrap text-lg leading-8 text-text-muted">{state.data?.lyrics || 'Lyrics are unavailable.'}</div>
}

export default function NowPlayingRoute() {
  const player = usePlayer(); const { user } = useAuth(); const liked = useLikedSongs(user?.uid)
  const [tab, setTab] = useState('lyrics'); const [sleep, setSleep] = useState(false)
  const current = player.currentTrack
  useEffect(() => { if (current) player.openNowPlaying(); return () => player.closeNowPlaying() }, [current])
  if (!current) return <div className="min-h-full grid place-items-center p-8 text-center"><div><Music2 size={52} className="mx-auto text-text-subdued mb-4"/><h1 className="text-3xl font-black">Nothing is playing</h1><p className="text-text-muted mt-2">Choose a song to open the full player.</p><Link to="/search" className="inline-flex mt-6 bg-brand text-black font-black px-5 py-3 rounded-full">Find music</Link></div></div>
  const isLiked = liked.some(t => t.id === current.id)
  const toggleLike = () => user && (isLiked ? unlikeSong(user.uid, current.id) : likeSong(user.uid, current))
  const share = async () => { try { if (navigator.share) await navigator.share({ title: current.title, text: `${current.title} — ${current.artist}` }); else await navigator.clipboard.writeText(`${current.title} — ${current.artist}`) } catch {} }
  return <div className="min-h-full bg-gradient-to-b from-white/[.06] via-transparent to-bg p-4 md:p-8">
    <div className="max-w-7xl mx-auto">
      <div className="flex items-center justify-between mb-5"><Link to="/" className="icon-button"><ChevronDown size={22}/></Link><div className="text-center"><p className="text-[10px] uppercase tracking-[.25em] text-brand font-black">Now Playing</p><p className="text-xs text-text-muted">{current.album || 'Spotifusion'}</p></div><button className="icon-button" onClick={share}><Share2 size={18}/></button></div>
      <div className="grid xl:grid-cols-[minmax(360px,520px)_1fr] gap-8 items-start">
        <section className="sf-panel p-5 md:p-7"><div className="aspect-square rounded-2xl overflow-hidden bg-surface-highlight shadow-2xl">{current.thumbnail ? <img src={current.thumbnail} alt="" className="w-full h-full object-cover"/> : <div className="w-full h-full grid place-items-center"><Music2 size={72}/></div>}</div><div className="flex items-start gap-3 mt-6"><div className="min-w-0 flex-1"><h1 className="text-2xl md:text-3xl font-black truncate">{current.title}</h1><p className="text-text-muted mt-1 truncate">{current.artist}</p></div><button onClick={toggleLike} className="p-2"><Heart size={24} className={isLiked ? 'fill-brand text-brand':'text-text-muted'}/></button></div><div className="mt-5"><input type="range" min="0" max={player.duration || 0} value={Math.min(player.progress, player.duration || 0)} onChange={e=>player.seekTo(Number(e.target.value))} className="w-full accent-white"/><div className="flex justify-between text-[11px] text-text-subdued mt-1"><span>{formatTime(player.progress)}</span><span>{formatTime(player.duration)}</span></div></div><div className="flex items-center justify-center gap-7 mt-6"><button onClick={player.toggleShuffle} className={player.shuffle?'text-brand':'text-text-muted'}><Shuffle size={20}/></button><button onClick={player.playPrevious}><SkipBack size={26}/></button><button onClick={player.togglePlay} className="w-16 h-16 rounded-full bg-white text-black grid place-items-center">{player.isPlaying?<Pause size={26}/>:<Play size={26} className="ml-1"/>}</button><button onClick={player.playNext}><SkipForward size={26}/></button><button onClick={player.cycleRepeat} className={player.repeatMode!=='off'?'text-brand':'text-text-muted'}>{player.repeatMode==='one'?<Repeat1 size={20}/>:<Repeat size={20}/>}</button></div><div className="flex items-center gap-3 mt-6"><button onClick={player.toggleMute}>{player.muted?<VolumeX size={18}/>:<Volume2 size={18}/>}</button><input type="range" min="0" max="100" value={player.muted?0:player.volume} onChange={e=>player.changeVolume(Number(e.target.value))} className="flex-1 accent-white"/></div><div className="flex flex-wrap justify-center gap-2 mt-5"><button className="sf-chip" onClick={()=>setTab('queue')}><ListMusic size={14}/> Queue ({player.queue.length})</button><button className="sf-chip" onClick={()=>setTab('lyrics')}><Music2 size={14}/> Lyrics</button><button className={`sf-chip ${player.sleepTimerSeconds?'text-brand':''}`} onClick={()=>setSleep(v=>!v)}><Moon size={14}/> Sleep</button><button className="sf-chip" onClick={share}><Share2 size={14}/> Share</button></div>{sleep&&<div className="mt-3 sf-panel p-2">{[5,10,15,30,60].map(m=><button key={m} onClick={()=>{player.setSleepTimer(m*60);setSleep(false)}} className="w-full text-left p-2 rounded hover:bg-white/10 text-sm">{m} minutes</button>)}<button onClick={()=>{player.setSleepTimer(Math.max(1,player.duration-player.progress));setSleep(false)}} className="w-full text-left p-2 rounded hover:bg-white/10 text-sm">End of track</button>{player.sleepTimerSeconds&&<button onClick={player.clearSleepTimer} className="w-full text-left p-2 rounded hover:bg-white/10 text-sm text-red-300">Cancel timer</button>}</div>}</section>
        <section className="sf-panel min-h-[520px] overflow-hidden"><div className="flex gap-1 p-2 border-b border-border">{['lyrics','queue','about'].map(x=><button key={x} onClick={()=>setTab(x)} className={`px-4 py-2 rounded-lg text-sm font-bold capitalize ${tab===x?'bg-white/10 text-white':'text-text-muted hover:text-white'}`}>{x}</button>)}</div><div className="p-5 md:p-8 min-h-[460px]">{tab==='lyrics'&&<Lyrics track={current} player={player}/>} {tab==='queue'&&<div><h2 className="text-xl font-black mb-4">Queue</h2><QueuePanel embedded onClose={() => setTab('lyrics')} /></div>} {tab==='about'&&<div><h2 className="text-xl font-black">{current.title}</h2><p className="text-text-muted mt-2">{current.artist} · {current.album || 'Single'}</p><div className="grid sm:grid-cols-2 gap-3 mt-6"><div className="sf-panel p-4"><p className="text-xs text-text-subdued">Source</p><p className="font-bold mt-1">{current.source === 'local' ? 'Local Music' : 'YouTube Music'}</p></div><div className="sf-panel p-4"><p className="text-xs text-text-subdued">Queue position</p><p className="font-bold mt-1">{player.queueIndex + 1} of {player.queue.length}</p></div></div></div>}</div></section>
      </div>
    </div>
  </div>
}
