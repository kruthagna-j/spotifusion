package com.spotifusion.app

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : NeonActivity() {
    companion object {
        private const val AUDIO_PERMISSION_REQUEST = 4101
        private const val NOTIFICATION_PERMISSION_REQUEST = 4102
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        PlaybackController.connect(this)
        requestMusicPermissions()

        val content = findViewById<ViewGroup>(android.R.id.content)
        val shell = content.getChildAt(0)
        shell?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bars.bottom)
                insets
            }
            ViewCompat.requestApplyInsets(root)
        }

        val volume = SeekBar(this).apply {
            max = 100
            progress = 75
            progressTintList = ColorStateList.valueOf(Color.rgb(45, 91, 239))
            thumbTintList = ColorStateList.valueOf(Color.rgb(45, 91, 239))
            contentDescription = "Volume"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) PlaybackController.setVolume(value / 100f)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(34),
            Gravity.BOTTOM
        ).apply {
            leftMargin = dp(82)
            rightMargin = dp(82)
            bottomMargin = dp(88)
        }
        content.addView(volume, lp)
    }

    private fun requestMusicPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), AUDIO_PERMISSION_REQUEST)
        } else {
            scanLocalMusic()
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQUEST) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) scanLocalMusic()
            else Toast.makeText(this, "Music access is needed to show songs stored on this device.", Toast.LENGTH_LONG).show()
        }
    }

    private fun scanLocalMusic() {
        val tracks = runCatching { LocalMusicScanner.scan(this) }.getOrElse { emptyList() }
        if (tracks.isNotEmpty()) {
            Toast.makeText(this, "Found ${tracks.size} music track${if (tracks.size == 1) "" else "s"} on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        PlaybackController.disconnect()
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
