package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.ImmersiveOnPrimary
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersiveOutlineVariant
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MiniPlayerBar(
  currentTrack: Track?, isPlaying: Boolean, currentPositionSec: Int, durationSec: Int,
  isLiked: Boolean, onTogglePlayPause: () -> Unit, onNextTrack: () -> Unit,
  onToggleLike: (Track) -> Unit, onClickBar: () -> Unit, modifier: Modifier = Modifier
) {
  if (currentTrack == null) return
  val progress = if (durationSec > 0) (currentPositionSec.toFloat() / durationSec.toFloat()).coerceIn(0f, 1f) else 0f
  Box(
    modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
      .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
      .clip(RoundedCornerShape(16.dp))
      .background(Brush.horizontalGradient(listOf(ImmersiveSurfaceCard, SurfaceElevated)))
      .border(1.dp, ImmersiveOutlineVariant, RoundedCornerShape(18.dp))
      .clickable(onClick = onClickBar).testTag("mini_player_bar")
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, ImmersiveOutline, RoundedCornerShape(12.dp)).background(BgDark)) {
            if (currentTrack.coverUrl.isNotBlank()) {
              AsyncImage(model = currentTrack.coverUrl, contentDescription = currentTrack.title, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            } else {
              Icon(Icons.Default.MusicNote, contentDescription = null, tint = ImmersivePrimary, modifier = Modifier.size(24.dp).align(Alignment.Center))
            }
          }
          Spacer(Modifier.width(12.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(currentTrack.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(currentTrack.artist, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = { onToggleLike(currentTrack) }, modifier = Modifier.size(36.dp).testTag("mini_player_like_button")) {
            Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (isLiked) "Unlike" else "Like", tint = if (isLiked) ImmersivePrimary else TextSecondary, modifier = Modifier.size(20.dp))
          }
          Spacer(Modifier.width(4.dp))
          Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(ImmersivePrimary).clickable(onClick = onTogglePlayPause).testTag("mini_player_play_pause"), contentAlignment = Alignment.Center) {
            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pause" else "Play", tint = ImmersiveOnPrimary, modifier = Modifier.size(22.dp))
          }
          Spacer(Modifier.width(4.dp))
          IconButton(onClick = onNextTrack, modifier = Modifier.size(36.dp).testTag("mini_player_next")) {
            Icon(Icons.Default.SkipNext, "Next", tint = TextPrimary, modifier = Modifier.size(24.dp))
          }
        }
      }
      LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp), color = ImmersivePrimary, trackColor = ImmersiveOutline)
    }
  }
}
