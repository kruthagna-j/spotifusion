package com.example.model

data class FusionBlend(
  val id: String,
  val title: String,
  val subtitle: String,
  val userA: String,
  val userB: String,
  val matchScore: Int, // e.g. 88% Match
  val commonGenres: List<String>,
  val tracks: List<Track>,
  val gradientStart: Long = 0xFF1DB954,
  val gradientEnd: Long = 0xFF7C3AED
)
