package com.spotifusion.app

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PlaylistDetailActivity : AppCompatActivity() {
    private lateinit var store: PlaylistStore
    private lateinit var library: List<LocalTrack>
    private lateinit var list: LinearLayout
    private lateinit var playlistId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = PlaylistStore(this)
        library = runCatching { LocalMusicScanner.scan(this) }.getOrElse { emptyList() }
        playlistId = intent.getStringExtra("playlist_id").orEmpty()
        render()
    }

    private fun render() {
        val playlist = store.all().firstOrNull { it.id == playlistId } ?: run { finish(); return }
        val byId = library.associateBy { it.id }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 16)
            setBackgroundColor(Color.rgb(246, 247, 252))
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = playlist.name; textSize = 24f; setTextColor(Color.rgb(24,24,30)); setTypeface(typeface, 1)
        }, LinearLayout.LayoutParams(0, 56, 1f))
        header.addView(Button(this).apply { text = "PLAY ALL"; setOnClickListener {
            val tracks = playlist.trackIds.mapNotNull { byId[it] }
            if (tracks.isNotEmpty()) PlaybackController.playTracks(tracks)
        } })
        root.addView(header)
        root.addView(TextView(this).apply {
            text = "${playlist.trackIds.size} songs"; textSize = 12f; setTextColor(Color.DKGRAY); setPadding(0,0,0,12)
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        list.removeAllViews()
        playlist.trackIds.forEachIndexed { index, id ->
            val track = byId[id] ?: return@forEachIndexed
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(12, 8, 4, 8); setBackgroundColor(Color.WHITE) }
            val info = TextView(this).apply { text = "${index + 1}. ${track.title}\n${track.artist}"; textSize = 14f; setTextColor(Color.rgb(24,24,30)) }
            row.addView(info, LinearLayout.LayoutParams(0, 64, 1f))
            row.addView(Button(this).apply { text = "UP"; isEnabled = index > 0; setOnClickListener { move(id, -1) } })
            row.addView(Button(this).apply { text = "DOWN"; isEnabled = index < playlist.trackIds.lastIndex; setOnClickListener { move(id, 1) } })
            row.addView(Button(this).apply { text = "REMOVE"; setOnClickListener { store.removeTrack(playlistId, id); render() } })
            list.addView(row, LinearLayout.LayoutParams(-1, 82).apply { bottomMargin = 8 })
        }
    }

    private fun move(trackId: Long, delta: Int) {
        val p = store.all().firstOrNull { it.id == playlistId } ?: return
        val ids = p.trackIds.toMutableList()
        val from = ids.indexOf(trackId)
        val to = from + delta
        if (from < 0 || to !in ids.indices) return
        val item = ids.removeAt(from)
        ids.add(to, item)
        // Rebuild using public store operations to keep persistence encapsulated.
        val existing = ids.toSet()
        p.trackIds.filter { it !in existing }.forEach { store.removeTrack(playlistId, it) }
        // Ordering is persisted by replacing through a small helper below.
        store.replaceTracks(playlistId, ids)
        render()
    }
}
