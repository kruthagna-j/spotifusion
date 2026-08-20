import { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react'
import { useAuth } from './AuthContext'
import { recordPlay } from '@/lib/library'

const PlayerContext = createContext(null)

// Loads the YouTube IFrame Player API script once.
function useYouTubeApi() {
  const [ready, setReady] = useState(!!window.YT?.Player)
  useEffect(() => {
    if (window.YT?.Player) {
      setReady(true)
      return
    }
    const prev = window.onYouTubeIframeAPIReady
    window.onYouTubeIframeAPIReady = () => {
      prev?.()
      setReady(true)
    }
    if (!document.getElementById('yt-iframe-api')) {
      const tag = document.createElement('script')
      tag.id = 'yt-iframe-api'
      tag.src = 'https://www.youtube.com/iframe_api'
      document.head.appendChild(tag)
    }
  }, [])
  return ready
}

export function PlayerProvider({ children }) {
  const apiReady = useYouTubeApi()
  const { user } = useAuth()
  const ytPlayerRef = useRef(null)
  const containerRef = useRef(null)
  const progressTimer = useRef(null)

  const [queue, setQueue] = useState([]) // array of track objects
  const [queueIndex, setQueueIndex] = useState(-1)
  const [isPlaying, setIsPlaying] = useState(false)
  const [progress, setProgress] = useState(0) // seconds
  const [duration, setDuration] = useState(0)
  const [volume, setVolume] = useState(70)
  const [muted, setMuted] = useState(false)
  const [shuffle, setShuffle] = useState(false)
  const [repeatMode, setRepeatMode] = useState('off') // off | all | one

  const currentTrack = queueIndex >= 0 ? queue[queueIndex] : null

  // Create the (invisible) YT player once the API script is ready.
  useEffect(() => {
    if (!apiReady || ytPlayerRef.current) return
    if (!document.getElementById('yt-player-mount')) {
      const div = document.createElement('div')
      div.id = 'yt-player-mount'
      div.style.position = 'fixed'
      div.style.bottom = '0'
      div.style.left = '0'
      div.style.width = '1px'
      div.style.height = '1px'
      div.style.opacity = '0'
      div.style.pointerEvents = 'none'
      document.body.appendChild(div)
    }
    ytPlayerRef.current = new window.YT.Player('yt-player-mount', {
      height: '1',
      width: '1',
      playerVars: { playsinline: 1, controls: 0, disablekb: 1 },
      events: {
        onReady: (e) => e.target.setVolume(volume),
        onStateChange: (e) => {
          const YTState = window.YT.PlayerState
          if (e.data === YTState.PLAYING) setIsPlaying(true)
          if (e.data === YTState.PAUSED) setIsPlaying(false)
          if (e.data === YTState.ENDED) handleEnded()
        },
      },
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiReady])

  function handleEnded() {
    if (repeatMode === 'one') {
      ytPlayerRef.current.seekTo(0)
      ytPlayerRef.current.playVideo()
      return
    }
    playNext()
  }

  // Poll progress while playing
  useEffect(() => {
    clearInterval(progressTimer.current)
    if (isPlaying) {
      progressTimer.current = setInterval(() => {
        const p = ytPlayerRef.current?.getCurrentTime?.() || 0
        const d = ytPlayerRef.current?.getDuration?.() || 0
        setProgress(p)
        setDuration(d)
      }, 500)
    }
    return () => clearInterval(progressTimer.current)
  }, [isPlaying])

  const loadAndPlay = useCallback(
    (index, list = queue) => {
      const track = list[index]
      if (!track || !ytPlayerRef.current?.loadVideoById) return
      ytPlayerRef.current.loadVideoById(track.id)
      ytPlayerRef.current.setVolume(muted ? 0 : volume)
      setQueueIndex(index)
      setIsPlaying(true)
      setProgress(0)
      if (user) recordPlay(user.uid, track)
    },
    [queue, muted, volume, user]
  )

  // Public API -----------------------------------------------------------

  function playTrack(track, contextTracks = null) {
    const list = contextTracks || [track]
    const idx = list.findIndex((t) => t.id === track.id)
    setQueue(list)
    loadAndPlay(idx === -1 ? 0 : idx, list)
  }

  function togglePlay() {
    if (!ytPlayerRef.current) return
    if (isPlaying) ytPlayerRef.current.pauseVideo()
    else ytPlayerRef.current.playVideo()
  }

  function playNext() {
    if (!queue.length) return
    let nextIndex
    if (shuffle) {
      nextIndex = Math.floor(Math.random() * queue.length)
    } else {
      nextIndex = queueIndex + 1
      if (nextIndex >= queue.length) {
        if (repeatMode === 'all') nextIndex = 0
        else return setIsPlaying(false)
      }
    }
    loadAndPlay(nextIndex)
  }

  function playPrevious() {
    if (!queue.length) return
    // Restart current track if we're more than 3s in (Spotify behavior)
    if (progress > 3) {
      ytPlayerRef.current.seekTo(0)
      return
    }
    const prevIndex = queueIndex - 1 < 0 ? (repeatMode === 'all' ? queue.length - 1 : 0) : queueIndex - 1
    loadAndPlay(prevIndex)
  }

  function seekTo(seconds) {
    ytPlayerRef.current?.seekTo(seconds, true)
    setProgress(seconds)
  }

  function changeVolume(v) {
    setVolume(v)
    setMuted(false)
    ytPlayerRef.current?.setVolume(v)
  }

  function toggleMute() {
    const next = !muted
    setMuted(next)
    ytPlayerRef.current?.setVolume(next ? 0 : volume)
  }

  function enqueue(track) {
    setQueue((q) => [...q, track])
  }

  const value = {
    queue,
    queueIndex,
    currentTrack,
    isPlaying,
    progress,
    duration,
    volume,
    muted,
    shuffle,
    repeatMode,
    playerReady: apiReady,
    playTrack,
    togglePlay,
    playNext,
    playPrevious,
    seekTo,
    changeVolume,
    toggleMute,
    toggleShuffle: () => setShuffle((s) => !s),
    cycleRepeat: () =>
      setRepeatMode((m) => (m === 'off' ? 'all' : m === 'all' ? 'one' : 'off')),
    enqueue,
  }

  return <PlayerContext.Provider value={value}>{children}</PlayerContext.Provider>
}

export function usePlayer() {
  const ctx = useContext(PlayerContext)
  if (!ctx) throw new Error('usePlayer must be used within PlayerProvider')
  return ctx
}
