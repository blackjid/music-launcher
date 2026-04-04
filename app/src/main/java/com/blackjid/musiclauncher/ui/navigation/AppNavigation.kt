package com.blackjid.musiclauncher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.blackjid.musiclauncher.ui.screens.HomeScreen

object Routes {
    const val HOME = "home"
    const val NOW_PLAYING = "now_playing"
    const val STANDBY = "standby"
    const val PROFILE_SELECT = "profile_select"
    const val TIMER_EXPIRED = "timer_expired"
    const val SETTINGS = "settings"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen()
        }
    }
}
