package com.spotifusion.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.graphics.drawable.GradientDrawable

open class NeonActivity : AppCompatActivity() {
    private val bg = Color.rgb(5, 7, 13)
    private val panel = Color.rgb(12, 16, 27)
    private val cyan = Color.rgb(0, 229, 255)
    private val pink = Color.rgb(255, 55, 190)
    private val purple = Color.rgb(145, 80, 255)
    private val white = Color.WHITE
    private val muted = Color.rgb(145, 155, 180)
    private lateinit var content: FrameLayout
    private var currentTab = 0

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun box(color: Int, radius: Int = 18) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun tv(s: String, size: Float, color: Int = white, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); includeFontPadding = false
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildShell()
        home()
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        content = FrameLayout(this).apply { setBackgroundColor(bg) }
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(miniPlayer(), LinearLayout.LayoutParams(-1, dp(64)).apply { setMargins(dp(10), 0, dp(10), dp(8)) })
        root.addView(bottomNav(), LinearLayout.LayoutParams(-1, dp(72)))
        setContentView(root)
    }

    private fun bottomNav() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setBackgroundColor(Color.rgb(8,10,18))
        arrayOf("⌂" to "Home", "⌕" to "Search", "♫" to "Library", "⚙" to "Settings").forEachIndexed { i, p ->
            val b = LinearLayout(this@NeonActivity).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
                setOnClickListener { currentTab = i; when (i) { 0 -> home(); 1 -> search(); 2 -> library(); else -> settings() } }
            }
            b.addView(tv(p.first, 23f, if (i == currentTab) cyan else muted).apply { gravity = Gravity.CENTER })
            b.addView(tv(p.second, 10f, if (i == currentTab) cyan else muted, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(5), 0, 0) })
            addView(b, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }

    private fun miniPlayer() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(6), dp(8), dp(6)); background = box(panel, 16)
        addView(tv("◉", 28f, cyan, true), LinearLayout.LayoutParams(dp(45), -1))
        val x = LinearLayout(this@NeonActivity).apply { orientation = LinearLayout.VERTICAL }
        x.addView(tv("Nothing playing", 13f, white, true)); x.addView(tv("Choose a track to start", 10f, muted))
        addView(x, LinearLayout.LayoutParams(0, -1, 1f)); addView(tv("▶", 25f, cyan, true), LinearLayout.LayoutParams(dp(45), -1))
    }

    private fun base() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(18), dp(20), dp(18)) }
    private fun header(c: LinearLayout, title: String, sub: String) { c.addView(tv(title, 28f, white, true)); c.addView(tv(sub, 12f, muted).apply { setPadding(0, dp(5), 0, dp(18)) }) }
    private fun card(c: LinearLayout, title: String, sub: String, accent: Int) {
        val v = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(15), dp(16), dp(15)); background = box(panel, 18) }
        v.addView(tv(title, 16f, white, true)); v.addView(tv(sub, 11f, muted).apply { setPadding(0, dp(6), 0, 0) })
        v.addView(View(this).apply { setBackgroundColor(accent); alpha = .8f }, LinearLayout.LayoutParams(dp(42), dp(2)).apply { topMargin = dp(12) })
        c.addView(v, LinearLayout.LayoutParams(-1, dp(88)).apply { bottomMargin = dp(12) })
    }

    private fun home() {
        currentTab = 0; val c = base(); header(c, "SPOTIFUSION", "NEON AUDIO SYSTEM  •  READY")
        val hero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(18), dp(20), dp(18), dp(20)); background = box(Color.rgb(10,14,25), 24) }
        hero.addView(tv("◉", 82f, cyan, true)); hero.addView(tv("FUSION PLAYER", 22f, white, true).apply { setPadding(0, dp(8), 0, 0) }); hero.addView(tv("Your music. Reimagined.", 12f, muted).apply { setPadding(0, dp(5), 0, dp(10)) })
        hero.addView(tv("  ▶  PLAY MUSIC  ", 13f, Color.BLACK, true).apply { gravity = Gravity.CENTER; background = box(cyan, 22); setPadding(dp(12), dp(10), dp(12), dp(10)); setOnClickListener { library() } }, LinearLayout.LayoutParams(-2, dp(44)))
        c.addView(hero, LinearLayout.LayoutParams(-1, dp(255))); c.addView(tv("QUICK ACCESS", 16f, white, true).apply { setPadding(0, dp(20), 0, dp(10)) })
        card(c, "ALL SONGS", "Browse your complete music collection", cyan); card(c, "FAVORITES", "Your starred tracks", pink); card(c, "EQUALIZER", "Shape the sound your way", purple); show(c)
    }

    private fun search() { currentTab = 1; val c = base(); header(c, "SEARCH", "FIND YOUR NEXT TRACK"); val e = EditText(this).apply { hint = "Search songs, artists, albums..."; setHintTextColor(muted); setTextColor(white); textSize = 14f; setSingleLine(true); setPadding(dp(18), 0, dp(18), 0); background = box(panel, 22) }; c.addView(e, LinearLayout.LayoutParams(-1, dp(52))); c.addView(tv("DISCOVER", 16f, white, true).apply { setPadding(0, dp(22), 0, dp(10)) }); card(c, "NEON HITS", "Trending sounds and new releases", cyan); card(c, "LOCAL MUSIC", "Music available on your device", purple); card(c, "RECENT SEARCHES", "Your latest discoveries", pink); show(c) }
    private fun library() { currentTab = 2; val c = base(); header(c, "LIBRARY", "YOUR PERSONAL MUSIC SPACE"); card(c, "ALL SONGS", "Your complete collection", cyan); card(c, "FAVORITES", "Your starred tracks", pink); card(c, "PLAYLISTS", "Organize your music", purple); card(c, "OFFLINE MUSIC", "Available without internet", cyan); show(c) }
    private fun settings() { currentTab = 3; val c = base(); header(c, "SETTINGS", "CONTROL YOUR EXPERIENCE"); card(c, "PLAYBACK", "Shuffle • Repeat • Sleep timer", cyan); card(c, "EQUALIZER", "5-band sound controls and presets", pink); card(c, "APPEARANCE", "Neon interface and visualizer", purple); card(c, "STORAGE", "Offline music and cache", cyan); card(c, "ABOUT SPOTIFUSION", "Version 1.0 • Built for music lovers", pink); show(c) }
    private fun show(c: LinearLayout) { content.removeAllViews(); content.addView(ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(c) }) }
}
