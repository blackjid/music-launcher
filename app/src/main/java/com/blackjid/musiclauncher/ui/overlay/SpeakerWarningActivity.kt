package com.blackjid.musiclauncher.ui.overlay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackjid.musiclauncher.MusicLauncherApp
import com.blackjid.musiclauncher.ui.theme.BgSecondary
import com.blackjid.musiclauncher.ui.theme.FgMuted
import com.blackjid.musiclauncher.ui.theme.FgPrimary
import com.blackjid.musiclauncher.ui.theme.FgSecondary
import com.blackjid.musiclauncher.ui.theme.MusicLauncherTheme

private const val SPOTIFY_PACKAGE = "com.spotify.music"

class SpeakerWarningActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val speakerMonitor = (applicationContext as MusicLauncherApp).speakerMonitor

        setContent {
            MusicLauncherTheme {
                WarningContent(
                    onCastToSpeaker = {
                        speakerMonitor.onDismiss()
                        finish()
                        packageManager.getLaunchIntentForPackage(SPOTIFY_PACKAGE)
                            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ?.let { startActivity(it) }
                    },
                    onPlayHereAnyway = {
                        speakerMonitor.onPlayHereAnyway()
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
private fun WarningContent(onCastToSpeaker: () -> Unit, onPlayHereAnyway: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 48.dp)
                .widthIn(max = 420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(BgSecondary)
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Playing on phone speakers",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = FgPrimary
            )
            Text(
                text = "Please cast to a speaker in the house.",
                fontSize = 14.sp,
                color = FgSecondary
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFD97706))
                    .clickable { onCastToSpeaker() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cast to a speaker",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
            TextButton(onClick = onPlayHereAnyway) {
                Text("Keep playing here", color = FgMuted, fontSize = 13.sp)
            }
        }
    }
}
