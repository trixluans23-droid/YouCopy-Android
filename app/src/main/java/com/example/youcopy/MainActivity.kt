package com.example.youcopy

import android.app.Activity
import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    private var player: ExoPlayer? = null
    private lateinit var catalogList: ListView
    private lateinit var status: TextView

    private val catalogUrl = "https://raw.githubusercontent.com/trixluans23-droid/YouCopy-Android/main/catalog.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        root.addView(TextView(this).apply { text = "YouCopy"; textSize = 28f })
        status = TextView(this).apply { text = "Carregando catálogo online..."; textSize = 16f }
        root.addView(status)
        catalogList = ListView(this)
        root.addView(catalogList, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        loadCatalog()
    }

    private fun loadCatalog() {
        Thread {
            try {
                val connection = URL(catalogUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val items = JSONArray(jsonText)
                val titles = ArrayList<String>()
                val videoUrls = ArrayList<String>()
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val type = item.optString("type", "Filme")
                    val name = item.optString("title", "Sem título")
                    titles.add("$type • $name")
                    videoUrls.add(item.optString("videoUrl", ""))
                }

                runOnUiThread {
                    status.text = "Catálogo online • " + titles.size + " título(s)"
                    catalogList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, titles)
                    catalogList.setOnItemClickListener { _, _, position, _ -> playVideo(videoUrls[position]) }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Não foi possível carregar o catálogo online."
                    Toast.makeText(this, "Verifique sua internet e tente novamente.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun playVideo(videoUrl: String) {
        if (videoUrl.isBlank()) {
            Toast.makeText(this, "Este título ainda não possui vídeo.", Toast.LENGTH_SHORT).show()
            return
        }
        val dialog = Dialog(this)
        val playerView = PlayerView(this)
        dialog.setContentView(playerView)
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        player?.release()
        player = ExoPlayer.Builder(this).build()
        playerView.player = player
        player!!.setMediaItem(MediaItem.fromUri(videoUrl))
        player!!.prepare()
        player!!.playWhenReady = true
        dialog.setOnDismissListener { player?.release(); player = null }
    }

    override fun onDestroy() {
        player?.release()
        super.onDestroy()
    }
}
