package com.spotifusion.app

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

/** UI-safe bridge to the background Media3 playback service. */
object PlaybackController {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private val listeners = mutableSetOf<Player.Listener>()

    fun connect(context: Context) {
        if (controller != null || controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, token).buildAsync().also { future ->
            future.addListener({
                runCatching {
                    controller = future.get()
                    controller?.let { c -> listeners.toList().forEach(c::addListener) }
                }
            }, MoreExecutors.directExecutor())
        }
    }

    fun disconnect() {
        controller?.let { c -> listeners.toList().forEach(c::removeListener) }
        controller?.release()
        controller = null
        controllerFuture?.cancel(false)
        controllerFuture = null
    }

    private fun item(uri: String, title: String, artist: String, album: String? = null): MediaItem =
        MediaItem.Builder().setUri(uri).setMediaMetadata(
            MediaMetadata.Builder().setTitle(title).setArtist(artist).apply { album?.let { setAlbumTitle(it) } }.build()
        ).build()

    fun play(uri: String, title: String, artist: String, album: String? = null) {
        controller?.let { c -> c.setMediaItem(item(uri, title, artist, album)); c.prepare(); c.play() }
    }

    fun playTracks(tracks: List<LocalTrack>, startIndex: Int = 0) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        val items = tracks.map { item(it.uri.toString(), it.title, it.artist, it.album) }
        val index = startIndex.coerceIn(0, items.lastIndex)
        c.setMediaItems(items, index, 0L)
        c.prepare()
        c.play()
    }

    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun pause() { controller?.pause() }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seek(positionMs: Long) { controller?.seekTo(positionMs.coerceAtLeast(0L)) }
    fun setVolume(value: Float) { controller?.volume = value.coerceIn(0f, 1f) }
    fun volume(): Float = controller?.volume ?: 0.75f
    fun setShuffle(enabled: Boolean) { controller?.shuffleModeEnabled = enabled }
    fun setRepeat(enabled: Boolean) { controller?.repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF }
    fun isConnected(): Boolean = controller != null
    fun isPlaying(): Boolean = controller?.isPlaying == true
    fun position(): Long = controller?.currentPosition ?: 0L
    fun duration(): Long = controller?.duration?.coerceAtLeast(0L) ?: 0L

    fun addListener(listener: Player.Listener) {
        listeners += listener
        controller?.addListener(listener)
    }

    fun removeListener(listener: Player.Listener) {
        listeners -= listener
        controller?.removeListener(listener)
    }

    fun currentTitle(): String = controller?.mediaMetadata?.title?.toString().orEmpty()
    fun currentArtist(): String = controller?.mediaMetadata?.artist?.toString().orEmpty()
    fun currentAlbum(): String = controller?.mediaMetadata?.albumTitle?.toString().orEmpty()
    fun isShuffleEnabled(): Boolean = controller?.shuffleModeEnabled == true
    fun isRepeatEnabled(): Boolean = controller?.repeatMode == Player.REPEAT_MODE_ONE
}
