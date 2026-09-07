package com.example.model

enum class RepeatMode {
  OFF, ALL, ONE
}

data class PlayerState(
  val currentTrack: Track? = null,
  val isPlaying: Boolean = false,
  val isBuffering: Boolean = false,
  val currentPositionSec: Int = 0,
  val durationSec: Int = 0,
  val queue: List<Track> = emptyList(),
  val queueIndex: Int = 0,
  val isShuffled: Boolean = false,
  val repeatMode: RepeatMode = RepeatMode.ALL,
  val volume: Float = 0.85f,
  val visualizerBars: List<Float> = List(32) { 0.08f }
)
