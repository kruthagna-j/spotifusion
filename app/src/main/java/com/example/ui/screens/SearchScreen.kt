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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val RefPurple = Color(0xFF7B51FB)

@Composable
fun SearchScreen(
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  searchResults: List<Track>,
  recent: List<String>,
  selectedGenre: String,
  genres: List<String>,
  onGenreSelected: (String) -> Unit,
  onTrackClick: (Track, List<Track>) -> Unit,
  onAddToPlaylist: (Track) -> Unit,
  onBack: () -> Unit
) {
  Column(Modifier.fillMaxSize().background(BgDark)) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(18.dp)) }
      Spacer(Modifier.width(4.dp))
      Text("Search", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
    OutlinedTextField(value = searchQuery, onValueChange = onSearchQueryChanged, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text("Search songs, artists, albums...", fontSize = 10.sp, color = TextMuted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) }, singleLine = true)
    LazyRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      items(genres) { genre ->
        val selected = selectedGenre.equals(genre, true)
        TextButton(onClick = { onGenreSelected(genre) }, modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) RefPurple else SurfaceCard).padding(horizontal = 4.dp), contentPadding = PaddingValues(start = 10.dp, top = 0.dp, end = 10.dp, bottom = 0.dp)) {
          Text(genre, color = if (selected) Color.White else TextSecondary, fontSize = 9.sp)
        }
      }
    }
    if (searchQuery.isNotBlank() || selectedGenre != "All") {
      Text("Top Results (${searchResults.size})", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
      LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(searchResults, key = { it.id }) { track ->
          Row(Modifier.fillMaxWidth().clickable { onTrackClick(track, searchResults) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(7.dp)).background(SurfaceElevated)) { if (track.coverUrl.isNotBlank()) AsyncImage(track.coverUrl, track.title, modifier = Modifier.fillMaxSize()) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
              Text(track.title, color = if (searchResults.firstOrNull { it.id == track.id }?.id == track.id) RefPurple else TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
              Text("${track.artist} • ${track.genre}", color = TextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = { onAddToPlaylist(track) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.MoreVert, "Options", tint = TextMuted, modifier = Modifier.size(16.dp)) }
          }
        }
      }
    } else {
      LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        item { Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) { Text("Recent Searches", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text("Clear all", color = RefPurple, fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
        items(recent) { query -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.History, null, tint = TextSecondary, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(10.dp)); Text(query, color = TextPrimary, fontSize = 11.sp, modifier = Modifier.weight(1f)); Icon(Icons.Default.MoreVert, "More", tint = TextMuted, modifier = Modifier.size(16.dp)) } }
        item { Spacer(Modifier.height(7.dp)); Text("Popular Searches", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 5.dp)) }
      }
    }
  }
}
