package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.screens.FusionBlendsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.theme.BgDark
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SpotiFusionTheme
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MusicViewModel

sealed class NavDestination(val route: String, val label: String, val icon: ImageVector) {
  object Home : NavDestination("home", "Home", Icons.Default.Home)
  object Search : NavDestination("search", "Search", Icons.Default.Search)
  object Blends : NavDestination("blends", "Fusion", Icons.Default.AutoAwesome)
  object Library : NavDestination("library", "Library", Icons.Default.LibraryMusic)
  object Equalizer : NavDestination("equalizer", "Equalizer", Icons.Default.GraphicEq)
}

class MainActivity : ComponentActivity() {

  private val viewModel: MusicViewModel by viewModels()
  private var shakeDetector: ShakeDetector? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    try {
      com.example.service.PlaybackController.connect(this)
    } catch (e: Exception) {
      android.util.Log.e("MainActivity", "Gracefully handled PlaybackController connection: ${e.message}")
    }

    try {
      val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
      if (auth.currentUser == null) {
        auth.signInAnonymously()
          .addOnSuccessListener {
            android.util.Log.d("MainActivity", "Firebase anonymous auth succeeded")
          }
          .addOnFailureListener { e ->
            android.util.Log.w("MainActivity", "Firebase auth note: ${e.message}")
          }
      }
    } catch (e: Exception) {
      android.util.Log.w("MainActivity", "Firebase init note: ${e.message}")
    }

    shakeDetector = ShakeDetector(this) {
      if (viewModel.settingsState.value.shakeToSkipEnabled) {
        viewModel.nextTrack()
      }
    }

    setContent {
      SpotiFusionTheme {
        SpotiFusionApp(viewModel = viewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    shakeDetector?.start()
  }

  override fun onPause() {
    super.onPause()
    shakeDetector?.stop()
  }

  override fun onDestroy() {
    try {
      com.example.service.PlaybackController.disconnect()
    } catch (e: Exception) {
      android.util.Log.e("MainActivity", "PlaybackController disconnect note: ${e.message}")
    }
    super.onDestroy()
  }
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
  val fusionBlends by viewModel.fusionBlends.collectAsStateWithLifecycle()
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

  val navItems = listOf(
    NavDestination.Home,
    NavDestination.Search,
    NavDestination.Blends,
    NavDestination.Library,
    NavDestination.Equalizer
  )

    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = BgDark,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      bottomBar = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .navigationBarsPadding()
        ) {
          // Floating Mini Player Bar if a track is active
          if (playerState.currentTrack != null) {
            MiniPlayerBar(
              currentTrack = playerState.currentTrack,
              isPlaying = playerState.isPlaying,
              currentPositionSec = playerState.currentPositionSec,
              durationSec = playerState.durationSec,
              isLiked = isCurrentTrackLiked,
              onTogglePlayPause = { viewModel.togglePlayPause() },
              onNextTrack = { viewModel.nextTrack() },
              onToggleLike = { viewModel.toggleLike(it) },
              onClickBar = { viewModel.setNowPlayingExpanded(true) }
            )
          }

          // Bottom Navigation Bar
          NavigationBar(
            containerColor = SurfaceDark,
            contentColor = SpotifyGreen,
            tonalElevation = 8.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier
              .fillMaxWidth()
              .border(width = 1.dp, color = GlassBorder)
              .height(64.dp)
              .testTag("main_bottom_nav_bar")
          ) {
            navItems.forEach { item ->
              val isSelected = currentDestination.route == item.route
              NavigationBarItem(
                selected = isSelected,
                onClick = {
                  currentDestination = item
                  if (item == NavDestination.Library) {
                    viewModel.selectPlaylist(null)
                  }
                },
                icon = {
                  Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    modifier = Modifier.size(24.dp)
                  )
                },
                label = {
                  Text(
                    text = item.label,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                  )
                },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = com.example.ui.theme.ImmersiveOnSecondaryContainer,
                  selectedTextColor = com.example.ui.theme.ImmersiveOnSecondaryContainer,
                  unselectedIconColor = TextSecondary.copy(alpha = 0.6f),
                  unselectedTextColor = TextMuted,
                  indicatorColor = com.example.ui.theme.ImmersiveSecondaryContainer
                ),
                modifier = Modifier.testTag("nav_item_${item.route}")
              )
            }
          }
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
      when (currentDestination) {
        NavDestination.Home -> HomeScreen(
          tracks = catalogTracks,
          fusionBlends = fusionBlends,
          recentTracks = recentHistory,
          currentPlayingTrack = playerState.currentTrack,
          isPlaying = playerState.isPlaying,
          visualizerBars = playerState.visualizerBars,
          onTrackClick = { track, queue -> viewModel.playTrack(track, queue) },
          onPlayBlend = { blend -> viewModel.playFusionBlend(blend) },
          onNavigateToSearch = { currentDestination = NavDestination.Search },
          onNavigateToLibrary = { currentDestination = NavDestination.Library },
          onNavigateToBlends = { currentDestination = NavDestination.Blends },
          onNavigateToEqualizer = { currentDestination = NavDestination.Equalizer },
          onOpenSettings = { viewModel.setSettingsOpen(true) },
          onOpenAddToPlaylist = { track -> viewModel.showAddToPlaylistDialog(track) }
        )

        NavDestination.Search -> SearchScreen(
          searchQuery = searchQuery,
          selectedGenre = selectedGenre,
          searchResults = searchResults,
          currentPlayingTrack = playerState.currentTrack,
          isPlaying = playerState.isPlaying,
          onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
          onGenreSelected = { viewModel.onGenreSelected(it) },
          onTrackClick = { track, queue -> viewModel.playTrack(track, queue) },
          onToggleLike = { viewModel.toggleLike(it) },
          isTrackLiked = { viewModel.isTrackLiked(it) },
          onAddToPlaylist = { track -> viewModel.showAddToPlaylistDialog(track) }
        )

        NavDestination.Blends -> FusionBlendsScreen(
          fusionBlends = fusionBlends,
          onPlayBlend = { blend -> viewModel.playFusionBlend(blend) },
          onTrackClick = { track, queue -> viewModel.playTrack(track, queue) },
          onGenerateBlend = { a, b, name -> viewModel.generateCustomBlend(a, b, name) },
          onSaveBlendAsPlaylist = { name, desc, tracks ->
            viewModel.createPlaylist(name, desc)
            tracks.forEach { track ->
              catalogTracks.find { it.id == track.id }?.let {
                // Persistent
              }
            }
          }
        )

        NavDestination.Library -> LibraryScreen(
          playlists = playlists,
          likedTracks = likedTracks,
          recentHistory = recentHistory,
          localTracks = localTracks,
          selectedPlaylist = selectedPlaylist,
          selectedPlaylistTracks = selectedPlaylistTracks,
          currentPlayingTrack = playerState.currentTrack,
          isPlaying = playerState.isPlaying,
          onSelectPlaylist = { viewModel.selectPlaylist(it) },
          onTrackClick = { track, queue -> viewModel.playTrack(track, queue) },
          onPlayAll = { tracks ->
            if (tracks.isNotEmpty()) viewModel.playQueue(tracks, startIndex = 0)
          },
          onShufflePlay = { tracks ->
            if (tracks.isNotEmpty()) viewModel.shufflePlay(tracks)
          },
          onToggleLike = { viewModel.toggleLike(it) },
          onCreatePlaylistDialog = {
            viewModel.createPlaylist("My SpotiFusion Mix", "Created from Library")
          },
          onDeletePlaylist = { playlistId -> viewModel.deletePlaylist(playlistId) },
          onScanLocalTracks = { viewModel.scanLocalMusic() },
          onRemoveTrackFromPlaylist = { playlistId, trackId ->
            viewModel.removeTrackFromPlaylist(playlistId, trackId)
          }
        )

        NavDestination.Equalizer -> EqualizerScreen(
          equalizerState = equalizerState,
          onToggleEnabled = { viewModel.setEqualizerEnabled(it) },
          onSelectPreset = { viewModel.setEqualizerPreset(it) },
          onBandGainChanged = { band, gain -> viewModel.setEqualizerBandGain(band, gain) },
          onBassBoostChanged = { viewModel.setBassBoost(it) },
          onVirtualizerChanged = { viewModel.setVirtualizer(it) },
          onBack = { currentDestination = NavDestination.Home }
        )
      }

