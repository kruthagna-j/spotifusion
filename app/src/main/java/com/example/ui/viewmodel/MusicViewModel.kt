package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.data.OfflineDownloadManager
import com.example.data.remote.StreamResolver
import com.example.data.local.SpotiFusionDatabase
import com.example.model.EqualizerState
import com.example.model.FusionBlend
import com.example.model.Playlist
import com.example.model.PlayerState
import com.example.model.SettingsState
import com.example.model.SyncedLyricLine
import com.example.model.Track
import com.example.service.PlaybackController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
  private val database = SpotiFusionDatabase.getDatabase(application)
  val repository = MusicRepository(database.musicDao(), application)
  val playerState: StateFlow<PlayerState> = PlaybackController.playerState
  val sleepTimerMinutes: StateFlow<Int?> = PlaybackController.sleepTimerMinutes
  val sleepTimerRemainingSec: StateFlow<Int> = PlaybackController.sleepTimerRemainingSec
  private val _homeTracks = MutableStateFlow<List<Track>>(repository.catalogTracks)
  val catalogTracks: StateFlow<List<Track>> = _homeTracks.asStateFlow()
  val likedTracks: StateFlow<List<Track>> = repository.getLikedTracks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.curatedPlaylists)
  val recentHistory: StateFlow<List<Track>> = repository.getRecentHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
  val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()
  private val _fusionBlends = MutableStateFlow(repository.presetFusionBlends)
  val fusionBlends: StateFlow<List<FusionBlend>> = _fusionBlends.asStateFlow()
  private val _equalizerState = MutableStateFlow(EqualizerState())
  val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()
  private val preferences = application.getSharedPreferences("spotifusion_preferences", Context.MODE_PRIVATE)
  private val _settingsState = MutableStateFlow(SettingsState(
    audioQuality = preferences.getString("audio_quality", "High (320kbps)") ?: "High (320kbps)",
    crossfadeSec = preferences.getInt("crossfade_sec", 3),
    shakeToSkipEnabled = preferences.getBoolean("shake_skip", true),
    lyricsAutoScroll = preferences.getBoolean("lyrics_auto_scroll", true),
    visualizer60fps = preferences.getBoolean("visualizer_high_fps", true),
    offlineCacheSizeMb = cacheSizeMb(application),
    notificationsEnabled = preferences.getBoolean("download_notifications", true),
    darkTheme = preferences.getBoolean("dark_theme", true)
  ))
  val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()
  private fun cacheSizeMb(context: Context): Float = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() } / 1024f / 1024f
  private val _downloadedTrackIds = MutableStateFlow(OfflineDownloadManager.downloadedIds(application))
  val downloadedTrackIds: StateFlow<Set<String>> = _downloadedTrackIds.asStateFlow()
  private val _currentSyncedLyrics = MutableStateFlow<List<SyncedLyricLine>>(emptyList())
  val currentSyncedLyrics: StateFlow<List<SyncedLyricLine>> = _currentSyncedLyrics.asStateFlow()
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
  private val _selectedGenre = MutableStateFlow("All")
  val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()
  private val _searchResults = MutableStateFlow<List<Track>>(repository.catalogTracks)
  val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()
  private var searchDebounceJob: Job? = null
  private val _isNowPlayingExpanded = MutableStateFlow(false)
  val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()
  private val _isSettingsOpen = MutableStateFlow(false)
  val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()
  private val _isSleepTimerOpen = MutableStateFlow(false)
  val isSleepTimerOpen: StateFlow<Boolean> = _isSleepTimerOpen.asStateFlow()
  private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
  val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()
  val selectedPlaylistTracks: StateFlow<List<Track>> = _selectedPlaylist.flatMapLatest { playlist -> if (playlist != null) repository.getTracksForPlaylist(playlist.id) else kotlinx.coroutines.flow.flowOf(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
  private val _trackForPlaylistDialog = MutableStateFlow<Track?>(null)
  val trackForPlaylistDialog: StateFlow<Track?> = _trackForPlaylistDialog.asStateFlow()

  init {
    val savedGains = preferences.getString("eq_gains", null)?.split(",")?.mapNotNull { it.toFloatOrNull() }
    val savedEq = EqualizerState(
      isEnabled = preferences.getBoolean("eq_enabled", true),
      activePreset = preferences.getString("eq_preset", "Bass Boost") ?: "Bass Boost",
      bandGains = if (savedGains?.size == 5) savedGains else EqualizerState().bandGains,
      bassBoost = preferences.getFloat("eq_bass", 0.75f),
      virtualizer = preferences.getFloat("eq_virtualizer", 0.5f),
      loudness = preferences.getFloat("eq_loudness", 0.65f)
    )
    _equalizerState.value = savedEq
    PlaybackController.setEqualizerState(savedEq)
    PlaybackController.setAudioQuality(_settingsState.value.audioQuality)
    PlaybackController.setCrossfadeSeconds(_settingsState.value.crossfadeSec)
    viewModelScope.launch {
      val liveTracks = repository.fetchDiscoverTracks()
      if (liveTracks.isNotEmpty()) { _homeTracks.value = liveTracks; _searchResults.value = liveTracks }
    }
    viewModelScope.launch {
      playerState.collect { state ->
        val currentTrack = state.currentTrack
        if (currentTrack != null) {
          launch { _currentSyncedLyrics.value = repository.fetchSyncedLyrics(currentTrack) }
          if (state.isPlaying && state.currentPositionSec >= 2) repository.recordHistory(currentTrack)
        } else _currentSyncedLyrics.value = emptyList()
      }
    }
    scanLocalMusic()
  }

  fun scanLocalMusic() { viewModelScope.launch { val found = repository.scanLocalAudioFiles(); _localTracks.value = found; if (found.isNotEmpty()) updateSearchResults(_searchQuery.value, _selectedGenre.value) } }
  fun setNowPlayingExpanded(expanded: Boolean) { _isNowPlayingExpanded.value = expanded }
  fun setSettingsOpen(open: Boolean) { _isSettingsOpen.value = open }
  fun setSleepTimerOpen(open: Boolean) { _isSleepTimerOpen.value = open }
  fun setSleepTimer(minutes: Int?) { PlaybackController.setSleepTimer(minutes) }
  fun selectPlaylist(playlist: Playlist?) { _selectedPlaylist.value = playlist }
  fun showAddToPlaylistDialog(track: Track?) { _trackForPlaylistDialog.value = track }
  fun playTrack(track: Track, queue: List<Track> = _homeTracks.value) { val targetQueue = if (queue.isEmpty()) _homeTracks.value else queue; val index = targetQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0); PlaybackController.setQueue(targetQueue, index, true); viewModelScope.launch { repository.recordHistory(track) } }
  fun playQueue(tracks: List<Track>, startIndex: Int = 0) { if (tracks.isEmpty()) return; PlaybackController.setQueue(tracks, startIndex, true); viewModelScope.launch { tracks.getOrNull(startIndex)?.let { repository.recordHistory(it) } } }
  fun shufflePlay(tracks: List<Track>) { if (tracks.isEmpty()) return; val shuffled = tracks.shuffled(); PlaybackController.setQueue(shuffled, 0, true); viewModelScope.launch { shuffled.firstOrNull()?.let { repository.recordHistory(it) } } }
  fun togglePlayPause() = PlaybackController.togglePlayPause()
  fun nextTrack() = PlaybackController.nextTrack()
  fun previousTrack() = PlaybackController.previousTrack()
  fun seekTo(positionSec: Int) = PlaybackController.seekTo(positionSec)
  fun toggleShuffle() = PlaybackController.toggleShuffle()
  fun cycleRepeatMode() = PlaybackController.cycleRepeatMode()
  fun setVolume(vol: Float) = PlaybackController.setVolume(vol)
  fun toggleLike(track: Track) { viewModelScope.launch { repository.toggleLike(track) } }
  fun isTrackLiked(trackId: String): Boolean = likedTracks.value.any { it.id == trackId }
  fun createPlaylist(title: String, description: String = "") { viewModelScope.launch { val newId = repository.createCustomPlaylist(title, description); playlists.value.find { it.id == newId }?.let { _selectedPlaylist.value = it } } }
  fun createPlaylistAndAddTrack(title: String, description: String = "", track: Track) {
    viewModelScope.launch {
      val playlistId = repository.createCustomPlaylist(title, description)
      repository.addTrackToPlaylist(playlistId, track)
      playlists.value.find { it.id == playlistId }?.let { _selectedPlaylist.value = it }
    }
  }
  fun addTrackToPlaylist(playlistId: String, track: Track) { if (playlistId.isBlank()) return; viewModelScope.launch { repository.addTrackToPlaylist(playlistId, track) } }
  fun removeTrackFromPlaylist(playlistId: String, trackId: String) { viewModelScope.launch { repository.removeTrackFromPlaylist(playlistId, trackId) } }
  fun deletePlaylist(playlistId: String) { viewModelScope.launch { repository.deletePlaylist(playlistId); if (_selectedPlaylist.value?.id == playlistId) _selectedPlaylist.value = null } }
  fun playFusionBlend(blend: FusionBlend) { if (blend.tracks.isNotEmpty()) playQueue(blend.tracks, 0) }
  fun generateCustomBlend(genreA: String, genreB: String, name: String = "") { val pool = _homeTracks.value + _localTracks.value; _fusionBlends.value = listOf(repository.createDynamicFusionBlend(genreA, genreB, name, pool)) + _fusionBlends.value }

  private fun persistEqualizer() { val eq = _equalizerState.value; preferences.edit().putBoolean("eq_enabled", eq.isEnabled).putString("eq_preset", eq.activePreset).putString("eq_gains", eq.bandGains.joinToString(",")).putFloat("eq_bass", eq.bassBoost).putFloat("eq_virtualizer", eq.virtualizer).putFloat("eq_loudness", eq.loudness).apply() }
  fun setEqualizerEnabled(enabled: Boolean) { _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled); persistEqualizer(); PlaybackController.setEqualizerState(_equalizerState.value) }
  fun setEqualizerPreset(presetName: String) { _equalizerState.value = _equalizerState.value.setPreset(presetName); persistEqualizer(); PlaybackController.setEqualizerState(_equalizerState.value) }
  fun setEqualizerBandGain(bandIndex: Int, gainDb: Float) { _equalizerState.value = _equalizerState.value.updateBandGain(bandIndex, gainDb); persistEqualizer(); PlaybackController.setEqualizerState(_equalizerState.value) }
  fun setBassBoost(value: Float) { _equalizerState.value = _equalizerState.value.copy(bassBoost = value); persistEqualizer(); PlaybackController.setEqualizerState(_equalizerState.value) }
  fun setVirtualizer(value: Float) { _equalizerState.value = _equalizerState.value.copy(virtualizer = value); persistEqualizer(); PlaybackController.setEqualizerState(_equalizerState.value) }

  private fun updateSettings(updated: SettingsState) { _settingsState.value = updated.copy(offlineCacheSizeMb = cacheSizeMb(getApplication())); preferences.edit().putString("audio_quality", updated.audioQuality).putInt("crossfade_sec", updated.crossfadeSec).putBoolean("shake_skip", updated.shakeToSkipEnabled).putBoolean("lyrics_auto_scroll", updated.lyricsAutoScroll).putBoolean("visualizer_high_fps", updated.visualizer60fps).putBoolean("download_notifications", updated.notificationsEnabled).putBoolean("dark_theme", updated.darkTheme).apply() }
  fun updateAudioQuality(quality: String) { updateSettings(_settingsState.value.copy(audioQuality = quality)); PlaybackController.setAudioQuality(quality) }
  fun updateCrossfade(sec: Int) { updateSettings(_settingsState.value.copy(crossfadeSec = sec)); PlaybackController.setCrossfadeSeconds(sec) }
  fun toggleShakeToSkip(enabled: Boolean) = updateSettings(_settingsState.value.copy(shakeToSkipEnabled = enabled))
  fun toggleLyricsAutoScroll(enabled: Boolean) = updateSettings(_settingsState.value.copy(lyricsAutoScroll = enabled))
  fun toggleVisualizer60fps(enabled: Boolean) = updateSettings(_settingsState.value.copy(visualizer60fps = enabled))
  fun toggleNotifications(enabled: Boolean) { updateSettings(_settingsState.value.copy(notificationsEnabled = enabled)); if (!enabled) (getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancelAll() }
  fun clearCache() { val cacheDir = getApplication<Application>().cacheDir; cacheDir.deleteRecursively(); cacheDir.mkdirs(); StreamResolver.clearCache(); updateSettings(_settingsState.value.copy(offlineCacheSizeMb = cacheSizeMb(getApplication()))) }
  fun downloadTrack(track: Track) { viewModelScope.launch { val file = OfflineDownloadManager.download(getApplication(), track, _settingsState.value.audioQuality); if (file != null) { _downloadedTrackIds.value = OfflineDownloadManager.downloadedIds(getApplication()); updateSettings(_settingsState.value); if (_settingsState.value.notificationsEnabled) postDownloadNotification(track) } } }
  private fun postDownloadNotification(track: Track) { val context = getApplication<Application>(); val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; val channelId = "spotifusion_downloads"; if (android.os.Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(channelId, "Offline downloads", NotificationManager.IMPORTANCE_LOW)); manager.notify(track.id.hashCode(), NotificationCompat.Builder(context, channelId).setSmallIcon(com.example.R.drawable.ic_launcher_foreground).setContentTitle("Downloaded for offline playback").setContentText("${track.title} · ${track.artist}").setAutoCancel(true).build()) }
  fun removeDownloadedTrack(track: Track) { OfflineDownloadManager.delete(getApplication(), track.id); _downloadedTrackIds.value = OfflineDownloadManager.downloadedIds(getApplication()); updateSettings(_settingsState.value) }
  fun isTrackDownloaded(trackId: String): Boolean = trackId in _downloadedTrackIds.value
  fun setDarkTheme(enabled: Boolean) = updateSettings(_settingsState.value.copy(darkTheme = enabled))
  fun onSearchQueryChanged(query: String) { _searchQuery.value = query; updateSearchResults(query, _selectedGenre.value); searchDebounceJob?.cancel(); if (query.trim().length >= 2) searchDebounceJob = viewModelScope.launch { delay(350); val onlineResults = repository.searchOnline(query, "all"); if (onlineResults.isNotEmpty()) _searchResults.value = (onlineResults + _localTracks.value).distinctBy { it.id } } }
  fun onGenreSelected(genre: String) { _selectedGenre.value = genre; updateSearchResults(_searchQuery.value, genre) }
  private fun updateSearchResults(query: String, genre: String) { _searchResults.value = repository.search(query, genre, _homeTracks.value + _localTracks.value) }
}