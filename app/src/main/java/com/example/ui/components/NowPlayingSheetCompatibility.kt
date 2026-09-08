package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.model.RepeatMode
import com.example.model.SyncedLyricLine
import com.example.model.Track

/**
 * Compatibility overload used by the current AI Studio MainActivity.
 * The repository's existing player sheet remains the source of truth while
 * newer callers can pass download/visualizer/lyrics preferences safely.
 */
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
  onAddToPlaylist: (Track) -> Unit,
  onDownload: (Track) -> Unit = {},
  onRemoveDownload: (Track) -> Unit = {},
  isDownloaded: Boolean = false,
  lyricsAutoScroll: Boolean = true,
  visualizerHighFps: Boolean = true
) {
  NowPlayingSheet(
    track = track,
    isPlaying = isPlaying,
    currentPositionSec = currentPositionSec,
    durationSec = durationSec,
    isLiked = isLiked,
    isShuffled = isShuffled,
    repeatMode = repeatMode,
    volume = volume,
    visualizerBars = visualizerBars,
    syncedLyrics = syncedLyrics,
    sleepTimerMinutes = sleepTimerMinutes,
    onClose = onClose,
    onTogglePlayPause = onTogglePlayPause,
    onNext = onNext,
    onPrevious = onPrevious,
    onSeek = onSeek,
    onToggleShuffle = onToggleShuffle,
    onCycleRepeat = onCycleRepeat,
    onToggleLike = onToggleLike,
    onSetVolume = onSetVolume,
    onOpenEqualizer = onOpenEqualizer,
    onOpenSleepTimer = onOpenSleepTimer,
    onAddToPlaylist = onAddToPlaylist
  )
}