      // Full-Screen Expandable Now Playing Sheet
      if (isNowPlayingExpanded && playerState.currentTrack != null) {
        NowPlayingSheet(
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
          onTogglePlayPause = { viewModel.togglePlayPause() },
          onNext = { viewModel.nextTrack() },
          onPrevious = { viewModel.previousTrack() },
          onSeek = { viewModel.seekTo(it) },
          onToggleShuffle = { viewModel.toggleShuffle() },
          onCycleRepeat = { viewModel.cycleRepeatMode() },
          onToggleLike = { viewModel.toggleLike(it) },
          onSetVolume = { viewModel.setVolume(it) },
          onOpenEqualizer = {
            viewModel.setNowPlayingExpanded(false)
            currentDestination = NavDestination.Equalizer
          },
          onOpenSleepTimer = {
            viewModel.setSleepTimerOpen(true)
          },
          onAddToPlaylist = { track ->
            viewModel.showAddToPlaylistDialog(track)
          }
        )
      }

      // Add To Playlist Dialog
      if (trackForPlaylistDialog != null) {
        AddToPlaylistDialog(
          track = trackForPlaylistDialog,
          playlists = playlists,
          onDismiss = { viewModel.showAddToPlaylistDialog(null) },
          onAddToPlaylist = { playlistId, track ->
            viewModel.addTrackToPlaylist(playlistId, track)
            viewModel.showAddToPlaylistDialog(null)
          },
          onCreatePlaylist = { name, desc, track ->
            viewModel.createPlaylist(name, desc)
            viewModel.showAddToPlaylistDialog(null)
          }
        )
      }

      // Settings Sheet Modal
      if (isSettingsOpen) {
        SettingsSheet(
          settings = settingsState,
          onUpdateAudioQuality = { viewModel.updateAudioQuality(it) },
          onUpdateCrossfade = { viewModel.updateCrossfade(it) },
          onToggleShakeToSkip = { viewModel.toggleShakeToSkip(it) },
          onToggleLyricsAutoScroll = { viewModel.toggleLyricsAutoScroll(it) },
          onToggleVisualizer60fps = { viewModel.toggleVisualizer60fps(it) },
          onClearCache = { viewModel.clearCache() },
          onDismiss = { viewModel.setSettingsOpen(false) }
        )
      }

      // Sleep Timer Dialog
      if (isSleepTimerOpen) {
        SleepTimerDialog(
          activeMinutes = sleepTimerMinutes,
          remainingSec = sleepTimerRemainingSec,
          onSetTimer = { viewModel.setSleepTimer(it) },
          onDismiss = { viewModel.setSleepTimerOpen(false) }
        )
      }
    }
  }
}
