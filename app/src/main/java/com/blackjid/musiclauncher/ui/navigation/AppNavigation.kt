package com.blackjid.musiclauncher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.blackjid.musiclauncher.MusicLauncherApp
import com.blackjid.musiclauncher.ui.screens.NowPlayingScreen
import com.blackjid.musiclauncher.ui.screens.SettingsScreen

object Routes {
    const val NOW_PLAYING = "now_playing"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(onRequestSpotifyAuth: () -> Unit = {}) {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as MusicLauncherApp

    NavHost(navController = navController, startDestination = Routes.NOW_PLAYING) {
        composable(Routes.NOW_PLAYING) {
            NowPlayingScreen(
                spotifyManager = app.spotifyManager,
                speakerMonitor = app.speakerMonitor,
                lyricsRepository = app.lyricsRepository,
                settingsStore = app.settingsStore,
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                profileRepository = app.profileRepository,
                settingsStore = app.settingsStore,
                onAddAccount = onRequestSpotifyAuth,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
