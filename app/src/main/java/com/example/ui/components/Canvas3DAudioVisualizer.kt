package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ImmersiveBg
import com.example.ui.theme.ImmersiveOnSecondaryContainer
import com.example.ui.theme.ImmersiveOutline
import com.example.ui.theme.ImmersiveOutlineVariant
import com.example.ui.theme.ImmersivePrimary
import com.example.ui.theme.ImmersivePrimaryContainer
import com.example.ui.theme.ImmersiveSecondaryContainer
import com.example.ui.theme.ImmersiveSurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Visualizer 3D Mode
 */
enum class Visualizer3DMode(val title: String, val icon: ImageVector) {
  BARS_3D("3D Pillars", Icons.Default.GraphicEq),
  CYBER_WAVE("3D Wave", Icons.Default.Waves),
  COSMIC_ORBIT("Cosmic Orbit", Icons.Default.AutoAwesome),
  NEON_RING("Sonic Ring", Icons.Default.Radar)
}

private data class StarParticle(
  var x: Float,
  var y: Float,
  var z: Float,
  var size: Float,
  var speed: Float,
  var alpha: Float,
  var color: Color
)

@Composable
fun Canvas3DAudioVisualizer(
  frequencies: List<Float>,
  isPlaying: Boolean,
  modifier: Modifier = Modifier,
  height: Dp = 180.dp,
  showModeSelector: Boolean = true,
  currentMode: Visualizer3DMode = Visualizer3DMode.BARS_3D,
  onModeChange: (Visualizer3DMode) -> Unit = {}
) {
  var selectedMode by remember(currentMode) { mutableStateOf(currentMode) }
  var yawAngle by remember { mutableFloatStateOf(0f) }
  var pitchAngle by remember { mutableFloatStateOf(15f) }

  val infiniteTransition = rememberInfiniteTransition(label = "visualizer_anim")
  val phaseAnimation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = (2 * PI).toFloat(),
    animationSpec = infiniteRepeatable(
      animation = tween(4000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "wave_phase"
  )

  // Peak drops physics simulation
  val peakLevels = remember { mutableStateListOf<Float>().apply { repeat(32) { add(0f) } } }
  val smoothedFrequencies = remember { mutableStateListOf<Float>().apply { repeat(32) { add(0.05f) } } }

  // Star particles for cosmic mode
  val particles = remember {
    List(40) {
      StarParticle(
        x = Random.nextFloat() * 2f - 1f,
        y = Random.nextFloat() * 2f - 1f,
        z = Random.nextFloat() * 0.8f + 0.2f,
        size = Random.nextFloat() * 3f + 1.5f,
        speed = Random.nextFloat() * 0.015f + 0.005f,
        alpha = Random.nextFloat() * 0.8f + 0.2f,
        color = listOf(
          ImmersivePrimary,
          ImmersiveOnSecondaryContainer,
          Color(0xFFE8DEF8),
          Color(0xFF9A82DB)
        ).random()
      )
    }
  }

  // Smooth lerp updating
  LaunchedEffect(frequencies, isPlaying) {
    for (i in 0 until 32) {
      val target = if (isPlaying && i < frequencies.size) frequencies[i].coerceIn(0.05f, 1.0f) else 0.05f
      val current = smoothedFrequencies.getOrElse(i) { 0.05f }
      val lerped = current + (target - current) * 0.35f
      if (i < smoothedFrequencies.size) {
        smoothedFrequencies[i] = lerped
      }

      val currPeak = peakLevels.getOrElse(i) { 0f }
      if (lerped > currPeak) {
        if (i < peakLevels.size) peakLevels[i] = lerped
      } else {
        if (i < peakLevels.size) peakLevels[i] = (currPeak - 0.02f).coerceAtLeast(0.05f)
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .background(ImmersiveSurfaceCard)
      .border(1.dp, ImmersiveOutlineVariant, RoundedCornerShape(24.dp))
      .padding(12.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // 3D Canvas Viewport
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(height)
        .clip(RoundedCornerShape(18.dp))
        .background(
          Brush.radialGradient(
            colors = listOf(
              ImmersivePrimaryContainer.copy(alpha = 0.3f),
              ImmersiveBg
            )
          )
        )
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            yawAngle = (yawAngle + dragAmount.x * 0.3f).coerceIn(-45f, 45f)
            pitchAngle = (pitchAngle - dragAmount.y * 0.2f).coerceIn(0f, 35f)
          }
        }
        .clickable {
          // Tap on canvas to cycle visualizer mode
          val nextMode = when (selectedMode) {
            Visualizer3DMode.BARS_3D -> Visualizer3DMode.CYBER_WAVE
            Visualizer3DMode.CYBER_WAVE -> Visualizer3DMode.COSMIC_ORBIT
            Visualizer3DMode.COSMIC_ORBIT -> Visualizer3DMode.NEON_RING
            Visualizer3DMode.NEON_RING -> Visualizer3DMode.BARS_3D
          }
          selectedMode = nextMode
          onModeChange(nextMode)
        }
        .testTag("canvas_3d_visualizer")
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val heightPx = size.height

        when (selectedMode) {
          Visualizer3DMode.BARS_3D -> {
            draw3DPillars(
              frequencies = smoothedFrequencies,
              peakLevels = peakLevels,
              yawAngle = yawAngle,
              pitchAngle = pitchAngle,
              isPlaying = isPlaying,
              phase = phaseAnimation
            )
          }
          Visualizer3DMode.CYBER_WAVE -> {
            draw3DCyberWave(
              frequencies = smoothedFrequencies,
              phase = phaseAnimation,
              yawAngle = yawAngle,
              pitchAngle = pitchAngle,
              isPlaying = isPlaying
            )
          }
          Visualizer3DMode.COSMIC_ORBIT -> {
            drawCosmicOrbit(
              frequencies = smoothedFrequencies,
              particles = particles,
              phase = phaseAnimation,
              isPlaying = isPlaying
            )
          }
          Visualizer3DMode.NEON_RING -> {
            drawNeonSoundRing(
              frequencies = smoothedFrequencies,
              phase = phaseAnimation,
              yawAngle = yawAngle,
              pitchAngle = pitchAngle,
              isPlaying = isPlaying
            )
          }
        }

        // 3D Glass Surface Grid Lines / Ambient Depth Floor
        drawGlassGridFloor(pitchAngle)
      }

      // Floating Minimal Mode Badge in Top-Right Corner
      Row(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(8.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(ImmersiveBg.copy(alpha = 0.75f))
          .border(0.5.dp, ImmersiveOutlineVariant, RoundedCornerShape(12.dp))
          .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = selectedMode.icon,
          contentDescription = null,
          tint = ImmersivePrimary,
          modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = selectedMode.title,
          style = MaterialTheme.typography.labelSmall,
          fontSize = 10.sp,
          color = ImmersivePrimary,
          fontWeight = FontWeight.Medium
        )
      }
    }

    if (showModeSelector) {
      Spacer(modifier = Modifier.height(10.dp))

      // Sleek Mode Selection Pills
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Visualizer3DMode.values().forEach { mode ->
          val isSelected = mode == selectedMode
          Surface(
            onClick = {
              selectedMode = mode
              onModeChange(mode)
            },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) ImmersiveSecondaryContainer else Color.Transparent,
            modifier = Modifier
              .border(
                width = 1.dp,
                color = if (isSelected) ImmersivePrimary else ImmersiveOutlineVariant,
                shape = RoundedCornerShape(12.dp)
              )
              .testTag("visualizer_mode_${mode.name.lowercase()}")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = mode.icon,
                contentDescription = mode.title,
                tint = if (isSelected) ImmersiveOnSecondaryContainer else TextMuted,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = mode.title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) ImmersiveOnSecondaryContainer else TextSecondary
              )
            }
          }
        }
      }
    }
  }
}

