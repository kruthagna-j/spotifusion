package com.spotifusion.app

import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import androidx.media3.session.MediaController

/** UI-facing snapshot of the single Media3 player. */
data class PlayerState(
    val title: String = "",
    val artist: String = "",
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF
)

class PlayerStateBridge(private val controller: MediaController) {
    private val handler = Handler(Looper.getMainLooper())
    private var listener: ((PlayerState) -> Unit)? = null
    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = emit()
    }

    init { controller.addListener(playerListener) }

    fun observe(onState: (PlayerState) -> Unit) {
        listener = onState
        emit()
        handler.post(positionTicker)
    }

    fun clear() {
        listener = null
        handler.removeCallbacks(positionTicker)
        controller.removeListener(playerListener)
    }

    private val positionTicker = object : Runnable {
        override fun run() {
            emit()
            handler.postDelayed(this, 500L)
        }
    }

    private fun emit() {
        val metadata = controller.mediaMetadata
        listener?.invoke(
            PlayerState(
                title = metadata.title?.toString().orEmpty(),
                artist = metadata.artist?.toString().orEmpty(),
                durationMs = controller.duration.coerceAtLeast(0L),
                positionMs = controller.currentPosition.coerceAtLeast(0L),
                isPlaying = controller.isPlaying,
                shuffle = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode
            )
        )
    }
}
