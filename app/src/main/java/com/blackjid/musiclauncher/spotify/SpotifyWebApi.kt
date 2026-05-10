package com.blackjid.musiclauncher.spotify

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class SpotifyDevice(
    val id: String,
    val name: String,
    val type: String,
    val isActive: Boolean,
    val volumePercent: Int
)

data class WebPlaybackState(
    val profileId: String,
    val profileName: String,
    val profileColor: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val albumArtUrl: String? = null,
    val albumArtBitmap: Bitmap? = null,
    val isPlaying: Boolean,
    val deviceName: String,
    val deviceType: String = "",
    val positionMs: Long = 0,
    val durationMs: Long = 0
)

object SpotifyWebApi {
    private const val TAG = "SpotifyWebApi"

    suspend fun play(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        sendCommand("https://api.spotify.com/v1/me/player/play", "PUT", accessToken)
    }

    suspend fun pause(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        sendCommand("https://api.spotify.com/v1/me/player/pause", "PUT", accessToken)
    }

    suspend fun skipNext(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        sendCommand("https://api.spotify.com/v1/me/player/next", "POST", accessToken)
    }

    suspend fun skipPrevious(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        sendCommand("https://api.spotify.com/v1/me/player/previous", "POST", accessToken)
    }

    private fun sendCommand(urlStr: String, method: String, accessToken: String): Boolean {
        return try {
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.requestMethod = method
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = false
            val code = conn.responseCode
            code in 200..204
        } catch (e: Exception) {
            Log.e(TAG, "Command $method $urlStr failed", e)
            false
        }
    }

    suspend fun getAvailableDevices(accessToken: String): List<SpotifyDevice> = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://api.spotify.com/v1/me/player/devices").openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            if (conn.responseCode != 200) return@withContext emptyList()
            val obj = JSONObject(conn.inputStream.bufferedReader().readText())
            val devices = obj.optJSONArray("devices") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until devices.length()) {
                    val d = devices.getJSONObject(i)
                    add(SpotifyDevice(
                        id = d.getString("id"),
                        name = d.getString("name"),
                        type = d.optString("type", "Unknown"),
                        isActive = d.optBoolean("is_active", false),
                        volumePercent = d.optInt("volume_percent", 0)
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching devices", e)
            emptyList()
        }
    }

    suspend fun transferPlayback(accessToken: String, deviceId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://api.spotify.com/v1/me/player").openConnection() as HttpURLConnection
            conn.requestMethod = "PUT"
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true
            val body = """{"device_ids":["$deviceId"],"play":true}"""
            conn.outputStream.use { it.write(body.toByteArray()) }
            conn.responseCode in 200..204
        } catch (e: Exception) {
            Log.e(TAG, "Error transferring playback", e)
            false
        }
    }

    suspend fun getCurrentPlayback(
        accessToken: String,
        profileId: String,
        profileName: String,
        profileColor: Long
    ): WebPlaybackState? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.spotify.com/v1/me/player")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            when (conn.responseCode) {
                200 -> {
                    val body = conn.inputStream.bufferedReader().readText()
                    parsePlaybackState(body, profileId, profileName, profileColor)
                }
                204 -> {
                    // No content — nothing playing
                    null
                }
                else -> {
                    Log.w(TAG, "Playback API returned ${conn.responseCode} for $profileName")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching playback for $profileName", e)
            null
        }
    }

    private fun parsePlaybackState(
        json: String,
        profileId: String,
        profileName: String,
        profileColor: Long
    ): WebPlaybackState? {
        val obj = JSONObject(json)
        val item = obj.optJSONObject("item") ?: return null
        val isPlaying = obj.optBoolean("is_playing", false)

        val artistNames = buildList {
            val artists = item.optJSONArray("artists")
            if (artists != null) {
                for (i in 0 until artists.length()) {
                    add(artists.getJSONObject(i).getString("name"))
                }
            }
        }

        val album = item.optJSONObject("album")
        val images = album?.optJSONArray("images")
        val albumArtUrl = if (images != null && images.length() > 0) {
            images.getJSONObject(0).getString("url")
        } else null

        val device = obj.optJSONObject("device")
        val deviceName = device?.optString("name", "") ?: ""
        val deviceType = device?.optString("type", "") ?: ""
        val positionMs = obj.optLong("progress_ms", 0)
        val durationMs = item.optLong("duration_ms", 0)

        return WebPlaybackState(
            profileId = profileId,
            profileName = profileName,
            profileColor = profileColor,
            trackName = item.getString("name"),
            artistName = artistNames.joinToString(", "),
            albumName = album?.optString("name", "") ?: "",
            albumArtUrl = albumArtUrl,
            isPlaying = isPlaying,
            deviceName = deviceName,
            deviceType = deviceType,
            positionMs = positionMs,
            durationMs = durationMs
        )
    }
}
