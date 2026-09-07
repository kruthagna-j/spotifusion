package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.HistoryEntity
import com.example.data.local.LikedTrackEntity
import com.example.data.local.MusicDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackEntity
import com.example.data.remote.ApiClient
import com.example.data.remote.TrackDto
import com.example.model.FusionBlend
import com.example.model.Playlist
import com.example.model.SyncedLyricLine
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class MusicRepository(
  private val musicDao: MusicDao,
  private val context: Context? = null
) {

  private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(8, TimeUnit.SECONDS)
    .readTimeout(10, TimeUnit.SECONDS)
    .build()

  private val lyricsMemoryCache = ConcurrentHashMap<String, List<SyncedLyricLine>>()

  // Curated initial track database as fallback & baseline
  val catalogTracks: List<Track> = listOf(
    Track(
      id = "track_1",
      title = "Starfall Horizons",
      artist = "Astral Dreamer",
      album = "Cosmic Odyssey",
      durationSec = 214,
      coverUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=600&auto=format&fit=crop&q=80",
      genre = "Synthwave",
      lyrics = listOf(
        "Neon lights reflecting in the midnight rain",
        "Driving through the city, washed away the pain",
        "Can you feel the pulse beneath the glowing sky?",
        "We are starfall travelers, you and I",
        "Into the infinite horizon we fly...",
        "Lost in the waves of a neon glow",
        "Where the synthetic currents flow"
      ),
      year = "2024",
      plays = 142300
    ),
    Track(
      id = "track_2",
      title = "Midnight Coffee",
      artist = "Komorebi",
      album = "Window Rain Whispers",
      durationSec = 178,
      coverUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&auto=format&fit=crop&q=80",
      genre = "Lo-Fi",
      lyrics = listOf(
        "Steam rising softly from the porcelain cup",
        "Clock ticking slow as the dawn wakes up",
        "Vinyl crackles in the corner room",
        "Dispelling the winter chill and gloom",
        "A quiet sanctuary just for two",
        "Sipping on warm midnight brew..."
      ),
      year = "2024",
      plays = 98400
    ),
    Track(
      id = "track_3",
      title = "Electric Pulse",
      artist = "Nova Wave",
      album = "Cybernetic Heart",
      durationSec = 195,
      coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=600&auto=format&fit=crop&q=80",
      genre = "EDM",
      lyrics = listOf(
        "Feel the bass vibrate in your core",
        "Dancing till our shadows hit the floor",
        "Frequency rising, voltage high",
        "Electric sparks across the velvet sky",
        "Drop the beat, ignite the crowd tonight!"
      ),
      year = "2024",
      plays = 320500
    ),
    Track(
      id = "track_4",
      title = "Golden Hour Reverie",
      artist = "Maya Lin",
      album = "Warm Amber",
      durationSec = 224,
      coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80",
      genre = "Pop",
      lyrics = listOf(
        "Sunlight spills across the wooden floor",
        "Never felt this kind of warmth before",
        "Every little glance makes time stand still",
        "If loving you is dreaming, then I will",
        "Golden hour kisses in the summer breeze"
      ),
      year = "2023",
      plays = 412000
    ),
    Track(
      id = "track_5",
      title = "Urban Mirage",
      artist = "K-Shadow",
      album = "Concrete Neon",
      durationSec = 186,
      coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=600&auto=format&fit=crop&q=80",
      genre = "Hip-Hop",
      lyrics = listOf(
        "Subway rumbling under city blocks",
        "Count the minutes as the clock tick-tocks",
        "Writing rhymes under amber streetlamps bright",
        "Turning our struggles into neon light",
        "From the pavement straight to the peak"
      ),
      year = "2024",
      plays = 285400
    ),
    Track(
      id = "track_6",
      title = "Coastal Drive",
      artist = "Echoes of Summer",
      album = "Pacific Breeze",
      durationSec = 208,
      coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&auto=format&fit=crop&q=80",
      genre = "Indie Rock",
      lyrics = listOf(
        "Windows rolled all the way down",
        "Leaving behind the buzzing town",
        "Salty air and crashing turquoise tide",
        "Nothing to chase, nowhere to hide",
        "Just you, the wheel, and the open road"
      ),
      year = "2023",
      plays = 175200
    )
  )

  // Curated Playlists
  val curatedPlaylists: List<Playlist> = listOf(
    Playlist(
      id = "curated_top50",
      title = "Global Top 50",
      description = "The hottest trending tracks on SpotiFusion right now.",
      coverGradientStart = 0xFF1DB954,
      coverGradientEnd = 0xFF0D47A1,
      trackCount = catalogTracks.size,
      tracks = catalogTracks
    ),
    Playlist(
      id = "curated_synth",
      title = "Cyberwave & Outrun",
      description = "Retro-futuristic beats and neon synth leads for nocturnal cruising.",
      coverGradientStart = 0xFF8B5CF6,
      coverGradientEnd = 0xFFEC4899,
      trackCount = catalogTracks.filter { it.genre in listOf("Synthwave", "EDM") }.size,
      tracks = catalogTracks.filter { it.genre in listOf("Synthwave", "EDM") }
    ),
    Playlist(
      id = "curated_lofi",
      title = "Deep Focus Lo-Fi",
      description = "Gentle beats to study, code, and relax to.",
      coverGradientStart = 0xFF10B981,
      coverGradientEnd = 0xFF065F46,
      trackCount = catalogTracks.filter { it.genre in listOf("Lo-Fi", "Acoustic") }.size,
      tracks = catalogTracks.filter { it.genre in listOf("Lo-Fi", "Acoustic") }
    )
  )

  // Preset Fusion Blends
  val presetFusionBlends: List<FusionBlend> = listOf(
    FusionBlend(
      id = "blend_1",
      title = "Neon Chill Fusion",
      subtitle = "Synthwave + Lo-Fi Beats Blend",
      userA = "Synthwave Explorer",
      userB = "Lo-Fi Dreamer",
      matchScore = 93,
      commonGenres = listOf("Synthwave", "Lo-Fi", "Electronic"),
      tracks = listOf(catalogTracks[0], catalogTracks[1], catalogTracks[4]),
      gradientStart = 0xFF1DB954,
      gradientEnd = 0xFF8B5CF6
    ),
    FusionBlend(
      id = "blend_2",
      title = "Midnight Horizon",
      subtitle = "Pop Hits + R&B Velvet",
      userA = "Pop Enthusiast",
      userB = "R&B Soul",
      matchScore = 88,
      commonGenres = listOf("Pop", "R&B", "Vocal"),
      tracks = listOf(catalogTracks[3], catalogTracks[5]),
      gradientStart = 0xFF00E5FF,
      gradientEnd = 0xFFFF5252
    ),
    FusionBlend(
      id = "blend_3",
      title = "High Voltage Groove",
      subtitle = "EDM Pulse + Hip-Hop Energy",
      userA = "Club DJ",
      userB = "Urban Beats",
      matchScore = 85,
      commonGenres = listOf("EDM", "Hip-Hop", "Bass"),
      tracks = listOf(catalogTracks[2], catalogTracks[4]),
      gradientStart = 0xFFFFB300,
      gradientEnd = 0xFF1DB954
    )
  )

  // Live Discovery from Backend
  suspend fun fetchDiscoverTracks(): List<Track> = withContext(Dispatchers.IO) {
    try {
      val response = ApiClient.service.getDiscover()
      val list = mutableListOf<Track>()
      response.sections?.forEach { section ->
        section.tracks?.forEach { dto ->
          dtoToTrack(dto)?.let { list.add(it) }
        }
      }
      if (list.isNotEmpty()) {
        return@withContext list.distinctBy { it.id }
      }
    } catch (e: Exception) {
      Log.w("MusicRepository", "Discover API fetch failed: ${e.message}")
    }
    catalogTracks
  }

  // Live Online Search
  suspend fun searchOnline(query: String, category: String = "all", batch: Int = 1): List<Track> = withContext(Dispatchers.IO) {
    val q = query.trim()
    if (q.length < 2) return@withContext emptyList()

    try {
      val response = ApiClient.service.search(query = q, category = category, batch = batch)
      val results = response.results?.mapNotNull { dtoToTrack(it) } ?: emptyList()
      if (results.isNotEmpty()) {
        return@withContext results
      }
    } catch (e: Exception) {
      Log.w("MusicRepository", "Search API failed: ${e.message}")
    }

    // Fallback: search catalog
    val all = catalogTracks
    all.filter {
      it.title.contains(q, ignoreCase = true) ||
          it.artist.contains(q, ignoreCase = true) ||
          it.album.contains(q, ignoreCase = true) ||
          it.genre.contains(q, ignoreCase = true)
    }
  }

  private fun dtoToTrack(dto: TrackDto): Track? {
    val id = dto.id ?: dto.videoId ?: return null
    val title = dto.title ?: "Untitled"
    val artist = dto.artist ?: "Unknown Artist"
    val album = dto.album ?: "Single"
    val durationSec = dto.durationSeconds ?: 210
    val thumb = dto.thumbnail ?: dto.artwork?.large ?: dto.artwork?.medium ?: ""
    val genre = dto.genre ?: "Music"

    return Track(
      id = id,
      title = title,
      artist = artist,
      album = album,
      durationSec = durationSec,
      coverUrl = thumb,
      genre = genre,
      audioUrl = "",
      year = "2024"
    )
  }

  // Liked tracks Flow
  fun getLikedTracks(): Flow<List<Track>> {
    return musicDao.getLikedTracks().map { entities ->
      entities.map { entity ->
        catalogTracks.find { it.id == entity.trackId } ?: Track(
          id = entity.trackId,
          title = entity.title,
          artist = entity.artist,
          album = entity.album,
          durationSec = entity.durationSec,
          coverUrl = entity.coverUrl,
          genre = entity.genre,
          isLiked = true
        )
      }
    }
  }

  fun isTrackLiked(trackId: String): Flow<Boolean> = musicDao.isTrackLiked(trackId)

  suspend fun toggleLike(track: Track) {
    val existing = musicDao.getLikedTracksSync().find { it.trackId == track.id }
    if (existing != null) {
      musicDao.removeLikedTrack(track.id)
    } else {
      musicDao.insertLikedTrack(
        LikedTrackEntity(
          trackId = track.id,
          title = track.title,
          artist = track.artist,
          album = track.album,
          durationSec = track.durationSec,
          coverUrl = track.coverUrl,
          genre = track.genre
        )
      )
    }
  }

  suspend fun removeLikedTrack(trackId: String) {
    musicDao.removeLikedTrack(trackId)
  }

  // Playlists Flow combining Curated + Custom Room Entities
  fun getAllPlaylists(): Flow<List<Playlist>> {
    return musicDao.getAllPlaylists().map { customEntities ->
      val customPlaylists = customEntities.map { entity ->
        Playlist(
          id = entity.id,
          title = entity.title,
          description = entity.description,
          coverGradientStart = entity.coverGradientStart,
          coverGradientEnd = entity.coverGradientEnd,
          isCustom = true
        )
      }
      curatedPlaylists + customPlaylists
    }
  }

  fun getTracksForPlaylist(playlistId: String): Flow<List<Track>> {
    val curated = curatedPlaylists.find { it.id == playlistId }
    if (curated != null) {
      return flowOf(curated.tracks)
    }

    return musicDao.getTracksForPlaylist(playlistId).map { entries ->
      entries.map { entry ->
        catalogTracks.find { it.id == entry.trackId } ?: Track(
          id = entry.trackId,
          title = entry.title,
          artist = entry.artist,
          album = entry.album,
          durationSec = entry.durationSec,
          coverUrl = entry.coverUrl,
          genre = entry.genre
        )
      }
    }
  }

  suspend fun createCustomPlaylist(title: String, description: String): String {
    val id = "playlist_${UUID.randomUUID().toString().take(8)}"
    val gradients = listOf(
      0xFF1DB954 to 0xFF0D47A1,
      0xFF8B5CF6 to 0xFF3B82F6,
      0xFFF59E0B to 0xFFEF4444,
      0xFF10B981 to 0xFF059669,
      0xFFEC4899 to 0xFF8B5CF6
    )
    val randomGradient = gradients.random()
    musicDao.insertPlaylist(
      PlaylistEntity(
        id = id,
        title = title,
        description = description,
        coverGradientStart = randomGradient.first,
        coverGradientEnd = randomGradient.second
      )
    )
    return id
  }

  suspend fun addTrackToPlaylist(playlistId: String, track: Track) {
    musicDao.addTrackToPlaylist(
      PlaylistTrackEntity(
        playlistId = playlistId,
        trackId = track.id,
        title = track.title,
        artist = track.artist,
        album = track.album,
        durationSec = track.durationSec,
        coverUrl = track.coverUrl,
        genre = track.genre
      )
    )
  }

  suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
    musicDao.removeTrackFromPlaylist(playlistId, trackId)
  }

  suspend fun deletePlaylist(playlistId: String) {
    musicDao.deletePlaylist(playlistId)
    musicDao.clearTracksForPlaylist(playlistId)
  }

  // Fusion Blend Generator
  fun createDynamicFusionBlend(genreA: String, genreB: String, name: String = "", basePool: List<Track> = catalogTracks): FusionBlend {
    val tracksA = basePool.filter { it.genre.equals(genreA, ignoreCase = true) }
    val tracksB = basePool.filter { it.genre.equals(genreB, ignoreCase = true) }
    val merged = (tracksA + tracksB).distinctBy { it.id }

    val matchScore = (82..97).random()
    val blendTitle = if (name.isNotBlank()) name else "$genreA × $genreB Fusion"

    return FusionBlend(
      id = "blend_${UUID.randomUUID().toString().take(6)}",
      title = blendTitle,
      subtitle = "Harmonized $genreA and $genreB audio blend",
      userA = "$genreA Lover",
      userB = "$genreB Vibe",
      matchScore = matchScore,
      commonGenres = listOf(genreA, genreB),
      tracks = if (merged.isNotEmpty()) merged else basePool.take(4),
      gradientStart = 0xFF1DB954,
      gradientEnd = 0xFF00E5FF
    )
  }

  // Playback History
  suspend fun recordHistory(track: Track) {
    musicDao.insertHistory(
      HistoryEntity(
        trackId = track.id,
        title = track.title,
        artist = track.artist,
        album = track.album,
        durationSec = track.durationSec,
        coverUrl = track.coverUrl,
        genre = track.genre
      )
    )
  }

  fun getRecentHistory(limit: Int = 20): Flow<List<Track>> {
    return musicDao.getRecentHistory(limit).map { historyEntities ->
      historyEntities.map { entity ->
        catalogTracks.find { it.id == entity.trackId } ?: Track(
          id = entity.trackId,
          title = entity.title,
          artist = entity.artist,
          album = entity.album,
          durationSec = entity.durationSec,
          coverUrl = entity.coverUrl,
          genre = entity.genre
        )
      }
    }
  }

  // Local search across catalog & local files
  fun search(query: String, selectedGenre: String? = null, localTracks: List<Track> = emptyList()): List<Track> {
    val allTracks = catalogTracks + localTracks
    val q = query.trim().lowercase()
    return allTracks.filter { track ->
      val matchesQuery = q.isEmpty() ||
          track.title.lowercase().contains(q) ||
          track.artist.lowercase().contains(q) ||
          track.album.lowercase().contains(q) ||
          track.genre.lowercase().contains(q)

      val matchesGenre = selectedGenre == null ||
          selectedGenre.equals("All", ignoreCase = true) ||
          track.genre.equals(selectedGenre, ignoreCase = true)

      matchesQuery && matchesGenre
    }.distinctBy { it.id }
  }

  // Local device media scanner
  suspend fun scanLocalAudioFiles(): List<Track> = withContext(Dispatchers.IO) {
    val ctx = context ?: return@withContext emptyList()
    val localList = mutableListOf<Track>()

    try {
      val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION
      )

      val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
      val cursor = ctx.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        null,
        "${MediaStore.Audio.Media.TITLE} ASC"
      )

      cursor?.use { c ->
        val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (c.moveToNext()) {
          val id = c.getLong(idCol)
          val title = c.getString(titleCol) ?: "Unknown Track"
          val artist = c.getString(artistCol) ?: "Unknown Artist"
          val album = c.getString(albumCol) ?: "Device Audio"
          val durationMs = c.getLong(durCol)
          val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString()

          localList.add(
            Track(
              id = "local_$id",
              title = title,
              artist = artist,
              album = album,
              durationSec = (durationMs / 1000).toInt().coerceAtLeast(1),
              coverUrl = "",
              genre = "Local Audio",
              audioUrl = contentUri,
              lyrics = listOf("Local device audio file", "Title: $title", "Artist: $artist")
            )
          )
        }
      }
    } catch (e: Exception) {
      Log.e("MusicRepository", "Local audio scan error: ${e.message}")
    }

    localList
  }

  // LRCLIB Synced Lyrics API Fetcher with backend lyrics support
  suspend fun fetchSyncedLyrics(track: Track): List<SyncedLyricLine> = withContext(Dispatchers.IO) {
    val cacheKey = "${track.title}_${track.artist}"
    lyricsMemoryCache[cacheKey]?.let { return@withContext it }

    // 1. Try Backend Lyrics Endpoint
    if (!track.id.startsWith("local_") && !track.id.startsWith("track_")) {
      try {
        val resp = ApiClient.service.getLyrics(track.id)
        val synced = resp.syncedLyrics
        if (!synced.isNullOrBlank()) {
          val parsed = parseLrcLines(synced)
          if (parsed.isNotEmpty()) {
            lyricsMemoryCache[cacheKey] = parsed
            return@withContext parsed
          }
        }
      } catch (e: Exception) {
        Log.d("MusicRepository", "Backend lyrics fetch failed for ${track.id}: ${e.message}")
      }
    }

    // 2. Try LRCLIB
    try {
      val urlBuilder = StringBuilder("https://lrclib.net/api/get?")
        .append("track_name=").append(java.net.URLEncoder.encode(track.title, "UTF-8"))
        .append("&artist_name=").append(java.net.URLEncoder.encode(track.artist, "UTF-8"))

      if (track.album.isNotBlank()) {
        urlBuilder.append("&album_name=").append(java.net.URLEncoder.encode(track.album, "UTF-8"))
      }
      if (track.durationSec > 0) {
        urlBuilder.append("&duration=").append(track.durationSec)
      }

      val request = Request.Builder()
        .url(urlBuilder.toString())
        .header("User-Agent", "SpotifusionAndroid/1.0")
        .build()

      val response = okHttpClient.newCall(request).execute()
      if (response.isSuccessful) {
        val bodyString = response.body?.string()
        if (!bodyString.isNullOrBlank()) {
          val json = JSONObject(bodyString)
          val syncedLyrics = json.optString("syncedLyrics", "")
          if (syncedLyrics.isNotBlank()) {
            val parsed = parseLrcLines(syncedLyrics)
            if (parsed.isNotEmpty()) {
              lyricsMemoryCache[cacheKey] = parsed
              return@withContext parsed
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.d("MusicRepository", "Lrclib fetch fallback: ${e.message}")
    }

    // Fallback: Generate timed lines from embedded lyrics if available
    val fallbackLines = if (track.lyrics.isNotEmpty()) {
      val interval = (track.durationSec.toFloat() / track.lyrics.size.coerceAtLeast(1)).coerceAtLeast(4f)
      track.lyrics.mapIndexed { idx, line ->
        SyncedLyricLine(timeSec = idx * interval, text = line)
      }
    } else emptyList()

    lyricsMemoryCache[cacheKey] = fallbackLines
    fallbackLines
  }

  private fun parseLrcLines(lrc: String): List<SyncedLyricLine> {
    val result = mutableListOf<SyncedLyricLine>()
    val pattern = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:\\.(\\d{1,3}))?\\]")

    lrc.lines().forEach { rawLine ->
      val matcher = pattern.matcher(rawLine)
      val text = rawLine.replace(Regex("\\[[^\\]]+\\]"), "").trim()
      if (text.isNotBlank()) {
        while (matcher.find()) {
          val min = matcher.group(1)?.toIntOrNull() ?: 0
          val sec = matcher.group(2)?.toIntOrNull() ?: 0
          val fractionStr = matcher.group(3) ?: "0"
          val frac = "0.$fractionStr".toFloatOrNull() ?: 0f
          val time = min * 60f + sec + frac
          result.add(SyncedLyricLine(timeSec = time, text = text))
        }
      }
    }
    return result.sortedBy { it.timeSec }
  }
}
