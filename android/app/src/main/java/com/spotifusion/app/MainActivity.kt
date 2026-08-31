package com.spotifusion.app

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : NeonActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

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

        // Always-visible volume control for the compact player. It sits above
        // the bottom navigation so it cannot be hidden by the gesture/system bar.
        val volume = SeekBar(this).apply {
            max = 100
            progress = 75
            progressTintList = ColorStateList.valueOf(Color.rgb(45, 91, 239))
            thumbTintList = ColorStateList.valueOf(Color.rgb(45, 91, 239))
            contentDescription = "Volume"
            isClickable = true
            isFocusable = true
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

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
