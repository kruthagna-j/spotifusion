package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Theme-aware semantic colors. UI components can keep using these names while
// SpotiFusionTheme swaps the values between the minimal dark and white themes.
var WarmBg by mutableStateOf(Color(0xFF07080B))
var WarmSurfaceNav by mutableStateOf(Color(0xFF0D0F14))
var WarmSurfaceCard by mutableStateOf(Color(0xFF11141B))
var WarmSurfaceElevated by mutableStateOf(Color(0xFF171A22))
var WarmPrimary by mutableStateOf(Color(0xFFB8FF3D))
var WarmOnPrimary by mutableStateOf(Color(0xFF0A0D08))
var WarmPrimaryContainer by mutableStateOf(Color(0xFF28351A))
var WarmSecondaryContainer by mutableStateOf(Color(0xFF1A2029))
var WarmOnSecondaryContainer by mutableStateOf(Color(0xFFF4F7FB))
var WarmOutline by mutableStateOf(Color(0xFF2A303B))
var WarmOutlineVariant by mutableStateOf(Color(0x664B5563))

var ImmersiveBg by mutableStateOf(WarmBg)
var ImmersiveSurfaceNav by mutableStateOf(WarmSurfaceNav)
var ImmersiveSurfaceCard by mutableStateOf(WarmSurfaceCard)
var ImmersiveSurfaceElevated by mutableStateOf(WarmSurfaceElevated)
var ImmersivePrimary by mutableStateOf(WarmPrimary)
var ImmersiveOnPrimary by mutableStateOf(WarmOnPrimary)
var ImmersivePrimaryContainer by mutableStateOf(WarmPrimaryContainer)
var ImmersiveSecondaryContainer by mutableStateOf(WarmSecondaryContainer)
var ImmersiveOnSecondaryContainer by mutableStateOf(WarmOnSecondaryContainer)
var ImmersiveOutline by mutableStateOf(WarmOutline)
var ImmersiveOutlineVariant by mutableStateOf(WarmOutlineVariant)

var SpotifyGreen by mutableStateOf(WarmPrimary)
var SpotifyGreenLight by mutableStateOf(Color(0xFFD7FFA0))
var SpotifyGreenDark by mutableStateOf(WarmPrimaryContainer)

var BgDark by mutableStateOf(WarmBg)
var SurfaceDark by mutableStateOf(WarmSurfaceNav)
var SurfaceCard by mutableStateOf(WarmSurfaceCard)
var SurfaceElevated by mutableStateOf(WarmSurfaceElevated)
var GlassSurface by mutableStateOf(Color(0xCC11141B))
var GlassBorder by mutableStateOf(WarmOutlineVariant)

var TextPrimary by mutableStateOf(Color(0xFFF5F7FA))
var TextSecondary by mutableStateOf(Color(0xFFA9B0BC))
var TextMuted by mutableStateOf(Color(0xFF737B88))

val AccentError = Color(0xFFFF5B5B)
val AccentSuccess = Color(0xFF55C66A)
val AccentGold = Color(0xFFE0A83B)
val BrandCyan = Color(0xFF3CC8FF)
val BrandPurple = Color(0xFF9B7CFF)
val BrandCoral = Color(0xFFFF7566)

internal fun applyThemePalette(dark: Boolean) {
  if (dark) {
    WarmBg = Color(0xFF07080B)
    WarmSurfaceNav = Color(0xFF0D0F14)
    WarmSurfaceCard = Color(0xFF11141B)
    WarmSurfaceElevated = Color(0xFF171A22)
    WarmPrimary = Color(0xFFB8FF3D)
    WarmOnPrimary = Color(0xFF0A0D08)
    WarmPrimaryContainer = Color(0xFF28351A)
    WarmSecondaryContainer = Color(0xFF1A2029)
    WarmOnSecondaryContainer = Color(0xFFF4F7FB)
    WarmOutline = Color(0xFF2A303B)
    WarmOutlineVariant = Color(0x664B5563)
    TextPrimary = Color(0xFFF5F7FA)
    TextSecondary = Color(0xFFA9B0BC)
    TextMuted = Color(0xFF737B88)
    SpotifyGreenLight = Color(0xFFD7FFA0)
    GlassSurface = Color(0xCC11141B)
  } else {
    WarmBg = Color(0xFFFFFFFF)
    WarmSurfaceNav = Color(0xFFF7F8FA)
    WarmSurfaceCard = Color(0xFFF1F3F5)
    WarmSurfaceElevated = Color(0xFFFFFFFF)
    WarmPrimary = Color(0xFF159447)
    WarmOnPrimary = Color(0xFFFFFFFF)
    WarmPrimaryContainer = Color(0xFFDDF7E5)
    WarmSecondaryContainer = Color(0xFFE8EBEF)
    WarmOnSecondaryContainer = Color(0xFF16191D)
    WarmOutline = Color(0xFFD5D9DE)
    WarmOutlineVariant = Color(0xFFB8BEC6)
    TextPrimary = Color(0xFF111318)
    TextSecondary = Color(0xFF555B65)
    TextMuted = Color(0xFF777E88)
    SpotifyGreenLight = Color(0xFFBDEBCB)
    GlassSurface = Color(0xFFF7F8FA)
  }

  ImmersiveBg = WarmBg
  ImmersiveSurfaceNav = WarmSurfaceNav
  ImmersiveSurfaceCard = WarmSurfaceCard
  ImmersiveSurfaceElevated = WarmSurfaceElevated
  ImmersivePrimary = WarmPrimary
  ImmersiveOnPrimary = WarmOnPrimary
  ImmersivePrimaryContainer = WarmPrimaryContainer
  ImmersiveSecondaryContainer = WarmSecondaryContainer
  ImmersiveOnSecondaryContainer = WarmOnSecondaryContainer
  ImmersiveOutline = WarmOutline
  ImmersiveOutlineVariant = WarmOutlineVariant
  SpotifyGreen = WarmPrimary
  SpotifyGreenDark = WarmPrimaryContainer
  BgDark = WarmBg
  SurfaceDark = WarmSurfaceNav
  SurfaceCard = WarmSurfaceCard
  SurfaceElevated = WarmSurfaceElevated
  GlassBorder = WarmOutlineVariant
}
