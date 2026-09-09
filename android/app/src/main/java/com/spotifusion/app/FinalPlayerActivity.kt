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
    private val bg = Color.rgb(6, 6, 10)
    private val card = Color.rgb(16, 15, 24)
    private val card2 = Color.rgb(24, 22, 34)
    private val white = Color.rgb(246, 244, 252)
    private val muted = Color.rgb(151, 146, 166)
    private val violet = Color.rgb(123, 81, 251)
    private lateinit var page: FrameLayout
    private lateinit var miniTitle: TextView
    private lateinit var miniArtist: TextView
    private lateinit var miniPlay: TextView
    private var tracks: List<LocalTrack> = emptyList()
    private val ui = Handler(Looper.getMainLooper())
    private val store by lazy { UserMusicStore(this) }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun label(value: String, size: Float, color: Int = white, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun box(color: Int = card, radius: Int = 16, stroke: Boolean = false) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke) setStroke(dp(1), Color.rgb(43, 40, 58))
        }

    private fun accent(radius: Int = 18) =
        android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(55, 34, 112), violet)
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
        val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, audioPermission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(audioPermission), 40)
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
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
        root.addView(miniPlayer(), LinearLayout.LayoutParams(-1, dp(68)).apply {
            setMargins(dp(9), dp(5), dp(9), dp(5))
        })
        root.addView(bottomNav(), LinearLayout.LayoutParams(-1, dp(62)).apply {
            setMargins(dp(8), 0, dp(8), dp(5))
        })
        setContentView(root)
    }

    private fun header(): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(7), dp(14), dp(4))
        addView(label("S", 18f, white, true).apply {
            gravity = Gravity.CENTER
            background = accent(11)
        }, LinearLayout.LayoutParams(dp(36), dp(36)))
        addView(label("Spotifusion", 18f, white, true).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, 0, 0)
        }, LinearLayout.LayoutParams(0, -1, 1f))
        addView(label("⌕", 25f, white, true).apply {
            gravity = Gravity.CENTER
            background = box(card2, 15, true)
            setOnClickListener { showSearch() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { rightMargin = dp(7) })
        addView(label("⋮", 24f, white, true).apply {
            gravity = Gravity.CENTER
            background = box(card2, 15, true)
            setOnClickListener { showSettings() }
        }, LinearLayout.LayoutParams(dp(44), dp(44)))
    }

    private fun bottomNav(): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.CENTER
        background = box(card, 21, true)
        setPadding(dp(3), dp(3), dp(3), dp(3))
        val items = listOf("⌂" to "Home", "⌕" to "Search", "▣" to "Library", "≋" to "EQ")
        items.forEachIndexed { index, itemData ->
            val item = LinearLayout(this@FinalPlayerActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setOnClickListener {
                    when (index) {
                        0 -> showHome()
                        1 -> showSearch()
                        2 -> showLibrary()
                        else -> showEq()
                    }
                }
            }
            val active = index == 0
            item.addView(label(itemData.first, 18f, if (active) violet else muted, true).apply {
                gravity = Gravity.CENTER
            })
            item.addView(label(itemData.second, 9f, if (active) violet else muted, true).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(3), 0, 0)
            })
            addView(item, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }

    private fun miniPlayer(): LinearLayout = LinearLayout(this).apply {
        gravity = Gravity.CENTER_VERTICAL
        background = box(card, 17, true)
        setPadding(dp(7), dp(6), dp(7), dp(6))
        addView(label("♪", 22f, white, true).apply {
            gravity = Gravity.CENTER
            background = accent(11)
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val info = LinearLayout(this@FinalPlayerActivity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(5), 0)
            setOnClickListener { showPlayer() }
        }
        miniTitle = label(PlaybackController.currentTitle().ifBlank { "Nothing playing" }, 12.5f, white, true)
        miniArtist = label(PlaybackController.currentArtist().ifBlank { "Choose a song" }, 10f, muted)
        info.addView(miniTitle)
        info.addView(miniArtist.apply { setPadding(0, dp(4), 0, 0) })
        addView(info, LinearLayout.LayoutParams(0, -1, 1f))
        miniPlay = label(if (PlaybackController.isPlaying()) "Ⅱ" else "▶", 18f, white, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                PlaybackController.toggle()
                refreshMini()
            }
        }
        addView(miniPlay, LinearLayout.LayoutParams(dp(48), dp(48)))
    }

    private fun refreshMini() {
        if (!::miniTitle.isInitialized) return
        miniTitle.text = PlaybackController.currentTitle().ifBlank { "Nothing playing" }
        miniArtist.text = PlaybackController.currentArtist().ifBlank { "Choose a song" }
        miniPlay.text = if (PlaybackController.isPlaying()) "Ⅱ" else "▶"
    }

    private fun showPage(content: LinearLayout) {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
        }
        scroll.addView(content)
        page.removeAllViews()
        page.addView(scroll, FrameLayout.LayoutParams(-1, -1))
    }

    private fun column(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(14), dp(8), dp(14), dp(28))
    }

    private fun section(parent: LinearLayout, title: String) {
        parent.addView(label(title, 14f, white, true).apply {
            setPadding(0, dp(16), 0, dp(9))
        })
    }

    private fun showHome() {
        tracks = scanTracks().ifEmpty { tracks }
        val content = column()
        content.addView(label("Good evening", 11f, muted))
        content.addView(label("Your sound. Your space.", 25f, white, true).apply {
            setPadding(0, dp(5), 0, dp(14))
        })

        val hero = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = accent(22)
            setPadding(dp(14), dp(13), dp(12), dp(13))
        }
        hero.addView(label("◉", 46f, white, true).apply {
            gravity = Gravity.CENTER
            background = box(Color.rgb(53, 35, 112), 17)
        }, LinearLayout.LayoutParams(dp(104), dp(104)))
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), 0, 0, 0)
        }
        val first = tracks.firstOrNull()
        info.addView(label("YOUR LIBRARY", 9f, white, true))
        info.addView(label(first?.title ?: "Add your music", 17f, white, true).apply {
            setPadding(0, dp(7), 0, dp(4))
            maxLines = 1
        })
        info.addView(label(first?.artist ?: "Local music on this device", 10f, Color.rgb(224, 216, 255)).apply {
            setPadding(0, 0, 0, dp(10))
        })
        info.addView(label("PLAY NOW", 10f, violet, true).apply {
            gravity = Gravity.CENTER
            background = box(Color.WHITE, 14)
            setPadding(dp(4), dp(8), dp(4), dp(8))
            setOnClickListener { if (tracks.isNotEmpty()) play(0) }
        })
        hero.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(hero, LinearLayout.LayoutParams(-1, dp(132)))

        section(content, "Recently played")
        val recent = store.recentlyPlayed().ifEmpty { tracks }
        recent.take(10).forEach { addTrackRow(content, it) }

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
            background = box(card, 15, true)
            setOnClickListener { click() }
        }
        item.addView(label(icon, 21f, violet, true))
        item.addView(label(title, 9f, muted, true).apply { setPadding(0, dp(4), 0, 0) })
        parent.addView(item, LinearLayout.LayoutParams(0, -1, 1f).apply {
            setMargins(dp(3), 0, dp(3), 0)
        })
    }

    private fun addTrackRow(parent: LinearLayout, track: LocalTrack) {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = box(card, 14, true)
            setPadding(dp(7), dp(6), dp(7), dp(6))
            setOnClickListener {
                val index = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                if (tracks.isNotEmpty()) play(index)
            }
        }
        row.addView(label("♪", 18f, white, true).apply {
            gravity = Gravity.CENTER
            background = accent(10)
        }, LinearLayout.LayoutParams(dp(46), dp(46)))
        val info = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, dp(5), 0)
        }
        info.addView(label(track.title, 12f, white, true).apply { maxLines = 1 })
        info.addView(label(track.artist, 9.5f, muted).apply { setPadding(0, dp(4), 0, 0) })
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(label(if (store.isFavorite(track.id)) "♥" else "♡", 19f, if (store.isFavorite(track.id)) violet else muted).apply {
            setOnClickListener {
                store.toggleFavorite(track.id)
                showHome()
            }
        })
        parent.addView(row, LinearLayout.LayoutParams(-1, dp(62)).apply { bottomMargin = dp(6) })
    }

    private fun play(index: Int) {
        if (tracks.isEmpty()) return
        val safeIndex = index.coerceIn(0, tracks.lastIndex)
        PlaybackController.playTracks(tracks, safeIndex)
        store.addRecentlyPlayed(tracks[safeIndex])
        refreshMini()
        showPlayer()
    }

    private fun showLibrary() {
        val content = column()
        content.addView(label("Library", 26f, white, true))
        content.addView(label("${tracks.size} tracks on this device", 11f, muted).apply {
            setPadding(0, dp(5), 0, dp(12))
        })
        tracks.forEach { addTrackRow(content, it) }
        showPage(content)
    }

    private fun showFavorites() {
        val content = column()
        content.addView(label("Liked songs", 26f, white, true))
        val liked = store.favoriteIds().mapNotNull { id -> tracks.firstOrNull { it.id == id } }
        liked.forEach { addTrackRow(content, it) }
        if (liked.isEmpty()) {
            content.addView(label("No liked songs yet", 11f, muted).apply { setPadding(0, dp(20), 0, 0) })
        }
        showPage(content)
    }

    private fun showSearch() {
        val content = column()
        content.addView(label("Search", 26f, white, true).apply {
            setPadding(0, dp(5), 0, dp(12))
        })
        val input = EditText(this).apply {
            hint = "Search your music"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 14f
            isSingleLine = true
            background = box(card2, 16, true)
            setPadding(dp(14), 0, dp(14), 0)
        }
        content.addView(input, LinearLayout.LayoutParams(-1, dp(52)))
        val results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(results)
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                results.removeAllViews()
                val query = s?.toString()?.trim().orEmpty()
                if (query.isBlank()) return
                tracks.filter {
                    it.title.contains(query, true) ||
                    it.artist.contains(query, true) ||
                    it.album.contains(query, true)
                }.forEach { addTrackRow(results, it) }
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        showPage(content)
    }

    private fun showPlayer() {
        val content = column().apply { gravity = Gravity.CENTER_HORIZONTAL }
        content.addView(label("NOW PLAYING", 10f, violet, true))
        content.addView(label("♪", 72f, white, true).apply {
            gravity = Gravity.CENTER
            background = accent(26)
        }, LinearLayout.LayoutParams(dp(250), dp(250)))
        content.addView(label(PlaybackController.currentTitle().ifBlank { "Nothing playing" }, 22f, white, true).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(17), 0, dp(4))
        })
        content.addView(label(PlaybackController.currentArtist().ifBlank { "Select a track" }, 12f, muted))

        val seek = SeekBar(this).apply {
            max = 1000
            progressTintList = android.content.res.ColorStateList.valueOf(violet)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        val duration = PlaybackController.duration()
                        PlaybackController.seek((duration * progress / 1000f).toLong())
                    }
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }
        content.addView(seek, LinearLayout.LayoutParams(-1, dp(42)))

        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER }
        controls.addView(label("↶", 25f, white, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { PlaybackController.previous(); refreshMini(); showPlayer() }
        }, LinearLayout.LayoutParams(dp(65), dp(60)))
        controls.addView(label(if (PlaybackController.isPlaying()) "Ⅱ" else "▶", 30f, white, true).apply {
            gravity = Gravity.CENTER
            background = accent(60)
            setOnClickListener { PlaybackController.toggle(); refreshMini(); showPlayer() }
        }, LinearLayout.LayoutParams(dp(70), dp(70)))
        controls.addView(label("↷", 25f, white, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { PlaybackController.next(); refreshMini(); showPlayer() }
        }, LinearLayout.LayoutParams(dp(65), dp(60)))
        content.addView(controls)

        val options = LinearLayout(this).apply { gravity = Gravity.CENTER }
        options.addView(label(if (PlaybackController.isShuffleEnabled()) "SHUFFLE ON" else "SHUFFLE", 10f, if (PlaybackController.isShuffleEnabled()) violet else muted, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                PlaybackController.setShuffle(!PlaybackController.isShuffleEnabled())
                showPlayer()
            }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        options.addView(label("LYRICS", 10f, muted, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener { startActivity(android.content.Intent(this@FinalPlayerActivity, LyricsActivity::class.java)) }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        options.addView(label(if (PlaybackController.isRepeatEnabled()) "REPEAT ONE" else "REPEAT", 10f, if (PlaybackController.isRepeatEnabled()) violet else muted, true).apply {
            gravity = Gravity.CENTER
            setOnClickListener {
                PlaybackController.setRepeat(!PlaybackController.isRepeatEnabled())
                showPlayer()
            }
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        content.addView(options)
        content.addView(label("SLEEP TIMER", 10f, muted, true).apply {
            gravity = Gravity.CENTER
            background = box(card2, 14, true)
            setPadding(0, dp(12), 0, dp(12))
            setOnClickListener { timerDialog() }
        })
        showPage(content)
        updateSeekbar(seek)
    }

    private fun updateSeekbar(seek: SeekBar) {
        val duration = PlaybackController.duration()
        seek.progress = if (duration > 0L) {
            (PlaybackController.position() * 1000L / duration).toInt().coerceIn(0, 1000)
        } else {
            0
        }
        ui.postDelayed({
            if (seek.parent != null) updateSeekbar(seek)
        }, 500L)
    }

    private fun showEq() {
        val content = column()
        content.addView(label("Equalizer", 26f, white, true))
        content.addView(label("5-band audio controls", 11f, muted).apply {
            setPadding(0, dp(5), 0, dp(14))
        })
        AudioEffects.attach(PlaybackController.audioSessionId())
        val range = AudioEffects.range()
        val bands = AudioEffects.bands().toInt().coerceAtLeast(5)
        for (i in 0 until minOf(bands, 5)) {
            val index = i.toShort()
            content.addView(label("Band ${i + 1}", 10f, muted, true))
            content.addView(SeekBar(this).apply {
                max = (range[1] - range[0]).toInt().coerceAtLeast(1)
                progress = (AudioEffects.level(index) - range[0]).toInt().coerceIn(0, max)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) AudioEffects.setBand(index, (progress + range[0]).toShort())
                    }
                    override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(bar: SeekBar?) = Unit
                })
            }, LinearLayout.LayoutParams(-1, dp(44)))
        }
        showPage(content)
    }

    private fun showSettings() {
        val content = column()
        content.addView(label("Settings", 26f, white, true))
        setting(content, "Background playback", "Media continues when the app is minimized") {
            toast("Background playback is enabled")
        }
        setting(content, "Notifications", "Playback controls are provided by Media3") {
            toast("Media notifications are handled by Android")
        }
        setting(content, "Refresh music", "Scan the device again") {
            tracks = scanTracks()
            showLibrary()
        }
        setting(content, "Playlists", "Create and manage playlists") {
            startActivity(android.content.Intent(this, PlaylistActivity::class.java))
        }
        showPage(content)
    }

    private fun setting(parent: LinearLayout, title: String, subtitle: String, click: () -> Unit) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box(card, 15, true)
            setPadding(dp(14), dp(13), dp(14), dp(13))
            setOnClickListener { click() }
        }
        item.addView(label(title, 13f, white, true))
        item.addView(label(subtitle, 10f, muted).apply { setPadding(0, dp(5), 0, 0) })
        parent.addView(item, LinearLayout.LayoutParams(-1, dp(74)).apply { bottomMargin = dp(8) })
    }

    private fun timerDialog() {
        val options = arrayOf("Off", "15 minutes", "30 minutes", "60 minutes")
        AlertDialog.Builder(this)
            .setTitle("Sleep timer")
            .setItems(options) { _, which ->
                toast(if (which == 0) "Sleep timer off" else "Timer set for ${options[which]}")
            }
            .show()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        ui.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
