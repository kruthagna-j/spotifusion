package com.example.service

import android.app.PendingIntent
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.model.EqualizerState
import kotlin.math.abs

@UnstableApi
class PlaybackService : MediaSessionService() {

  private var mediaSession: MediaSession? = null
  private var hwEqualizer: Equalizer? = null
  private var hwBassBoost: BassBoost? = null
  private var hwVirtualizer: Virtualizer? = null

  companion object {
    private const val TAG = "PlaybackService"
    private val TARGET_FREQUENCIES_HZ = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)
    private var instance: PlaybackService? = null
    private var pendingEqualizerState: EqualizerState = EqualizerState()

    fun applyEqualizerState(state: EqualizerState) {
      pendingEqualizerState = state
      instance?.updateAudioEffects(state)
    }
  }

  override fun onCreate() {
    super.onCreate()
    instance = this

    val player = ExoPlayer.Builder(this)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
          .setUsage(C.USAGE_MEDIA)
          .build(),
        true
      )
      .setHandleAudioBecomingNoisy(true)
      .build().apply {
        repeatMode = Player.REPEAT_MODE_ALL
        addListener(object : Player.Listener {
          override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) setupAudioEffects(audioSessionId)
          }

          override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) setupAudioEffects(audioSessionId)
          }
        })
      }

    val sessionActivityPendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    mediaSession = MediaSession.Builder(this, player)
      .setSessionActivity(sessionActivityPendingIntent)
      .build()
  }

  private fun setupAudioEffects(sessionId: Int) {
    if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == 0) return
    try {
      if (hwEqualizer == null || hwEqualizer?.hasControl() != true) {
        hwEqualizer = Equalizer(0, sessionId)
      }
      if (hwBassBoost == null) {
        hwBassBoost = BassBoost(0, sessionId)
      }
      if (hwVirtualizer == null) {
        hwVirtualizer = Virtualizer(0, sessionId)
      }
      updateAudioEffects(pendingEqualizerState)
    } catch (e: Exception) {
      Log.w(TAG, "Hardware audio effects initialization notice: ${e.message}")
    }
  }

  fun updateAudioEffects(state: EqualizerState) {
    try {
      hwEqualizer?.enabled = state.isEnabled
      hwBassBoost?.enabled = state.isEnabled
      hwVirtualizer?.enabled = state.isEnabled

      if (!state.isEnabled) return

      val equalizer = hwEqualizer
      if (equalizer != null) {
        val numberOfBands = equalizer.numberOfBands.toInt()
        val lower = equalizer.bandLevelRange[0].toInt()
        val upper = equalizer.bandLevelRange[1].toInt()

        // Devices expose different numbers of EQ bands. Map each hardware band
        // to the nearest one of Spotifusion's five UI bands instead of assuming
        // the device has exactly five bands.
        for (band in 0 until numberOfBands) {
          val centerHz = equalizer.getCenterFreq(band.toShort()) / 1000f
          var nearest = 0
          var nearestDistance = Float.MAX_VALUE
          for (target in TARGET_FREQUENCIES_HZ.indices) {
            val distance = abs(centerHz - TARGET_FREQUENCIES_HZ[target])
            if (distance < nearestDistance) {
              nearestDistance = distance
              nearest = target
            }
          }

          val requestedMilliBel = (state.bandGains.getOrElse(nearest) { 0f } * 100f).toInt()
          val clampedMilliBel = requestedMilliBel.coerceIn(lower, upper).toShort()
          equalizer.setBandLevel(band.toShort(), clampedMilliBel)
        }
      }

      hwBassBoost?.setStrength((state.bassBoost.coerceIn(0f, 1f) * 1000f).toInt().toShort())
      hwVirtualizer?.setStrength((state.virtualizer.coerceIn(0f, 1f) * 1000f).toInt().toShort())
    } catch (e: Exception) {
      Log.w(TAG, "Could not apply hardware EQ: ${e.message}")
    }
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = mediaSession?.player
    if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
  }

  override fun onDestroy() {
    if (instance === this) instance = null
    try {
      hwEqualizer?.release()
      hwBassBoost?.release()
      hwVirtualizer?.release()
    } catch (_: Exception) {}
    hwEqualizer = null
    hwBassBoost = null
    hwVirtualizer = null

    mediaSession?.run {
      player.release()
      release()
      mediaSession = null
    }
    super.onDestroy()
  }
}
