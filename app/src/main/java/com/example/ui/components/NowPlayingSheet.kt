package com.example.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.SyncedLyricLine
import com.example.model.Track
import com.example.model.RepeatMode
import com.example.ui.theme.BgDark
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.ImmersiveOnPrimary
import com.example.ui.theme.ImmersiveOnSecondaryContainer
import com.example.ui.theme.ImmersiveOutlineVariant
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersivePrimaryContainer
import com.example.ui.theme.ImmersiveSecondaryContainer
import com.example.ui.theme.ImmersiveSurfaceCard
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NowPlayingSheet(
  track: Track?,
  isPlaying: Boolean,
  currentPositionSec: Int,
  durationSec: Int,
  isLiked: Boolean,
  isShuffled: Boolean,
  repeatMode: RepeatMode,
  volume: Float,
  visualizerBars: List<Float>,
  syncedLyrics: List<SyncedLyricLine> = emptyList(),
  sleepTimerMinutes: Int? = null,
  onClose: () -> Unit,
  onTogglePlayPause: () -> Unit,
  onNext: () -> Unit,
  onPrevious: () -> Unit,
  onSeek: (Int) -> Unit,
  onToggleShuffle: () -> Unit,
  onCycleRepeat: () -> Unit,
  onToggleLike: (Track) -> Unit,
  onSetVolume: (Float) -> Unit,
  onOpenEqualizer: () -> Unit,
  onOpenSleepTimer: () -> Unit = {},
  onAddToPlaylist: (Track) -> Unit
) {
  if (track == null) return

  var activeViewTab by remember { mutableIntStateOf(0) } // 0: Artwork, 1: 3D Visualizer, 2: Lyrics
  var visualizer3DMode by remember { mutableStateOf(Visualizer3DMode.BARS_3D) }
  var isUserSeeking by remember { mutableStateOf(false) }
  var seekSliderValue by remember { mutableFloatStateOf(currentPositionSec.toFloat()) }

  val lyricsListState = rememberLazyListState()

  // Calculate active synced lyric line
  val activeLyricIndex = remember(currentPositionSec, syncedLyrics) {
    if (syncedLyrics.isNotEmpty()) {
      val idx = syncedLyrics.indexOfLast { it.timeSec <= currentPositionSec }
      if (idx >= 0) idx else 0
    } else {
      if (track.lyrics.isNotEmpty()) {
        val interval = (track.durationSec.toFloat() / track.lyrics.size.coerceAtLeast(1)).coerceAtLeast(4f)
        (currentPositionSec / interval).toInt().coerceIn(0, track.lyrics.lastIndex)
      } else 0
    }
  }

  // Smooth auto-scroll lyrics
  LaunchedEffect(activeLyricIndex, activeViewTab) {
    if (activeViewTab == 2) {
      lyricsListState.animateScrollToItem((activeLyricIndex - 2).coerceAtLeast(0))
    }
  }

  // Animated vinyl rotation
  val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
  val spinAngle by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(12000, easing = LinearEasing),
      repeatMode = AnimRepeatMode.Restart
    ),
    label = "spin_angle"
  )

  Dialog(
    onDismissRequest = onClose,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .testTag("now_playing_screen"),
      color = BgDark
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(
            Brush.verticalGradient(
              listOf(
                ImmersivePrimaryContainer.copy(alpha = 0.45f),
                ImmersiveSurfaceCard,
                ImmersiveBg
              )
            )
          )
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          // Top Bar
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = onClose,
              modifier = Modifier.testTag("collapse_now_playing_button")
            ) {
              Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = "Collapse Player",
                tint = TextPrimary,
                modifier = Modifier.size(32.dp)
              )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "NOW PLAYING",
                style = MaterialTheme.typography.labelSmall,
                color = ImmersivePrimary,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                fontSize = 11.sp
              )
              Text(
                text = track.album.ifBlank { "SpotiFusion Mix" },
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              // Sleep timer button
              IconButton(onClick = onOpenSleepTimer) {
                if (sleepTimerMinutes != null) {
                  BadgedBox(
                    badge = {
                      Badge(containerColor = SpotifyGreen) {
                        Text("${sleepTimerMinutes}m", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                      }
                    }
                  ) {
                    Icon(
                      Icons.Default.Timer,
                      contentDescription = "Sleep Timer",
                      tint = SpotifyGreen,
                      modifier = Modifier.size(22.dp)
                    )
                  }
                } else {
                  Icon(
                    Icons.Default.Timer,
                    contentDescription = "Sleep Timer",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                  )
                }
              }

              IconButton(onClick = onOpenEqualizer) {
                Icon(
                  Icons.Default.Equalizer,
                  contentDescription = "Equalizer",
                  tint = TextPrimary,
                  modifier = Modifier.size(24.dp)
                )
              }
            }
          }

          // View Mode Selector Tabs (Cover / 3D Visualizer / Lyrics)
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(ImmersiveSurfaceCard)
              .border(1.dp, ImmersiveOutlineVariant, RoundedCornerShape(16.dp))
              .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            val tabs = listOf("Cover", "3D Visualizer", "Lyrics")
            tabs.forEachIndexed { index, label ->
              val isSelected = activeViewTab == index
              Surface(
                onClick = { activeViewTab = index },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) ImmersiveSecondaryContainer else Color.Transparent,
                modifier = Modifier.testTag("now_playing_tab_$index")
              ) {
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  fontSize = 12.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) ImmersiveOnSecondaryContainer else TextMuted,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Center Area (Artwork, 3D Canvas Visualizer, or Synced Lyrics)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f, fill = false)
              .height(280.dp),
            contentAlignment = Alignment.Center
          ) {
            Crossfade(targetState = activeViewTab, label = "view_mode_crossfade") { tab ->
              when (tab) {
                0 -> {
                  // Premium 3D Vinyl / Artwork Card
                  Box(
                    modifier = Modifier
                      .size(250.dp)
                      .shadow(
                        elevation = 28.dp,
                        shape = RoundedCornerShape(32.dp),
                        spotColor = ImmersivePrimaryContainer,
                        ambientColor = Color.Black
                      )
                      .clip(RoundedCornerShape(32.dp))
                      .background(SurfaceElevated)
                      .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                          listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.05f),
                            Color.Black.copy(alpha = 0.4f)
                          )
                        ),
                        shape = RoundedCornerShape(32.dp)
                      )
                  ) {
                    if (track.coverUrl.isNotBlank()) {
                      AsyncImage(
                        model = track.coverUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                      )
                    } else {
                      // Sleek Vinyl Center Graphic
                      Box(
                        modifier = Modifier
                          .fillMaxSize()
                          .background(Color(0xFF121212)),
                        contentAlignment = Alignment.Center
                      ) {
                        Box(
                          modifier = Modifier
                            .size(190.dp)
                            .rotate(if (isPlaying) spinAngle else 0f)
                            .clip(CircleShape)
                            .background(
                              Brush.radialGradient(
                                listOf(
                                  Color(0xFF2C2C2C),
                                  Color(0xFF141414),
                                  Color(0xFF080808)
                                )
                              )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.1f), CircleShape),
                          contentAlignment = Alignment.Center
                        ) {
                          Box(
                            modifier = Modifier
                              .size(60.dp)
                              .clip(CircleShape)
                              .background(ImmersivePrimaryContainer)
                              .border(1.dp, ImmersivePrimary, CircleShape),
                            contentAlignment = Alignment.Center
                          ) {
                            Icon(
                              Icons.Default.MusicNote,
                              contentDescription = null,
                              tint = ImmersivePrimary,
                              modifier = Modifier.size(28.dp)
                            )
                          }
                        }
                      }
                    }

                    // 3D Specular Light Overlay
                    Box(
                      modifier = Modifier
                        .fillMaxSize()
                        .background(
                          Brush.linearGradient(
                            listOf(
                              Color.White.copy(alpha = 0.15f),
                              Color.Transparent,
                              Color.Black.copy(alpha = 0.35f)
                            )
                          )
                        )
                    )
                  }
                }
                1 -> {
                  // Interactive 3D Canvas-Based Audio Visualizer
                  Canvas3DAudioVisualizer(
                    frequencies = visualizerBars,
                    isPlaying = isPlaying,
                    height = 240.dp,
                    currentMode = visualizer3DMode,
                    onModeChange = { visualizer3DMode = it },
                    showModeSelector = true,
                    modifier = Modifier.fillMaxWidth()
                  )
                }
                2 -> {
                  // Synced Lyrics View with Timestamp Highlighting & Smooth Auto-Scroll
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .clip(RoundedCornerShape(24.dp))
                      .background(ImmersiveSurfaceCard)
                      .border(1.dp, ImmersiveOutlineVariant, RoundedCornerShape(24.dp))
                      .padding(18.dp)
                  ) {
                    Column {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Text(
                          text = "Synced Lyrics",
                          style = MaterialTheme.typography.titleMedium,
                          fontWeight = FontWeight.Bold,
                          color = ImmersivePrimary
                        )
                        Text(
                          text = if (syncedLyrics.isNotEmpty()) "LRCLIB Live Sync" else "Embedded",
                          style = MaterialTheme.typography.labelSmall,
                          color = SpotifyGreen,
                          fontWeight = FontWeight.SemiBold
                        )
                      }

                      Spacer(modifier = Modifier.height(10.dp))

                      if (syncedLyrics.isNotEmpty()) {
                        LazyColumn(
                          state = lyricsListState,
                          modifier = Modifier.fillMaxSize()
                        ) {
                          itemsIndexed(syncedLyrics) { index, line ->
                            val isHighlighted = index == activeLyricIndex
                            Text(
                              text = line.text,
                              style = MaterialTheme.typography.bodyLarge,
                              fontWeight = if (isHighlighted) FontWeight.Black else FontWeight.Medium,
                              fontSize = if (isHighlighted) 20.sp else 15.sp,
                              color = if (isHighlighted) SpotifyGreen else TextSecondary.copy(alpha = 0.45f),
                              lineHeight = 28.sp,
                              modifier = Modifier
                                .padding(vertical = 6.dp)
                                .clickable {
                                  onSeek(line.timeSec.toInt())
                                }
                            )
                          }
                        }
                      } else if (track.lyrics.isNotEmpty()) {
                        LazyColumn(
                          state = lyricsListState,
                          modifier = Modifier.fillMaxSize()
                        ) {
                          itemsIndexed(track.lyrics) { index, line ->
                            val isHighlighted = index == activeLyricIndex
                            Text(
                              text = line,
                              style = MaterialTheme.typography.bodyLarge,
                              fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                              fontSize = if (isHighlighted) 19.sp else 15.sp,
                              color = if (isHighlighted) ImmersivePrimary else TextSecondary.copy(alpha = 0.45f),
                              lineHeight = 28.sp,
                              modifier = Modifier.padding(vertical = 6.dp)
                            )
                          }
                        }
                      } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                          Text(
                            text = "Lyrics unavailable for this track.",
                            color = TextMuted,
                            textAlign = TextAlign.Center
                          )
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Track Title, Artist, and Like Button
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = track.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "${track.artist} • ${track.genre}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }

            IconButton(
              onClick = { onToggleLike(track) },
              modifier = Modifier.testTag("now_playing_like_button")
            ) {
              Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = if (isLiked) ImmersivePrimary else TextPrimary,
                modifier = Modifier.size(28.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Compact Real-Time Frequency Waveform Strip (Visible in Artwork mode)
          if (activeViewTab == 0) {
            Canvas3DAudioVisualizer(
              frequencies = visualizerBars,
              isPlaying = isPlaying,
              height = 42.dp,
              currentMode = Visualizer3DMode.CYBER_WAVE,
              showModeSelector = false,
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Scrubber Progress Bar & Timers
          Column(modifier = Modifier.fillMaxWidth()) {
            val progress = if (durationSec > 0) {
              if (isUserSeeking) seekSliderValue / durationSec else currentPositionSec.toFloat() / durationSec
            } else 0f

            Slider(
              value = progress.coerceIn(0f, 1f),
              onValueChange = { frac ->
                isUserSeeking = true
                seekSliderValue = frac * durationSec
              },
              onValueChangeFinished = {
                onSeek(seekSliderValue.toInt())
                isUserSeeking = false
              },
              colors = SliderDefaults.colors(
                thumbColor = ImmersivePrimary,
                activeTrackColor = ImmersivePrimary,
                inactiveTrackColor = ImmersiveOutlineVariant
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("playback_progress_slider")
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              val currentDisplaySec = if (isUserSeeking) seekSliderValue.toInt() else currentPositionSec
              Text(
                text = "%d:%02d".format(currentDisplaySec / 60, currentDisplaySec % 60),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
              )
              Text(
                text = "%d:%02d".format(durationSec / 60, durationSec % 60),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Playback Control Buttons (Shuffle, Prev, Play/Pause, Next, Repeat)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Shuffle
            IconButton(
              onClick = onToggleShuffle,
              modifier = Modifier.testTag("shuffle_button")
            ) {
              Icon(
                Icons.Default.Shuffle,
                contentDescription = "Shuffle",
                tint = if (isShuffled) ImmersivePrimary else TextSecondary,
                modifier = Modifier.size(24.dp)
              )
            }

            // Previous
            IconButton(
              onClick = onPrevious,
              modifier = Modifier
                .size(48.dp)
                .testTag("prev_button")
            ) {
              Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp)
              )
            }

            // Play / Pause prominent rounded button
            Box(
              modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(ImmersivePrimary)
                .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = ImmersivePrimaryContainer)
                .clickable(onClick = onTogglePlayPause)
                .testTag("now_playing_play_pause_button"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = ImmersiveOnPrimary,
                modifier = Modifier.size(40.dp)
              )
            }

            // Next
            IconButton(
              onClick = onNext,
              modifier = Modifier
                .size(48.dp)
                .testTag("next_button")
            ) {
              Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next",
                tint = TextPrimary,
                modifier = Modifier.size(36.dp)
              )
            }

            // Repeat
            IconButton(
              onClick = onCycleRepeat,
              modifier = Modifier.testTag("repeat_button")
            ) {
              when (repeatMode) {
                RepeatMode.OFF -> Icon(
                  Icons.Default.Repeat,
                  contentDescription = "Repeat Off",
                  tint = TextSecondary,
                  modifier = Modifier.size(24.dp)
                )
                RepeatMode.ALL -> Icon(
                  Icons.Default.Repeat,
                  contentDescription = "Repeat All",
                  tint = ImmersivePrimary,
                  modifier = Modifier.size(24.dp)
                )
                RepeatMode.ONE -> Icon(
                  Icons.Default.RepeatOne,
                  contentDescription = "Repeat One",
                  tint = ImmersivePrimary,
                  modifier = Modifier.size(24.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Bottom Bar Actions: Volume + Lyrics Toggle + Add to Playlist
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(
              onClick = {
                activeViewTab = if (activeViewTab == 1) 0 else 1
              },
              modifier = Modifier.testTag("visualizer_shortcut_button")
            ) {
              Icon(
                Icons.Default.GraphicEq,
                contentDescription = "Toggle Visualizer",
                tint = if (activeViewTab == 1) ImmersivePrimary else TextSecondary,
                modifier = Modifier.size(22.dp)
              )
            }

            // Volume Slider
            Row(
              modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.VolumeDown, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
              Slider(
                value = volume,
                onValueChange = onSetVolume,
                colors = SliderDefaults.colors(
                  thumbColor = Color.White,
                  activeTrackColor = Color.White,
                  inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                ),
                modifier = Modifier
                  .weight(1f)
                  .padding(horizontal = 4.dp)
                  .testTag("volume_slider")
              )
              Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }

            // Add to Playlist
            IconButton(
              onClick = { onAddToPlaylist(track) },
              modifier = Modifier.testTag("add_to_playlist_shortcut")
            ) {
              Icon(
                Icons.Default.PlaylistAdd,
                contentDescription = "Add to Playlist",
                tint = TextSecondary,
                modifier = Modifier.size(24.dp)
              )
            }
          }
        }
      }
    }
  }
}
