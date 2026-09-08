package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.PlayerState
import com.example.service.ShakeDetector
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.NowPlayingSheet
import com.example.ui.components.SettingsSheet
import com.example.ui.components.SleepTimerDialog
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.BgDark
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SpotiFusionTheme
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.MusicViewModel

sealed class NavDestination(val route: String, val label: String, val icon: ImageVector) {
  object Home : NavDestination("home", "Home", Icons.Default.Home)
  object Search : NavDestination("search", "Search", Icons.Default.Search)
  object Library : NavDestination("library", "Library", Icons.Default.LibraryMusic)
  object Equalizer : NavDestination("equalizer", "Equalizer", Icons.Default.GraphicEq)
}

class MainActivity : ComponentActivity() {
  private val viewModel: MusicViewModel by viewModels()
  private var shakeDetector: ShakeDetector? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    try { com.example.service.PlaybackController.connect(this) } catch (e: Exception) { android.util.Log.e("MainActivity", "PlaybackController connection: ${e.message}") }
    try {
      val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
      if (auth.currentUser == null) auth.signInAnonymously().addOnFailureListener { e -> android.util.Log.w("MainActivity", "Firebase auth: ${e.message}") }
    } catch (e: Exception) { android.util.Log.w("MainActivity", "Firebase init: ${e.message}") }
    shakeDetector = ShakeDetector(this) { if (viewModel.settingsState.value.shakeToSkipEnabled) viewModel.nextTrack() }
    setContent {
      val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
      SpotiFusionTheme(darkTheme = settingsState.darkTheme) { SpotiFusionApp(viewModel) }
    }
  }
  override fun onResume() { super.onResume(); shakeDetector?.start() }
  override fun onPause() { super.onPause(); shakeDetector?.stop() }
  override fun onDestroy() { runCatching { com.example.service.PlaybackController.disconnect() }; super.onDestroy() }
}

