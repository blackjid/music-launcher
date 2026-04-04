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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackjid.musiclauncher.R
import com.blackjid.musiclauncher.profile.Profile
import com.blackjid.musiclauncher.profile.ProfileRepository
import com.blackjid.musiclauncher.spotify.PlaybackPoller
import com.blackjid.musiclauncher.spotify.WebPlaybackState
import com.blackjid.musiclauncher.ui.theme.SpotifyGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.random.Random

private const val SPOTIFY_PACKAGE = "com.spotify.music"
private val ClockFormatter = DateTimeFormatter.ofPattern("HH:mm")
private const val BURN_IN_SHIFT_INTERVAL_MS = 45_000L
private const val BURN_IN_MAX_OFFSET_DP = 8f

@Composable
fun HomeScreen(
    profileRepository: ProfileRepository,
    playbackPoller: PlaybackPoller,
    onConnectSpotify: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val profiles by profileRepository.profiles.collectAsState()
    val allPlayback by playbackPoller.allPlaybackStates.collectAsState()

    // Clock — updates at the next minute boundary
    var clockText by remember { mutableStateOf(LocalTime.now().format(ClockFormatter)) }
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalTime.now()
            val msUntilNext = now.until(
                now.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1),
                ChronoUnit.MILLIS
            )
            delay(msUntilNext)
            clockText = LocalTime.now().format(ClockFormatter)
        }
    }

    // OLED burn-in prevention
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
            .background(Color.Black)
    ) {
        if (profiles.isEmpty()) {
            // First-run: no accounts yet
            ConnectSpotifyContent(
                onConnectSpotify = onConnectSpotify,
                offsetX = offsetX.value,
                offsetY = offsetY.value
            )
        } else {
            val pagerState = rememberPagerState(pageCount = { profiles.size })
            val scope = rememberCoroutineScope()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(x = offsetX.value.dp, y = offsetY.value.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top bar: Spotify icon | Clock | Settings icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        context.packageManager.getLaunchIntentForPackage(SPOTIFY_PACKAGE)
                            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ?.let { context.startActivity(it) }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_spotify),
                            contentDescription = "Open Spotify",
                            tint = SpotifyGreen,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = clockText,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Thin,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )

                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "Settings",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Profile pills — tap to jump to that profile's page
                if (profiles.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        profiles.forEachIndexed { index, profile ->
                            val isSelected = pagerState.currentPage == index
                            val isPlaying = allPlayback.any { it.profileId == profile.id && it.isPlaying }
                            ProfilePill(
                                profile = profile,
                                isSelected = isSelected,
                                isPlaying = isPlaying,
                                onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                            )
                            if (index < profiles.lastIndex) Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Swipeable now-playing pages
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val profile = profiles[page]
                    val playback = allPlayback.find { it.profileId == profile.id }
                    ProfileNowPlayingPage(
                        profile = profile,
                        playback = playback,
                        poller = playbackPoller
                    )
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
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun ProfilePill(
    profile: Profile,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(profile.avatarColor) else Color(profile.avatarColor).copy(alpha = 0.25f)
    val textColor = if (isSelected) Color.Black else Color(profile.avatarColor)

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color.Black.copy(alpha = 0.5f) else Color(profile.avatarColor))
            )
        }
        Text(
            text = profile.name.split(" ").first(),
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

@Composable
private fun ProfileNowPlayingPage(
    profile: Profile,
    playback: WebPlaybackState?,
    poller: PlaybackPoller
) {
    val scope = rememberCoroutineScope()

    if (playback == null) {
        // Profile is not playing anything
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Not playing",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.3f)
            )
        }
        return
    }

    // Load album art
    var albumArt by remember(playback.albumArtUrl) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(playback.albumArtUrl) {
        albumArt = playback.albumArtUrl?.let { url ->
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

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Album art
        if (albumArt != null) {
            Image(
                bitmap = albumArt!!.asImageBitmap(),
                contentDescription = "Album art",
                modifier = Modifier
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(profile.avatarColor).copy(alpha = 0.25f))
            )
        }

        // Track info + controls
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = playback.trackName,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = playback.artistName,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (playback.deviceName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = playback.deviceName,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.3f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { scope.launch { poller.skipPrevious(profile.id) } },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = "Previous",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(
                    onClick = { scope.launch { poller.togglePlayPause(profile.id, playback.isPlaying) } },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (playback.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (playback.isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(
                    onClick = { scope.launch { poller.skipNext(profile.id) } },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = "Next",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
