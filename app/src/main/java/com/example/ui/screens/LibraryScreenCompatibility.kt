package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.Playlist
import com.example.model.Track

@Composable
fun LibraryScreen(
  playlists: List<Playlist>, likedTracks: List<Track>, recentHistory: List<Track>,
  localTracks: List<Track>, downloadedTracks: List<Track>, selectedPlaylist: Playlist?,
  selectedPlaylistTracks: List<Track>, currentPlayingTrack: Track?, isPlaying: Boolean,
  onSelectPlaylist: (Playlist?) -> Unit, onTrackClick: (Track, List<Track>) -> Unit,
  onPlayAll: (List<Track>) -> Unit, onShufflePlay: (List<Track>) -> Unit,
  onToggleLike: (Track) -> Unit, onCreatePlaylistDialog: () -> Unit,
  onDeletePlaylist: (String) -> Unit, onScanLocalTracks: () -> Unit,
  onRemoveTrackFromPlaylist: (String, String) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    com.example.ui.screens.LibraryScreen(
      playlists = playlists, likedTracks = likedTracks, recentHistory = recentHistory,
      localTracks = localTracks, selectedPlaylist = selectedPlaylist,
      selectedPlaylistTracks = selectedPlaylistTracks, currentPlayingTrack = currentPlayingTrack,
      isPlaying = isPlaying, onSelectPlaylist = onSelectPlaylist, onTrackClick = onTrackClick,
      onPlayAll = onPlayAll, onShufflePlay = onShufflePlay, onToggleLike = onToggleLike,
      onCreatePlaylistDialog = onCreatePlaylistDialog, onDeletePlaylist = onDeletePlaylist,
      onScanLocalTracks = onScanLocalTracks, onRemoveTrackFromPlaylist = onRemoveTrackFromPlaylist
    )
    if (downloadedTracks.isNotEmpty()) {
      Text("Offline Downloads", style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
      LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(downloadedTracks, key = { it.id }) { track ->
          ListItem(
            headlineContent = { Text(track.title) },
            supportingContent = { Text(track.artist) },
            leadingContent = { Icon(Icons.Default.DownloadDone, contentDescription = "Downloaded") },
            modifier = Modifier.padding(horizontal = 12.dp)
          )
        }
      }
    }
  }
}
