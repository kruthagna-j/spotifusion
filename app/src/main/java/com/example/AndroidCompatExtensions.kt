package com.example

import android.app.Activity
import androidx.core.app.ActivityCompat
import com.example.model.Track

/** Compatibility helpers for the current AI Studio Android build. */
fun Activity.requestPermissions(permissions: Array<String>, requestCode: Int) {
  ActivityCompat.requestPermissions(this, permissions, requestCode)
}

/** Safe nullable Track id access for delegated Compose state. */
val Track?.id: String
  get() = this?.let { it.id } ?: ""
