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
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.ui.theme.GlassBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
  settings: SettingsState,
  onUpdateAudioQuality: (String) -> Unit,
  onUpdateCrossfade: (Int) -> Unit,
  onToggleShakeToSkip: (Boolean) -> Unit,
  onToggleLyricsAutoScroll: (Boolean) -> Unit,
  onToggleVisualizer60fps: (Boolean) -> Unit,
  onToggleNotifications: (Boolean) -> Unit,
  onToggleDarkTheme: (Boolean) -> Unit,
  onClearCache: () -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val colors = MaterialTheme.colorScheme
  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = colors.background,
    tonalElevation = 12.dp,
    modifier = Modifier.testTag("settings_modal_bottom_sheet")
  ) {
    Column(
      Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 36.dp)
    ) {
      Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Settings, null, tint = colors.primary, modifier = Modifier.size(24.dp))
          Spacer(Modifier.width(8.dp))
          Text("Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = colors.onBackground)
        }
        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close", tint = colors.onSurfaceVariant) }
      }
      Spacer(Modifier.height(12.dp))
      SettingsSectionTitle("APPEARANCE")
      Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).border(1.dp, GlassBorder, RoundedCornerShape(14.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ThemeOption(Icons.Default.DarkMode, "Dark", settings.darkTheme, { onToggleDarkTheme(true) }, Modifier.weight(1f))
        ThemeOption(Icons.Default.LightMode, "Light", !settings.darkTheme, { onToggleDarkTheme(false) }, Modifier.weight(1f))
      }
      Spacer(Modifier.height(16.dp))
      SettingsSectionTitle("AUDIO & STREAMING")
      val qualities = listOf("Normal (160kbps)", "High (320kbps)", "Maximum")
      Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surface).border(1.dp, GlassBorder, RoundedCornerShape(12.dp)).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        qualities.forEach { quality ->
          val selected = settings.audioQuality == quality
          Box(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (selected) colors.primary else Color.Transparent).clickable { onUpdateAudioQuality(quality) }.padding(vertical = 8.dp), Alignment.Center) {
            Text(quality.substringBefore(" "), fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, color = if (selected) colors.onPrimary else colors.onSurfaceVariant, fontSize = 11.sp)
          }
        }
      }
      Spacer(Modifier.height(16.dp))
      SettingsSectionTitle("PLAYBACK")
      Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).border(1.dp, GlassBorder, RoundedCornerShape(14.dp)).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) { Text("Crossfade", color = colors.onSurface, fontWeight = FontWeight.SemiBold); Text("${settings.crossfadeSec}s", color = colors.primary, fontWeight = FontWeight.Bold) }
        Slider(value = settings.crossfadeSec.toFloat(), onValueChange = { onUpdateCrossfade(it.toInt()) }, valueRange = 0f..12f, steps = 11, colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary, inactiveTrackColor = colors.surfaceVariant))
      }
      Spacer(Modifier.height(10.dp))
      SettingsToggleRow(Icons.Default.PhoneAndroid, "Shake to Skip Track", "Skip to the next song by shaking the device", settings.shakeToSkipEnabled, onToggleShakeToSkip)
      Spacer(Modifier.height(10.dp))
      SettingsToggleRow(Icons.Default.Lyrics, "Synced Lyrics Auto Scroll", "Follow the active lyric line while playing", settings.lyricsAutoScroll, onToggleLyricsAutoScroll)
      Spacer(Modifier.height(10.dp))
      SettingsToggleRow(Icons.Default.GraphicEq, "High FPS Visualizer", "Use the faster visualizer animation cadence", settings.visualizer60fps, onToggleVisualizer60fps)
      Spacer(Modifier.height(10.dp))
      SettingsToggleRow(Icons.Default.Notifications, "Download Notifications", "Notify when an offline download finishes", settings.notificationsEnabled, onToggleNotifications)
      Spacer(Modifier.height(16.dp))
      SettingsSectionTitle("STORAGE")
      Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).border(1.dp, GlassBorder, RoundedCornerShape(14.dp)).padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Column { Text("App Cache", color = colors.onSurface, fontWeight = FontWeight.SemiBold); Text("%.1f MB used".format(settings.offlineCacheSizeMb), color = colors.onSurfaceVariant, fontSize = 12.sp) }
        TextButton(onClick = onClearCache, modifier = Modifier.testTag("clear_cache_button")) { Icon(Icons.Default.Cached, null, tint = colors.primary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Clear", color = colors.primary, fontWeight = FontWeight.Bold) }
      }
      Spacer(Modifier.height(20.dp))
      Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surfaceVariant).padding(14.dp)) {
        Column { Text("SpotiFusion Android", color = colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp); Text("Playback, offline music, lyrics and equalizer", color = colors.onSurfaceVariant, fontSize = 11.sp) }
      }
    }
  }
}

@Composable private fun ThemeOption(icon: ImageVector, title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
  val colors = MaterialTheme.colorScheme
  Row(modifier.clip(RoundedCornerShape(10.dp)).background(if (selected) colors.primary else Color.Transparent).clickable(onClick = onClick).padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, title, tint = if (selected) colors.onPrimary else colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(7.dp))
    Text(title, color = if (selected) colors.onPrimary else colors.onSurfaceVariant, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
  }
}

@Composable private fun SettingsSectionTitle(title: String) {
  Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 11.sp, modifier = Modifier.padding(vertical = 6.dp))
}

@Composable private fun SettingsToggleRow(icon: ImageVector, title: String, subtitle: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  val colors = MaterialTheme.colorScheme
  Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface).border(1.dp, GlassBorder, RoundedCornerShape(14.dp)).padding(14.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
      Box(Modifier.size(36.dp).clip(CircleShape).background(colors.surfaceVariant), Alignment.Center) { Icon(icon, null, tint = colors.primary, modifier = Modifier.size(20.dp)) }
      Spacer(Modifier.width(12.dp))
      Column { Text(title, color = colors.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(subtitle, color = colors.onSurfaceVariant, fontSize = 11.sp) }
    }
    Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = colors.onPrimary, checkedTrackColor = colors.primary, uncheckedThumbColor = colors.onSurfaceVariant, uncheckedTrackColor = colors.surfaceVariant))
  }
}
