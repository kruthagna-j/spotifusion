package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpotifusionApi {

  @GET("api/discover")
  suspend fun getDiscover(): DiscoverResponse

  @GET("api/search")
  suspend fun search(
    @Query("q") query: String,
    @Query("category") category: String = "all",
    @Query("batch") batch: Int = 1
  ): SearchResponse

  @GET("api/song/{videoId}")
  suspend fun getSong(
    @Path("videoId") videoId: String
  ): TrackDto

  @GET("api/stream/{videoId}")
  suspend fun getStream(
    @Path("videoId") videoId: String,
    @Query("quality") quality: String = "High (320kbps)"
  ): StreamResponse

  @GET("api/lyrics/{videoId}")
  suspend fun getLyrics(
    @Path("videoId") videoId: String
  ): LyricsResponse

  @GET("api/artist/{artistId}")
  suspend fun getArtist(
    @Path("artistId") artistId: String
  ): ArtistDetailDto

  @GET("api/album/{albumId}")
  suspend fun getAlbum(
    @Path("albumId") albumId: String
  ): AlbumDetailDto
}
