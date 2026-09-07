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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SettingsState
import com.example.ui.theme.BgDark
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ImmersiveOutlineVariant
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSecondaryContainer
import com.example.ui.theme.ImmersiveSurfaceCard
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
  settings: SettingsState,
  onUpdateAudioQuality: (String) -> Unit,
  onUpdateCrossfade: (Int) -> Unit,
  onToggleShakeToSkip: (Boolean) -> Unit,
  onToggleLyricsAutoScroll: (Boolean) -> Unit,
  onToggleVisualizer60fps: (Boolean) -> Unit,
  onClearCache: () -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = SurfaceDark,
    tonalElevation = 12.dp,
    modifier = Modifier.testTag("settings_modal_bottom_sheet")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .padding(bottom = 36.dp)
    ) {
      // Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Settings, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(24.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Settings & Audio Lab",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = TextPrimary
          )
        }

        IconButton(onClick = onDismiss) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section: Audio Streaming Quality
      SettingsSectionTitle(title = "AUDIO & STREAMING")

      val qualities = listOf("Normal (160kbps)", "High (320kbps)", "Hi-Fi Studio")
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(SurfaceCard)
          .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
          .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        qualities.forEach { quality ->
          val isSelected = settings.audioQuality == quality
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(if (isSelected) SpotifyGreen else Color.Transparent)
              .clickable { onUpdateAudioQuality(quality) }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = quality.split(" ").first(),
              style = MaterialTheme.typography.bodySmall,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) BgDark else TextSecondary,
              fontSize = 11.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Section: Crossfade Playback
      SettingsSectionTitle(title = "PLAYBACK ENGINE")

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(SurfaceCard)
          .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
          .padding(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Crossfade Between Songs", color = TextPrimary, fontWeight = FontWeight.SemiBold)
          Text("${settings.crossfadeSec}s", color = SpotifyGreen, fontWeight = FontWeight.Bold)
        }
        Slider(
          value = settings.crossfadeSec.toFloat(),
          onValueChange = { onUpdateCrossfade(it.toInt()) },
          valueRange = 0f..12f,
          steps = 11,
          colors = SliderDefaults.colors(
            thumbColor = SpotifyGreen,
            activeTrackColor = SpotifyGreen,
            inactiveTrackColor = SurfaceElevated
          )
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Shake to skip
      SettingsToggleRow(
        icon = Icons.Default.PhoneAndroid,
        title = "Shake to Skip Track",
        subtitle = "Skip to next song by shaking your device",
        isChecked = settings.shakeToSkipEnabled,
        onCheckedChange = onToggleShakeToSkip
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Synced lyrics auto scroll
      SettingsToggleRow(
        icon = Icons.Default.Lyrics,
        title = "Synced Lyrics Real-Time Scroll",
        subtitle = "Automatically follow and highlight active lines",
        isChecked = settings.lyricsAutoScroll,
        onCheckedChange = onToggleLyricsAutoScroll
      )

      Spacer(modifier = Modifier.height(10.dp))

      // 60FPS Visualizer Acceleration
      SettingsToggleRow(
        icon = Icons.Default.GraphicEq,
        title = "3D Visualizer 60 FPS Mode",
        subtitle = "Smooth high-frequency canvas GPU rendering",
        isChecked = settings.visualizer60fps,
        onCheckedChange = onToggleVisualizer60fps
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Storage & Cache
      SettingsSectionTitle(title = "STORAGE & OFFLINE")

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(SurfaceCard)
          .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Cached Audio & Artworks", color = TextPrimary, fontWeight = FontWeight.SemiBold)
          Text("%.1f MB used".format(settings.offlineCacheSizeMb), color = TextMuted, fontSize = 12.sp)
        }

        TextButton(
          onClick = onClearCache,
          modifier = Modifier.testTag("clear_cache_button")
        ) {
          Icon(Icons.Default.Cached, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Clear", color = SpotifyGreen, fontWeight = FontWeight.Bold)
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // About
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(SurfaceElevated)
          .padding(14.dp)
      ) {
        Column {
          Text("SpotiFusion Android v2.4.0", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Text("Connected to Spotifusion Web Audio & LRCLIB Core", color = TextMuted, fontSize = 11.sp)
        }
      }
    }
  }
}

@Composable
private fun SettingsSectionTitle(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelSmall,
    color = SpotifyGreen,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.sp,
    fontSize = 11.sp,
    modifier = Modifier.padding(vertical = 6.dp)
  )
}

@Composable
private fun SettingsToggleRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  isChecked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(SurfaceCard)
      .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
      .padding(14.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(SurfaceElevated),
        contentAlignment = Alignment.Center
      ) {
        Icon(icon, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(20.dp))
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text(subtitle, color = TextMuted, fontSize = 11.sp)
      }
    }

    Switch(
      checked = isChecked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = BgDark,
        checkedTrackColor = SpotifyGreen,
        uncheckedThumbColor = TextMuted,
        uncheckedTrackColor = SurfaceElevated
      )
    )
  }
}
