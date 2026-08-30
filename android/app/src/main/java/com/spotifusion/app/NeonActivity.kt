package com.spotifusion.app

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

/**
 * Android-only presentation layer for Spotifusion.
 *
 * The web player is intentionally untouched. This activity follows the
 * supplied Nordic Studio Glass HTML reference while keeping the existing
 * Android navigation and screen entry points intact.
 */
open class NeonActivity : AppCompatActivity() {
    private val bg = Color.rgb(244, 244, 245)
    private val ink = Color.rgb(24, 24, 27)
    private val muted = Color.rgb(113, 113, 122)
    private val blue = Color.rgb(37, 99, 235)
    private val indigo = Color.rgb(79, 70, 229)
    private val cyan = Color.rgb(14, 165, 233)
    private val amber = Color.rgb(245, 158, 11)
    private val white = Color.WHITE

    private lateinit var content: FrameLayout
    private lateinit var miniTitle: TextView
    private lateinit var miniSubtitle: TextView
    private lateinit var miniPlay: TextView
    private lateinit var nav: LinearLayout

    private var currentTab = 0
    private var playing = false
    private var shuffle = false
    private var repeat = false
    private var progress = 42

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun alpha(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))

    private fun rounded(c: Int, radius: Int = 18, stroke: Int? = null, strokeWidth: Int = 1) =
        GradientDrawable().apply {
            setColor(c)
            cornerRadius = dp(radius).toFloat()
            stroke?.let { setStroke(dp(strokeWidth), it) }
        }

    private fun gradient(vararg colors: IntArray, radius: Int = 22) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors.first()).apply {
            cornerRadius = dp(radius).toFloat()
        }

    private fun text(value: String, size: Float, c: Int = ink, bold: Boolean = false) =
        TextView(this).apply {
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
            setPadding(dp(10), dp(8), dp(10), 0)
        }

        content = FrameLayout(this)
        content.setBackgroundColor(bg)
        root.addView(content, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(miniPlayer(), LinearLayout.LayoutParams(-1, dp(70)).apply {
            setMargins(dp(2), dp(6), dp(2), dp(8))
        })

        nav = bottomNav()
        root.addView(nav, LinearLayout.LayoutParams(-1, dp(72)))
        setContentView(root)
    }

    private fun bottomNav(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(6), dp(5), dp(6), dp(6))
        background = rounded(alpha(white, 232), 22, alpha(white, 245))
        elevation = dp(5).toFloat()

        val items = arrayOf(
            "⌂" to "Home",
            "⌕" to "Search",
            "♫" to "Library",
            "≋" to "Equalizer",
            "⚙" to "Settings"
        )

        items.forEachIndexed { i, item ->
            val selected = i == currentTab
            val holder = LinearLayout(this@NeonActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(4), dp(4), dp(4), dp(3))
                if (selected) background = rounded(alpha(blue, 25), 16)
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
            holder.addView(text(item.first, 20f, if (selected) blue else muted, true))
            holder.addView(text(item.second, 9f, if (selected) blue else muted, true).apply {
                setPadding(0, dp(3), 0, 0)
            })
            addView(holder, LinearLayout.LayoutParams(0, -1, 1f).apply {
                setMargins(dp(2), 0, dp(2), 0)
            })
        }
    }

    private fun miniPlayer(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(10), dp(8), dp(8), dp(8))
        background = rounded(alpha(white, 235), 20, alpha(white, 245))
        elevation = dp(4).toFloat()

        val art = FrameLayout(this@NeonActivity).apply {
            background = rounded(blue, 14)
            elevation = dp(3).toFloat()
            addView(text("◉", 24f, white, true).apply { gravity = Gravity.CENTER })
        }
        addView(art, LinearLayout.LayoutParams(dp(50), dp(50)))

        val info = LinearLayout(this@NeonActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
            setOnClickListener { nowPlaying() }
        }
        miniTitle = text("Nothing playing", 14f, ink, true)
        miniSubtitle = text("Choose a track to start", 11f, muted)
        info.addView(miniTitle)
        info.addView(miniSubtitle.apply { setPadding(0, dp(4), 0, 0) })
        addView(info, LinearLayout.LayoutParams(0, -1, 1f))

        miniPlay = text(if (playing) "Ⅱ" else "▶", 20f, blue, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                playing = !playing
                updateMiniPlayer()
            }
        }
        addView(miniPlay, LinearLayout.LayoutParams(dp(44), dp(50)))
    }

    private fun updateMiniPlayer() {
        miniTitle.text = if (playing) "Glass Architecture" else "Glass Architecture"
        miniSubtitle.text = if (playing) "Astral Pulse · Playing" else "Astral Pulse · 3:45"
        miniPlay.text = if (playing) "Ⅱ" else "▶"
    }

    private fun column() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(20))
    }

    private fun heading(c: LinearLayout, title: String, subtitle: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(text(title, 23f, ink, true))
        titles.addView(text(subtitle, 12f, muted).apply { setPadding(0, dp(5), 0, 0) })
        row.addView(titles, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text("•", 24f, blue, true))
        c.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(16) })
    }

    private fun sectionLabel(c: LinearLayout, value: String, top: Int = 10) {
        c.addView(text(value, 10.5f, muted, true).apply {
            letterSpacing = .08f
            setPadding(0, dp(top), 0, dp(9))
        })
    }

    private fun glassCard(
        c: LinearLayout,
        title: String,
        subtitle: String,
        icon: String = "♪",
        accent: Int = blue,
        click: (() -> Unit)? = null
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(alpha(white, 214), 18, alpha(white, 238))
            elevation = dp(2).toFloat()
            if (click != null) setOnClickListener { click() }
        }

        val iconBox = FrameLayout(this).apply {
            background = rounded(alpha(accent, 30), 14)
            addView(text(icon, 21f, accent, true).apply { gravity = Gravity.CENTER })
        }
        card.addView(iconBox, LinearLayout.LayoutParams(dp(48), dp(48)))

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, dp(8), 0)
        }
        info.addView(text(title, 14f, ink, true))
        info.addView(text(subtitle, 11f, muted).apply { setPadding(0, dp(4), 0, 0) })
        card.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(text("›", 27f, muted))

        c.addView(card, LinearLayout.LayoutParams(-1, dp(70)).apply { bottomMargin = dp(9) })
    }

    private fun hero(c: LinearLayout) {
        val h = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(18))
            background = rounded(blue, 23)
            elevation = dp(5).toFloat()
        }
        h.addView(text("FEATURED GLASS SESSION", 10f, alpha(white, 235), true))
        h.addView(text("Nordic Resonance Vol. 4", 25f, white, true).apply {
            setPadding(0, dp(8), 0, 0)
        })
        h.addView(text("Immersive acoustic frequencies rendered through translucent soundscapes and frosted vinyl tones.", 12f, alpha(white, 218)).apply {
            setPadding(0, dp(5), 0, dp(15))
        })
        h.addView(text("  ▶   LISTEN NOW  ", 12f, blue, true).apply {
            gravity = Gravity.CENTER
            background = rounded(white, 17)
            setPadding(dp(8), dp(10), dp(8), dp(10))
            setOnClickListener {
                playing = true
                updateMiniPlayer()
                nowPlaying()
            }
        }, LinearLayout.LayoutParams(-2, dp(43)))
        c.addView(h, LinearLayout.LayoutParams(-1, dp(202)).apply { bottomMargin = dp(17) })
    }

    private fun home() {
        currentTab = 0
        val c = column()
        heading(c, "Good morning, Kruthagna", "Your Nordic music space")
        hero(c)

        sectionLabel(c, "RECENTLY PLAYED")
        glassCard(c, "Glass Architecture", "Astral Pulse · 3:45", "◉", blue) { selectTrack("Glass Architecture", "Astral Pulse") }
        glassCard(c, "Obsidian & Light", "Kaelen Voss · 4:12", "◉", indigo) { selectTrack("Obsidian & Light", "Kaelen Voss") }
        glassCard(c, "Frictionless Orbit", "Sora & The Echoes · 2:58", "◉", amber) { selectTrack("Frictionless Orbit", "Sora & The Echoes") }

        sectionLabel(c, "MADE FOR KRUTHAGNA", 8)
        glassCard(c, "Daily Mix 1", "Glass · Ambient · Electronic", "✦", blue) { library() }
        glassCard(c, "Chill Vibes", "Lofi · Instrumental", "◌", indigo) { library() }
        glassCard(c, "Nordic Studio", "Minimal · Clean", "◌", cyan) { library() }
        glassCard(c, "Focus Frequencies", "Binaural · Drone", "✧", amber) { library() }
        show(c)
    }

    private fun search() {
        currentTab = 1
        val c = column()
        heading(c, "Search Frequencies", "Artists, tracks, albums and playlists")

        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(12), 0)
            background = rounded(alpha(white, 225), 20, alpha(white, 242))
            elevation = dp(2).toFloat()
        }
        searchBox.addView(text("⌕", 24f, muted, true))
        val input = EditText(this).apply {
            hint = "Search artists, tracks, or albums..."
            setHintTextColor(muted)
            setTextColor(ink)
            textSize = 14f
            setSingleLine(true)
            background = null
            setPadding(dp(10), 0, 0, 0)
        }
        searchBox.addView(input, LinearLayout.LayoutParams(0, dp(54), 1f))
        c.addView(searchBox, LinearLayout.LayoutParams(-1, dp(54)).apply { bottomMargin = dp(15) })

        sectionLabel(c, "RECENT SEARCHES", 0)
        val recent = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, dp(4))
        }
        arrayOf("Gramophone Acoustics", "Nordic Synth", "Astral Pulse").forEach {
            recent.addView(text("  $it  ", 11.5f, ink, true).apply {
                background = rounded(alpha(white, 218), 15)
                setPadding(dp(4), dp(9), dp(4), dp(9))
            }, LinearLayout.LayoutParams(-2, dp(36)).apply { rightMargin = dp(7) })
        }
        c.addView(recent)

        sectionLabel(c, "BROWSE ALL GENRES", 12)
        glassCard(c, "Ambient Glass", "Translucent ambient soundscapes", "◌", blue)
        glassCard(c, "Electronica", "Modern electronic textures", "≋", cyan)
        glassCard(c, "Acoustic Vinyl", "Warm acoustic sessions", "◉", amber)
        glassCard(c, "Minimalist", "Clean and restrained", "◇", indigo)
        glassCard(c, "Deep Focus", "Binaural · Drone · Focus", "✧", Color.rgb(16, 185, 129))
        glassCard(c, "Nordic Pop", "Bright Nordic-inspired pop", "♪", Color.rgb(244, 63, 94))
        show(c)
    }

    private fun library() {
        currentTab = 2
        val c = column()
        heading(c, "Your Library", "Everything you saved in one place")
        glassCard(c, "Liked Songs", "428 tracks · Pin to top", "♥", blue) { nowPlaying() }
        glassCard(c, "Chill Vibes & Glass", "Created by Kruthagna · 32 songs", "◉", cyan) { nowPlaying() }
        glassCard(c, "Nordic Morning Ambient", "Playlist · 18 songs", "◉", indigo) { nowPlaying() }
        glassCard(c, "Local Music", "Music stored on this device", "▣", amber)
        glassCard(c, "Downloaded", "Available offline", "↓", blue)
        show(c)
    }

    private fun equalizer() {
        currentTab = 3
        val c = column()
        heading(c, "Audio Equalizer", "Precision 5-band frequency shaping in Nordic glass")

        val status = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(11), dp(14), dp(11))
            background = rounded(alpha(white, 218), 18)
        }
        status.addView(text("EQUALIZER", 10f, muted, true), LinearLayout.LayoutParams(0, -2, 1f))
        status.addView(text("Active", 12f, blue, true))
        c.addView(status, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(12) })

        sectionLabel(c, "PRESETS", 0)
        val presets = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        arrayOf("Nordic Flat", "Acoustic Glass", "Deep Bass", "Vocal Presence").forEachIndexed { i, label ->
            presets.addView(text(label, 11f, if (i == 0) white else ink, true).apply {
                gravity = Gravity.CENTER
                background = rounded(if (i == 0) blue else alpha(white, 218), 14)
                setPadding(dp(10), dp(9), dp(10), dp(9))
            }, LinearLayout.LayoutParams(-2, dp(38)).apply { rightMargin = dp(7) })
        }
        c.addView(presets, LinearLayout.LayoutParams(-1, dp(40)).apply { bottomMargin = dp(14) })

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(18), dp(8), dp(14))
            background = rounded(alpha(white, 216), 20, alpha(white, 240))
            elevation = dp(2).toFloat()
        }
        val freqs = arrayOf("60 Hz", "230 Hz", "910 Hz", "3.6k", "14k")
        freqs.forEach { freq ->
            val band = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            band.addView(text("+0 dB", 9.5f, muted, true))
            val seek = SeekBar(this).apply {
                max = 24
                progress = 12
                rotation = -90f
            }
            band.addView(seek, LinearLayout.LayoutParams(dp(72), dp(58)))
            band.addView(text(freq, 10f, ink, true).apply { setPadding(0, dp(4), 0, 0) })
            panel.addView(band, LinearLayout.LayoutParams(0, dp(140), 1f))
        }
        c.addView(panel)
        show(c)
    }

    private fun settings() {
        currentTab = 4
        val c = column()
        heading(c, "Settings & Preferences", "Configure playback, appearance, and device integration")
        glassCard(c, "High Fidelity Audio", "Best available streaming quality", "◉", blue)
        glassCard(c, "Dynamic Glass Refraction", "Animate ambient color blobs", "✧", cyan)
        glassCard(c, "Gramophone 3D Physics", "Cursor-driven tilt and vinyl inertia", "◌", indigo)
        glassCard(c, "Shake to Change", "Change song by shaking your phone", "↯", amber)
        glassCard(c, "Storage", "Offline songs and cached artwork", "▣", blue)
        glassCard(c, "Account", "Profile and connected services", "◎", indigo)
        glassCard(c, "About Spotifusion", "Nordic Studio · Android", "ⓘ", muted)
        show(c)
    }

    private fun selectTrack(title: String, artist: String) {
        playing = true
        miniTitle.text = title
        miniSubtitle.text = "$artist · Playing"
        miniPlay.text = "Ⅱ"
        nowPlaying(title, artist)
    }

    private fun nowPlaying(title: String = "Glass Architecture", artist: String = "Astral Pulse") {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
            setBackgroundColor(bg)
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(text("NOW PLAYING", 10.5f, muted, true), LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(text("•••", 15f, muted, true))
        root.addView(top, LinearLayout.LayoutParams(-1, dp(28)).apply { bottomMargin = dp(4) })

        root.addView(text(title, 22f, ink, true))
        root.addView(text("$artist · Nordic Studio Sessions", 12f, muted).apply {
            setPadding(0, dp(5), 0, dp(15))
        })

        val recordWrap = FrameLayout(this).apply {
            background = rounded(alpha(blue, 35), 200)
            elevation = dp(3).toFloat()
        }
        val record = FrameLayout(this).apply {
            background = rounded(Color.rgb(24, 24, 27), 200)
            elevation = dp(7).toFloat()
        }
        record.addView(text("SPOTIFUSION", 10f, alpha(white, 190), true).apply {
            gravity = Gravity.CENTER
        })
        record.addView(text("•", 23f, blue, true).apply {
            gravity = Gravity.CENTER
        })
        recordWrap.addView(record, FrameLayout.LayoutParams(dp(274), dp(274)).apply {
            gravity = Gravity.CENTER
        })
        root.addView(recordWrap, LinearLayout.LayoutParams(dp(302), dp(302)).apply { bottomMargin = dp(20) })

        val seek = SeekBar(this).apply {
            max = 100
            progress = this@NeonActivity.progress
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) this@NeonActivity.progress = value
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        }
        root.addView(seek, LinearLayout.LayoutParams(-1, dp(32)))

        val times = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(text("1:28", 10f, muted), LinearLayout.LayoutParams(0, -1, 1f))
            addView(text("3:45", 10f, muted).apply { gravity = Gravity.RIGHT })
        }
        root.addView(times, LinearLayout.LayoutParams(-1, dp(20)))

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }

        val shuffleButton = text("⤨", 21f, if (shuffle) blue else muted, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { shuffle = !shuffle; setTextColor(if (shuffle) blue else muted) }
        }
        controls.addView(shuffleButton, LinearLayout.LayoutParams(dp(48), dp(60)))
        controls.addView(text("‹", 36f, ink, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { progress = 0; seek.progress = 0 }
        }, LinearLayout.LayoutParams(dp(55), dp(60)))

        val playButton = text(if (playing) "Ⅱ" else "▶", 25f, white, true).apply {
            gravity = Gravity.CENTER
            background = rounded(blue, 19)
            setOnClickListener {
                playing = !playing
                text = if (playing) "Ⅱ" else "▶"
                updateMiniPlayer()
            }
        }
        controls.addView(playButton, LinearLayout.LayoutParams(dp(64), dp(64)).apply {
            setMargins(dp(3), 0, dp(3), 0)
        })

        controls.addView(text("›", 36f, ink, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { progress = 0; seek.progress = 0 }
        }, LinearLayout.LayoutParams(dp(55), dp(60)))

        val repeatButton = text("↻", 21f, if (repeat) blue else muted, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { repeat = !repeat; setTextColor(if (repeat) blue else muted) }
        }
        controls.addView(repeatButton, LinearLayout.LayoutParams(dp(48), dp(60)))
        root.addView(controls)

        content.removeAllViews()
        content.addView(ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(root)
        })
    }

    private fun show(c: LinearLayout) {
        content.removeAllViews()
        val scroll = ScrollView(this)
        scroll.isVerticalScrollBarEnabled = false
        scroll.overScrollMode = View.OVER_SCROLL_NEVER
        scroll.addView(c)
        content.addView(scroll)
    }
}
