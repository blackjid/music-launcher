package com.blackjid.musiclauncher.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackjid.musiclauncher.R
import com.blackjid.musiclauncher.spotify.PlaybackPoller
import com.blackjid.musiclauncher.spotify.SpotifyDevice
import com.blackjid.musiclauncher.ui.theme.AccentPurple
import com.blackjid.musiclauncher.ui.theme.ActiveRowBg
import com.blackjid.musiclauncher.ui.theme.BgPrimary
import com.blackjid.musiclauncher.ui.theme.BgSecondary
import com.blackjid.musiclauncher.ui.theme.BgTertiary
import com.blackjid.musiclauncher.ui.theme.FgMuted
import com.blackjid.musiclauncher.ui.theme.FgPrimary
import com.blackjid.musiclauncher.ui.theme.FgSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun NowPlayingScreen(
    profileId: String,
    playbackPoller: PlaybackPoller,
    onBack: () -> Unit
) {
    val allPlayback by playbackPoller.allPlaybackStates.collectAsState()
    val playback = allPlayback.find { it.profileId == profileId }
    val scope = rememberCoroutineScope()

    var showDevicePicker by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<SpotifyDevice>?>(null) }

    var albumArt by remember(playback?.albumArtUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(playback?.albumArtUrl) {
        albumArt = playback?.albumArtUrl?.let { url ->
            withContext(Dispatchers.IO) {
                try {
                    val conn = URL(url).openConnection()
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    BitmapFactory.decodeStream(conn.getInputStream())
                } catch (_: Exception) { null }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(BgSecondary)
                    .clickable { onBack() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = FgSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Standby",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = FgSecondary
                )
            }

            Text(
                text = "Now Playing",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = FgMuted
            )

            if (!playback?.deviceName.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9999.dp))
                        .background(BgSecondary)
                        .clickable {
                            devices = null
                            showDevicePicker = true
                            scope.launch {
                                devices = playbackPoller.getAvailableDevices(profileId)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_spotify),
                        contentDescription = null,
                        tint = FgMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = playback!!.deviceName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = FgSecondary
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(80.dp))
            }
        }

        // Content area
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, end = 40.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(44.dp)
        ) {
            // Album art
            if (albumArt != null) {
                Image(
                    bitmap = albumArt!!.asImageBitmap(),
                    contentDescription = "Album art",
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(BgSecondary)
                )
            }

            // Controls panel
            if (playback != null) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    // Song title + artist
                    Text(
                        text = playback.trackName,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = FgPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 38.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playback.artistName,
                        fontSize = 17.sp,
                        color = FgSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(BgTertiary)
                    ) {
                        val progress = if (playback.durationMs > 0) {
                            (playback.positionMs.toFloat() / playback.durationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(AccentPurple)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatMs(playback.positionMs),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = FgMuted
                        )
                        Text(
                            text = formatMs(playback.durationMs),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = FgMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Playback controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { scope.launch { playbackPoller.skipPrevious(profileId) } },
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_skip_previous),
                                contentDescription = "Previous",
                                tint = FgPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(AccentPurple)
                                .clickable {
                                    scope.launch {
                                        playbackPoller.togglePlayPause(profileId, playback.isPlaying)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (playback.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                ),
                                contentDescription = if (playback.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        IconButton(
                            onClick = { scope.launch { playbackPoller.skipNext(profileId) } },
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_skip_next),
                                contentDescription = "Next",
                                tint = FgPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Not playing",
                        fontSize = 18.sp,
                        color = FgMuted
                    )
                }
            }
        }
    }

    if (showDevicePicker) {
        AlertDialog(
            onDismissRequest = { showDevicePicker = false },
            containerColor = BgSecondary,
            title = {
                Text(
                    text = "Play on device",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FgPrimary
                )
            },
            text = {
                if (devices == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentPurple, modifier = Modifier.size(32.dp))
                    }
                } else if (devices!!.isEmpty()) {
                    Text(text = "No devices found", color = FgMuted, fontSize = 14.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        devices!!.forEach { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (device.isActive) ActiveRowBg else BgTertiary)
                                    .clickable {
                                        showDevicePicker = false
                                        scope.launch {
                                            playbackPoller.transferPlayback(profileId, device.id)
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (device.isActive) FgPrimary else FgSecondary
                                    )
                                    Text(
                                        text = device.type,
                                        fontSize = 11.sp,
                                        color = FgMuted
                                    )
                                }
                                if (device.isActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(AccentPurple)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDevicePicker = false }) {
                    Text(text = "Cancel", color = FgMuted)
                }
            }
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
