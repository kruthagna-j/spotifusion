package com.spotifusion.app

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

open class ReferenceActivity : AppCompatActivity() {
    private val green = Color.rgb(30, 215, 96)
    private val black = Color.rgb(7, 8, 8)
    private val card = Color.rgb(25, 26, 26)
    private val card2 = Color.rgb(32, 33, 33)
    private val white = Color.WHITE
    private val gray = Color.rgb(165, 165, 165)
    private val muted = Color.rgb(112, 112, 112)

    private data class Track(val id: Long, val title: String, val artist: String, val uri: Uri, val album: String = "")
    private val tracks = mutableListOf<Track>()
    private val favorites = mutableSetOf<Long>()
    private val prefs by lazy { getSharedPreferences("spotifusion", MODE_PRIVATE) }
    private var player: MediaPlayer? = null
    private var current: Track? = null
    private var currentIndex = -1
    private var shuffle = false
    private var repeat = false
    private var sleepMinutes = 0
    private val handler = Handler(Looper.getMainLooper())
    private var mini: LinearLayout? = null
    private var body: FrameLayout? = null
    private var nav: LinearLayout? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun bg(c: Int, r: Int) = android.graphics.drawable.GradientDrawable().apply { setColor(c); cornerRadius = dp(r).toFloat() }
    private fun text(s: String, size: Float, color: Int = white, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); includeFontPadding = false
        if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private fun icon(s: String, action: (() -> Unit)? = null) = text(s, 23f, white, false).apply {
        gravity = Gravity.CENTER
        if (action != null) setOnClickListener { action() }
    }
    private fun row(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun vspace(h: Int) = Space(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = black
        window.navigationBarColor = black
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        loadFavorites()
        requestAudio { loadLocalMusic(); buildShell(); showHome() }
    }

    private fun requestAudio(after: () -> Unit) {
        val p = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED) after() else {
            pending = after; requestPermissions(arrayOf(p), 90)
        }
    }
    private var pending: (() -> Unit)? = null
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 90) { pending?.invoke(); pending = null }
    }

    private fun loadFavorites() { prefs.getStringSet("favorites", emptySet())?.forEach { it.toLongOrNull()?.let(favorites::add) } }
    private fun saveFavorites() { prefs.edit().putStringSet("favorites", favorites.map(Long::toString).toSet()).apply() }

    private fun loadLocalMusic() {
        tracks.clear()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM)
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null, "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC")?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID); val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST); val album = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            while (c.moveToNext()) tracks += Track(c.getLong(id), c.getString(title).orEmpty().ifBlank { "Unknown song" }, c.getString(artist).orEmpty().ifBlank { "Unknown artist" }, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, c.getLong(id)), c.getString(album).orEmpty())
        }
    }

    private fun buildShell() {
        val root = FrameLayout(this).apply { setBackgroundColor(black) }
        body = FrameLayout(this).apply { setBackgroundColor(black) }
        root.addView(body, FrameLayout.LayoutParams(-1, -1))
        nav = bottomNav()
        val np = FrameLayout.LayoutParams(-1, dp(72), Gravity.BOTTOM)
        root.addView(nav, np)
        mini = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; visibility = View.GONE; setPadding(dp(12), dp(8), dp(8), dp(8)); background = bg(Color.rgb(20,21,21), 18) }
        root.addView(mini, FrameLayout.LayoutParams(-1, dp(64), Gravity.BOTTOM).apply { setMargins(dp(10),0,dp(10),dp(80)) })
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(bottom = bars.bottom)
            insets
        }
        setContentView(root)
    }

    private fun bottomNav() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setBackgroundColor(Color.rgb(10,11,11)); setPadding(dp(6),0,dp(6),0)
        listOf("⌂" to "Home", "⌕" to "Search", "▣" to "Library", "⚙" to "Settings").forEach { (ico, label) ->
            val b = LinearLayout(this@ReferenceActivity).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isClickable = true }
            b.addView(text(ico, 22f, muted).apply { gravity = Gravity.CENTER })
            b.addView(text(label, 11f, muted).apply { gravity = Gravity.CENTER; setPadding(0,dp(3),0,0) })
            b.setOnClickListener { when(label) { "Home" -> showHome(); "Search" -> showSearch(); "Library" -> showLibrary(); else -> showSettings() } }
            addView(b, LinearLayout.LayoutParams(0,-1,1f))
        }
    }
    private fun setActiveNav(index: Int) {
        nav?.let { for (i in 0 until it.childCount) { val b=it.getChildAt(i) as LinearLayout; val on=i==index; (b.getChildAt(0) as TextView).setTextColor(if(on) green else muted); (b.getChildAt(1) as TextView).setTextColor(if(on) green else muted) } }
    }

    private fun screen(): ScrollView = ScrollView(this).apply { isVerticalScrollBarEnabled = false; clipToPadding = false }
    private fun column(): LinearLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18),dp(18),dp(18),dp(100)) }
    private fun titleBar(title: String, back: Boolean=false, action: (() -> Unit)?=null): LinearLayout {
        val r=row(); if(back) r.addView(icon("‹") { showHome() }, LinearLayout.LayoutParams(dp(42),dp(48)))
        r.addView(text(title,25f,white,true), LinearLayout.LayoutParams(0,dp(48),1f))
        if(action!=null) r.addView(icon("＋",action), LinearLayout.LayoutParams(dp(48),dp(48)))
        return r
    }
    private fun section(c: LinearLayout, s: String, more: Boolean=false) {
        val r=row(); r.addView(text(s,18f,white,true),LinearLayout.LayoutParams(0,dp(38),1f)); if(more) r.addView(text("View all",12f,gray)); c.addView(r)
    }
    private fun pill(s:String, selected:Boolean=false, action:(()->Unit)?=null) = text(s,13f,if(selected) Color.BLACK else white,selected).apply { gravity=Gravity.CENTER; background=bg(if(selected) green else card2,22); setPadding(dp(15),dp(9),dp(15),dp(9)); if(action!=null)setOnClickListener{action()} }

    private fun showHome() {
        setActiveNav(0); mini?.visibility = if(current!=null) View.VISIBLE else View.GONE
        val c=column(); val h=row()
        h.addView(text("Spotifusion",24f,white,true),LinearLayout.LayoutParams(0,dp(48),1f)); h.addView(icon("♧") { toast("Notifications") },LinearLayout.LayoutParams(dp(48),dp(48))); h.addView(icon("⚙") { showSettings() },LinearLayout.LayoutParams(dp(48),dp(48))); c.addView(h)
        c.addView(text("Good evening",28f,white,true).apply{setPadding(0,dp(18),0,dp(3))}); c.addView(text("Your music, your way.",14f,gray)); c.addView(vspace(18))
        section(c,"Quick access",true)
        val hs=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false}; val q=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        quick(q,"♫","All Songs",tracks.size.toString()){ showLibrary() }; quick(q,"♡","Favorites",favorites.size.toString()){ showLibrary() }; quick(q,"↻","Recently Played","50"){ showQueue() }; hs.addView(q); c.addView(hs,LinearLayout.LayoutParams(-1,dp(86))); c.addView(vspace(18))
        section(c,"Recently played",true); c.addView(horizontalTracks(if(tracks.isEmpty()) emptyList() else tracks.take(8),true)); c.addView(vspace(18))
        section(c,"Your local music")
        val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; val show=if(tracks.isEmpty()) emptyList() else tracks.take(40); if(show.isEmpty()) list.addView(text("No local music found. Tap Settings → Rescan music.",14f,gray).apply{setPadding(dp(10),dp(16),0,dp(16))}) else show.forEach{ list.addView(trackRow(it)) }; c.addView(list)
        body?.removeAllViews(); body?.addView(screen().apply{addView(c)})
    }

    private fun quick(q:LinearLayout, ico:String, name:String, sub:String, action:()->Unit){ val b=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(10),dp(16),dp(10));background=bg(card,14);setOnClickListener{action()}}; val box=text(ico,23f,green).apply{gravity=Gravity.CENTER;background=bg(card2,12)}; b.addView(box,LinearLayout.LayoutParams(dp(52),dp(56))); val t=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),0,0,0)};t.addView(text(name,14f,white,true));t.addView(text(sub+if(name=="All Songs")" songs" else " songs",11f,gray).apply{setPadding(0,dp(4),0,0)});b.addView(t,LinearLayout.LayoutParams(dp(145),-1));q.addView(b,LinearLayout.LayoutParams(dp(205),dp(76)).apply{rightMargin=dp(10)}) }

    private fun horizontalTracks(ts:List<Track>,art:Boolean):HorizontalScrollView { val hs=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false};val l=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};ts.forEach{t->val b=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;isClickable=true;setOnClickListener{play(t)}};val im=artwork(t,dp(112));b.addView(im,LinearLayout.LayoutParams(dp(112),dp(112)));b.addView(text(t.title,12f,white,true).apply{maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(0,dp(7),0,0)});b.addView(text(t.artist,10f,gray).apply{maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(0,dp(3),0,0)});l.addView(b,LinearLayout.LayoutParams(dp(112),dp(155)).apply{rightMargin=dp(12)})};hs.addView(l);return hs }

    private fun artwork(t:Track,size:Int)=ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_CROP;background=bg(card2,12);setImageDrawable(android.graphics.drawable.ColorDrawable(card2));post{Thread{try{val r=MediaMetadataRetriever();r.setDataSource(this@ReferenceActivity,t.uri);val bytes=r.embeddedPicture;r.release();if(bytes!=null){val bm=BitmapFactory.decodeByteArray(bytes,0,bytes.size);post{setImageBitmap(bm)}}}catch(_:Exception){}}.start()}}

    private fun trackRow(t:Track)=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(5),dp(6),dp(5));background=bg(Color.rgb(17,18,18),12);setOnClickListener{play(t)};addView(artwork(t,dp(58)),LinearLayout.LayoutParams(dp(58),dp(58)));val m=LinearLayout(this@ReferenceActivity).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),0,dp(8),0)};m.addView(text(t.title,14f,white,true).apply{maxLines=2;ellipsize=android.text.TextUtils.TruncateAt.END});m.addView(text(t.artist,11f,gray).apply{setPadding(0,dp(5),0,0);maxLines=1});addView(m,LinearLayout.LayoutParams(0,dp(68),1f));addView(text(if(favorites.contains(t.id))"♥" else "♡",24f,if(favorites.contains(t.id))green:gray).apply{gravity=Gravity.CENTER;setOnClickListener{toggleFavorite(t)}},LinearLayout.LayoutParams(dp(40),dp(58)));addView(text("⋮",22f,gray).apply{gravity=Gravity.CENTER;setOnClickListener{songMenu(t)}},LinearLayout.LayoutParams(dp(30),dp(58))) }

    private fun toggleFavorite(t:Track){if(favorites.contains(t.id))favorites.remove(t.id)else favorites.add(t.id);saveFavorites();showHome()}
    private fun songMenu(t:Track){AlertDialog.Builder(this).setItems(arrayOf(if(favorites.contains(t.id))"Remove from favorites" else "Add to favorites","Play next","Add to queue")){_,which->when(which){0->toggleFavorite(t);1->play(t);2->toast("Added to queue")}}.show()}

    private fun showSearch(){setActiveNav(1);val c=column();c.addView(titleBar("Search"));val input=EditText(this).apply{hint="Songs, artists, albums, playlists...";hintTextColor=gray;setTextColor(white);textSize=14f;singleLine=true;setPadding(dp(16),0,dp(16),0);background=bg(card,22)};c.addView(input,LinearLayout.LayoutParams(-1,dp(48)));c.addView(vspace(14));val chips=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};arrayOf("Songs","Albums","Artists","Playlists").forEachIndexed{i,s->chips.addView(pill(s,i==0),LinearLayout.LayoutParams(-2,dp(38)).apply{rightMargin=dp(7)})};c.addView(chips);c.addView(vspace(18));section(c,"Recent searches");arrayOf("Blinding Lights","On My Way","Kesariya").forEach{q->c.addView(text("⌕  $q",14f,white).apply{setPadding(dp(8),dp(12),0,dp(12))})};c.addView(vspace(10));section(c,"Popular searches");c.addView(horizontalArtists());c.addView(vspace(18));section(c,"Results")
        val result=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};c.addView(result);fun refresh(q:String){result.removeAllViews();val filtered=tracks.filter{it.title.contains(q,true)||it.artist.contains(q,true)||it.album.contains(q,true)}.take(80);filtered.forEach{result.addView(trackRow(it))};if(filtered.isEmpty())result.addView(text("No local matches",14f,gray).apply{setPadding(dp(8),dp(18),0,0)})};input.addTextChangedListener(object:android.text.TextWatcher{override fun beforeTextChanged(s:CharSequence?,st:Int,c:Int,a:Int){};override fun onTextChanged(s:CharSequence?,st:Int,b:Int,c2:Int){refresh(s?.toString().orEmpty())};override fun afterTextChanged(e:android.text.Editable?){}});refresh("");body?.removeAllViews();body?.addView(screen().apply{addView(c)})}
    private fun horizontalArtists()=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false;val l=LinearLayout(this@ReferenceActivity).apply{orientation=LinearLayout.HORIZONTAL};listOf("Arijit Singh","Alan Walker","AP Dhillon","Taylor Swift").forEach{l.addView(pill(it),LinearLayout.LayoutParams(-2,dp(42)).apply{rightMargin=dp(8)})};addView(l)}

    private fun showLibrary(){setActiveNav(2);val c=column();c.addView(titleBar("Your Library",false){toast("Create playlist")});val tabs=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};arrayOf("Playlists","Songs","Albums","Artists").forEachIndexed{i,s->tabs.addView(pill(s,i==0),LinearLayout.LayoutParams(-2,dp(40)).apply{rightMargin=dp(7)})};c.addView(tabs);c.addView(vspace(12));val create=row().apply{setPadding(dp(12),dp(10),dp(12),dp(10));background=bg(card,12);addView(text("＋",28f,green),LinearLayout.LayoutParams(dp(52),dp(52)));addView(text("Create new playlist",14f,white,true).apply{setPadding(dp(12),0,0,0)})};c.addView(create,LinearLayout.LayoutParams(-1,dp(72)));c.addView(vspace(12));playlist(c,"♥","Liked Songs",favorites.size.toString()+" songs");playlist(c,"♫","My Playlist","24 songs");playlist(c,"▣","Workout Mix","50 songs");playlist(c,"◈","Chill Vibes","30 songs");playlist(c,"◉","Road Trip","42 songs");playlist(c,"◆","Party Hits","120 songs");body?.removeAllViews();body?.addView(screen().apply{addView(c)})}
    private fun playlist(c:LinearLayout,ico:String,name:String,count:String){val r=row().apply{setPadding(dp(8),dp(7),0,dp(7));setOnClickListener{if(name=="Liked Songs")showFavorites() else toast(name)}};r.addView(text(ico,24f,green).apply{gravity=Gravity.CENTER;background=bg(card2,10)},LinearLayout.LayoutParams(dp(58),dp(58)));val m=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),0,0,0)};m.addView(text(name,14f,white,true));m.addView(text(count,11f,gray).apply{setPadding(0,dp(5),0,0)});r.addView(m,LinearLayout.LayoutParams(0,dp(72),1f));r.addView(text("⋮",22f,gray),LinearLayout.LayoutParams(dp(38),dp(60)));c.addView(r)}
    private fun showFavorites(){val c=column();c.addView(titleBar("Liked Songs",true));favorites.mapNotNull{id->tracks.find{it.id==id}}.forEach{c.addView(trackRow(it))};body?.removeAllViews();body?.addView(screen().apply{addView(c)})}

    private fun showSettings(){setActiveNav(3);val c=column();c.addView(titleBar("Settings"));c.addView(text("Playback",18f,white,true).apply{setPadding(0,dp(12),0,dp(8))});setting(c,"Shuffle","${if(shuffle)"On" else "Off"}"){shuffle=!shuffle;showSettings()};setting(c,"Repeat","${if(repeat)"On" else "Off"}"){repeat=!repeat;showSettings()};setting(c,"Equalizer","Normal"){showEqualizer()};setting(c,"Sleep timer",if(sleepMinutes==0)"Off" else "$sleepMinutes min"){sleepDialog()};c.addView(text("Appearance",18f,white,true).apply{setPadding(0,dp(22),0,dp(8))});setting(c,"Theme","Dark"){toast("Dark theme is active")};setting(c,"Accent color","Green"){toast("Green accent is active")};c.addView(text("Library",18f,white,true).apply{setPadding(0,dp(22),0,dp(8))});setting(c,"Show local music","On"){toast("Local music is visible")};setting(c,"Rescan local music",""){loadLocalMusic();toast("Found ${tracks.size} songs");showHome()};c.addView(text("Other",18f,white,true).apply{setPadding(0,dp(22),0,dp(8))});setting(c,"Reset onboarding",""){prefs.edit().putBoolean("onboarding_done",false).apply();toast("Onboarding reset")};setting(c,"About Spotifusion","1.0"){toast("Spotifusion • Your music, your way")};body?.removeAllViews();body?.addView(screen().apply{addView(c)})}
    private fun setting(c:LinearLayout,name:String,value:String,action:()->Unit){val r=row().apply{setPadding(dp(14),dp(12),dp(10),dp(12));background=bg(card,12);setOnClickListener{action()}};val m=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};m.addView(text(name,14f,white));if(value.isNotEmpty())m.addView(text(value,11f,gray).apply{setPadding(0,dp(4),0,0)});r.addView(m,LinearLayout.LayoutParams(0,dp(60),1f));r.addView(text("›",24f,gray),LinearLayout.LayoutParams(dp(28),dp(60)));c.addView(r,LinearLayout.LayoutParams(-1,dp(68)).apply{bottomMargin=dp(7)})}

    private fun showEqualizer(){val c=column();c.addView(titleBar("Equalizer",true));c.addView(text("Normal",14f,black,true).apply{gravity=Gravity.CENTER;background=bg(green,20);setPadding(dp(16),dp(9),dp(16),dp(9))},LinearLayout.LayoutParams(-2,dp(40)));c.addView(vspace(18));val names=arrayOf("60Hz","230Hz","910Hz","3.6kHz","14kHz");val bars=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER};names.forEachIndexed{i,n->val col=LinearLayout(this@ReferenceActivity).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL};val seek=SeekBar(this@ReferenceActivity).apply{max=24;progress=intArrayOf(12,14,13,10,8)[i];rotation=-90};col.addView(seek,LinearLayout.LayoutParams(dp(120),dp(46)));col.addView(text(n,10f,gray).apply{gravity=Gravity.CENTER});bars.addView(col,LinearLayout.LayoutParams(dp(55),dp(190)))};c.addView(bars);setting(c,"Bass boost","On"){toast("Bass boost toggled")};setting(c,"Preset","Normal"){toast("Presets coming next")};body?.removeAllViews();body?.addView(screen().apply{addView(c)})}

    private fun showQueue(){val c=column();c.addView(titleBar("Up next",true));c.addView(text("Now playing",13f,gray,true).apply{setPadding(0,dp(10),0,dp(10))});current?.let{c.addView(trackRow(it))};c.addView(text("Next in queue",13f,gray,true).apply{setPadding(0,dp(18),0,dp(10))});tracks.filter{it.id!=current?.id}.take(20).forEach{c.addView(trackRow(it))};body?.removeAllViews();body?.addView(screen().apply{addView(c)})}

    private fun play(t:Track){current=t;currentIndex=tracks.indexOfFirst{it.id==t.id};player?.release();player=MediaPlayer().apply{setAudioStreamType(android.media.AudioManager.STREAM_MUSIC);setDataSource(this@ReferenceActivity,t.uri);prepareAsync();setOnPreparedListener{start();updateMini()};setOnCompletionListener{if(repeat)play(t)else if(shuffle&&!tracks.isEmpty())play(tracks.random())else if(currentIndex+1<tracks.size)play(tracks[currentIndex+1])}};updateMini()}
    private fun updateMini(){mini?.let{it.visibility=if(current!=null)View.VISIBLE else View.GONE;it.removeAllViews();val t=current?:return;it.addView(artwork(t,dp(46)),LinearLayout.LayoutParams(dp(46),dp(46)));val m=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(10),0,0,0)};m.addView(text(t.title,13f,white,true).apply{maxLines=1});m.addView(text(t.artist,10f,gray).apply{maxLines=1});it.addView(m,LinearLayout.LayoutParams(0,dp(48),1f));it.addView(icon(if(player?.isPlaying==true)"Ⅱ" else "▶"){if(player?.isPlaying==true)player?.pause()else player?.start();updateMini()},LinearLayout.LayoutParams(dp(48),dp(48)));it.addView(icon("→"){showNowPlaying()},LinearLayout.LayoutParams(dp(42),dp(48)))}
    private fun showNowPlaying(){val t=current?:return;val c=column();c.addView(titleBar("Playing now",true));c.gravity=Gravity.CENTER_HORIZONTAL;c.addView(artwork(t,dp(285)),LinearLayout.LayoutParams(dp(285),dp(285)).apply{topMargin=dp(12)});c.addView(text(t.title,23f,white,true).apply{setPadding(0,dp(20),0,dp(3))});c.addView(text(t.artist,14f,gray));val seek=SeekBar(this).apply{max=100;progress=0};c.addView(seek,LinearLayout.LayoutParams(-1,dp(45)).apply{topMargin=dp(16)});val controls=row().apply{gravity=Gravity.CENTER};controls.addView(icon("⤨"){shuffle=!shuffle},LinearLayout.LayoutParams(dp(50),dp(60)));controls.addView(icon("|‹"){previous()},LinearLayout.LayoutParams(dp(50),dp(60)));controls.addView(text(if(player?.isPlaying==true)"Ⅱ" else "▶",27f,Color.BLACK,true).apply{gravity=Gravity.CENTER;background=bg(green,40);setOnClickListener{if(player?.isPlaying==true)player?.pause()else player?.start();showNowPlaying()}},LinearLayout.LayoutParams(dp(64),dp(64)));controls.addView(icon("›|"),LinearLayout.LayoutParams(dp(50),dp(60)).apply{setOnClickListener{next()}});controls.addView(icon("↻"){repeat=!repeat},LinearLayout.LayoutParams(dp(50),dp(60)));c.addView(controls);c.addView(text("Lyrics",13f,white).apply{gravity=Gravity.CENTER;background=bg(card2,20);setPadding(dp(20),dp(10),dp(20),dp(10));setOnClickListener{toast("Lyrics panel ready for API connection")}});c.addView(vspace(12));c.addView(text("≡  Up next",15f,white,true).apply{setOnClickListener{showQueue()}});body?.removeAllViews();body?.addView(screen().apply{addView(c)})}
    private fun previous(){if(currentIndex>0)play(tracks[currentIndex-1])}
    private fun next(){if(tracks.isEmpty())return;if(shuffle)play(tracks.random())else if(currentIndex+1<tracks.size)play(tracks[currentIndex+1])}

    private fun sleepDialog(){val vals=arrayOf("Off","15 minutes","30 minutes","45 minutes","60 minutes");AlertDialog.Builder(this).setTitle("Sleep timer").setItems(vals){_,i->sleepMinutes=if(i==0)0 else intArrayOf(15,30,45,60)[i-1];sleepMinutes.takeIf{it>0}?.let{handler.postDelayed({player?.pause()},it*60_000L)};showSettings()}.show()}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
