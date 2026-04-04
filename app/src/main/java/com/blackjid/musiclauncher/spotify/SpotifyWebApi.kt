package com.blackjid.musiclauncher.spotify

import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

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
    val deviceName: String
)

object SpotifyWebApi {
    private const val TAG = "SpotifyWebApi"

    suspend fun getCurrentPlayback(
        accessToken: String,
        profileId: String,
        profileName: String,
        profileColor: Long
    ): WebPlaybackState? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.spotify.com/v1/me/player/currently-playing")
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

        return WebPlaybackState(
            profileId = profileId,
            profileName = profileName,
            profileColor = profileColor,
            trackName = item.getString("name"),
            artistName = artistNames.joinToString(", "),
            albumName = album?.optString("name", "") ?: "",
            albumArtUrl = albumArtUrl,
            isPlaying = isPlaying,
            deviceName = deviceName
        )
    }
}
