package com.blackjid.musiclauncher.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.blackjid.musiclauncher.ui.theme.NunitoFontFamily
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
    val density = LocalDensity.current
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

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableStateOf(0f) }
    var barWidthPx by remember { mutableIntStateOf(0) }

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

    val lyricsEnabled = settingsStore.lyricsEnabled
    var isLyricsFullScreen by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
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
            ) {
                // Album art — scaled, shadowed, clipped
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = albumScale.value
                            scaleY = albumScale.value
                        }
                        .shadow(
                            elevation = 24.dp,
                            shape = RoundedCornerShape(16.dp),
                            clip = false,
                            ambientColor = Color.Black.copy(alpha = 0.6f),
                            spotColor = Color.Black.copy(alpha = 0.8f)
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

                // Lyrics overlay — outside the scale transform, clipped to cover shape
                if (lyricsEnabled && lyrics.isNotEmpty() && currentLineIndex >= 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        // Gradient scrim at bottom for text legibility
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(110.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xDD000000))
                                    )
                                )
                        )
                        // Current lyric line — sits above the icon button
                        AnimatedContent(
                            targetState = currentLineIndex,
                            transitionSpec = {
                                (fadeIn(tween(450, easing = EaseOut)) +
                                    slideInVertically(tween(450, easing = EaseOut)) { it }) togetherWith
                                (fadeOut(tween(300, easing = EaseIn)) +
                                    slideOutVertically(tween(300, easing = EaseIn)) { -it })
                            },
                            label = "lyric-cover",
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 52.dp)
                        ) { idx ->
                            Text(
                                text = lyrics[idx].text,
                                fontFamily = NunitoFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = FgPrimary,
                                maxLines = 2,
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    shadow = Shadow(
                                        color = Color.Black,
                                        offset = Offset.Zero,
                                        blurRadius = 24f
                                    )
                                )
                            )
                        }
                        // Expand to full-screen button — bottom-right corner
                        IconButton(
                            onClick = { isLyricsFullScreen = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(BgSecondary.copy(alpha = 0.75f))
                                .size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lyrics),
                                contentDescription = "Full screen lyrics",
                                tint = FgPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
                    // Section 1: Info
                    Text(
                        text = state.trackName,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = FgPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.5).sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.artistName,
                        fontSize = 15.sp,
                        color = FgSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Section 2: Main controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { spotifyManager.skipPrevious() },
                            modifier = Modifier.size(60.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_skip_previous),
                                contentDescription = "Previous",
                                tint = FgPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = { spotifyManager.togglePlayPause() },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                                ),
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = FgPrimary,
                                modifier = Modifier.size(48.dp)
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
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section 3: Playhead
                    val progress = if (state.durationMs > 0) {
                        (displayPositionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    val displayFraction = if (isScrubbing) scrubFraction else progress

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = formatMs(if (isScrubbing) (scrubFraction * state.durationMs).toLong() else displayPositionMs),
                            fontSize = 13.sp,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = FgPrimary,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .onSizeChanged { barWidthPx = it.width }
                                .pointerInput(state.durationMs) {
                                    awaitEachGesture {
                                        val down = awaitPointerEvent().changes.firstOrNull() ?: return@awaitEachGesture
                                        if (!down.pressed) return@awaitEachGesture
                                        down.consume()
                                        if (barWidthPx > 0) scrubFraction = (down.position.x / barWidthPx).coerceIn(0f, 1f)
                                        isScrubbing = true
                                        try {
                                            while (true) {
                                                val change = awaitPointerEvent().changes.firstOrNull() ?: break
                                                change.consume()
                                                if (barWidthPx > 0) scrubFraction = (change.position.x / barWidthPx).coerceIn(0f, 1f)
                                                if (!change.pressed) break
                                            }
                                            if (state.durationMs > 0) {
                                                val seekMs = (scrubFraction * state.durationMs).toLong()
                                                spotifyManager.seekTo(seekMs)
                                                displayPositionMs = seekMs
                                            }
                                        } finally {
                                            isScrubbing = false
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isScrubbing) 12.dp else 10.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BgTertiary)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(displayFraction)
                                        .fillMaxHeight()
                                        .background(FgPrimary)
                                )
                            }
                            if (isScrubbing && barWidthPx > 0) {
                                val thumbCenterDp = with(density) { (scrubFraction * barWidthPx).toDp() }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = thumbCenterDp - 7.dp)
                                        .size(14.dp)
                                        .shadow(4.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(FgPrimary)
                                )
                            }
                        }
                        Text(
                            text = formatMs(state.durationMs),
                            fontSize = 13.sp,
                            fontFamily = NunitoFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = FgPrimary,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section 4: Extra controls
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
                                tint = if (state.isShuffling) FgPrimary else FgMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

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
                                tint = if (state.repeatMode > 0) FgPrimary else FgMuted,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(24.dp))

                        IconButton(
                            onClick = { isFavorite = !isFavorite },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (isFavorite) R.drawable.ic_favorite
                                    else R.drawable.ic_favorite_border
                                ),
                                contentDescription = "Favorite",
                                tint = if (isFavorite) FgPrimary else FgMuted,
                                modifier = Modifier.size(24.dp)
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
        if (isLyricsFullScreen && lyrics.isNotEmpty()) {
            LyricsFullScreenOverlay(
                lyrics = lyrics,
                currentLineIndex = currentLineIndex,
                albumArt = albumArt,
                canBlur = canBlur,
                trackName = state.trackName,
                artistName = state.artistName,
                onCollapse = { isLyricsFullScreen = false }
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
    trackName: String,
    artistName: String,
    onCollapse: () -> Unit
) {
    val listState = rememberLazyListState()
    val lyricsAlpha = remember { Animatable(0f) }
    val lyricsState = rememberUpdatedState(lyrics)
    val lineIndexState = rememberUpdatedState(currentLineIndex)
    val trackNameState = rememberUpdatedState(trackName)
    val autoScrollEnabled = remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    val halfHeightDp = LocalConfiguration.current.screenHeightDp.dp / 2

    // Pause autoscroll on any user drag
    val userScrollDetector = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (source == NestedScrollSource.Drag) autoScrollEnabled.value = false
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    LaunchedEffect(Unit) {
        var isFirst = true
        var prevLyrics: List<LyricLine>? = null
        var prevTrackName = ""
        var inTransition = false
        var lineScrollJob: Job? = null

        snapshotFlow {
            Triple(lyricsState.value, lineIndexState.value, trackNameState.value)
        }.collect { (curLyrics, lineIdx, curTrackName) ->
            val first = isFirst
            val trackChanged = !first && curTrackName != prevTrackName
            val lyricsChanged = !first && curLyrics !== prevLyrics
            isFirst = false
            prevLyrics = curLyrics
            prevTrackName = curTrackName

            when {
                first -> {
                    if (lineIdx >= 0) listState.scrollToItem(lineIdx + 1)
                    if (curLyrics.isNotEmpty()) lyricsAlpha.animateTo(1f, tween(400))
                }
                trackChanged -> {
                    // Track changed: fade out immediately so the old lyrics don't scroll
                    // visibly while waiting for new lyrics to load
                    inTransition = true
                    lineScrollJob?.cancel()
                    lineScrollJob?.join()
                    autoScrollEnabled.value = true
                    lyricsAlpha.animateTo(0f, tween(300))
                    if (lyricsChanged) {
                        // New lyrics already in this same emission — complete the transition
                        listState.scrollToItem(0)
                        if (curLyrics.isNotEmpty()) lyricsAlpha.animateTo(1f, tween(400))
                        inTransition = false
                    }
                }
                lyricsChanged -> {
                    // New lyrics arrived after the trackChanged fade-out
                    lineScrollJob?.cancel()
                    lineScrollJob?.join()
                    if (lyricsAlpha.value > 0f) lyricsAlpha.animateTo(0f, tween(300))
                    listState.scrollToItem(0)
                    if (curLyrics.isNotEmpty()) lyricsAlpha.animateTo(1f, tween(400))
                    inTransition = false
                }
                inTransition -> {
                    // Faded out, waiting for new lyrics — ignore line index changes
                }
                lineIdx >= 0 && autoScrollEnabled.value -> {
                    lineScrollJob?.cancel()
                    lineScrollJob = coroutineScope.launch {
                        listState.animateScrollToItem(lineIdx + 1)
                    }
                }
            }
        }
    }

    val fadeColor = Color(0xCC000000)

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
        Box(Modifier.fillMaxSize().background(fadeColor))

        // Lyrics list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp)
                .graphicsLayer { alpha = lyricsAlpha.value }
                .nestedScroll(userScrollDetector),
            contentPadding = PaddingValues(top = halfHeightDp - 41.dp, bottom = halfHeightDp + 41.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: album art + track info, scrolls up with the lyrics
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (albumArt != null) {
                        Image(
                            bitmap = albumArt.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Text(
                        text = trackName,
                        fontFamily = NunitoFontFamily,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = FgPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = artistName,
                        fontFamily = NunitoFontFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = FgSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            items(lyrics.size) { idx ->
                val distance = idx - currentLineIndex
                val targetSize by animateFloatAsState(
                    targetValue = when {
                        distance == 0 -> 30f
                        distance in -1..1 -> 24f
                        else -> 20f
                    },
                    animationSpec = tween(durationMillis = 400),
                    label = "lyric-size"
                )
                val targetColor by animateColorAsState(
                    targetValue = when {
                        distance == 0 -> FgPrimary
                        distance in -1..1 -> FgSecondary
                        else -> FgMuted
                    },
                    animationSpec = tween(durationMillis = 400),
                    label = "lyric-color"
                )
                val targetWeight by animateFloatAsState(
                    targetValue = when {
                        distance == 0 -> FontWeight.Bold.weight.toFloat()
                        else -> FontWeight.Normal.weight.toFloat()
                    },
                    animationSpec = tween(durationMillis = 400),
                    label = "lyric-weight"
                )
                if (distance in -1..1) {
                    // Fixed height so wrapping never shifts the list
                    Box(
                        modifier = Modifier.fillMaxWidth().height(82.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lyrics[idx].text,
                            fontFamily = NunitoFontFamily,
                            fontSize = targetSize.sp,
                            fontWeight = FontWeight(targetWeight.toInt()),
                            color = targetColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 41.sp,
                            style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        text = lyrics[idx].text,
                        fontFamily = NunitoFontFamily,
                        fontSize = targetSize.sp,
                        fontWeight = FontWeight(targetWeight.toInt()),
                        color = targetColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Top fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(fadeColor, Color.Transparent)))
        )

        // Bottom fade
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, fadeColor)))
        )

        // Close button — top-right, above the fade
        IconButton(
            onClick = onCollapse,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(BgSecondary.copy(alpha = 0.75f))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_collapse),
                contentDescription = "Close lyrics",
                tint = FgPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        // "Back to current line" button — appears below close when autoscroll is paused
        AnimatedVisibility(
            visible = !autoScrollEnabled.value,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { -it },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 68.dp, end = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgSecondary.copy(alpha = 0.9f))
                    .clickable {
                        autoScrollEnabled.value = true
                        coroutineScope.launch {
                            listState.animateScrollToItem(lineIndexState.value + 1)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lyrics),
                    contentDescription = null,
                    tint = FgPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Back to current",
                    fontFamily = NunitoFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = FgPrimary
                )
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
