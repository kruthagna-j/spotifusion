package com.spotifusion.app

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val green = Color.rgb(30, 215, 96)
    private val black = Color.rgb(8, 8, 8)
    private val surface = Color.rgb(18, 18, 18)
    private val surface2 = Color.rgb(28, 28, 28)
    private val surface3 = Color.rgb(43, 43, 43)
    private val white = Color.WHITE
    private val gray = Color.rgb(170, 170, 170)
    private val muted = Color.rgb(115, 115, 115)

    private data class Track(
        val id: Long,
        val title: String,
        val artist: String,
        val uri: Uri
    )

    private val tracks = mutableListOf<Track>()
    private val favorites = mutableSetOf<Long>()

    private val prefs by lazy {
        getSharedPreferences("spotifusion", MODE_PRIVATE)
    }

    private var player: MediaPlayer? = null
    private var currentIndex = -1
    private var currentTrack: Track? = null

    private var repeat = false
    private var shuffle = false
    private var searchQuery = ""
    private var sleepMinutes = 0

    private var sleepRunnable: Runnable? = null

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = black
        window.navigationBarColor = black

        loadFavorites()

        if (!prefs.getBoolean("onboarding_done", false)) {
            showWelcome()
        } else {
            startApp()
        }
    }

    private fun dp(v: Int): Int {
        return (v * resources.displayMetrics.density + 0.5f).toInt()
    }

    private fun bg(color: Int, radius: Int) =
        android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
        }

    private fun tv(
        value: String,
        size: Float,
        color: Int = white,
        bold: Boolean = false
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        includeFontPadding = false

        if (bold) {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    }

    private fun page() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(black)
    }

    private fun startApp() {
        requestAudioPermissionIfNeeded {
            loadLocalMusic()
            showHome()
        }
    }

    private fun audioPermission(): String? {
        return if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun requestAudioPermissionIfNeeded(after: () -> Unit) {
        val permission = audioPermission()

        if (
            permission == null ||
            checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        ) {
            after()
            return
        }

        pendingAfterPermission = after
        requestPermissions(arrayOf(permission), 900)
    }

    private var pendingAfterPermission: (() -> Unit)? = null

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 900) {
            pendingAfterPermission?.invoke()
            pendingAfterPermission = null
        }
    }

    private fun showWelcome() {
        val root = page()

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(
                dp(28),
                dp(40),
                dp(28),
                dp(30)
            )
        }

        box.addView(
            tv("♫", 58f, green, true).apply {
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(-1, dp(75))
        )

        box.addView(
            tv("SPOTIFUSION", 25f, white, true).apply {
                gravity = Gravity.CENTER
                letterSpacing = 0.08f
            }
        )

        box.addView(
            tv("MUSIC WITHOUT LIMITS", 11f, green, true).apply {
                gravity = Gravity.CENTER
                letterSpacing = 0.15f
                setPadding(0, dp(8), 0, 0)
            }
        )

        box.addView(
            tv(
                "Your music.\nYour way.",
                38f,
                white,
                true
            ).apply {
                gravity = Gravity.CENTER
                setPadding(
                    0,
                    dp(58),
                    0,
                    dp(18)
                )
            }
        )

        box.addView(
            tv(
                "A native music experience for your local songs, playlists and listening controls.",
                15f,
                gray
            ).apply {
                gravity = Gravity.CENTER
                setLineSpacing(dp(3).toFloat(), 1f)
            }
        )

        val continueButton = button("Continue ->") {
            showLanguages()
        }

        box.addView(
            continueButton,
            LinearLayout.LayoutParams(-1, dp(56)).apply {
                topMargin = dp(34)
            }
        )

        box.addView(
            tv(
                "Your music stays on your device unless you explicitly connect a service.",
                11f,
                muted
            ).apply {
                gravity = Gravity.CENTER
                setPadding(0, dp(16), 0, 0)
            }
        )

        scroll.addView(box)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        setContentView(root)
    }

    private fun button(
        label: String,
        primary: Boolean = true,
        action: () -> Unit
    ) = TextView(this).apply {
        text = label
        textSize = 15f
        gravity = Gravity.CENTER
        typeface = Typeface.DEFAULT_BOLD

        setTextColor(
            if (primary) Color.BLACK else white
        )

        background = bg(
            if (primary) green else surface2,
            28
        )

        setOnClickListener {
            action()
        }
    }

    private fun showLanguages() {
        val root = page()

        root.setPadding(
            dp(22),
            dp(28),
            dp(22),
            dp(20)
        )

        root.addView(
            tv(
                "Spotifusion",
                25f,
                green,
                true
            )
        )

        root.addView(
            tv(
                "STEP 1 OF 2",
                11f,
                green,
                true
            ).apply {
                setPadding(
                    0,
                    dp(24),
                    0,
                    dp(12)
                )
            }
        )

        root.addView(
            tv(
                "What languages do you listen to?",
                29f,
                white,
                true
            )
        )

        root.addView(
            tv(
                "Choose your favorite languages.",
                14f,
                gray
            ).apply {
                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(18)
                )
            }
        )

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val selected = mutableSetOf<String>()

        arrayOf(
            "English",
            "Telugu",
            "Hindi",
            "Tamil",
            "Kannada",
            "Malayalam",
            "Marathi",
            "Bengali"
        ).forEach { language ->

            val row = CheckBox(this).apply {
                text = language
                textSize = 16f
                setTextColor(white)

                buttonTintList =
                    android.content.res.ColorStateList.valueOf(green)

                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        selected.add(language)
                    } else {
                        selected.remove(language)
                    }
                }
            }

            list.addView(
                row,
                LinearLayout.LayoutParams(-1, dp(52))
            )
        }

        val scroll = ScrollView(this)

        scroll.addView(list)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        root.addView(
            button("Continue ->") {
                if (selected.isEmpty()) {
                    toast("Select at least one language")
                } else {
                    showArtists()
                }
            },
            LinearLayout.LayoutParams(-1, dp(56)).apply {
                topMargin = dp(12)
            }
        )

        setContentView(root)
    }

    private fun showArtists() {
        val root = page()

        root.setPadding(
            dp(22),
            dp(28),
            dp(22),
            dp(20)
        )

        root.addView(
            tv(
                "Spotifusion",
                25f,
                green,
                true
            )
        )

        root.addView(
            tv(
                "STEP 2 OF 2",
                11f,
                green,
                true
            ).apply {
                setPadding(
                    0,
                    dp(24),
                    0,
                    dp(12)
                )
            }
        )

        root.addView(
            tv(
                "Choose your favorite artists.",
                29f,
                white,
                true
            )
        )

        root.addView(
            tv(
                "Pick as many as you like.",
                14f,
                gray
            ).apply {
                setPadding(
                    0,
                    dp(8),
                    0,
                    dp(18)
                )
            }
        )

        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        arrayOf(
            "Anirudh Ravichander",
            "A. R. Rahman",
            "Arijit Singh",
            "Sid Sriram",
            "Shreya Ghoshal",
            "Taylor Swift",
            "The Weeknd",
            "Ed Sheeran",
            "Dua Lipa",
            "Bruno Mars"
        ).forEach { artist ->

            list.addView(
                CheckBox(this).apply {
                    text = artist
                    textSize = 16f
                    setTextColor(white)

                    buttonTintList =
                        android.content.res.ColorStateList.valueOf(green)
                },
                LinearLayout.LayoutParams(-1, dp(52))
            )
        }

        val scroll = ScrollView(this)

        scroll.addView(list)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        root.addView(
            button("Start listening ->") {
                prefs.edit()
                    .putBoolean("onboarding_done", true)
                    .apply()

                startApp()
            },
            LinearLayout.LayoutParams(-1, dp(56)).apply {
                topMargin = dp(12)
            }
        )

        setContentView(root)
    }

    private fun loadFavorites() {
        prefs.getStringSet(
            "favorites",
            emptySet()
        )?.forEach {
            favorites.add(
                it.toLongOrNull() ?: -1L
            )
        }
    }

    private fun saveFavorites() {
        prefs.edit()
            .putStringSet(
                "favorites",
                favorites
                    .filter { it >= 0 }
                    .map { it.toString() }
                    .toSet()
            )
            .apply()
    }

    private fun loadLocalMusic() {
        tracks.clear()

        val permission = audioPermission()

        if (
            permission != null &&
            checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST
        )

        val selection =
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { c ->

            val idCol =
                c.getColumnIndexOrThrow(
                    MediaStore.Audio.Media._ID
                )

            val titleCol =
                c.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.TITLE
                )

            val artistCol =
                c.getColumnIndexOrThrow(
                    MediaStore.Audio.Media.ARTIST
                )

            while (c.moveToNext()) {

                val id = c.getLong(idCol)

                val title =
                    c.getString(titleCol)
                        .orEmpty()
                        .ifBlank {
                            "Unknown song"
                        }

                val artist =
                    c.getString(artistCol)
                        .orEmpty()
                        .ifBlank {
                            "Unknown artist"
                        }

                val uri =
                    ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                tracks.add(
                    Track(
                        id,
                        title,
                        artist,
                        uri
                    )
                )
            }
        }
    }

    private fun showHome() {
        val root = page()

        val content =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp(20),
                    dp(18),
                    dp(20),
                    dp(18)
                )
            }

        val scroll =
            ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
            }

        val header =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

        header.addView(
            tv(
                "Spotifusion",
                24f,
                white,
                true
            ),
            LinearLayout.LayoutParams(
                0,
                dp(46),
                1f
            )
        )

        header.addView(
            icon("Search") {
                showSearch()
            },
            LinearLayout.LayoutParams(
                dp(70),
                dp(44)
            ).apply {
                rightMargin = dp(8)
            }
        )

        header.addView(
            icon("Settings") {
                showSettings()
            },
            LinearLayout.LayoutParams(
                dp(70),
                dp(44)
            )
        )

        content.addView(header)

        content.addView(
            tv(
                "Good evening",
                29f,
                white,
                true
            ).apply {
                setPadding(
                    0,
                    dp(24),
                    0,
                    dp(4)
                )
            }
        )

        content.addView(
            tv(
                "Your music, your way.",
                14f,
                gray
            ).apply {
                setPadding(
                    0,
                    0,
                    0,
                    dp(8)
                )
            }
        )

        section(
            content,
            "Quick access"
        )

        val quick =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

        quickCard(
            quick,
            "All Songs",
            "♫"
        ) {
            showLibrary()
        }

        quickCard(
            quick,
            "Favorites",
            "♡"
        ) {
            showFavorites()
        }

        quickCard(
            quick,
            "Recently",
            "R"
        ) {
            showRecent()
        }

        content.addView(
            horizontal(quick),
            LinearLayout.LayoutParams(
                -1,
                dp(78)
            )
        )

        section(
            content,
            "Your local music"
        )

        if (tracks.isEmpty()) {

            val empty =
                tv(
                    "No local songs found. Tap below to scan your device again.",
                    14f,
                    gray
                ).apply {
                    setPadding(
                        dp(16),
                        dp(16),
                        dp(16),
                        dp(16)
                    )

                    background =
                        bg(surface2, 14)
                }

            content.addView(
                empty,
                LinearLayout.LayoutParams(
                    -1,
                    dp(78)
                )
            )

            content.addView(
                button("Scan music") {
                    loadLocalMusic()
                    showHome()
                },
                LinearLayout.LayoutParams(
                    -1,
                    dp(50)
                ).apply {
                    topMargin = dp(10)
                }
            )

        } else {

            tracks
                .take(8)
                .forEach {
                    songRow(
                        content,
                        it
                    )
                }
        }

        section(
            content,
            "Popular shortcuts"
        )

        content.addView(
            button("Open full library") {
                showLibrary()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            )
        )

        content.addView(
            button(
                "Search your music",
                false
            ) {
                showSearch()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(52)
            ).apply {
                topMargin = dp(10)
            }
        )

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        addBottom(
            root,
            0
        )

        setContentView(root)
    }

    private fun horizontal(row: View) =
        HorizontalScrollView(this).apply {

            isHorizontalScrollBarEnabled = false
            overScrollMode =
                View.OVER_SCROLL_NEVER

            addView(row)
        }

    private fun icon(
        s: String,
        action: () -> Unit
    ) =
        tv(
            s,
            14f,
            white,
            true
        ).apply {

            gravity = Gravity.CENTER
            background = bg(
                surface2,
                50
            )

            setOnClickListener {
                action()
            }
        }

    private fun section(
        p: LinearLayout,
        title: String
    ) {

        p.addView(
            tv(
                title,
                20f,
                white,
                true
            ).apply {
                setPadding(
                    0,
                    dp(22),
                    0,
                    dp(11)
                )
            }
        )
    }

    private fun quickCard(
        row: LinearLayout,
        title: String,
        glyph: String,
        action: () -> Unit
    ) {

        val c =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                background =
                    bg(surface2, 12)

                setPadding(
                    dp(8),
                    dp(7),
                    dp(10),
                    dp(7)
                )

                setOnClickListener {
                    action()
                }
            }

        c.addView(
            tv(
                glyph,
                20f,
                green,
                true
            ).apply {
                gravity = Gravity.CENTER
                background =
                    bg(surface3, 9)
            },
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        c.addView(
            tv(
                title,
                13f,
                white,
                true
            ).apply {
                setPadding(
                    dp(10),
                    0,
                    0,
                    0
                )
            },
            LinearLayout.LayoutParams(
                dp(95),
                -1
            )
        )

        row.addView(
            c,
            LinearLayout.LayoutParams(
                dp(150),
                dp(64)
            ).apply {
                rightMargin = dp(10)
            }
        )
    }

    private fun songRow(
        parent: LinearLayout,
        track: Track
    ) {

        val row =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                background =
                    bg(surface, 12)

                setPadding(
                    dp(7),
                    dp(7),
                    dp(4),
                    dp(7)
                )

                setOnClickListener {
                    playTrack(track)
                }
            }

        row.addView(
            tv(
                "♫",
                22f,
                green,
                true
            ).apply {
                gravity = Gravity.CENTER
                background =
                    bg(surface3, 9)
            },
            LinearLayout.LayoutParams(
                dp(54),
                dp(54)
            )
        )

        val info =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(12),
                    0,
                    dp(4),
                    0
                )
            }

        info.addView(
            tv(
                track.title,
                15f,
                white,
                true
            )
        )

        info.addView(
            tv(
                track.artist,
                12f,
                gray
            ).apply {
                setPadding(
                    0,
                    dp(4),
                    0,
                    0
                )
            }
        )

        row.addView(
            info,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )

        val favorite =
            tv(
                if (favorites.contains(track.id)) {
                    "♥"
                } else {
                    "♡"
                },
                22f,
                if (favorites.contains(track.id)) {
                    green
                } else {
                    gray
                }
            ).apply {

                gravity = Gravity.CENTER

                setOnClickListener {

                    toggleFavorite(track)

                    text =
                        if (favorites.contains(track.id)) {
                            "♥"
                        } else {
                            "♡"
                        }
                }
            }

        row.addView(
            favorite,
            LinearLayout.LayoutParams(
                dp(42),
                dp(54)
            )
        )

        parent.addView(
            row,
            LinearLayout.LayoutParams(
                -1,
                dp(68)
            ).apply {
                bottomMargin = dp(7)
            }
        )
    }

    private fun showLibrary() {

        val root = page()

        val content =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(22),
                    dp(20),
                    dp(20)
                )
            }

        val scroll =
            ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
            }

        content.addView(
            tv(
                "Your Library",
                30f,
                white,
                true
            )
        )

        content.addView(
            tv(
                "${tracks.size} songs on this device",
                14f,
                gray
            ).apply {
                setPadding(
                    0,
                    dp(7),
                    0,
                    dp(18)
                )
            }
        )

        if (tracks.isEmpty()) {

            content.addView(
                tv(
                    "No local music found.",
                    15f,
                    gray
                ).apply {
                    setPadding(
                        dp(15),
                        dp(15),
                        dp(15),
                        dp(15)
                    )

                    background =
                        bg(surface2, 14)
                }
            )

        } else {

            tracks.forEach {
                songRow(
                    content,
                    it
                )
            }
        }

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        addBottom(
            root,
            2
        )

        setContentView(root)
    }

    private fun showFavorites() =
        showTrackList(
            "Liked Songs",
            tracks.filter {
                favorites.contains(it.id)
            }
        )

    private fun showRecent() =
        showTrackList(
            "Recently Played",
            tracks.filter {
                prefs.getStringSet(
                    "recent",
                    emptySet()
                )?.contains(
                    it.id.toString()
                ) == true
            }
        )

    private fun showTrackList(
        title: String,
        list: List<Track>
    ) {

        val root = page()

        val content =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(22),
                    dp(20),
                    dp(20)
                )
            }

        val scroll =
            ScrollView(this)

        content.addView(
            tv(
                title,
                30f,
                white,
                true
            )
        )

        content.addView(
            tv(
                "${list.size} songs",
                14f,
                gray
            ).apply {
                setPadding(
                    0,
                    dp(7),
                    0,
                    dp(18)
                )
            }
        )

        if (list.isEmpty()) {

            content.addView(
                tv(
                    "Nothing here yet.",
                    15f,
                    gray
                ).apply {
                    setPadding(
                        dp(16),
                        dp(16),
                        dp(16),
                        dp(16)
                    )

                    background =
                        bg(surface2, 14)
                }
            )

        } else {

            list.forEach {
                songRow(
                    content,
                    it
                )
            }
        }

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        addBottom(
            root,
            2
        )

        setContentView(root)
    }

    private fun showSearch() {

        val root = page()

        val content =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(22),
                    dp(20),
                    dp(18)
                )
            }

        val scroll =
            ScrollView(this).apply {
                isVerticalScrollBarEnabled = false
            }

        content.addView(
            tv(
                "Search",
                30f,
                white,
                true
            )
        )

        val input =
            EditText(this).apply {

                hint =
                    "Search songs or artists"

                textSize = 15f

                setTextColor(Color.BLACK)

                setHintTextColor(
                    Color.DKGRAY
                )

                background =
                    bg(white, 14)

                setPadding(
                    dp(14),
                    0,
                    dp(14),
                    0
                )

                isSingleLine = true
            }

        content.addView(
            input,
            LinearLayout.LayoutParams(
                -1,
                dp(54)
            ).apply {
                topMargin = dp(18)
            }
        )

        val results =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL
            }

        content.addView(results)

        fun render(q: String) {

            results.removeAllViews()

            val found =
                tracks.filter {
                    it.title.contains(
                        q,
                        true
                    ) ||
                    it.artist.contains(
                        q,
                        true
                    )
                }

            if (q.isBlank()) {

                tracks
                    .take(12)
                    .forEach {
                        songRow(
                            results,
                            it
                        )
                    }

            } else if (found.isEmpty()) {

                results.addView(
                    tv(
                        "No matching songs.",
                        14f,
                        gray
                    ).apply {
                        setPadding(
                            0,
                            dp(24),
                            0,
                            0
                        )
                    }
                )

            } else {

                found.forEach {
                    songRow(
                        results,
                        it
                    )
                }
            }
        }

        input.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    searchQuery =
                        s?.toString().orEmpty()

                    render(searchQuery)
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )

        render("")

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        addBottom(
            root,
            1
        )

        setContentView(root)
    }

    private fun playTrack(track: Track) {

        currentIndex =
            tracks.indexOfFirst {
                it.id == track.id
            }

        currentTrack = track

        player?.release()

        player =
            MediaPlayer().apply {

                setAudioStreamType(
                    AudioManager.STREAM_MUSIC
                )

                setDataSource(
                    this@MainActivity,
                    track.uri
                )

                setOnPreparedListener {
                    start()
                    showPlayer()
                }

                setOnCompletionListener {

                    if (repeat) {
                        playTrack(track)
                    } else {
                        nextTrack()
                    }
                }

                setOnErrorListener { _, _, _ ->

                    toast(
                        "Unable to play this file"
                    )

                    true
                }

                prepareAsync()
            }

        addRecent(track)
    }

    private fun addRecent(track: Track) {

        val old =
            prefs.getStringSet(
                "recent",
                emptySet()
            )!!.toMutableSet()

        old.remove(
            track.id.toString()
        )

        old.add(
            track.id.toString()
        )

        while (old.size > 20) {
            old.remove(
                old.first()
            )
        }

        prefs.edit()
            .putStringSet(
                "recent",
                old
            )
            .apply()
    }

    private fun toggleFavorite(
        t: Track
    ) {

        if (!favorites.add(t.id)) {
            favorites.remove(t.id)
        }

        saveFavorites()

        showHome()
    }

    private fun nextTrack() {

        if (tracks.isEmpty()) {
            return
        }

        val next =
            if (shuffle) {
                tracks.indices.random()
            } else {
                (currentIndex + 1) % tracks.size
            }

        playTrack(
            tracks[next]
        )
    }

    private fun previousTrack() {

        if (tracks.isEmpty()) {
            return
        }

        val prev =
            if (currentIndex <= 0) {
                tracks.lastIndex
            } else {
                currentIndex - 1
            }

        playTrack(
            tracks[prev]
        )
    }

    private fun showPlayer() {

        val t =
            currentTrack ?: return

        val root = page()

        root.setPadding(
            dp(20),
            dp(22),
            dp(20),
            dp(20)
        )

        root.addView(
            tv(
                "Now Playing",
                20f,
                gray,
                true
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        root.addView(
            tv(
                "♫",
                88f,
                green,
                true
            ).apply {
                gravity = Gravity.CENTER
                background =
                    bg(surface2, 24)
            },
            LinearLayout.LayoutParams(
                -1,
                dp(260)
            ).apply {
                topMargin = dp(24)
            }
        )

        root.addView(
            tv(
                t.title,
                24f,
                white,
                true
            ).apply {
                gravity = Gravity.CENTER

                setPadding(
                    0,
                    dp(22),
                    0,
                    dp(4)
                )
            }
        )

        root.addView(
            tv(
                t.artist,
                15f,
                gray
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        val seek =
            SeekBar(this)

        root.addView(
            seek,
            LinearLayout.LayoutParams(
                -1,
                dp(40)
            ).apply {
                topMargin = dp(20)
            }
        )

        player?.let {

            seek.max =
                it.duration

            seek.progress =
                it.currentPosition
        }

        seek.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(
                    s: SeekBar?,
                    p: Int,
                    u: Boolean
                ) {

                    if (u) {
                        player?.seekTo(p)
                    }
                }

                override fun onStartTrackingTouch(
                    s: SeekBar?
                ) {
                }

                override fun onStopTrackingTouch(
                    s: SeekBar?
                ) {
                }
            }
        )

        val controls =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER
            }

        controls.addView(
            icon("Prev") {
                previousTrack()
            },
            LinearLayout.LayoutParams(
                dp(70),
                dp(54)
            )
        )

        controls.addView(
            icon(
                if (player?.isPlaying == true) {
                    "Pause"
                } else {
                    "Play"
                }
            ) {

                player?.let {

                    if (it.isPlaying) {
                        it.pause()
                    } else {
                        it.start()
                    }
                }

                showPlayer()
            },
            LinearLayout.LayoutParams(
                dp(90),
                dp(64)
            ).apply {
                leftMargin = dp(12)
                rightMargin = dp(12)
            }
        )

        controls.addView(
            icon("Next") {
                nextTrack()
            },
            LinearLayout.LayoutParams(
                dp(70),
                dp(54)
            )
        )

        root.addView(
            controls,
            LinearLayout.LayoutParams(
                -1,
                dp(70)
            ).apply {
                topMargin = dp(18)
            }
        )

        val opts =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER
            }

        opts.addView(
            button(
                if (shuffle) {
                    "Shuffle OK"
                } else {
                    "Shuffle"
                },
                false
            ) {

                shuffle = !shuffle

                showPlayer()
            },
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        opts.addView(
            button(
                if (repeat) {
                    "Repeat OK"
                } else {
                    "Repeat"
                },
                false
            ) {

                repeat = !repeat

                showPlayer()
            },
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            ).apply {
                leftMargin = dp(8)
            }
        )

        root.addView(
            opts,
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            ).apply {
                topMargin = dp(18)
            }
        )

        root.addView(
            button(
                "♡  Like",
                false
            ) {
                toggleFavorite(t)
            },
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            ).apply {
                topMargin = dp(10)
            }
        )

        root.addView(
            button(
                "Sleep timer",
                false
            ) {
                showSleepDialog()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            ).apply {
                topMargin = dp(10)
            }
        )

        root.addView(
            button(
                "<- Back",
                false
            ) {
                showHome()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(48)
            ).apply {
                topMargin = dp(10)
            }
        )

        setContentView(root)

        updateSeek(seek)
    }

    private fun updateSeek(
        seek: SeekBar
    ) {

        handler.postDelayed({

            if (isFinishing) {
                return@postDelayed
            }

            player?.let {

                if (seek.max > 0) {
                    seek.progress =
                        it.currentPosition
                }
            }

            if (currentTrack != null) {
                updateSeek(seek)
            }

        }, 500)
    }

    private fun showSleepDialog() {

        val choices =
            arrayOf(
                "Off",
                "15 minutes",
                "30 minutes",
                "60 minutes"
            )

        AlertDialog.Builder(this)
            .setTitle("Sleep timer")
            .setItems(choices) { _, which ->

                sleepMinutes =
                    when (which) {
                        1 -> 15
                        2 -> 30
                        3 -> 60
                        else -> 0
                    }

                sleepRunnable?.let {
                    handler.removeCallbacks(it)
                }

                if (sleepMinutes > 0) {

                    sleepRunnable =
                        Runnable {
                            player?.pause()
                        }

                    handler.postDelayed(
                        sleepRunnable!!,
                        sleepMinutes * 60_000L
                    )
                }

                toast(
                    if (sleepMinutes == 0) {
                        "Sleep timer off"
                    } else {
                        "Stops in $sleepMinutes minutes"
                    }
                )
            }
            .show()
    }

    private fun showSettings() {

        val root = page()

        root.setPadding(
            dp(20),
            dp(22),
            dp(20),
            dp(20)
        )

        root.addView(
            tv(
                "Settings",
                30f,
                white,
                true
            )
        )

        root.addView(
            tv(
                "Playback",
                20f,
                white,
                true
            ).apply {
                setPadding(
                    0,
                    dp(24),
                    0,
                    dp(12)
                )
            }
        )

        root.addView(
            button(
                "Rescan local music",
                false
            ) {

                loadLocalMusic()

                toast(
                    "Found ${tracks.size} songs"
                )

                showSettings()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            )
        )

        root.addView(
            button(
                "Reset onboarding",
                false
            ) {

                prefs.edit()
                    .putBoolean(
                        "onboarding_done",
                        false
                    )
                    .apply()

                showWelcome()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            ).apply {
                topMargin = dp(10)
            }
        )

        root.addView(
            button(
                "<- Back",
                false
            ) {
                showHome()
            },
            LinearLayout.LayoutParams(
                -1,
                dp(50)
            ).apply {
                topMargin = dp(24)
            }
        )

        setContentView(root)
    }

    private fun addBottom(
        root: LinearLayout,
        selected: Int
    ) {

        val nav =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER

                setBackgroundColor(surface)

                setPadding(
                    dp(6),
                    dp(6),
                    dp(6),
                    dp(6)
                )
            }

        navItem(
            nav,
            "Home",
            "Home",
            selected == 0
        ) {
            showHome()
        }

        navItem(
            nav,
            "Search",
            "Search",
            selected == 1
        ) {
            showSearch()
        }

        navItem(
            nav,
            "Library",
            "Library",
            selected == 2
        ) {
            showLibrary()
        }

        root.addView(
            nav,
            LinearLayout.LayoutParams(
                -1,
                dp(64)
            )
        )
    }

    private fun navItem(
        parent: LinearLayout,
        glyph: String,
        label: String,
        selected: Boolean,
        action: () -> Unit
    ) {

        val item =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                setOnClickListener {
                    action()
                }
            }

        item.addView(
            tv(
                glyph,
                12f,
                if (selected) green else gray,
                true
            ).apply {
                gravity = Gravity.CENTER
            }
        )

        item.addView(
            tv(
                label,
                10f,
                if (selected) green else gray,
                selected
            ).apply {
                gravity = Gravity.CENTER

                setPadding(
                    0,
                    dp(3),
                    0,
                    0
                )
            }
        )

        parent.addView(
            item,
            LinearLayout.LayoutParams(
                0,
                -1,
                1f
            )
        )
    }

    private fun toast(s: String) =
        Toast.makeText(
            this,
            s,
            Toast.LENGTH_SHORT
        ).show()

    override fun onDestroy() {

        sleepRunnable?.let {
            handler.removeCallbacks(it)
        }

        handler.removeCallbacksAndMessages(null)

        player?.release()
        player = null

        super.onDestroy()
    }
}