package com.blackjid.musiclauncher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.blackjid.musiclauncher.MusicLauncherApp
import com.blackjid.musiclauncher.ui.screens.HomeScreen
import com.blackjid.musiclauncher.ui.screens.NowPlayingScreen
import com.blackjid.musiclauncher.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val NOW_PLAYING = "now_playing/{profileId}"
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
                spotifyManager = app.spotifyManager,
                onConnectSpotify = onRequestSpotifyAuth,
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToNowPlaying = { profileId ->
                    navController.navigate("now_playing/$profileId")
                }
            )
        }
        composable(
            route = Routes.NOW_PLAYING,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: ""
            NowPlayingScreen(
                profileId = profileId,
                spotifyManager = app.spotifyManager,
                playbackPoller = app.playbackPoller,
                onBack = { navController.popBackStack() }
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
