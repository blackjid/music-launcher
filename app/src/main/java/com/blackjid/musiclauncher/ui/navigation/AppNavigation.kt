package com.blackjid.musiclauncher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.blackjid.musiclauncher.MusicLauncherApp
import com.blackjid.musiclauncher.ui.screens.HomeScreen
import com.blackjid.musiclauncher.ui.screens.NowPlayingScreen
import com.blackjid.musiclauncher.ui.screens.ProfileSelectScreen

object Routes {
    const val HOME = "home"
    const val NOW_PLAYING = "now_playing"
    const val STANDBY = "standby"
    const val PROFILE_SELECT = "profile_select"
    const val TIMER_EXPIRED = "timer_expired"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(onRequestSpotifyAuth: () -> Unit = {}) {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as MusicLauncherApp
    val spotifyManager = app.spotifyManager
    val profileRepository = app.profileRepository
    val playbackPoller = app.playbackPoller

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                spotifyManager = spotifyManager,
                profileRepository = profileRepository,
                playbackPoller = playbackPoller,
                onConnectSpotify = onRequestSpotifyAuth,
                onNavigateToNowPlaying = {
                    navController.navigate(Routes.NOW_PLAYING)
                },
                onNavigateToProfiles = {
                    navController.navigate(Routes.PROFILE_SELECT)
                }
            )
        }

        composable(Routes.NOW_PLAYING) {
            NowPlayingScreen(
                spotifyManager = spotifyManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PROFILE_SELECT) {
            ProfileSelectScreen(
                profileRepository = profileRepository,
                onProfileSelected = { profile ->
                    profileRepository.setActiveProfile(profile.id)
                    // Reconnect Spotify with the selected profile
                    spotifyManager.disconnect()
                    spotifyManager.connect()
                    navController.popBackStack()
                },
                onAddAccount = {
                    // Trigger Spotify auth flow — new account will be
                    // auto-created from the Spotify user info on callback
                    onRequestSpotifyAuth()
                    navController.popBackStack()
                }
            )
        }
    }
}
