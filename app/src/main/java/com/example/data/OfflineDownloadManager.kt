package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.remote.StreamResolver
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/** Real offline storage for tracks explicitly downloaded by the user. */
object OfflineDownloadManager {
  private const val DIR = "offline_music"
  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()

  private fun folder(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }
  private fun audioFile(context: Context, trackId: String): File = File(folder(context), "$trackId.audio")

  suspend fun download(context: Context, track: Track, quality: String): File? = withContext(Dispatchers.IO) {
    val url = if (track.audioUrl.startsWith("http")) track.audioUrl
    else StreamResolver.resolveStreamUrl(track.id, quality) ?: return@withContext null

    val destination = audioFile(context, track.id)
    val temp = File(destination.parentFile, destination.name + ".part")
    try {
      val response = client.newCall(Request.Builder().url(url).build()).execute()
      if (!response.isSuccessful) return@withContext null
      response.body?.byteStream()?.use { input ->
        temp.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
      } ?: return@withContext null
      if (destination.exists()) destination.delete()
      temp.renameTo(destination)
      destination
    } catch (_: Exception) {
      temp.delete()
      null
    }
  }

  fun isDownloaded(context: Context, trackId: String): Boolean = audioFile(context, trackId).exists()

  fun localUri(context: Context, trackId: String): Uri? {
    val file = audioFile(context, trackId)
    return if (file.exists()) Uri.fromFile(file) else null
  }

  fun delete(context: Context, trackId: String) { audioFile(context, trackId).delete() }

  fun clear(context: Context) { folder(context).deleteRecursively() }

  fun downloadedIds(context: Context): Set<String> = folder(context).listFiles()?.filter { it.isFile && it.name.endsWith(".audio") }?.map { it.name.removeSuffix(".audio") }?.toSet() ?: emptySet()

  fun sizeMb(context: Context): Float {
    val bytes = folder(context).walkTopDown().filter { it.isFile }.sumOf { it.length() }
    return bytes / 1024f / 1024f
  }
}
