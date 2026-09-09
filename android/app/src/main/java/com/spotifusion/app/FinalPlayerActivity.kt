package com.spotifusion.app

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class FinalPlayerActivity : AppCompatActivity() {
    private val bg = Color.rgb(5, 5, 9)
    private val card = Color.rgb(14, 14, 21)
    private val card2 = Color.rgb(22, 21, 31)
    private val border = Color.rgb(43, 40, 58)
    private val white = Color.rgb(247, 245, 252)
    private val muted = Color.rgb(145, 140, 160)
    private val violet = Color.rgb(123, 81, 251)
    private val violetSoft = Color.rgb(69, 48, 139)

    private lateinit var page: FrameLayout
    private lateinit var miniTitle: TextView
    private lateinit var miniArtist: TextView
    private lateinit var miniPlay: TextView
    private lateinit var bottomBar: LinearLayout
    private var navItems = emptyList<LinearLayout>()
    private var activeTab = 0
    private var tracks: List<LocalTrack> = emptyList()
    private val ui = Handler(Looper.getMainLooper())
    private val store by lazy { UserMusicStore(this) }
    private var timerRunnable: Runnable? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun text(value: String, size: Float, color: Int = white, bold: Boolean = false): TextView =
        TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun rounded(color: Int = card, radius: Int = 16, outlined: Boolean = false) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (outlined) setStroke(dp(1), border)
        }

    private fun gradient(radius: Int = 18) =
        android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(47, 29, 99), violet)
        ).apply { cornerRadius = dp(radius).toFloat() }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        requestPermissionsIfNeeded()
        PlaybackController.connect(this)
        tracks = scanTracks()
        buildShell()
        showHome()
    }

    private fun scanTracks(): List<LocalTrack> =
        runCatching { LocalMusicScanner.scan(this) }.getOrDefault(emptyList())

    private fun requestPermissionsIfNeeded() {
        val audioPermission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, audioPermission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(audioPermission), 40)
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 41)
        }
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }
        root.addView(header(), LinearLayout.LayoutParams(-1, dp(62)))
        page = FrameLayout(this)
        root.addView(page, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(buildMiniPlayer(), LinearLayout.LayoutParams(-1, dp(70)).apply { setMargins(dp(8), dp(4), dp(8), dp(4)) })
        bottomBar = buildBottomNav()
        root.addView(bottomBar, LinearLayout.LayoutParams(-1, dp(62)).apply { setMargins(dp(8), 0, dp(8), dp(6)) })
        setContentView(root)
    }

    private fun header(): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(7), dp(14), dp(4))
        addView(text("S", 18f, white, true).apply {
            gravity = Gravity.CENTER
            background = gradient(11)
        }, LinearLayout.LayoutParams(dp(36), dp(36)))
        addView(text("Spotifusion", 18f, white, true).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -1, 1f))
        addView(text("⌕", 25f, white, true).apply {
            gravity = Gravity.CENTER
            background = rounded(card2, 14, true)
            setOnClickListener { showSearch() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = dp(7) })
        addView(text("⋮", 24f, white, true).apply {
            gravity = Gravity.CENTER
            background = rounded(card2, 14, true)
            setOnClickListener { showSettings() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
    }

    private fun buildBottomNav(): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.CENTER
        background = rounded(card, 21, true)
        setPadding(dp(3), dp(3), dp(3), dp(3))
        val names = listOf("Home", "Search", "Library", "EQ")
        val icons = listOf("⌂", "⌕", "▣", "≋")
        val items = mutableListOf<LinearLayout>()
        names.forEachIndexed { index, name ->
            val item = LinearLayout(this@FinalPlayerActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setOnClickListener {
                    when (index) {
                        0 -> showHome()
                        1 -> showSearch()
                        2 -> showLibrary()
                        3 -> showEq()
                    }
                }
            }
            item.addView(text(icons[index], 18f, if (index == activeTab) violet else muted, true).apply { gravity = Gravity.CENTER })
            item.addView(text(name, 9f, if (index == activeTab) violet else muted, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(3), 0, 0)
            })
            addView(item, LinearLayout.LayoutParams(0, -1, 1f))
            items += item
        }
        navItems = items
    }

    private fun setActiveTab(index: Int) {
        activeTab = index
        navItems.forEachIndexed { i, item ->
            val color = if (i == index) violet else muted
            (item.getChildAt(0) as TextView).setTextColor(color)
            (item.getChildAt(1) as TextView).setTextColor(color)
        }
    }

    private fun buildMiniPlayer(): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        background = rounded(card, 17, true)
        setPadding(dp(7), dp(6), dp(7), dp(6))
        val artwork = currentArtwork(48)
        addView(artwork, LinearLayout.LayoutParams(dp(48), dp(48)))
        val info = LinearLayout(this@FinalPlayerActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(5), 0)
            setOnClickListener { showPlayer() }
        }
        miniTitle = text(PlaybackController.currentTitle().ifBlank { "Nothing playing" }, 12.5f, white, true)
        miniArtist = text(PlaybackController.currentArtist().ifBlank { "Choose a song" }, 10f, muted)
        info.addView(miniTitle)
        info.addView(miniArtist.apply { setPadding(0, dp(4), 0, 0) })
        addView(info, LinearLayout.LayoutParams(0, -1, 1f))
        miniPlay = text(if (PlaybackController.isPlaying()) "Ⅱ" else "▶", 18f, white, true).apply {
            gravity = Gravity.CENTER
            background = rounded(card2, 14, true)
            setOnClickListener { PlaybackController.toggle(); refreshMini() }
        }
        addView(miniPlay, LinearLayout.LayoutParams(dp(48), dp(48)))
    }

    private fun refreshMini() {
        if (!::miniTitle.isInitialized) return
        miniTitle.text = PlaybackController.currentTitle().ifBlank { "Nothing playing" }
        miniArtist.text = PlaybackController.currentArtist().ifBlank { "Choose a song" }
        miniPlay.text = if (PlaybackController.isPlaying()) "Ⅱ" else "▶"
    }

    private fun currentArtwork(size: Int): View {
        val currentId = PlaybackController.currentMediaId()?.toLongOrNull()
        val track = currentId?.let { id -> tracks.firstOrNull { it.id == id } }
        return ArtworkViewFactory.create(this, track, dp(size))
    }

    private fun showPage(content: LinearLayout) {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        scroll.addView(content)
        page.removeAllViews()
        page.addView(scroll, FrameLayout.LayoutParams(-1, -1))
    }

    private fun column(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(8), dp(14), dp(30))
    }

    private fun section(parent: LinearLayout, title: String, action: String? = null, click: (() -> Unit)? = null) {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(text(title, 14f, white, true), LinearLayout.LayoutParams(0, dp(38), 1f))
        if (action != null) row.addView(text(action, 10f, violet, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { click?.invoke() }
        })
        parent.addView(row)
    }

    private fun showHome() {
        setActiveTab(0)
        tracks = scanTracks().ifEmpty { tracks }
        val content = column()
        content.addView(text("Good evening", 11f, muted))
        content.addView(text("Your sound. Your space.", 25f, white, true).apply { setPadding(0, dp(5), 0, dp(14)) })

        val first = tracks.firstOrNull()
        val hero = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = gradient(22)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        hero.addView(ArtworkViewFactory.create(this, first, dp(108)), LinearLayout.LayoutParams(dp(108), dp(108)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(13), 0, 0, 0) }
        info.addView(text("YOUR LIBRARY", 9f, Color.rgb(220, 211, 255), true))
        info.addView(text(first?.title ?: "Add your music", 17f, white, true).apply { setPadding(0, dp(7), 0, dp(4)); maxLines = 1 })
        info.addView(text(first?.artist ?: "Local music on this device", 10f, Color.rgb(220, 211, 255)).apply { setPadding(0, 0, 0, dp(10)); maxLines = 1 })
        info.addView(text("PLAY NOW", 10f, violet, true).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.WHITE, 14)
            setPadding(dp(4), dp(8), dp(4), dp(8))
            setOnClickListener { if (tracks.isNotEmpty()) play(0) }
        })
        hero.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(hero, LinearLayout.LayoutParams(-1, dp(132)))

        section(content, "Recently played", "SEE ALL") { showLibrary() }
        val recent = store.recentlyPlayed().ifEmpty { tracks }
        recent.take(8).forEach { addTrackRow(content, it) }

        section(content, "Quick actions")
        val quick = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        action(quick, "♥", "Liked") { showFavorites() }
        action(quick, "▤", "Playlists") { startActivity(android.content.Intent(this@FinalPlayerActivity, PlaylistActivity::class.java)) }
        action(quick, "◷", "Timer") { timerDialog() }
        content.addView(quick, LinearLayout.LayoutParams(-1, dp(82)))
        showPage(content)
    }

    private fun action(parent: LinearLayout, icon: String, title: String, click: () -> Unit) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(card, 15, true)
            setOnClickListener { click() }
        }
        item.addView(text(icon, 21f, violet, true))
        item.addView(text(title, 9f, muted, true).apply { setPadding(0, dp(4), 0, 0) })
        parent.addView(item, LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
    }

    private fun addTrackRow(parent: LinearLayout, track: LocalTrack) {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(card, 14, true)
            setPadding(dp(7), dp(6), dp(7), dp(6))
            setOnClickListener {
                val index = tracks.indexOfFirst { it.id == track.id }
                if (index >= 0) play(index) else playTrackDirect(track)
            }
            setOnLongClickListener { playlistPicker(track); true }
        }
        row.addView(ArtworkViewFactory.create(this, track, dp(46)), LinearLayout.LayoutParams(dp(46), dp(46)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, dp(5), 0) }
        info.addView(text(track.title, 12f, white, true).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END })
        info.addView(text(track.artist.ifBlank { "Unknown artist" }, 9.5f, muted).apply { setPadding(0, dp(4), 0, 0); maxLines = 1 })
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(text(if (store.isFavorite(track.id)) "♥" else "♡", 19f, if (store.isFavorite(track.id)) violet else muted).apply {
            gravity = Gravity.CENTER
            setPadding(dp(7), 0, dp(4), 0)
            setOnClickListener { store.toggleFavorite(track.id); showCurrentPageRefresh() }
        })
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(62)).apply { bottomMargin = dp(6) })
    }

    private fun showCurrentPageRefresh() {
        when (activeTab) {
            0 -> showHome()
            2 -> showLibrary()
            else -> showFavorites()
        }
    }

    private fun play(index: Int) {
        if (tracks.isEmpty()) return
        val safe = index.coerceIn(0, tracks.lastIndex)
        PlaybackController.playTracks(tracks, safe)
        store.addRecentlyPlayed(tracks[safe])
        refreshMini()
        showPlayer()
    }

    private fun playTrackDirect(track: LocalTrack) {
        PlaybackController.play(track.uri.toString(), track.title, track.artist, track.album, track.id.toString(), ArtworkResolver.uri(track.albumId).toString())
        store.addRecentlyPlayed(track)
        refreshMini()
        showPlayer()
    }

    private fun showLibrary() {
        setActiveTab(2)
        tracks = scanTracks().ifEmpty { tracks }
        val content = column()
        content.addView(text("Library", 26f, white, true))
        content.addView(text("${tracks.size} tracks on this device", 11f, muted).apply { setPadding(0, dp(5), 0, dp(12)) })
        val refresh = text("REFRESH MUSIC", 10f, violet, true).apply {
            gravity = Gravity.CENTER
            background = rounded(card2, 14, true)
            setPadding(dp(10), dp(10), dp(10), dp(10))
            setOnClickListener { tracks = scanTracks(); showLibrary() }
        }
        content.addView(refresh, LinearLayout.LayoutParams(-1, dp(42)).apply { bottomMargin = dp(10) })
        tracks.forEach { addTrackRow(content, it) }
        if (tracks.isEmpty()) emptyState(content, "No music found", "Allow music access and add audio files to your device.")
        showPage(content)
    }

    private fun showFavorites() {
        val content = column()
        content.addView(text("Liked songs", 26f, white, true))
        content.addView(text("Your saved tracks", 11f, muted).apply { setPadding(0, dp(5), 0, dp(12)) })
        val liked = store.favoriteIds().mapNotNull { id -> tracks.firstOrNull { it.id == id } }
        liked.forEach { addTrackRow(content, it) }
        if (liked.isEmpty()) emptyState(content, "No liked songs yet", "Tap the heart beside a song to save it here.")
        showPage(content)
    }

    private fun emptyState(parent: LinearLayout, title: String, subtitle: String) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = rounded(card, 18, true)
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }
        box.addView(text("♪", 36f, violet, true).apply { gravity = Gravity.CENTER })
        box.addView(text(title, 15f, white, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(5)) })
        box.addView(text(subtitle, 10f, muted).apply { gravity = Gravity.CENTER })
        parent.addView(box, LinearLayout.LayoutParams(-1, dp(150)).apply { topMargin = dp(8) })
    }

    private fun showSearch() {
        setActiveTab(1)
        val content = column()
        content.addView(text("Search", 26f, white, true).apply { setPadding(0, dp(5), 0, dp(12)) })
        val input = EditText(this).apply {
            hint = "Search songs, artists, albums"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 14f
            isSingleLine = true
            background = rounded(card2, 16, true)
            setPadding(dp(14), 0, dp(14), 0)
        }
        content.addView(input, LinearLayout.LayoutParams(-1, dp(52)))
        val chips = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        listOf("Songs", "Artists", "Albums").forEachIndexed { i, value ->
            chips.addView(text(value, 10f, if (i == 0) white else muted, true).apply {
                gravity = Gravity.CENTER
                background = rounded(if (i == 0) violetSoft else card, 14, true)
                setPadding(dp(12), dp(8), dp(12), dp(8))
            }, LinearLayout.LayoutParams(0, dp(38), 1f).apply { setMargins(dp(2), dp(8), dp(2), dp(8)) })
        }
        content.addView(chips)
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(results)
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                results.removeAllViews()
                val query = s?.toString()?.trim().orEmpty()
                if (query.isBlank()) {
                    results.addView(text("Start typing to search your music", 11f, muted).apply { setPadding(dp(4), dp(24), 0, 0) })
                    return
                }
                tracks.filter { it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true) }
                    .forEach { addTrackRow(results, it) }
                if (results.childCount == 0) emptyState(results, "No results", "Try another song, artist, or album.")
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        showPage(content)
        input.requestFocus()
    }

    private fun showPlayer() {
        val content = column()
        content.gravity = Gravity.CENTER_HORIZONTAL
        val title = PlaybackController.currentTitle().ifBlank { "Nothing playing" }
        val artist = PlaybackController.currentArtist().ifBlank { "Select a song" }
        content.addView(text("NOW PLAYING", 10f, violet, true).apply { gravity = Gravity.CENTER })
        content.addView(currentArtwork(264), LinearLayout.LayoutParams(dp(264), dp(264)).apply { topMargin = dp(10) })
        content.addView(text(title, 22f, white, true).apply {
            gravity = Gravity.CENTER
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            setPadding(0, dp(17), 0, dp(4))
        })
        content.addView(text(artist, 12f, muted).apply { gravity = Gravity.CENTER })

        val seek = SeekBar(this).apply {
            max = 1000
            progressTintList = android.content.res.ColorStateList.valueOf(violet)
            thumbTintList = android.content.res.ColorStateList.valueOf(white)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) PlaybackController.seek((PlaybackController.duration() * progress / 1000f).toLong())
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }
        content.addView(seek, LinearLayout.LayoutParams(-1, dp(42)).apply { topMargin = dp(4) })
        val times = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val elapsed = text(formatTime(PlaybackController.position()), 9f, muted)
        val total = text(formatTime(PlaybackController.duration()), 9f, muted)
        times.addView(elapsed, LinearLayout.LayoutParams(0, dp(18), 1f))
        times.addView(total.apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, dp(18), 1f))
        content.addView(times)

        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER; setPadding(0, dp(2), 0, dp(2)) }
        controls.addView(text("↶", 25f, white, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { PlaybackController.previous(); refreshMini(); showPlayer() }
        }, LinearLayout.LayoutParams(dp(66), dp(64)))
        controls.addView(text(if (PlaybackController.isPlaying()) "Ⅱ" else "▶", 30f, white, true).apply {
            gravity = Gravity.CENTER
            background = gradient(60)
            elevation = dp(3).toFloat()
            setOnClickListener { PlaybackController.toggle(); refreshMini(); showPlayer() }
        }, LinearLayout.LayoutParams(dp(72), dp(72)))
        controls.addView(text("↷", 25f, white, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { PlaybackController.next(); refreshMini(); showPlayer() }
        }, LinearLayout.LayoutParams(dp(66), dp(64)))
        content.addView(controls)

        val options = LinearLayout(this).apply { gravity = Gravity.CENTER; background = rounded(card, 16, true); setPadding(dp(4), 0, dp(4), 0) }
        options.addView(text(if (PlaybackController.isShuffleEnabled()) "SHUFFLE ON" else "SHUFFLE", 9.5f, if (PlaybackController.isShuffleEnabled()) violet else muted, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { PlaybackController.setShuffle(!PlaybackController.isShuffleEnabled()); showPlayer() }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        options.addView(text("LYRICS", 9.5f, muted, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { startActivity(android.content.Intent(this@FinalPlayerActivity, LyricsActivity::class.java)) }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        options.addView(text(if (PlaybackController.isRepeatEnabled()) "REPEAT ONE" else "REPEAT", 9.5f, if (PlaybackController.isRepeatEnabled()) violet else muted, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { PlaybackController.setRepeat(!PlaybackController.isRepeatEnabled()); showPlayer() }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        content.addView(options, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(10) })

        val lower = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        lower.addView(text("SLEEP TIMER", 9.5f, muted, true).apply {
            gravity = Gravity.CENTER
            background = rounded(card2, 14, true)
            setPadding(dp(16), dp(11), dp(16), dp(11))
            setOnClickListener { timerDialog() }
        }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { rightMargin = dp(4) })
        lower.addView(text("VOLUME", 9.5f, muted, true).apply {
            gravity = Gravity.CENTER
            background = rounded(card2, 14, true)
            setPadding(dp(16), dp(11), dp(16), dp(11))
            setOnClickListener { volumeDialog() }
        }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(4) })
        content.addView(lower, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(6) })
        showPage(content)
        updateSeekbar(seek, elapsed, total)
    }

    private fun updateSeekbar(seek: SeekBar, elapsed: TextView, total: TextView) {
        val duration = PlaybackController.duration()
        seek.progress = if (duration > 0) (PlaybackController.position() * 1000L / duration).toInt().coerceIn(0, 1000) else 0
        elapsed.text = formatTime(PlaybackController.position())
        total.text = formatTime(duration)
        ui.postDelayed({ if (seek.parent != null) updateSeekbar(seek, elapsed, total) }, 500L)
    }

    private fun formatTime(ms: Long): String {
        val safe = ms.coerceAtLeast(0L) / 1000L
        return "%d:%02d".format(safe / 60L, safe % 60L)
    }

    private fun volumeDialog() {
        val seek = SeekBar(this).apply {
            max = 100
            progress = (PlaybackController.volume() * 100f).toInt()
        }
        val box = LinearLayout(this).apply { setPadding(dp(22), dp(4), dp(22), 0); addView(seek, LinearLayout.LayoutParams(-1, dp(48))) }
        AlertDialog.Builder(this).setTitle("Volume").setView(box).setPositiveButton("DONE", null).show()
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) PlaybackController.setVolume(progress / 100f) }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })
    }

    private fun showEq() {
        setActiveTab(3)
        val content = column()
        content.addView(text("Equalizer", 26f, white, true))
        content.addView(text("5-band audio controls", 11f, muted).apply { setPadding(0, dp(5), 0, dp(14)) })
        AudioEffects.attach(PlaybackController.audioSessionId())
        val range = AudioEffects.range()
        val bands = AudioEffects.bands().toInt().coerceAtLeast(5)
        for (i in 0 until minOf(bands, 5)) {
            val index = i.toShort()
            val bandBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; background = rounded(card, 15, true); setPadding(dp(12), dp(8), dp(12), dp(5)) }
            val hz = when (i) { 0 -> "60 Hz"; 1 -> "230 Hz"; 2 -> "910 Hz"; 3 -> "3.6 kHz"; else -> "14 kHz" }
            bandBox.addView(text(hz, 10f, muted, true))
            bandBox.addView(SeekBar(this).apply {
                max = (range[1] - range[0]).toInt().coerceAtLeast(1)
                progress = (AudioEffects.level(index) - range[0]).toInt().coerceIn(0, max)
                progressTintList = android.content.res.ColorStateList.valueOf(violet)
                thumbTintList = android.content.res.ColorStateList.valueOf(white)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) AudioEffects.setBand(index, (progress + range[0]).toShort()) }
                    override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(bar: SeekBar?) = Unit
                })
            }, LinearLayout.LayoutParams(-1, dp(44)))
            content.addView(bandBox, LinearLayout.LayoutParams(-1, dp(82)).apply { bottomMargin = dp(8) })
        }
        val reset = text("RESET EQUALIZER", 10f, violet, true).apply {
            gravity = Gravity.CENTER
            background = rounded(card2, 14, true)
            setPadding(0, dp(12), 0, dp(12))
            setOnClickListener {
                for (i in 0 until minOf(AudioEffects.bands().toInt(), 5)) AudioEffects.setBand(i.toShort(), 0)
                showEq()
            }
        }
        content.addView(reset, LinearLayout.LayoutParams(-1, dp(44)))
        showPage(content)
    }

    private fun showSettings() {
        val content = column()
        content.addView(text("Settings", 26f, white, true))
        content.addView(text("Playback and library", 11f, muted).apply { setPadding(0, dp(5), 0, dp(14)) })
        setting(content, "Background playback", "Continue listening while the app is minimized") { toast("Background playback is enabled") }
        setting(content, "Notifications", "Android media controls are enabled") { toast("Media notification controls are provided by Media3") }
        setting(content, "Refresh music", "Scan your device for newly added audio") { tracks = scanTracks(); showLibrary() }
        setting(content, "Playlists", "Create playlists and add songs") { startActivity(android.content.Intent(this, PlaylistActivity::class.java)) }
        setting(content, "Lyrics", "Open synced lyrics for the current song") { startActivity(android.content.Intent(this, LyricsActivity::class.java)) }
        setting(content, "About Spotifusion", "Local-first music player") { aboutDialog() }
        showPage(content)
    }

    private fun setting(parent: LinearLayout, title: String, subtitle: String, click: () -> Unit) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(card, 15, true)
            setPadding(dp(14), dp(13), dp(14), dp(13))
            setOnClickListener { click() }
        }
        item.addView(text(title, 13f, white, true))
        item.addView(text(subtitle, 10f, muted).apply { setPadding(0, dp(5), 0, 0) })
        parent.addView(item, LinearLayout.LayoutParams(-1, dp(74)).apply { bottomMargin = dp(8) })
    }

    private fun playlistPicker(track: LocalTrack) {
        val playlists = PlaylistStore(this).all()
        if (playlists.isEmpty()) {
            toast("Create a playlist first")
            startActivity(android.content.Intent(this, PlaylistActivity::class.java))
            return
        }
        val store = PlaylistStore(this)
        AlertDialog.Builder(this)
            .setTitle("Add to playlist")
            .setItems(playlists.map { it.name }.toTypedArray()) { _, which ->
                store.addTrack(playlists[which].id, track.id)
                toast("Added to ${playlists[which].name}")
            }.show()
    }

    private fun timerDialog() {
        val options = arrayOf("Off", "15 minutes", "30 minutes", "45 minutes", "60 minutes")
        AlertDialog.Builder(this).setTitle("Sleep timer").setItems(options) { _, which ->
            timerRunnable?.let(ui::removeCallbacks)
            timerRunnable = null
            if (which == 0) {
                toast("Sleep timer off")
            } else {
                val minutes = intArrayOf(0, 15, 30, 45, 60)[which]
                timerRunnable = Runnable { PlaybackController.pause(); refreshMini(); toast("Sleep timer finished") }
                ui.postDelayed(timerRunnable!!, minutes * 60_000L)
                toast("Timer set for ${options[which]}")
            }
        }.show()
    }

    private fun aboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Spotifusion")
            .setMessage("A local-first music player with background playback, synced lyrics, playlists, favorites, equalizer controls and sleep timer.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onResume() {
        super.onResume()
        refreshMini()
    }

    override fun onDestroy() {
        timerRunnable?.let(ui::removeCallbacks)
        ui.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