@Composable
fun SpotiFusionApp(viewModel: MusicViewModel) {
  var currentDestination by remember { mutableStateOf<NavDestination>(NavDestination.Home) }
  val playerState by viewModel.playerState.collectAsStateWithLifecycle()
  val likedTracks by viewModel.likedTracks.collectAsStateWithLifecycle()
  val playlists by viewModel.playlists.collectAsStateWithLifecycle()
  val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()
  val localTracks by viewModel.localTracks.collectAsStateWithLifecycle()
  val catalogTracks by viewModel.catalogTracks.collectAsStateWithLifecycle()
  val downloadedIds by viewModel.downloadedTrackIds.collectAsStateWithLifecycle()
  val downloadedTracks = remember(catalogTracks, localTracks, downloadedIds) { (catalogTracks + localTracks).distinctBy { it.id }.filter { it.id in downloadedIds } }
  val equalizerState by viewModel.equalizerState.collectAsStateWithLifecycle()
  val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
  val currentSyncedLyrics by viewModel.currentSyncedLyrics.collectAsStateWithLifecycle()
  val sleepTimerMinutes by viewModel.sleepTimerMinutes.collectAsStateWithLifecycle()
  val sleepTimerRemainingSec by viewModel.sleepTimerRemainingSec.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
  val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
  val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsStateWithLifecycle()
  val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
  val isSleepTimerOpen by viewModel.isSleepTimerOpen.collectAsStateWithLifecycle()
  val selectedPlaylist by viewModel.selectedPlaylist.collectAsStateWithLifecycle()
  val selectedPlaylistTracks by viewModel.selectedPlaylistTracks.collectAsStateWithLifecycle()
  val trackForPlaylistDialog by viewModel.trackForPlaylistDialog.collectAsStateWithLifecycle()
  val isCurrentTrackLiked = playerState.currentTrack?.let { viewModel.isTrackLiked(it.id) } ?: false
  val navItems = listOf(NavDestination.Home, NavDestination.Search, NavDestination.Library, NavDestination.Equalizer)
  val context = LocalContext.current
  val activity = context as? ComponentActivity

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = BgDark,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    bottomBar = {
      Column(Modifier.fillMaxWidth().background(SurfaceDark).navigationBarsPadding()) {
        if (playerState.currentTrack != null) MiniPlayerBar(
          currentTrack = playerState.currentTrack, isPlaying = playerState.isPlaying,
          currentPositionSec = playerState.currentPositionSec, durationSec = playerState.durationSec,
          isLiked = isCurrentTrackLiked, onTogglePlayPause = viewModel::togglePlayPause,
          onNextTrack = viewModel::nextTrack, onToggleLike = viewModel::toggleLike,
          onClickBar = { viewModel.setNowPlayingExpanded(true) }
        )
        NavigationBar(
          containerColor = SurfaceDark, contentColor = SpotifyGreen, tonalElevation = 0.dp,
          windowInsets = WindowInsets(0, 0, 0, 0), modifier = Modifier.fillMaxWidth().border(1.dp, GlassBorder).height(64.dp).testTag("main_bottom_nav_bar")
        ) {
          navItems.forEach { item ->
            val selected = currentDestination.route == item.route
            NavigationBarItem(
              selected = selected,
              onClick = { currentDestination = item; if (item == NavDestination.Library) viewModel.selectPlaylist(null) },
              icon = {
                Box(Modifier.size(if (selected) 42.dp else 38.dp).clip(RoundedCornerShape(14.dp)).background(if (selected) SpotifyGreen.copy(alpha = .16f) else Color.Transparent)) {
                  Icon(imageVector = item.icon, contentDescription = item.label, modifier = Modifier.fillMaxSize().padding(9.dp))
                }
              },
              label = { Text(item.label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
              colors = NavigationBarItemDefaults.colors(selectedIconColor = SpotifyGreen, selectedTextColor = SpotifyGreen, unselectedIconColor = TextMuted, unselectedTextColor = TextMuted),
              modifier = Modifier.testTag("nav_item_${item.route}")
            )
          }
        }
      }
    }
  ) { innerPadding ->
    Box(Modifier.fillMaxSize().padding(innerPadding)) {
      when (currentDestination) {
        NavDestination.Home -> HomeScreen(
          tracks = catalogTracks,
          recentTracks = recentHistory,
          currentPlayingTrack = playerState.currentTrack,
          isPlaying = playerState.isPlaying,
          onTrackClick = { t, q -> viewModel.playTrack(t, q) },
          onNavigateToSearch = { currentDestination = NavDestination.Search },
          onNavigateToLibrary = { currentDestination = NavDestination.Library },
          onOpenSettings = { viewModel.setSettingsOpen(true) }
        )
        NavDestination.Search -> SearchScreen(
          searchQuery = searchQuery,
          selectedGenre = selectedGenre,
          searchResults = searchResults,
          currentPlayingTrack = playerState.currentTrack,
          isPlaying = playerState.isPlaying,
          onSearchQueryChanged = viewModel::onSearchQueryChanged,
          onGenreSelected = viewModel::onGenreSelected,
          onTrackClick = { t, q -> viewModel.playTrack(t, q) },
          onToggleLike = viewModel::toggleLike,
          isTrackLiked = { trackId -> viewModel.isTrackLiked(trackId) },
          onAddToPlaylist = viewModel::showAddToPlaylistDialog
        )
        NavDestination.Library -> LibraryScreen(
          playlists = playlists,
          likedTracks = likedTracks,
          recentHistory = recentHistory,
          localTracks = localTracks,
          downloadedTracks = downloadedTracks,
          selectedPlaylist = selectedPlaylist,
          selectedPlaylistTracks = selectedPlaylistTracks,
          currentPlayingTrack = playerState.currentTrack,
          isPlaying = playerState.isPlaying,
          onSelectPlaylist = viewModel::selectPlaylist,
          onTrackClick = { t, q -> viewModel.playTrack(t, q) },
          onPlayAll = { tracks -> viewModel.playTrack(tracks.first(), tracks) },
          onShufflePlay = { tracks -> viewModel.playTrack(tracks.random(), tracks) },
          onToggleLike = viewModel::toggleLike,
          onCreatePlaylistDialog = { viewModel.showAddToPlaylistDialog(null) },
          onDeletePlaylist = viewModel::deletePlaylist,
          onScanLocalTracks = viewModel::scanLocalMusic,
          onRemoveTrackFromPlaylist = viewModel::removeTrackFromPlaylist
        )
        NavDestination.Equalizer -> EqualizerScreen(
          equalizerState = equalizerState,
          onToggleEnabled = viewModel::setEqualizerEnabled,
          onSelectPreset = viewModel::setEqualizerPreset,
          onBandGainChanged = viewModel::setEqualizerBandGain,
          onBassBoostChanged = viewModel::setBassBoost,
          onVirtualizerChanged = viewModel::setVirtualizer,
          onBack = { currentDestination = NavDestination.Home }
        )
      }

      if (isNowPlayingExpanded && playerState.currentTrack != null) NowPlayingSheet(
        track = playerState.currentTrack,
        isPlaying = playerState.isPlaying,
        currentPositionSec = playerState.currentPositionSec,
        durationSec = playerState.durationSec,
        isLiked = isCurrentTrackLiked,
        isShuffled = playerState.isShuffled,
        repeatMode = playerState.repeatMode,
        volume = playerState.volume,
        visualizerBars = playerState.visualizerBars,
        syncedLyrics = currentSyncedLyrics,
        sleepTimerMinutes = sleepTimerMinutes,
        onClose = { viewModel.setNowPlayingExpanded(false) },
        onTogglePlayPause = viewModel::togglePlayPause,
        onNext = viewModel::nextTrack,
        onPrevious = viewModel::previousTrack,
        onSeek = viewModel::seekTo,
        onToggleShuffle = viewModel::toggleShuffle,
        onCycleRepeat = viewModel::cycleRepeatMode,
        onToggleLike = viewModel::toggleLike,
        onSetVolume = viewModel::setVolume,
        onOpenEqualizer = { viewModel.setNowPlayingExpanded(false); currentDestination = NavDestination.Equalizer },
        onOpenSleepTimer = { viewModel.setSleepTimerOpen(true) },
        onAddToPlaylist = viewModel::showAddToPlaylistDialog,
        onDownload = viewModel::downloadTrack,
        onRemoveDownload = viewModel::removeDownloadedTrack,
        isDownloaded = viewModel.isTrackDownloaded(playerState.currentTrack.id),
        lyricsAutoScroll = settingsState.lyricsAutoScroll,
        visualizerHighFps = settingsState.visualizer60fps
      )

      if (trackForPlaylistDialog != null) AddToPlaylistDialog(
        trackForPlaylistDialog,
        playlists,
        { viewModel.showAddToPlaylistDialog(null) },
        { id, track -> viewModel.addTrackToPlaylist(id, track) },
        onCreatePlaylist = { name, description, track ->
          viewModel.createPlaylist(name, description)
          viewModel.addTrackToPlaylist(playlists.find { it.title == name }?.id ?: "", track)
        }
      )

      if (isSettingsOpen) SettingsSheet(
        settings = settingsState, onUpdateAudioQuality = viewModel::updateAudioQuality, onUpdateCrossfade = viewModel::updateCrossfade,
        onToggleShakeToSkip = viewModel::toggleShakeToSkip, onToggleLyricsAutoScroll = viewModel::toggleLyricsAutoScroll,
        onToggleVisualizer60fps = viewModel::toggleVisualizer60fps, onToggleNotifications = { enabled -> viewModel.toggleNotifications(enabled); if (enabled && android.os.Build.VERSION.SDK_INT >=[...]
        onToggleDarkTheme = viewModel::setDarkTheme, onClearCache = viewModel::clearCache, onDismiss = { viewModel.setSettingsOpen(false) }
      )

      if (isSleepTimerOpen) SleepTimerDialog(sleepTimerMinutes, sleepTimerRemainingSec, viewModel::setSleepTimer) { viewModel.setSleepTimerOpen(false) }
    }
  }
}