package com.spotifusion.app

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PlaylistActivity : AppCompatActivity() {
    private lateinit var store: PlaylistStore
    private lateinit var library: List<LocalTrack>
    private lateinit var list: LinearLayout
    private val blue = Color.rgb(45, 91, 239)
    private val ink = Color.rgb(24, 24, 30)
    private val muted = Color.rgb(111, 114, 128)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = PlaylistStore(this)
        library = runCatching { LocalMusicScanner.scan(this) }.getOrElse { emptyList() }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(Color.rgb(246, 247, 252))
        }
        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = "Playlists"; textSize = 26f; setTextColor(ink); setTypeface(typeface, 1)
        }, LinearLayout.LayoutParams(0, 56, 1f))
        header.addView(Button(this).apply {
            text = "+ CREATE"
            setTextColor(Color.WHITE); setBackgroundColor(blue)
            setOnClickListener { createPlaylist() }
        })
        root.addView(header)
        root.addView(TextView(this).apply {
            text = "${library.size} songs available on this device"
            textSize = 12f; setTextColor(muted); setPadding(0, 0, 0, 16)
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        render()
    }

    private fun createPlaylist() {
        val input = EditText(this).apply { hint = "Playlist name"; singleLine = true }
        AlertDialog.Builder(this)
            .setTitle("Create playlist")
            .setView(input)
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("CREATE") { _, _ ->
                if (store.create(input.text.toString()) == null) {
                    Toast.makeText(this, "Enter a unique playlist name.", Toast.LENGTH_SHORT).show()
                }
                render()
            }.show()
    }

    private fun render() {
        list.removeAllViews()
        val playlists = store.all()
        if (playlists.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "No playlists yet. Create one and add songs from your library."
                textSize = 14f; setTextColor(muted); setPadding(8, 30, 8, 30)
            })
            return
        }
        playlists.forEach { playlist ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setPadding(16, 10, 8, 10); setBackgroundColor(Color.WHITE)
            }
            val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            info.addView(TextView(this).apply { text = playlist.name; textSize = 16f; setTextColor(ink); setTypeface(typeface, 1) })
            info.addView(TextView(this).apply { text = "${playlist.trackIds.size} songs"; textSize = 11f; setTextColor(muted) })
            row.addView(info, LinearLayout.LayoutParams(0, 62, 1f))
            row.addView(Button(this).apply {
                text = "PLAY"; setOnClickListener { playPlaylist(playlist) }
            })
            row.addView(Button(this).apply {
                text = "+ SONG"; setOnClickListener { addSong(playlist) }
            })
            row.addView(Button(this).apply {
                text = "×"; setOnClickListener {
                    store.delete(playlist.id); render()
                }
            })
            list.addView(row, LinearLayout.LayoutParams(-1, 82).apply { bottomMargin = 8 })
        }
    }

    private fun addSong(playlist: PlaylistStore.Playlist) {
        if (library.isEmpty()) {
            Toast.makeText(this, "No local music found.", Toast.LENGTH_SHORT).show(); return
        }
        val names = library.map { "${it.title} — ${it.artist}" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Add to ${playlist.name}")
            .setItems(names) { _, which ->
                store.addTrack(playlist.id, library[which].id)
                render()
            }.show()
    }

    private fun playPlaylist(playlist: PlaylistStore.Playlist) {
        val byId = library.associateBy { it.id }
        val tracks = playlist.trackIds.mapNotNull { byId[it] }
        if (tracks.isEmpty()) {
            Toast.makeText(this, "This playlist has no available local songs.", Toast.LENGTH_SHORT).show(); return
        }
        PlaybackController.playTracks(tracks)
        Toast.makeText(this, "Playing ${playlist.name}", Toast.LENGTH_SHORT).show()
    }
}
