package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.EqualizerState
import com.example.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read app_name from context matches SpotiFusion`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SpotiFusion", appName)
  }

  @Test
  fun `track formatted duration returns correct minutes and seconds`() {
    val track = Track(
      id = "test_1",
      title = "Starfall",
      artist = "Nova",
      album = "Cosmic",
      durationSec = 214,
      coverUrl = "",
      genre = "Synthwave"
    )
    assertEquals("3:34", track.formattedDuration)
  }

  @Test
  fun `equalizer presets contain expected configurations`() {
    val flatPreset = EqualizerState.PRESETS["Flat"]
    assertNotNull(flatPreset)
    assertEquals(listOf(0f, 0f, 0f, 0f, 0f), flatPreset)

    val bassPreset = EqualizerState.PRESETS["Bass Boost"]
    assertNotNull(bassPreset)
    assertTrue(bassPreset!!.first() > 0f)
  }
}
