package com.example.youcopy

import android.app.Activity
import android.os.Bundle
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : Activity() {
 private var player: ExoPlayer? = null
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val root=LinearLayout(this); root.orientation=LinearLayout.VERTICAL; root.setPadding(24,24,24,24)
  val title=TextView(this); title.text="YouCopy\nReprodução de vídeo"; title.textSize=24f; root.addView(title)
  val url=EditText(this); url.hint="URL do vídeo (MP4/HLS)"; url.setText("https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"); root.addView(url)
  val play=Button(this); play.text="Reproduzir"; root.addView(play)
  val pv=PlayerView(this); root.addView(pv, LinearLayout.LayoutParams(-1,0,1f))
  play.setOnClickListener { player?.release(); player=ExoPlayer.Builder(this).build(); pv.player=player; player!!.setMediaItem(MediaItem.fromUri(url.text.toString().trim())); player!!.prepare(); player!!.playWhenReady=true }
  setContentView(root)
 }
 override fun onDestroy(){ player?.release(); super.onDestroy() }
}
