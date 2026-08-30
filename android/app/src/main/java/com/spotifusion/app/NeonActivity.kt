package com.spotifusion.app

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

/**
 * Android-only UI for Spotifusion.
 * The web player is intentionally untouched.
 *
 * Layout is deliberately built around a real phone viewport:
 * system bars are respected, the content scrolls independently, and the
 * mini-player + bottom navigation stay fixed above the Android navigation bar.
 */
open class NeonActivity : AppCompatActivity() {
    private val background = Color.rgb(246, 247, 252)
    private val surface = Color.rgb(255, 255, 255)
    private val ink = Color.rgb(24, 24, 30)
    private val muted = Color.rgb(111, 114, 128)
    private val blue = Color.rgb(45, 91, 239)
    private val indigo = Color.rgb(91, 73, 226)
    private val cyan = Color.rgb(60, 178, 235)
    private val pink = Color.rgb(222, 86, 171)
    private val orange = Color.rgb(245, 157, 72)

    private lateinit var page: FrameLayout
    private lateinit var miniTitle: TextView
    private lateinit var miniSubtitle: TextView
    private lateinit var miniPlay: TextView
    private lateinit var nav: LinearLayout
    private lateinit var headerTitle: TextView

    private var currentTab = 0
    private var playing = false
    private var shuffle = false
    private var repeat = false
    private var currentSong = "Glass Architecture"
    private var currentArtist = "Astral Pulse"

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun sp(value: Float): Float = value * resources.displayMetrics.scaledDensity
    private fun alpha(color: Int, amount: Int): Int = Color.argb(amount, Color.red(color), Color.green(color), Color.blue(color))

    private fun bg(color: Int, radius: Int = 20, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            stroke?.let { setStroke(dp(1), it) }
        }

