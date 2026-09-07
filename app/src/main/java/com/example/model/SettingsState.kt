package com.example.model

data class SettingsState(
  val audioQuality: String = "High (320kbps)",
  val crossfadeSec: Int = 3,
  val shakeToSkipEnabled: Boolean = true,
  val lyricsAutoScroll: Boolean = true,
  val visualizer60fps: Boolean = true,
  val offlineCacheSizeMb: Float = 48.5f,
  val notificationsEnabled: Boolean = true
)
