package com.blackjid.musiclauncher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.blackjid.musiclauncher.kiosk.KioskManager
import com.blackjid.musiclauncher.spotify.SpotifyTokenManager
import com.blackjid.musiclauncher.spotify.SpotifyUserApi
import com.blackjid.musiclauncher.ui.navigation.AppNavigation
import com.blackjid.musiclauncher.ui.theme.MusicLauncherTheme
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val SPOTIFY_AUTH_REQUEST_CODE = 1337
        private const val REDIRECT_URI = "com.blackjid.musiclauncher://callback"
    }

    private lateinit var kioskManager: KioskManager
    private var codeVerifier: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        kioskManager = KioskManager(this)
        kioskManager.lockDown()

        setContent {
            MusicLauncherTheme {
                AppNavigation(
                    onRequestSpotifyAuth = { requestSpotifyAuth() }
                )
            }
        }
    }

    private fun requestSpotifyAuth() {
        // Generate PKCE verifier/challenge
        val verifier = SpotifyTokenManager.generateCodeVerifier()
        val challenge = SpotifyTokenManager.generateCodeChallenge(verifier)
        codeVerifier = verifier

        val request = AuthorizationRequest.Builder(
            BuildConfig.SPOTIFY_CLIENT_ID,
            AuthorizationResponse.Type.CODE,
            REDIRECT_URI
        )
            .setScopes(arrayOf(
                "user-read-private",
                "user-read-playback-state",
                "user-read-currently-playing"
            ))
            .setCustomParam("code_challenge_method", "S256")
            .setCustomParam("code_challenge", challenge)
            .build()

        AuthorizationClient.openLoginActivity(this, SPOTIFY_AUTH_REQUEST_CODE, request)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPOTIFY_AUTH_REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, data)
            val app = applicationContext as MusicLauncherApp
            val verifier = codeVerifier

            when (response.type) {
                AuthorizationResponse.Type.CODE -> {
                    if (verifier == null) {
                        Log.e(TAG, "No code verifier stored — cannot exchange code")
                        return
                    }
                    Log.d(TAG, "Got auth code, exchanging for tokens via PKCE")
                    lifecycleScope.launch {
                        val tokens = SpotifyTokenManager.exchangeCode(
                            code = response.code,
                            codeVerifier = verifier,
                            clientId = BuildConfig.SPOTIFY_CLIENT_ID,
                            clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET,
                            redirectUri = REDIRECT_URI
                        )
                        if (tokens != null) {
                            val user = SpotifyUserApi.getCurrentUser(tokens.accessToken)
                            if (user != null) {
                                Log.d(TAG, "Added account: ${user.displayName}")
                                app.profileRepository.upsertFromSpotify(
                                    spotifyUserId = user.id,
                                    displayName = user.displayName,
                                    tokens = tokens
                                )
                            }
                        } else {
                            Log.e(TAG, "Token exchange failed")
                        }
                        codeVerifier = null
                    }
                }
                AuthorizationResponse.Type.ERROR -> {
                    Log.e(TAG, "Spotify auth error: ${response.error}")
                    codeVerifier = null
                }
                else -> {
                    Log.w(TAG, "Spotify auth cancelled")
                    codeVerifier = null
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            kioskManager.hideSystemBars()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Intentionally empty — launcher has nowhere to go back to
    }
}
