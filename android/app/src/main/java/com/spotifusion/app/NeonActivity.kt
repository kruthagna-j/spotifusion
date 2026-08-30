package com.spotifusion.app

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity

open class NeonActivity : AppCompatActivity() {
    private val bg = Color.rgb(244, 244, 245)
    private val ink = Color.rgb(24, 24, 27)
    private val muted = Color.rgb(113, 113, 122)
    private val blue = Color.rgb(37, 99, 235)
    private val white = Color.WHITE
    private lateinit var content: FrameLayout
    private var currentTab = 0

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun color(a: Int, r: Int, g: Int, b: Int) = Color.argb(a, r, g, b)
    private fun rounded(c: Int, radius: Int = 18) = GradientDrawable().apply {
        setColor(c)
        cornerRadius = dp(radius).toFloat()
    }
    private fun text(value: String, size: Float, c: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(c)
        includeFontPadding = false
        if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        buildShell()
        home()
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        content = FrameLayout(this)
        content.setBackgroundColor(bg)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(miniPlayer(), LinearLayout.LayoutParams(-1, dp(72)).apply {
            setMargins(dp(12), 0, dp(12), dp(8))
        })
        root.addView(bottomNav(), LinearLayout.LayoutParams(-1, dp(76)))
        setContentView(root)
    }

