package com.example.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class ShakeDetector(
  private val context: Context,
  private val onShake: () -> Unit
) : SensorEventListener {

  private var sensorManager: SensorManager? = null
  private var accelerometer: Sensor? = null
  private var lastShakeTime = 0L
  private val shakeThreshold = 14.5f // Acceleration threshold (m/s^2)
  private val shakeIntervalMs = 1200L

  fun start() {
    try {
      sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
      accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
      accelerometer?.let {
        sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
      }
    } catch (_: Exception) {}
  }

  fun stop() {
    try {
      sensorManager?.unregisterListener(this)
    } catch (_: Exception) {}
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

    val x = event.values[0]
    val y = event.values[1]
    val z = event.values[2]

    // Calculate acceleration excluding Earth's gravity (~9.8 m/s^2)
    val gX = x / SensorManager.GRAVITY_EARTH
    val gY = y / SensorManager.GRAVITY_EARTH
    val gZ = z / SensorManager.GRAVITY_EARTH

    val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

    if (gForce > (shakeThreshold / SensorManager.GRAVITY_EARTH)) {
      val now = System.currentTimeMillis()
      if (now - lastShakeTime > shakeIntervalMs) {
        lastShakeTime = now
        onShake()
      }
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
