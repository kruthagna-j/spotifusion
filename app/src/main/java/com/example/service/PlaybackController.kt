package com.example.service

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.remote.StreamResolver
import com.example.data.OfflineDownloadManager
import com.example.model.EqualizerState
import com.example.model.PlayerState
import com.example.model.RepeatMode
import com.example.model.Track
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin
import kotlin.random.Random

@OptIn(UnstableApi::class)
object PlaybackController {
  private const val TAG = "PlaybackController"
  private val coroutineScope = CoroutineScope(Dispatchers.Main)
  private var controllerFuture: ListenableFuture<MediaController>? = null
  private var controller: MediaController? = null
  private var appContext: Context? = null
  private var audioQuality: String = "High (320kbps)"
  private var crossfadeSeconds: Int = 3
  private var crossfadeJob: Job? = null
  private val _playerState = MutableStateFlow(PlayerState())
  val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
  private val _sleepTimerMinutes = MutableStateFlow<Int?>(null)
  val sleepTimerMinutes: StateFlow<Int?> = _sleepTimerMinutes.asStateFlow()
  private val _sleepTimerRemainingSec = MutableStateFlow(0)
  val sleepTimerRemainingSec: StateFlow<Int> = _sleepTimerRemainingSec.asStateFlow()
  private var sleepTimerJob: Job? = null
  private var progressTimerJob: Job? = null
  private var streamResolveJob: Job? = null
  private var originalQueue: List<Track> = emptyList()
  private var currentEqualizerState = EqualizerState()

