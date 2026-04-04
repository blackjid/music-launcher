package com.blackjid.musiclauncher.spotify

import android.util.Base64
import android.util.Log
import com.blackjid.musiclauncher.data.StoredTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom

object SpotifyTokenManager {
    private const val TAG = "SpotifyTokenManager"
    private const val TOKEN_URL = "https://accounts.spotify.com/api/token"

    // PKCE helpers

    fun generateCodeVerifier(): String {
        val bytes = ByteArray(64)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray())
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    // Exchange authorization code for access + refresh tokens

    suspend fun exchangeCode(
        code: String,
        codeVerifier: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String
    ): StoredTokens? = withContext(Dispatchers.IO) {
        post(
            body = "grant_type=authorization_code" +
                "&code=${encode(code)}" +
                "&redirect_uri=${encode(redirectUri)}" +
                "&client_id=${encode(clientId)}" +
                "&client_secret=${encode(clientSecret)}" +
                "&code_verifier=${encode(codeVerifier)}"
        )
    }

    // Use refresh token to get a new access token

    suspend fun refreshToken(
        refreshToken: String,
        clientId: String,
        clientSecret: String
    ): StoredTokens? = withContext(Dispatchers.IO) {
        post(
            body = "grant_type=refresh_token" +
                "&refresh_token=${encode(refreshToken)}" +
                "&client_id=${encode(clientId)}" +
                "&client_secret=${encode(clientSecret)}"
        )
    }

    private fun post(body: String): StoredTokens? {
        return try {
            val conn = URL(TOKEN_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000

            conn.outputStream.use { it.write(body.toByteArray()) }

            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                val accessToken = json.getString("access_token")
                // refresh_token may not be present on refresh responses if unchanged
                val newRefreshToken = json.optString("refresh_token", "")
                val expiresIn = json.getInt("expires_in")
                StoredTokens(
                    accessToken = accessToken,
                    refreshToken = newRefreshToken,
                    expiresAt = System.currentTimeMillis() + expiresIn * 1000L
                )
            } else {
                val error = conn.errorStream?.bufferedReader()?.readText() ?: ""
                Log.e(TAG, "Token exchange failed: ${conn.responseCode} — $error")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Token exchange error", e)
            null
        }
    }

    private fun encode(value: String) =
        java.net.URLEncoder.encode(value, "UTF-8")
}
