package com.example.ui.screens

import androidx.compose.runtime.Composable
import com.example.model.FusionBlend
import com.example.model.Track

/** Compatibility overload for the current MainActivity contract. */
@Composable
fun HomeScreen(
  tracks: List<Track>,
  fusionBlends: List<FusionBlend>,
  recentTracks: List<Track>,
  currentPlayingTrack: Track?,
  isPlaying: Boolean,
  visualizerBars: List<Float> = emptyList(),
  onTrackClick: (Track, List<Track>) -> Unit,
  onPlayBlend: (FusionBlend) -> Unit,
  onNavigateToSearch: () -> Unit,
  onNavigateToLibrary: () -> Unit,
  onNavigateToBlends: () -> Unit,
  onNavigateToEqualizer: () -> Unit,
  onOpenSettings: () -> Unit = {},
  onOpenAddToPlaylist: (Track) -> Unit
) {
  HomeScreen(
    tracks = tracks,
    recentTracks = recentTracks,
    currentPlayingTrack = currentPlayingTrack,
    isPlaying = isPlaying,
    onTrackClick = onTrackClick,
    onNavigateToSearch = onNavigateToSearch,
    onNavigateToLibrary = onNavigateToLibrary,
    onOpenSettings = onOpenSettings
  )
}
