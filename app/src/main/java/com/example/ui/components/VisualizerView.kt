package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ImmersiveOnSecondaryContainer
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersivePrimaryContainer

@Composable
fun VisualizerView(
  bars: List<Float>,
  isPlaying: Boolean,
  modifier: Modifier = Modifier,
  height: Dp = 48.dp,
  barWidth: Dp = 4.dp
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(height),
    horizontalArrangement = Arrangement.SpaceEvenly,
    verticalAlignment = Alignment.CenterVertically
  ) {
    val gradient = Brush.verticalGradient(
      listOf(ImmersiveOnSecondaryContainer, ImmersivePrimary, ImmersivePrimaryContainer)
    )

    bars.forEachIndexed { index, rawAmplitude ->
      val target = if (isPlaying) rawAmplitude.coerceIn(0.12f, 1.0f) else 0.08f
      val animatedHeight by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "bar_$index"
      )

      Box(
        modifier = Modifier
          .width(barWidth)
          .fillMaxHeight(animatedHeight)
          .clip(RoundedCornerShape(percent = 50))
          .background(gradient)
      )
    }
  }
}
