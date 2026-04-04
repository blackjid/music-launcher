package com.blackjid.musiclauncher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackjid.musiclauncher.R
import com.blackjid.musiclauncher.profile.ProfileRepository
import com.blackjid.musiclauncher.spotify.PlaybackPoller
import com.blackjid.musiclauncher.spotify.SpotifyManager
import com.blackjid.musiclauncher.ui.components.AccountPlaybackCard
import com.blackjid.musiclauncher.ui.theme.SpotifyGreen

@Composable
fun HomeScreen(
    spotifyManager: SpotifyManager,
    profileRepository: ProfileRepository,
    playbackPoller: PlaybackPoller,
    onConnectSpotify: () -> Unit,
    onNavigateToNowPlaying: () -> Unit,
    onNavigateToProfiles: () -> Unit
) {
    val isConnected by spotifyManager.isConnected.collectAsState()
    val activeProfile by profileRepository.activeProfile.collectAsState()
    val profiles by profileRepository.profiles.collectAsState()
    val allPlayback by playbackPoller.allPlaybackStates.collectAsState()
    val error by spotifyManager.error.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Profile button (top-left)
        IconButton(
            onClick = onNavigateToProfiles,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            if (activeProfile != null) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(activeProfile!!.avatarColor),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = activeProfile!!.name.take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_person),
                    contentDescription = "Profiles",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        if (profiles.isEmpty()) {
            // No accounts connected — show connect button
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = onConnectSpotify,
                    modifier = Modifier.size(160.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                ) {
                    Text(
                        text = "Connect\nSpotify",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.background,
                        lineHeight = 28.sp
                    )
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else if (allPlayback.isEmpty()) {
            // Accounts connected but nobody is playing
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "No music playing",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${profiles.size} account${if (profiles.size > 1) "s" else ""} connected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            // Show all accounts' now playing
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 72.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(allPlayback, key = { it.profileId }) { state ->
                    AccountPlaybackCard(state = state)
                }
            }
        }

        // Greeting
        if (profiles.isNotEmpty() && allPlayback.isEmpty()) {
            val greeting = activeProfile?.let { "Hi, ${it.name}" } ?: "Music Launcher"
            Text(
                text = greeting,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            )
        }
    }
}