/**
 * 1. 3D Extruded Pillars with Perspective Depth, Peak Indicators, and Ground Mirror Reflection
 */
private fun DrawScope.draw3DPillars(
  frequencies: List<Float>,
  peakLevels: List<Float>,
  yawAngle: Float,
  pitchAngle: Float,
  isPlaying: Boolean,
  phase: Float
) {
  val barCount = 20
  val totalWidth = size.width * 0.88f
  val startX = (size.width - totalWidth) / 2f
  val barSpacing = totalWidth / barCount
  val barWidth = barSpacing * 0.65f
  val baseGroundY = size.height * 0.72f
  val maxHeight = size.height * 0.55f

  // 3D Depth Extrusion vector
  val depthX = sin(yawAngle * PI / 180f).toFloat() * 12f
  val depthY = -cos(pitchAngle * PI / 180f).toFloat() * 10f

  for (i in 0 until barCount) {
    val freqIndex = (i * frequencies.size / barCount).coerceIn(0, frequencies.size - 1)
    val amp = frequencies[freqIndex]
    val currentBarHeight = (amp * maxHeight).coerceAtLeast(6f)
    val x = startX + i * barSpacing
    val topY = baseGroundY - currentBarHeight

    // 1. Bottom Mirror Reflection on Glassy Floor
    val reflectionHeight = currentBarHeight * 0.35f
    val reflectionBrush = Brush.verticalGradient(
      colors = listOf(
        ImmersivePrimary.copy(alpha = 0.25f * amp),
        Color.Transparent
      ),
      startY = baseGroundY,
      endY = baseGroundY + reflectionHeight
    )
    drawRect(
      brush = reflectionBrush,
      topLeft = Offset(x, baseGroundY),
      size = Size(barWidth, reflectionHeight)
    )

    // 2. 3D Side Depth Face (Extrusion)
    if (depthX != 0f) {
      val sidePath = Path().apply {
        moveTo(x + barWidth, topY)
        lineTo(x + barWidth + depthX, topY + depthY)
        lineTo(x + barWidth + depthX, baseGroundY + depthY)
        lineTo(x + barWidth, baseGroundY)
        close()
      }
      drawPath(
        path = sidePath,
        color = ImmersivePrimaryContainer.copy(alpha = 0.5f + amp * 0.3f),
        style = Fill
      )
    }

    // 3. 3D Top Cap
    val topCapPath = Path().apply {
      moveTo(x, topY)
      lineTo(x + depthX, topY + depthY)
      lineTo(x + barWidth + depthX, topY + depthY)
      lineTo(x + barWidth, topY)
      close()
    }
    drawPath(
      path = topCapPath,
      color = Color(0xFFE8DEF8).copy(alpha = 0.85f),
      style = Fill
    )

    // 4. Front Face of Bar (Vibrant Lavender Gradient)
    val frontBrush = Brush.verticalGradient(
      colors = listOf(
        Color(0xFFE8DEF8),
        ImmersivePrimary,
        ImmersivePrimaryContainer
      ),
      startY = topY,
      endY = baseGroundY
    )

    drawRoundRect(
      brush = frontBrush,
      topLeft = Offset(x, topY),
      size = Size(barWidth, currentBarHeight),
      cornerRadius = CornerRadius(4f, 4f)
    )

    // 5. Peak Indicator Drop
    val peak = peakLevels[freqIndex]
    val peakY = baseGroundY - (peak * maxHeight).coerceAtLeast(currentBarHeight + 4f)
    drawRoundRect(
      color = Color(0xFFFFFFFF),
      topLeft = Offset(x, peakY - 3f),
      size = Size(barWidth, 3f),
      cornerRadius = CornerRadius(2f, 2f)
    )
  }
}