    private fun gradient(start: Int, end: Int, radius: Int = 22): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, end)).apply {
            cornerRadius = dp(radius).toFloat()
        }

    private fun label(value: String, size: Float, color: Int = ink, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        // Critical: never draw the app underneath Android's status/navigation bars.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = background
        window.navigationBarColor = background

        buildShell()
        showHome()
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(background)
            setPadding(dp(12), dp(8), dp(12), 0)
        }

        val top = topBar()
        root.addView(top, LinearLayout.LayoutParams(-1, dp(58)))

        page = FrameLayout(this)
        root.addView(page, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(miniPlayer(), LinearLayout.LayoutParams(-1, dp(72)).apply {
            setMargins(dp(2), dp(7), dp(2), dp(7))
        })

        nav = bottomNavigation()
        root.addView(nav, LinearLayout.LayoutParams(-1, dp(68)))

        setContentView(root)
    }

    private fun topBar(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(4), 0, dp(2), 0)

        val brand = LinearLayout(this@NeonActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val logo = FrameLayout(this@NeonActivity).apply {
            background = gradient(blue, indigo, 13)
            addView(label("◉", 18f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        }
        brand.addView(logo, LinearLayout.LayoutParams(dp(38), dp(38)))

        headerTitle = label("Spotifusion", 18f, ink, true).apply {
            setPadding(dp(11), 0, 0, 0)
        }
        brand.addView(headerTitle)
        addView(brand, LinearLayout.LayoutParams(0, -1, 1f))

        addView(label("⌕", 25f, ink, true).apply {
            gravity = Gravity.CENTER
            background = bg(alpha(surface, 225), 17, alpha(Color.WHITE, 230))
            setOnClickListener { showSearch() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = dp(5) })

        addView(label("♧", 22f, ink, false).apply {
            gravity = Gravity.CENTER
            background = bg(alpha(surface, 225), 17, alpha(Color.WHITE, 230))
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
    }

    private fun bottomNavigation(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        setPadding(dp(5), dp(4), dp(5), dp(5))
        background = bg(alpha(Color.WHITE, 242), 23, alpha(Color.WHITE, 250))
        elevation = dp(5).toFloat()

        val items = arrayOf(
            "⌂" to "Home",
            "⌕" to "Search",
            "▣" to "Library",
            "≋" to "Equalizer",
            "⚙" to "Settings"
        )

        items.forEachIndexed { index, pair ->
            val selected = index == currentTab
            val cell = LinearLayout(this@NeonActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(2), dp(3), dp(2), dp(2))
                if (selected) background = bg(alpha(blue, 25), 16)
                setOnClickListener {
                    currentTab = index
                    when (index) {
                        0 -> showHome()
                        1 -> showSearch()
                        2 -> showLibrary()
                        3 -> showEqualizer()
                        4 -> showSettings()
                    }
                    refreshNav()
                }
            }
            cell.addView(label(pair.first, 19f, if (selected) blue else muted, true).apply { gravity = Gravity.CENTER })
            cell.addView(label(pair.second, 9f, if (selected) blue else muted, true).apply { gravity = Gravity.CENTER })
            addView(cell, LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
    }

    private fun refreshNav() {
        val names = arrayOf("Home", "Search", "Library", "Equalizer", "Settings")
        for (i in 0 until nav.childCount) {
            val cell = nav.getChildAt(i) as LinearLayout
            val selected = i == currentTab
            cell.background = if (selected) bg(alpha(blue, 25), 16) else null
            (cell.getChildAt(0) as TextView).setTextColor(if (selected) blue else muted)
            (cell.getChildAt(1) as TextView).setTextColor(if (selected) blue else muted)
        }
    }

    private fun miniPlayer(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), dp(7), dp(7), dp(7))
        background = bg(alpha(Color.WHITE, 242), 20, alpha(Color.WHITE, 250))
        elevation = dp(4).toFloat()

        val art = FrameLayout(this@NeonActivity).apply {
            background = gradient(indigo, cyan, 14)
            addView(label("◉", 22f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        }
        addView(art, LinearLayout.LayoutParams(dp(50), dp(50)))

        val info = LinearLayout(this@NeonActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(11), 0, dp(4), 0)
            setOnClickListener { showNowPlaying() }
        }
        miniTitle = label(currentSong, 13.5f, ink, true)
        miniSubtitle = label(currentArtist, 10.5f, muted)
        info.addView(miniTitle)
        info.addView(miniSubtitle.apply { setPadding(0, dp(4), 0, 0) })
        addView(info, LinearLayout.LayoutParams(0, -1, 1f))

        addView(label("♡", 22f, muted).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(38), dp(50)))
        miniPlay = label(if (playing) "Ⅱ" else "▶", 18f, ink, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                playing = !playing
                updateMini()
            }
        }
        addView(miniPlay, LinearLayout.LayoutParams(dp(43), dp(50)))
        addView(label("≋", 20f, ink, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { showNowPlaying() }
        }, LinearLayout.LayoutParams(dp(40), dp(50)))
    }

    private fun updateMini() {
        miniTitle.text = currentSong
        miniSubtitle.text = if (playing) "$currentArtist · Playing" else currentArtist
        miniPlay.text = if (playing) "Ⅱ" else "▶"
    }

    private fun scrollPage(content: View): ScrollView = ScrollView(this).apply {
        isFillViewport = true
        overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        setBackgroundColor(background)
        addView(content, ViewGroup.LayoutParams(-1, -1))
    }

    private fun column(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(8), dp(6), dp(8), dp(24))
    }

    private fun clearAndShow(view: View) {
        page.removeAllViews()
        page.addView(view, FrameLayout.LayoutParams(-1, -1))
    }

    private fun section(c: LinearLayout, title: String, action: String = "See all") {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(2), dp(5), dp(2), dp(9))
        }
        row.addView(label(title, 12f, ink, true), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(label(action, 10.5f, blue, true))
        c.addView(row)
    }

    private fun showHome() {
        headerTitle.text = "Spotifusion"
        val c = column()

        val greeting = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        greeting.addView(label("Good morning,", 13f, muted))
        greeting.addView(label("Kruthagna 👋", 26f, ink, true).apply { setPadding(0, dp(4), 0, 0) })
        c.addView(greeting, LinearLayout.LayoutParams(-1, dp(72)).apply { bottomMargin = dp(8) })

        // Large hero exactly where it belongs: inside the page, below the greeting.
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(13), dp(16))
            background = gradient(Color.rgb(52, 87, 221), Color.rgb(106, 73, 222), 23)
            elevation = dp(4).toFloat()
        }

        val heroArt = FrameLayout(this).apply {
            background = gradient(Color.rgb(99, 146, 255), Color.rgb(65, 54, 170), 18)
            elevation = dp(3).toFloat()
            addView(label("◉", 48f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
            addView(label("GLASS", 8f, alpha(Color.WHITE, 200), true).apply {
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setPadding(0, 0, 0, dp(8))
            })
        }
        hero.addView(heroArt, LinearLayout.LayoutParams(dp(112), dp(112)))

        val heroInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }
        heroInfo.addView(label("FEATURED SESSION", 9f, alpha(Color.WHITE, 210), true))
        heroInfo.addView(label("Glass Architecture", 18f, Color.WHITE, true).apply { setPadding(0, dp(7), 0, 0) })
        heroInfo.addView(label("Nordic Echoes", 11f, alpha(Color.WHITE, 215)).apply { setPadding(0, dp(4), 0, dp(12)) })
        heroInfo.addView(label("  ▶  PLAY NOW  ", 10.5f, blue, true).apply {
            gravity = Gravity.CENTER
            background = bg(Color.WHITE, 15)
            setPadding(dp(5), dp(9), dp(5), dp(9))
            setOnClickListener {
                currentSong = "Glass Architecture"
                currentArtist = "Astral Pulse"
                playing = true
                updateMini()
                showNowPlaying()
            }
        }, LinearLayout.LayoutParams(-2, dp(38)))
        hero.addView(heroInfo, LinearLayout.LayoutParams(0, -2, 1f))
        c.addView(hero, LinearLayout.LayoutParams(-1, dp(144)).apply { bottomMargin = dp(17) })

        section(c, "Recently Played")
        val recentScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val recent = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        recentTrack(recent, "Glass Architecture", "Astral Pulse", blue)
        recentTrack(recent, "Astral Pulse", "Lunar Drift", indigo)
        recentTrack(recent, "Midnight Drive", "Neon Skyline", cyan)
        recentTrack(recent, "Ocean Bloom", "Cobalt Waves", pink)
        recentScroll.addView(recent)
        c.addView(recentScroll, LinearLayout.LayoutParams(-1, dp(151)).apply { bottomMargin = dp(12) })

        section(c, "Made For You")
        val mixes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        mixCard(mixes, "Focus\nFlow", blue, cyan)
        mixCard(mixes, "Chill\nSpace", indigo, pink)
        mixCard(mixes, "Energy\nBoost", orange, pink)
        c.addView(mixes, LinearLayout.LayoutParams(-1, dp(142)))

        val now = miniSuggestion()
        c.addView(now, LinearLayout.LayoutParams(-1, dp(62)).apply { topMargin = dp(9) })

        clearAndShow(scrollPage(c))
    }

    private fun recentTrack(parent: LinearLayout, title: String, artist: String, accent: Int) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(7), dp(7), dp(7), dp(4))
            setOnClickListener {
                selectTrack(title, artist)
            }
        }
        val art = FrameLayout(this).apply {
            background = gradient(accent, Color.rgb(215, 224, 255), 16)
            addView(label("◉", 28f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
            addView(label("▶", 10f, blue, true).apply {
                gravity = Gravity.BOTTOM or Gravity.RIGHT
                background = bg(Color.WHITE, 12)
                setPadding(dp(6), dp(5), dp(6), dp(5))
            })
        }
        card.addView(art, LinearLayout.LayoutParams(dp(102), dp(102)))
        card.addView(label(title, 10.5f, ink, true).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(2), dp(6), 0, 0)
        })
        card.addView(label(artist, 9f, muted).apply {
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(dp(2), dp(3), 0, 0)
        })
        parent.addView(card, LinearLayout.LayoutParams(dp(116), -1))
    }

    private fun mixCard(parent: LinearLayout, title: String, start: Int, end: Int) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(5), 0, dp(5), 0)
            setOnClickListener { showLibrary() }
        }
        val art = FrameLayout(this).apply {
            background = gradient(start, end, 17)
            val ring = TextView(this@NeonActivity).apply {
                text = "◉"
                textSize = 30f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                background = bg(alpha(Color.WHITE, 25), 999, alpha(Color.WHITE, 100))
            }
            addView(ring, FrameLayout.LayoutParams(dp(65), dp(65), Gravity.CENTER))
            addView(label("▶", 9f, blue, true).apply {
                gravity = Gravity.BOTTOM or Gravity.RIGHT
                background = bg(Color.WHITE, 12)
                setPadding(dp(6), dp(5), dp(6), dp(5))
            })
        }
        card.addView(art, LinearLayout.LayoutParams(dp(108), dp(108)))
        card.addView(label(title, 10.5f, ink, true).apply { setPadding(dp(2), dp(7), 0, 0) })
        parent.addView(card, LinearLayout.LayoutParams(0, -1, 1f))
    }

    private fun miniSuggestion(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(9), dp(6), dp(7), dp(6))
        background = bg(alpha(Color.WHITE, 235), 18, alpha(Color.WHITE, 245))
        val art = FrameLayout(this@NeonActivity).apply {
            background = gradient(blue, indigo, 13)
            addView(label("◉", 19f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        }
        addView(art, LinearLayout.LayoutParams(dp(48), dp(48)))
        val info = LinearLayout(this@NeonActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }
        info.addView(label("Glass Architecture", 11.5f, ink, true))
        info.addView(label("Nordic Echoes", 9.5f, muted).apply { setPadding(0, dp(3), 0, 0) })
        addView(info, LinearLayout.LayoutParams(0, -1, 1f))
        addView(label("▶", 15f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = bg(blue, 15)
            setPadding(dp(9), dp(8), dp(9), dp(8))
            setOnClickListener {
                selectTrack("Glass Architecture", "Astral Pulse")
                playing = true
                updateMini()
            }
        }, LinearLayout.LayoutParams(dp(38), dp(38)))
    }

    private fun selectTrack(title: String, artist: String) {
        currentSong = title
        currentArtist = artist
        playing = true
        updateMini()
        showNowPlaying()
    }

    private fun showSearch() {
        currentTab = 1
        headerTitle.text = "Search"
        val c = column()
        c.addView(label("Search", 26f, ink, true).apply { setPadding(dp(2), dp(7), 0, dp(14)) })

        val searchBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(10), 0)
            background = bg(alpha(Color.WHITE, 235), 19, alpha(Color.WHITE, 245))
            elevation = dp(2).toFloat()
        }
        searchBox.addView(label("⌕", 23f, muted, true))
        val input = EditText(this).apply {
            hint = "Search songs, artists, albums..."
            textSize = 13.5f
            setTextColor(ink)
            setHintTextColor(muted)
            singleLine = true
            background = null
            setPadding(dp(9), 0, 0, 0)
        }
        searchBox.addView(input, LinearLayout.LayoutParams(0, dp(54), 1f))
        searchBox.addView(label("⌁", 19f, muted, true).apply { gravity = Gravity.CENTER })
        c.addView(searchBox, LinearLayout.LayoutParams(-1, dp(54)).apply { bottomMargin = dp(15) })

        section(c, "Recent Searches", "Clear all")
        arrayOf("glass architecture", "astral pulse", "nordic echoes", "midnight drive", "ocean bloom").forEach { query ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(12), dp(5), dp(12), dp(5))
                setOnClickListener { input.setText(query); input.setSelection(query.length) }
            }
            row.addView(label("◷", 16f, muted), LinearLayout.LayoutParams(dp(28), dp(40)))
            row.addView(label(query, 11.5f, ink), LinearLayout.LayoutParams(0, dp(40), 1f))
            row.addView(label("×", 18f, muted))
            c.addView(row, LinearLayout.LayoutParams(-1, dp(50)))
        }

        section(c, "Trending Now")
        glassRow(c, "Velvet Horizon", "Satin Dust", blue)
        glassRow(c, "Echoes of You", "Midnight Signal", cyan)
        glassRow(c, "Endless Blue", "Ocean Bloom", indigo)

        clearAndShow(scrollPage(c))
        refreshNav()
    }

    private fun glassRow(c: LinearLayout, title: String, artist: String, accent: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(7), dp(8), dp(7))
            background = bg(alpha(Color.WHITE, 215), 16)
            setOnClickListener { selectTrack(title, artist) }
        }
        val art = FrameLayout(this).apply {
            background = gradient(accent, Color.rgb(210, 220, 250), 13)
            addView(label("◉", 19f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        }
        row.addView(art, LinearLayout.LayoutParams(dp(48), dp(48)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        info.addView(label(title, 12f, ink, true))
        info.addView(label(artist, 10f, muted).apply { setPadding(0, dp(4), 0, 0) })
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(label("×", 18f, muted))
        c.addView(row, LinearLayout.LayoutParams(-1, dp(64)).apply { bottomMargin = dp(7) })
    }

    private fun showNowPlaying() {
        headerTitle.text = "Now Playing"
        val c = column().apply { gravity = Gravity.CENTER_HORIZONTAL }

        c.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(label("⌄", 22f, ink, true))
            addView(label("Now Playing", 14f, ink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(38), 1f))
            addView(label("⋮", 22f, ink, true))
        }, LinearLayout.LayoutParams(-1, dp(42)).apply { bottomMargin = dp(4) })

        val disc = FrameLayout(this).apply {
            background = bg(Color.rgb(252, 253, 255), 999, alpha(blue, 90))
            elevation = dp(5).toFloat()
        }
        val vinyl = FrameLayout(this).apply {
            background = bg(Color.rgb(25, 27, 34), 999, alpha(Color.WHITE, 40))
            addView(label("◉", 42f, blue, true).apply { gravity = Gravity.CENTER })
        }
        disc.addView(vinyl, FrameLayout.LayoutParams(dp(214), dp(214), Gravity.CENTER))
        c.addView(disc, LinearLayout.LayoutParams(dp(274), dp(274)).apply { topMargin = dp(8); bottomMargin = dp(22) })

        c.addView(label("$currentSong", 22f, ink, true).apply { gravity = Gravity.CENTER })
        c.addView(label("$currentArtist · Nordic Echoes", 12f, muted).apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(18)) })

        val progress = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(3), 0, dp(3), 0)
        }
        val bar = SeekBar(this).apply {
            max = 100
            progress = 42
            progressTintList = android.content.res.ColorStateList.valueOf(blue)
            thumbTintList = android.content.res.ColorStateList.valueOf(blue)
        }
        progress.addView(bar, LinearLayout.LayoutParams(-1, dp(28)))
        val times = LinearLayout(this).apply {
            addView(label("1:28", 9.5f, muted), LinearLayout.LayoutParams(0, -2, 1f))
            addView(label("3:42", 9.5f, muted).apply { gravity = Gravity.RIGHT })
        }
        progress.addView(times)
        c.addView(progress, LinearLayout.LayoutParams(-1, dp(47)))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, 0)
        }
        actions.addView(label(if (shuffle) "🔀" else "⇄", 18f, if (shuffle) blue else muted).apply {
            setOnClickListener { shuffle = !shuffle; setTextColor(if (shuffle) blue else muted) }
        }, LinearLayout.LayoutParams(dp(45), dp(55)))
        actions.addView(label("|◀", 21f, ink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(50), dp(55)))
        actions.addView(label(if (playing) "Ⅱ" else "▶", 24f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
            background = bg(blue, 20)
            setOnClickListener { playing = !playing; updateMini(); text = if (playing) "Ⅱ" else "▶" }
        }, LinearLayout.LayoutParams(dp(64), dp(58)))
        actions.addView(label("▶|", 21f, ink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(50), dp(55)))
        actions.addView(label("↻", 21f, if (repeat) blue else muted, true).apply {
            setOnClickListener { repeat = !repeat; setTextColor(if (repeat) blue else muted) }
        }, LinearLayout.LayoutParams(dp(45), dp(55)))
        c.addView(actions)

        clearAndShow(scrollPage(c))
    }

    private fun showLibrary() {
        currentTab = 2
        headerTitle.text = "Your Library"
        val c = column()
        c.addView(label("Your Library", 26f, ink, true).apply { setPadding(dp(2), dp(7), 0, dp(14)) })

        val filters = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        arrayOf("Playlists", "Songs", "Artists", "Albums").forEachIndexed { i, name ->
            filters.addView(label(" $name ", 10.5f, if (i == 0) blue else muted, true).apply {
                gravity = Gravity.CENTER
                background = bg(if (i == 0) alpha(blue, 28) else alpha(Color.WHITE, 220), 13)
                setPadding(dp(4), dp(8), dp(4), dp(8))
            }, LinearLayout.LayoutParams(0, dp(36), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        c.addView(filters, LinearLayout.LayoutParams(-1, dp(38)).apply { bottomMargin = dp(14) })

        libraryItem(c, "♥", "Liked Songs", "432 songs", blue)
        libraryItem(c, "◉", "Chill Vibes", "by Kruthagna · 50 songs", indigo)
        libraryItem(c, "◉", "Night Drive", "by Kruthagna · 42 songs", cyan)
        libraryItem(c, "✦", "Focus Flow", "by Kruthagna · 38 songs", pink)
        libraryItem(c, "◉", "Retro Wave", "by Kruthagna · 55 songs", orange)
        libraryItem(c, "≋", "Workout Hits", "by Kruthagna · 60 songs", blue)

        clearAndShow(scrollPage(c))
        refreshNav()
    }

    private fun libraryItem(c: LinearLayout, icon: String, title: String, sub: String, accent: Int) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(7), dp(7), dp(7), dp(7))
            background = bg(alpha(Color.WHITE, 218), 18)
            setOnClickListener { showNowPlaying() }
        }
        row.addView(FrameLayout(this).apply {
            background = gradient(accent, Color.rgb(215, 224, 255), 15)
            addView(label(icon, 21f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        }, LinearLayout.LayoutParams(dp(56), dp(56)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, dp(5), 0) }
        info.addView(label(title, 12.5f, ink, true))
        info.addView(label(sub, 10f, muted).apply { setPadding(0, dp(4), 0, 0) })
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(label("⋮", 19f, muted))
        c.addView(row, LinearLayout.LayoutParams(-1, dp(70)).apply { bottomMargin = dp(8) })
    }

    private fun showEqualizer() {
        currentTab = 3
        headerTitle.text = "Equalizer"
        val c = column()
        c.addView(label("Equalizer", 26f, ink, true).apply { setPadding(dp(2), dp(7), 0, dp(4)) })
        c.addView(label("Fine-tune your sound", 11.5f, muted).apply { setPadding(dp(2), 0, 0, dp(15)) })

        val presetRow = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        val presets = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        arrayOf("Custom", "Rock", "Pop", "Electronic", "Jazz").forEachIndexed { i, p ->
            presets.addView(label(" $p ", 10.5f, if (i == 0) Color.WHITE else muted, true).apply {
                gravity = Gravity.CENTER
                background = bg(if (i == 0) blue else alpha(Color.WHITE, 220), 14)
                setPadding(dp(8), dp(9), dp(8), dp(9))
            }, LinearLayout.LayoutParams(-2, dp(38)).apply { rightMargin = dp(7) })
        }
        presetRow.addView(presets)
        c.addView(presetRow, LinearLayout.LayoutParams(-1, dp(42)).apply { bottomMargin = dp(14) })

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(18), dp(7), dp(15))
            background = bg(alpha(Color.WHITE, 225), 22, alpha(Color.WHITE, 245))
            elevation = dp(2).toFloat()
        }
        val freqs = arrayOf("60Hz", "230Hz", "910Hz", "3.6k", "14k")
        val levels = intArrayOf(72, 48, 60, 80, 54)
        freqs.forEachIndexed { i, f ->
            val band = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
            band.addView(label("+12", 8f, muted).apply { gravity = Gravity.CENTER })
            val seek = SeekBar(this).apply {
                max = 100
                progress = levels[i]
                rotation = -90f
                progressTintList = android.content.res.ColorStateList.valueOf(blue)
                thumbTintList = android.content.res.ColorStateList.valueOf(blue)
            }
            band.addView(seek, LinearLayout.LayoutParams(dp(45), dp(150)))
            band.addView(label(f, 9f, ink, true).apply { gravity = Gravity.CENTER })
            panel.addView(band, LinearLayout.LayoutParams(0, dp(190), 1f))
        }
        c.addView(panel, LinearLayout.LayoutParams(-1, dp(222)))

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        arrayOf("Bass Boost", "Virtualizer", "Loudness").forEach { name ->
            controls.addView(label("○\n$name", 10f, ink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(70), 1f))
        }
        c.addView(controls)
        clearAndShow(scrollPage(c))
        refreshNav()
    }

    private fun showSettings() {
        currentTab = 4
        headerTitle.text = "Settings"
        val c = column()
        c.addView(label("Settings", 26f, ink, true).apply { setPadding(dp(2), dp(7), 0, dp(4)) })
        c.addView(label("Playback, appearance and audio", 11.5f, muted).apply { setPadding(dp(2), 0, 0, dp(15)) })

        settingsSection(c, "Playback")
        settingRow(c, "Playback Quality", "High", false)
        settingRow(c, "Crossfade", "5 sec", false)
        settingRow(c, "Gapless Playback", "ON", true)
        settingRow(c, "Normalize Volume", "ON", true)

        settingsSection(c, "Appearance")
        settingRow(c, "Theme", "Light", false)
        settingRow(c, "Accent Color", "Blue", false)

        settingsSection(c, "Audio")
        settingRow(c, "Equalizer", "Custom", false)
        settingRow(c, "Bass Boost", "Moderate", false)

        settingsSection(c, "Other")
        settingRow(c, "Sleep Timer", "Off", false)
        settingRow(c, "Shake to change song", "ON", true)

        clearAndShow(scrollPage(c))
        refreshNav()
    }

    private fun settingsSection(c: LinearLayout, title: String) {
        c.addView(label(title, 10f, muted, true).apply {
            letterSpacing = .08f
            setPadding(dp(3), dp(10), 0, dp(7))
        })
    }

    private fun settingRow(c: LinearLayout, title: String, value: String, toggle: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(13), dp(7), dp(10), dp(7))
            background = bg(alpha(Color.WHITE, 222), 16)
        }
        row.addView(label(title, 11.5f, ink, true), LinearLayout.LayoutParams(0, dp(48), 1f))
        if (toggle) {
            row.addView(label("●", 18f, blue, true).apply { gravity = Gravity.CENTER })
        } else {
            row.addView(label("$value  ›", 10.5f, muted, false).apply { gravity = Gravity.CENTER })
        }
        c.addView(row, LinearLayout.LayoutParams(-1, dp(62)).apply { bottomMargin = dp(7) })
    }
}
