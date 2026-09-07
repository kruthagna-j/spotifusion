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

@UnstableApi
class PlaybackService : MediaSessionService() {

  private var mediaSession: MediaSession? = null
  private var hwEqualizer: Equalizer? = null
  private var hwBassBoost: BassBoost? = null
  private var hwVirtualizer: Virtualizer? = null

  companion object {
    private const val TAG = "PlaybackService"
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
        true // handleAudioFocus
      )
      .setHandleAudioBecomingNoisy(true)
      .build().apply {
        repeatMode = Player.REPEAT_MODE_ALL

        addListener(object : Player.Listener {
          override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
              setupAudioEffects(audioSessionId)
            }
          }

          override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
              setupAudioEffects(audioSessionId)
            }
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
        hwEqualizer = Equalizer(0, sessionId).apply {
          enabled = pendingEqualizerState.isEnabled
        }
      }
      if (hwBassBoost == null) {
        hwBassBoost = BassBoost(0, sessionId).apply {
          enabled = pendingEqualizerState.isEnabled
          setStrength((pendingEqualizerState.bassBoost * 1000).toInt().toShort())
        }
      }
      if (hwVirtualizer == null) {
        hwVirtualizer = Virtualizer(0, sessionId).apply {
          enabled = pendingEqualizerState.isEnabled
          setStrength((pendingEqualizerState.virtualizer * 1000).toInt().toShort())
        }
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

      if (state.isEnabled) {
        val numBands = hwEqualizer?.numberOfBands?.toInt() ?: 0
        for (i in 0 until numBands) {
          val gainDb = state.bandGains.getOrElse(i) { 0f }
          val milliBels = (gainDb * 100).toInt().toShort()
          hwEqualizer?.setBandLevel(i.toShort(), milliBels)
        }
        hwBassBoost?.setStrength((state.bassBoost * 1000).toInt().toShort())
        hwVirtualizer?.setStrength((state.virtualizer * 1000).toInt().toShort())
      }
    } catch (e: Exception) {
      Log.w(TAG, "Could not apply hardware EQ: ${e.message}")
    }
  }

  override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
    return mediaSession
  }

  override fun onTaskRemoved(rootIntent: Intent?) {
    val player = mediaSession?.player
    if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
      stopSelf()
    }
  }

  override fun onDestroy() {
    if (instance === this) {
      instance = null
    }
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
