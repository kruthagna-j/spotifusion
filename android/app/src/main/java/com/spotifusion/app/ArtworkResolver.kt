package com.spotifusion.app

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore

/** Resolves local album artwork without uploading audio files. */
object ArtworkResolver {
    fun load(context: Context, albumId: Long, size: Int = 512): Bitmap? {
        if (albumId <= 0L) return null
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
        return runCatching {
            context.contentResolver.loadThumbnail(uri, android.util.Size(size, size), null)
        }.getOrNull()
    }
}
