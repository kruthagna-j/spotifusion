package com.spotifusion.app

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore

/** Resolves embedded/local album artwork without uploading the audio file. */
object ArtworkResolver {
    fun load(context: Context, trackId: Long, size: Int = 512): Bitmap? {
        val uri = ContentUris.withAppendedId(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, trackId
        )
        return runCatching {
            context.contentResolver.loadThumbnail(uri, android.util.Size(size, size), null)
        }.getOrNull()
    }
}
