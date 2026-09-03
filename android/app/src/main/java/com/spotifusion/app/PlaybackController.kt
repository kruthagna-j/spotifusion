package com.spotifusion.app

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/** UI-safe bridge to the background Media3 playback service. */
object PlaybackController {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    fun connect(context: Context) {
        if (controller != null || controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync().also { future ->
            future.addListener({
                runCatching { controller = future.get() }
            }, MoreExecutors.directExecutor())
        }
    }

    fun disconnect() {
        controller?.release()
        controller = null
        controllerFuture?.cancel(false)
        controllerFuture = null
    }

    fun play(uri: String, title: String, artist: String, album: String? = null) {
        val c = controller ?: return
        val metadata = androidx.media3.common.MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .apply { album?.let { setAlbumTitle(it) } }
            .build()
        c.setMediaItem(MediaItem.Builder().setUri(uri).setMediaMetadata(metadata).build())
        c.prepare()
        c.play()
    }

    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun pause() { controller?.pause() }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seek(positionMs: Long) { controller?.seekTo(positionMs.coerceAtLeast(0L)) }
    fun setVolume(value: Float) { controller?.volume = value.coerceIn(0f, 1f) }
    fun setShuffle(enabled: Boolean) { controller?.shuffleModeEnabled = enabled }
    fun setRepeat(enabled: Boolean) { controller?.repeatMode = if (enabled) 1 else 0 }

    fun isConnected(): Boolean = controller != null
    fun isPlaying(): Boolean = controller?.isPlaying == true
    fun position(): Long = controller?.currentPosition ?: 0L
    fun duration(): Long = controller?.duration?.coerceAtLeast(0L) ?: 0L
}
