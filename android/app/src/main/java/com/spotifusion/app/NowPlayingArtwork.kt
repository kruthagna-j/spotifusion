package com.spotifusion.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageView

/** Reusable Now Playing artwork surface. Keeps artwork local to the device. */
object NowPlayingArtwork {
    fun create(context: Context, track: LocalTrack?, sizeDp: Int = 274): FrameLayout {
        val density = context.resources.displayMetrics.density
        val px = (sizeDp * density + 0.5f).toInt()
        val container = FrameLayout(context).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(91, 73, 226), Color.rgb(60, 178, 235))).apply {
                cornerRadius = 9999f
            }
            elevation = 6f * density
        }
        val image = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            clipToOutline = true
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
        }
        container.addView(image, FrameLayout.LayoutParams(px, px, Gravity.CENTER))
        if (track != null) load(context, image, track)
        return container
    }

    private fun load(context: Context, image: ImageView, track: LocalTrack) {
        Thread {
            val bitmap: Bitmap? = ArtworkResolver.load(context, track.albumId)
            image.post {
                if (bitmap != null) {
                    image.background = BitmapDrawable(context.resources, bitmap)
                    image.setImageBitmap(bitmap)
                } else {
                    image.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            }
        }.start()
    }
}
