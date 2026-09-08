package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.model.RepeatMode
import com.example.model.SyncedLyricLine
import com.example.model.Track
import com.example.ui.theme.BgDark
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NowPlayingSheet(
  track: Track?, isPlaying: Boolean, currentPositionSec: Int, durationSec: Int, isLiked: Boolean,
  isShuffled: Boolean, repeatMode: RepeatMode, volume: Float, visualizerBars: List<Float>,
  syncedLyrics: List<SyncedLyricLine> = emptyList(), sleepTimerMinutes: Int? = null,
  onClose: () -> Unit, onTogglePlayPause: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
  onSeek: (Int) -> Unit, onToggleShuffle: () -> Unit, onCycleRepeat: () -> Unit,
  onToggleLike: (Track) -> Unit, onSetVolume: (Float) -> Unit, onOpenEqualizer: () -> Unit,
  onOpenSleepTimer: () -> Unit = {}, onAddToPlaylist: (Track) -> Unit,
  onDownload: (Track) -> Unit = {}, onRemoveDownload: (Track) -> Unit = {}, isDownloaded: Boolean = false,
  lyricsAutoScroll: Boolean = true, visualizerHighFps: Boolean = true
) {
  if (track == null) return
  var lyricsOpen = false
  Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Surface(Modifier.fillMaxSize().background(BgDark), color = BgDark) {
      Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
          IconButton(onClick = onClose) { Icon(Icons.Default.KeyboardArrowDown, "Close", tint = TextPrimary, modifier = Modifier.size(26.dp)) }
          Text("Now Playing", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          IconButton(onClick = onOpenSleepTimer) { Icon(Icons.Default.Timer, "Sleep timer", tint = if (sleepTimerMinutes != null) BrandPurple else TextSecondary, modifier = Modifier.size(20.dp)) }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)).background(SurfaceCard)) {
          if (track.coverUrl.isNotBlank()) AsyncImage(track.coverUrl, track.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
          else Box(Modifier.fillMaxSize(), Alignment.Center) { Text("♪", color = BrandPurple, fontSize = 80.sp) }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
          Column(Modifier.weight(1f)) {
            Text(track.title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = TextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          IconButton(onClick = { onToggleLike(track) }) { Icon(if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Like", tint = if (isLiked) BrandPurple else TextSecondary) }
        }
        Slider(value = currentPositionSec.coerceIn(0, durationSec.coerceAtLeast(1)).toFloat(), onValueChange = { onSeek(it.toInt()) }, valueRange = 0f..durationSec.coerceAtLeast(1).toFloat(), colors = SliderDefaults.colors(thumbColor = BrandPurple, activeTrackColor = BrandPurple, inactiveTrackColor = SurfaceElevated), modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) { Text(formatTime(currentPositionSec), color = TextMuted, fontSize = 9.sp); Text(formatTime(durationSec), color = TextMuted, fontSize = 9.sp) }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
          IconButton(onClick = onToggleShuffle) { Icon(Icons.Default.Shuffle, "Shuffle", tint = if (isShuffled) BrandPurple else TextSecondary) }
          IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, "Previous", tint = TextPrimary, modifier = Modifier.size(28.dp)) }
          IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(58.dp).clip(androidx.compose.foundation.shape.CircleShape).background(BrandPurple)) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", tint = Color.White, modifier = Modifier.size(30.dp)) }
          IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, "Next", tint = TextPrimary, modifier = Modifier.size(28.dp)) }
          IconButton(onClick = onCycleRepeat) { Icon(if (repeatMode.name.contains("ONE", true)) Icons.Default.RepeatOne else Icons.Default.Repeat, "Repeat", tint = if (repeatMode.name.contains("OFF", true)) TextSecondary else BrandPurple) }
        }
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
          MiniAction(Icons.Default.Lyrics, "Lyrics") { lyricsOpen = !lyricsOpen }
          MiniAction(Icons.Default.VolumeUp, "Volume") { }
          MiniAction(Icons.Default.Add, "Playlist") { onAddToPlaylist(track) }
          MiniAction(Icons.Default.Timer, "Timer", onOpenSleepTimer)
        }
        if (lyricsOpen) {
          Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceElevated).padding(12.dp)) {
            Column { Text("Lyrics", color = BrandPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text((syncedLyrics.firstOrNull()?.text ?: track.lyrics.firstOrNull() ?: "Lyrics unavailable"), color = TextPrimary, fontSize = 13.sp) }
          }
        }
      }
    }
  }
}

@Composable private fun MiniAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(4.dp)) { Icon(icon, label, tint = TextSecondary, modifier = Modifier.size(20.dp)); Text(label, color = TextMuted, fontSize = 8.sp) }
}
private fun formatTime(sec: Int): String = "%d:%02d".format(sec.coerceAtLeast(0) / 60, sec.coerceAtLeast(0) % 60)
