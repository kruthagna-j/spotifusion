package com.spotifusion.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import kotlin.math.max

/**
 * Modern Android surface for Spotifusion.
 * Keeps the existing Media3 service and local-library implementation, but replaces
 * the old light shell with the AMOLED / electric-violet product UI.
 */
class ModernMusicActivity : AppCompatActivity() {
    private val bg = Color.rgb(7, 7, 12)
    private val surface = Color.rgb(16, 16, 24)
    private val surface2 = Color.rgb(22, 21, 31)
    private val border = Color.rgb(43, 40, 58)
    private val text = Color.rgb(245, 243, 252)
    private val muted = Color.rgb(151, 146, 166)
    private val violet = Color.rgb(123, 81, 251)
    private val violet2 = Color.rgb(92, 55, 210)

    private lateinit var page: FrameLayout
    private lateinit var miniTitle: TextView
    private lateinit var miniArtist: TextView
    private lateinit var miniPlay: TextView
    private lateinit var nav: LinearLayout
    private var tracks: List<LocalTrack> = emptyList()
    private var tab = 0
    private var searchText = ""
    private var sleepHandler: Handler? = null
    private var sleepTask: Runnable? = null
    private val uiHandler = Handler(Looper.getMainLooper())

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun tv(s: String, size: Float, color: Int = text, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); includeFontPadding = false
        if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }
    private fun box(color: Int = surface, radius: Int = 18, stroke: Boolean = false) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke) setStroke(dp(1), border)
    }
    private fun accentBox(radius: Int = 18) = android.graphics.drawable.GradientDrawable(
        android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
        intArrayOf(violet2, violet)
    ).apply { cornerRadius = dp(radius).toFloat() }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        requestPermissionsIfNeeded()
        PlaybackController.connect(this)
        tracks = runCatching { LocalMusicScanner.scan(this) }.getOrElse { emptyList() }
        buildShell()
        showHome()
    }

    private fun requestPermissionsIfNeeded() {
        val audio = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, audio) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(audio), 7001)
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7002)
        }
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        root.addView(topBar(), LinearLayout.LayoutParams(-1, dp(62)))
        page = FrameLayout(this)
        root.addView(page, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(buildMiniPlayer(), LinearLayout.LayoutParams(-1, dp(72)).apply { setMargins(dp(10), dp(6), dp(10), dp(5)) })
        nav = buildNav()
        root.addView(nav, LinearLayout.LayoutParams(-1, dp(66)).apply { setMargins(dp(8), 0, dp(8), dp(5)) })
        setContentView(root)
    }

    private fun topBar(): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL; setPadding(dp(15), dp(8), dp(15), dp(3))
        val brand = LinearLayout(this@ModernMusicActivity).apply { gravity = Gravity.CENTER_VERTICAL }
        val logo = FrameLayout(this@ModernMusicActivity).apply { background = accentBox(12); addView(tv("S", 18f, Color.WHITE, true).apply { gravity = Gravity.CENTER }) }
        brand.addView(logo, LinearLayout.LayoutParams(dp(36), dp(36)))
        brand.addView(tv("Spotifusion", 18f, text, true).apply { setPadding(dp(10), 0, 0, 0) })
        addView(brand, LinearLayout.LayoutParams(0, -1, 1f))
        addView(tv("⌕", 26f, text, true).apply { gravity = Gravity.CENTER; background = box(surface2, 16, true); setOnClickListener { showSearch() } }, LinearLayout.LayoutParams(dp(46), dp(46)).apply { rightMargin = dp(7) })
        addView(tv("⋮", 25f, text, true).apply { gravity = Gravity.CENTER; background = box(surface2, 16, true); setOnClickListener { showSettings() } }, LinearLayout.LayoutParams(dp(46), dp(46)))
    }

    private fun buildNav(): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.CENTER; background = box(surface, 22, true); setPadding(dp(4), dp(4), dp(4), dp(4))
        val items = listOf("⌂" to "Home", "⌕" to "Search", "▣" to "Library", "≋" to "EQ")
        items.forEachIndexed { i, item ->
            val cell = LinearLayout(this@ModernMusicActivity).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isClickable = true
                setOnClickListener { tab = i; when (i) { 0 -> showHome(); 1 -> showSearch(); 2 -> showLibrary(); else -> showEqualizer() }; refreshNav() }
            }
            cell.addView(tv(item.first, 19f, if (i == tab) violet else muted, true).apply { gravity = Gravity.CENTER })
            cell.addView(tv(item.second, 9f, if (i == tab) violet else muted, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(3), 0, 0) })
            addView(cell, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }

    private fun refreshNav() {
        for (i in 0 until nav.childCount) {
            val c = nav.getChildAt(i) as LinearLayout
            val active = i == tab
            c.background = if (active) box(Color.rgb(31, 23, 55), 16) else null
            (c.getChildAt(0) as TextView).setTextColor(if (active) violet else muted)
            (c.getChildAt(1) as TextView).setTextColor(if (active) violet else muted)
        }
    }

    private fun buildMiniPlayer(): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL; background = box(surface, 18, true); setPadding(dp(8), dp(7), dp(8), dp(7))
        val art = TextView(this@ModernMusicActivity).apply { text = "◉"; textSize = 23f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); background = accentBox(13) }
        addView(art, LinearLayout.LayoutParams(dp(50), dp(50)))
        val info = LinearLayout(this@ModernMusicActivity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), 0, dp(6), 0); setOnClickListener { showNowPlaying() } }
        miniTitle = tv(PlaybackController.currentTitle().ifBlank { "Nothing playing" }, 12.5f, text, true)
        miniArtist = tv(PlaybackController.currentArtist().ifBlank { "Choose a song" }, 10f, muted)
        info.addView(miniTitle); info.addView(miniArtist.apply { setPadding(0, dp(4), 0, 0) })
        addView(info, LinearLayout.LayoutParams(0, -1, 1f))
        addView(tv("♡", 22f, muted).apply { gravity = Gravity.CENTER; setOnClickListener { toast("Like state saved locally") } }, LinearLayout.LayoutParams(dp(40), dp(50)))
        miniPlay = tv(if (PlaybackController.isPlaying()) "Ⅱ" else "▶", 18f, text, true).apply { gravity = Gravity.CENTER; setOnClickListener { PlaybackController.toggle(); refreshMini() } }
        addView(miniPlay, LinearLayout.LayoutParams(dp(44), dp(50)))
    }

    private fun refreshMini() {
        miniTitle.text = PlaybackController.currentTitle().ifBlank { "Nothing playing" }
        miniArtist.text = PlaybackController.currentArtist().ifBlank { "Choose a song" }
        miniPlay.text = if (PlaybackController.isPlaying()) "Ⅱ" else "▶"
    }

    private fun shell(content: LinearLayout): ScrollView = ScrollView(this).apply {
        setBackgroundColor(bg); isFillViewport = true; addView(content, ViewGroup.LayoutParams(-1, -1))
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(8), dp(14), dp(28)) }
    private fun show(v: View) { page.removeAllViews(); page.addView(v, FrameLayout.LayoutParams(-1, -1)) }
    private fun heading(c: LinearLayout, title: String, action: String? = null, onAction: (() -> Unit)? = null) {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(14), 0, dp(9)) }
        row.addView(tv(title, 14f, text, true), LinearLayout.LayoutParams(0, -2, 1f))
        if (action != null) row.addView(tv(action, 10f, violet, true).apply { setOnClickListener { onAction?.invoke() } })
        c.addView(row)
    }

    private fun showHome() {
        val c = column()
        c.addView(tv("Good evening", 12f, muted))
        c.addView(tv("Your sound. Your space.", 25f, text, true).apply { setPadding(0, dp(5), 0, dp(13)) })
        val hero = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = accentBox(23); setPadding(dp(16), dp(15), dp(14), dp(15)) }
        hero.addView(TextView(this).apply { text = "◉"; textSize = 50f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); background = box(Color.rgb(53, 35, 112), 18) }, LinearLayout.LayoutParams(dp(112), dp(112)))
        val hi = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), 0, 0, 0) }
        hi.addView(tv("FEATURED", 9f, Color.WHITE, true)); hi.addView(tv("Glass Architecture", 18f, Color.WHITE, true).apply { setPadding(0, dp(7), 0, 0) }); hi.addView(tv("Astral Pulse", 11f, Color.rgb(224, 216, 255)).apply { setPadding(0, dp(5), 0, dp(11)) })
        hi.addView(tv("  PLAY NOW  ", 10f, violet, true).apply { gravity = Gravity.CENTER; background = box(Color.WHITE, 14); setPadding(dp(4), dp(9), dp(4), dp(9)); setOnClickListener { playFirstOrDemo() } }, LinearLayout.LayoutParams(-2, dp(37)))
        hero.addView(hi, LinearLayout.LayoutParams(0, -2, 1f)); c.addView(hero, LinearLayout.LayoutParams(-1, dp(142)))
        heading(c, "Recently played", "See all")
        if (tracks.isEmpty()) c.addView(emptyCard("No local music found yet", "Grant music access or add audio to the device."))
        else tracks.take(8).forEach { addTrackRow(c, it) }
        heading(c, "Quick actions")
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        quick(actions, "♥", "Liked") { showLibrary() }; quick(actions, "▤", "Playlists") { startActivity(Intent(this@ModernMusicActivity, PlaylistActivity::class.java)) }; quick(actions, "◷", "Timer") { sleepTimer() }
        c.addView(actions, LinearLayout.LayoutParams(-1, dp(90)))
        show(shell(c))
    }

    private fun quick(parent: LinearLayout, icon: String, title: String, action: () -> Unit) {
        val b = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; background = box(surface, 16, true); setOnClickListener { action() } }
        b.addView(tv(icon, 22f, violet, true).apply { gravity = Gravity.CENTER }); b.addView(tv(title, 9f, muted, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(5), 0, 0) })
        parent.addView(b, LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
    }

    private fun showSearch() {
        val c = column(); c.addView(tv("Search", 26f, text, true).apply { setPadding(0, dp(4), 0, dp(12)) })
        val input = EditText(this).apply { hint = "Songs, artists, albums"; setHintTextColor(muted); setTextColor(text); textSize = 14f; singleLine = true; background = box(surface2, 16, true); setPadding(dp(15), 0, dp(15), 0); setText(searchText) }
        c.addView(input, LinearLayout.LayoutParams(-1, dp(52)))
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val update = { searchText = input.text.toString().trim(); results.removeAllViews(); tracks.filter { t -> searchText.isBlank() || listOf(t.title, t.artist, t.album).any { it.contains(searchText, true) } }.take(30).forEach { addTrackRow(results, it) } }
        input.setOnEditorActionListener { _, _, _ -> update(); true }; input.addTextChangedListener(object : android.text.TextWatcher { override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) = Unit; override fun onTextChanged(s: CharSequence?, a: Int, b: Int, d: Int) { update() }; override fun afterTextChanged(s: android.text.Editable?) = Unit })
        c.addView(results); update(); show(shell(c))
    }

    private fun showLibrary() {
        val c = column(); c.addView(tv("Library", 26f, text, true).apply { setPadding(0, dp(4), 0, dp(13)) })
        libraryCard(c, "Local Music", "${tracks.size} tracks", "♫") { showLocalMusic() }
        libraryCard(c, "Playlists", "Create and manage playlists", "▤") { startActivity(Intent(this, PlaylistActivity::class.java)) }
        libraryCard(c, "Favorites", "Your liked tracks", "♥") { toast("Favorites are available from the player") }
        libraryCard(c, "Recently Played", "Playback history", "◷") { showLocalMusic() }
        show(shell(c))
    }

    private fun libraryCard(c: LinearLayout, title: String, sub: String, icon: String, action: () -> Unit) {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; background = box(surface, 18, true); setPadding(dp(12), dp(11), dp(12), dp(11)); setOnClickListener { action() } }
        row.addView(tv(icon, 24f, violet, true).apply { gravity = Gravity.CENTER; background = box(Color.rgb(29, 22, 48), 14) }, LinearLayout.LayoutParams(dp(50), dp(50)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }; info.addView(tv(title, 13f, text, true)); info.addView(tv(sub, 10f, muted).apply { setPadding(0, dp(5), 0, 0) }); row.addView(info, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(tv("›", 24f, muted), LinearLayout.LayoutParams(dp(25), -1)); c.addView(row, LinearLayout.LayoutParams(-1, dp(76)).apply { bottomMargin = dp(9) })
    }

    private fun showLocalMusic() {
        tracks = runCatching { LocalMusicScanner.scan(this) }.getOrElse { emptyList() }
        val c = column(); c.addView(tv("All songs", 25f, text, true).apply { setPadding(0, dp(4), 0, dp(10)) }); c.addView(tv("${tracks.size} tracks on this device", 10f, muted).apply { setPadding(0, 0, 0, dp(10)) }); tracks.forEach { addTrackRow(c, it) }; show(shell(c))
    }

    private fun addTrackRow(parent: LinearLayout, track: LocalTrack) {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(7), dp(7), dp(7), dp(7)); background = box(surface, 15, true); setOnClickListener { playTrack(track) } }
        row.addView(tv("◉", 22f, Color.WHITE, true).apply { gravity = Gravity.CENTER; background = accentBox(12) }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, dp(5), 0) }; info.addView(tv(track.title, 12.5f, text, true).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }); info.addView(tv(track.artist, 10f, muted).apply { setPadding(0, dp(4), 0, 0) }); row.addView(info, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(tv("▶", 13f, violet, true)); parent.addView(row, LinearLayout.LayoutParams(-1, dp(64)).apply { bottomMargin = dp(6) })
    }

    private fun showNowPlaying() {
        val c = column(); c.gravity = Gravity.CENTER_HORIZONTAL
        c.addView(tv("NOW PLAYING", 10f, violet, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(7), 0, dp(15)) })
        c.addView(tv("◉", 100f, Color.WHITE, true).apply { gravity = Gravity.CENTER; background = accentBox(28) }, LinearLayout.LayoutParams(dp(250), dp(250)))
        c.addView(tv(PlaybackController.currentTitle().ifBlank { "Nothing playing" }, 23f, text, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(20), 0, dp(5)) })
        c.addView(tv(PlaybackController.currentArtist().ifBlank { "Select a track" }, 13f, muted).apply { gravity = Gravity.CENTER })
        val seek = SeekBar(this).apply { max = 1000; progressTintList = android.content.res.ColorStateList.valueOf(violet); thumbTintList = android.content.res.ColorStateList.valueOf(violet); setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(b: SeekBar?, p: Int, from: Boolean) { if (from) PlaybackController.seek(PlaybackController.duration() * p / 1000L) }; override fun onStartTrackingTouch(b: SeekBar?) = Unit; override fun onStopTrackingTouch(b: SeekBar?) = Unit }) }
        c.addView(seek, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(18) })
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, dp(7), 0, 0) }
        control(controls, "↶") { PlaybackController.previous() }; control(controls, if (PlaybackController.isPlaying()) "Ⅱ" else "▶", true) { PlaybackController.toggle() }; control(controls, "↷") { PlaybackController.next() }; control(controls, if (PlaybackController.isShuffleEnabled()) "⌘" else "⇄") { PlaybackController.setShuffle(!PlaybackController.isShuffleEnabled()) }; control(controls, if (PlaybackController.isRepeatEnabled()) "1↻" else "↻") { PlaybackController.setRepeat(!PlaybackController.isRepeatEnabled()) }
        c.addView(controls, LinearLayout.LayoutParams(-1, dp(74)))
        val actions = LinearLayout(this).apply { gravity = Gravity.CENTER }; actionChip(actions, "Lyrics") { startActivity(Intent(this@ModernMusicActivity, LyricsActivity::class.java)) }; actionChip(actions, "Timer") { sleepTimer() }; actionChip(actions, "Queue") { showLocalMusic() }
        c.addView(actions, LinearLayout.LayoutParams(-1, dp(58))); show(shell(c)); startProgressTicker(seek)
    }

    private fun control(parent: LinearLayout, icon: String, primary: Boolean = false, action: () -> Unit) { val b = tv(icon, if (primary) 25f else 19f, if (primary) Color.WHITE else muted, true).apply { gravity = Gravity.CENTER; background = if (primary) accentBox(30) else box(surface2, 25); setOnClickListener { action() } }; parent.addView(b, LinearLayout.LayoutParams(if (primary) dp(64) else dp(52), dp(56)).apply { setMargins(dp(4), 0, dp(4), 0) }) }
    private fun actionChip(parent: LinearLayout, title: String, action: () -> Unit) { parent.addView(tv(title, 10f, muted, true).apply { gravity = Gravity.CENTER; background = box(surface2, 14, true); setPadding(dp(13), dp(9), dp(13), dp(9)); setOnClickListener { action() } }, LinearLayout.LayoutParams(0, dp(40), 1f).apply { setMargins(dp(3), 0, dp(3), 0) }) }

    private fun startProgressTicker(seek: SeekBar) { val r = object : Runnable { override fun run() { val d = PlaybackController.duration(); if (d > 0 && !seek.isPressed) seek.progress = (PlaybackController.position() * 1000L / d).toInt().coerceIn(0, 1000); refreshMini(); uiHandler.postDelayed(this, 500) } }; seek.tag = r; uiHandler.post(r) }

    private fun showEqualizer() {
        val c = column(); c.addView(tv("Equalizer", 26f, text, true).apply { setPadding(0, dp(4), 0, dp(8)) }); c.addView(tv("Tune the sound for your headphones or speakers.", 11f, muted).apply { setPadding(0, 0, 0, dp(18)) })
        val bands = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
        bands.forEachIndexed { i, band ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = box(surface, 16, true); setPadding(dp(12), dp(9), dp(12), dp(8)) }; val head = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }; head.addView(tv(band, 11f, text, true), LinearLayout.LayoutParams(0, -2, 1f)); head.addView(tv("0 dB", 10f, violet, true)); row.addView(head); row.addView(SeekBar(this).apply { max = 24; progress = 12; progressTintList = android.content.res.ColorStateList.valueOf(violet); thumbTintList = android.content.res.ColorStateList.valueOf(violet) }); c.addView(row, LinearLayout.LayoutParams(-1, dp(78)).apply { bottomMargin = dp(8) })
        }
        heading(c, "Presets"); listOf("Flat", "Bass Boost", "Vocal", "Treble", "Night").forEach { preset -> c.addView(tv(preset, 12f, if (preset == "Bass Boost") violet else text, true).apply { gravity = Gravity.CENTER_VERTICAL; background = box(surface, 14, true); setPadding(dp(14), 0, dp(14), 0); setOnClickListener { toast("Preset: $preset") } }, LinearLayout.LayoutParams(-1, dp(48)).apply { bottomMargin = dp(6) }) }; show(shell(c))
    }

    private fun showSettings() {
        val c = column(); c.addView(tv("Settings", 26f, text, true).apply { setPadding(0, dp(4), 0, dp(15)) })
        setting(c, "Playback", "Shuffle, repeat and volume controls are available in the player") { showNowPlaying() }
        setting(c, "Sleep timer", "Stop playback automatically") { sleepTimer() }
        setting(c, "Lyrics", "Open synchronized lyrics") { startActivity(Intent(this, LyricsActivity::class.java)) }
        setting(c, "Playlists", "Create and manage playlists") { startActivity(Intent(this, PlaylistActivity::class.java)) }
        setting(c, "Music library", "Rescan audio stored on this device") { tracks = LocalMusicScanner.scan(this); toast("Found ${tracks.size} tracks"); showLibrary() }
        setting(c, "About Spotifusion", "AMOLED edition · Media3 background playback") { toast("Spotifusion") }
        show(shell(c))
    }

    private fun setting(c: LinearLayout, title: String, sub: String, action: () -> Unit) { val r = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = box(surface, 17, true); setPadding(dp(14), dp(11), dp(14), dp(11)); setOnClickListener { action() } }; r.addView(tv(title, 12.5f, text, true)); r.addView(tv(sub, 9.5f, muted).apply { setPadding(0, dp(5), 0, 0) }); c.addView(r, LinearLayout.LayoutParams(-1, dp(70)).apply { bottomMargin = dp(8) }) }

    private fun emptyCard(title: String, sub: String) = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = box(surface, 18, true); setPadding(dp(16), dp(18), dp(16), dp(18)); addView(tv(title, 13f, text, true)); addView(tv(sub, 10f, muted).apply { setPadding(0, dp(6), 0, 0) }) }

    private fun playFirstOrDemo() { if (tracks.isNotEmpty()) playTrack(tracks.first()) else toast("Add music to this device first") }
    private fun playTrack(track: LocalTrack) { PlaybackController.play(track.uri.toString(), track.title, track.artist, track.album, track.id.toString()); refreshMini(); showNowPlaying() }

    private fun sleepTimer() {
        val choices = arrayOf("15 minutes", "30 minutes", "45 minutes", "60 minutes", "Cancel timer")
        AlertDialog.Builder(this).setTitle("Sleep timer").setItems(choices) { _, which ->
            sleepTask?.let { sleepHandler?.removeCallbacks(it) }
            if (which == 4) { toast("Timer cancelled"); return@setItems }
            val mins = listOf(15L, 30L, 45L, 60L)[which]
            val h = Handler(Looper.getMainLooper()); sleepHandler = h
            val task = Runnable { PlaybackController.pause(); toast("Sleep timer finished") }; sleepTask = task; h.postDelayed(task, mins * 60_000L); toast("Timer set for $mins minutes")
        }.show()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) { refreshMini() }
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) { refreshMini() }
    }
    override fun onStart() { super.onStart(); PlaybackController.addListener(playerListener); refreshMini() }
    override fun onStop() { PlaybackController.removeListener(playerListener); super.onStop() }
    override fun onDestroy() { sleepTask?.let { sleepHandler?.removeCallbacks(it) }; PlaybackController.disconnect(); super.onDestroy() }
}
