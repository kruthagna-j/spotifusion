package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
  @PrimaryKey val id: String,
  val title: String,
  val description: String,
  val coverGradientStart: Long = 0xFF1DB954,
  val coverGradientEnd: Long = 0xFF0D47A1,
  val createdAt: Long = System.currentTimeMillis()
)
