package com.example.youcopy

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.net.Uri
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
    private lateinit var items: JSONArray
    private val catalogUrl = "https://raw.githubusercontent.com/trixluans23-droid/YouCopy-Android/main/catalog.json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24) }
        root.addView(TextView(this).apply { text = "YouCopy"; textSize = 28f })

        val categories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Todos", "Filmes", "Séries", "TV ao Vivo", "18+").forEach { label ->
            val button = Button(this).apply { text = label }
            categories.addView(button, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            button.setOnClickListener { filterCatalog(label) }
        }
        root.addView(categories)

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
                val connection = URL(catalogUrl + "?v=" + System.currentTimeMillis()).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"
                val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                items = JSONArray(jsonText)
                runOnUiThread { filterCatalog("Todos") }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "Não foi possível carregar o catálogo online."
                    Toast.makeText(this, "Verifique sua internet e tente novamente.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun filterCatalog(category: String) {
        if (!::items.isInitialized) return
        val titles = ArrayList<String>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            val type = item.optString("type", "Filme")
            val matches = category == "Todos" ||
                (category == "Filmes" && type == "Filme") ||
                (category == "Séries" && type == "Série") ||
                (category == "TV ao Vivo" && type == "TV") ||
                (category == "18+" && type == "18+")
            if (matches) titles.add("$type • " + item.optString("title", "Sem título"))
        }
        status.text = category + " • " + titles.size + " item(ns)"
        catalogList.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, titles)
        catalogList.setOnItemClickListener { _, _, position, _ ->
            val filtered = ArrayList<String>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val type = item.optString("type", "Filme")
                val matches = category == "Todos" ||
                    (category == "Filmes" && type == "Filme") ||
                    (category == "Séries" && type == "Série") ||
                    (category == "TV ao Vivo" && type == "TV") ||
                    (category == "18+" && type == "18+")
                if (matches) filtered.add(item.optString("videoUrl", ""))
            }
            if (category == "18+") {
                AlertDialog.Builder(this)
                    .setTitle("Conteúdo 18+")
                    .setMessage("Esta área é destinada exclusivamente a maiores de 18 anos.")
                    .setNegativeButton("Voltar", null)
                    .setPositiveButton("Continuar") { _, _ -> openContent(filtered[position]) }
                    .show()
            } else {
                openContent(filtered[position])
            }
        }
    }

    private fun openContent(url: String) {
        if (url.isBlank()) {
            Toast.makeText(this, "Este item ainda não possui link.", Toast.LENGTH_SHORT).show()
            return
        }
        val lower = url.lowercase()
        val isDirectVideo = lower.contains(".mp4") || lower.contains(".m3u8") ||
            lower.contains(".mkv") || lower.contains(".mov") || lower.contains(".webm")
        if (!isDirectVideo) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Toast.makeText(this, "Não foi possível abrir este conteúdo.", Toast.LENGTH_SHORT).show()
            }
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
        player!!.setMediaItem(MediaItem.fromUri(url))
        player!!.prepare()
        player!!.playWhenReady = true
        dialog.setOnDismissListener { player?.release(); player = null }
    }

    override fun onDestroy() {
        player?.release()
        super.onDestroy()
    }
}
