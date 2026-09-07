package com.example.model

data class Playlist(
  val id: String,
  val title: String,
  val description: String,
  val coverGradientStart: Long = 0xFF1DB954,
  val coverGradientEnd: Long = 0xFF0D47A1,
  val isCustom: Boolean = false,
  val trackCount: Int = 0,
  val tracks: List<Track> = emptyList()
)
