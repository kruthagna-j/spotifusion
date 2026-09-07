package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Playlist
import com.example.model.Track
import com.example.ui.theme.BgDark
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
  playlists: List<Playlist>,
  likedTracks: List<Track>,
  recentHistory: List<Track>,
  localTracks: List<Track> = emptyList(),
  selectedPlaylist: Playlist?,
  selectedPlaylistTracks: List<Track>,
  currentPlayingTrack: Track?,
  isPlaying: Boolean,
  onSelectPlaylist: (Playlist?) -> Unit,
  onTrackClick: (Track, List<Track>) -> Unit,
  onPlayAll: (List<Track>) -> Unit,
  onShufflePlay: (List<Track>) -> Unit,
  onToggleLike: (Track) -> Unit,
  onCreatePlaylistDialog: () -> Unit,
  onDeletePlaylist: (String) -> Unit,
  onScanLocalTracks: () -> Unit = {},
  onRemoveTrackFromPlaylist: (playlistId: String, trackId: String) -> Unit
) {
  var currentTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Playlists", "Liked Songs", "History", "Local Files")

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BgDark)
      .testTag("library_screen")
  ) {
    if (selectedPlaylist != null) {
      // Playlist Detail View
      PlaylistDetailView(
        playlist = selectedPlaylist,
        tracks = selectedPlaylistTracks,
        currentPlayingTrack = currentPlayingTrack,
        isPlaying = isPlaying,
        onBack = { onSelectPlaylist(null) },
        onTrackClick = { track -> onTrackClick(track, selectedPlaylistTracks) },
        onPlayAll = { onPlayAll(selectedPlaylistTracks) },
        onShufflePlay = { onShufflePlay(selectedPlaylistTracks) },
        onDeletePlaylist = { onDeletePlaylist(selectedPlaylist.id) },
        onRemoveTrack = { trackId -> onRemoveTrackFromPlaylist(selectedPlaylist.id, trackId) }
      )
    } else {
      // Main Library Tabs
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Your Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = TextPrimary
          )

          IconButton(
            onClick = onCreatePlaylistDialog,
            modifier = Modifier.testTag("create_playlist_fab")
          ) {
            Icon(Icons.Default.Add, contentDescription = "Create Playlist", tint = SpotifyGreen, modifier = Modifier.size(28.dp))
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TabRow(
          selectedTabIndex = currentTab,
          containerColor = BgDark,
          contentColor = SpotifyGreen,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              Modifier.tabIndicatorOffset(tabPositions[currentTab]),
              color = SpotifyGreen,
              height = 3.dp
            )
          },
          divider = {
            Box(
              Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(GlassBorder)
            )
          }
        ) {
          tabs.forEachIndexed { index, title ->
            Tab(
              selected = currentTab == index,
              onClick = { currentTab = index },
              text = {
                Text(
                  text = title,
                  fontWeight = if (currentTab == index) FontWeight.Bold else FontWeight.Normal,
                  color = if (currentTab == index) SpotifyGreen else TextSecondary,
                  fontSize = 13.sp
                )
              }
            )
          }
        }
      }

      Box(
        modifier = Modifier.fillMaxSize()
      ) {
        when (currentTab) {
          0 -> PlaylistsTab(
            playlists = playlists,
            onSelectPlaylist = onSelectPlaylist,
            onCreatePlaylist = onCreatePlaylistDialog
          )
          1 -> LikedSongsTab(
            likedTracks = likedTracks,
            currentPlayingTrack = currentPlayingTrack,
            onTrackClick = onTrackClick,
            onPlayAll = onPlayAll,
            onShufflePlay = onShufflePlay,
            onToggleLike = onToggleLike
          )
          2 -> HistoryTab(
            historyTracks = recentHistory,
            currentPlayingTrack = currentPlayingTrack,
            onTrackClick = onTrackClick
          )
          3 -> LocalTracksTab(
            localTracks = localTracks,
            currentPlayingTrack = currentPlayingTrack,
            onTrackClick = onTrackClick,
            onPlayAll = onPlayAll,
            onShufflePlay = onShufflePlay,
            onScan = onScanLocalTracks
          )
        }
      }
    }
  }
}

