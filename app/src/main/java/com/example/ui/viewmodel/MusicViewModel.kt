package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
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

  val likedTracks: StateFlow<List<Track>> = repository.getLikedTracks()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val playlists: StateFlow<List<Playlist>> = repository.getAllPlaylists()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), repository.curatedPlaylists)

  val recentHistory: StateFlow<List<Track>> = repository.getRecentHistory()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
  val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

  private val _fusionBlends = MutableStateFlow(repository.presetFusionBlends)
  val fusionBlends: StateFlow<List<FusionBlend>> = _fusionBlends.asStateFlow()

  private val _equalizerState = MutableStateFlow(EqualizerState())
  val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

  private val _settingsState = MutableStateFlow(SettingsState())
  val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

  // Synced lyrics for current playing track
  private val _currentSyncedLyrics = MutableStateFlow<List<SyncedLyricLine>>(emptyList())
  val currentSyncedLyrics: StateFlow<List<SyncedLyricLine>> = _currentSyncedLyrics.asStateFlow()

  // Search state
  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _selectedGenre = MutableStateFlow("All")
  val selectedGenre: StateFlow<String> = _selectedGenre.asStateFlow()

  private val _searchResults = MutableStateFlow<List<Track>>(repository.catalogTracks)
  val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

  private var searchDebounceJob: Job? = null

  // Navigation & Dialog modals
  private val _isNowPlayingExpanded = MutableStateFlow(false)
  val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

  private val _isSettingsOpen = MutableStateFlow(false)
  val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

  private val _isSleepTimerOpen = MutableStateFlow(false)
  val isSleepTimerOpen: StateFlow<Boolean> = _isSleepTimerOpen.asStateFlow()

  private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
  val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

  val selectedPlaylistTracks: StateFlow<List<Track>> = _selectedPlaylist.flatMapLatest { playlist ->
    if (playlist != null) {
      repository.getTracksForPlaylist(playlist.id)
    } else {
      kotlinx.coroutines.flow.flowOf(emptyList())
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _trackForPlaylistDialog = MutableStateFlow<Track?>(null)
  val trackForPlaylistDialog: StateFlow<Track?> = _trackForPlaylistDialog.asStateFlow()

  init {
    PlaybackController.setEqualizerState(_equalizerState.value)

    // Load Live Discovery Tracks from backend
    viewModelScope.launch {
      val liveTracks = repository.fetchDiscoverTracks()
      if (liveTracks.isNotEmpty()) {
        _homeTracks.value = liveTracks
        _searchResults.value = liveTracks
      }
    }

    // Listen to current playing track to fetch synced lyrics & record history
    viewModelScope.launch {
      playerState.collect { state ->
        val currentTrack = state.currentTrack
        if (currentTrack != null) {
          launch {
            val lyrics = repository.fetchSyncedLyrics(currentTrack)
            _currentSyncedLyrics.value = lyrics
          }
          if (state.isPlaying && state.currentPositionSec >= 2) {
            repository.recordHistory(currentTrack)
          }
        } else {
          _currentSyncedLyrics.value = emptyList()
        }
      }
    }

    // Initial local scan if permissions present
    scanLocalMusic()
  }

  fun scanLocalMusic() {
    viewModelScope.launch {
      val found = repository.scanLocalAudioFiles()
      _localTracks.value = found
      if (found.isNotEmpty()) {
        updateSearchResults(_searchQuery.value, _selectedGenre.value)
      }
    }
  }

  fun setNowPlayingExpanded(expanded: Boolean) {
    _isNowPlayingExpanded.value = expanded
  }

  fun setSettingsOpen(open: Boolean) {
    _isSettingsOpen.value = open
  }

  fun setSleepTimerOpen(open: Boolean) {
    _isSleepTimerOpen.value = open
  }

  fun setSleepTimer(minutes: Int?) {
    PlaybackController.setSleepTimer(minutes)
  }

  fun selectPlaylist(playlist: Playlist?) {
    _selectedPlaylist.value = playlist
  }

  fun showAddToPlaylistDialog(track: Track?) {
    _trackForPlaylistDialog.value = track
  }

  fun playTrack(track: Track, queue: List<Track> = _homeTracks.value) {
    val targetQueue = if (queue.isEmpty()) _homeTracks.value else queue
    val index = targetQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
    PlaybackController.setQueue(targetQueue, startIndex = index, autoPlay = true)
    viewModelScope.launch {
      repository.recordHistory(track)
    }
  }

  fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
    if (tracks.isEmpty()) return
    PlaybackController.setQueue(tracks, startIndex = startIndex, autoPlay = true)
    viewModelScope.launch {
      tracks.getOrNull(startIndex)?.let { repository.recordHistory(it) }
    }
  }

  fun shufflePlay(tracks: List<Track>) {
    if (tracks.isEmpty()) return
    val shuffled = tracks.shuffled()
    PlaybackController.setQueue(shuffled, startIndex = 0, autoPlay = true)
    viewModelScope.launch {
      shuffled.firstOrNull()?.let { repository.recordHistory(it) }
    }
  }

  fun togglePlayPause() {
    PlaybackController.togglePlayPause()
  }

  fun nextTrack() {
    PlaybackController.nextTrack()
  }

  fun previousTrack() {
    PlaybackController.previousTrack()
  }

  fun seekTo(positionSec: Int) {
    PlaybackController.seekTo(positionSec)
  }

  fun toggleShuffle() {
    PlaybackController.toggleShuffle()
  }

  fun cycleRepeatMode() {
    PlaybackController.cycleRepeatMode()
  }

  fun setVolume(vol: Float) {
    PlaybackController.setVolume(vol)
  }

  fun toggleLike(track: Track) {
    viewModelScope.launch {
      repository.toggleLike(track)
    }
  }

  fun isTrackLiked(trackId: String): Boolean {
    return likedTracks.value.any { it.id == trackId }
  }

  // Playlist Management
  fun createPlaylist(title: String, description: String = "") {
    viewModelScope.launch {
      val newId = repository.createCustomPlaylist(title, description)
      val created = playlists.value.find { it.id == newId }
      if (created != null) {
        _selectedPlaylist.value = created
      }
    }
  }

  fun addTrackToPlaylist(playlistId: String, track: Track) {
    viewModelScope.launch {
      repository.addTrackToPlaylist(playlistId, track)
    }
  }

  fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
    viewModelScope.launch {
      repository.removeTrackFromPlaylist(playlistId, trackId)
    }
  }

  fun deletePlaylist(playlistId: String) {
    viewModelScope.launch {
      repository.deletePlaylist(playlistId)
      if (_selectedPlaylist.value?.id == playlistId) {
        _selectedPlaylist.value = null
      }
    }
  }

  // Fusion Blend Actions
  fun playFusionBlend(blend: FusionBlend) {
    if (blend.tracks.isNotEmpty()) {
      playQueue(blend.tracks, startIndex = 0)
    }
  }

  fun generateCustomBlend(genreA: String, genreB: String, name: String = "") {
    val pool = _homeTracks.value + _localTracks.value
    val newBlend = repository.createDynamicFusionBlend(genreA, genreB, name, pool)
    _fusionBlends.value = listOf(newBlend) + _fusionBlends.value
  }

  // Equalizer Controls
  fun setEqualizerEnabled(enabled: Boolean) {
    _equalizerState.value = _equalizerState.value.copy(isEnabled = enabled)
    PlaybackController.setEqualizerState(_equalizerState.value)
  }

  fun setEqualizerPreset(presetName: String) {
    _equalizerState.value = _equalizerState.value.setPreset(presetName)
    PlaybackController.setEqualizerState(_equalizerState.value)
  }

  fun setEqualizerBandGain(bandIndex: Int, gainDb: Float) {
    _equalizerState.value = _equalizerState.value.updateBandGain(bandIndex, gainDb)
    PlaybackController.setEqualizerState(_equalizerState.value)
  }

  fun setBassBoost(value: Float) {
    _equalizerState.value = _equalizerState.value.copy(bassBoost = value)
    PlaybackController.setEqualizerState(_equalizerState.value)
  }

  fun setVirtualizer(value: Float) {
    _equalizerState.value = _equalizerState.value.copy(virtualizer = value)
    PlaybackController.setEqualizerState(_equalizerState.value)
  }

  // Settings Actions
  fun updateAudioQuality(quality: String) {
    _settingsState.value = _settingsState.value.copy(audioQuality = quality)
  }

  fun updateCrossfade(sec: Int) {
    _settingsState.value = _settingsState.value.copy(crossfadeSec = sec)
  }

  fun toggleShakeToSkip(enabled: Boolean) {
    _settingsState.value = _settingsState.value.copy(shakeToSkipEnabled = enabled)
  }

  fun toggleLyricsAutoScroll(enabled: Boolean) {
    _settingsState.value = _settingsState.value.copy(lyricsAutoScroll = enabled)
  }

  fun toggleVisualizer60fps(enabled: Boolean) {
    _settingsState.value = _settingsState.value.copy(visualizer60fps = enabled)
  }

  fun clearCache() {
    _settingsState.value = _settingsState.value.copy(offlineCacheSizeMb = 0.0f)
  }

  // Search with debounce & live online lookup
  fun onSearchQueryChanged(query: String) {
    _searchQuery.value = query
    updateSearchResults(query, _selectedGenre.value)

    searchDebounceJob?.cancel()
    if (query.trim().length >= 2) {
      searchDebounceJob = viewModelScope.launch {
        delay(350)
        val onlineResults = repository.searchOnline(query, category = "all")
        if (onlineResults.isNotEmpty()) {
          val merged = (onlineResults + _localTracks.value).distinctBy { it.id }
          _searchResults.value = merged
        }
      }
    }
  }

  fun onGenreSelected(genre: String) {
    _selectedGenre.value = genre
    updateSearchResults(_searchQuery.value, genre)
  }

  private fun updateSearchResults(query: String, genre: String) {
    val pool = _homeTracks.value + _localTracks.value
    _searchResults.value = repository.search(query, genre, pool)
  }

  override fun onCleared() {
    super.onCleared()
  }
}