/**
 * 2. 3D Cyber Wave Mesh: Multi-layered Neon Spline Ribbons
 */
private fun DrawScope.draw3DCyberWave(
  frequencies: List<Float>,
  phase: Float,
  yawAngle: Float,
  pitchAngle: Float,
  isPlaying: Boolean
) {
  val layers = 3
  val baseY = size.height * 0.55f
  val width = size.width
  val step = width / 24f

  val layerColors = listOf(
    ImmersivePrimaryContainer.copy(alpha = 0.3f),
    ImmersivePrimary.copy(alpha = 0.6f),
    Color(0xFFE8DEF8).copy(alpha = 0.9f)
  )

  for (layer in 0 until layers) {
    val depthOffset = (layer - 1) * 20f
    val layerY = baseY + depthOffset * (pitchAngle / 25f)
    val wavePath = Path()
    val fillPath = Path()

    var firstPoint = true

    for (i in 0..24) {
      val x = i * step + sin(yawAngle * PI / 180f).toFloat() * 10f
      val freqIdx = (i * frequencies.size / 24).coerceIn(0, frequencies.size - 1)
      val amp = frequencies[freqIdx] * (if (isPlaying) 1f else 0.2f)

      val waveOffset = sin(phase + i * 0.4f + layer * 1.2f) * (amp * 45f + 8f)
      val y = (layerY - waveOffset).toFloat()

      if (firstPoint) {
        wavePath.moveTo(x, y)
        fillPath.moveTo(x, size.height)
        fillPath.lineTo(x, y)
        firstPoint = false
      } else {
        wavePath.lineTo(x, y)
        fillPath.lineTo(x, y)
      }
    }

    fillPath.lineTo(width, size.height)
    fillPath.close()

    // Draw ambient fill under wave
    drawPath(
      path = fillPath,
      brush = Brush.verticalGradient(
        colors = listOf(
          layerColors[layer].copy(alpha = 0.25f),
          Color.Transparent
        ),
        startY = layerY - 40f,
        endY = size.height
      )
    )

    // Draw Glowing Stroke Line
    drawPath(
      path = wavePath,
      color = layerColors[layer],
      style = Stroke(
        width = if (layer == 2) 3f else 1.8f,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
      )
    )
  }
}

/**
 * 3. Cosmic Orbit: Center Pulsing Core & Radial Spectrum Spikes + Particle Field
 */
