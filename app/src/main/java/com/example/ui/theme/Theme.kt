package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WarmLightColorScheme = lightColorScheme(
  primary = WarmPrimary,
  onPrimary = WarmOnPrimary,
  primaryContainer = WarmPrimaryContainer,
  onPrimaryContainer = WarmOnSecondaryContainer,
  secondary = WarmSecondaryContainer,
  onSecondary = WarmOnSecondaryContainer,
  secondaryContainer = WarmSecondaryContainer,
  onSecondaryContainer = WarmOnSecondaryContainer,
  tertiary = WarmOnSecondaryContainer,
  onTertiary = WarmOnPrimary,
  background = WarmBg,
  onBackground = TextPrimary,
  surface = WarmSurfaceNav,
  onSurface = TextPrimary,
  surfaceVariant = WarmSurfaceCard,
  onSurfaceVariant = TextSecondary,
  outline = WarmOutline,
  outlineVariant = WarmOutlineVariant
)

@Composable
fun SpotiFusionTheme(
  darkTheme: Boolean = false,
  content: @Composable () -> Unit
) {
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = BgDark.toArgb()
        window.navigationBarColor = BgDark.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
          isAppearanceLightStatusBars = true
          isAppearanceLightNavigationBars = true
        }
      }
    }
  }

  MaterialTheme(
    colorScheme = WarmLightColorScheme,
    typography = Typography,
    content = content
  )
}
