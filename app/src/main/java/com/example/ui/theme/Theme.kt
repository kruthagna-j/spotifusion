package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val SpotifusionDarkColorScheme = darkColorScheme(
  primary = Color(0xFF7B51FB),
  onPrimary = Color.White,
  primaryContainer = Color(0xFF24154F),
  onPrimaryContainer = Color(0xFFE5DCFF),
  secondary = Color(0xFF61C8FF),
  onSecondary = Color(0xFF061018),
  secondaryContainer = Color(0xFF15263A),
  onSecondaryContainer = Color(0xFFE4F2FF),
  tertiary = Color(0xFFB69CFF),
  onTertiary = Color(0xFF160D2B),
  background = Color(0xFF05080F),
  onBackground = Color(0xFFF6F7FB),
  surface = Color(0xFF0A0F18),
  onSurface = Color(0xFFF6F7FB),
  surfaceVariant = Color(0xFF0E1420),
  onSurfaceVariant = Color(0xFF9AA4B5),
  outline = Color(0xFF253149),
  outlineVariant = Color(0xFF4A5A73)
)

private val SpotifusionLightColorScheme = lightColorScheme(
  primary = Color(0xFF6D42E8),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFE9E1FF),
  onPrimaryContainer = Color(0xFF28135E),
  secondary = Color(0xFF137FA8),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFDDF3FB),
  onSecondaryContainer = Color(0xFF0C4358),
  tertiary = Color(0xFF7355C8),
  onTertiary = Color.White,
  background = Color.White,
  onBackground = Color(0xFF111318),
  surface = Color(0xFFF7F8FA),
  onSurface = Color(0xFF111318),
  surfaceVariant = Color(0xFFF1F3F5),
  onSurfaceVariant = Color(0xFF555B65),
  outline = Color(0xFFD5D9DE),
  outlineVariant = Color(0xFFB8BEC6)
)

@Composable
fun SpotiFusionTheme(
  // Reference UI is dark AMOLED; keep dark as the default even on light-system devices.
  darkTheme: Boolean = true,
  content: @Composable () -> Unit
) {
  val view = LocalView.current
  val scheme = if (darkTheme) SpotifusionDarkColorScheme else SpotifusionLightColorScheme

  SideEffect {
    applyThemePalette(darkTheme)
    val window = (view.context as? Activity)?.window
    if (window != null) {
      window.statusBarColor = scheme.background.toArgb()
      window.navigationBarColor = scheme.background.toArgb()
      WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = !darkTheme
        isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
