package com.blackjid.musiclauncher.spotify

import com.blackjid.musiclauncher.profile.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlaybackPoller(
    private val profileRepository: ProfileRepository,
    private val scope: CoroutineScope,
    private val pollIntervalMs: Long = 5_000L
) {
    private var pollingJob: Job? = null

    private val _allPlaybackStates = MutableStateFlow<List<WebPlaybackState>>(emptyList())
    val allPlaybackStates: StateFlow<List<WebPlaybackState>> = _allPlaybackStates.asStateFlow()

    fun start() {
        if (pollingJob?.isActive == true) return

        pollingJob = scope.launch {
            while (true) {
                pollAllAccounts()
                delay(pollIntervalMs)
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private suspend fun pollAllAccounts() {
        val profiles = profileRepository.profiles.value
        if (profiles.isEmpty()) return

        val states = profiles.mapNotNull { profile ->
            val token = profileRepository.getValidToken(profile.id) ?: return@mapNotNull null
            SpotifyWebApi.getCurrentPlayback(
                accessToken = token,
                profileId = profile.id,
                profileName = profile.name,
                profileColor = profile.avatarColor
            )
        }
        _allPlaybackStates.value = states
    }
}
