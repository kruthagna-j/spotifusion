package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.FusionBlend
import com.example.model.Track
import com.example.ui.components.Canvas3DAudioVisualizer
import com.example.ui.components.Visualizer3DMode
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurfaceCard
import com.example.ui.theme.ImmersiveOutlineVariant
import com.example.ui.theme.BgDark
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SpotifyGreenDark
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

import androidx.compose.material.icons.filled.Settings

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
    modifier = Modifier
      .fillMaxSize()
      .background(BgDark)
      .testTag("home_screen"),
    contentPadding = PaddingValues(bottom = 16.dp)
  ) {
    // Top Bar
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(SpotifyGreenDark)
              .border(1.dp, SpotifyGreen, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Image(
              painter = painterResource(R.drawable.ic_spotifusion_logo),
              contentDescription = "SpotiFusion Logo",
              modifier = Modifier.size(34.dp)
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(
              text = "Good Evening",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Black,
              color = TextPrimary
            )
            Text(
              text = "Ready to explore your soundscape?",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onNavigateToEqualizer) {
            Icon(Icons.Default.GraphicEq, contentDescription = "Equalizer", tint = SpotifyGreen)
          }
          IconButton(onClick = onOpenSettings, modifier = Modifier.testTag("home_settings_button")) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
          }
        }
      }
    }

    // Hero Banner Card
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .shadow(16.dp, RoundedCornerShape(24.dp))
          .clip(RoundedCornerShape(24.dp))
          .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
          .background(SurfaceElevated)
      ) {
        // Hero background image
        Image(
          painter = painterResource(R.drawable.img_spotifusion_hero),
          contentDescription = "SpotiFusion Banner",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
        )

        // Gradient overlay
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
              Brush.verticalGradient(
                listOf(
                  Color.Transparent,
                  Color.Black.copy(alpha = 0.55f),
                  Color.Black.copy(alpha = 0.85f)
                )
              )
            )
        )

        // Banner content
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .align(Alignment.BottomStart)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(20.dp))
              .background(SpotifyGreen.copy(alpha = 0.2f))
              .border(1.dp, SpotifyGreen.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
              .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("MUSIC WITHOUT LIMITS", color = SpotifyGreen, fontSize = 10.sp, fontWeight = FontWeight.Black)
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Everything you love. One place.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = Color.White
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = {
                if (tracks.isNotEmpty()) onTrackClick(tracks.first(), tracks)
              },
              colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
              shape = RoundedCornerShape(20.dp),
              modifier = Modifier.testTag("hero_quick_play_button")
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Quick Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            OutlinedButton(
              onClick = onNavigateToSearch,
              shape = RoundedCornerShape(20.dp),
              colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.5f))
            ) {
              Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Search Music", fontSize = 13.sp)
            }
          }
        }
      }
    }

    // Quick Shortcuts Grid
    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
          text = "Quick Access",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          QuickAccessTile(
            icon = Icons.Default.Search,
            title = "Search & Explore",
            subtitle = "Songs, artists, tags",
            accentColor = BrandCyan,
            modifier = Modifier.weight(1f),
            onClick = onNavigateToSearch
          )
          QuickAccessTile(
            icon = Icons.Default.Favorite,
            title = "Liked Songs",
            subtitle = "Your favorites",
            accentColor = SpotifyGreen,
            modifier = Modifier.weight(1f),
            onClick = onNavigateToLibrary
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          QuickAccessTile(
            icon = Icons.Default.AutoAwesome,
            title = "Fusion Blends",
            subtitle = "Genre harmony mixes",
            accentColor = BrandPurple,
            modifier = Modifier.weight(1f),
            onClick = onNavigateToBlends
          )
          QuickAccessTile(
            icon = Icons.Default.Equalizer,
            title = "Equalizer",
            subtitle = "5-band audio FX",
            accentColor = Color(0xFFFFB300),
            modifier = Modifier.weight(1f),
            onClick = onNavigateToEqualizer
          )
        }
      }
    }

    // 3D Soundscape Visualizer Card
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.GraphicEq,
              contentDescription = null,
              tint = ImmersivePrimary,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "3D Real-Time Visualizer",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }

          if (currentPlayingTrack != null) {
            Text(
              text = if (isPlaying) "Live Playing" else "Paused",
              style = MaterialTheme.typography.labelSmall,
              color = if (isPlaying) ImmersivePrimary else TextMuted,
              fontWeight = FontWeight.Medium
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Canvas3DAudioVisualizer(
          frequencies = if (isPlaying) visualizerBars else List(32) { 0.08f },
          isPlaying = isPlaying,
          height = 150.dp,
          showModeSelector = true,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    // Fusion Blends Spotlight
    if (fusionBlends.isNotEmpty()) {
      item {
        val blend = fusionBlends.first()
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Fusion Spotlight",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "See all",
              style = MaterialTheme.typography.labelMedium,
              color = SpotifyGreen,
              modifier = Modifier.clickable(onClick = onNavigateToBlends)
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(18.dp))
              .background(
                Brush.linearGradient(
                  listOf(Color(blend.gradientStart), Color(blend.gradientEnd))
                )
              )
              .padding(18.dp)
              .clickable { onPlayBlend(blend) }
              .testTag("fusion_spotlight_card")
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
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = "${blend.matchScore}% Taste Match",
                    color = SpotifyGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                  )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = blend.title,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Black,
                  color = Color.White
                )
                Text(
                  text = blend.subtitle,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.White.copy(alpha = 0.8f),
                  fontSize = 12.sp
                )
              }

              Box(
                modifier = Modifier
                  .size(46.dp)
                  .clip(CircleShape)
                  .background(Color.White)
                  .shadow(6.dp, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  Icons.Default.PlayArrow,
                  contentDescription = "Play Blend",
                  tint = SpotifyGreen,
                  modifier = Modifier.size(28.dp)
                )
              }
            }
          }
        }
      }
    }

    // Made For You / Recommended
    item {
      Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Made For You",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              text = "Personalized audio selections",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          items(tracks.take(6), key = { it.id }) { track ->
            TrackCard(
              track = track,
              isCurrent = currentPlayingTrack?.id == track.id,
              isPlaying = isPlaying && currentPlayingTrack?.id == track.id,
              onClick = { onTrackClick(track, tracks) }
            )
          }
        }
      }
    }

    // Trending Top Charts
    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
          text = "Trending Now",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )
        Text(
          text = "Top picks across genres today",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        tracks.drop(3).take(5).forEachIndexed { index, track ->
          TrackRowItem(
            track = track,
            index = index + 1,
            isCurrent = currentPlayingTrack?.id == track.id,
            isPlaying = isPlaying && currentPlayingTrack?.id == track.id,
            onClick = { onTrackClick(track, tracks) },
            onOptionsClick = { onOpenAddToPlaylist(track) }
          )
        }
      }
    }
  }
}

