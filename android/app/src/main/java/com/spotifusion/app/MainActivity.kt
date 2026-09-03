package com.spotifusion.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.Player

class MainActivity : NeonActivity() {
    companion object {
        private const val AUDIO_PERMISSION_REQUEST = 4101
        private const val NOTIFICATION_PERMISSION_REQUEST = 4102
    }

    private lateinit var musicStore: UserMusicStore
    private var lastRecordedMediaId: String? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        musicStore = UserMusicStore(this)
        PlaybackController.connect(this)
        requestMusicPermissions()

        val content = findViewById<ViewGroup>(android.R.id.content)
        val shell = content.getChildAt(0)
        shell?.let { root ->
            ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, bars.bottom)
                insets
            }
            ViewCompat.requestApplyInsets(root)
        }

        val volume = SeekBar(this).apply {
            max = 100
            progress = (PlaybackController.volume() * 100).toInt()
            progressTintList = ColorStateList.valueOf(Color.rgb(45, 91, 239))
            thumbTintList = ColorStateList.valueOf(Color.rgb(45, 91, 239))
            contentDescription = "Volume"
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) PlaybackController.setVolume(value / 100f)
                }
                override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                override fun onStopTrackingTouch(bar: SeekBar?) = Unit
            })
        }
        val lp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(34),
            Gravity.BOTTOM
        ).apply {
            leftMargin = dp(82)
            rightMargin = dp(82)
            bottomMargin = dp(88)
        }
        content.addView(volume, lp)

        val lyricsButton = TextView(this).apply {
            text = "LYRICS"
            textSize = 10f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            background = roundedBlue()
            contentDescription = "Open lyrics"
            setOnClickListener { startActivity(Intent(this@MainActivity, LyricsActivity::class.java)) }
        }
        val lyricsLp = FrameLayout.LayoutParams(dp(72), dp(38), Gravity.BOTTOM or Gravity.END).apply {
            rightMargin = dp(10)
            bottomMargin = dp(126)
        }
        content.addView(lyricsButton, lyricsLp)
    }

    private fun roundedBlue() = android.graphics.drawable.GradientDrawable().apply {
        setColor(Color.rgb(45, 91, 239))
        cornerRadius = dp(19).toFloat()
    }

    private fun requestMusicPermissions() {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), AUDIO_PERMISSION_REQUEST)
        } else {
            scanLocalMusic()
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQUEST) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) scanLocalMusic()
            else Toast.makeText(this, "Music access is needed to show songs stored on this device.", Toast.LENGTH_LONG).show()
        }
    }

    private fun scanLocalMusic() {
        val tracks = runCatching { LocalMusicScanner.scan(this) }.getOrElse { emptyList() }
        if (tracks.isNotEmpty()) {
            Toast.makeText(this, "Found ${tracks.size} music track${if (tracks.size == 1) "" else "s"} on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) = syncPlayerUi()
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            recordRecentlyPlayed(mediaItem)
            syncPlayerUi()
        }
        override fun onPlaybackStateChanged(playbackState: Int) = syncPlayerUi()
        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = syncPlayerUi()
        override fun onRepeatModeChanged(repeatMode: Int) = syncPlayerUi()
    }

    private val uiTicker = object : Runnable {
        override fun run() {
            syncPlayerUi()
            window.decorView.postDelayed(this, 500L)
        }
    }

    override fun onStart() {
        super.onStart()
        PlaybackController.addListener(playerListener)
        window.decorView.post(uiTicker)
        syncPlayerUi()
    }

    override fun onStop() {
        window.decorView.removeCallbacks(uiTicker)
        PlaybackController.removeListener(playerListener)
        super.onStop()
    }

    private fun recordRecentlyPlayed(mediaItem: androidx.media3.common.MediaItem?) {
        val id = mediaItem?.mediaId ?: return
        if (id == lastRecordedMediaId) return
        val trackId = id.toLongOrNull() ?: return
        val track = runCatching { LocalMusicScanner.scan(this).firstOrNull { it.id == trackId } }.getOrNull() ?: return
        musicStore.addRecentlyPlayed(track)
        lastRecordedMediaId = id
    }

    private fun syncPlayerUi() {
        val root = findViewById<ViewGroup>(android.R.id.content)?.getChildAt(0) as? ViewGroup ?: return
        val title = PlaybackController.currentTitle().ifBlank { null }
        val artist = PlaybackController.currentArtist().ifBlank { null }
        val mini = root.getChildAt(2) as? ViewGroup
        val info = mini?.getChildAt(1) as? ViewGroup
        (info?.getChildAt(0) as? TextView)?.let { if (title != null) it.text = title }
        (info?.getChildAt(1) as? TextView)?.let { if (artist != null) it.text = if (PlaybackController.isPlaying()) "$artist · Playing" else artist }
        (mini?.getChildAt(2) as? TextView)?.setOnClickListener {
            val trackId = PlaybackController.currentMediaId()?.toLongOrNull()
            if (trackId != null) {
                val favorite = musicStore.toggleFavorite(trackId)
                Toast.makeText(this, if (favorite) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
            }
        }
        (mini?.getChildAt(3) as? TextView)?.let {
            it.text = if (PlaybackController.isPlaying()) "Ⅱ" else "▶"
            it.setOnClickListener { PlaybackController.toggle() }
        }

        val page = root.getChildAt(1) as? ViewGroup ?: return
        val nowPlayingHeader = findTextView(page, "NOW PLAYING") ?: return
        val column = nowPlayingHeader.parent as? ViewGroup ?: return
        if (column.childCount >= 7) {
            (column.getChildAt(1) as? TextView)?.let { if (title != null) it.text = title }
            (column.getChildAt(2) as? TextView)?.let { if (artist != null) it.text = artist }
            val seeks = mutableListOf<SeekBar>()
            collectSeekBars(page, seeks)
            seeks.firstOrNull()?.let { seek ->
                val duration = PlaybackController.duration()
                seek.max = duration.coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(1000)
                seek.progress = PlaybackController.position().coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                if (seek.tag != "spotifusion-progress-bound") {
                    seek.tag = "spotifusion-progress-bound"
                    seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                            if (fromUser) PlaybackController.seek(value.toLong())
                        }
                        override fun onStartTrackingTouch(bar: SeekBar?) = Unit
                        override fun onStopTrackingTouch(bar: SeekBar?) = Unit
                    })
                }
            }
            val actions = column.getChildAt(6) as? ViewGroup
            actions?.let {
                (it.getChildAt(0) as? TextView)?.setOnClickListener {
                    PlaybackController.setShuffle(!PlaybackController.isShuffleEnabled())
                }
                (it.getChildAt(1) as? TextView)?.setOnClickListener { PlaybackController.previous() }
                (it.getChildAt(2) as? TextView)?.let { button ->
                    button.text = if (PlaybackController.isPlaying()) "Ⅱ" else "▶"
                    button.setOnClickListener { PlaybackController.toggle() }
                }
                (it.getChildAt(3) as? TextView)?.setOnClickListener { PlaybackController.next() }
                (it.getChildAt(4) as? TextView)?.setOnClickListener {
                    PlaybackController.setRepeat(!PlaybackController.isRepeatEnabled())
                }
            }
        }
    }

    private fun findTextView(view: View, text: String): TextView? {
        if (view is TextView && view.text?.toString() == text) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findTextView(view.getChildAt(i), text)
                if (found != null) return found
            }
        }
        return null
    }

    private fun collectSeekBars(view: View, result: MutableList<SeekBar>) {
        if (view is SeekBar) result += view
        if (view is ViewGroup) for (i in 0 until view.childCount) collectSeekBars(view.getChildAt(i), result)
    }

    override fun onDestroy() {
        PlaybackController.disconnect()
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
