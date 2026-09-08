package com.example

import android.app.Activity
import androidx.core.app.ActivityCompat
import com.example.model.Track

/** Compatibility helpers for the Compose/Activity API surface used by the AI Studio project. */
fun Activity.requestPermissions(permissions: Array<String>, requestCode: Int) {
  ActivityCompat.requestPermissions(this, permissions, requestCode)
}

/** Allows safe access from delegated nullable Track state in older Kotlin smart-cast contexts. */
val Track?.id: String
  get() = this?.let { it.id } ?: ""
