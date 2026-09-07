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
  primary = Color(0xFFB8FF3D),
  onPrimary = Color(0xFF0A0D08),
  primaryContainer = Color(0xFF28351A),
  onPrimaryContainer = Color(0xFFD7FFA0),
  secondary = Color(0xFF3CC8FF),
  onSecondary = Color(0xFF061018),
  secondaryContainer = Color(0xFF1A2029),
  onSecondaryContainer = Color(0xFFF4F7FB),
  tertiary = Color(0xFF9B7CFF),
  onTertiary = Color(0xFF100A20),
  background = Color(0xFF07080B),
  onBackground = Color(0xFFF5F7FA),
  surface = Color(0xFF0D0F14),
  onSurface = Color(0xFFF5F7FA),
  surfaceVariant = Color(0xFF11141B),
  onSurfaceVariant = Color(0xFFA9B0BC),
  outline = Color(0xFF2A303B),
  outlineVariant = Color(0xFF4B5563)
)

private val SpotifusionLightColorScheme = lightColorScheme(
  primary = Color(0xFF159447),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFDDF7E5),
  onPrimaryContainer = Color(0xFF0B4D25),
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
  darkTheme: Boolean = isSystemInDarkTheme(),
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

  MaterialTheme(
    colorScheme = scheme,
    typography = Typography,
    content = content
  )
}
