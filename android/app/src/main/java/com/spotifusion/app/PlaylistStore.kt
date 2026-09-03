package com.spotifusion.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Device-only playlist persistence for local tracks. */
class PlaylistStore(context: Context) {
    private val prefs = context.getSharedPreferences("spotifusion_playlists", Context.MODE_PRIVATE)

    data class Playlist(val id: String, val name: String, val trackIds: List<Long>)

    fun all(): List<Playlist> = decode(prefs.getString("data", "[]") ?: "[]")

    fun create(name: String): Playlist? {
        val clean = name.trim()
        if (clean.isBlank()) return null
        val existing = all().toMutableList()
        if (existing.any { it.name.equals(clean, ignoreCase = true) }) return null
        val playlist = Playlist(java.util.UUID.randomUUID().toString(), clean, emptyList())
        save(existing + playlist)
        return playlist
    }

    fun addTrack(playlistId: String, trackId: Long) {
        save(all().map { p ->
            if (p.id == playlistId && trackId !in p.trackIds) p.copy(trackIds = p.trackIds + trackId) else p
        })
    }

    fun removeTrack(playlistId: String, trackId: Long) {
        save(all().map { p -> if (p.id == playlistId) p.copy(trackIds = p.trackIds.filterNot { it == trackId }) else p })
    }

    fun delete(playlistId: String) { save(all().filterNot { it.id == playlistId }) }

    private fun save(list: List<Playlist>) {
        val array = JSONArray()
        list.forEach { p ->
            array.put(JSONObject().apply {
                put("id", p.id); put("name", p.name)
                put("tracks", JSONArray(p.trackIds))
            })
        }
        prefs.edit().putString("data", array.toString()).apply()
    }

    private fun decode(raw: String): List<Playlist> {
        val array = runCatching { JSONArray(raw) }.getOrDefault(JSONArray())
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val tracks = o.optJSONArray("tracks")
                val ids = buildList { if (tracks != null) for (j in 0 until tracks.length()) add(tracks.optLong(j)) }
                add(Playlist(o.optString("id"), o.optString("name"), ids))
            }
        }
    }
}
