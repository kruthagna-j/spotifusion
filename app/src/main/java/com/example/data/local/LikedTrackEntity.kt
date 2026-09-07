package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "liked_tracks")
data class LikedTrackEntity(
  @PrimaryKey val trackId: String,
  val title: String,
  val artist: String,
  val album: String,
  val durationSec: Int,
  val coverUrl: String,
  val genre: String,
  val addedAt: Long = System.currentTimeMillis()
)
