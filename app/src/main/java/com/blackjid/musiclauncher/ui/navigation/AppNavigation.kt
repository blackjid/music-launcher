package com.blackjid.musiclauncher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.blackjid.musiclauncher.MusicLauncherApp
import com.blackjid.musiclauncher.ui.screens.HomeScreen
import com.blackjid.musiclauncher.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation(onRequestSpotifyAuth: () -> Unit = {}) {
    val navController = rememberNavController()
    val app = LocalContext.current.applicationContext as MusicLauncherApp

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                profileRepository = app.profileRepository,
                playbackPoller = app.playbackPoller,
                onConnectSpotify = onRequestSpotifyAuth,
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                profileRepository = app.profileRepository,
                onAddAccount = onRequestSpotifyAuth,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
