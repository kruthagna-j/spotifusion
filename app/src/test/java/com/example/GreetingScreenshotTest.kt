package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.Track
import com.example.ui.components.MiniPlayerBar
import com.example.ui.theme.SpotiFusionTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleTrack = Track(
      id = "shot_1",
      title = "Starfall Horizons",
      artist = "Astral Dreamer",
      album = "Cosmic Odyssey",
      durationSec = 214,
      coverUrl = "",
      genre = "Synthwave"
    )

    composeTestRule.setContent {
      SpotiFusionTheme {
        MiniPlayerBar(
          currentTrack = sampleTrack,
          isPlaying = true,
          currentPositionSec = 45,
          durationSec = 214,
          isLiked = true,
          onTogglePlayPause = {},
          onNextTrack = {},
          onToggleLike = {},
          onClickBar = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
