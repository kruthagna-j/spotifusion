package com.spotifusion.app

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PlaylistActivity : AppCompatActivity() {
    private lateinit var store: PlaylistStore
    private lateinit var library: List<LocalTrack>
    private lateinit var list: LinearLayout
    private val bg = Color.rgb(5, 5, 9)
    private val card = Color.rgb(14, 14, 21)
    private val card2 = Color.rgb(22, 21, 31)
    private val border = Color.rgb(43, 40, 58)
    private val white = Color.rgb(247, 245, 252)
    private val muted = Color.rgb(145, 140, 160)
    private val violet = Color.rgb(123, 81, 251)

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun rounded(color: Int = card, radius: Int = 16) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(1), border) }
    private fun text(value: String, size: Float, color: Int = white, bold: Boolean = false) = TextView(this).apply { text = value; textSize = size; setTextColor(color); includeFontPadding = false; if (bold) typeface = Typeface.DEFAULT_BOLD }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        store = PlaylistStore(this)
        library = runCatching { LocalMusicScanner.scan(this) }.getOrElse { emptyList() }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(dp(14), dp(12), dp(14), dp(20)) }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(text("Playlists", 26f, white, true), LinearLayout.LayoutParams(0, dp(52), 1f))
        header.addView(text("+", 24f, white, true).apply {
            gravity = Gravity.CENTER
            background = rounded(violet, 15)
            setOnClickListener { createPlaylist() }
        }, LinearLayout.LayoutParams(dp(48), dp(48)))
        root.addView(header)
        root.addView(text("${library.size} songs available on this device", 11f, muted).apply { setPadding(0, 0, 0, dp(14)) })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        render()
    }

    private fun createPlaylist() {
        val input = EditText(this).apply { hint = "Playlist name"; setHintTextColor(muted); setTextColor(white); setSingleLine(true); background = rounded(card2, 14); setPadding(dp(12), 0, dp(12), 0) }
        val wrap = FrameLayout(this).apply { setPadding(dp(18), 0, dp(18), 0); addView(input, FrameLayout.LayoutParams(-1, dp(50))) }
        AlertDialog.Builder(this).setTitle("Create playlist").setView(wrap).setNegativeButton("CANCEL", null).setPositiveButton("CREATE") { _, _ ->
            if (store.create(input.text.toString()) == null) Toast.makeText(this, "Enter a unique playlist name.", Toast.LENGTH_SHORT).show()
            render()
        }.show()
    }

    private fun render() {
        list.removeAllViews()
        val playlists = store.all()
        if (playlists.isEmpty()) {
            val empty = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; background = rounded(card, 18); setPadding(dp(20), dp(30), dp(20), dp(30)) }
            empty.addView(text("▤", 34f, violet, true).apply { gravity = Gravity.CENTER })
            empty.addView(text("No playlists yet", 15f, white, true).apply { gravity = Gravity.CENTER; setPadding(0, dp(10), 0, dp(5)) })
            empty.addView(text("Create a playlist and add songs from your library.", 10f, muted).apply { gravity = Gravity.CENTER })
            list.addView(empty, LinearLayout.LayoutParams(-1, dp(170)))
            return
        }
        val byId = library.associateBy { it.id }
        playlists.forEach { playlist ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = rounded(card, 16); setPadding(dp(8), dp(8), dp(8), dp(8)) }
            val firstTrack = playlist.trackIds.firstNotNullOfOrNull { byId[it] }
            row.addView(ArtworkViewFactory.create(this, firstTrack, dp(62)), LinearLayout.LayoutParams(dp(62), dp(62)))
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(12), 0, dp(8), 0) }
            info.addView(text(playlist.name, 14f, white, true).apply { maxLines = 1 })
            info.addView(text("${playlist.trackIds.size} songs", 10f, muted).apply { setPadding(0, dp(5), 0, 0) })
            row.addView(info, LinearLayout.LayoutParams(0, dp(64), 1f))
            row.addView(text("▶", 12f, violet, true).apply { gravity = Gravity.CENTER; setPadding(dp(8), 0, dp(8), 0); setOnClickListener { playPlaylist(playlist) } })
            row.addView(text("+", 18f, muted, true).apply { gravity = Gravity.CENTER; setPadding(dp(8), 0, dp(8), 0); setOnClickListener { addSong(playlist) } })
            row.addView(text("×", 18f, muted, true).apply { gravity = Gravity.CENTER; setPadding(dp(8), 0, dp(5), 0); setOnClickListener { store.delete(playlist.id); render() } })
            list.addView(row, LinearLayout.LayoutParams(-1, dp(80)).apply { bottomMargin = dp(8) })
        }
    }

    private fun addSong(playlist: PlaylistStore.Playlist) {
        if (library.isEmpty()) { Toast.makeText(this, "No local music found.", Toast.LENGTH_SHORT).show(); return }
        val names = library.map { "${it.title} — ${it.artist}" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Add to ${playlist.name}").setItems(names) { _, which -> store.addTrack(playlist.id, library[which].id); render() }.show()
    }

    private fun playPlaylist(playlist: PlaylistStore.Playlist) {
        val byId = library.associateBy { it.id }
        val selected = playlist.trackIds.mapNotNull { byId[it] }
        if (selected.isEmpty()) { Toast.makeText(this, "This playlist has no available local songs.", Toast.LENGTH_SHORT).show(); return }
        PlaybackController.playTracks(selected)
        Toast.makeText(this, "Playing ${playlist.name}", Toast.LENGTH_SHORT).show()
    }
}
