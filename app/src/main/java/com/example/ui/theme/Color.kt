package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Warm Light Theme Palette
val WarmBg = Color(0xFFFAEEDA)
val WarmSurfaceNav = Color(0xFFFFFFFF)
val WarmSurfaceCard = Color(0xFFFFFFFF)
val WarmSurfaceElevated = Color(0xFFFFF7EC)
val WarmPrimary = Color(0xFFD85A30)
val WarmOnPrimary = Color(0xFFFFFFFF)
val WarmPrimaryContainer = Color(0xFFF5C4B3)
val WarmSecondaryContainer = Color(0xFFFAC775)
val WarmOnSecondaryContainer = Color(0xFF412402)
val WarmOutline = Color(0xFFE8DCC4)
val WarmOutlineVariant = Color(0x33D85A30)

// Original "Immersive" identifiers kept as aliases (same names, new warm-light
// values) so every existing reference across the app keeps compiling.
// These were previously a dark palette; they now point at the warm palette above.
val ImmersiveBg = WarmBg
val ImmersiveSurfaceNav = WarmSurfaceNav
val ImmersiveSurfaceCard = WarmSurfaceCard
val ImmersiveSurfaceElevated = WarmSurfaceElevated
val ImmersivePrimary = WarmPrimary
val ImmersiveOnPrimary = WarmOnPrimary
val ImmersivePrimaryContainer = WarmPrimaryContainer
val ImmersiveSecondaryContainer = WarmSecondaryContainer
val ImmersiveOnSecondaryContainer = WarmOnSecondaryContainer
val ImmersiveOutline = WarmOutline
val ImmersiveOutlineVariant = WarmOutlineVariant

// Primary Accent Aliases
val SpotifyGreen = ImmersivePrimary
val SpotifyGreenLight = ImmersiveOnSecondaryContainer
val SpotifyGreenDark = ImmersivePrimaryContainer

val BrandCyan = Color(0xFF378ADD)
val BrandPurple = Color(0xFF7F77DD)
val BrandCoral = WarmPrimary

// Surfaces & Backgrounds
val BgDark = ImmersiveBg
val SurfaceDark = ImmersiveSurfaceNav
val SurfaceCard = ImmersiveSurfaceCard
val SurfaceElevated = ImmersiveSurfaceElevated
val GlassSurface = Color(0xE6FFFFFF)
val GlassBorder = ImmersiveOutlineVariant

// Text Colors
val TextPrimary = Color(0xFF412402)
val TextSecondary = Color(0xFF6B4A2E)
val TextMuted = Color(0xFF9C8064)

// Status & Indicators
val AccentError = Color(0xFFA32D2D)
val AccentSuccess = Color(0xFF3B6D11)
val AccentGold = Color(0xFFEF9F27)
