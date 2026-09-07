package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_tracks")
data class PlaylistTrackEntity(
  @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
  val playlistId: String,
  val trackId: String,
  val title: String,
  val artist: String,
  val album: String,
  val durationSec: Int,
  val coverUrl: String,
  val genre: String,
  val addedAt: Long = System.currentTimeMillis()
)
