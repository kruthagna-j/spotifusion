package com.example.model

data class Track(
  val id: String,
  val title: String,
  val artist: String,
  val album: String,
  val durationSec: Int,
  val coverUrl: String,
  val genre: String,
  val audioUrl: String = "",
  val lyrics: List<String> = emptyList(),
  val year: String = "2024",
  val isLiked: Boolean = false,
  val plays: Int = 0
) {
  val formattedDuration: String
    get() {
      val minutes = durationSec / 60
      val seconds = durationSec % 60
      return "%d:%02d".format(minutes, seconds)
    }
}
