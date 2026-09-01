import { createContext, useContext, useEffect, useRef, useState, useCallback, useMemo } from 'react'
import { useAuth } from './AuthContext'
import { recordPlay } from '@/lib/library'
import { getLocalSongBlob } from '@/lib/localMusicDb'
import { isPrivateSession } from '@/lib/privacySettings'
import { getYouTubeStream } from '@/lib/streamApi'

const PlayerContext = createContext(null)
const PlayerRowContext = createContext(null)

// 5-band EQ, standard-ish frequencies. Only applies to local files: YouTube
// audio plays inside a cross-origin <iframe>, and browsers deliberately
// block reading/processing audio from a cross-origin source via Web Audio.
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

function useYouTubeApi() {
  const [ready, setReady] = useState(!!window.YT?.Player)
  useEffect(() => {
    if (window.YT?.Player) { setReady(true); return }
    const prev = window.onYouTubeIframeAPIReady
    window.onYouTubeIframeAPIReady = () => { prev?.(); setReady(true) }
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
  const shuffleStateRef = useRef({ order: [], position: -1 })
  const audioCtxRef = useRef(null)
  const eqFiltersRef = useRef(null)
  const streamAbortRef = useRef(null)
  const directFallbackAttemptedRef = useRef(null)
  const [playbackMode, setPlaybackMode] = useState('youtube')

  const savedPlayerState = (() => {
    try { return JSON.parse(localStorage.getItem('spotifusion:player-state:v2') || 'null') || {} } catch { return {} }
  })()
  const initialQueue = Array.isArray(savedPlayerState.queue) ? savedPlayerState.queue : []
  const initialIndex = Number.isInteger(savedPlayerState.queueIndex) && savedPlayerState.queueIndex >= 0 && savedPlayerState.queueIndex < initialQueue.length ? savedPlayerState.queueIndex : -1
  const [queue, setQueue] = useState(initialQueue)
  const [queueIndex, setQueueIndex] = useState(initialIndex)
  const [isPlaying, setIsPlaying] = useState(false)
  const [progress, setProgress] = useState(0)
  const [duration, setDuration] = useState(0)
  const [volume, setVolume] = useState(Number.isFinite(savedPlayerState.volume) ? savedPlayerState.volume : 70)
  const [muted, setMuted] = useState(Boolean(savedPlayerState.muted))
  const [shuffle, setShuffle] = useState(Boolean(savedPlayerState.shuffle))
  const [repeatMode, setRepeatMode] = useState(['off','all','one'].includes(savedPlayerState.repeatMode) ? savedPlayerState.repeatMode : 'off')
  const [eqGains, setEqGains] = useState(EQ_PRESETS.Flat)
  const [eqPreset, setEqPreset] = useState('Flat')
  const [outputDeviceId, setOutputDeviceId] = useState(null)
  const [outputDeviceLabel, setOutputDeviceLabel] = useState(null)
  const [sleepTimerSeconds, setSleepTimerSeconds] = useState(null)
  const [nowPlayingOpen, setNowPlayingOpen] = useState(false)

  const currentTrack = queueIndex >= 0 ? queue[queueIndex] : null
  const isLocal = currentTrack?.source === 'local'
  const isDirectStream = playbackMode === 'direct' && !isLocal

  useEffect(() => {
    try { localStorage.setItem('spotifusion:player-state:v2', JSON.stringify({ queue, queueIndex, volume, muted, shuffle, repeatMode })) } catch {}
  }, [queue, queueIndex, volume, muted, shuffle, repeatMode])

  const handleEndedRef = useRef(() => {})
  const fallbackToDirectRef = useRef(async () => false)

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
  function applyEqGains(gains) { eqFiltersRef.current?.forEach((filter, i) => { filter.gain.value = gains[i] }) }
  function setEqBand(index, value) {
    setEqGains((prev) => { const next = [...prev]; next[index] = value; applyEqGains(next); return next })
    setEqPreset('Custom')
  }
  function applyEqPreset(name) { const gains = EQ_PRESETS[name]; if (!gains) return; setEqGains(gains); setEqPreset(name); applyEqGains(gains) }

  useEffect(() => {
    const audio = new Audio()
    audio.preload = 'auto'
    localAudioRef.current = audio
    const onEnded = () => handleEndedRef.current()
    const onPlay = () => setIsPlaying(true)
    const onPause = () => setIsPlaying(false)
    const onLoadedMetadata = () => setDuration(Number.isFinite(audio.duration) ? audio.duration : 0)
    const onError = async () => {
      if (playbackMode !== 'direct' || !currentTrack?.id) return
      setIsPlaying(false)
      // A signed media URL can expire or be rejected. Retry through YouTube once;
      // the YouTube error handler can then try yt-dlp again if necessary.
      if (directFallbackAttemptedRef.current === currentTrack.id) return
      directFallbackAttemptedRef.current = currentTrack.id
      setPlaybackMode('youtube')
      try { ytPlayerRef.current?.loadVideoById(currentTrack.id) } catch {}
    }
    audio.addEventListener('ended', onEnded); audio.addEventListener('play', onPlay); audio.addEventListener('pause', onPause)
    audio.addEventListener('loadedmetadata', onLoadedMetadata); audio.addEventListener('error', onError)
    return () => {
      audio.removeEventListener('ended', onEnded); audio.removeEventListener('play', onPlay); audio.removeEventListener('pause', onPause)
      audio.removeEventListener('loadedmetadata', onLoadedMetadata); audio.removeEventListener('error', onError)
      audio.pause(); if (currentObjectUrl.current) URL.revokeObjectURL(currentObjectUrl.current)
      streamAbortRef.current?.abort()
    }
  }, [playbackMode, currentTrack?.id])

  useEffect(() => {
    if (!apiReady || ytPlayerRef.current) return
    if (!document.getElementById('yt-player-mount')) {
      const div = document.createElement('div')
      div.id = 'yt-player-mount'; div.style.position = 'fixed'; div.style.bottom = '0'; div.style.left = '0'; div.style.width = '1px'; div.style.height = '1px'; div.style.opacity = '0'; div.style.pointerEvents = 'none'
      document.body.appendChild(div)
    }
    ytPlayerRef.current = new window.YT.Player('yt-player-mount', {
      height: '1', width: '1', playerVars: { playsinline: 1, controls: 0, disablekb: 1, origin: window.location.origin, enablejsapi: 1 },
      events: {
        onReady: (e) => e.target.setVolume(volume),
        onStateChange: (e) => {
          const YTState = window.YT.PlayerState
          if (e.data === YTState.PLAYING) { setPlaybackMode('youtube'); setIsPlaying(true) }
          if (e.data === YTState.PAUSED) setIsPlaying(false)
          if (e.data === YTState.ENDED) handleEndedRef.current()
        },
        onError: () => { fallbackToDirectRef.current() },
      },
    })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiReady])

  fallbackToDirectRef.current = async () => {
    const track = currentTrack
    if (!track || track.source === 'local' || !track.id) return false
    if (directFallbackAttemptedRef.current === track.id) return false
    directFallbackAttemptedRef.current = track.id
    streamAbortRef.current?.abort()
    const controller = new AbortController()
    streamAbortRef.current = controller
    try {
      const stream = await getYouTubeStream(track.id, { signal: controller.signal })
      if (controller.signal.aborted || queue[queueIndex]?.id !== track.id) return false
      ytPlayerRef.current?.pauseVideo?.()
      const audio = localAudioRef.current
      audio.pause()
      audio.src = stream.url
      audio.currentTime = 0
      audio.volume = muted ? 0 : volume / 100
      setPlaybackMode('direct')
      setProgress(0)
      setDuration(Number.isFinite(stream.durationSeconds) ? stream.durationSeconds : 0)
      await audio.play()
      return true
    } catch (err) {
      if (!controller.signal.aborted) console.warn('yt-dlp playback fallback failed:', err)
      if (!controller.signal.aborted && queue[queueIndex]?.id === track.id) setPlaybackMode('youtube')
      return false
    }
  }

  function handleEnded() {
    if (repeatMode === 'one') {
      if (isLocal || isDirectStream) { localAudioRef.current.currentTime = 0; localAudioRef.current.play() }
      else { ytPlayerRef.current?.seekTo(0); ytPlayerRef.current?.playVideo() }
      return
    }
    playNext()
  }
  handleEndedRef.current = handleEnded

  useEffect(() => {
    clearInterval(progressTimer.current)
    if (isPlaying) progressTimer.current = setInterval(() => {
      if (isLocal || isDirectStream) { setProgress(localAudioRef.current?.currentTime || 0); setDuration(localAudioRef.current?.duration || duration || 0) }
      else { setProgress(ytPlayerRef.current?.getCurrentTime?.() || 0); setDuration(ytPlayerRef.current?.getDuration?.() || 0) }
    }, 500)
    return () => clearInterval(progressTimer.current)
  }, [isPlaying, isLocal, isDirectStream, duration])

  const loadAndPlay = useCallback(async (index, list = queue) => {
    const track = list[index]
    if (!track) return
    streamAbortRef.current?.abort()
    directFallbackAttemptedRef.current = null
    setQueueIndex(index); setProgress(0); setDuration(0)
    if (track.source === 'local') {
      setPlaybackMode('local')
      ytPlayerRef.current?.pauseVideo?.()
      const blob = await getLocalSongBlob(track.id)
      // A local file can disappear or lose permission after the queue was saved.
      // Skip it rather than leaving a non-playable track selected in the player.
      if (!blob) {
        setIsPlaying(false)
        const nextIndex = index + 1 < list.length ? index + 1 : (repeatMode === 'all' && list.length ? 0 : -1)
        if (nextIndex >= 0 && nextIndex !== index) {
          await loadAndPlay(nextIndex, list)
        } else {
          localAudioRef.current?.pause()
          if (currentObjectUrl.current) { URL.revokeObjectURL(currentObjectUrl.current); currentObjectUrl.current = null }
          setQueueIndex(-1)
          setProgress(0)
          setDuration(0)
          setPlaybackMode('youtube')
        }
        return
      }
      if (user && !isPrivateSession()) recordPlay(user.uid, track)
      if (currentObjectUrl.current) URL.revokeObjectURL(currentObjectUrl.current)
      const url = URL.createObjectURL(blob); currentObjectUrl.current = url
      const audio = localAudioRef.current; audio.src = url; audio.volume = muted ? 0 : volume / 100
      try { ensureEqGraph(); await audioCtxRef.current?.resume(); await audio.play() } catch { setIsPlaying(false) }
    } else {
      setPlaybackMode('youtube')
      localAudioRef.current?.pause()
      if (!ytPlayerRef.current?.loadVideoById) return
      ytPlayerRef.current.loadVideoById(track.id); ytPlayerRef.current.setVolume(muted ? 0 : volume); setIsPlaying(true)
      if (user && !isPrivateSession()) recordPlay(user.uid, track)
    }
  }, [queue, muted, volume, user, repeatMode])

  useEffect(() => {
    if (!('mediaSession' in navigator)) return
    if (!currentTrack) { navigator.mediaSession.metadata = null; navigator.mediaSession.playbackState = 'none'; return }
    navigator.mediaSession.metadata = new window.MediaMetadata({ title: currentTrack.title, artist: currentTrack.artist, album: 'Spotifusion', artwork: currentTrack.thumbnail ? [{ src: currentTrack.thumbnail, sizes: '96x96', type: 'image/png' }, { src: currentTrack.thumbnail, sizes: '512x512', type: 'image/png' }] : [] })
  }, [currentTrack])
  useEffect(() => { if ('mediaSession' in navigator) navigator.mediaSession.playbackState = isPlaying ? 'playing' : 'paused' }, [isPlaying])
  useEffect(() => {
    if (!('mediaSession' in navigator)) return
    navigator.mediaSession.setActionHandler('play', () => togglePlay())
    navigator.mediaSession.setActionHandler('pause', () => togglePlay())
    navigator.mediaSession.setActionHandler('previoustrack', () => playPrevious())
    navigator.mediaSession.setActionHandler('nexttrack', () => playNext())
    navigator.mediaSession.setActionHandler('seekto', (details) => { if (details.seekTime != null) seekTo(details.seekTime) })
    try { if (duration) navigator.mediaSession.setPositionState({ duration, position: Math.min(progress, duration), playbackRate: 1 }) } catch {}
    return () => { ['play','pause','previoustrack','nexttrack','seekto'].forEach((action) => { try { navigator.mediaSession.setActionHandler(action, null) } catch {} }) }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queue, queueIndex, shuffle, repeatMode, duration, progress])

  function clearSleepTimer() { if (sleepTimerRef.current) clearTimeout(sleepTimerRef.current); sleepTimerRef.current = null; setSleepTimerSeconds(null) }
  function setSleepTimer(seconds) {
    clearSleepTimer(); if (!seconds) return
    setSleepTimerSeconds(seconds)
    sleepTimerRef.current = setTimeout(() => { if (isLocal || isDirectStream) localAudioRef.current?.pause(); else ytPlayerRef.current?.pauseVideo?.(); setIsPlaying(false); setSleepTimerSeconds(null); sleepTimerRef.current = null }, seconds * 1000)
  }
  useEffect(() => () => { if (sleepTimerRef.current) clearTimeout(sleepTimerRef.current) }, [])

  function resetShuffleOrder(length = queue.length, currentIndex = queueIndex) {
    const indices = []
    for (let i = 0; i < length; i++) if (i !== currentIndex) indices.push(i)
    for (let i = indices.length - 1; i > 0; i--) { const j = Math.floor(Math.random() * (i + 1)); [indices[i], indices[j]] = [indices[j], indices[i]] }
    shuffleStateRef.current = { order: indices, position: -1 }
  }
  function playTrack(track, contextTracks = null) {
    const list = contextTracks || [track]; const idx = list.findIndex((t) => t.id === track.id); const nextIndex = idx === -1 ? 0 : idx
    setQueue(list); if (shuffle) resetShuffleOrder(list.length, nextIndex); else shuffleStateRef.current = { order: [], position: -1 }; loadAndPlay(nextIndex, list)
  }
  function togglePlay() {
    if (isLocal || isDirectStream) { if (!localAudioRef.current?.src) return; if (isPlaying) localAudioRef.current.pause(); else localAudioRef.current.play(); return }
    if (!ytPlayerRef.current) return; if (isPlaying) ytPlayerRef.current.pauseVideo(); else ytPlayerRef.current.playVideo()
  }
  function playNext() {
    if (!queue.length) return
    let nextIndex
    if (shuffle) {
      const state = shuffleStateRef.current
      if (!state.order.length || state.position >= state.order.length - 1) { if (state.position >= 0 && repeatMode !== 'all') return setIsPlaying(false); resetShuffleOrder(queue.length, queueIndex) }
      const nextState = shuffleStateRef.current; nextState.position += 1; nextIndex = nextState.order[nextState.position]; if (nextIndex == null) return setIsPlaying(false)
    } else { nextIndex = queueIndex + 1; if (nextIndex >= queue.length) { if (repeatMode === 'all') nextIndex = 0; else return setIsPlaying(false) } }
    loadAndPlay(nextIndex)
  }
  function playPrevious() {
    if (!queue.length) return
    if (progress > 3) { if (isLocal || isDirectStream) localAudioRef.current.currentTime = 0; else ytPlayerRef.current?.seekTo(0); setProgress(0); return }
    if (shuffle) { const state = shuffleStateRef.current; if (state.position > 0) { state.position -= 1; loadAndPlay(state.order[state.position]); return } }
    const prevIndex = queueIndex - 1 < 0 ? (repeatMode === 'all' ? queue.length - 1 : 0) : queueIndex - 1
    loadAndPlay(prevIndex)
  }
  function seekTo(seconds) { if (isLocal || isDirectStream) { if (localAudioRef.current) localAudioRef.current.currentTime = seconds } else ytPlayerRef.current?.seekTo(seconds, true); setProgress(seconds) }
  function changeVolume(v) { setVolume(v); setMuted(false); ytPlayerRef.current?.setVolume(v); if (localAudioRef.current) localAudioRef.current.volume = v / 100 }
  function toggleMute() { const next = !muted; setMuted(next); ytPlayerRef.current?.setVolume(next ? 0 : volume); if (localAudioRef.current) localAudioRef.current.volume = next ? 0 : volume / 100 }

  function removeFromQueue(index) {
    if (index < 0 || index >= queue.length) return
    const wasCurrent = index === queueIndex
    const nextQueue = queue.filter((_, i) => i !== index)
    if (!nextQueue.length) { clearQueue(); return }
    let nextIndex = queueIndex
    if (index < queueIndex) nextIndex -= 1
    else if (wasCurrent) nextIndex = Math.min(index, nextQueue.length - 1)
    setQueue(nextQueue); setQueueIndex(nextIndex)
    if (shuffle) resetShuffleOrder(nextQueue.length, nextIndex)
    if (wasCurrent) loadAndPlay(nextIndex, nextQueue)
  }
  function reorderQueue(fromIndex, toIndex) {
    if (fromIndex < 0 || toIndex < 0 || fromIndex >= queue.length || toIndex >= queue.length || fromIndex === toIndex) return
    const nextQueue = [...queue]; const [moved] = nextQueue.splice(fromIndex, 1); nextQueue.splice(toIndex, 0, moved)
    const currentId = currentTrack?.id; const nextIndex = currentId == null ? queueIndex : nextQueue.findIndex((track) => track.id === currentId)
    setQueue(nextQueue); if (nextIndex >= 0) setQueueIndex(nextIndex); if (shuffle) resetShuffleOrder(nextQueue.length, nextIndex)
  }
  function clearQueue() {
    streamAbortRef.current?.abort()
    localAudioRef.current?.pause(); ytPlayerRef.current?.pauseVideo?.()
    if (currentObjectUrl.current) { URL.revokeObjectURL(currentObjectUrl.current); currentObjectUrl.current = null }
    setQueue([]); setQueueIndex(-1); setProgress(0); setDuration(0); setIsPlaying(false); setPlaybackMode('youtube'); shuffleStateRef.current = { order: [], position: -1 }
  }

  const outputSupported = typeof HTMLMediaElement !== 'undefined' && 'setSinkId' in HTMLMediaElement.prototype && typeof navigator !== 'undefined' && !!navigator.mediaDevices?.selectAudioOutput
  async function chooseOutputDevice() {
    if (!outputSupported) return
    try {
      const device = await navigator.mediaDevices.selectAudioOutput()
      if (audioCtxRef.current && 'setSinkId' in audioCtxRef.current) await audioCtxRef.current.setSinkId(device.deviceId)
      else await localAudioRef.current?.setSinkId(device.deviceId)
      setOutputDeviceId(device.deviceId); setOutputDeviceLabel(device.label || 'Connected device')
    } catch (err) { if (err.name !== 'NotFoundError' && err.name !== 'NotAllowedError') console.error('Output device selection failed:', err) }
  }

  const value = useMemo(() => ({
    queue, queueIndex, currentTrack, isPlaying, progress, duration, volume, muted, shuffle, repeatMode,
    playbackMode, isDirectStream,
    setVolume: changeVolume, setMuted, setRepeatMode, removeFromQueue, reorderQueue, clearQueue,
    playerReady: apiReady, playTrack, togglePlay, playNext, playPrevious, seekTo, changeVolume, toggleMute,
    toggleShuffle: () => setShuffle((s) => { const next = !s; if (next) resetShuffleOrder(queue.length, queueIndex); else shuffleStateRef.current = { order: [], position: -1 }; return next }),
    cycleRepeat: () => setRepeatMode((m) => (m === 'off' ? 'all' : m === 'all' ? 'one' : 'off')),
    eqSupported: isLocal, eqGains, eqPreset, eqBands: EQ_BANDS, eqPresetNames: Object.keys(EQ_PRESETS), setEqBand, applyEqPreset,
    outputSupported, outputDeviceId, outputDeviceLabel, chooseOutputDevice, sleepTimerSeconds, nowPlayingOpen,
    openNowPlaying: () => setNowPlayingOpen(true), closeNowPlaying: () => setNowPlayingOpen(false), setSleepTimer, clearSleepTimer,
  }), [queue, queueIndex, currentTrack, isPlaying, progress, duration, volume, muted, shuffle, repeatMode, playbackMode, isDirectStream, eqGains, eqPreset, outputSupported, outputDeviceId, outputDeviceLabel, sleepTimerSeconds, nowPlayingOpen, apiReady, isLocal])

  return <PlayerContext.Provider value={value}><PlayerRowContext.Provider value={{ currentTrackId: currentTrack?.id ?? null, isPlaying, playTrack, togglePlay, openNowPlaying: () => setNowPlayingOpen(true) }}>{children}</PlayerRowContext.Provider></PlayerContext.Provider>
}
export function usePlayer() { const ctx = useContext(PlayerContext); if (!ctx) throw new Error('usePlayer must be used within PlayerProvider'); return ctx }
export function usePlayerRow() { const ctx = useContext(PlayerRowContext); if (!ctx) throw new Error('usePlayerRow must be used within PlayerRowContext'); return ctx }