    private fun bottomNav() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(8), dp(6), dp(8), dp(8))
        background = rounded(color(220, 255, 255, 255), 22)

        val items = arrayOf("⌂" to "Home", "⌕" to "Search", "♫" to "Library", "≋" to "Equalizer", "⚙" to "Settings")
        items.forEachIndexed { i, item ->
            val selected = i == currentTab
            val holder = LinearLayout(this@NeonActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(5), dp(5), dp(5), dp(4))
                if (selected) background = rounded(color(28, 37, 99, 235), 16)
                setOnClickListener {
                    currentTab = i
                    when (i) {
                        0 -> home()
                        1 -> search()
                        2 -> library()
                        3 -> equalizer()
                        else -> settings()
                    }
                }
            }
            holder.addView(text(item.first, 22f, if (selected) blue else muted, true))
            holder.addView(text(item.second, 9f, if (selected) blue else muted, true).apply {
                setPadding(0, dp(3), 0, 0)
            })
            addView(holder, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }

    private fun miniPlayer() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10), dp(8), dp(8), dp(8))
        background = rounded(color(235, 255, 255, 255), 20)

        val art = FrameLayout(this@NeonActivity).apply {
            background = rounded(blue, 14)
            addView(text("◉", 25f, white, true).apply { gravity = Gravity.CENTER })
        }
        addView(art, LinearLayout.LayoutParams(dp(50), dp(50)))

        val info = LinearLayout(this@NeonActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
            setOnClickListener { nowPlaying() }
        }
        info.addView(text("Nothing playing", 14f, ink, true))
        info.addView(text("Choose a track to start", 11f, muted))
        addView(info, LinearLayout.LayoutParams(0, -1, 1f))

        addView(text("▶", 22f, blue, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { nowPlaying() }
        }, LinearLayout.LayoutParams(dp(44), dp(50)))
    }

    private fun page() = ScrollView(this).apply {
        isVerticalScrollBarEnabled = false
        setPadding(0, 0, 0, dp(8))
    }

    private fun column() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
    }

    private fun heading(c: LinearLayout, title: String, subtitle: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(text(title, 25f, ink, true))
        titles.addView(text(subtitle, 12f, muted).apply { setPadding(0, dp(5), 0, 0) })
        row.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text("•", 24f, blue, true))
        c.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(18) })
    }

    private fun glassCard(c: LinearLayout, title: String, subtitle: String, icon: String = "♪", accent: Int = blue, click: (() -> Unit)? = null) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(color(215, 255, 255, 255), 18)
            if (click != null) setOnClickListener { click() }
        }
        val iconBox = FrameLayout(this).apply {
            background = rounded(color(32, accent.red, accent.green, accent.blue), 14)
            addView(text(icon, 22f, accent, true).apply { gravity = Gravity.CENTER })
        }
        card.addView(iconBox, LinearLayout.LayoutParams(dp(48), dp(48)))
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
        }
        info.addView(text(title, 14f, ink, true))
        info.addView(text(subtitle, 11f, muted).apply { setPadding(0, dp(4), 0, 0) })
        card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(text("›", 28f, muted))
        c.addView(card, LinearLayout.LayoutParams(-1, dp(74)).apply { bottomMargin = dp(10) })
    }

    private fun hero(c: LinearLayout) {
        val h = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(20))
            background = rounded(blue, 24)
        }
        h.addView(text("FEATURED GLASS SESSION", 10f, color(230,255,255,255), true))
        h.addView(text("Nordic Resonance", 27f, white, true).apply { setPadding(0, dp(8), 0, 0) })
        h.addView(text("Immersive frequencies and frosted vinyl tones.", 12f, color(220,225,235,255)).apply { setPadding(0, dp(5), 0, dp(16)) })
        h.addView(text("  ▶   LISTEN NOW  ", 12f, blue, true).apply {
            gravity = Gravity.CENTER
            background = rounded(white, 18)
            setPadding(dp(8), dp(11), dp(8), dp(11))
            setOnClickListener { nowPlaying() }
        }, LinearLayout.LayoutParams(-2, dp(44)))
        c.addView(h, LinearLayout.LayoutParams(-1, dp(190)).apply { bottomMargin = dp(18) })
    }

    private fun home() {
        currentTab = 0
        val c = column()
        heading(c, "Good morning, Kruthagna", "Your Nordic music space")
        hero(c)
        c.addView(text("RECENTLY PLAYED", 11f, muted, true).apply { setPadding(0, 0, 0, dp(10)) })
        glassCard(c, "Glass Architecture", "Astral Pulse · 3:45", "◉", blue) { nowPlaying() }
        glassCard(c, "Obsidian & Light", "Kaelen Voss · 4:12", "◉", Color.rgb(99,102,241)) { nowPlaying() }
        glassCard(c, "Frictionless Orbit", "Sora & The Echoes · 2:58", "◉", Color.rgb(245,158,11)) { nowPlaying() }
        c.addView(text("MADE FOR YOU", 11f, muted, true).apply { setPadding(0, dp(12), 0, dp(10)) })
        glassCard(c, "Daily Mix 1", "Glass · Ambient · Electronic", "✦", blue) { library() }
        glassCard(c, "Chill Vibes", "Lofi · Instrumental", "◌", Color.rgb(99,102,241)) { library() }
        show(c)
    }

    private fun search() {
        currentTab = 1
        val c = column()
        heading(c, "Search", "Artists, tracks, albums and more")
        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            background = rounded(color(220,255,255,255), 20)
        }
        searchBox.addView(text("⌕", 25f, muted, true))
        searchBox.addView(EditText(this).apply {
            hint = "Search your music"
            hintTextColor = muted
            setTextColor(ink)
            textSize = 14f
            setSingleLine(true)
            background = null
            setPadding(dp(10), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, dp(54), 1f))
        c.addView(searchBox, LinearLayout.LayoutParams(-1, dp(54)).apply { bottomMargin = dp(18) })
        c.addView(text("RECENT SEARCHES", 11f, muted, true).apply { setPadding(0, 0, 0, dp(10)) })
        arrayOf("Gramophone Acoustics", "Nordic Synth", "Astral Pulse", "5-Band Preset").forEach {
            val chip = text("  $it  ", 12f, ink, true).apply {
                background = rounded(color(220,255,255,255), 16)
                setPadding(dp(5), dp(10), dp(5), dp(10))
            }
            c.addView(chip, LinearLayout.LayoutParams(-2, dp(38)).apply { bottomMargin = dp(8) })
        }
        c.addView(text("BROWSE", 11f, muted, true).apply { setPadding(0, dp(14), 0, dp(10)) })
        glassCard(c, "Songs", "Search individual tracks", "♪", blue)
        glassCard(c, "Artists", "Explore artists", "♬", Color.rgb(99,102,241))
        glassCard(c, "Albums", "Discover albums", "▣", Color.rgb(14,165,233))
        glassCard(c, "Playlists", "Find playlists and mixes", "≡", Color.rgb(245,158,11))
        show(c)
    }

    private fun library() {
        currentTab = 2
        val c = column()
        heading(c, "Your Library", "Everything you saved in one place")
        glassCard(c, "Liked Songs", "428 tracks", "♥", blue) { nowPlaying() }
        glassCard(c, "Chill Vibes & Glass", "32 songs", "◉", Color.rgb(14,165,233)) { nowPlaying() }
        glassCard(c, "Nordic Morning Ambient", "18 songs", "◉", Color.rgb(99,102,241)) { nowPlaying() }
        glassCard(c, "Local Music", "Music on this device", "▣", Color.rgb(245,158,11))
        show(c)
    }

    private fun equalizer() {
        currentTab = 3
        val c = column()
        heading(c, "Audio Equalizer", "Precision 5-band frequency shaping")
        glassCard(c, "Nordic Flat", "Balanced reference sound", "≋", blue)
        glassCard(c, "Acoustic Glass", "Warm acoustic presence", "♫", Color.rgb(14,165,233))
        glassCard(c, "Deep Bass", "Low-end emphasis", "≈", Color.rgb(99,102,241))
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(18), dp(14), dp(18))
            background = rounded(color(215,255,255,255), 20)
        }
        arrayOf("60", "230", "910", "3.6k", "14k").forEach { freq ->
            val band = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            band.addView(text("+0", 10f, muted, true))
            band.addView(SeekBar(this).apply {
                max = 24
                progress = 12
                rotation = -90f
            }, LinearLayout.LayoutParams(dp(80), dp(50)))
            band.addView(text(freq, 10f, ink, true).apply { setPadding(0, dp(4), 0, 0) })
            panel.addView(band, LinearLayout.LayoutParams(0, dp(150), 1f))
        }
        c.addView(panel)
        show(c)
    }

    private fun settings() {
        currentTab = 4
        val c = column()
        heading(c, "Settings", "Configure your Spotifusion experience")
        glassCard(c, "High Fidelity Audio", "Best available streaming quality", "◉", blue)
        glassCard(c, "Dynamic Glass", "Ambient visual effects", "✧", Color.rgb(14,165,233))
        glassCard(c, "Shake to Change", "Change track by shaking your phone", "↯", Color.rgb(99,102,241))
        glassCard(c, "Storage", "Offline songs and cached artwork", "▣", Color.rgb(245,158,11))
        glassCard(c, "Account", "Profile and connected services", "◎", blue)
        glassCard(c, "About Spotifusion", "Version 1.0", "ⓘ", Color.rgb(113,113,122))
        show(c)
    }

    private fun nowPlaying() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(22), dp(20), dp(22))
            setBackgroundColor(bg)
        }
        root.addView(text("NOW PLAYING", 11f, muted, true))
        root.addView(text("Glass Architecture", 23f, ink, true).apply { setPadding(0, dp(10), 0, 0) })
        root.addView(text("Astral Pulse · Nordic Studio Sessions", 12f, muted).apply { setPadding(0, dp(4), 0, dp(18)) })

        val record = FrameLayout(this).apply {
            background = rounded(Color.rgb(24,24,27), 200)
            elevation = dp(8).toFloat()
            addView(text("SPOTIFUSION", 11f, white, true).apply { gravity = Gravity.CENTER })
            addView(text("•", 22f, blue, true).apply { gravity = Gravity.CENTER })
        }
        root.addView(record, LinearLayout.LayoutParams(dp(290), dp(290)).apply { bottomMargin = dp(24) })

        root.addView(SeekBar(this).apply { max = 100; progress = 42 }, LinearLayout.LayoutParams(-1, dp(36)))
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(text("1:28", 10f, muted), LinearLayout.LayoutParams(0, -1, 1f))
            addView(text("3:45", 10f, muted).apply { gravity = Gravity.RIGHT })
        }, LinearLayout.LayoutParams(-1, dp(24)))

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        controls.addView(text("⤨", 22f, muted, true), LinearLayout.LayoutParams(dp(50), dp(60)))
        controls.addView(text("‹", 36f, ink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(60), dp(60)))
        controls.addView(text("▶", 28f, white, true).apply {
            gravity = Gravity.CENTER
            background = rounded(blue, 20)
        }, LinearLayout.LayoutParams(dp(64), dp(64)))
        controls.addView(text("›", 36f, ink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(60), dp(60)))
        controls.addView(text("↻", 22f, muted, true), LinearLayout.LayoutParams(dp(50), dp(60)))
        root.addView(controls)
        content.removeAllViews()
        content.addView(ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(root)
        })
    }

    private fun show(c: LinearLayout) {
        content.removeAllViews()
        val scroll = page()
        scroll.addView(c)
        content.addView(scroll)
    }
}
