package com.blackjid.musiclauncher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.blackjid.musiclauncher.kiosk.KioskManager
import com.blackjid.musiclauncher.spotify.SpotifyManager
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
    }

    private lateinit var kioskManager: KioskManager

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

    fun requestSpotifyAuth() {
        val app = applicationContext as MusicLauncherApp
        val request = AuthorizationRequest.Builder(
            app.spotifyManager.clientId,
            AuthorizationResponse.Type.TOKEN,
            SpotifyManager.REDIRECT_URI
        )
            .setScopes(arrayOf(
                "app-remote-control",
                "streaming",
                "user-read-private",
                "user-read-playback-state",
                "user-read-currently-playing"
            ))
            .build()

        AuthorizationClient.openLoginActivity(this, SPOTIFY_AUTH_REQUEST_CODE, request)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == SPOTIFY_AUTH_REQUEST_CODE) {
            val response = AuthorizationClient.getResponse(resultCode, data)
            val app = applicationContext as MusicLauncherApp

            when (response.type) {
                AuthorizationResponse.Type.TOKEN -> {
                    val token = response.accessToken
                    Log.d(TAG, "Spotify auth success, fetching user info")

                    lifecycleScope.launch {
                        val user = SpotifyUserApi.getCurrentUser(token)
                        if (user != null) {
                            Log.d(TAG, "Logged in as: ${user.displayName} (${user.id})")
                            app.profileRepository.upsertFromSpotify(
                                spotifyUserId = user.id,
                                displayName = user.displayName,
                                token = token
                            )
                        }
                        // Connect App Remote after profile is set up
                        app.spotifyManager.connect()
                    }
                }
                AuthorizationResponse.Type.ERROR -> {
                    Log.e(TAG, "Spotify auth error: ${response.error}")
                }
                else -> {
                    Log.w(TAG, "Spotify auth cancelled or unknown response")
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
