package com.blackjid.musiclauncher.spotify

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.blackjid.musiclauncher.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SpeakerMonitor(
    private val context: Context,
    private val spotifyManager: SpotifyManager,
    private val settingsStore: SettingsStore,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "SpeakerMonitor"
        private const val REPEAT_INTERVAL_MS = 2 * 60_000L
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val _isOnPhoneSpeaker = MutableStateFlow(false)
    val isOnPhoneSpeaker: StateFlow<Boolean> = _isOnPhoneSpeaker.asStateFlow()

    private val _showWarning = MutableStateFlow(false)
    val showWarning: StateFlow<Boolean> = _showWarning.asStateFlow()

    private var timerJob: Job? = null
    private var warningFired = false

    fun start() {
        scope.launch {
            spotifyManager.playerState.collect { appState ->
                val spotifyPlaying = appState.isPlaying && appState.trackName.isNotEmpty()
                val audioLocal = audioManager.isMusicActive

                _isOnPhoneSpeaker.value = audioLocal

                Log.d(TAG, "spotifyPlaying=$spotifyPlaying audioLocal=$audioLocal timerActive=${timerJob?.isActive}")

                when {
                    spotifyPlaying && !audioLocal -> {
                        // Spotify is playing but audio isn't on this device → cast
                        Log.d(TAG, "Cast detected, resetting timer")
                        timerJob?.cancel(); timerJob = null
                        warningFired = false
                        _showWarning.value = false
                    }
                    audioLocal -> {
                        // Audio on phone speaker — start the timer if not already running.
                        // The timer handles pauses internally; we never cancel it from here.
                        if (timerJob == null || !timerJob!!.isActive) startTimer()
                    }
                    // else: briefly paused — leave the timer alone, it pauses itself
                }
            }
        }
    }

    private fun startTimer() {
        val configured = settingsStore.phoneSpeakerTimeoutMs
        val threshold = if (warningFired) minOf(REPEAT_INTERVAL_MS, configured) else configured
        Log.d(TAG, "Starting speaker timer: ${threshold / 1000}s (warningFired=$warningFired)")
        timerJob = scope.launch {
            // Poll every second; only count elapsed time while audio is actually playing.
            // This way pauses (brief or long) don't reset the clock.
            var elapsed = 0L
            while (elapsed < threshold) {
                delay(1000)
                if (audioManager.isMusicActive) elapsed += 1000
            }
            Log.d(TAG, "Timer fired — pausing and showing warning")
            spotifyManager.pause()
            warningFired = true
            _showWarning.value = true
        }
    }

    fun onPlayHereAnyway() {
        _showWarning.value = false
        timerJob?.cancel(); timerJob = null
        spotifyManager.play()
        startTimer()
    }

    fun onDismiss() {
        _showWarning.value = false
        timerJob?.cancel(); timerJob = null
        // warningFired stays true — if they play again on phone, repeat interval applies
    }
}
