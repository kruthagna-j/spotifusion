package com.spotifusion.app

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

/** Reads audio already indexed on the Android device. Nothing is uploaded. */
data class LocalTrack(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long
)

object LocalMusicScanner {
    fun scan(context: Context): List<LocalTrack> {
        val resolver: ContentResolver = context.contentResolver
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.IS_MUSIC
        )
        val tracks = mutableListOf<LocalTrack>()
        resolver.query(
            collection,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            while (cursor.moveToNext()) {
                val trackId = cursor.getLong(id)
                tracks += LocalTrack(
                    id = trackId,
                    uri = Uri.withAppendedPath(collection, trackId.toString()),
                    title = cursor.getString(title).orEmpty().ifBlank { "Unknown title" },
                    artist = cursor.getString(artist).orEmpty().ifBlank { "Unknown artist" },
                    album = cursor.getString(album).orEmpty().ifBlank { "Unknown album" },
                    durationMs = cursor.getLong(duration).coerceAtLeast(0L)
                )
            }
        }
        return tracks
    }
}
