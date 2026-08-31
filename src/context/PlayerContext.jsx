import React, { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react'

import { getLocalSongBlob } from '../lib/localMusicDb'
import { recordPlay } from '../lib/library'
import { isPrivateSession } from '../lib/privacySettings'

const PlayerContext = createContext(null)
const PlayerRowContext = createContext(null)

const EQ_PRESETS = {
  Flat: [0, 0, 0, 0, 0],
  Bass: [6, 4, 1, 0, -1],
  Treble: [-1, 0, 1, 4, 6],
  Vocal: [-2, 1, 4, 3, 1],
  Rock: [4, 2, -1, 2, 4],
}

export function PlayerProvider({ children }) {
  const [queue, setQueue] = useState(() => {
    try { return JSON.parse(localStorage.getItem('spotifusion:queue:v2') || '[]') || [] } catch { return [] }
  })
  const [queueIndex, setQueueIndex] = useState(() => {
    try { return Number(localStorage.getItem('spotifusion:queue-index:v2') || 0) || 0 } catch { return 0 }
  })
  const [isPlaying, setIsPlaying] = useState(false)
  const [volume, setVolume] = useState(() => {
    try { return Number(localStorage.getItem('spotifusion:volume') || 80) || 80 } catch { return 80 }
  })
  const [muted, setMuted] = useState(false)
  const [shuffle, setShuffle] = useState(false)
  const [repeatMode, setRepeatMode] = useState('off')
  const [progress, setProgress] = useState(0)
  const [duration, setDuration] = useState(0)
  const [sleepTimerSeconds, setSleepTimerSeconds] = useState(null)
  const [outputDeviceId, setOutputDeviceId] = useState('default')
  const [outputDeviceLabel, setOutputDeviceLabel] = useState('Default device')
  const [eqGains, setEqGains] = useState(EQ_PRESETS.Flat)
  const [eqPreset, setEqPreset] = useState('Flat')
  const [currentTrack, setCurrentTrack] = useState(() => {
    try { return JSON.parse(localStorage.getItem('spotifusion:current-track:v2') || 'null') || null } catch { return null }
  })

  const localAudioRef = useRef(null)
  const ytPlayerRef = useRef(null)
  const apiReady = typeof window !== 'undefined' && !!window.YT
  const progressTimer = useRef(null)
  const currentObjectUrl = useRef(null)
  const handleEndedRef = useRef(() => {})
  const shuffleStateRef = useRef({ order: [], position: -1 })
  const sleepTimerRef = useRef(null)
  const audioCtxRef = useRef(null)
  const eqSourceRef = useRef(null)

  useEffect(() => {
    try {
      localStorage.setItem('spotifusion:queue:v2', JSON.stringify(queue))
      localStorage.setItem('spotifusion:queue-index:v2', String(queueIndex))
      if (currentTrack) localStorage.setItem('spotifusion:current-track:v2', JSON.stringify(currentTrack))
      else localStorage.removeItem('spotifusion:current-track:v2')
      localStorage.setItem('spotifusion:volume', String(volume))
    } catch { /* storage is best effort */ }
  }, [queue, queueIndex, currentTrack, volume])

  const isLocal = currentTrack?.source === 'local'

  function ensureEqGraph() {
    const audio = localAudioRef.current
    if (!audio || eqSourceRef.current) return
    try {
      const AudioContextCtor = window.AudioContext || window.webkitAudioContext
      if (!AudioContextCtor) return
      const ctx = new AudioContextCtor()
      const source = ctx.createMediaElementSource(audio)
      const filters = [60, 230, 910, 3600, 14000].map((frequency) => {
        const filter = ctx.createBiquadFilter()
        filter.type = 'peaking'
        filter.frequency.value = frequency
        filter.Q.value = 1
        filter.gain.value = 0
        return filter
      })
      let node = source
      filters.forEach((filter) => { node.connect(filter); node = filter })
      node.connect(ctx.destination)
      audioCtxRef.current = ctx
      eqSourceRef.current = { source, filters }
    } catch {
      // EQ is an enhancement; playback should continue without it.
    }
  }

  function applyEqGains(gains) {
    eqSourceRef.current?.filters?.forEach((filter, index) => { filter.gain.value = gains[index] || 0 })
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
    async (index, list = queue, recoveryDepth = 0) => {
      const track = list[index]
      if (!track) return
      setQueueIndex(index)
      setCurrentTrack(track)
      setProgress(0)
      if (user && !isPrivateSession()) recordPlay(user.uid, track)

      if (track.source === 'local') {
        ytPlayerRef.current?.pauseVideo?.()
        const blob = await getLocalSongBlob(track.id)
        if (!blob) {
          // A local record can outlive its underlying file. Recover instead of
          // leaving the player stuck on a dead track.
          setIsPlaying(false)
          if (recoveryDepth < list.length - 1) {
            const nextIndex = index + 1 < list.length ? index + 1 : 0
            if (nextIndex !== index) await loadAndPlay(nextIndex, list, recoveryDepth + 1)
          }
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
          if (recoveryDepth < list.length - 1) {
            const nextIndex = index + 1 < list.length ? index + 1 : 0
            if (nextIndex !== index) await loadAndPlay(nextIndex, list, recoveryDepth + 1)
          }
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
        navigator.mediaSession.setPositionState({ duration, position: Math.min(progress, duration), playbackRate: 1 })
      }
    } catch { /* Some browsers throw transiently. */ }
    return () => {
      ;['play', 'pause', 'previoustrack', 'nexttrack', 'seekto'].forEach((action) => {
        try { navigator.mediaSession.setActionHandler(action, null) } catch { /* no-op */ }
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

  useEffect(() => () => { if (sleepTimerRef.current) clearTimeout(sleepTimerRef.current) }, [])

  function resetShuffleOrder(length = queue.length, currentIndex = queueIndex) {
    const indices = []
    for (let i = 0; i < length; i++) if (i !== currentIndex) indices.push(i)
    for (let i = indices.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [indices[i], indices[j]] = [indices[j], indices[i]] }
    shuffleStateRef.current = { order: indices, position: -1 }
  }

  function playTrack(track, contextTracks = null) {
    const list = contextTracks || [track]
    const idx = list.findIndex((t) => t.id === track.id)
    const nextIndex = idx === -1 ? 0 : idx
    setQueue(list)
    if (shuffle) resetShuffleOrder(list.length, nextIndex)
    else shuffleStateRef.current = { order: [], position: -1 }
    loadAndPlay(nextIndex, list)
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
      const state = shuffleStateRef.current
      if (!state.order.length || state.position >= state.order.length - 1) {
        if (state.position >= 0 && repeatMode !== 'all') return setIsPlaying(false)
        resetShuffleOrder(queue.length, queueIndex)
      }
      const nextState = shuffleStateRef.current
      nextState.position += 1
      nextIndex = nextState.order[nextState.position]
      if (nextIndex == null) return setIsPlaying(false)
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
    if (progress > 3) {
      if (isLocal) localAudioRef.current.currentTime = 0
      else ytPlayerRef.current.seekTo(0)
      setProgress(0)
      return
    }
    if (shuffle) {
      const state = shuffleStateRef.current
      if (state.position > 0) { state.position -= 1; loadAndPlay(state.order[state.position]); return }
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

  const outputSupported = typeof HTMLMediaElement !== 'undefined' && 'setSinkId' in HTMLMediaElement.prototype && typeof navigator !== 'undefined' && !!navigator.mediaDevices?.selectAudioOutput

  async function chooseOutputDevice() {
    if (!outputSupported) return
    try {
      const device = await navigator.mediaDevices.selectAudioOutput()
      if (audioCtxRef.current && 'setSinkId' in audioCtxRef.current) await audioCtxRef.current.setSinkId(device.deviceId)
      else await localAudioRef.current?.setSinkId(device.deviceId)
      setOutputDeviceId(device.deviceId)
      setOutputDeviceLabel(device.label || 'Connected device')
    } catch (err) {
      if (err.name !== 'NotFoundError' && err.name !== 'NotAllowedError') console.error('Output device selection failed:', err)
    }
  }

  const rowPlayTrack = useCallback((track, contextTracks = null) => playTrack(track, contextTracks), [loadAndPlay, shuffle])

  const value = useMemo(() => ({
    queue, queueIndex, currentTrack, isPlaying, volume, muted, shuffle, repeatMode, progress, duration,
    sleepTimerSeconds, outputSupported, outputDeviceId, outputDeviceLabel, eqGains, eqPreset, isLocal,
    playTrack, playNext, playPrevious, togglePlay, seekTo, changeVolume, toggleMute, setShuffle, setRepeatMode,
    setSleepTimer, clearSleepTimer, chooseOutputDevice, setEqBand, applyEqPreset, resetShuffleOrder, setQueue,
    setQueueIndex, loadAndPlay,
  }), [queue, queueIndex, currentTrack, isPlaying, volume, muted, shuffle, repeatMode, progress, duration,
    sleepTimerSeconds, outputSupported, outputDeviceId, outputDeviceLabel, eqGains, eqPreset, isLocal, loadAndPlay])

  const rowValue = useMemo(() => ({ playTrack: rowPlayTrack, currentTrack, isPlaying }), [rowPlayTrack, currentTrack, isPlaying])

  return <PlayerContext.Provider value={value}><PlayerRowContext.Provider value={rowValue}>{children}</PlayerRowContext.Provider></PlayerContext.Provider>
}

export function usePlayer() { const ctx = useContext(PlayerContext); if (!ctx) throw new Error('usePlayer must be used within PlayerProvider'); return ctx }
export function usePlayerRow() { const ctx = useContext(PlayerRowContext); if (!ctx) throw new Error('usePlayerRow must be used within PlayerProvider'); return ctx }
