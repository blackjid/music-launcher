package com.blackjid.musiclauncher.spotify

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class SpotifyUser(
    val id: String,
    val displayName: String
)

object SpotifyUserApi {
    private const val TAG = "SpotifyUserApi"

    suspend fun getCurrentUser(accessToken: String): SpotifyUser? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.spotify.com/v1/me")
            val conn = url.openConnection() as HttpURLConnection
            conn.setRequestProperty("Authorization", "Bearer $accessToken")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(body)
                SpotifyUser(
                    id = json.getString("id"),
                    displayName = json.optString("display_name", json.getString("id"))
                )
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.readText() ?: ""
                Log.e(TAG, "Failed to get user: ${conn.responseCode} — $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user", e)
            null
        }
    }
}
