package com.spotifusion.app

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.app.AppCompatActivity

class NeonActivity : AppCompatActivity() {
    private val bg = Color.rgb(5, 7, 7)
    private val panel = Color.rgb(12, 16, 16)
    private val panel2 = Color.rgb(18, 23, 23)
    private val neon = Color.rgb(57, 255, 20)
    private val white = Color.WHITE
    private val gray = Color.rgb(150, 160, 160)
    private val dim = Color.rgb(85, 100, 100)

    private data class Track(val id: Long, val title: String, val artist: String, val uri: Uri)
    private val tracks = mutableListOf<Track>()
    private val favorites = mutableSetOf<Long>()
    private var player: MediaPlayer? = null
    private var current: Track? = null
    private var currentIndex = -1
    private var root: FrameLayout? = null
    private var content: FrameLayout? = null
    private var mini: LinearLayout? = null
    private var page = "home"

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun box(color: Int, radius: Int = 18, stroke: Int = 0): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke > 0) setStroke(dp(1), stroke)
    }
    private fun tv(s: String, size: Float, color: Int = white, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); includeFontPadding = false
        if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private fun spacer(h: Int) = Space(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        loadFavorites()
        askAudioPermission()
    }

    private fun askAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) start() else requestPermissions(arrayOf(permission), 42)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        start()
    }

    private fun start() {
        loadMusic()
        buildShell()
        showHome()
    }

    private fun loadFavorites() {
        getSharedPreferences("spotifusion", MODE_PRIVATE).getStringSet("favorites", emptySet())?.forEach { it.toLongOrNull()?.let(favorites::add) }
    }

    private fun loadMusic() {
        tracks.clear()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST)
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            while (c.moveToNext()) {
                tracks += Track(c.getLong(id), c.getString(title).orEmpty().ifBlank { "Unknown track" }, c.getString(artist).orEmpty().ifBlank { "Unknown artist" }, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, c.getLong(id)))
            }
        }
    }

    private fun buildShell() {
        root = FrameLayout(this).apply { setBackgroundColor(bg) }
        content = FrameLayout(this).apply { setBackgroundColor(bg) }
        root!!.addView(content, FrameLayout.LayoutParams(-1, -1))
        mini = miniPlayer()
        root!!.addView(mini, FrameLayout.LayoutParams(-1, dp(70), Gravity.BOTTOM).apply { setMargins(dp(12), 0, dp(12), dp(78)) })
        root!!.addView(bottomBar(), FrameLayout.LayoutParams(-1, dp(72), Gravity.BOTTOM))
        setContentView(root)
    }

    private fun bottomBar() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER
        setPadding(dp(10), dp(5), dp(10), dp(5)); setBackgroundColor(Color.rgb(8, 11, 11))
        val items = listOf("⌂" to "HOME", "⌕" to "SEARCH", "◉" to "LIBRARY", "☷" to "EQ", "⚙" to "SETTINGS")
        items.forEachIndexed { index, pair ->
            val b = LinearLayout(this@NeonActivity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isClickable = true }
            b.addView(tv(pair.first, 24f, dim, false).apply { gravity = Gravity.CENTER })
            b.addView(tv(pair.second, 8f, dim, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(4), 0, 0) })
            b.setOnClickListener { when (pair.second) { "HOME" -> showHome(); "SEARCH" -> showSearch(); "LIBRARY" -> showLibrary(); "EQ" -> showEqualizer(); "SETTINGS" -> showSettings() } }
            addView(b, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }

    private fun header(title: String, subtitle: String? = null) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(tv(title, 28f, white, true))
        if (subtitle != null) addView(tv(subtitle, 12f, gray).apply { setPadding(0, dp(6), 0, 0) })
    }

    private fun pageRoot(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(22), dp(18), dp(110))
    }

    private fun scroll(c: View): ScrollView = ScrollView(this).apply {
        isVerticalScrollBarEnabled = false; clipToPadding = false; addView(c)
    }

    private fun show(v: View) { content?.removeAllViews(); content?.addView(scroll(v), FrameLayout.LayoutParams(-1, -1)) }

    private fun neonLine() = View(this).apply {
        setBackgroundColor(neon); alpha = .8f
        layoutParams = LinearLayout.LayoutParams(dp(38), dp(2)).apply { topMargin = dp(10); bottomMargin = dp(14) }
    }

    private fun showHome() {
        page = "home"
        val c = pageRoot()
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val brand = LinearLayout(this@NeonActivity).apply { orientation = LinearLayout.VERTICAL }
        brand.addView(tv("SPOTIFUSION", 21f, neon, true)); brand.addView(tv("NEON MUSIC SYSTEM", 8f, dim, true).apply { letterSpacing = .22f; setPadding(0, dp(5), 0, 0) })
        top.addView(brand, LinearLayout.LayoutParams(0, dp(52), 1f))
        top.addView(tv("◌", 26f, neon).apply { gravity = Gravity.CENTER; setOnClickListener { toast("Notifications") } }, LinearLayout.LayoutParams(dp(46), dp(46)))
        c.addView(top)
        c.addView(neonLine())
        c.addView(tv("GOOD EVENING", 11f, dim, true))
        c.addView(tv("Your sound.\nYour space.", 31f, white, true).apply { setPadding(0, dp(8), 0, dp(4)) })
        c.addView(tv("A neon player built around your music library.", 12f, gray))
        c.addView(spacer(22))

        val hero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)); background = box(panel, 22, Color.rgb(35, 70, 35)) }
        hero.addView(tv("NOW PLAYING", 9f, neon, true).apply { letterSpacing = .18f })
        hero.addView(tv(current?.title ?: "SELECT A TRACK", 22f, white, true).apply { setPadding(0, dp(10), 0, dp(3)); maxLines = 1 })
        hero.addView(tv(current?.artist ?: "Choose music from Library", 12f, gray))
        val bar = SeekBar(this).apply { progress = 34; max = 100; progressTintList = android.content.res.ColorStateList.valueOf(neon); thumbTintList = android.content.res.ColorStateList.valueOf(neon) }
        hero.addView(bar, LinearLayout.LayoutParams(-1, dp(28)).apply { topMargin = dp(8) })
        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        controls.addView(tv("↶", 23f, gray).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(46), dp(46)))
        controls.addView(tv("▶", 24f, bg, true).apply { gravity = Gravity.CENTER; background = box(neon, 25); setOnClickListener { current?.let { play(it) } } }, LinearLayout.LayoutParams(dp(52), dp(52)))
        controls.addView(tv("↷", 23f, gray).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(46), dp(46)))
        controls.addView(Space(this), LinearLayout.LayoutParams(0, 1, 1f))
        controls.addView(tv("☷", 21f, neon).apply { gravity = Gravity.CENTER; setOnClickListener { showEqualizer() } }, LinearLayout.LayoutParams(dp(42), dp(46)))
        hero.addView(controls)
        c.addView(hero)
        c.addView(spacer(24))

        section(c, "NEON PICKS", "CURATED")
        val picks = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val list = if (tracks.isEmpty()) listOf(null, null, null) else tracks.take(3).map { it }
        list.forEachIndexed { i, t ->
            val card = LinearLayout(this@NeonActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(12), dp(12), dp(12)); background = box(if (i == 0) Color.rgb(18, 42, 20) else panel2, 18, if (i == 0) Color.rgb(60, 255, 40) else Color.TRANSPARENT); setOnClickListener { t?.let { play(it) } } }
            card.addView(tv(if (i == 0) "01" else "0${i + 1}", 10f, neon, true))
            card.addView(tv(t?.title ?: "NEON MIX", 14f, white, true).apply { setPadding(0, dp(16), 0, dp(4)); maxLines = 1 })
            card.addView(tv(t?.artist ?: "Spotifusion", 10f, gray).apply { maxLines = 1 })
            picks.addView(card, LinearLayout.LayoutParams(dp(145), dp(118)).apply { rightMargin = dp(10) })
        }
        val hs = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(picks) }
        c.addView(hs)
        c.addView(spacer(24))

        section(c, "YOUR LIBRARY", "${tracks.size} TRACKS")
        if (tracks.isEmpty()) c.addView(tv("No local songs found yet.\nOpen Settings to rescan your device.", 13f, gray).apply { setPadding(dp(4), dp(14), 0, 0) })
        else tracks.take(12).forEach { c.addView(trackRow(it)) }
        show(c)
    }

    private fun section(c: LinearLayout, left: String, right: String) {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        r.addView(tv(left, 12f, white, true).apply { letterSpacing = .12f }, LinearLayout.LayoutParams(0, dp(36), 1f))
        r.addView(tv(right, 8f, neon, true).apply { letterSpacing = .1f })
        c.addView(r)
    }

    private fun trackRow(t: Track) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(8), dp(8), dp(8)); background = box(panel, 15); setOnClickListener { play(t) }
        val num = tv(String.format("%02d", tracks.indexOf(t) + 1), 10f, neon, true).apply { gravity = Gravity.CENTER }
        addView(num, LinearLayout.LayoutParams(dp(30), dp(54)))
        val art = TextView(this@NeonActivity).apply { text = "♫"; textSize = 22f; setTextColor(neon); gravity = Gravity.CENTER; background = box(Color.rgb(16, 35, 18), 13) }
        addView(art, LinearLayout.LayoutParams(dp(54), dp(54)))
        val m = LinearLayout(this@NeonActivity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, dp(6), 0) }
        m.addView(tv(t.title, 13f, white, true).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END })
        m.addView(tv(t.artist, 10f, gray).apply { maxLines = 1; setPadding(0, dp(5), 0, 0) })
        addView(m, LinearLayout.LayoutParams(0, dp(62), 1f))
        addView(tv(if (favorites.contains(t.id)) "◆" else "◇", 15f, if (favorites.contains(t.id)) neon else dim).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(30), dp(54)))
    }

    private fun showSearch() {
        val c = pageRoot(); c.addView(header("SEARCH", "Find your next frequency")); c.addView(neonLine())
        val input = EditText(this).apply { hint = "Search songs, artists..."; hintTextColor = dim; setTextColor(white); textSize = 14f; singleLine = true; setPadding(dp(18), 0, dp(18), 0); background = box(panel2, 22, Color.rgb(35, 75, 35)) }
        c.addView(input, LinearLayout.LayoutParams(-1, dp(52)))
        c.addView(spacer(18)); section(c, "FILTERS", "LIVE")
        val chips = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("SONGS", "ALBUMS", "ARTISTS", "PLAYLISTS").forEachIndexed { i, s -> chips.addView(tv(s, 9f, if (i == 0) bg else white, true).apply { gravity = Gravity.CENTER; background = box(if (i == 0) neon else panel2, 18, if (i == 0) neon else Color.rgb(35, 45, 45)); setPadding(dp(14), dp(10), dp(14), dp(10)) }, LinearLayout.LayoutParams(-2, dp(38)).apply { rightMargin = dp(7) }) }
        c.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(chips) })
        c.addView(spacer(22)); section(c, "RESULTS", "${tracks.size} AVAILABLE")
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun refresh(q: String) { results.removeAllViews(); tracks.filter { it.title.contains(q, true) || it.artist.contains(q, true) }.take(80).forEach { results.addView(trackRow(it)) } }
        input.addTextChangedListener(object : android.text.TextWatcher { override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {} ; override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) { refresh(s?.toString().orEmpty()) }; override fun afterTextChanged(e: android.text.Editable?) {} })
        refresh(""); c.addView(results); show(c)
    }

    private fun showLibrary() {
        val c = pageRoot(); c.addView(header("LIBRARY", "Your music stays on your device")); c.addView(neonLine())
        val stat = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("TRACKS" to tracks.size.toString(), "FAVORITES" to favorites.size.toString(), "QUEUED" to "0").forEach { pair -> val x = LinearLayout(this@NeonActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(13), dp(14), dp(13)); background = box(panel2, 16, Color.rgb(28, 55, 30)); addView(tv(pair.second, 21f, neon, true)); addView(tv(pair.first, 8f, dim, true).apply { setPadding(0, dp(4), 0, 0) }) }; stat.addView(x, LinearLayout.LayoutParams(0, dp(76), 1f).apply { rightMargin = dp(7) }) }
        c.addView(stat); c.addView(spacer(20)); section(c, "ALL SONGS", "A-Z"); tracks.forEach { c.addView(trackRow(it)) }; if (tracks.isEmpty()) c.addView(tv("Your local music will appear here.", 13f, gray).apply { setPadding(dp(4), dp(15), 0, 0) }); show(c)
    }

    private fun showEqualizer() {
        val c = pageRoot(); c.addView(header("NEON EQ", "Shape the frequency")); c.addView(neonLine())
        c.addView(tv("PRESETS", 10f, dim, true)); c.addView(spacer(8))
        val presets = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("FLAT", "BASS", "VOCAL", "NIGHT").forEach { s -> presets.addView(tv(s, 9f, white, true).apply { gravity = Gravity.CENTER; background = box(panel2, 16, Color.rgb(30, 60, 35)); setPadding(dp(13), dp(10), dp(13), dp(10)) }, LinearLayout.LayoutParams(-2, dp(38)).apply { rightMargin = dp(7) }) }
        c.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(presets) }); c.addView(spacer(24))
        val bands = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM }
        listOf("60", "150", "400", "1K", "2.4K", "6K", "15K").forEachIndexed { i, label -> val col = LinearLayout(this@NeonActivity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; addView(tv("${i - 2}", 8f, dim).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(18))); val seek = SeekBar(this@NeonActivity).apply { max = 10; progress = 5; rotation = -90; progressTintList = android.content.res.ColorStateList.valueOf(neon); thumbTintList = android.content.res.ColorStateList.valueOf(neon) }; addView(seek, LinearLayout.LayoutParams(dp(42), dp(210))); addView(tv(label, 8f, gray, true).apply { gravity = Gravity.CENTER }) }; bands.addView(col, LinearLayout.LayoutParams(0, dp(250), 1f) }
        c.addView(bands); c.addView(spacer(18)); c.addView(tv("NEONIZER", 10f, dim, true)); val sw = Switch(this).apply { isChecked = true; buttonTintList = android.content.res.ColorStateList.valueOf(neon); setText("  Spatial glow") ; setTextColor(white) }; c.addView(sw); show(c)
    }

    private fun showSettings() {
        val c = pageRoot(); c.addView(header("SETTINGS", "Spotifusion control center")); c.addView(neonLine())
        setting(c, "MUSIC SCAN", "Refresh local music library") { loadMusic(); showLibrary(); toast("Library rescanned") }
        setting(c, "SHAKE TO CHANGE", "Change song when device is shaken") { toast("Shake control ready") }
        setting(c, "NEON THEME", "Always use the neon interface") { toast("Neon theme is active") }
        setting(c, "AUDIO OUTPUT", "Device speaker / connected output") { toast("Using device output") }
        setting(c, "ABOUT SPOTIFUSION", "Version 1.0 • Local-first music player") { toast("Spotifusion") }
        show(c)
    }

    private fun setting(c: LinearLayout, title: String, sub: String, action: () -> Unit) { val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(15), dp(13), dp(12), dp(13)); background = box(panel, 17, Color.rgb(25, 45, 27)); setOnClickListener { action() } }; val m = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; m.addView(tv(title, 12f, white, true)); m.addView(tv(sub, 9f, gray).apply { setPadding(0, dp(5), 0, 0) }); r.addView(m, LinearLayout.LayoutParams(0, dp(66), 1f)); r.addView(tv("›", 25f, neon).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(30), dp(50))); c.addView(r); c.addView(spacer(9)) }

    private fun miniPlayer() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(8), dp(10), dp(8)); background = box(Color.rgb(10, 17, 12), 18, Color.rgb(40, 100, 45)); visibility = if (current == null) View.GONE else View.VISIBLE
        addView(tv("♫", 21f, neon).apply { gravity = Gravity.CENTER; background = box(Color.rgb(17, 35, 18), 12) }, LinearLayout.LayoutParams(dp(50), dp(50)))
        val m = LinearLayout(this@NeonActivity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, dp(6), 0) }; m.addView(tv(current?.title ?: "", 12f, white, true).apply { maxLines = 1 }); m.addView(tv(current?.artist ?: "", 9f, gray).apply { setPadding(0, dp(4), 0, 0) }); addView(m, LinearLayout.LayoutParams(0, -1, 1f)); addView(tv("▶", 18f, neon).apply { gravity = Gravity.CENTER; setOnClickListener { current?.let { play(it) } } }, LinearLayout.LayoutParams(dp(45), -1))
    }

    private fun play(t: Track) {
        try { player?.release(); player = MediaPlayer.create(this, t.uri); current = t; currentIndex = tracks.indexOf(t); player?.start(); mini?.visibility = View.VISIBLE; refreshMini(); toast("Playing ${t.title}") } catch (_: Exception) { toast("Unable to play this track") }
    }

    private fun refreshMini() { mini?.removeAllViews(); mini?.addView(tv("♫", 21f, neon).apply { gravity = Gravity.CENTER; background = box(Color.rgb(17, 35, 18), 12) }, LinearLayout.LayoutParams(dp(50), dp(50))); val m = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), 0, dp(6), 0) }; m.addView(tv(current?.title ?: "", 12f, white, true).apply { maxLines = 1 }); m.addView(tv(current?.artist ?: "", 9f, gray)); mini?.addView(m, LinearLayout.LayoutParams(0, -1, 1f)); mini?.addView(tv("▶", 18f, neon).apply { gravity = Gravity.CENTER; setOnClickListener { current?.let { play(it) } } }, LinearLayout.LayoutParams(dp(45), -1)) }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    override fun onDestroy() { player?.release(); player = null; super.onDestroy() }
}
