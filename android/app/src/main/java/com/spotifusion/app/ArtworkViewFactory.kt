package com.spotifusion.app

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.content.Context

/** Creates a lightweight artwork view with a deterministic fallback. */
object ArtworkViewFactory {
    fun create(context: Context, track: LocalTrack?, sizePx: Int): FrameLayout {
        val box = FrameLayout(context)
        box.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(91, 73, 226), Color.rgb(60, 178, 235))
        ).apply { cornerRadius = 18f }

        val fallback = TextView(context).apply {
            text = "◉"
            textSize = 24f
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
                            clipToOutline = true
                            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
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
