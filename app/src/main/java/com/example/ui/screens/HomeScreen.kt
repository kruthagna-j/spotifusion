package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.FusionBlend
import com.example.model.Track
import com.example.ui.theme.BgDark
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
  LazyColumn(
    modifier = Modifier.fillMaxSize().background(BgDark),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("SpotiFusion", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
          Spacer(Modifier.height(3.dp))
          Text("Music is Peace", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onNavigateToSearch) { Icon(Icons.Default.Search, "Search", tint = TextPrimary) }
          IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Settings", tint = TextSecondary) }
        }
      }
    }

    item {
      Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp)
      ) {
        Text("Find your next song", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text("Search, listen, and let the music breathe.", color = TextSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(SpotifyGreen).clickable { onNavigateToSearch }.padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text("Search Music", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
          if (tracks.isNotEmpty()) {
            Row(
              modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable { onTrackClick(tracks.first(), tracks) }.padding(horizontal = 16.dp, vertical = 11.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.PlayArrow, null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(7.dp))
              Text("Play", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
          }
        }
      }
    }

    if (recentTracks.isNotEmpty()) {
      item {
        Column {
          Text("Recently Played", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
          Spacer(Modifier.height(10.dp))
          LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recentTracks.take(8), key = { it.id }) { track ->
              Column(modifier = Modifier.width(132.dp).clickable { onTrackClick(track, recentTracks) }) {
                AsyncImage(model = track.thumbnailUrl, contentDescription = track.title, modifier = Modifier.size(132.dp).clip(RoundedCornerShape(16.dp)))
                Spacer(Modifier.height(7.dp))
                Text(track.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                Text(track.artist, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
              }
            }
          }
        }
      }
    }

    if (tracks.isNotEmpty()) {
      item {
        Column {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Your Music", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            IconButton(onClick = onNavigateToLibrary) { Icon(Icons.Default.Favorite, "Library", tint = TextSecondary) }
          }
          Spacer(Modifier.height(4.dp))
          tracks.take(8).forEach { track ->
            Row(
              modifier = Modifier.fillMaxWidth().clickable { onTrackClick(track, tracks) }.padding(vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              AsyncImage(model = track.thumbnailUrl, contentDescription = track.title, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)))
              Spacer(Modifier.width(12.dp))
              Column(modifier = Modifier.weight(1f)) {
                Text(track.title, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                Text(track.artist, color = TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp)
              }
              Icon(Icons.Default.PlayArrow, "Play", tint = if (currentPlayingTrack?.id == track.id && isPlaying) SpotifyGreen else TextSecondary)
            }
          }
        }
      }
    }
  }
}
