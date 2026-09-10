import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  ChevronDown, Heart, Shuffle, Repeat, Repeat1, Play, Pause,
  SkipBack, SkipForward, Volume2, VolumeX, SlidersHorizontal,
  Sparkles, Moon, Share2, ListMusic,
} from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'
import { useAuth } from '@/context/AuthContext'
import { likeSong, unlikeSong } from '@/lib/library'
import { useLikedSongs } from '@/hooks/useLibraryData'
import { formatTime } from '@/lib/timeFormat'

export default function NowPlaying({ route = false }) {
  const navigate = useNavigate()
  const player = usePlayer()
  const { user } = useAuth()
  const liked = useLikedSongs(user?.uid)
  const [sleepOpen, setSleepOpen] = useState(false)
  const current = player.currentTrack
  const isLiked = !!current && liked.some((track) => track.id === current.id)

  if (!current || (!route && !player.nowPlayingOpen)) return null

  const toggleLike = () => {
    if (!user) return
    if (isLiked) unlikeSong(user.uid, current.id)
    else likeSong(user.uid, current)
  }

  const share = async () => {
    const text = `${current.title} — ${current.artist}`
    try {
      if (navigator.share) await navigator.share({ title: current.title, text })
      else await navigator.clipboard.writeText(text)
    } catch { /* user cancelled or clipboard unavailable */ }
  }

  const dismiss = () => {
    if (route) navigate(-1)
    else player.closeNowPlaying()
  }

  return (
    <div data-testid="player-screen-view" className={route ? 'sf-player-route sf-emergent-player' : 'sf-now-playing-overlay sf-emergent-player'}>
      <div className="sf-emergent-player__aura" style={current.thumbnail ? { backgroundImage: `url(${current.thumbnail})` } : undefined} aria-hidden="true" />
      <header className="sf-emergent-player__header">
        <button type="button" onClick={dismiss} className="sf-emergent-icon-button" aria-label="Minimize player"><ChevronDown size={22} /></button>
        <div className="sf-emergent-player__heading">
          <span>Playing From Spotifusion</span>
          <strong>Now Playing</strong>
        </div>
        <button type="button" onClick={() => navigate('/lyrics')} className="sf-emergent-icon-button sf-emergent-icon-button--accent" aria-label="Open lyrics and queue"><Sparkles size={18} /></button>
      </header>

      <div className="sf-emergent-player__scroll">
        <div className="sf-emergent-player__art-wrap">
          <div className={`sf-emergent-player__art ${player.isPlaying ? 'is-playing' : ''}`}>
            {current.thumbnail ? <img src={current.thumbnail} alt={current.title} data-testid="player-album-artwork" /> : <div className="sf-emergent-player__fallback">♪</div>}
          </div>
          <Waveform isPlaying={player.isPlaying} />
        </div>

        <div className="sf-emergent-player__details">
          <div className="sf-emergent-player__title-row">
            <div className="sf-emergent-player__track-copy">
              <h1 data-testid="player-track-title">{current.title}</h1>
              <p data-testid="player-track-artist">{current.artist}</p>
            </div>
            <button type="button" onClick={toggleLike} className={`sf-emergent-like ${isLiked ? 'is-liked' : ''}`} aria-label={isLiked ? 'Unlike track' : 'Like track'}><Heart size={22} fill={isLiked ? 'currentColor' : 'none'} /></button>
          </div>

          <div className="sf-emergent-seek">
            <input type="range" min="0" max={player.duration || 100} value={Math.min(player.progress, player.duration || 0)} onChange={(event) => player.seekTo(Number(event.target.value))} aria-label="Seek" />
            <div><span>{formatTime(player.progress)}</span><span>{formatTime(player.duration)}</span></div>
          </div>

          <div className="sf-emergent-transport">
            <button type="button" onClick={player.toggleShuffle} className={player.shuffle ? 'is-active' : ''} aria-label="Toggle shuffle"><Shuffle size={20} /></button>
            <button type="button" onClick={player.playPrevious} aria-label="Previous track"><SkipBack size={26} /></button>
            <button type="button" onClick={player.togglePlay} className="sf-emergent-play" aria-label={player.isPlaying ? 'Pause' : 'Play'}>{player.isPlaying ? <Pause size={28} fill="currentColor" /> : <Play size={28} fill="currentColor" />}</button>
            <button type="button" onClick={player.playNext} aria-label="Next track"><SkipForward size={26} /></button>
            <button type="button" onClick={player.cycleRepeat} className={player.repeatMode !== 'off' ? 'is-active' : ''} aria-label="Toggle repeat">{player.repeatMode === 'one' ? <Repeat1 size={20} /> : <Repeat size={20} />}</button>
          </div>

          <div className="sf-emergent-volume">
            <Volume2 size={16} aria-hidden="true" />
            <input type="range" min="0" max="100" value={player.muted ? 0 : player.volume} onChange={(event) => player.changeVolume(Number(event.target.value))} aria-label="Volume" />
            <button type="button" onClick={player.toggleMute} aria-label={player.muted ? 'Unmute' : 'Mute'}>{player.muted ? <VolumeX size={17} /> : <Volume2 size={17} />}</button>
          </div>

          <div className="sf-emergent-utilities">
            <button type="button" onClick={() => navigate('/queue')}><ListMusic size={15} /> Queue</button>
            <button type="button" onClick={() => setSleepOpen((open) => !open)} className={player.sleepTimerSeconds ? 'is-active' : ''}><Moon size={15} /> Sleep</button>
            <button type="button" onClick={() => navigate('/lyrics')}><Sparkles size={15} /> Lyrics</button>
            <button type="button" onClick={share}><Share2 size={15} /> Share</button>
          </div>
          {sleepOpen && <SleepMenu player={player} onClose={() => setSleepOpen(false)} />}
        </div>
      </div>
    </div>
  )
}

function Waveform({ isPlaying }) {
  return <div className={`sf-emergent-waveform ${isPlaying ? 'is-playing' : ''}`} aria-label={isPlaying ? 'Audio playing' : 'Audio paused'}>{Array.from({ length: 22 }, (_, index) => <i key={index} style={{ '--bar': `${22 + ((index * 17) % 62)}%` }} />)}</div>
}

function SleepMenu({ player, onClose }) {
  return <div className="sf-emergent-sleep-menu"><strong>Sleep timer</strong>{[5, 10, 15, 30, 60].map((minutes) => <button key={minutes} onClick={() => { player.setSleepTimer(minutes * 60); onClose() }}>{minutes} minutes</button>)}{player.sleepTimerSeconds && <button className="is-danger" onClick={() => { player.clearSleepTimer(); onClose() }}>Cancel timer</button>}</div>
}
