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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val Purple = Color(0xFF7B51FB)
private val DeepPurple = Color(0xFF26134F)
private val Chip = Color(0xFF121A28)

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
  val recent = (recentTracks.ifEmpty { tracks }).take(6)
  val featured = tracks.take(3)
  val fallbackTitles = listOf("Chill Vibes", "Top 50", "Trending Now")

  LazyColumn(
    modifier = Modifier.fillMaxSize().background(BgDark),
    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 18.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text("Spotifusion", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onOpenSettings, modifier = Modifier.size(34.dp)) {
          Icon(Icons.Default.Settings, "Settings", tint = TextSecondary, modifier = Modifier.size(19.dp))
        }
      }
    }

    item {
      Column {
        Text("Good morning,", color = TextSecondary, fontSize = 12.sp)
        Text("Kruthagna", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
      }
    }

    item {
      Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(SurfaceElevated).clickable(onClick = onNavigateToSearch).padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(8.dp))
        Text("Search songs, artists, albums...", color = TextSecondary, fontSize = 11.sp)
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(horizontal = 1.dp)) {
        items(listOf("All", "Songs", "Albums", "Artists", "Jukebox").size) { index ->
          val label = listOf("All", "Songs", "Albums", "Artists", "Jukebox")[index]
          Box(Modifier.clip(RoundedCornerShape(16.dp)).background(if (index == 0) Purple else Chip).padding(horizontal = 11.dp, vertical = 6.dp)) {
            Text(label, color = if (index == 0) Color.White else TextSecondary, fontSize = 9.sp, fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Medium)
          }
        }
      }
    }

    item {
      Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text("Featured", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("See all", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
      }
    }

    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        items(3) { index ->
          val track = featured.getOrNull(index)
          val title = track?.title?.takeIf { it.isNotBlank() } ?: fallbackTitles[index]
          Column(Modifier.width(91.dp).clickable(enabled = track != null) { track?.let { onTrackClick(it, tracks) } }) {
            Box(Modifier.size(91.dp).clip(RoundedCornerShape(8.dp)).background(Brush.linearGradient(listOf(Purple, DeepPurple))), Alignment.BottomStart) {
              if (track?.coverUrl?.isNotBlank() == true) AsyncImage(track.coverUrl, title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
              Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(7.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(title, color = TextPrimary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (index == 1) "YouTube" else "Playlist", color = TextMuted, fontSize = 7.sp)
          }
        }
      }
    }

    item { Text("Recently Played", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold) }

    if (recent.isEmpty()) {
      item {
        Box(Modifier.fillMaxWidth().height(80.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceCard), Alignment.Center) {
          Text("No recently played songs", color = TextMuted, fontSize = 11.sp)
        }
      }
    } else {
      itemsIndexed(recent, key = { _, t -> t.id }) { _, track ->
        Row(Modifier.fillMaxWidth().clickable { onTrackClick(track, recent) }.padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
          Box(Modifier.size(42.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceCard)) {
            if (track.coverUrl.isNotBlank()) AsyncImage(track.coverUrl, track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
          }
          Spacer(Modifier.width(10.dp))
          Column(Modifier.weight(1f)) {
            Text(track.title, color = if (currentPlayingTrack?.id == track.id) Purple else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = TextMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          Icon(Icons.Default.MoreVert, "More", tint = TextMuted, modifier = Modifier.size(17.dp))
        }
      }
    }

    item {
      Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SurfaceCard).padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column {
          Text("Daily Mix", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          Text("Your personalized mix", color = TextMuted, fontSize = 8.sp)
        }
        Text("Explore  ›", color = Purple, fontSize = 9.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}
