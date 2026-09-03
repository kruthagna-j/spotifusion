package com.spotifusion.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Small local persistence layer for user music state. Files never leave the device. */
class UserMusicStore(context: Context) {
    private val prefs = context.getSharedPreferences("spotifusion_music", Context.MODE_PRIVATE)

    fun isFavorite(trackId: Long): Boolean = favorites().contains(trackId)
    fun favoriteIds(): Set<Long> = favorites()

    fun toggleFavorite(trackId: Long): Boolean {
        val set = favorites()
        if (!set.add(trackId)) set.remove(trackId)
        saveFavorites(set)
        return set.contains(trackId)
    }

    fun addRecentlyPlayed(track: LocalTrack) {
        val list = recent().filterNot { it.id == track.id }.toMutableList()
        list.add(0, track)
        saveRecent(list.take(50))
    }

    fun recentlyPlayed(): List<LocalTrack> = recent()

    private fun favorites(): MutableSet<Long> = prefs.getStringSet("favorites", emptySet())
        ?.mapNotNull { it.toLongOrNull() }?.toMutableSet() ?: mutableSetOf()

    private fun saveFavorites(set: Set<Long>) = prefs.edit()
        .putStringSet("favorites", set.map(Long::toString).toSet()).apply()

    private fun recent(): List<LocalTrack> {
        val json = prefs.getString("recent", "[]") ?: "[]"
        val array = runCatching { JSONArray(json) }.getOrDefault(JSONArray())
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                add(LocalTrack(
                    id = o.optLong("id"), uri = android.net.Uri.parse(o.optString("uri")),
                    title = o.optString("title"), artist = o.optString("artist"),
                    album = o.optString("album"), durationMs = o.optLong("duration")
                ))
            }
        }
    }

    private fun saveRecent(list: List<LocalTrack>) {
        val array = JSONArray()
        list.forEach { t -> array.put(JSONObject().apply {
            put("id", t.id); put("uri", t.uri.toString()); put("title", t.title)
            put("artist", t.artist); put("album", t.album); put("duration", t.durationMs)
        }) }
        prefs.edit().putString("recent", array.toString()).apply()
    }
}
