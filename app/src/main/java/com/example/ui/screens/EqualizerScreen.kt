package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EqualizerState
import com.example.ui.theme.BgDark
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EqualizerScreen(equalizerState: EqualizerState, onToggleEnabled: (Boolean) -> Unit, onSelectPreset: (String) -> Unit, onBandGainChanged: (bandIndex: Int, gainDb: Float) -> Unit, onBassBoostChanged: (Float) -> Unit, onVirtualizerChanged: (Float) -> Unit, onBack: () -> Unit) {
  val presets = listOf("Normal", "Pop", "Rock", "Jazz", "Classical", "Custom")
  val labels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
  Column(Modifier.fillMaxSize().background(BgDark).padding(horizontal = 14.dp).testTag("equalizer_screen")) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
      Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(18.dp)) }; Spacer(Modifier.size(5.dp)); Text("Equalizer", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
      Switch(checked = equalizerState.isEnabled, onCheckedChange = onToggleEnabled, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPurple, uncheckedThumbColor = TextMuted, uncheckedTrackColor = SurfaceElevated), modifier = Modifier.testTag("equalizer_power_switch"))
    }
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      presets.forEachIndexed { i, p ->
        val selected = equalizerState.activePreset.equals(p, true)
        FilterChip(selected = selected, onClick = { onSelectPreset(p) }, label = { Text(p, fontSize = 8.sp, color = if (selected) Color.White else TextSecondary) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = BrandPurple, containerColor = SurfaceCard, labelColor = TextSecondary), modifier = Modifier.weight(1f), border = null)
      }
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(12.dp)).background(SurfaceCard).border(1.dp, GlassBorder, RoundedCornerShape(12.dp)).padding(horizontal = 8.dp, vertical = 10.dp), Arrangement.SpaceEvenly, Alignment.CenterVertically) {
      labels.forEachIndexed { index, label ->
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
          Box(Modifier.height(145.dp).width(38.dp), Alignment.Center) {
            Slider(value = equalizerState.bandGains.getOrElse(index) { 0f }, onValueChange = { onBandGainChanged(index, it) }, valueRange = -12f..12f, enabled = equalizerState.isEnabled, colors = SliderDefaults.colors(thumbColor = BrandPurple, activeTrackColor = BrandPurple, inactiveTrackColor = Color(0xFF293348)), modifier = Modifier.size(145.dp, 34.dp).graphicsLayer { rotationZ = -90f }.testTag("eq_slider_$index"))
          }
          Text(label, color = TextMuted, fontSize = 7.sp)
        }
      }
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), Arrangement.SpaceBetween) { Text("+12dB", color = TextMuted, fontSize = 7.sp); Text("0dB", color = TextMuted, fontSize = 7.sp); Text("-12dB", color = TextMuted, fontSize = 7.sp) }
    Spacer(Modifier.height(16.dp))
    Text("Bass Boost", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).border(1.dp, GlassBorder, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
      Text("Bass Boost", color = TextSecondary, fontSize = 9.sp); Switch(checked = equalizerState.bassBoost > 0f, onCheckedChange = { onBassBoostChanged(if (it) .7f else 0f) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPurple, uncheckedThumbColor = TextMuted, uncheckedTrackColor = SurfaceElevated), modifier = Modifier.size(40.dp))
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).border(1.dp, GlassBorder, RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
      Text("Virtualizer", color = TextSecondary, fontSize = 9.sp); Switch(checked = equalizerState.virtualizer > 0f, onCheckedChange = { onVirtualizerChanged(if (it) .7f else 0f) }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandPurple, uncheckedThumbColor = TextMuted, uncheckedTrackColor = SurfaceElevated), modifier = Modifier.size(40.dp))
    }
  }
}
