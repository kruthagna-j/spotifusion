package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.FusionBlend
import com.example.model.Track
import com.example.ui.theme.BgDark
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyGreenLight
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FusionBlendsScreen(
  fusionBlends: List<FusionBlend>,
  onPlayBlend: (FusionBlend) -> Unit,
  onTrackClick: (Track, List<Track>) -> Unit,
  onGenerateBlend: (genreA: String, genreB: String, name: String) -> Unit,
  onSaveBlendAsPlaylist: (name: String, description: String, tracks: List<Track>) -> Unit
) {
  var showCreator by remember { mutableStateOf(false) }
  var genreA by remember { mutableStateOf("Synthwave") }
  var genreB by remember { mutableStateOf("Lo-Fi") }
  var blendName by remember { mutableStateOf("") }

  val genreOptions = listOf("Synthwave", "Lo-Fi", "Pop", "EDM", "Hip-Hop", "Indie Rock", "R&B", "Acoustic")

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(BgDark)
      .testTag("fusion_blends_screen"),
    contentPadding = PaddingValues(bottom = 16.dp)
  ) {
    // Header
    item {
      Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Fusion Blends",
              style = MaterialTheme.typography.headlineMedium,
              fontWeight = FontWeight.Black,
              color = TextPrimary
            )
            Text(
              text = "Merge genres, moods, and tastes together",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              fontSize = 12.sp
            )
          }

          Button(
            onClick = { showCreator = !showCreator },
            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.testTag("create_blend_toggle_button")
          ) {
            Icon(
              imageVector = if (showCreator) Icons.Default.Close else Icons.Default.Add,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (showCreator) "Close" else "New Blend", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }

    // Blend Creator Studio
    item {
      AnimatedVisibility(visible = showCreator) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceCard)
            .border(1.dp, SpotifyGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(18.dp)
            .testTag("blend_creator_studio")
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Create Taste Fusion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Primary Taste (Genre A)", style = MaterialTheme.typography.labelMedium, color = BrandCyan, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(genreOptions) { opt ->
                val isSelected = genreA == opt
                FilterChip(
                  selected = isSelected,
                  onClick = { genreA = opt },
                  label = { Text(opt, fontSize = 11.sp, color = if (isSelected) BgDark else TextPrimary) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandCyan, containerColor = SurfaceElevated),
                  border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = GlassBorder, selectedBorderColor = BrandCyan)
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Secondary Taste (Genre B)", style = MaterialTheme.typography.labelMedium, color = BrandPurple, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(genreOptions) { opt ->
                val isSelected = genreB == opt
                FilterChip(
                  selected = isSelected,
                  onClick = { genreB = opt },
                  label = { Text(opt, fontSize = 11.sp, color = if (isSelected) BgDark else TextPrimary) },
                  colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandPurple, containerColor = SurfaceElevated),
                  border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = GlassBorder, selectedBorderColor = BrandPurple)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
              value = blendName,
              onValueChange = { blendName = it },
              placeholder = { Text("Custom Blend Name (Optional)", color = TextMuted, fontSize = 12.sp) },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SpotifyGreen,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
              ),
              modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Simulated Harmony Match
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text("Taste Compatibility", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
              Text("92% Harmonious", style = MaterialTheme.typography.bodySmall, color = SpotifyGreen, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
              progress = { 0.92f },
              modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
              color = SpotifyGreen,
              trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
              onClick = {
                onGenerateBlend(genreA, genreB, blendName.trim())
                showCreator = false
                blendName = ""
              },
              colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth().testTag("generate_fusion_blend_button")
            ) {
              Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Generate SpotiFusion Blend", color = Color.White, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Blends List
    items(fusionBlends, key = { it.id }) { blend ->
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .shadow(12.dp, RoundedCornerShape(20.dp))
          .clip(RoundedCornerShape(20.dp))
          .background(SurfaceCard)
          .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
      ) {
        Column {
          // Gradient Top Header
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                Brush.horizontalGradient(
                  listOf(Color(blend.gradientStart), Color(blend.gradientEnd))
                )
              )
              .padding(18.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.35f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                  Text(
                    text = "${blend.matchScore}% Match • ${blend.tracks.size} Tracks",
                    color = SpotifyGreenLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                  )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                  text = blend.title,
                  style = MaterialTheme.typography.titleLarge,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
                Text(
                  text = blend.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.85f),
                  fontSize = 12.sp
                )
              }

              // Play Blend Button
              Box(
                modifier = Modifier
                  .size(52.dp)
                  .clip(CircleShape)
                  .background(Color.White)
                  .shadow(8.dp, CircleShape)
                  .clickable { onPlayBlend(blend) }
                  .testTag("play_blend_${blend.id}"),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.PlayArrow,
                  contentDescription = "Play Blend",
                  tint = SpotifyGreen,
                  modifier = Modifier.size(32.dp)
                )
              }
            }
          }

          // Blend Tracks Preview
          Column(modifier = Modifier.padding(14.dp)) {
            blend.tracks.take(3).forEach { track ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable { onTrackClick(track, blend.tracks) }
                  .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
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
                      modifier = Modifier.size(20.dp).align(Alignment.Center)
                    )
                  }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = track.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                  )
                  Text(
                    text = "${track.artist} • ${track.genre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                  )
                }

                Text(
                  text = track.formattedDuration,
                  style = MaterialTheme.typography.labelSmall,
                  color = TextMuted
                )
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Save to Library action
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              Button(
                onClick = {
                  onSaveBlendAsPlaylist(blend.title, blend.subtitle, blend.tracks)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.BookmarkBorder, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save to Library", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
