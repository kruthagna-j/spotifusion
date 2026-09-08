package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SettingsState
import com.example.ui.theme.BgDark
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

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
  Box(Modifier.fillMaxSize().background(BgDark).padding(horizontal = 16.dp)) {
    Column(Modifier.fillMaxSize().padding(top = 10.dp, bottom = 18.dp)) {
      Row(Modifier.fillMaxWidth(), Arrangement.Start, Alignment.CenterVertically) {
        Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(20.dp).clickable(onClick = onDismiss))
        Spacer(Modifier.width(10.dp))
        Text("Settings", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
      }
      Spacer(Modifier.height(18.dp))
      Section("Account")
      SettingRow(Icons.Default.Settings, "Sign in with Google", "Connect your account", false, {})
      Spacer(Modifier.height(12.dp))
      Section("Playback")
      SettingRow(Icons.Default.PhoneAndroid, "Background Playback", "Continue playing when screen is locked", true, {})
      SettingRow(Icons.Default.Notifications, "Download over WiFi only", "Save mobile data", true, {})
      SettingRow(Icons.Default.PhoneAndroid, "Shake to change song", "Skip to the next song", settings.shakeToSkipEnabled, onToggleShakeToSkip)
      Spacer(Modifier.height(12.dp))
      Section("Appearance")
      Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).border(1.dp, GlassBorder, RoundedCornerShape(10.dp)).padding(4.dp)) {
        ThemeChoice(Icons.Default.DarkMode, "Dark", settings.darkTheme, { onToggleDarkTheme(true) }, Modifier.weight(1f))
        ThemeChoice(Icons.Default.LightMode, "Light", !settings.darkTheme, { onToggleDarkTheme(false) }, Modifier.weight(1f))
      }
      Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), Arrangement.SpaceBetween) {
        Text("Theme", color = TextPrimary, fontSize = 12.sp)
        Text(if (settings.darkTheme) "Dark" else "Light", color = TextSecondary, fontSize = 11.sp)
      }
      Spacer(Modifier.height(6.dp))
      Section("About")
      SettingRow(Icons.Default.Settings, "Version", "1.0.0", false, {})
      SettingRow(Icons.Default.Settings, "About Us", "SpotiFusion", false, {})
    }
  }
}

@Composable private fun Section(text: String) { Text(text, color = BrandPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp)) }

@Composable private fun SettingRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
  Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).border(1.dp, GlassBorder, RoundedCornerShape(10.dp)).padding(horizontal = 11.dp, vertical = 10.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp)); Column { Text(title, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = TextMuted, fontSize = 8.sp) }
    }
    Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPurple, uncheckedThumbColor = TextMuted, uncheckedTrackColor = SurfaceElevated), modifier = Modifier.size(42.dp))
  }
}

@Composable private fun ThemeChoice(icon: ImageVector, text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
  Row(modifier.clip(RoundedCornerShape(8.dp)).background(if (selected) BrandPurple else Color.Transparent).clickable(onClick = onClick).padding(vertical = 8.dp), Arrangement.Center, Alignment.CenterVertically) {
    Icon(icon, null, tint = if (selected) Color.White else TextMuted, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text(text, color = if (selected) Color.White else TextMuted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
  }
}
