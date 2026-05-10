package com.blackjid.musiclauncher.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackjid.musiclauncher.R
import com.blackjid.musiclauncher.profile.Profile
import com.blackjid.musiclauncher.profile.ProfileRepository
import com.blackjid.musiclauncher.spotify.PlaybackPoller
import com.blackjid.musiclauncher.spotify.SpotifyManager
import com.blackjid.musiclauncher.spotify.WebPlaybackState
import com.blackjid.musiclauncher.ui.theme.AccentPurple
import com.blackjid.musiclauncher.ui.theme.ActiveRowBg
import com.blackjid.musiclauncher.ui.theme.BgPrimary
import com.blackjid.musiclauncher.ui.theme.BgSecondary
import com.blackjid.musiclauncher.ui.theme.BgTertiary
import com.blackjid.musiclauncher.ui.theme.FgMuted
import com.blackjid.musiclauncher.ui.theme.FgPrimary
import com.blackjid.musiclauncher.ui.theme.FgSecondary
import com.blackjid.musiclauncher.ui.theme.SpotifyGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.random.Random

private const val SPOTIFY_PACKAGE = "com.spotify.music"
private val ClockFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val DateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d")
private const val BURN_IN_SHIFT_INTERVAL_MS = 45_000L
private const val BURN_IN_MAX_OFFSET_DP = 8f

@Composable
fun HomeScreen(
    profileRepository: ProfileRepository,
    playbackPoller: PlaybackPoller,
    spotifyManager: SpotifyManager,
    onConnectSpotify: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNowPlaying: (String) -> Unit
) {
    val context = LocalContext.current
    val profiles by profileRepository.profiles.collectAsState()
    val allPlayback by playbackPoller.allPlaybackStates.collectAsState()
    val appRemoteState by spotifyManager.playerState.collectAsState()

    var clockText by remember { mutableStateOf(LocalTime.now().format(ClockFormatter)) }
    var dateText by remember { mutableStateOf(LocalDate.now().format(DateFormatter)) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalTime.now()
            val msUntilNext = now.until(
                now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1),
                ChronoUnit.MILLIS
            )
            delay(msUntilNext)
            clockText = LocalTime.now().format(ClockFormatter)
            dateText = LocalDate.now().format(DateFormatter)
        }
    }

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(BURN_IN_SHIFT_INTERVAL_MS)
            val tx = Random.nextFloat() * BURN_IN_MAX_OFFSET_DP * 2 - BURN_IN_MAX_OFFSET_DP
            val ty = Random.nextFloat() * BURN_IN_MAX_OFFSET_DP * 2 - BURN_IN_MAX_OFFSET_DP
            offsetX.animateTo(tx, tween(3_000))
            offsetY.animateTo(ty, tween(3_000))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        if (profiles.isEmpty()) {
            ConnectSpotifyContent(
                onConnectSpotify = onConnectSpotify,
                offsetX = offsetX.value,
                offsetY = offsetY.value
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = offsetX.value.dp, y = offsetY.value.dp)
                    .padding(horizontal = 32.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top bar: clock (left) + date + settings (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = clockText,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = FgPrimary,
                        letterSpacing = (-2).sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = dateText,
                            fontSize = 14.sp,
                            color = FgMuted
                        )
                        IconButton(
                            onClick = {
                                context.packageManager.getLaunchIntentForPackage(SPOTIFY_PACKAGE)
                                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    ?.let { context.startActivity(it) }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_spotify),
                                contentDescription = "Open Spotify",
                                tint = SpotifyGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings),
                                contentDescription = "Settings",
                                tint = FgMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Profile rows
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    profiles.forEach { profile ->
                        val playback = allPlayback.find { it.profileId == profile.id }
                        val isLocalTrack = appRemoteState.trackName.isNotEmpty() &&
                            appRemoteState.trackName == playback?.trackName &&
                            appRemoteState.artistName == playback?.artistName
                        ProfileRow(
                            profile = profile,
                            playback = playback,
                            liveAlbumArt = if (isLocalTrack) appRemoteState.albumArt else null,
                            liveIsPlaying = if (isLocalTrack) appRemoteState.isPlaying else null,
                            livePositionMs = if (isLocalTrack) appRemoteState.positionMs else null,
                            onClick = { onNavigateToNowPlaying(profile.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectSpotifyContent(
    onConnectSpotify: () -> Unit,
    offsetX: Float,
    offsetY: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .offset(x = offsetX.dp, y = offsetY.dp),
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                lineHeight = 26.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Music Launcher",
            fontSize = 14.sp,
            color = FgMuted
        )
    }
}

@Composable
private fun ProfileRow(
    profile: Profile,
    playback: WebPlaybackState?,
    liveAlbumArt: Bitmap? = null,
    liveIsPlaying: Boolean? = null,
    livePositionMs: Long? = null,
    onClick: () -> Unit
) {
    val isPlaying = liveIsPlaying ?: (playback?.isPlaying == true)
    val rowBg = if (isPlaying) ActiveRowBg else BgSecondary

    val snapshotPositionMs = livePositionMs ?: (playback?.positionMs ?: 0L)
    var positionMs by remember { mutableStateOf(snapshotPositionMs) }
    LaunchedEffect(snapshotPositionMs, isPlaying) {
        positionMs = snapshotPositionMs
        if (isPlaying) {
            while (true) {
                delay(1000)
                positionMs += 1000
            }
        }
    }
    val rowAlpha = if (isPlaying || playback?.isPlaying == false) 1f else 0.5f

    // Fetch album art via HTTP only when App Remote hasn't provided it directly
    var httpAlbumArt by remember(playback?.albumArtUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(playback?.albumArtUrl, liveAlbumArt != null) {
        if (liveAlbumArt == null) {
            httpAlbumArt = playback?.albumArtUrl?.let { url ->
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
    }
    val albumArt = liveAlbumArt ?: httpAlbumArt

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .clip(RoundedCornerShape(16.dp))
            .background(rowBg)
            .clickable(enabled = playback != null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Album art thumbnail
        if (albumArt != null) {
            Image(
                bitmap = albumArt!!.asImageBitmap(),
                contentDescription = "Album art",
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(profile.avatarColor).copy(alpha = 0.4f))
            )
        }

        // Track info (middle)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(profile.avatarColor))
                )
                Text(
                    text = profile.name.split(" ").first(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(profile.avatarColor)
                )
            }
            Text(
                text = playback?.trackName ?: "Not playing",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (playback != null) FgPrimary else FgMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = playback?.artistName ?: "",
                fontSize = 12.sp,
                color = FgSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right side: device, time/status, progress bar
        if (playback != null) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (playback.deviceName.isNotEmpty()) {
                    Text(
                        text = playback.deviceName,
                        fontSize = 10.sp,
                        color = FgMuted
                    )
                }
                if (!isPlaying) {
                    Text(
                        text = "Paused",
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic,
                        color = FgMuted
                    )
                } else if (playback.durationMs > 0) {
                    Text(
                        text = "${formatMs(positionMs)} / ${formatMs(playback.durationMs)}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = FgMuted
                    )
                    Box(
                        modifier = Modifier
                            .width(96.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(BgTertiary)
                    ) {
                        val progress = (positionMs.toFloat() / playback.durationMs.toFloat())
                            .coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(Color(profile.avatarColor))
                        )
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
