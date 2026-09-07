package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

  // --- Liked Tracks ---
  @Query("SELECT * FROM liked_tracks ORDER BY addedAt DESC")
  fun getLikedTracks(): Flow<List<LikedTrackEntity>>

  @Query("SELECT * FROM liked_tracks")
  suspend fun getLikedTracksSync(): List<LikedTrackEntity>

  @Query("SELECT EXISTS(SELECT 1 FROM liked_tracks WHERE trackId = :trackId)")
  fun isTrackLiked(trackId: String): Flow<Boolean>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertLikedTrack(track: LikedTrackEntity)

  @Query("DELETE FROM liked_tracks WHERE trackId = :trackId")
  suspend fun removeLikedTrack(trackId: String)

  // --- Playlists ---
  @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
  fun getAllPlaylists(): Flow<List<PlaylistEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPlaylist(playlist: PlaylistEntity)

  @Query("DELETE FROM playlists WHERE id = :playlistId")
  suspend fun deletePlaylist(playlistId: String)

  // --- Playlist Tracks ---
  @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY addedAt ASC")
  fun getTracksForPlaylist(playlistId: String): Flow<List<PlaylistTrackEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun addTrackToPlaylist(entry: PlaylistTrackEntity)

  @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
  suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)

  @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
  suspend fun clearTracksForPlaylist(playlistId: String)

  // --- History ---
  @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit")
  fun getRecentHistory(limit: Int = 30): Flow<List<HistoryEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertHistory(entry: HistoryEntity)

  @Query("DELETE FROM playback_history")
  suspend fun clearHistory()
}
