package com.spotifusion.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SavedTracksActivity : AppCompatActivity() {
    private lateinit var store: UserMusicStore
    private lateinit var content: LinearLayout
    private var mode = "favorites"

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        store = UserMusicStore(this)
        mode = intent.getStringExtra("mode") ?: "favorites"
        build()
        render()
    }

    private fun build() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24,24,24,16); setBackgroundColor(Color.rgb(246,247,252)) }
        root.addView(TextView(this).apply {
            text = if (mode == "recent") "Recently Played" else "Favorites"
            textSize = 24f; setTextColor(Color.rgb(24,24,30)); setTypeface(typeface,1); setPadding(0,0,0,18)
        })
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(content) }, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
    }

    private fun render() {
        content.removeAllViews()
        val tracks = if (mode == "recent") store.recentlyPlayed() else {
            val ids = store.favoriteIds()
            runCatching { LocalMusicScanner.scan(this).filter { it.id in ids } }.getOrElse { emptyList() }
        }
        if (tracks.isEmpty()) {
            content.addView(TextView(this).apply { text = if (mode == "recent") "No recently played songs" else "No favorite songs yet"; textSize = 15f; setTextColor(Color.DKGRAY); gravity = Gravity.CENTER; setPadding(0,40,0,40) })
            return
        }
        tracks.forEach { track ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(12,10,8,10); setBackgroundColor(Color.WHITE) }
            val info = TextView(this).apply { text = "${track.title}\n${track.artist}"; textSize = 14f; setTextColor(Color.rgb(24,24,30)) }
            row.addView(info, LinearLayout.LayoutParams(0,68,1f))
            row.addView(Button(this).apply { text = "PLAY"; setOnClickListener { PlaybackController.playTracks(listOf(track)); finish() } })
            content.addView(row, LinearLayout.LayoutParams(-1,82).apply { bottomMargin = 8 })
        }
    }
}
