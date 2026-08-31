package com.spotifusion.app

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

open class NeonActivity : AppCompatActivity() {
    private val backgroundColor = Color.rgb(246, 247, 252)
    private val surface = Color.WHITE
    private val ink = Color.rgb(24, 24, 30)
    private val muted = Color.rgb(111, 114, 128)
    private val blue = Color.rgb(45, 91, 239)
    private val indigo = Color.rgb(91, 73, 226)
    private val cyan = Color.rgb(60, 178, 235)
    private val pink = Color.rgb(222, 86, 171)
    private val orange = Color.rgb(245, 157, 72)
    private lateinit var page: FrameLayout
    private lateinit var miniTitle: TextView
    private lateinit var miniSubtitle: TextView
    private lateinit var miniPlay: TextView
    private lateinit var nav: LinearLayout
    private lateinit var headerTitle: TextView
    private var currentTab = 0
    private var playing = false
    private var shuffle = false
    private var repeat = false
    private var currentSong = "Glass Architecture"
    private var currentArtist = "Astral Pulse"

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun alpha(c: Int, a: Int) = Color.argb(a, Color.red(c), Color.green(c), Color.blue(c))
    private fun bg(c: Int, r: Int = 20, stroke: Int? = null) = GradientDrawable().apply { setColor(c); cornerRadius = dp(r).toFloat(); stroke?.let { setStroke(dp(1), it) } }
    private fun gradient(a: Int, b: Int, r: Int = 22) = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(a, b)).apply { cornerRadius = dp(r).toFloat() }
    private fun label(s: String, size: Float, color: Int = ink, bold: Boolean = false) = TextView(this).apply { text = s; textSize = size; setTextColor(color); includeFontPadding = false; if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD) }
    private fun tint(c: Int) = ColorStateList.valueOf(c)

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        buildShell()
        showHome()
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(backgroundColor); setPadding(dp(12), dp(8), dp(12), 0) }
        root.addView(topBar(), LinearLayout.LayoutParams(-1, dp(58)))
        page = FrameLayout(this)
        root.addView(page, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(miniPlayer(), LinearLayout.LayoutParams(-1, dp(72)).apply { setMargins(dp(2), dp(7), dp(2), dp(7)) })
        nav = bottomNavigation()
        root.addView(nav, LinearLayout.LayoutParams(-1, dp(72)))
        setContentView(root)
    }

    private fun topBar() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
        val brand = LinearLayout(this@NeonActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val logo = FrameLayout(this@NeonActivity).apply { background = gradient(blue, indigo, 13); addView(label("◉", 18f, Color.WHITE, true).apply { gravity = Gravity.CENTER }) }
        brand.addView(logo, LinearLayout.LayoutParams(dp(38), dp(38)))
        headerTitle = label("Spotifusion", 18f, ink, true).apply { setPadding(dp(11), 0, 0, 0) }
        brand.addView(headerTitle); addView(brand, LinearLayout.LayoutParams(0, -1, 1f))
        addView(label("⌕", 25f, ink, true).apply { gravity = Gravity.CENTER; contentDescription = "Search"; background = bg(alpha(surface,225),17,alpha(Color.WHITE,230)); setOnClickListener { showSearch() } }, LinearLayout.LayoutParams(dp(44),dp(44)).apply { rightMargin=dp(5) })
        addView(label("♧", 22f, ink).apply { gravity=Gravity.CENTER; contentDescription="Settings"; background=bg(alpha(surface,225),17,alpha(Color.WHITE,230)); setOnClickListener{showSettings()} }, LinearLayout.LayoutParams(dp(44),dp(44)))
    }

    private fun bottomNavigation() = LinearLayout(this).apply {
        orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER; setPadding(dp(5),dp(4),dp(5),dp(6)); background=bg(alpha(Color.WHITE,242),23,alpha(Color.WHITE,250)); elevation=dp(5).toFloat()
        val items=arrayOf("⌂" to "Home","⌕" to "Search","▣" to "Library","≋" to "Equalizer","⚙" to "Settings")
        items.forEachIndexed { i,p ->
            val cell=LinearLayout(this@NeonActivity).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; isClickable=true; isFocusable=true; setPadding(dp(2),dp(3),dp(2),dp(2)); if(i==currentTab) background=bg(alpha(blue,28),16); setOnClickListener { currentTab=i; when(i){0->showHome();1->showSearch();2->showLibrary();3->showEqualizer();4->showSettings()}; refreshNav() } }
            cell.addView(label(p.first,19f,if(i==currentTab)blue else muted,true).apply{gravity=Gravity.CENTER}); cell.addView(label(p.second,9f,if(i==currentTab)blue else muted,true).apply{gravity=Gravity.CENTER}); addView(cell,LinearLayout.LayoutParams(0,-1,1f).apply{setMargins(dp(2),0,dp(2),0)})
        }
    }
    private fun refreshNav(){ for(i in 0 until nav.childCount){val c=nav.getChildAt(i) as LinearLayout; val s=i==currentTab; c.background=if(s)bg(alpha(blue,28),16)else null;(c.getChildAt(0)as TextView).setTextColor(if(s)blue else muted);(c.getChildAt(1)as TextView).setTextColor(if(s)blue else muted)} }

    private fun miniPlayer() = LinearLayout(this).apply {
        orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(dp(8),dp(7),dp(7),dp(7)); background=bg(alpha(Color.WHITE,242),20,alpha(Color.WHITE,250)); elevation=dp(4).toFloat()
        addView(FrameLayout(this@NeonActivity).apply{background=gradient(indigo,cyan,14);addView(label("◉",22f,Color.WHITE,true).apply{gravity=Gravity.CENTER})},LinearLayout.LayoutParams(dp(50),dp(50)))
        val info=LinearLayout(this@NeonActivity).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(11),0,dp(4),0);setOnClickListener{showNowPlaying()}}
        miniTitle=label(currentSong,13.5f,ink,true);miniSubtitle=label(currentArtist,10.5f,muted);info.addView(miniTitle);info.addView(miniSubtitle.apply{setPadding(0,dp(4),0,0)});addView(info,LinearLayout.LayoutParams(0,-1,1f))
        addView(label("♡",22f,muted).apply{gravity=Gravity.CENTER;contentDescription="Favorite"},LinearLayout.LayoutParams(dp(38),dp(50)))
        miniPlay=label(if(playing)"Ⅱ" else "▶",18f,ink,true).apply{gravity=Gravity.CENTER;contentDescription="Play or pause";setOnClickListener{playing=!playing;updateMini()}};addView(miniPlay,LinearLayout.LayoutParams(dp(43),dp(50)))
        addView(label("≋",20f,ink,true).apply{gravity=Gravity.CENTER;contentDescription="Now playing";setOnClickListener{showNowPlaying()}},LinearLayout.LayoutParams(dp(40),dp(50)))
    }
    private fun updateMini(){miniTitle.text=currentSong;miniSubtitle.text=if(playing)"$currentArtist · Playing" else currentArtist;miniPlay.text=if(playing)"Ⅱ" else "▶"}
    private fun scrollPage(v:View)=ScrollView(this).apply{isFillViewport=true;overScrollMode=View.OVER_SCROLL_IF_CONTENT_SCROLLS;setBackgroundColor(backgroundColor);addView(v,ViewGroup.LayoutParams(-1,-1))}
    private fun column()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(8),dp(6),dp(8),dp(24))}
    private fun show(v:View){page.removeAllViews();page.addView(v,FrameLayout.LayoutParams(-1,-1))}
    private fun section(c:LinearLayout,t:String){val r=LinearLayout(this).apply{gravity=Gravity.CENTER_VERTICAL;setPadding(dp(2),dp(5),dp(2),dp(9))};r.addView(label(t,12f,ink,true),LinearLayout.LayoutParams(0,-2,1f));r.addView(label("See all",10.5f,blue,true));c.addView(r)}

    private fun showHome(){currentTab=0;headerTitle.text="Spotifusion";val c=column();c.addView(label("Good morning,",13f,muted));c.addView(label("Kruthagna 👋",26f,ink,true).apply{setPadding(0,dp(4),0,dp(14))})
        val hero=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(16),dp(16),dp(13),dp(16));background=gradient(Color.rgb(52,87,221),Color.rgb(106,73,222),23)}
        hero.addView(FrameLayout(this).apply{background=gradient(Color.rgb(99,146,255),Color.rgb(65,54,170),18);addView(label("◉",48f,Color.WHITE,true).apply{gravity=Gravity.CENTER})},LinearLayout.LayoutParams(dp(112),dp(112)))
        val info=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(14),0,0,0)};info.addView(label("FEATURED SESSION",9f,alpha(Color.WHITE,210),true));info.addView(label("Glass Architecture",18f,Color.WHITE,true).apply{setPadding(0,dp(7),0,0)});info.addView(label("Nordic Echoes",11f,alpha(Color.WHITE,215)).apply{setPadding(0,dp(4),0,dp(10))});info.addView(label("  ▶  PLAY NOW  ",10.5f,blue,true).apply{gravity=Gravity.CENTER;background=bg(Color.WHITE,15);setPadding(dp(5),dp(9),dp(5),dp(9));setOnClickListener{currentSong="Glass Architecture";currentArtist="Astral Pulse";playing=true;updateMini();showNowPlaying()}},LinearLayout.LayoutParams(-2,dp(38)));hero.addView(info,LinearLayout.LayoutParams(0,-2,1f));c.addView(hero,LinearLayout.LayoutParams(-1,dp(144)).apply{bottomMargin=dp(17)})
        section(c,"Recently Played");val hs=HorizontalScrollView(this).apply{isHorizontalScrollBarEnabled=false};val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};listOf("Glass Architecture" to "Astral Pulse","Astral Pulse" to "Lunar Drift","Midnight Drive" to "Neon Skyline","Ocean Bloom" to "Cobalt Waves").forEach{recentTrack(row,it.first,it.second)};hs.addView(row);c.addView(hs,LinearLayout.LayoutParams(-1,dp(151)).apply{bottomMargin=dp(12)});section(c,"Made For You");show(scrollPage(c));refreshNav()}
    private fun recentTrack(p:LinearLayout,t:String,a:String){val card=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(7),dp(7),dp(7),dp(4));setOnClickListener{selectTrack(t,a)}};card.addView(FrameLayout(this@NeonActivity).apply{background=gradient(blue,Color.rgb(215,224,255),16);addView(label("◉",28f,Color.WHITE,true).apply{gravity=Gravity.CENTER})},LinearLayout.LayoutParams(dp(102),dp(102)));card.addView(label(t,10.5f,ink,true).apply{maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(dp(2),dp(6),0,0)});card.addView(label(a,9f,muted));p.addView(card,LinearLayout.LayoutParams(dp(116),-1))}
    private fun selectTrack(t:String,a:String){currentSong=t;currentArtist=a;playing=true;updateMini()}

    private fun showSearch(){currentTab=1;headerTitle.text="Search";val c=column();c.addView(label("Find your music",26f,ink,true));val input=EditText(this).apply{hint="Songs, artists, albums...";singleLine=true;background=bg(Color.WHITE,18,alpha(blue,45));setPadding(dp(14),0,dp(14),0)};c.addView(input,LinearLayout.LayoutParams(-1,dp(52)).apply{topMargin=dp(14),bottomMargin=dp(14)});section(c,"Browse");arrayOf("Songs","Albums","Artists","Playlists").forEach{b->c.addView(label("  $b",14f,ink,true).apply{gravity=Gravity.CENTER_VERTICAL;background=bg(alpha(Color.WHITE,225),16);setPadding(dp(10),0,0,0);setOnClickListener{showHome()}},LinearLayout.LayoutParams(-1,dp(52)).apply{bottomMargin=dp(8)})};show(scrollPage(c));refreshNav()}
    private fun showLibrary(){currentTab=2;headerTitle.text="Your Library";val c=column();c.addView(label("Your Library",26f,ink,true));arrayOf("♥  Liked Songs","◉  Chill Vibes","◉  Night Drive","✦  Focus Flow","◉  Retro Wave").forEach{t->c.addView(label(t,13f,ink,true).apply{gravity=Gravity.CENTER_VERTICAL;background=bg(Color.WHITE,18);setPadding(dp(14),0,0,0);setOnClickListener{showNowPlaying()}},LinearLayout.LayoutParams(-1,dp(64)).apply{bottomMargin=dp(8)})};show(scrollPage(c));refreshNav()}
    private fun showEqualizer(){currentTab=3;headerTitle.text="Equalizer";val c=column();c.addView(label("Equalizer",26f,ink,true));c.addView(label("Fine-tune your sound",11.5f,muted).apply{setPadding(0,dp(4),0,dp(15))});val panel=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER;background=bg(Color.WHITE,22);setPadding(dp(7),dp(18),dp(7),dp(15))};arrayOf("60Hz","230Hz","910Hz","3.6k","14k").forEach{f->val band=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER};band.addView(label("+12",8f,muted));band.addView(SeekBar(this).apply{max=100;progress=60;progressTintList=tint(blue);thumbTintList=tint(blue)},LinearLayout.LayoutParams(0,dp(150),1f));band.addView(label(f,9f,ink,true));panel.addView(band,LinearLayout.LayoutParams(0,dp(190),1f))};c.addView(panel,LinearLayout.LayoutParams(-1,dp(222)));show(scrollPage(c));refreshNav()}
    private fun showSettings(){currentTab=4;headerTitle.text="Settings";val c=column();c.addView(label("Settings",26f,ink,true));c.addView(label("Playback, appearance and audio",11.5f,muted).apply{setPadding(0,dp(4),0,dp(15))});arrayOf("Playback Quality  ·  High","Crossfade  ·  5 sec","Gapless Playback  ·  ON","Normalize Volume  ·  ON","Theme  ·  Light","Accent Color  ·  Blue","Equalizer  ·  Custom","Sleep Timer  ·  Off","Shake to change song  ·  ON").forEach{t->c.addView(label(t,12f,ink,true).apply{gravity=Gravity.CENTER_VERTICAL;background=bg(Color.WHITE,16);setPadding(dp(13),0,0,0)},LinearLayout.LayoutParams(-1,dp(58)).apply{bottomMargin=dp(7)})};show(scrollPage(c));refreshNav()}
    private fun showNowPlaying(){headerTitle.text="Now Playing";val c=column().apply{gravity=Gravity.CENTER_HORIZONTAL};c.addView(label(currentSong,22f,ink,true).apply{gravity=Gravity.CENTER});c.addView(label("$currentArtist · Nordic Echoes",12f,muted).apply{gravity=Gravity.CENTER;setPadding(0,dp(6),0,dp(18))});val bar=SeekBar(this).apply{max=100;progress=42;progressTintList=tint(blue);thumbTintList=tint(blue)};c.addView(bar,LinearLayout.LayoutParams(-1,dp(36)));val actions=LinearLayout(this).apply{gravity=Gravity.CENTER};actions.addView(label(if(shuffle)"🔀" else "⇄",20f,muted).apply{setOnClickListener{shuffle=!shuffle}});actions.addView(label("|◀",21f,ink,true),LinearLayout.LayoutParams(dp(60),dp(60)));actions.addView(label(if(playing)"Ⅱ" else "▶",24f,Color.WHITE,true).apply{gravity=Gravity.CENTER;background=bg(blue,20);setOnClickListener{playing=!playing;updateMini();text=if(playing)"Ⅱ" else "▶"}},LinearLayout.LayoutParams(dp(64),dp(58)));actions.addView(label("▶|",21f,ink,true),LinearLayout.LayoutParams(dp(60),dp(60)));actions.addView(label("↻",21f,muted,true).apply{setOnClickListener{repeat=!repeat}});c.addView(actions);show(scrollPage(c))}
}
