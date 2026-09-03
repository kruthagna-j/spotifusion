package com.spotifusion.app

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

/** Fetches lyrics by track metadata. No audio files are uploaded. */
object LyricsRepository {
    data class Line(val timeMs: Long, val text: String)
    data class Result(val plain: String, val lines: List<Line>)

    fun fetch(title: String, artist: String, album: String = "", durationSec: Int? = null): Result? {
        if (title.isBlank() || artist.isBlank()) return null
        val query = buildString {
            append("https://lrclib.net/api/get?track_name=")
            append(URLEncoder.encode(title, "UTF-8"))
            append("&artist_name=")
            append(URLEncoder.encode(artist, "UTF-8"))
            if (album.isNotBlank()) {
                append("&album_name=")
                append(URLEncoder.encode(album, "UTF-8"))
            }
            durationSec?.let {
                append("&duration=")
                append(it)
            }
        }
        val connection = (URL(query).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7000
            readTimeout = 10000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Spotifusion/1.1")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val plain = json.optString("plainLyrics", "")
            val synced = json.optString("syncedLyrics", "")
            Result(plain, parseLrc(synced))
        } finally {
            connection.disconnect()
        }
    }

    private fun parseLrc(value: String): List<Line> {
        if (value.isBlank()) return emptyList()
        val result = mutableListOf<Line>()
        val regex = Regex("\\[(\\d{1,2}):(\\d{2})(?:\\.(\\d{1,3}))?\\](.*)")
        value.lineSequence().forEach { raw ->
            val match = regex.matchEntire(raw.trim()) ?: return@forEach
            val minutes = match.groupValues[1].toLong()
            val seconds = match.groupValues[2].toLong()
            val fraction = match.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L
            result += Line((minutes * 60 + seconds) * 1000 + fraction, match.groupValues[4].trim())
        }
        return result.sortedBy { it.timeMs }
    }
}
