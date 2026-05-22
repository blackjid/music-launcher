package com.blackjid.musiclauncher.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OrientationMode { LANDSCAPE, PORTRAIT, AUTO }

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var phoneSpeakerTimeoutMs: Long
        get() = prefs.getLong("phone_speaker_timeout_ms", 5 * 60_000L)
        set(value) { prefs.edit().putLong("phone_speaker_timeout_ms", value).apply() }

    var lyricsEnabled: Boolean
        get() = prefs.getBoolean("lyrics_enabled", true)
        set(value) { prefs.edit().putBoolean("lyrics_enabled", value).apply() }

    private val _orientationMode = MutableStateFlow(
        OrientationMode.valueOf(prefs.getString("orientation_mode", OrientationMode.LANDSCAPE.name)!!)
    )
    val orientationModeFlow: StateFlow<OrientationMode> = _orientationMode.asStateFlow()

    var orientationMode: OrientationMode
        get() = _orientationMode.value
        set(value) {
            prefs.edit().putString("orientation_mode", value.name).apply()
            _orientationMode.value = value
        }
}
