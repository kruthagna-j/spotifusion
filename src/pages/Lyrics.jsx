import { useEffect, useMemo, useRef } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft, Music2, Pause, Play, SkipBack, SkipForward } from 'lucide-react'
import { usePlayer } from '@/context/PlayerContext'
import { parseSyncedLyrics } from '@/lib/lyrics'
import { formatTime } from '@/lib/timeFormat'

export default function Lyrics() {
  const player = usePlayer()
  const current = player.currentTrack
  const activeRef = useRef(null)
  const lines = useMemo(() => parseSyncedLyrics(current?.lyrics || current?.syncedLyrics || ''), [current])
  const activeIndex = useMemo(() => {
    let index = -1
    lines.forEach((line, i) => { if (line.time <= player.progress) index = i })
    return index
  }, [lines, player.progress])

  useEffect(() => {
    activeRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }, [activeIndex])

  if (!current) {
    return (
      <div className="sf-lyrics-empty">
        <Music2 size={44} aria-hidden="true" />
        <h1>Lyrics</h1>
        <p>Play a song to see synchronized lyrics.</p>
        <Link to="/search" className="sf-primary-button">Find music</Link>
      </div>
    )
  }

  return (
    <div className="sf-lyrics-route">
      <header className="sf-immersive-header">
        <Link to="/" className="sf-icon-button" aria-label="Back to home"><ArrowLeft size={20} /></Link>
        <div className="sf-immersive-header__title">
          <span>Lyrics</span>
          <small>{current.artist}</small>
        </div>
        <Link to="/player" className="sf-icon-button" aria-label="Open full player"><Music2 size={19} /></Link>
      </header>
      <div className="sf-lyrics-art" style={current.thumbnail ? { backgroundImage: `url(${current.thumbnail})` } : undefined} aria-hidden="true" />
      <div className="sf-lyrics-meta">
        <h1>{current.title}</h1>
        <p>{current.artist}</p>
      </div>
      <div className="sf-lyrics-scroll" aria-label="Synchronized lyrics">
        {lines.length ? lines.map((line, index) => (
          <button
            key={`${line.time}-${index}`}
            ref={index === activeIndex ? activeRef : null}
            type="button"
            title={`Jump to ${formatTime(line.time)}`}
            onClick={() => player.seekTo(line.time)}
            className={`sf-lyric-line ${index === activeIndex ? 'is-active' : index < activeIndex ? 'is-past' : ''}`}
          >
            {line.text}
          </button>
        )) : (
          <div className="sf-lyrics-no-data">
            <Music2 size={32} />
            <p>Synchronized lyrics are not available for this track.</p>
          </div>
        )}
      </div>
      <footer className="sf-lyrics-controls">
        <div className="sf-lyrics-controls__track">
          <span>{current.title}</span><small>{current.artist}</small>
        </div>
        <div className="sf-lyrics-controls__buttons">
          <button onClick={player.playPrevious} aria-label="Previous track"><SkipBack size={18} /></button>
          <button onClick={player.togglePlay} aria-label={player.isPlaying ? 'Pause' : 'Play'} className="sf-lyrics-play">
            {player.isPlaying ? <Pause size={18} /> : <Play size={18} className="ml-0.5" />}
          </button>
          <button onClick={player.playNext} aria-label="Next track"><SkipForward size={18} /></button>
        </div>
        <div className="sf-lyrics-progress"><input type="range" min="0" max={player.duration || 0} value={Math.min(player.progress, player.duration || 0)} onChange={(event) => player.seekTo(Number(event.target.value))} aria-label="Lyrics progress" /><div><span>{formatTime(player.progress)}</span><span>{formatTime(player.duration)}</span></div></div>
      </footer>
    </div>
  )
}
