package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EqualizerState
import com.example.ui.theme.BgDark
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun EqualizerScreen(
  equalizerState: EqualizerState,
  onToggleEnabled: (Boolean) -> Unit,
  onSelectPreset: (String) -> Unit,
  onBandGainChanged: (bandIndex: Int, gainDb: Float) -> Unit,
  onBassBoostChanged: (Float) -> Unit,
  onVirtualizerChanged: (Float) -> Unit,
  onBack: () -> Unit
) {
  val presets = listOf("Flat", "Bass Boost", "Vocal Booster", "Electronic", "Rock", "Acoustic", "Jazz")

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(BgDark)
      .testTag("equalizer_screen"),
    contentPadding = PaddingValues(bottom = 16.dp)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
          }
          Spacer(modifier = Modifier.width(4.dp))
          Column {
            Text(
              text = "Equalizer",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Black,
              color = TextPrimary
            )
            Text(
              text = "Studio Sound Calibration",
              style = MaterialTheme.typography.bodySmall,
              color = TextSecondary,
              fontSize = 12.sp
            )
          }
        }

        Switch(
          checked = equalizerState.isEnabled,
          onCheckedChange = onToggleEnabled,
          colors = SwitchDefaults.colors(
            checkedThumbColor = BgDark,
            checkedTrackColor = SpotifyGreen,
            uncheckedThumbColor = TextMuted,
            uncheckedTrackColor = SurfaceElevated
          ),
          modifier = Modifier.testTag("equalizer_power_switch")
        )
      }
    }

    // Interactive Dynamic Frequency Curve Canvas
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp)
          .height(130.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(SurfaceCard)
          .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
          .padding(16.dp)
      ) {
        val zeroLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        Canvas(modifier = Modifier.fillMaxSize()) {
          val width = size.width
          val height = size.height
          val centerY = height / 2f

          // Center zero line
          drawLine(
            color = zeroLineColor,
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1.dp.toPx()
          )

          if (equalizerState.isEnabled) {
            val path = Path()
            val points = equalizerState.bandGains.mapIndexed { index, gain ->
              val x = (index.toFloat() / (equalizerState.bandGains.size - 1)) * width
              // Normalize -12dB .. +12dB to height
              val normalized = (gain / 12f).coerceIn(-1f, 1f)
              val y = centerY - (normalized * (height * 0.4f))
              Offset(x, y)
            }

            if (points.isNotEmpty()) {
              path.moveTo(points.first().x, points.first().y)
              for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val controlX = (prev.x + curr.x) / 2f
                path.cubicTo(controlX, prev.y, controlX, curr.y, curr.x, curr.y)
              }

              drawPath(
                path = path,
                brush = Brush.horizontalGradient(listOf(SpotifyGreen, BrandCyan, SpotifyGreen)),
                style = Stroke(width = 3.dp.toPx())
              )

              points.forEach { pt ->
                drawCircle(color = SpotifyGreen, radius = 5.dp.toPx(), center = pt)
                drawCircle(color = BgDark, radius = 2.dp.toPx(), center = pt)
              }
            }
          }
        }
      }
    }

    // Presets Row
    item {
      Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
          text = "Sound Profiles",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          items(presets) { preset ->
            val isSelected = equalizerState.activePreset.equals(preset, ignoreCase = true)
            FilterChip(
              selected = isSelected,
              onClick = { onSelectPreset(preset) },
              label = {
                Text(
                  preset,
                  color = if (isSelected) BgDark else TextPrimary,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 12.sp
                )
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SpotifyGreen,
                containerColor = SurfaceElevated
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = isSelected,
                borderColor = GlassBorder,
                selectedBorderColor = SpotifyGreen
              ),
              shape = RoundedCornerShape(20.dp)
            )
          }
        }
      }
    }

    // 5-Band Frequency Sliders
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(SurfaceCard)
          .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
          .padding(16.dp)
      ) {
        Text(
          text = "Frequency Bands (-12 dB to +12 dB)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )

        Spacer(modifier = Modifier.height(14.dp))

        EqualizerState.FREQUENCIES.forEachIndexed { index, freqLabel ->
          val gain = equalizerState.bandGains.getOrElse(index) { 0f }
          Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(text = freqLabel, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontWeight = FontWeight.SemiBold)
              Text(
                text = "${if (gain > 0) "+" else ""}${gain.toInt()} dB",
                style = MaterialTheme.typography.bodySmall,
                color = if (gain != 0f) SpotifyGreen else TextMuted,
                fontWeight = FontWeight.Bold
              )
            }

            Slider(
              value = gain,
              onValueChange = { onBandGainChanged(index, it) },
              valueRange = -12f..12f,
              enabled = equalizerState.isEnabled,
              colors = SliderDefaults.colors(
                thumbColor = SpotifyGreen,
                activeTrackColor = SpotifyGreen,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("eq_slider_$index")
            )
          }
        }
      }
    }

    // Audio Enhancers: Bass Boost + 3D Virtualizer
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(SurfaceCard)
          .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
          .padding(16.dp)
      ) {
        Text(
          text = "Audio Enhancements",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Bass Boost
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Bass Boost", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
          }
          Text(
            text = "${(equalizerState.bassBoost * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = SpotifyGreen,
            fontWeight = FontWeight.Bold
          )
        }

        Slider(
          value = equalizerState.bassBoost,
          onValueChange = onBassBoostChanged,
          enabled = equalizerState.isEnabled,
          colors = SliderDefaults.colors(
            thumbColor = SpotifyGreen,
            activeTrackColor = SpotifyGreen,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
          ),
          modifier = Modifier.fillMaxWidth().testTag("bass_boost_slider")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 3D Virtualizer
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SurroundSound, contentDescription = null, tint = BrandCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("3D Surround Virtualizer", style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
          }
          Text(
            text = "${(equalizerState.virtualizer * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = BrandCyan,
            fontWeight = FontWeight.Bold
          )
        }

        Slider(
          value = equalizerState.virtualizer,
          onValueChange = onVirtualizerChanged,
          enabled = equalizerState.isEnabled,
          colors = SliderDefaults.colors(
            thumbColor = BrandCyan,
            activeTrackColor = BrandCyan,
            inactiveTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
          ),
          modifier = Modifier.fillMaxWidth().testTag("virtualizer_slider")
        )
      }
    }
  }
}
