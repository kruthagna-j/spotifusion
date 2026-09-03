package com.spotifusion.app

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : NeonActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        PlaybackController.connect(this)

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

        // Volume is now connected to the real Media3 player, not just UI state.
        val volume = SeekBar(this).apply {
            max = 100
            progress = 75
            progressTintList = ColorStateList.valueOf(Color.rgb(45, 91, 239))
            thumbTintList = ColorStateList.valueOf(Color.rgb(45, 91, 239))
            contentDescription = "Volume"
            isClickable = true
            isFocusable = true
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

    override fun onDestroy() {
        PlaybackController.disconnect()
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
