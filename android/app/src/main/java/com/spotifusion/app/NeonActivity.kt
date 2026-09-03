package com.spotifusion.app

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

open class NeonActivity : AppCompatActivity() {
    private val backgroundColor = Color.rgb(246, 247, 252)
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
    private var localTracks: List<LocalTrack> = emptyList()
    private val playerHandler = Handler(Looper.getMainLooper())
    private var playerTicker: Runnable? = null
    private var nowPlayingSeek: SeekBar? = null
    private var nowPlayingElapsed: TextView? = null
    private var nowPlayingTotal: TextView? = null
    private var nowPlayingPlay: TextView? = null
    private var miniArtwork: FrameLayout? = null
    private var nowPlayingArtwork: FrameLayout? = null
    private var lastArtworkKey: String = ""

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
    private fun alpha(color: Int, amount: Int): Int = Color.argb(amount, Color.red(color), Color.green(color), Color.blue(color))
    private fun bg(color: Int, radius: Int = 20, stroke: Int? = null): GradientDrawable = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); stroke?.let { setStroke(dp(1), it) } }
    private fun gradient(start: Int, end: Int, radius: Int = 22): GradientDrawable = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(start, end)).apply { cornerRadius = dp(radius).toFloat() }
    private fun label(value: String, size: Float, color: Int = ink, bold: Boolean = false): TextView = TextView(this).apply { text = value; textSize = size; setTextColor(color); includeFontPadding = false; if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD) }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        buildShell()
        localTracks = runCatching { LocalMusicScanner.scan(this) }.getOrElse { emptyList() }
        refreshArtwork(true)
        showHome()
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(backgroundColor); setPadding(dp(12), dp(8), dp(12), 0) }
        root.addView(topBar(), LinearLayout.LayoutParams(-1, dp(58)))
        page = FrameLayout(this)
        root.addView(page, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(miniPlayer(), LinearLayout.LayoutParams(-1, dp(72)).apply { setMargins(dp(2), dp(7), dp(2), dp(7)) })
        nav = bottomNavigation()
        root.addView(nav, LinearLayout.LayoutParams(-1, dp(68)))
        setContentView(root)
    }

    private fun topBar(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(4), 0, dp(2), 0)
        val brand = LinearLayout(this@NeonActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val logo = FrameLayout(this@NeonActivity).apply { this.background = gradient(blue, indigo, 13); addView(label("◉", 18f, Color.WHITE, true).apply { gravity = Gravity.CENTER }) }
        brand.addView(logo, LinearLayout.LayoutParams(dp(38), dp(38)))
        headerTitle = label("Spotifusion", 18f, ink, true).apply { setPadding(dp(11), 0, 0, 0) }
        brand.addView(headerTitle); addView(brand, LinearLayout.LayoutParams(0, -1, 1f))
        addView(label("⌕", 25f, ink, true).apply { gravity = Gravity.CENTER; this.background = bg(alpha(surface, 225), 17, alpha(Color.WHITE, 230)); setOnClickListener { showSearch() } }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = dp(5) })
        addView(label("♧", 22f, ink).apply { gravity = Gravity.CENTER; this.background = bg(alpha(surface, 225), 17, alpha(Color.WHITE, 230)) }, LinearLayout.LayoutParams(dp(44), dp(44)))
    }

    private fun bottomNavigation(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(5), dp(4), dp(5), dp(5)); this.background = bg(alpha(Color.WHITE, 242), 23, alpha(Color.WHITE, 250)); elevation = dp(5).toFloat()
        val items = arrayOf("⌂" to "Home", "⌕" to "Search", "▣" to "Library", "≋" to "Equalizer", "⚙" to "Settings")
        items.forEachIndexed { index, pair ->
            val selected = index == currentTab
            val cell = LinearLayout(this@NeonActivity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(2), dp(3), dp(2), dp(2)); if (selected) this.background = bg(alpha(blue, 25), 16); setOnClickListener { currentTab = index; when (index) { 0 -> showHome(); 1 -> showSearch(); 2 -> showLibrary(); 3 -> showEqualizer(); 4 -> showSettings() }; refreshNav() } }
            cell.addView(label(pair.first, 19f, if (selected) blue else muted, true).apply { gravity = Gravity.CENTER }); cell.addView(label(pair.second, 9f, if (selected) blue else muted, true).apply { gravity = Gravity.CENTER }); addView(cell, LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
    }

    private fun refreshNav() { for (i in 0 until nav.childCount) { val cell = nav.getChildAt(i) as LinearLayout; val selected = i == currentTab; cell.background = if (selected) bg(alpha(blue, 25), 16) else null; (cell.getChildAt(0) as TextView).setTextColor(if (selected) blue else muted); (cell.getChildAt(1) as TextView).setTextColor(if (selected) blue else muted) } }

    private fun miniPlayer(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(7), dp(7), dp(7)); this.background = bg(alpha(Color.WHITE, 242), 20, alpha(Color.WHITE, 250)); elevation = dp(4).toFloat()
        val art = FrameLayout(this@NeonActivity).apply { this.background = gradient(indigo, cyan, 14); addView(label("◉", 22f, Color.WHITE, true).apply { gravity = Gravity.CENTER }) }
        miniArtwork = art
        addView(art, LinearLayout.LayoutParams(dp(50), dp(50)))
        val info = LinearLayout(this@NeonActivity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(11), 0, dp(4), 0); setOnClickListener { showNowPlaying() } }
        miniTitle = label(currentSong, 13.5f, ink, true); miniSubtitle = label(currentArtist, 10.5f, muted); info.addView(miniTitle); info.addView(miniSubtitle.apply { setPadding(0, dp(4), 0, 0) }); addView(info, LinearLayout.LayoutParams(0, -1, 1f))
        addView(label("♡", 22f, muted).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(38), dp(50)))
        miniPlay = label(if (playing) "Ⅱ" else "▶", 18f, ink, true).apply { gravity = Gravity.CENTER; setOnClickListener { PlaybackController.toggle(); syncPlayerState() } }; addView(miniPlay, LinearLayout.LayoutParams(dp(43), dp(50)))
        addView(label("≋", 20f, ink, true).apply { gravity = Gravity.CENTER; setOnClickListener { showNowPlaying() } }, LinearLayout.LayoutParams(dp(40), dp(50)))
    }

    private fun updateMini() { miniTitle.text = currentSong; miniSubtitle.text = if (playing) "$currentArtist · Playing" else currentArtist; miniPlay.text = if (playing) "Ⅱ" else "▶" }

    private fun syncPlayerState() {
        val title = PlaybackController.currentTitle()
        val artist = PlaybackController.currentArtist()
        if (title.isNotBlank()) currentSong = title
        if (artist.isNotBlank()) currentArtist = artist
        playing = PlaybackController.isPlaying()
        shuffle = PlaybackController.isShuffleEnabled()
        repeat = PlaybackController.isRepeatEnabled()
        updateMini()
        refreshArtwork()
        val duration = PlaybackController.duration()
        val position = PlaybackController.position()
        nowPlayingSeek?.let { seek -> if (!seek.isPressed) seek.progress = progressForPlayer() }
        nowPlayingElapsed?.text = formatTime(position)
        nowPlayingTotal?.text = formatTime(duration)
        nowPlayingPlay?.text = if (playing) "Ⅱ" else "▶"
    }

    private fun startPlayerSync() {
        playerTicker?.let { playerHandler.removeCallbacks(it) }
        val ticker = object : Runnable {
            override fun run() { syncPlayerState(); playerHandler.postDelayed(this, 500L) }
        }
        playerTicker = ticker
        playerHandler.post(ticker)
    }

    override fun onResume() { super.onResume(); startPlayerSync() }
    override fun onPause() { playerTicker?.let { playerHandler.removeCallbacks(it) }; super.onPause() }

    private fun scrollPage(content: View): ScrollView = ScrollView(this).apply { isFillViewport = true; overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS; setBackgroundColor(backgroundColor); addView(content, ViewGroup.LayoutParams(-1, -1)) }
    private fun column(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(6), dp(8), dp(24)) }
    private fun clearAndShow(view: View) { page.removeAllViews(); page.addView(view, FrameLayout.LayoutParams(-1, -1)) }
    private fun section(c: LinearLayout, title: String, action: String = "See all") { val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(2), dp(5), dp(2), dp(9)) }; row.addView(label(title, 12f, ink, true), LinearLayout.LayoutParams(0, -2, 1f)); row.addView(label(action, 10.5f, blue, true)); c.addView(row) }

    private fun showHome() {
        headerTitle.text = "Spotifusion"; val c = column()
        val greeting = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; greeting.addView(label("Good morning,", 13f, muted)); greeting.addView(label("Kruthagna 👋", 26f, ink, true).apply { setPadding(0, dp(4), 0, 0) }); c.addView(greeting, LinearLayout.LayoutParams(-1, dp(72)).apply { bottomMargin = dp(8) })
        val hero = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(16), dp(13), dp(16)); this.background = gradient(Color.rgb(52, 87, 221), Color.rgb(106, 73, 222), 23); elevation = dp(4).toFloat() }
        val heroArt = FrameLayout(this).apply { this.background = gradient(Color.rgb(99, 146, 255), Color.rgb(65, 54, 170), 18); elevation = dp(3).toFloat(); addView(label("◉", 48f, Color.WHITE, true).apply { gravity = Gravity.CENTER }); addView(label("GLASS", 8f, alpha(Color.WHITE, 200), true).apply { gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL; setPadding(0, 0, 0, dp(8)) }) }
        hero.addView(heroArt, LinearLayout.LayoutParams(dp(112), dp(112)))
        val heroInfo = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0) }; heroInfo.addView(label("FEATURED SESSION", 9f, alpha(Color.WHITE, 210), true)); heroInfo.addView(label("Glass Architecture", 18f, Color.WHITE, true).apply { setPadding(0, dp(7), 0, 0) }); heroInfo.addView(label("Nordic Echoes", 11f, alpha(Color.WHITE, 215)).apply { setPadding(0, dp(4), 0, dp(12)) }); heroInfo.addView(label("  ▶  PLAY NOW  ", 10.5f, blue, true).apply { gravity = Gravity.CENTER; this.background = bg(Color.WHITE, 15); setPadding(dp(5), dp(9), dp(5), dp(9)); setOnClickListener { currentSong = "Glass Architecture"; currentArtist = "Astral Pulse"; playing = true; updateMini(); showNowPlaying() } }, LinearLayout.LayoutParams(-2, dp(38))); hero.addView(heroInfo, LinearLayout.LayoutParams(0, -2, 1f)); c.addView(hero, LinearLayout.LayoutParams(-1, dp(144)).apply { bottomMargin = dp(17) })
        section(c, "Recently Played"); val hs = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }; val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }; listOf("Glass Architecture" to "Astral Pulse", "Astral Pulse" to "Lunar Drift", "Midnight Drive" to "Neon Skyline", "Ocean Bloom" to "Cobalt Waves").forEachIndexed { i, pair -> recentTrack(row, pair.first, pair.second, arrayOf(blue, indigo, cyan, pink)[i]) }; hs.addView(row); c.addView(hs, LinearLayout.LayoutParams(-1, dp(151)).apply { bottomMargin = dp(12) }); section(c, "Made For You"); val mixes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }; mixCard(mixes, "Focus\nFlow", blue, cyan); mixCard(mixes, "Chill\nSpace", indigo, pink); mixCard(mixes, "Energy\nBoost", orange, pink); c.addView(mixes, LinearLayout.LayoutParams(-1, dp(142))); c.addView(miniSuggestion(), LinearLayout.LayoutParams(-1, dp(62)).apply { topMargin = dp(9) }); clearAndShow(scrollPage(c))
    }

    private fun recentTrack(parent: LinearLayout, title: String, artist: String, accent: Int) { val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(7), dp(7), dp(7), dp(4)); setOnClickListener { selectTrack(title, artist) } }; val art = FrameLayout(this).apply { this.background = gradient(accent, Color.rgb(215, 224, 255), 16); addView(label("◉", 28f, Color.WHITE, true).apply { gravity = Gravity.CENTER }); addView(label("▶", 10f, blue, true).apply { gravity = Gravity.BOTTOM or Gravity.RIGHT; this.background = bg(Color.WHITE, 12); setPadding(dp(6), dp(5), dp(6), dp(5)) }) }; card.addView(art, LinearLayout.LayoutParams(dp(102), dp(102))); card.addView(label(title, 10.5f, ink, true).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(dp(2), dp(6), 0, 0) }); card.addView(label(artist, 9f, muted).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; setPadding(dp(2), dp(3), 0, 0) }); parent.addView(card, LinearLayout.LayoutParams(dp(116), -1)) }
    private fun mixCard(parent: LinearLayout, title: String, start: Int, end: Int) { val card = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(5), 0, dp(5), 0); setOnClickListener { showLibrary() } }; val art = FrameLayout(this).apply { this.background = gradient(start, end, 17); val ring = TextView(this@NeonActivity).apply { text = "◉"; textSize = 30f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; this.background = bg(alpha(Color.WHITE, 25), 999, alpha(Color.WHITE, 35)); setPadding(dp(10), dp(10), dp(10), dp(10)) }; addView(ring, FrameLayout.LayoutParams(dp(62), dp(62), Gravity.CENTER)) }; card.addView(art, LinearLayout.LayoutParams(dp(110), dp(102))); card.addView(label(title, 10f, ink, true).apply { setPadding(dp(2), dp(7), 0, 0) }); parent.addView(card, LinearLayout.LayoutParams(dp(120), -1)) }
    private fun miniSuggestion(): View = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), dp(8), dp(12), dp(8)); this.background = bg(alpha(Color.WHITE, 220), 18, alpha(Color.WHITE, 245)); addView(label("Daily Mix", 11f, ink, true), LinearLayout.LayoutParams(0, -2, 1f)); addView(label("Explore  ›", 10f, blue, true)) }

    private fun showSearch() { currentTab = 1; headerTitle.text = "Search"; val c = column(); c.addView(label("Search", 26f, ink, true).apply { setPadding(dp(2), dp(7), 0, 0) }); c.addView(label("Find songs, artists, albums and playlists", 11.5f, muted).apply { setPadding(dp(2), dp(4), 0, dp(7)) }); val searchBox = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(11), 0, dp(9), 0); this.background = bg(Color.WHITE, 17, alpha(blue, 35)) }; searchBox.addView(label("⌕", 23f, muted, true)); val input = EditText(this).apply { hint = "Search songs, artists, albums..."; textSize = 13.5f; setTextColor(ink); setHintTextColor(muted); isSingleLine = true; background = null; setPadding(dp(9), 0, 0, 0) }; searchBox.addView(input, LinearLayout.LayoutParams(0, dp(54), 1f)); searchBox.addView(label("⌁", 19f, muted, true).apply { gravity = Gravity.CENTER }); c.addView(searchBox, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(14); bottomMargin = dp(15) }); section(c, "Browse"); arrayOf("Songs", "Albums", "Artists", "Playlists").forEach { b -> c.addView(label("  $b", 14f, ink, true).apply { gravity = Gravity.CENTER_VERTICAL; this.background = bg(Color.WHITE, 16, alpha(blue, 35)); setPadding(dp(10), 0, dp(10), 0); setOnClickListener { showLibrary() } }, LinearLayout.LayoutParams(-1, dp(50)).apply { bottomMargin = dp(8) }) }; clearAndShow(scrollPage(c)); refreshNav() }

    private fun showNowPlaying() {
        headerTitle.text = "Now Playing"; val c = column(); c.gravity = Gravity.CENTER_HORIZONTAL
        val titleView = label(currentSong, 23f, ink, true); val artistView = label(currentArtist, 11f, muted).apply { setPadding(0, dp(5), 0, dp(14)) }
        c.addView(label("NOW PLAYING", 9f, blue, true).apply { setPadding(0, dp(10), 0, dp(5)) }); c.addView(titleView); c.addView(artistView)
        val disc = FrameLayout(this).apply { this.background = bg(Color.rgb(252, 253, 255), 999, alpha(blue, 90)); elevation = dp(5).toFloat() }
        val artwork = ArtworkViewFactory.create(this@NeonActivity, currentLocalTrack(), dp(214)).apply {
            background = bg(Color.rgb(25, 27, 34), 999, alpha(Color.WHITE, 40))
        }
        nowPlayingArtwork = artwork
        disc.addView(artwork, FrameLayout.LayoutParams(dp(214), dp(214), Gravity.CENTER)); c.addView(disc, LinearLayout.LayoutParams(dp(274), dp(274)).apply { topMargin = dp(8); bottomMargin = dp(18) })
        val elapsed = label(formatTime(PlaybackController.position()), 9f, muted); val total = label(formatTime(PlaybackController.duration()), 9f, muted); nowPlayingElapsed = elapsed; nowPlayingTotal = total
        val times = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; addView(elapsed, LinearLayout.LayoutParams(0, -2, 1f)); addView(total, LinearLayout.LayoutParams(0, -2, 1f).apply { gravity = Gravity.RIGHT }) }; c.addView(times, LinearLayout.LayoutParams(-1, dp(18)))
        val seek = SeekBar(this).apply { max = 1000; progress = progressForPlayer(); progressTintList = android.content.res.ColorStateList.valueOf(blue); thumbTintList = android.content.res.ColorStateList.valueOf(blue); setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) { if (fromUser) { val d = PlaybackController.duration(); if (d > 0) PlaybackController.seek((d * value / 1000L).coerceIn(0L, d)) } }; override fun onStartTrackingTouch(bar: SeekBar?) = Unit; override fun onStopTrackingTouch(bar: SeekBar?) = Unit }) }; nowPlayingSeek = seek; c.addView(seek, LinearLayout.LayoutParams(-1, dp(40)))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp(7), 0, 0) }
        actions.addView(label(if (shuffle) "🔀" else "⇄", 18f, if (shuffle) blue else muted).apply { setOnClickListener { shuffle = !shuffle; PlaybackController.setShuffle(shuffle); setTextColor(if (shuffle) blue else muted) } }, LinearLayout.LayoutParams(dp(45), dp(55)))
        actions.addView(label("|◀", 21f, ink, true).apply { gravity = Gravity.CENTER; setOnClickListener { PlaybackController.previous() } }, LinearLayout.LayoutParams(dp(50), dp(55)))
        val playButton = label(if (playing) "Ⅱ" else "▶", 24f, Color.WHITE, true).apply { gravity = Gravity.CENTER; this.background = bg(blue, 20); setOnClickListener { PlaybackController.toggle(); syncPlayerState() } }; nowPlayingPlay = playButton; actions.addView(playButton, LinearLayout.LayoutParams(dp(64), dp(60)))
        actions.addView(label("▶|", 21f, ink, true).apply { gravity = Gravity.CENTER; setOnClickListener { PlaybackController.next() } }, LinearLayout.LayoutParams(dp(50), dp(55)))
        actions.addView(label(if (repeat) "↻" else "↻", 19f, if (repeat) blue else muted).apply { setOnClickListener { repeat = !repeat; PlaybackController.setRepeat(repeat); setTextColor(if (repeat) blue else muted) } }, LinearLayout.LayoutParams(dp(45), dp(55)))
        c.addView(actions, LinearLayout.LayoutParams(-1, dp(70)))
        val tools = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(0, dp(5), 0, 0) }
        tools.addView(label("Lyrics", 10.5f, blue, true).apply { gravity = Gravity.CENTER; this.background = bg(alpha(blue, 18), 15); setPadding(dp(18), dp(9), dp(18), dp(9)); setOnClickListener { startActivity(android.content.Intent(this@NeonActivity, LyricsActivity::class.java).apply { putExtra("title", currentSong); putExtra("artist", currentArtist); putExtra("album", "") }) } }, LinearLayout.LayoutParams(-2, dp(38)))
        tools.addView(label("Queue", 10.5f, ink, true).apply { gravity = Gravity.CENTER; this.background = bg(Color.WHITE, 15); setPadding(dp(18), dp(9), dp(18), dp(9)) }, LinearLayout.LayoutParams(-2, dp(38)).apply { leftMargin = dp(8) }); c.addView(tools)
        clearAndShow(scrollPage(c))
    }

    private fun currentLocalTrack(): LocalTrack? = localTracks.firstOrNull {
        it.title.equals(currentSong, ignoreCase = true) && it.artist.equals(currentArtist, ignoreCase = true)
    }

    private fun refreshArtwork(force: Boolean = false) {
        val track = currentLocalTrack()
        val key = "${track?.id ?: 0L}:$currentSong:$currentArtist"
        if (!force && key == lastArtworkKey) return
        lastArtworkKey = key
        miniArtwork?.let { box ->
            box.removeAllViews()
            box.addView(ArtworkViewFactory.create(this, track, dp(50)), FrameLayout.LayoutParams(-1, -1))
        }
        nowPlayingArtwork?.let { box ->
            val parent = box.parent as? ViewGroup
            if (parent != null) {
                val index = parent.indexOfChild(box)
                parent.removeViewAt(index)
                val replacement = ArtworkViewFactory.create(this, track, dp(214))
                nowPlayingArtwork = replacement
                parent.addView(replacement, index, FrameLayout.LayoutParams(dp(214), dp(214), Gravity.CENTER))
            }
        }
    }

    private fun progressForPlayer(): Int { val d = PlaybackController.duration(); if (d <= 0L) return 0; return ((PlaybackController.position() * 1000L) / d).coerceIn(0L, 1000L).toInt() }
    private fun formatTime(ms: Long): String { val total = (ms.coerceAtLeast(0L) / 1000L).toInt(); return "%d:%02d".format(total / 60, total % 60) }

    private fun showLibrary() { currentTab = 2; headerTitle.text = "Library"; val c = column(); c.addView(label("Your Library", 26f, ink, true).apply { setPadding(dp(2), dp(7), 0, dp(5)) }); c.addView(label("Music stored on this device", 11.5f, muted).apply { setPadding(dp(2), 0, 0, dp(14)) }); val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }; controls.addView(label("Favorites", 10.5f, blue, true).apply { gravity = Gravity.CENTER; this.background = bg(alpha(blue, 18), 14); setPadding(dp(15), dp(9), dp(15), dp(9)) }, LinearLayout.LayoutParams(-2, dp(38))); controls.addView(label("Recently Played", 10.5f, ink, true).apply { gravity = Gravity.CENTER; this.background = bg(Color.WHITE, 14); setPadding(dp(15), dp(9), dp(15), dp(9)) }, LinearLayout.LayoutParams(-2, dp(38)).apply { leftMargin = dp(7) }); c.addView(controls, LinearLayout.LayoutParams(-1, dp(42)).apply { bottomMargin = dp(12) }); if (localTracks.isNotEmpty()) localTracks.take(50).forEachIndexed { i, t -> libraryItem(c, "♪", t.title, t.artist.ifBlank { "Local track" }, arrayOf(blue, indigo, cyan, pink, orange)[i % 5]) } else c.addView(label("Grant music permission, then tap SCAN to find MP3/WAV and other audio stored on this phone.", 12f, muted).apply { setPadding(dp(8), dp(18), dp(8), dp(18)) }); clearAndShow(scrollPage(c)); refreshNav() }

    private fun libraryItem(c: LinearLayout, icon: String, title: String, sub: String, accent: Int) { val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(7), dp(7), dp(7), dp(7)); this.background = bg(alpha(Color.WHITE, 218), 18); setOnClickListener { selectTrack(title, sub) } }; val track = localTracks.firstOrNull { it.title == title && it.artist == sub }; row.addView(ArtworkViewFactory.create(this@NeonActivity, track, dp(56)), LinearLayout.LayoutParams(dp(56), dp(56))); val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, dp(5), 0) }; info.addView(label(title, 12.5f, ink, true)); info.addView(label(sub, 10f, muted).apply { setPadding(0, dp(4), 0, 0) }); row.addView(info, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(label("⋮", 19f, muted)); c.addView(row, LinearLayout.LayoutParams(-1, dp(70)).apply { bottomMargin = dp(8) }) }
    private fun showEqualizer() { currentTab = 3; headerTitle.text = "Equalizer"; val c = column(); c.addView(label("Equalizer", 26f, ink, true).apply { setPadding(dp(2), dp(7), 0, dp(4)) }); c.addView(label("Fine-tune your sound", 11.5f, muted).apply { setPadding(dp(2), 0, 0, dp(15)) }); val presetRow = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }; val presets = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }; arrayOf("Custom", "Rock", "Pop", "Electronic", "Jazz").forEachIndexed { i, p -> presets.addView(label(" $p ", 10.5f, if (i == 0) Color.WHITE else muted, true).apply { gravity = Gravity.CENTER; this.background = bg(if (i == 0) blue else alpha(Color.WHITE, 220), 14); setPadding(dp(8), dp(9), dp(8), dp(9)) }, LinearLayout.LayoutParams(-2, dp(38)).apply { rightMargin = dp(7) }) }; presetRow.addView(presets); c.addView(presetRow, LinearLayout.LayoutParams(-1, dp(42)).apply { bottomMargin = dp(14) }); val panel = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(7), dp(18), dp(7), dp(15)); this.background = bg(alpha(Color.WHITE, 225), 22, alpha(Color.WHITE, 245)); elevation = dp(2).toFloat() }; val freqs = arrayOf("60Hz", "230Hz", "910Hz", "3.6k", "14k"); val levels = intArrayOf(72, 48, 60, 80, 54); freqs.forEachIndexed { i, f -> val band = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }; band.addView(label("+12", 8f, muted).apply { gravity = Gravity.CENTER }); val seek = SeekBar(this).apply { max = 100; this.progress = levels[i]; rotation = -90f; progressTintList = android.content.res.ColorStateList.valueOf(blue); thumbTintList = android.content.res.ColorStateList.valueOf(blue) }; band.addView(seek, LinearLayout.LayoutParams(dp(45), dp(150))); band.addView(label(f, 9f, ink, true).apply { gravity = Gravity.CENTER }); panel.addView(band, LinearLayout.LayoutParams(0, dp(190), 1f)) }; c.addView(panel, LinearLayout.LayoutParams(-1, dp(222))); val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }; arrayOf("Bass Boost", "Virtualizer", "Loudness").forEach { name -> controls.addView(label("○\n$name", 10f, ink, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(70), 1f)) }; c.addView(controls); clearAndShow(scrollPage(c)); refreshNav() }
    private fun showSettings() { currentTab = 4; headerTitle.text = "Settings"; val c = column(); c.addView(label("Settings", 26f, ink, true).apply { setPadding(dp(2), dp(7), 0, dp(14)) }); settingRow(c, "Offline music", "On", true); settingRow(c, "Shake to change song", "On", true); settingRow(c, "High quality audio", "On", true); settingRow(c, "Gapless playback", "On", true); settingRow(c, "Theme", "Nordic Glass", false); settingRow(c, "Sleep timer", "Off", false); clearAndShow(scrollPage(c)); refreshNav() }
    private fun settingRow(c: LinearLayout, title: String, value: String, toggle: Boolean) { val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(13), dp(7), dp(10), dp(7)); this.background = bg(alpha(Color.WHITE, 222), 16) }; row.addView(label(title, 11.5f, ink, true), LinearLayout.LayoutParams(0, dp(48), 1f)); if (toggle) row.addView(label("●", 18f, blue, true).apply { gravity = Gravity.CENTER }) else row.addView(label("$value  ›", 10.5f, muted).apply { gravity = Gravity.CENTER }); c.addView(row, LinearLayout.LayoutParams(-1, dp(62)).apply { bottomMargin = dp(7) }) }
    private fun selectTrack(title: String, artist: String) { currentSong = title; currentArtist = artist; playing = false; updateMini(); refreshArtwork(); showNowPlaying() }
}
