package com.spotifusion.app

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

/** Creates lightweight artwork using real device album art with a violet fallback. */
object ArtworkViewFactory {
    fun create(context: Context, track: LocalTrack?, sizePx: Int): FrameLayout {
        val box = FrameLayout(context)
        box.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(47, 29, 99), Color.rgb(123, 81, 251))
        ).apply { cornerRadius = 18f }
        box.clipToOutline = true
        box.outlineProvider = ViewOutlineProvider.BACKGROUND

        val fallback = TextView(context).apply {
            text = "♪"
            textSize = if (sizePx >= 180) 52f else 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        box.addView(fallback, FrameLayout.LayoutParams(-1, -1))

        if (track != null && track.albumId > 0L) {
            Thread {
                val bitmap = ArtworkResolver.load(context, track.albumId, 512)
                if (bitmap != null) {
                    box.post {
                        val image = ImageView(context).apply {
                            setImageBitmap(bitmap)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        box.addView(image, FrameLayout.LayoutParams(-1, -1))
                    }
                }
            }.start()
        }
        box.minimumWidth = sizePx
        box.minimumHeight = sizePx
        return box
    }
}
