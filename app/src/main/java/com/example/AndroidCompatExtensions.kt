package com.example

import android.app.Activity
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.model.Track

/** Compatibility helpers for the current AI Studio Android build. */
fun requestPermissions(permissions: Array<String>, requestCode: Int) {
  val activity = currentForegroundActivity() ?: return
  ActivityCompat.requestPermissions(activity, permissions, requestCode)
}

private fun currentForegroundActivity(): Activity? = try {
  val activityThread = Class.forName("android.app.ActivityThread")
    .getMethod("currentActivityThread")
    .invoke(null)
  val activitiesField = activityThread.javaClass.getDeclaredField("mActivities")
  activitiesField.isAccessible = true
  @Suppress("UNCHECKED_CAST")
  val activities = activitiesField.get(activityThread) as Map<Any, Any>
  activities.values.asSequence()
    .mapNotNull { record ->
      runCatching {
        val activityField = record.javaClass.getDeclaredField("activity")
        activityField.isAccessible = true
        activityField.get(record) as? Activity
      }.getOrNull()
    }
    .firstOrNull { activity ->
      runCatching {
        val pausedField = activity.javaClass.getDeclaredField("mResumed")
        pausedField.isAccessible = true
        pausedField.getBoolean(activity)
      }.getOrDefault(!activity.isFinishing)
    }
} catch (_: Throwable) {
  null
}

/** Safe nullable Track id access for delegated Compose state. */
val Track?.id: String
  get() = this?.let { it.id } ?: ""
