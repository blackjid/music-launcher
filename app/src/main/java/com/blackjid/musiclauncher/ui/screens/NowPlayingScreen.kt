package com.blackjid.musiclauncher.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackjid.musiclauncher.R
import com.blackjid.musiclauncher.data.LyricLine
import com.blackjid.musiclauncher.data.LyricsRepository
import com.blackjid.musiclauncher.data.SettingsStore
import com.blackjid.musiclauncher.profile.ProfileRepository
import com.blackjid.musiclauncher.spotify.SpeakerMonitor
import com.blackjid.musiclauncher.spotify.SpotifyManager
import com.blackjid.musiclauncher.ui.theme.AccentPurple
import com.blackjid.musiclauncher.ui.theme.BgPrimary
import com.blackjid.musiclauncher.ui.theme.BgSecondary
import com.blackjid.musiclauncher.ui.theme.BgTertiary
import com.blackjid.musiclauncher.ui.theme.FgMuted
import com.blackjid.musiclauncher.ui.theme.FgPrimary
import com.blackjid.musiclauncher.ui.theme.FgSecondary
import com.blackjid.musiclauncher.ui.theme.SpotifyGreen
import kotlinx.coroutines.delay

private const val SPOTIFY_PACKAGE = "com.spotify.music"

@Composable
fun NowPlayingScreen(
    profileRepository: ProfileRepository,
    spotifyManager: SpotifyManager,
    speakerMonitor: SpeakerMonitor,
    lyricsRepository: LyricsRepository,
    settingsStore: SettingsStore,
    onRequestSpotifyAuth: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val profiles by profileRepository.profiles.collectAsState()
    val profileId = profiles.firstOrNull()?.id

    val state by spotifyManager.playerState.collectAsState()
    val isConnected by spotifyManager.isConnected.collectAsState()
    val isOnPhoneSpeaker by speakerMonitor.isOnPhoneSpeaker.collectAsState()
    val context = LocalContext.current
    val hasTrack = isConnected && state.trackName.isNotEmpty()
    val albumArt = state.albumArt
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    LaunchedEffect(Unit) { spotifyManager.connect() }

    var displayPositionMs by remember { mutableStateOf(0L) }
    LaunchedEffect(state.positionMs, state.isPlaying) {
        displayPositionMs = state.positionMs
        if (state.isPlaying) {
            while (true) {
                delay(1000)
                displayPositionMs += 1000
            }
        }
    }

    val albumScale = remember { Animatable(1f) }
    LaunchedEffect(state.trackName) {
        if (state.trackName.isNotEmpty()) {
            albumScale.animateTo(1.06f, tween(300, easing = EaseOut))
            albumScale.animateTo(1f, tween(500, easing = EaseIn))
        }
    }

    var showSettingsButton by remember { mutableStateOf(false) }
    var settingsTapCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(settingsTapCount) {
        if (settingsTapCount > 0) {
            delay(3000L)
            showSettingsButton = false
        }
    }

    var lyricsMode by remember { mutableIntStateOf(settingsStore.lyricsMode) }
    var lyrics by remember { mutableStateOf<List<LyricLine>>(emptyList()) }

    LaunchedEffect(state.trackName, state.artistName) {
        lyrics = if (state.trackName.isNotEmpty()) {
            lyricsRepository.getLyrics(
                state.trackName,
                state.artistName,
                state.albumName,
                state.durationMs / 1000
            )
        } else {
            emptyList()
        }
    }

    val currentLineIndex = lyrics.indexOfLast { it.timestampMs <= displayPositionMs }

    if (profileId == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(BgPrimary),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onRequestSpotifyAuth,
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
                Text(text = "Music Launcher", fontSize = 14.sp, color = FgMuted)
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    showSettingsButton = true
                    settingsTapCount++
                }
            }
    ) {
        // Layer 1: blurred album art background
        Crossfade(targetState = albumArt, animationSpec = tween(700), label = "bg") { art ->
            if (art != null && canBlur) {
                Image(
                    bitmap = art.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(32.dp, 32.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                )
            } else {
                Box(Modifier.fillMaxSize().background(BgPrimary))
            }
        }

        // Layer 2: dark scrim
        Box(Modifier.fillMaxSize().background(Color(0x99000000)))

        // Content area — full screen, album art vertically centered
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(44.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 24.dp)
                    .aspectRatio(1f)
                    .graphicsLayer {
                        scaleX = albumScale.value
                        scaleY = albumScale.value
                    }
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(16.dp),
                        clip = false,
                        ambientColor = AccentPurple.copy(alpha = 0.6f),
                        spotColor = AccentPurple.copy(alpha = 0.8f)
                    )
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Crossfade(targetState = albumArt, animationSpec = tween(500), label = "art") { art ->
                    if (art != null) {
                        Image(
                            bitmap = art.asImageBitmap(),
                            contentDescription = "Album art",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(BgSecondary))
                    }
                }
            }

            if (hasTrack) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = state.trackName,
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
                        text = state.artistName,
                        fontSize = 17.sp,
                        color = FgSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(BgTertiary)
                    ) {
                        val progress = if (state.durationMs > 0) {
                            (displayPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
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
                            text = formatMs(displayPositionMs),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = FgMuted
                        )
                        Text(
                            text = formatMs(state.durationMs),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = FgMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { spotifyManager.toggleShuffle() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_shuffle),
                                contentDescription = "Shuffle",
                                tint = if (state.isShuffling) AccentPurple else FgMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        IconButton(
                            onClick = { spotifyManager.skipPrevious() },
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_skip_previous),
                                contentDescription = "Previous",
                                tint = FgPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(AccentPurple)
                                .clickable { spotifyManager.togglePlayPause() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                ),
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = { spotifyManager.skipNext() },
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_skip_next),
                                contentDescription = "Next",
                                tint = FgPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        IconButton(
                            onClick = { spotifyManager.cycleRepeat() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (state.repeatMode == 2) R.drawable.ic_repeat_one
                                    else R.drawable.ic_repeat
                                ),
                                contentDescription = "Repeat",
                                tint = if (state.repeatMode > 0) AccentPurple else FgMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        if (lyrics.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(12.dp))

                            IconButton(
                                onClick = {
                                    lyricsMode = (lyricsMode + 1) % 3
                                    settingsStore.lyricsMode = lyricsMode
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lyrics),
                                    contentDescription = "Lyrics",
                                    tint = if (lyricsMode > 0) AccentPurple else FgMuted,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Single-line lyrics
                    if (lyricsMode == 1 && lyrics.isNotEmpty() && currentLineIndex >= 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AnimatedContent(
                            targetState = currentLineIndex,
                            transitionSpec = {
                                (fadeIn(tween(300)) + slideInVertically { it / 3 }) togetherWith
                                    (fadeOut(tween(200)) + slideOutVertically { -it / 3 })
                            },
                            label = "lyric-line"
                        ) { idx ->
                            Text(
                                text = lyrics[idx].text,
                                fontSize = 15.sp,
                                color = FgSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Nothing playing",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = FgPrimary,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Open Spotify to start listening",
                        fontSize = 15.sp,
                        color = FgMuted
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9999.dp))
                            .background(SpotifyGreen)
                            .clickable {
                                context.packageManager
                                    .getLaunchIntentForPackage(SPOTIFY_PACKAGE)
                                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    ?.let { context.startActivity(it) }
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_spotify),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Open Spotify",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                    }
                }
            }
        }

        // Spotify button — top right
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clip(RoundedCornerShape(9999.dp))
                .background(BgSecondary.copy(alpha = 0.9f))
                .clickable {
                    context.packageManager
                        .getLaunchIntentForPackage(SPOTIFY_PACKAGE)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ?.let { context.startActivity(it) }
                }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_spotify),
                contentDescription = "Open Spotify",
                tint = SpotifyGreen,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Spotify",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = FgSecondary
            )
        }

        // Speaker warning badge — top center
        if (isOnPhoneSpeaker) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFD97706))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Play on a speaker",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }

        // Tap-to-reveal settings button
        AnimatedVisibility(
            visible = showSettingsButton,
            modifier = Modifier.align(Alignment.BottomEnd),
            enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f),
            exit = fadeOut(tween(300)) + scaleOut(tween(300), targetScale = 0.8f)
        ) {
            IconButton(
                onClick = {
                    showSettingsButton = false
                    onNavigateToSettings()
                },
                modifier = Modifier
                    .padding(24.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(BgSecondary.copy(alpha = 0.9f))
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings",
                    tint = FgSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Full-screen lyrics overlay
        if (lyricsMode == 2 && lyrics.isNotEmpty()) {
            LyricsFullScreenOverlay(
                lyrics = lyrics,
                currentLineIndex = currentLineIndex,
                albumArt = albumArt,
                canBlur = canBlur,
                isPlaying = state.isPlaying,
                repeatMode = state.repeatMode,
                isShuffling = state.isShuffling,
                onTogglePlayPause = { spotifyManager.togglePlayPause() },
                onSkipPrevious = { spotifyManager.skipPrevious() },
                onSkipNext = { spotifyManager.skipNext() },
                onToggleLyrics = {
                    lyricsMode = (lyricsMode + 1) % 3
                    settingsStore.lyricsMode = lyricsMode
                }
            )
        }
    }
}

@Composable
private fun LyricsFullScreenOverlay(
    lyrics: List<LyricLine>,
    currentLineIndex: Int,
    albumArt: Bitmap?,
    canBlur: Boolean,
    isPlaying: Boolean,
    repeatMode: Int,
    isShuffling: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onToggleLyrics: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0) {
            listState.animateScrollToItem(
                index = maxOf(0, currentLineIndex - 2),
                scrollOffset = 0
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures {} }
    ) {
        // Blurred background
        if (albumArt != null && canBlur) {
            Image(
                bitmap = albumArt.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(48.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            )
        } else {
            Box(Modifier.fillMaxSize().background(BgPrimary))
        }

        // Darker scrim for readability
        Box(Modifier.fillMaxSize().background(Color(0xCC000000)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lyrics list
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(lyrics.size) { idx ->
                    val distance = idx - currentLineIndex
                    val (textSize, textColor, weight) = when {
                        distance == 0 -> Triple(20.sp, FgPrimary, FontWeight.Bold)
                        distance in -1..1 -> Triple(16.sp, FgSecondary, FontWeight.Normal)
                        else -> Triple(14.sp, FgMuted, FontWeight.Normal)
                    }
                    Text(
                        text = lyrics[idx].text,
                        fontSize = textSize,
                        fontWeight = weight,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Playback controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onSkipPrevious, modifier = Modifier.size(52.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_previous),
                        contentDescription = "Previous",
                        tint = FgPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AccentPurple)
                        .clickable { onTogglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                IconButton(onClick = onSkipNext, modifier = Modifier.size(52.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_skip_next),
                        contentDescription = "Next",
                        tint = FgPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                IconButton(
                    onClick = onToggleLyrics,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lyrics),
                        contentDescription = "Lyrics",
                        tint = AccentPurple,
                        modifier = Modifier.size(20.dp)
                    )
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
