package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// Reference palette: AMOLED black/navy surfaces with a violet primary accent.
var WarmBg by mutableStateOf(Color(0xFF05080F))
var WarmSurfaceNav by mutableStateOf(Color(0xFF0A0F18))
var WarmSurfaceCard by mutableStateOf(Color(0xFF0E1420))
var WarmSurfaceElevated by mutableStateOf(Color(0xFF131B29))
var WarmPrimary by mutableStateOf(Color(0xFF7B51FB))
var WarmOnPrimary by mutableStateOf(Color(0xFFFFFFFF))
var WarmPrimaryContainer by mutableStateOf(Color(0xFF24154F))
var WarmSecondaryContainer by mutableStateOf(Color(0xFF151D2B))
var WarmOnSecondaryContainer by mutableStateOf(Color(0xFFF4F5FA))
var WarmOutline by mutableStateOf(Color(0xFF253149))
var WarmOutlineVariant by mutableStateOf(Color(0x664A5A73))

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
var SpotifyGreenLight by mutableStateOf(Color(0xFFB9A7FF))
var SpotifyGreenDark by mutableStateOf(WarmPrimaryContainer)

var BgDark by mutableStateOf(WarmBg)
var SurfaceDark by mutableStateOf(WarmSurfaceNav)
var SurfaceCard by mutableStateOf(WarmSurfaceCard)
var SurfaceElevated by mutableStateOf(WarmSurfaceElevated)
var GlassSurface by mutableStateOf(Color(0xE60E1420))
var GlassBorder by mutableStateOf(WarmOutlineVariant)

var TextPrimary by mutableStateOf(Color(0xFFF6F7FB))
var TextSecondary by mutableStateOf(Color(0xFF9AA4B5))
var TextMuted by mutableStateOf(Color(0xFF687386))

val AccentError = Color(0xFFFF5B6E)
val AccentSuccess = Color(0xFF61D49A)
val AccentGold = Color(0xFFF0B94B)
val BrandCyan = Color(0xFF61C8FF)
val BrandPurple = Color(0xFF7B51FB)
val BrandCoral = Color(0xFFFF6D6D)

internal fun applyThemePalette(dark: Boolean) {
  if (dark) {
    WarmBg = Color(0xFF05080F)
    WarmSurfaceNav = Color(0xFF0A0F18)
    WarmSurfaceCard = Color(0xFF0E1420)
    WarmSurfaceElevated = Color(0xFF131B29)
    WarmPrimary = Color(0xFF7B51FB)
    WarmOnPrimary = Color(0xFFFFFFFF)
    WarmPrimaryContainer = Color(0xFF24154F)
    WarmSecondaryContainer = Color(0xFF151D2B)
    WarmOnSecondaryContainer = Color(0xFFF4F5FA)
    WarmOutline = Color(0xFF253149)
    WarmOutlineVariant = Color(0x664A5A73)
    TextPrimary = Color(0xFFF6F7FB)
    TextSecondary = Color(0xFF9AA4B5)
    TextMuted = Color(0xFF687386)
    SpotifyGreenLight = Color(0xFFB9A7FF)
    GlassSurface = Color(0xE60E1420)
  } else {
    WarmBg = Color(0xFFFFFFFF)
    WarmSurfaceNav = Color(0xFFF7F8FA)
    WarmSurfaceCard = Color(0xFFF1F3F5)
    WarmSurfaceElevated = Color(0xFFFFFFFF)
    WarmPrimary = Color(0xFF6D42E8)
    WarmOnPrimary = Color.White
    WarmPrimaryContainer = Color(0xFFE9E1FF)
    WarmSecondaryContainer = Color(0xFFE8EBEF)
    WarmOnSecondaryContainer = Color(0xFF16191D)
    WarmOutline = Color(0xFFD5D9DE)
    WarmOutlineVariant = Color(0xFFB8BEC6)
    TextPrimary = Color(0xFF111318)
    TextSecondary = Color(0xFF555B65)
    TextMuted = Color(0xFF777E88)
    SpotifyGreenLight = Color(0xFFD6CCFF)
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
