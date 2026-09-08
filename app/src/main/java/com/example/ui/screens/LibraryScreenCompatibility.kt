package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.model.Playlist
import com.example.model.Track

/** Compatibility overload for the current AI Studio MainActivity signature. */
@Composable
fun LibraryScreen(
  playlists: List<Playlist>,
  likedTracks: List<Track>,
  recentHistory: List<Track>,
  localTracks: List<Track>,
  downloadedTracks: List<Track>,
  selectedPlaylist: Playlist?,
  selectedPlaylistTracks: List<Track>,
  currentPlayingTrack: Track?,
  isPlaying: Boolean,
  onSelectPlaylist: (Playlist?) -> Unit,
  onTrackClick: (Track, List<Track>) -> Unit,
  onPlayAll: (List<Track>) -> Unit,
  onShufflePlay: (List<Track>) -> Unit,
  onToggleLike: (Track) -> Unit,
  onCreatePlaylistDialog: () -> Unit,
  onDeletePlaylist: (String) -> Unit,
  onScanLocalTracks: () -> Unit,
  onRemoveTrackFromPlaylist: (String, String) -> Unit
) {
  LibraryScreen(
    playlists = playlists,
    likedTracks = likedTracks,
    recentHistory = recentHistory,
    localTracks = localTracks,
    selectedPlaylist = selectedPlaylist,
    selectedPlaylistTracks = selectedPlaylistTracks,
    currentPlayingTrack = currentPlayingTrack,
    isPlaying = isPlaying,
    onSelectPlaylist = onSelectPlaylist,
    onTrackClick = onTrackClick,
    onPlayAll = onPlayAll,
    onShufflePlay = onShufflePlay,
    onToggleLike = onToggleLike,
    onCreatePlaylistDialog = onCreatePlaylistDialog,
    onDeletePlaylist = onDeletePlaylist,
    onScanLocalTracks = onScanLocalTracks,
    onRemoveTrackFromPlaylist = onRemoveTrackFromPlaylist
  )
}