@Composable
fun LocalTracksTab(
  localTracks: List<Track>,
  currentPlayingTrack: Track?,
  onTrackClick: (Track, List<Track>) -> Unit,
  onPlayAll: (List<Track>) -> Unit,
  onShufflePlay: (List<Track>) -> Unit,
  onScan: () -> Unit
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "${localTracks.size} Device Audio Files",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary
      )
      Button(
        onClick = onScan,
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Icon(Icons.Default.Sync, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Scan Files", color = TextPrimary, fontSize = 12.sp)
      }
    }

    if (localTracks.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextMuted, modifier = Modifier.size(54.dp))
          Spacer(modifier = Modifier.height(12.dp))
          Text("No Local Audio Files Found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
          Spacer(modifier = Modifier.height(4.dp))
          Text("Tap 'Scan Files' to search your device storage", style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
      }
    } else {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = { onPlayAll(localTracks) },
          colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Play All", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Button(
          onClick = { onShufflePlay(localTracks) },
          colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.Default.Shuffle, contentDescription = null, tint = TextPrimary)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Shuffle", color = TextPrimary, fontWeight = FontWeight.Bold)
        }
      }

      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(localTracks, key = { it.id }) { track ->
          val isCurrent = currentPlayingTrack?.id == track.id
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(if (isCurrent) SpotifyGreen.copy(alpha = 0.12f) else SurfaceCard)
              .border(1.dp, if (isCurrent) SpotifyGreen.copy(alpha = 0.3f) else GlassBorder, RoundedCornerShape(12.dp))
              .clickable { onTrackClick(track, localTracks) }
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceElevated),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(track.title, fontWeight = FontWeight.Bold, color = if (isCurrent) SpotifyGreen else TextPrimary, maxLines = 1)
              Text("${track.artist} • ${track.formattedDuration}", color = TextSecondary, fontSize = 12.sp, maxLines = 1)
            }
          }
        }
      }
    }
  }
}

@Composable
fun PlaylistsTab(
  playlists: List<Playlist>,
  onSelectPlaylist: (Playlist) -> Unit,
  onCreatePlaylist: () -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Add Playlist Card
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(SurfaceCard)
          .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
          .clickable(onClick = onCreatePlaylist)
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SpotifyGreen.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
          Text("Create New Playlist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
          Text("Add your favorite tracks and blends", style = MaterialTheme.typography.bodySmall, color = TextMuted, fontSize = 12.sp)
        }
      }
    }

    items(playlists, key = { it.id }) { playlist ->
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(SurfaceCard)
          .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
          .clickable { onSelectPlaylist(playlist) }
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
              Brush.linearGradient(
                listOf(Color(playlist.coverGradientStart), Color(playlist.coverGradientEnd))
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(28.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = playlist.title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = if (playlist.isCustom) "Custom Playlist" else playlist.description,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        Icon(Icons.Default.PlayArrow, contentDescription = "Open", tint = SpotifyGreen, modifier = Modifier.size(20.dp))
      }
    }
  }
}

@Composable
fun LikedSongsTab(
  likedTracks: List<Track>,
  currentPlayingTrack: Track?,
  onTrackClick: (Track, List<Track>) -> Unit,
  onPlayAll: (List<Track>) -> Unit,
  onShufflePlay: (List<Track>) -> Unit,
  onToggleLike: (Track) -> Unit
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp)
  ) {
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(
            Brush.linearGradient(
              listOf(Color(0xFF4A148C), SpotifyGreen)
            )
          )
          .padding(20.dp)
      ) {
        Column {
          Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
          Spacer(modifier = Modifier.height(12.dp))
          Text("Liked Songs", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
          Text("${likedTracks.size} songs saved", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))

          Spacer(modifier = Modifier.height(16.dp))

          if (likedTracks.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = { onPlayAll(likedTracks) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play", color = SpotifyGreen, fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = { onShufflePlay(likedTracks) },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp)
              ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
    }

    if (likedTracks.isEmpty()) {
      item {
        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text("No liked songs yet", fontWeight = FontWeight.Bold, color = TextSecondary)
            Text("Tap the heart on any track to save it here", color = TextMuted, fontSize = 12.sp)
          }
        }
      }
    } else {
      items(likedTracks, key = { it.id }) { track ->
        val isCurrent = currentPlayingTrack?.id == track.id
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onTrackClick(track, likedTracks) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
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
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(20.dp).align(Alignment.Center))
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
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
                fontSize = 11.sp
              )
            }
          }

          IconButton(onClick = { onToggleLike(track) }) {
            Icon(Icons.Default.Favorite, contentDescription = "Unlike", tint = SpotifyGreen, modifier = Modifier.size(20.dp))
          }
        }
      }
    }
  }
}