private fun DrawScope.drawCosmicOrbit(
  frequencies: List<Float>,
  particles: List<StarParticle>,
  phase: Float,
  isPlaying: Boolean
) {
  val centerX = size.width / 2f
  val centerY = size.height / 2f
  val avgFreq = frequencies.take(8).average().toFloat().coerceIn(0.1f, 1.0f)
  val coreRadius = 26f + avgFreq * 18f

  // 1. Draw Star particles
  particles.forEach { p ->
    p.z -= p.speed * (if (isPlaying) 1.5f + avgFreq else 0.5f)
    if (p.z <= 0.05f) p.z = 1f

    val projX = centerX + (p.x / p.z) * (size.width * 0.38f)
    val projY = centerY + (p.y / p.z) * (size.height * 0.38f)
    val radius = (p.size / p.z).coerceIn(1f, 6f)

    if (projX in 0f..size.width && projY in 0f..size.height) {
      drawCircle(
        color = p.color.copy(alpha = (p.alpha / p.z).coerceIn(0.1f, 0.9f)),
        radius = radius,
        center = Offset(projX, projY)
      )
    }
  }

  // 2. Radial Frequency Spikes
  val rayCount = 28
  for (i in 0 until rayCount) {
    val angle = (i.toFloat() / rayCount) * 2 * PI + phase * 0.5f
    val freqIdx = (i * frequencies.size / rayCount).coerceIn(0, frequencies.size - 1)
    val rayLength = coreRadius + 10f + frequencies[freqIdx] * 40f

    val startX = centerX + cos(angle).toFloat() * (coreRadius + 4f)
    val startY = centerY + sin(angle).toFloat() * (coreRadius + 4f)
    val endX = centerX + cos(angle).toFloat() * rayLength
    val endY = centerY + sin(angle).toFloat() * rayLength

    drawLine(
      brush = Brush.linearGradient(
        listOf(ImmersivePrimary, Color(0xFFE8DEF8)),
        start = Offset(startX, startY),
        end = Offset(endX, endY)
      ),
      start = Offset(startX, startY),
      end = Offset(endX, endY),
      strokeWidth = 3f,
      cap = StrokeCap.Round
    )
  }

  // 3. Central Glowing Pulsing Orb
  drawCircle(
    brush = Brush.radialGradient(
      colors = listOf(
        Color(0xFFE8DEF8),
        ImmersivePrimary,
        ImmersivePrimaryContainer.copy(alpha = 0.2f),
        Color.Transparent
      ),
      center = Offset(centerX, centerY),
      radius = coreRadius * 1.6f
    ),
    radius = coreRadius * 1.6f,
    center = Offset(centerX, centerY)
  )

  drawCircle(
    color = ImmersivePrimary,
    radius = coreRadius,
    center = Offset(centerX, centerY)
  )
}

/**
 * 4. Neon Sound Ring: Isometric 3D Sonic Ripples
 */
private fun DrawScope.drawNeonSoundRing(
  frequencies: List<Float>,
  phase: Float,
  yawAngle: Float,
  pitchAngle: Float,
  isPlaying: Boolean
) {
  val centerX = size.width / 2f
  val centerY = size.height / 2f
  val rings = 4

  val avgBass = frequencies.take(4).average().toFloat().coerceIn(0.1f, 1f)

  for (r in 1..rings) {
    val baseRadius = r * 26f + (avgBass * 14f)
    val ringPhase = phase + r * 0.8f

    val ringPath = Path()
    val segments = 32

    for (s in 0..segments) {
      val theta = (s.toFloat() / segments) * 2 * PI
      val freqIdx = (s * frequencies.size / segments).coerceIn(0, frequencies.size - 1)
      val deformation = frequencies[freqIdx] * 12f * sin(ringPhase + s * 0.5f).toFloat()

      val currentR = baseRadius + deformation
      // Flatten into 3D isometric plane via pitch angle
      val x = centerX + cos(theta).toFloat() * currentR
      val y = centerY + sin(theta).toFloat() * (currentR * 0.45f)

      if (s == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
    }

    drawPath(
      path = ringPath,
      color = if (r == 1) Color(0xFFE8DEF8) else ImmersivePrimary.copy(alpha = (1f / r).coerceIn(0.2f, 0.8f)),
      style = Stroke(
        width = (4f - r * 0.6f).coerceAtLeast(1.5f),
        cap = StrokeCap.Round
      )
    )
  }
}

/**
 * Ambient Glass Perspective Floor Grid
 */
private fun DrawScope.drawGlassGridFloor(pitchAngle: Float) {
  val startY = size.height * 0.72f
  val endY = size.height
  val lineCount = 4

  for (i in 1..lineCount) {
    val y = startY + (endY - startY) * (i.toFloat() / lineCount)
    drawLine(
      color = ImmersiveOutlineVariant.copy(alpha = 0.2f * (i.toFloat() / lineCount)),
      start = Offset(0f, y),
      end = Offset(size.width, y),
      strokeWidth = 1f
    )
  }
}
