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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BgDark
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersiveSecondaryContainer
import com.example.ui.theme.ImmersiveSurfaceCard
import com.example.ui.theme.SpotifyGreen
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SleepTimerDialog(
  activeMinutes: Int?,
  remainingSec: Int,
  onSetTimer: (Int?) -> Unit,
  onDismiss: () -> Unit
) {
  val durations = listOf(5, 10, 15, 30, 45, 60)

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = SurfaceCard,
      border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("sleep_timer_dialog")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              Icons.Default.Timer,
              contentDescription = "Sleep Timer",
              tint = SpotifyGreen,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Sleep Timer",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
          }
        }

        if (activeMinutes != null && remainingSec > 0) {
          val mins = remainingSec / 60
          val secs = remainingSec % 60
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(ImmersiveSecondaryContainer)
              .padding(12.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Stopping playback in %02d:%02d".format(mins, secs),
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Bold,
              color = SpotifyGreen
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        durations.chunked(2).forEach { rowItems ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            rowItems.forEach { min ->
              val isSelected = activeMinutes == min
              Box(
                modifier = Modifier
                  .weight(1f)
                  .padding(vertical = 4.dp)
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (isSelected) SpotifyGreen else SurfaceElevated)
                  .border(1.dp, if (isSelected) SpotifyGreen else GlassBorder, RoundedCornerShape(12.dp))
                  .clickable {
                    onSetTimer(min)
                    onDismiss()
                  }
                  .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "$min Minutes",
                  style = MaterialTheme.typography.bodyMedium,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  color = if (isSelected) BgDark else TextPrimary
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (activeMinutes != null) {
          TextButton(
            onClick = {
              onSetTimer(null)
              onDismiss()
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("cancel_sleep_timer_button")
          ) {
            Text("Turn off timer", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
