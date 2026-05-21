package com.blackjid.musiclauncher.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LyricLine(val timestampMs: Long, val text: String)

class LyricsRepository {
    private val tag = "LyricsRepository"

    private var cacheKey: String? = null
    private var cachedLyrics: List<LyricLine> = emptyList()

    suspend fun getLyrics(
        trackName: String,
        artistName: String,
        albumName: String,
        durationSec: Long
    ): List<LyricLine> = withContext(Dispatchers.IO) {
        val key = "$trackName|$artistName|$durationSec"
        if (key == cacheKey) return@withContext cachedLyrics
        val lyrics = fetch(trackName, artistName, albumName, durationSec)
        cacheKey = key
        cachedLyrics = lyrics
        lyrics
    }

    private fun fetch(
        trackName: String,
        artistName: String,
        albumName: String,
        durationSec: Long
    ): List<LyricLine> {
        return try {
            fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
            val url = URL(
                "https://lrclib.net/api/get" +
                    "?track_name=${enc(trackName)}" +
                    "&artist_name=${enc(artistName)}" +
                    "&album_name=${enc(albumName)}" +
                    "&duration=$durationSec"
            )
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Lrclib-Client", "MusicLauncher/1.0")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode != 200) return emptyList()
            val body = conn.inputStream.bufferedReader().readText()
            val synced = JSONObject(body).optString("syncedLyrics").takeIf { it.isNotBlank() }
                ?: return emptyList()
            parseLrc(synced)
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch lyrics", e)
            emptyList()
        }
    }

    private fun parseLrc(lrc: String): List<LyricLine> {
        val regex = Regex("""\[(\d+):(\d+)\.(\d+)]\s*(.*)""")
        return lrc.lines().mapNotNull { line ->
            val m = regex.matchEntire(line.trim()) ?: return@mapNotNull null
            val (min, sec, frac, text) = m.destructured
            if (text.isBlank()) return@mapNotNull null
            val fracMs = when (frac.length) {
                1 -> frac.toLong() * 100
                2 -> frac.toLong() * 10
                else -> frac.take(3).toLong()
            }
            LyricLine(min.toLong() * 60_000 + sec.toLong() * 1_000 + fracMs, text)
        }
    }
}
