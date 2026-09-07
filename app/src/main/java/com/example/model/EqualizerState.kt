package com.example.model

data class EqualizerState(
  val isEnabled: Boolean = true,
  val activePreset: String = "Bass Boost",
  // 5 bands: 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz. Range: -12.0f to +12.0f dB
  val bandGains: List<Float> = listOf(6f, 4f, 0f, 2f, 3f),
  val bassBoost: Float = 0.75f, // 0.0f to 1.0f
  val virtualizer: Float = 0.50f, // 0.0f to 1.0f
  val loudness: Float = 0.65f // 0.0f to 1.0f
) {
  fun setPreset(presetName: String): EqualizerState {
    val gains = PRESETS[presetName] ?: bandGains
    return copy(activePreset = presetName, bandGains = gains)
  }

  fun updateBandGain(bandIndex: Int, gainDb: Float): EqualizerState {
    val updatedGains = bandGains.toMutableList()
    if (bandIndex in updatedGains.indices) {
      updatedGains[bandIndex] = gainDb.coerceIn(-12f, 12f)
    }
    return copy(bandGains = updatedGains, activePreset = "Custom")
  }

  companion object {
    val FREQUENCIES = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
    val PRESETS = mapOf(
      "Flat" to listOf(0f, 0f, 0f, 0f, 0f),
      "Bass Boost" to listOf(7f, 5f, 0f, 1f, 2f),
      "Vocal Booster" to listOf(-2f, 1f, 6f, 4f, -1f),
      "Electronic" to listOf(5f, 3f, -1f, 2f, 6f),
      "Rock" to listOf(6f, 3f, -2f, 4f, 5f),
      "Acoustic" to listOf(4f, 2f, 3f, 4f, 2f),
      "Jazz" to listOf(3f, 2f, -1f, 3f, 4f)
    )
  }
}