@Composable
fun QuickAccessTile(
  icon: ImageVector,
  title: String,
  subtitle: String,
  accentColor: Color,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(SurfaceCard)
      .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(14.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(accentColor.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(10.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = TextMuted,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
fun TrackCard(
  track: Track,
  isCurrent: Boolean,
  isPlaying: Boolean,
  onClick: () -> Unit
) {
  Column(
    modifier = Modifier
      .width(145.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(SurfaceCard)
      .border(
        width = 1.dp,
        color = if (isCurrent) SpotifyGreen else GlassBorder,
        shape = RoundedCornerShape(16.dp)
      )
      .clickable(onClick = onClick)
      .padding(10.dp)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(12.dp))
        .background(BgDark)
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
          modifier = Modifier
            .size(32.dp)
            .align(Alignment.Center)
        )
      }

      if (isCurrent) {
        Box(
          modifier = Modifier
            .size(32.dp)
            .padding(4.dp)
            .clip(CircleShape)
            .background(SpotifyGreen)
            .align(Alignment.BottomEnd),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (isPlaying) Icons.Default.Equalizer else Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = track.title,
      style = MaterialTheme.typography.bodyMedium,
      fontWeight = FontWeight.Bold,
      color = if (isCurrent) SpotifyGreen else TextPrimary,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
    Text(
      text = track.artist,
      style = MaterialTheme.typography.bodySmall,
      color = TextSecondary,
      fontSize = 11.sp,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

@Composable
fun TrackRowItem(
  track: Track,
  index: Int,
  isCurrent: Boolean,
  isPlaying: Boolean,
  onClick: () -> Unit,
  onOptionsClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = index.toString(),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = if (isCurrent) SpotifyGreen else TextMuted,
        modifier = Modifier.width(28.dp)
      )

      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(8.dp))
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
            modifier = Modifier
              .size(20.dp)
              .align(Alignment.Center)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column {
        Text(
          text = track.title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
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
      Text(
        text = track.formattedDuration,
        style = MaterialTheme.typography.labelSmall,
        color = TextMuted
      )

      IconButton(onClick = onOptionsClick) {
        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = TextMuted)
      }
    }
  }
}
