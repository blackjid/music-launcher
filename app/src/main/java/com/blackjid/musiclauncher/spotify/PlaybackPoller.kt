package com.blackjid.musiclauncher.spotify

import com.blackjid.musiclauncher.profile.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class PlaybackPoller(
    private val profileRepository: ProfileRepository,
    private val spotifyManager: SpotifyManager,
    private val scope: CoroutineScope,
    private val pollIntervalMs: Long = 5_000L
) {
    private var pollingJob: Job? = null

    private val _polledStates = MutableStateFlow<List<WebPlaybackState>>(emptyList())

    private val _allPlaybackStates = MutableStateFlow<List<WebPlaybackState>>(emptyList())
    val allPlaybackStates: StateFlow<List<WebPlaybackState>> = _allPlaybackStates.asStateFlow()

    fun start() {
        if (pollingJob?.isActive == true) return

        // Poll non-active accounts via Web API
        pollingJob = scope.launch {
            while (true) {
                pollOtherAccounts()
                delay(pollIntervalMs)
            }
        }

        // Merge real-time App Remote state (active account) with polled states (others)
        scope.launch {
            combine(
                _polledStates,
                spotifyManager.playerState,
                profileRepository.activeProfile
            ) { polled, appRemoteState, activeProfile ->
                if (activeProfile == null || appRemoteState.trackName.isEmpty()) {
                    polled
                } else {
                    // Build a WebPlaybackState from App Remote for the active account
                    val activeState = WebPlaybackState(
                        profileId = activeProfile.id,
                        profileName = activeProfile.name,
                        profileColor = activeProfile.avatarColor,
                        trackName = appRemoteState.trackName,
                        artistName = appRemoteState.artistName,
                        albumName = appRemoteState.albumName,
                        albumArtUrl = null, // App Remote uses Bitmap directly
                        albumArtBitmap = appRemoteState.albumArt,
                        isPlaying = appRemoteState.isPlaying,
                        deviceName = "This device"
                    )
                    // Replace or prepend the active account, keep polled others
                    val others = polled.filter { it.profileId != activeProfile.id }
                    listOf(activeState) + others
                }
            }.collect { merged ->
                _allPlaybackStates.value = merged
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun pollOtherAccounts() {
        val profiles = profileRepository.profiles.value
        val activeId = profileRepository.activeProfile.value?.id
        val states = profiles.mapNotNull { profile ->
            // Skip active account — it gets real-time updates via App Remote
            if (profile.id == activeId && spotifyManager.isConnected.value) return@mapNotNull null
            val token = profileRepository.getToken(profile.id) ?: return@mapNotNull null
            SpotifyWebApi.getCurrentPlayback(
                accessToken = token,
                profileId = profile.id,
                profileName = profile.name,
                profileColor = profile.avatarColor
            )
        }
        _polledStates.value = states
    }
}
