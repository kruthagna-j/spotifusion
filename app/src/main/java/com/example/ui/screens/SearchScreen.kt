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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import com.example.model.Track
import com.example.ui.theme.BgDark
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val RefPurple = Color(0xFF7B51FB)

@Composable
fun SearchScreen(
  searchQuery: String,
  selectedGenre: String,
  searchResults: List<Track>,
  currentPlayingTrack: Track?,
  isPlaying: Boolean,
  onSearchQueryChanged: (String) -> Unit,
  onGenreSelected: (String) -> Unit,
  onTrackClick: (Track, List<Track>) -> Unit,
  onToggleLike: (Track) -> Unit,
  isTrackLiked: (String) -> Boolean,
  onAddToPlaylist: (Track) -> Unit
) {
  val categories = listOf("All", "Songs", "Albums", "Artists", "Jukebox")
  val recent = listOf("arijit singh", "lofi", "the weeknd", "taylor swift")
  val popular = listOf("billie eilish", "michael jackson", "arijit singh hits", "lofi hip hop")

  Column(Modifier.fillMaxSize().background(BgDark)) {
    Row(Modifier.fillMaxWidth().padding(start = 10.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = {}) { Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(19.dp)) }
      Text("Search", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
      Icon(Icons.Default.MoreVert, "Menu", tint = TextSecondary, modifier = Modifier.size(20.dp))
    }

    TextField(
      value = searchQuery,
      onValueChange = onSearchQueryChanged,
      placeholder = { Text("Search songs, artists, albums...", color = TextMuted, fontSize = 10.sp) },
      leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(16.dp)) },
      singleLine = true,
      colors = TextFieldDefaults.colors(
        focusedContainerColor = SurfaceElevated, unfocusedContainerColor = SurfaceElevated,
        disabledContainerColor = SurfaceElevated, focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
        cursorColor = RefPurple, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary
      ),
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(46.dp)
    )

    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
      items(categories) { category ->
        Box(Modifier.clip(RoundedCornerShape(14.dp)).background(if (category == "All") RefPurple else SurfaceElevated).padding(horizontal = 11.dp, vertical = 6.dp)) {
          Text(category, color = if (category == "All") Color.White else TextSecondary, fontSize = 9.sp, fontWeight = if (category == "All") FontWeight.Bold else FontWeight.Medium)
        }
      }
    }

    if (searchQuery.isNotBlank() || selectedGenre != "All") {
      Text("Top Results (${searchResults.size})", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
      LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(searchResults, key = { it.id }) { track ->
          Row(Modifier.fillMaxWidth().clickable { onTrackClick(track, searchResults) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(7.dp)).background(SurfaceElevated)) {
              if (track.coverUrl.isNotBlank()) AsyncImage(track.coverUrl, track.title, modifier = Modifier.fillMaxSize())
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
              Text(track.title, color = if (currentPlayingTrack?.id == track.id) RefPurple else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text("${track.artist} • ${track.genre}", color = TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { onAddToPlaylist(track) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = TextMuted, modifier = Modifier.size(16.dp)) }
          }
        }
      }
    } else {
      LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        item {
          Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Recent Searches", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Clear all", color = RefPurple, fontSize = 8.sp, fontWeight = FontWeight.Bold)
          }
        }
        items(recent) { query ->
          Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
            Text(query, color = TextPrimary, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.MoreVert, "More", tint = TextMuted, modifier = Modifier.size(16.dp))
          }
        }
        item {
          Spacer(Modifier.height(7.dp))
          Text("Popular Searches", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 5.dp))
        }
        items(popular) { query ->
          Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(10.dp))
            Text(query, color = TextPrimary, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ArrowForward, "Search", tint = TextMuted, modifier = Modifier.size(14.dp))
          }
        }
      }
    }
  }
}
