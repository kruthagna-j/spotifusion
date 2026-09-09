package com.spotifusion.app

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

class LyricsActivity : AppCompatActivity() {
    private val executor=Executors.newSingleThreadExecutor(); private val handler=Handler(Looper.getMainLooper())
    private lateinit var view:LinearLayout; private var lines:List<LyricsRepository.Line> = emptyList(); private var ticker:Runnable?=null
    private val bg=Color.rgb(6,6,10); private val white=Color.rgb(246,244,252); private val muted=Color.rgb(151,146,166); private val violet=Color.rgb(123,81,251)
    private fun dp(v:Int)=(v*resources.displayMetrics.density+.5f).toInt()
    override fun onCreate(s:Bundle?){super.onCreate(s);window.statusBarColor=bg;window.navigationBarColor=bg;view=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(18),dp(20),dp(30));setBackgroundColor(bg)};setContentView(android.widget.ScrollView(this).apply{addView(view)});load()}
    private fun load(){val title=PlaybackController.currentTitle();val artist=PlaybackController.currentArtist();val album=PlaybackController.currentAlbum();bar(title,artist);if(title.isBlank()){message("Play a song to view its lyrics.");return};message("Loading lyrics…");executor.execute{val r=runCatching{LyricsRepository.fetch(title,artist,album,(PlaybackController.duration()/1000).toInt())}.getOrNull();handler.post{view.removeAllViews();bar(title,artist);if(r==null||(r.lines.isEmpty()&&r.plain.isBlank())){message("Lyrics aren't available for this song yet.");return@post};lines=r.lines;if(lines.isNotEmpty()){lines.forEachIndexed{i,l->view.addView(TextView(this).apply{text=l.text.ifBlank{"♪"};textSize=19f;setTextColor(muted);gravity=Gravity.CENTER;setPadding(dp(4),dp(10),dp(4),dp(10));tag=i})};startSync()}else message(r.plain)}}}
    private fun bar(song:String,artist:String){view.addView(TextView(this).apply{text="Lyrics";textSize=27f;setTextColor(white);setTypeface(typeface,1)});view.addView(TextView(this).apply{text="$song · $artist";textSize=12f;setTextColor(muted);setPadding(0,dp(6),0,dp(22))})}
    private fun message(s:String){view.addView(TextView(this).apply{text=s;textSize=14f;setTextColor(muted);gravity=Gravity.CENTER;setPadding(dp(8),dp(40),dp(8),dp(40))})}
    private fun startSync(){ticker?.let(handler::removeCallbacks);ticker=object:Runnable{override fun run(){val p=PlaybackController.position();val active=lines.indexOfLast{it.timeMs<=p};for(i in 2 until view.childCount){val v=view.getChildAt(i) as TextView;val selected=(v.tag as? Int)==active;v.setTextColor(if(selected)white else muted);v.textSize=if(selected)22f else 19f;if(selected)v.setTypeface(v.typeface,1)};handler.postDelayed(this,200)}};handler.post(ticker!!)}
    override fun onDestroy(){ticker?.let(handler::removeCallbacks);executor.shutdownNow();super.onDestroy()}
}