@Composable
fun HistoryTab(
  historyTracks: List<Track>,
  currentPlayingTrack: Track?,
  onTrackClick: (Track, List<Track>) -> Unit
) {
  if (historyTracks.isEmpty()) {
    Box(modifier = Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.History, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text("No playback history yet", fontWeight = FontWeight.Bold, color = TextSecondary)
        Text("Play your first song to see listening history", color = TextMuted, fontSize = 12.sp)
      }
    }
  } else {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(16.dp)
    ) {
      items(historyTracks) { track ->
        val isCurrent = currentPlayingTrack?.id == track.id
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onTrackClick(track, historyTracks) }
            .padding(vertical = 8.dp, horizontal = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
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
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(20.dp).align(Alignment.Center))
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) SpotifyGreen else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${track.artist} • ${track.genre}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
              )
            }
          }

          Text(
            text = track.formattedDuration,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
          )
        }
      }
    }
  }
}

@Composable
fun PlaylistDetailView(
  playlist: Playlist,
  tracks: List<Track>,
  currentPlayingTrack: Track?,
  isPlaying: Boolean,
  onBack: () -> Unit,
  onTrackClick: (Track) -> Unit,
  onPlayAll: () -> Unit,
  onShufflePlay: () -> Unit,
  onDeletePlaylist: () -> Unit,
  onRemoveTrack: (String) -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(bottom = 100.dp)
  ) {
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(
            Brush.verticalGradient(
              listOf(
                Color(playlist.coverGradientStart),
                Color(playlist.coverGradientEnd).copy(alpha = 0.6f),
                BgDark
              )
            )
          )
          .padding(horizontal = 16.dp, vertical = 12.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = onBack) {
              Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            if (playlist.isCustom) {
              IconButton(onClick = onDeletePlaylist) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = Color.White.copy(alpha = 0.8f))
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.3f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.QueueMusic, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
              Text(
                text = playlist.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = Color.White
              )
              Text(
                text = playlist.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${tracks.size} tracks",
                style = MaterialTheme.typography.labelSmall,
                color = SpotifyGreen,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          if (tracks.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              Button(
                onClick = onPlayAll,
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                shape = RoundedCornerShape(20.dp)
              ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Play All", color = Color.White, fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = onShufflePlay,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(20.dp)
              ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Shuffle", color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    if (tracks.isEmpty()) {
      item {
        Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Playlist is empty", color = TextSecondary, fontWeight = FontWeight.Bold)
            Text("Search or explore tracks to add them here", color = TextMuted, fontSize = 12.sp)
          }
        }
      }
    } else {
      items(tracks, key = { it.id }) { track ->
        val isCurrent = currentPlayingTrack?.id == track.id
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onTrackClick(track) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
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
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(20.dp).align(Alignment.Center))
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
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
                fontSize = 11.sp
              )
            }
          }

          if (playlist.isCustom) {
            IconButton(onClick = { onRemoveTrack(track.id) }) {
              Icon(Icons.Default.Delete, contentDescription = "Remove Track", tint = TextMuted, modifier = Modifier.size(18.dp))
            }
          } else {
            Text(
              text = track.formattedDuration,
              style = MaterialTheme.typography.labelSmall,
              color = TextMuted
            )
          }
        }
      }
    }
  }
}
