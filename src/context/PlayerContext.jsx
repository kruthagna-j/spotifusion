import { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react'
import { useAuth } from './AuthContext'
import { recordPlay } from '@/lib/library'
import { getLocalTrackBlob } from '@/lib/localLibrary'

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
  const localAudioRef = useRef(null)
  const currentObjectUrl = useRef(null)
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
  const isLocal = currentTrack?.source === 'local'

  // Keep the latest handler in a ref so the <audio> "ended" listener (added
  // once) always calls the current repeatMode/queue-aware logic.
  const handleEndedRef = useRef(() => {})

  // Create the (invisible) HTML5 audio element used for local device files.
  useEffect(() => {
    const audio = new Audio()
    audio.preload = 'auto'
    localAudioRef.current = audio

    const onEnded = () => handleEndedRef.current()
    const onPlay = () => setIsPlaying(true)
    const onPause = () => setIsPlaying(false)
    audio.addEventListener('ended', onEnded)
    audio.addEventListener('play', onPlay)
    audio.addEventListener('pause', onPause)

    return () => {
      audio.removeEventListener('ended', onEnded)
      audio.removeEventListener('play', onPlay)
      audio.removeEventListener('pause', onPause)
      audio.pause()
      if (currentObjectUrl.current) URL.revokeObjectURL(currentObjectUrl.current)
    }
  }, [])

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
          if (e.data === YTState.ENDED) handleEndedRef.current()
        },
      },
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiReady])

  function handleEnded() {
    if (repeatMode === 'one') {
      if (isLocal) {
        localAudioRef.current.currentTime = 0
        localAudioRef.current.play()
      } else {
        ytPlayerRef.current.seekTo(0)
        ytPlayerRef.current.playVideo()
      }
      return
    }
    playNext()
  }
  handleEndedRef.current = handleEnded

  // Poll progress while playing (works for both sources)
  useEffect(() => {
    clearInterval(progressTimer.current)
    if (isPlaying) {
      progressTimer.current = setInterval(() => {
        if (isLocal) {
          setProgress(localAudioRef.current?.currentTime || 0)
          setDuration(localAudioRef.current?.duration || 0)
        } else {
          const p = ytPlayerRef.current?.getCurrentTime?.() || 0
          const d = ytPlayerRef.current?.getDuration?.() || 0
          setProgress(p)
          setDuration(d)
        }
      }, 500)
    }
    return () => clearInterval(progressTimer.current)
  }, [isPlaying, isLocal])

  const loadAndPlay = useCallback(
    async (index, list = queue) => {
      const track = list[index]
      if (!track) return
      setQueueIndex(index)
      setProgress(0)
      if (user) recordPlay(user.uid, track)

      if (track.source === 'local') {
        ytPlayerRef.current?.pauseVideo?.()
        const blob = await getLocalTrackBlob(track.id)
        if (!blob) {
          setIsPlaying(false)
          return
        }
        if (currentObjectUrl.current) URL.revokeObjectURL(currentObjectUrl.current)
        const url = URL.createObjectURL(blob)
        currentObjectUrl.current = url
        const audio = localAudioRef.current
        audio.src = url
        audio.volume = muted ? 0 : volume / 100
        try {
          await audio.play()
        } catch {
          setIsPlaying(false)
        }
      } else {
        localAudioRef.current?.pause()
        if (!ytPlayerRef.current?.loadVideoById) return
        ytPlayerRef.current.loadVideoById(track.id)
        ytPlayerRef.current.setVolume(muted ? 0 : volume)
        setIsPlaying(true)
      }
    },
    [queue, muted, volume, user]
  )

  // Media Session API: shows Now Playing info on the lock screen / notification
  // shade, and — crucially — routes Bluetooth headset/earbud hardware buttons
  // (play/pause/next/previous) and OS media-key presses to our player.
  useEffect(() => {
    if (!('mediaSession' in navigator)) return
    if (!currentTrack) {
      navigator.mediaSession.metadata = null
      navigator.mediaSession.playbackState = 'none'
      return
    }
    navigator.mediaSession.metadata = new window.MediaMetadata({
      title: currentTrack.title,
      artist: currentTrack.artist,
      album: 'Spotifusion',
      artwork: currentTrack.thumbnail
        ? [
            { src: currentTrack.thumbnail, sizes: '96x96', type: 'image/png' },
            { src: currentTrack.thumbnail, sizes: '512x512', type: 'image/png' },
          ]
        : [],
    })
  }, [currentTrack])

  useEffect(() => {
    if (!('mediaSession' in navigator)) return
    navigator.mediaSession.playbackState = isPlaying ? 'playing' : 'paused'
  }, [isPlaying])

  useEffect(() => {
    if (!('mediaSession' in navigator)) return
    navigator.mediaSession.setActionHandler('play', () => togglePlay())
    navigator.mediaSession.setActionHandler('pause', () => togglePlay())
    navigator.mediaSession.setActionHandler('previoustrack', () => playPrevious())
    navigator.mediaSession.setActionHandler('nexttrack', () => playNext())
    navigator.mediaSession.setActionHandler('seekto', (details) => {
      if (details.seekTime != null) seekTo(details.seekTime)
    })
    try {
      if (duration) {
        navigator.mediaSession.setPositionState({
          duration,
          position: Math.min(progress, duration),
          playbackRate: 1,
        })
      }
    } catch {
      // Some browsers throw if position > duration transiently; safe to ignore.
    }
    return () => {
      ;['play', 'pause', 'previoustrack', 'nexttrack', 'seekto'].forEach((action) => {
        try {
          navigator.mediaSession.setActionHandler(action, null)
        } catch {
          /* no-op */
        }
      })
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queue, queueIndex, shuffle, repeatMode, duration, progress])

  // Public API -----------------------------------------------------------

  function playTrack(track, contextTracks = null) {
    const list = contextTracks || [track]
    const idx = list.findIndex((t) => t.id === track.id)
    setQueue(list)
    loadAndPlay(idx === -1 ? 0 : idx, list)
  }

  function togglePlay() {
    if (isLocal) {
      if (!localAudioRef.current?.src) return
      if (isPlaying) localAudioRef.current.pause()
      else localAudioRef.current.play()
      return
    }
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
      if (isLocal) localAudioRef.current.currentTime = 0
      else ytPlayerRef.current.seekTo(0)
      setProgress(0)
      return
    }
    const prevIndex = queueIndex - 1 < 0 ? (repeatMode === 'all' ? queue.length - 1 : 0) : queueIndex - 1
    loadAndPlay(prevIndex)
  }

  function seekTo(seconds) {
    if (isLocal) {
      if (localAudioRef.current) localAudioRef.current.currentTime = seconds
    } else {
      ytPlayerRef.current?.seekTo(seconds, true)
    }
    setProgress(seconds)
  }

  function changeVolume(v) {
    setVolume(v)
    setMuted(false)
    ytPlayerRef.current?.setVolume(v)
    if (localAudioRef.current) localAudioRef.current.volume = v / 100
  }

  function toggleMute() {
    const next = !muted
    setMuted(next)
    ytPlayerRef.current?.setVolume(next ? 0 : volume)
    if (localAudioRef.current) localAudioRef.current.volume = next ? 0 : volume / 100
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
