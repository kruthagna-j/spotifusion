import { createContext, useContext, useEffect, useRef, useState, useCallback } from 'react'
import { useAuth } from './AuthContext'
import { recordPlay } from '@/lib/library'
import { getLocalSongBlob } from '@/lib/localMusicDb'
import { isPrivateSession } from '@/lib/privacySettings'

const PlayerContext = createContext(null)

// 5-band EQ, standard-ish frequencies. Only applies to local files: YouTube
// audio plays inside a cross-origin <iframe>, and browsers deliberately
// block reading/processing audio from a cross-origin source via Web Audio
// (a security boundary, not a bug) — so there is no technically honest way
// to apply EQ to YouTube-backed playback. The UI disables EQ for those
// tracks rather than pretending it works.
export const EQ_BANDS = [60, 230, 910, 3600, 14000]
export const EQ_PRESETS = {
  Flat: [0, 0, 0, 0, 0],
  Pop: [-1, 2, 3, 2, -1],
  Rock: [4, 2, -2, 2, 3],
  Classical: [3, 2, 0, 2, 3],
  Jazz: [2, 1, -1, 1, 2],
  'Bass Boost': [6, 4, 0, 0, 0],
  Vocal: [-2, 0, 3, 3, 0],
}

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
  const sleepTimerRef = useRef(null)

  // Web Audio graph for the local-file EQ (created lazily, once).
  const audioCtxRef = useRef(null)
  const eqFiltersRef = useRef(null)

  const [queue, setQueue] = useState([]) // array of track objects
  const [queueIndex, setQueueIndex] = useState(-1)
  const [isPlaying, setIsPlaying] = useState(false)
  const [progress, setProgress] = useState(0) // seconds
  const [duration, setDuration] = useState(0)
  const [volume, setVolume] = useState(70)
  const [muted, setMuted] = useState(false)
  const [shuffle, setShuffle] = useState(false)
  const [repeatMode, setRepeatMode] = useState('off') // off | all | one
  const [eqGains, setEqGains] = useState(EQ_PRESETS.Flat)
  const [eqPreset, setEqPreset] = useState('Flat')
  const [outputDeviceId, setOutputDeviceId] = useState(null) // null = system default
  const [outputDeviceLabel, setOutputDeviceLabel] = useState(null)
  const [sleepTimerSeconds, setSleepTimerSeconds] = useState(null)
  const [nowPlayingOpen, setNowPlayingOpen] = useState(false)

  const currentTrack = queueIndex >= 0 ? queue[queueIndex] : null
  const isLocal = currentTrack?.source === 'local'

  // Keep the latest handler in a ref so the <audio> "ended" listener (added
  // once) always calls the current repeatMode/queue-aware logic.
  const handleEndedRef = useRef(() => {})

  // Build the Web Audio graph the first time it's needed: source -> 5x
  // BiquadFilter (peaking EQ bands) -> destination. createMediaElementSource
  // can only be called ONCE per <audio> element ever, hence the ref guard.
  function ensureEqGraph() {
    if (eqFiltersRef.current) return
    const AudioCtx = window.AudioContext || window.webkitAudioContext
    if (!AudioCtx || !localAudioRef.current) return
    const ctx = new AudioCtx()
    const source = ctx.createMediaElementSource(localAudioRef.current)
    const filters = EQ_BANDS.map((freq, i) => {
      const filter = ctx.createBiquadFilter()
      filter.type = i === 0 ? 'lowshelf' : i === EQ_BANDS.length - 1 ? 'highshelf' : 'peaking'
      filter.frequency.value = freq
      filter.Q.value = 1
      filter.gain.value = eqGains[i]
      return filter
    })
    source.connect(filters[0])
    for (let i = 0; i < filters.length - 1; i++) filters[i].connect(filters[i + 1])
    filters[filters.length - 1].connect(ctx.destination)
    audioCtxRef.current = ctx
    eqFiltersRef.current = filters
  }

  function applyEqGains(gains) {
    eqFiltersRef.current?.forEach((filter, i) => {
      filter.gain.value = gains[i]
    })
  }

  function setEqBand(index, value) {
    setEqGains((prev) => {
      const next = [...prev]
      next[index] = value
      applyEqGains(next)
      return next
    })
    setEqPreset('Custom')
  }

  function applyEqPreset(name) {
    const gains = EQ_PRESETS[name]
    if (!gains) return
    setEqGains(gains)
    setEqPreset(name)
    applyEqGains(gains)
  }

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
      playerVars: { playsinline: 1, controls: 0, disablekb: 1, origin: window.location.origin, enablejsapi: 1 },
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
      if (user && !isPrivateSession()) recordPlay(user.uid, track)

      if (track.source === 'local') {
        ytPlayerRef.current?.pauseVideo?.()
        const blob = await getLocalSongBlob(track.id)
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
          ensureEqGraph()
          await audioCtxRef.current?.resume()
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

  function clearSleepTimer() {
    if (sleepTimerRef.current) {
      clearTimeout(sleepTimerRef.current)
      sleepTimerRef.current = null
    }
    setSleepTimerSeconds(null)
  }

  function setSleepTimer(seconds) {
    clearSleepTimer()
    if (!seconds) return
    const durationMs = seconds * 1000
    setSleepTimerSeconds(seconds)
    sleepTimerRef.current = setTimeout(() => {
      if (isLocal) localAudioRef.current?.pause()
      else ytPlayerRef.current?.pauseVideo?.()
      setIsPlaying(false)
      setSleepTimerSeconds(null)
      sleepTimerRef.current = null
    }, durationMs)
  }

  useEffect(() => {
    return () => {
      if (sleepTimerRef.current) clearTimeout(sleepTimerRef.current)
    }
  }, [])

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

  // Real equivalent of Spotify's "Connect to a device" for Bluetooth
  // speakers/headphones: this doesn't pair Bluetooth (that's always an
  // OS-level action, in any app), it lets the user pick which *already
  // paired/connected* output device audio plays through, from inside the
  // app — which is what Spotify's own feature actually does under the hood.
  // Only affects local-file playback: YouTube's audio renders inside its
  // own cross-origin iframe, which this API has no access to.
  //
  // Gated on selectAudioOutput specifically (not just setSinkId): older
  // Chrome has setSinkId without selectAudioOutput, but picking a device
  // that way needs mic-permission-gated device labels with no clean UX —
  // rather than ship a half-working fallback, the button simply doesn't
  // appear on browsers that lack the full picker.
  const outputSupported =
    typeof HTMLMediaElement !== 'undefined' &&
    'setSinkId' in HTMLMediaElement.prototype &&
    typeof navigator !== 'undefined' &&
    !!navigator.mediaDevices?.selectAudioOutput

  async function chooseOutputDevice() {
    if (!outputSupported) return
    try {
      const device = await navigator.mediaDevices.selectAudioOutput()
      // If the EQ graph is active, audio is routed through
      // audioCtxRef.current.destination, not the <audio> element directly —
      // setSinkId has to target whichever one is actually producing output.
      if (audioCtxRef.current && 'setSinkId' in audioCtxRef.current) {
        await audioCtxRef.current.setSinkId(device.deviceId)
      } else {
        await localAudioRef.current?.setSinkId(device.deviceId)
      }
      setOutputDeviceId(device.deviceId)
      setOutputDeviceLabel(device.label || 'Connected device')
    } catch (err) {
      if (err.name !== 'NotFoundError' && err.name !== 'NotAllowedError') {
        console.error('Output device selection failed:', err)
      }
    }
  }

  async function resetOutputDevice() {
    if (!outputSupported) return
    await localAudioRef.current?.setSinkId('')
    setOutputDeviceId(null)
    setOutputDeviceLabel(null)
  }

  function enqueue(track) {
    setQueue((q) => [...q, track])
  }

  // Insert right after the currently playing track ("Play Next").
  function playNext_insert(track) {
    setQueue((q) => {
      const next = [...q]
      next.splice(queueIndex + 1, 0, track)
      return next
    })
  }

  function removeFromQueue(index) {
    setQueue((q) => q.filter((_, i) => i !== index))
    if (index < queueIndex) setQueueIndex((i) => i - 1)
  }

  function reorderQueue(fromIndex, toIndex) {
    setQueue((q) => {
      const next = [...q]
      const [moved] = next.splice(fromIndex, 1)
      next.splice(toIndex, 0, moved)
      return next
    })
    setQueueIndex((i) => {
      if (fromIndex === i) return toIndex
      if (fromIndex < i && toIndex >= i) return i - 1
      if (fromIndex > i && toIndex <= i) return i + 1
      return i
    })
  }

  // Clears everything except the currently playing track (matches Spotify's
  // "Clear queue" behavior — it doesn't stop what's playing).
  function clearQueue() {
    setQueue((q) => (queueIndex >= 0 ? [q[queueIndex]] : []))
    setQueueIndex(queueIndex >= 0 ? 0 : -1)
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
    playNextInQueue: playNext_insert,
    removeFromQueue,
    reorderQueue,
    clearQueue,
    // Equalizer — only meaningfully affects playback when isLocal is true.
    // eqSupported tells the UI whether to disable the controls for the
    // current track (true for local files, false for YouTube — a real
    // cross-origin-audio limitation, not a missing feature).
    eqSupported: isLocal,
    eqGains,
    eqPreset,
    eqBands: EQ_BANDS,
    eqPresetNames: Object.keys(EQ_PRESETS),
    setEqBand,
    applyEqPreset,
    // Audio output device (Bluetooth/speaker) selection
    outputSupported,
    outputDeviceId,
    outputDeviceLabel,
    chooseOutputDevice,
    resetOutputDevice,
    sleepTimerSeconds,
    nowPlayingOpen,
    openNowPlaying: () => setNowPlayingOpen(true),
    closeNowPlaying: () => setNowPlayingOpen(false),
    setSleepTimer,
    clearSleepTimer,
  }

  return <PlayerContext.Provider value={value}>{children}</PlayerContext.Provider>
}

export function usePlayer() {
  const ctx = useContext(PlayerContext)
  if (!ctx) throw new Error('usePlayer must be used within PlayerProvider')
  return ctx
}