  private val playerListener = object : Player.Listener {
    override fun onIsPlayingChanged(isPlaying: Boolean) {
      _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
      if (isPlaying) startProgressLoop()
    }
    override fun onPlaybackStateChanged(playbackState: Int) {
      when (playbackState) {
        Player.STATE_BUFFERING -> _playerState.value = _playerState.value.copy(isBuffering = true)
        Player.STATE_READY -> {
          val dur = ((controller?.duration ?: 0L) / 1000).toInt().coerceAtLeast(0)
          _playerState.value = _playerState.value.copy(isBuffering = false, durationSec = if (dur > 0) dur else (_playerState.value.currentTrack?.durationSec ?: 0))
        }
        Player.STATE_ENDED -> { _playerState.value = _playerState.value.copy(isBuffering = false); nextTrack() }
        Player.STATE_IDLE -> _playerState.value = _playerState.value.copy(isBuffering = false)
      }
    }
    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
      _playerState.value = _playerState.value.copy(currentPositionSec = (newPosition.positionMs / 1000).toInt())
    }
  }

  fun connect(context: Context) {
    appContext = context.applicationContext
    if (controller != null || controllerFuture != null) return
    try {
      val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
      controllerFuture = MediaController.Builder(context, sessionToken).buildAsync().also { future ->
        future.addListener({
          runCatching {
            val c = future.get(); controller = c; c.addListener(playerListener)
            c.volume = _playerState.value.volume
            c.repeatMode = when (_playerState.value.repeatMode) { RepeatMode.OFF -> Player.REPEAT_MODE_OFF; RepeatMode.ALL -> Player.REPEAT_MODE_ALL; RepeatMode.ONE -> Player.REPEAT_MODE_ONE }
            Log.d(TAG, "MediaController connected to PlaybackService successfully")
          }.onFailure { e -> Log.e(TAG, "Failed to resolve MediaController: ${e.message}", e) }
        }, MoreExecutors.directExecutor())
      }
    } catch (e: Exception) { Log.e(TAG, "Failed to create SessionToken: ${e.message}", e) }
  }

  fun setAudioQuality(quality: String) { audioQuality = quality }
  fun setCrossfadeSeconds(seconds: Int) { crossfadeSeconds = seconds.coerceIn(0, 12); if (crossfadeSeconds == 0) crossfadeJob?.cancel() }

  fun disconnect() {
    try { controller?.removeListener(playerListener); controller?.release(); controller = null; controllerFuture?.cancel(false); controllerFuture = null; appContext = null }
    catch (e: Exception) { Log.e(TAG, "Error disconnecting MediaController: ${e.message}", e) }
  }

  fun setEqualizerState(state: EqualizerState) { currentEqualizerState = state; PlaybackService.applyEqualizerState(state) }

  fun setSleepTimer(minutes: Int?) {
    _sleepTimerMinutes.value = minutes; sleepTimerJob?.cancel()
    if (minutes == null || minutes <= 0) { _sleepTimerRemainingSec.value = 0; return }
    val totalSeconds = minutes * 60; _sleepTimerRemainingSec.value = totalSeconds
    sleepTimerJob = coroutineScope.launch {
      var remaining = totalSeconds
      while (isActive && remaining > 0) { delay(1000); remaining--; _sleepTimerRemainingSec.value = remaining
        if (remaining <= 0) { pause(); _sleepTimerMinutes.value = null; _sleepTimerRemainingSec.value = 0; break }
      }
    }
  }

  fun setQueue(tracks: List<Track>, startIndex: Int = 0, autoPlay: Boolean = true) {
    if (tracks.isEmpty()) return
    originalQueue = tracks
    val activeQueue = if (_playerState.value.isShuffled) { val current = tracks.getOrNull(startIndex); val rest = tracks.filterIndexed { i, _ -> i != startIndex }.shuffled(); if (current != null) listOf(current) + rest else rest } else tracks
    val safeIndex = if (_playerState.value.isShuffled) 0 else startIndex.coerceIn(0, activeQueue.lastIndex)
    val targetTrack = activeQueue[safeIndex]
    _playerState.value = _playerState.value.copy(queue = activeQueue, queueIndex = safeIndex, currentTrack = targetTrack, currentPositionSec = 0, durationSec = targetTrack.durationSec)
    if (autoPlay) playTrack(targetTrack)
  }

  fun playTrack(track: Track, queue: List<Track> = emptyList()) {
    if (queue.isNotEmpty() && queue != originalQueue) { originalQueue = queue; val index = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0); _playerState.value = _playerState.value.copy(queue = queue, queueIndex = index) }
    streamResolveJob?.cancel()
    _playerState.value = _playerState.value.copy(currentTrack = track, isPlaying = true, isBuffering = true, currentPositionSec = 0, durationSec = track.durationSec)
    val offlineUri = appContext?.let { OfflineDownloadManager.localUri(it, track.id) }
    if (offlineUri != null) playMediaUri(offlineUri, track)
    else if (track.audioUrl.isNotBlank() && (track.audioUrl.startsWith("http") || track.audioUrl.startsWith("content://") || track.audioUrl.startsWith("file://"))) playMediaUri(Uri.parse(track.audioUrl), track)
    else {
      streamResolveJob = coroutineScope.launch {
        val resolvedUrl = StreamResolver.resolveStreamUrl(track.id, audioQuality)
        if (resolvedUrl != null) withContext(Dispatchers.Main) { playMediaUri(Uri.parse(resolvedUrl), track) }
        else if (track.audioUrl.isNotBlank()) withContext(Dispatchers.Main) { playMediaUri(Uri.parse(track.audioUrl), track) }
        else _playerState.value = _playerState.value.copy(isBuffering = false)
      }
    }
    startProgressLoop()
  }

  private fun playMediaUri(uri: Uri, track: Track) {
    try {
      val metadata = MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist).setAlbumTitle(track.album).setArtworkUri(if (track.coverUrl.isNotBlank()) Uri.parse(track.coverUrl) else null).build()
      val item = MediaItem.Builder().setMediaId(track.id).setUri(uri).setMediaMetadata(metadata).build()
      controller?.apply { setMediaItem(item); prepare(); playWhenReady = true }
    } catch (e: Exception) { Log.e(TAG, "Error setting media item on MediaController: ${e.message}", e) }
  }

  fun togglePlayPause() { val state = _playerState.value; if (state.currentTrack == null) { if (state.queue.isNotEmpty()) playTrack(state.queue[state.queueIndex.coerceIn(0, state.queue.lastIndex)]); return }; if (state.isPlaying) pause() else resume() }
  fun pause() { _playerState.value = _playerState.value.copy(isPlaying = false); controller?.playWhenReady = false; progressTimerJob?.cancel() }
  fun resume() { val track = _playerState.value.currentTrack ?: return; _playerState.value = _playerState.value.copy(isPlaying = true); controller?.let { if (it.playbackState == Player.STATE_IDLE || it.mediaItemCount == 0) playTrack(track) else it.playWhenReady = true }; startProgressLoop() }
  fun seekTo(positionSec: Int) { val track = _playerState.value.currentTrack ?: return; val clamped = positionSec.coerceIn(0, track.durationSec.coerceAtLeast(300)); _playerState.value = _playerState.value.copy(currentPositionSec = clamped); controller?.seekTo((clamped * 1000).toLong()) }

  fun nextTrack() {
    val state = _playerState.value; if (state.queue.isEmpty()) return
    val nextIndex = if (state.queueIndex + 1 < state.queue.size) state.queueIndex + 1 else if (state.repeatMode != RepeatMode.OFF) 0 else state.queueIndex
    if (nextIndex < state.queue.size) { _playerState.value = _playerState.value.copy(queueIndex = nextIndex); playTrack(state.queue[nextIndex]) } else pause()
  }
  fun previousTrack() { val state = _playerState.value; if (state.queue.isEmpty()) return; if (state.currentPositionSec > 3) { seekTo(0); return }; val prevIndex = if (state.queueIndex - 1 >= 0) state.queueIndex - 1 else state.queue.lastIndex; _playerState.value = _playerState.value.copy(queueIndex = prevIndex); playTrack(state.queue[prevIndex]) }

  fun toggleShuffle() {
    val current = _playerState.value; val newShuffle = !current.isShuffled; val currTrack = current.currentTrack
    val newQueue = if (newShuffle) { val filtered = originalQueue.filter { it.id != currTrack?.id }.shuffled(); if (currTrack != null) listOf(currTrack) + filtered else filtered } else originalQueue
    val newIndex = if (currTrack != null) newQueue.indexOfFirst { it.id == currTrack.id }.coerceAtLeast(0) else 0
    _playerState.value = current.copy(isShuffled = newShuffle, queue = newQueue, queueIndex = newIndex)
  }
  fun cycleRepeatMode() { val nextMode = when (_playerState.value.repeatMode) { RepeatMode.OFF -> RepeatMode.ALL; RepeatMode.ALL -> RepeatMode.ONE; RepeatMode.ONE -> RepeatMode.OFF }; _playerState.value = _playerState.value.copy(repeatMode = nextMode); controller?.repeatMode = when (nextMode) { RepeatMode.OFF -> Player.REPEAT_MODE_OFF; RepeatMode.ALL -> Player.REPEAT_MODE_ALL; RepeatMode.ONE -> Player.REPEAT_MODE_ONE } }
  fun setVolume(vol: Float) { val clamped = vol.coerceIn(0f, 1f); _playerState.value = _playerState.value.copy(volume = clamped); controller?.volume = clamped }

  private fun startProgressLoop() {
    progressTimerJob?.cancel()
    progressTimerJob = coroutineScope.launch {
      var animStep = 0
      while (isActive && _playerState.value.isPlaying) {
        delay(200); val c = controller
        if (c != null && c.isPlaying) {
          val posSec = (c.currentPosition / 1000).toInt(); val durSec = (c.duration / 1000).toInt()
          if (crossfadeSeconds > 0 && durSec > 0 && _playerState.value.repeatMode != RepeatMode.ONE && durSec - posSec <= crossfadeSeconds && crossfadeJob?.isActive != true) {
            crossfadeJob = coroutineScope.launch { val baseVolume = _playerState.value.volume; val steps = 8; repeat(steps) { step -> controller?.volume = baseVolume * (1f - (step + 1) / steps.toFloat()); delay((crossfadeSeconds * 1000L / steps).coerceAtLeast(40L)) }; nextTrack(); controller?.volume = baseVolume }
          }
          val eqMultiplier = if (currentEqualizerState.isEnabled) 1.0f + (currentEqualizerState.bassBoost * 0.5f) else 1.0f
          val bars = List(32) { idx ->
            val harmonicRatio = 1.0f - (idx / 32f) * 0.25f
            val eqGainMultiplier = if (currentEqualizerState.isEnabled) { val bandIdx = (idx * 5 / 32).coerceIn(0, 4); val gain = currentEqualizerState.bandGains.getOrElse(bandIdx) { 0f }; 1.0f + (gain / 12f) * 0.5f } else 1.0f
            val beatPulse = if (idx < 6) sin(animStep * 0.35).toFloat().coerceAtLeast(0f) * 0.45f * eqMultiplier else 0f
            val wave = sin(animStep * 0.2 + idx * 0.4).toFloat().coerceAtLeast(0.08f); val randomFactor = 0.5f + Random.nextFloat() * 0.5f
            ((wave * harmonicRatio * eqGainMultiplier * randomFactor) + beatPulse).coerceIn(0.08f, 1.0f)
          }
          _playerState.value = _playerState.value.copy(currentPositionSec = posSec, durationSec = if (durSec > 0) durSec else _playerState.value.durationSec, visualizerBars = bars)
        }
        animStep++
      }
    }
  }

  fun release() { sleepTimerJob?.cancel(); crossfadeJob?.cancel(); progressTimerJob?.cancel(); streamResolveJob?.cancel(); disconnect() }
}
