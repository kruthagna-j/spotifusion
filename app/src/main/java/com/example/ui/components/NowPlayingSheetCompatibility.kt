package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.RepeatMode
import com.example.model.SyncedLyricLine
import com.example.model.Track

@Composable
fun NowPlayingSheet(
  track: Track?, isPlaying: Boolean, currentPositionSec: Int, durationSec: Int,
  isLiked: Boolean, isShuffled: Boolean, repeatMode: RepeatMode, volume: Float,
  visualizerBars: List<Float>, syncedLyrics: List<SyncedLyricLine> = emptyList(),
  sleepTimerMinutes: Int? = null, onClose: () -> Unit,
  onTogglePlayPause: () -> Unit, onNext: () -> Unit, onPrevious: () -> Unit,
  onSeek: (Int) -> Unit, onToggleShuffle: () -> Unit, onCycleRepeat: () -> Unit,
  onToggleLike: (Track) -> Unit, onSetVolume: (Float) -> Unit,
  onOpenEqualizer: () -> Unit, onOpenSleepTimer: () -> Unit = {},
  onAddToPlaylist: (Track) -> Unit, onDownload: (Track) -> Unit,
  onRemoveDownload: (Track) -> Unit, isDownloaded: Boolean,
  lyricsAutoScroll: Boolean, visualizerHighFps: Boolean
) {
  NowPlayingSheet(
    track = track, isPlaying = isPlaying, currentPositionSec = currentPositionSec,
    durationSec = durationSec, isLiked = isLiked, isShuffled = isShuffled,
    repeatMode = repeatMode, volume = volume, visualizerBars = visualizerBars,
    syncedLyrics = syncedLyrics, sleepTimerMinutes = sleepTimerMinutes,
    onClose = onClose, onTogglePlayPause = onTogglePlayPause, onNext = onNext,
    onPrevious = onPrevious, onSeek = onSeek, onToggleShuffle = onToggleShuffle,
    onCycleRepeat = onCycleRepeat, onToggleLike = onToggleLike, onSetVolume = onSetVolume,
    onOpenEqualizer = onOpenEqualizer, onOpenSleepTimer = onOpenSleepTimer,
    onAddToPlaylist = onAddToPlaylist
  )

  if (track != null) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(
      usePlatformDefaultWidth = false, dismissOnBackPress = false, dismissOnClickOutside = false
    )) {
      Box(Modifier.fillMaxSize()) {
        Surface(
          modifier = Modifier.align(Alignment.TopEnd).padding(top = 44.dp, end = 12.dp),
          color = Color.Transparent
        ) {
          IconButton(
            onClick = { if (isDownloaded) onRemoveDownload(track) else onDownload(track) },
            modifier = Modifier.background(Color.Black.copy(alpha = 0.35f))
          ) {
            Icon(
              imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
              contentDescription = if (isDownloaded) "Remove download" else "Download for offline playback",
              tint = Color.White
            )
          }
        }
      }
    }
  }
}
