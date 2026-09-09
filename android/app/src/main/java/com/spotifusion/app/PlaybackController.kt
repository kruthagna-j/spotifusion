package com.spotifusion.app

import android.content.ComponentName
import android.content.Context
import android.net.Uri
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
            future.addListener({ runCatching { controller = future.get(); controller?.let { c -> listeners.toList().forEach(c::addListener) } } }, MoreExecutors.directExecutor())
        }
    }
    fun disconnect() {
        controller?.let { c -> listeners.toList().forEach(c::removeListener); c.release() }
        controller = null; controllerFuture?.cancel(false); controllerFuture = null
    }
    private fun item(uri: String, title: String, artist: String, album: String? = null, mediaId: String = uri, artworkUri: String? = null): MediaItem =
        MediaItem.Builder().setUri(uri).setMediaId(mediaId).setMediaMetadata(
            MediaMetadata.Builder().setTitle(title).setArtist(artist).apply { album?.let { setAlbumTitle(it) }; artworkUri?.let { setArtworkUri(Uri.parse(it)) } }.build()
        ).build()
    fun play(uri: String, title: String, artist: String, album: String? = null, mediaId: String = uri, artworkUri: String? = null) {
        controller?.let { c -> c.setMediaItem(item(uri,title,artist,album,mediaId,artworkUri)); c.prepare(); c.play() }
    }
    fun playTracks(tracks: List<LocalTrack>, startIndex: Int = 0) {
        val c = controller ?: return; if (tracks.isEmpty()) return
        val items = tracks.map { item(it.uri.toString(),it.title,it.artist,it.album,it.id.toString(),ArtworkResolver.uri(it.albumId).toString()) }
        c.setMediaItems(items, startIndex.coerceIn(0,items.lastIndex),0L); c.prepare(); c.play()
    }
    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun pause() { controller?.pause() }
    fun next() { controller?.seekToNextMediaItem() }
    fun previous() { controller?.seekToPreviousMediaItem() }
    fun seek(positionMs: Long) { controller?.seekTo(positionMs.coerceAtLeast(0L)) }
    fun setVolume(value: Float) { controller?.volume=value.coerceIn(0f,1f) }
    fun volume(): Float = controller?.volume ?: .75f
    fun setShuffle(enabled:Boolean){controller?.shuffleModeEnabled=enabled}
    fun setRepeat(enabled:Boolean){controller?.repeatMode=if(enabled)Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF}
    fun isConnected()=controller!=null
    fun isPlaying()=controller?.isPlaying==true
    fun position()=controller?.currentPosition?:0L
    fun duration()=controller?.duration?.coerceAtLeast(0L)?:0L
    fun audioSessionId(): Int = controller?.audioSessionId ?: 0
    fun addListener(listener:Player.Listener){listeners+=listener;controller?.addListener(listener)}
    fun removeListener(listener:Player.Listener){listeners-=listener;controller?.removeListener(listener)}
    fun currentMediaId():String?=controller?.currentMediaItem?.mediaId
    fun currentTitle()=controller?.mediaMetadata?.title?.toString().orEmpty()
    fun currentArtist()=controller?.mediaMetadata?.artist?.toString().orEmpty()
    fun currentAlbum()=controller?.mediaMetadata?.albumTitle?.toString().orEmpty()
    fun currentArtworkUri()=controller?.mediaMetadata?.artworkUri
    fun isShuffleEnabled()=controller?.shuffleModeEnabled==true
    fun isRepeatEnabled()=controller?.repeatMode==Player.REPEAT_MODE_ONE
}