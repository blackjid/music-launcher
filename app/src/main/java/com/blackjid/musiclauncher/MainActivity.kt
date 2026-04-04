package com.blackjid.musiclauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.blackjid.musiclauncher.kiosk.KioskManager
import com.blackjid.musiclauncher.ui.navigation.AppNavigation
import com.blackjid.musiclauncher.ui.theme.MusicLauncherTheme

class MainActivity : ComponentActivity() {

    private lateinit var kioskManager: KioskManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        kioskManager = KioskManager(this)
        kioskManager.lockDown()

        setContent {
            MusicLauncherTheme {
                AppNavigation()
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
