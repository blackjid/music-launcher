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

    suspend fun togglePlayPause(profileId: String, isPlaying: Boolean) {
        val token = profileRepository.getValidToken(profileId) ?: return
        if (isPlaying) SpotifyWebApi.pause(token) else SpotifyWebApi.play(token)
    }

    suspend fun skipNext(profileId: String) {
        val token = profileRepository.getValidToken(profileId) ?: return
        SpotifyWebApi.skipNext(token)
    }

    suspend fun skipPrevious(profileId: String) {
        val token = profileRepository.getValidToken(profileId) ?: return
        SpotifyWebApi.skipPrevious(token)
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
