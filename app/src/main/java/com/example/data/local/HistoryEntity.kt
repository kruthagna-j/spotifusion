package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playback_history")
data class HistoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val trackId: String,
  val title: String,
  val artist: String,
  val album: String,
  val durationSec: Int,
  val coverUrl: String,
  val genre: String,
  val playedAt: Long = System.currentTimeMillis()
)
