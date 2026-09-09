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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BgDark
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SleepTimerDialog(activeMinutes: Int?, remainingSec: Int, onSetTimer: (Int?) -> Unit, onDismiss: () -> Unit) {
  val options = listOf(0, 5, 10, 15, 30, 60)
  var selected by remember { mutableStateOf(activeMinutes ?: 0) }
  Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
    Box(Modifier.fillMaxSize().background(BgDark).padding(horizontal = 16.dp)) {
      Column(Modifier.fillMaxSize().padding(top = 12.dp, bottom = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(20.dp).clickable(onClick = onDismiss))
          Spacer(Modifier.size(10.dp)); Text("Sleep Timer", color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))
        Text("Now Playing", color = TextMuted, fontSize = 8.sp)
        Text("Choose when playback should stop", color = TextSecondary, fontSize = 10.sp)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(SurfaceCard).border(1.dp, GlassBorder, RoundedCornerShape(10.dp))) {
          options.forEach { min ->
            Row(Modifier.fillMaxWidth().clickable { selected = min }.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
              Box(Modifier.size(15.dp).border(1.5.dp, if (selected == min) BrandPurple else TextSecondary, androidx.compose.foundation.shape.CircleShape), Alignment.Center) {
                if (selected == min) Box(Modifier.size(7.dp).background(BrandPurple, androidx.compose.foundation.shape.CircleShape))
              }
              Spacer(Modifier.size(10.dp))
              Text(if (min == 0) "Off" else if (min == 60) "1 hour" else "$min minutes", color = TextPrimary, fontSize = 10.sp)
            }
          }
        }
        Spacer(Modifier.weight(1f))
        if (remainingSec > 0) Text("Remaining: %02d:%02d".format(remainingSec / 60, remainingSec % 60), color = BrandPurple, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
        Button(onClick = { onSetTimer(if (selected == 0) null else selected); onDismiss() }, modifier = Modifier.fillMaxWidth().height(42.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)) { Text("Set Timer", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
      }
    }
  }
}
