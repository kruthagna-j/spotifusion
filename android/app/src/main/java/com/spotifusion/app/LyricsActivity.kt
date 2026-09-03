package com.spotifusion.app

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

class LyricsActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var lyricsView: LinearLayout
    private var lines: List<LyricsRepository.Line> = emptyList()
    private var ticker: Runnable? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = Color.rgb(246, 247, 252)
        window.navigationBarColor = Color.rgb(246, 247, 252)
        lyricsView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(24), dp(22), dp(32))
            setBackgroundColor(Color.rgb(246, 247, 252))
        }
        setContentView(lyricsView)
        load()
    }

    private fun load() {
        val title = PlaybackController.currentTitle()
        val artist = PlaybackController.currentArtist()
        val album = PlaybackController.currentAlbum()
        titleBar("Lyrics", title, artist)
        if (title.isBlank()) {
            message("Play a song to view its lyrics.")
            return
        }
        message("Loading lyrics…")
        executor.execute {
            val result = runCatching {
                LyricsRepository.fetch(title, artist, album, (PlaybackController.duration() / 1000L).toInt())
            }.getOrNull()
            handler.post {
                lyricsView.removeAllViews()
                titleBar("Lyrics", title, artist)
                if (result == null || (result.lines.isEmpty() && result.plain.isBlank())) {
                    message("Lyrics aren't available for this song yet.")
                    return@post
                }
                lines = result.lines
                if (lines.isNotEmpty()) {
                    lines.forEachIndexed { index, line ->
                        val view = TextView(this).apply {
                            text = line.text.ifBlank { "♪" }
                            textSize = 19f
                            setTextColor(Color.rgb(111, 114, 128))
                            gravity = Gravity.CENTER
                            setPadding(dp(4), dp(11), dp(4), dp(11))
                            tag = index
                        }
                        lyricsView.addView(view)
                    }
                    startSync()
                } else {
                    message(result.plain)
                }
            }
        }
    }

    private fun startSync() {
        ticker?.let(handler::removeCallbacks)
        ticker = object : Runnable {
            override fun run() {
                val position = PlaybackController.position()
                val active = lines.indexOfLast { it.timeMs <= position }
                for (i in 1 until lyricsView.childCount) {
                    val view = lyricsView.getChildAt(i) as TextView
                    val selected = (view.tag as? Int) == active
                    view.setTextColor(if (selected) Color.rgb(45, 91, 239) else Color.rgb(111, 114, 128))
                    view.textSize = if (selected) 22f else 19f
                    if (selected) view.setTypeface(view.typeface, android.graphics.Typeface.BOLD)
                }
                handler.postDelayed(this, 250L)
            }
        }
        handler.post(ticker!!)
    }

    private fun titleBar(title: String, song: String, artist: String) {
        lyricsView.addView(TextView(this).apply {
            text = title
            textSize = 28f
            setTextColor(Color.rgb(24, 24, 30))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        lyricsView.addView(TextView(this).apply {
            text = "$song · $artist"
            textSize = 12f
            setTextColor(Color.rgb(111, 114, 128))
            setPadding(0, dp(6), 0, dp(22))
        })
    }

    private fun message(text: String) {
        lyricsView.addView(TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.rgb(111, 114, 128))
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(40), dp(8), dp(40))
        })
    }

    override fun onDestroy() {
        ticker?.let(handler::removeCallbacks)
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
}
