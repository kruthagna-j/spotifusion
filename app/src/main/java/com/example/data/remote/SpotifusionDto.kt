package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DiscoverResponse(
  @Json(name = "sections") val sections: List<DiscoverSectionDto>? = null,
  @Json(name = "cached") val cached: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class DiscoverSectionDto(
  @Json(name = "id") val id: String? = null,
  @Json(name = "label") val label: String? = null,
  @Json(name = "title") val title: String? = null,
  @Json(name = "subtitle") val subtitle: String? = null,
  @Json(name = "tracks") val tracks: List<TrackDto>? = null
)

@JsonClass(generateAdapter = true)
data class SearchResponse(
  @Json(name = "query") val query: String? = null,
  @Json(name = "category") val category: String? = null,
  @Json(name = "batch") val batch: Int? = null,
  @Json(name = "pageSize") val pageSize: Int? = null,
  @Json(name = "results") val results: List<TrackDto>? = null,
  @Json(name = "hasMore") val hasMore: Boolean? = null,
  @Json(name = "available") val available: Int? = null
)

@JsonClass(generateAdapter = true)
data class TrackDto(
  @Json(name = "id") val id: String? = null,
  @Json(name = "videoId") val videoId: String? = null,
  @Json(name = "title") val title: String? = null,
  @Json(name = "artist") val artist: String? = null,
  @Json(name = "artistId") val artistId: String? = null,
  @Json(name = "album") val album: String? = null,
  @Json(name = "albumId") val albumId: String? = null,
  @Json(name = "duration") val duration: String? = null,
  @Json(name = "durationSeconds") val durationSeconds: Int? = null,
  @Json(name = "thumbnail") val thumbnail: String? = null,
  @Json(name = "artwork") val artwork: ArtworkDto? = null,
  @Json(name = "source") val source: String? = null,
  @Json(name = "resultType") val resultType: String? = null,
  @Json(name = "genre") val genre: String? = null
)

@JsonClass(generateAdapter = true)
data class ArtworkDto(
  @Json(name = "small") val small: String? = null,
  @Json(name = "medium") val medium: String? = null,
  @Json(name = "large") val large: String? = null
)

@JsonClass(generateAdapter = true)
data class StreamResponse(
  @Json(name = "url") val url: String? = null,
  @Json(name = "expires_at") val expiresAt: Long? = null,
  @Json(name = "bitrate") val bitrate: Int? = null
)

@JsonClass(generateAdapter = true)
data class LyricsResponse(
  @Json(name = "available") val available: Boolean? = null,
  @Json(name = "lyrics") val lyrics: String? = null,
  @Json(name = "syncedLyrics") val syncedLyrics: String? = null
)

@JsonClass(generateAdapter = true)
data class ArtistDetailDto(
  @Json(name = "id") val id: String? = null,
  @Json(name = "name") val name: String? = null,
  @Json(name = "description") val description: String? = null,
  @Json(name = "thumbnail") val thumbnail: String? = null,
  @Json(name = "topSongs") val topSongs: List<TrackDto>? = null,
  @Json(name = "albums") val albums: List<TrackDto>? = null
)

@JsonClass(generateAdapter = true)
data class AlbumDetailDto(
  @Json(name = "id") val id: String? = null,
  @Json(name = "title") val title: String? = null,
  @Json(name = "artist") val artist: String? = null,
  @Json(name = "year") val year: String? = null,
  @Json(name = "thumbnail") val thumbnail: String? = null,
  @Json(name = "tracks") val tracks: List<TrackDto>? = null
)
