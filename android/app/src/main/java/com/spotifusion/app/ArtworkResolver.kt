package com.spotifusion.app

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore

/** Resolves local album artwork without uploading audio files. */
object ArtworkResolver {
    fun uri(albumId: Long): Uri = if (albumId > 0L) ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId) else Uri.EMPTY
    fun load(context: Context, albumId: Long, size: Int = 512): Bitmap? {
        val uri = uri(albumId)
        if (uri == Uri.EMPTY) return null
        return runCatching { context.contentResolver.loadThumbnail(uri, android.util.Size(size,size), null) }.getOrNull()
    }
}