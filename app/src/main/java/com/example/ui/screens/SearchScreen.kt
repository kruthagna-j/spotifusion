package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.ui.theme.BgDark
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

data class BrowseCategory(
  val name: String,
  val gradientStart: Long,
  val gradientEnd: Long,
  val genreQuery: String
)

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
  val genres = listOf("All", "Pop", "Synthwave", "Lo-Fi", "EDM", "Hip-Hop", "Indie Rock", "R&B", "Acoustic")

  val browseCategories = listOf(
    BrowseCategory("Pop Hits", 0xFFE91E63, 0xFFFF5722, "Pop"),
    BrowseCategory("Synthwave", 0xFF9C27B0, 0xFF3F51B5, "Synthwave"),
    BrowseCategory("Lo-Fi Study", 0xFF009688, 0xFF4CAF50, "Lo-Fi"),
    BrowseCategory("EDM Club", 0xFF00BCD4, 0xFF2196F3, "EDM"),
    BrowseCategory("Hip-Hop Flow", 0xFFFF9800, 0xFFF44336, "Hip-Hop"),
    BrowseCategory("Indie Vibe", 0xFF3F51B5, 0xFF009688, "Indie Rock"),
    BrowseCategory("R&B Velvet", 0xFF673AB7, 0xFFE91E63, "R&B"),
    BrowseCategory("Acoustic Calm", 0xFF795548, 0xFFFFB74D, "Acoustic")
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BgDark)
      .testTag("search_screen")
  ) {
    // Top Title
    Text(
      text = "Search",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Black,
      color = TextPrimary,
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
    )

    // Search Box
    OutlinedTextField(
      value = searchQuery,
      onValueChange = onSearchQueryChanged,
      placeholder = { Text("What do you want to listen to?", color = TextMuted) },
      leadingIcon = {
        Icon(Icons.Default.Search, contentDescription = "Search", tint = SpotifyGreen)
      },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { onSearchQueryChanged("") }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(16.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = SurfaceCard,
        unfocusedContainerColor = SurfaceCard,
        focusedBorderColor = SpotifyGreen,
        unfocusedBorderColor = GlassBorder,
        focusedTextColor = TextPrimary,
        unfocusedTextColor = TextPrimary
      ),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .testTag("search_input_field")
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Genre Filter Pills
    LazyRow(
      contentPadding = PaddingValues(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(genres) { genre ->
        val isSelected = selectedGenre.equals(genre, ignoreCase = true)
        FilterChip(
          selected = isSelected,
          onClick = { onGenreSelected(genre) },
          label = {
            Text(
              text = genre,
              color = if (isSelected) BgDark else TextPrimary,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
              fontSize = 12.sp
            )
          },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = SpotifyGreen,
            containerColor = SurfaceElevated
          ),
          border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = GlassBorder,
            selectedBorderColor = SpotifyGreen
          ),
          shape = RoundedCornerShape(20.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Content: Search Results or Category Tiles
    if (searchQuery.isNotBlank() || selectedGenre != "All") {
      // Results list
      Text(
        text = "Top Results (${searchResults.size})",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
      )

      if (searchResults.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("No tracks found", color = TextSecondary, fontWeight = FontWeight.Bold)
            Text("Try searching for artists, songs, or genres", color = TextMuted, fontSize = 12.sp)
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().weight(1f),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
        ) {
          items(searchResults, key = { it.id }) { track ->
            val isCurrent = currentPlayingTrack?.id == track.id
            val isLiked = isTrackLiked(track.id)

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onTrackClick(track, searchResults) }
                .padding(vertical = 8.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated)
                ) {
                  if (track.coverUrl.isNotBlank()) {
                    AsyncImage(
                      model = track.coverUrl,
                      contentDescription = track.title,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize()
                    )
                  } else {
                    Icon(
                      Icons.Default.MusicNote,
                      contentDescription = null,
                      tint = SpotifyGreen,
                      modifier = Modifier.size(24.dp).align(Alignment.Center)
                    )
                  }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                  Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) SpotifyGreen else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${track.artist} • ${track.genre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onToggleLike(track) }) {
                  Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike" else "Like",
                    tint = if (isLiked) SpotifyGreen else TextMuted,
                    modifier = Modifier.size(20.dp)
                  )
                }

                IconButton(onClick = { onAddToPlaylist(track) }) {
                  Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextMuted)
                }
              }
            }
          }
        }
      }
    } else {
      // Browse All Categories
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = 16.dp)
      ) {
        Text(
          text = "Browse all",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyVerticalGrid(
          columns = GridCells.Fixed(2),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          items(browseCategories) { cat ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                  Brush.linearGradient(
                    listOf(Color(cat.gradientStart), Color(cat.gradientEnd))
                  )
                )
                .clickable {
                  onGenreSelected(cat.genreQuery)
                }
                .padding(14.dp)
            ) {
              Text(
                text = cat.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopStart)
              )

              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(Color.Black.copy(alpha = 0.25f))
                  .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.PlayArrow,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      }
    }
  }
}
