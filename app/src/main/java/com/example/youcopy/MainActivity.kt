package com.example.youcopy

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.rgb(15, 15, 15))
        }

        root.addView(TextView(this).apply {
            text = "YouCopy"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            setPadding(8, 8, 8, 12)
        })

        val categories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Todos", "Filmes", "Séries", "TV ao Vivo", "18+").forEach { label ->
            val button = Button(this).apply { text = label }
            categories.addView(button, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            button.setOnClickListener { filterCatalog(label) }
        }
        root.addView(categories)

        status = TextView(this).apply {
            text = "Carregando catálogo online..."
            textSize = 16f
            setTextColor(Color.LTGRAY)
            setPadding(8, 12, 8, 12)
        }
        root.addView(status)

        catalogList = ListView(this).apply {
            divider = null
            setBackgroundColor(Color.TRANSPARENT)
        }
        root.addView(catalogList, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        loadCatalog()
    }

    private fun loadCatalog() {
        Thread {
            try {
                val jsonText = downloadCatalog(catalogUrl)
                items = JSONArray(jsonText)
                runOnUiThread { filterCatalog("Todos") }
            } catch (e: Exception) {
                val message = e.message ?: "erro desconhecido"
                runOnUiThread {
                    status.text = "Erro ao carregar catálogo: " + message
                    Toast.makeText(this, "Não foi possível carregar os filmes. Verifique a internet.", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun downloadCatalog(url: String): String {
        val connection = URL(url + "?v=" + System.currentTimeMillis()).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        connection.setRequestProperty("User-Agent", "YouCopy-Android/1.0")
        connection.setRequestProperty("Accept", "application/json")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw Exception("HTTP " + code)
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun matches(item: org.json.JSONObject, category: String): Boolean {
        val type = item.optString("type", "Filme")
        return category == "Todos" ||
            (category == "Filmes" && type == "Filme") ||
            (category == "Séries" && type == "Série") ||
            (category == "TV ao Vivo" && type == "TV") ||
            (category == "18+" && type == "18+")
    }

    private fun filterCatalog(category: String) {
        if (!::items.isInitialized) return

        val filtered = ArrayList<org.json.JSONObject>()
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            if (matches(item, category)) filtered.add(item)
        }

        status.text = category + " • " + filtered.size + " item(ns)"
        catalogList.adapter = CatalogAdapter(filtered)

        catalogList.setOnItemClickListener { _, _, position, _ ->
            val item = filtered[position]
            val url = item.optString("videoUrl", "")
            if (category == "18+") {
                AlertDialog.Builder(this)
                    .setTitle("Conteúdo 18+")
                    .setMessage("Esta área é destinada exclusivamente a maiores de 18 anos.")
                    .setNegativeButton("Voltar", null)
                    .setPositiveButton("Continuar") { _, _ -> openContent(url) }
                    .show()
            } else {
                openContent(url)
            }
        }
    }

    private inner class CatalogAdapter(private val data: List<org.json.JSONObject>) : BaseAdapter() {
        override fun getCount() = data.size
        override fun getItem(position: Int) = data[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = data[position]
            val row = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(8, 8, 8, 8)
                setBackgroundColor(Color.rgb(25, 25, 25))
            }

            val poster = ImageView(this@MainActivity).apply {
                layoutParams = LinearLayout.LayoutParams(105, 155)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageResource(android.R.drawable.ic_menu_gallery)
                setBackgroundColor(Color.rgb(45, 45, 45))
            }

            val textBox = LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14, 8, 8, 8)
                layoutParams = LinearLayout.LayoutParams(0, -1, 1f)
            }

            val title = TextView(this@MainActivity).apply {
                text = item.optString("title", "Sem título")
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }

            val type = TextView(this@MainActivity).apply {
                text = item.optString("type", "Filme")
                textSize = 14f
                setTextColor(Color.LTGRAY)
                setPadding(0, 8, 0, 0)
            }

            textBox.addView(title)
            textBox.addView(type)
            row.addView(poster)
            row.addView(textBox)

            val posterUrl = item.optString("posterUrl", "")
            if (posterUrl.isNotBlank()) {
                poster.tag = posterUrl
                Thread {
                    try {
                        val bitmap = URL(posterUrl).openStream().use { BitmapFactory.decodeStream(it) }
                        if (bitmap != null) {
                            runOnUiThread {
                                if (poster.tag == posterUrl) poster.setImageBitmap(bitmap)
                            }
                        }
                    } catch (_: Exception) {
                    }
                }.start()
            }

            return row
        }
    }

    private fun openContent(url: String) {
        if (url.isBlank()) {
            Toast.makeText(this, "Este item ainda não possui link.", Toast.LENGTH_SHORT).show()
            return
        }
        val lower = url.lowercase()
        val isDirectVideo = lower.contains(".mp4") || lower.contains(".m3u8") ||
            lower.contains(".mkv") || lower.contains(".mov") || lower.contains(".webm") || lower.contains("manifest") || lower.contains("playlist")

        if (!isDirectVideo) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {
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
