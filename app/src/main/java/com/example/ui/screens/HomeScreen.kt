package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.ui.theme.BgDark
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val RefPurple = Color(0xFF7B51FB)
private val RefBlue = Color(0xFF273B72)

@Composable
fun HomeScreen(
  tracks: List<Track>,
  recentTracks: List<Track>,
  currentPlayingTrack: Track?,
  isPlaying: Boolean,
  onTrackClick: (Track, List<Track>) -> Unit,
  onNavigateToSearch: () -> Unit,
  onNavigateToLibrary: () -> Unit,
  onOpenSettings: () -> Unit = {}
) {
  val featured = listOf("Chill Vibes", "Top 50", "Trending Now")
  val featuredTracks = tracks.take(3)
  val recent = recentTracks.ifEmpty { tracks }.take(6)

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(BgDark),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 18.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Spotifusion", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
          Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
      }
    }

    item {
      Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SurfaceElevated).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(9.dp))
        Text("Search songs, artists, albums...", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.clickable(onClick = onNavigateToSearch))
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(horizontal = 1.dp)) {
        items(listOf("All", "Songs", "Albums", "Artists", "Jukebox")) { chip ->
          Box(
            Modifier.clip(RoundedCornerShape(16.dp)).background(if (chip == "All") RefPurple else SurfaceElevated).padding(horizontal = 12.dp, vertical = 7.dp)
          ) {
            Text(chip, color = if (chip == "All") Color.White else TextSecondary, fontSize = 10.sp, fontWeight = if (chip == "All") FontWeight.Bold else FontWeight.Medium)
          }
        }
      }
    }

    item {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Featured", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("See all", fontSize = 10.sp, color = RefPurple, fontWeight = FontWeight.SemiBold)
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(featured.indices.toList()) { index ->
          val title = featured[index]
          val track = featuredTracks.getOrNull(index)
          Column(Modifier.width(92.dp).clickable(enabled = track != null) { if (track != null) onTrackClick(track, tracks) }) {
            Box(Modifier.size(92.dp).clip(RoundedCornerShape(9.dp)).background(Brush.linearGradient(listOf(RefPurple, RefBlue)))) {
              if (track?.coverUrl?.isNotBlank() == true) {
                AsyncImage(track.coverUrl, track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
              }
              Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomStart).padding(7.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(title, color = TextPrimary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (index == 1) "YouTube" else "Playlist", color = TextMuted, fontSize = 8.sp)
          }
        }
      }
    }

    item {
      Text("Recently Played", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }

    items(recent, key = { it.id }) { track ->
      Row(
        Modifier.fillMaxWidth().clickable { onTrackClick(track, recent) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(Modifier.size(43.dp).clip(RoundedCornerShape(7.dp)).background(SurfaceCard)) {
          if (track.coverUrl.isNotBlank()) AsyncImage(track.coverUrl, track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
          Text(track.title, color = if (currentPlayingTrack?.id == track.id) RefPurple else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
          Text(track.artist, color = TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextMuted, modifier = Modifier.size(17.dp))
      }
    }
  }
}
