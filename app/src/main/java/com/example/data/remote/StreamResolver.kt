package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object StreamResolver {
  private const val TAG = "StreamResolver"

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(12, TimeUnit.SECONDS)
    .build()

  private data class CachedStream(
    val url: String,
    val expiresAt: Long
  )

  private val streamCache = ConcurrentHashMap<String, CachedStream>()

  private val INVIDIOUS_INSTANCES = listOf(
    "https://invidious.protokolla.fi",
    "https://invidious.private.coffee",
    "https://inv.nadeko.net",
    "https://invidious.nerdvpn.de",
    "https://invidious.jing.rocks",
    "https://yt.drgnz.club"
  )

  suspend fun resolveStreamUrl(videoId: String, quality: String = "High (320kbps)"): String? = withContext(Dispatchers.IO) {
    if (videoId.isBlank()) return@withContext null

    val cacheKey = "$videoId|$quality"
    val cached = streamCache[cacheKey]
    if (cached != null && System.currentTimeMillis() < cached.expiresAt) {
      return@withContext cached.url
    }

    try {
      val response = ApiClient.service.getStream(videoId, quality)
      val streamUrl = response.url
      if (!streamUrl.isNullOrBlank()) {
        val ttlMs = 15 * 60 * 1000L
        streamCache[cacheKey] = CachedStream(streamUrl, System.currentTimeMillis() + ttlMs)
        return@withContext streamUrl
      }
    } catch (e: Exception) {
      Log.w(TAG, "Backend stream resolver failed for $videoId: ${e.message}")
    }

    for (instance in INVIDIOUS_INSTANCES) {
      try {
        val req = Request.Builder()
          .url("$instance/api/v1/videos/${videoId}?fields=adaptiveFormats,formatStreams")
          .header("User-Agent", "Mozilla/5.0 SpotifusionAndroid/1.0")
          .build()

        val resp = httpClient.newCall(req).execute()
        if (resp.isSuccessful) {
          val jsonStr = resp.body?.string() ?: continue
          val json = JSONObject(jsonStr)
          val adaptive = json.optJSONArray("adaptiveFormats")
          var bestAudioUrl: String? = null
          var bestScore = Long.MAX_VALUE
          val targetBitrate = when {
            quality.startsWith("Normal") -> 160_000
            quality.startsWith("High") -> 320_000
            else -> 512_000
          }

          if (adaptive != null) {
            for (i in 0 until adaptive.length()) {
              val item = adaptive.optJSONObject(i) ?: continue
              val type = item.optString("type", "")
              if (type.startsWith("audio/")) {
                val bitrate = item.optInt("bitrate", 0)
                val url = item.optString("url", "")
                if (url.isNotBlank() && bitrate > 0) {
                  val score = kotlin.math.abs(bitrate - targetBitrate).toLong()
                  if (score < bestScore) {
                    bestScore = score
                    bestAudioUrl = url
                  }
                }
              }
            }
          }

          if (bestAudioUrl != null) {
            streamCache[cacheKey] = CachedStream(bestAudioUrl, System.currentTimeMillis() + 10 * 60 * 1000L)
            return@withContext bestAudioUrl
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Invidious instance $instance failed for $videoId: ${e.message}")
      }
    }

    null
  }

  fun clearCache() {
    streamCache.clear()
  }
}
