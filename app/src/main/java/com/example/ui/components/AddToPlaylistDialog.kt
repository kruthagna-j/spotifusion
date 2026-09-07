package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Playlist
import com.example.model.Track
import com.example.ui.theme.BgDark
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AddToPlaylistDialog(
  track: Track?,
  playlists: List<Playlist>,
  onDismiss: () -> Unit,
  onAddToPlaylist: (playlistId: String, track: Track) -> Unit,
  onCreatePlaylist: (name: String, description: String, track: Track) -> Unit
) {
  if (track == null) return

  var showNewPlaylistInput by remember { mutableStateOf(false) }
  var newPlaylistTitle by remember { mutableStateOf("") }
  var newPlaylistDesc by remember { mutableStateOf("") }
  var addedPlaylistIds by remember { mutableStateOf(setOf<String>()) }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = SurfaceCard,
      tonalElevation = 8.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)
        .testTag("add_to_playlist_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .fillMaxWidth()
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Add to Playlist",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "${track.title} • ${track.artist}",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showNewPlaylistInput) {
          OutlinedTextField(
            value = newPlaylistTitle,
            onValueChange = { newPlaylistTitle = it },
            label = { Text("Playlist Name") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SpotifyGreen,
              unfocusedBorderColor = GlassBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("new_playlist_name_input")
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = newPlaylistDesc,
            onValueChange = { newPlaylistDesc = it },
            label = { Text("Description (Optional)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = SpotifyGreen,
              unfocusedBorderColor = GlassBorder,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(16.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(onClick = { showNewPlaylistInput = false }) {
              Text("Cancel", color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = {
                if (newPlaylistTitle.isNotBlank()) {
                  onCreatePlaylist(newPlaylistTitle.trim(), newPlaylistDesc.trim(), track)
                  onDismiss()
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
              enabled = newPlaylistTitle.isNotBlank(),
              modifier = Modifier.testTag("confirm_create_playlist_button")
            ) {
              Text("Create & Add", color = Color.White, fontWeight = FontWeight.Bold)
            }
          }
        } else {
          // Button to create new playlist
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(SurfaceElevated)
              .clickable { showNewPlaylistInput = true }
              .padding(14.dp)
              .testTag("create_new_playlist_row"),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SpotifyGreen.copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.Add, contentDescription = null, tint = SpotifyGreen)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
              text = "New Playlist",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = TextPrimary
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Existing Playlists
          Text(
            text = "Your Playlists",
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(8.dp))

          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height((playlists.size * 56).coerceAtMost(220).dp)
          ) {
            items(playlists, key = { it.id }) { playlist ->
              val isAdded = addedPlaylistIds.contains(playlist.id)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    if (!isAdded) {
                      onAddToPlaylist(playlist.id, track)
                      addedPlaylistIds = addedPlaylistIds + playlist.id
                    }
                  }
                  .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.weight(1f)
                ) {
                  Box(
                    modifier = Modifier
                      .size(40.dp)
                      .clip(RoundedCornerShape(6.dp))
                      .background(
                        Brush.linearGradient(
                          listOf(Color(playlist.coverGradientStart), Color(playlist.coverGradientEnd))
                        )
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    Icon(
                      Icons.Default.QueueMusic,
                      contentDescription = null,
                      tint = Color.White.copy(alpha = 0.8f),
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(12.dp))
                  Column {
                    Text(
                      text = playlist.title,
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = TextPrimary,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis
                    )
                    Text(
                      text = if (playlist.isCustom) "Custom Playlist" else "Curated Playlist",
                      style = MaterialTheme.typography.bodySmall,
                      color = TextSecondary,
                      fontSize = 11.sp
                    )
                  }
                }

                if (isAdded) {
                  Icon(
                    Icons.Default.Check,
                    contentDescription = "Added",
                    tint = SpotifyGreen,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
