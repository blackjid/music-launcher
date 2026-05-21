package com.blackjid.musiclauncher.data

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var phoneSpeakerTimeoutMs: Long
        get() = prefs.getLong("phone_speaker_timeout_ms", 5 * 60_000L)
        set(value) { prefs.edit().putLong("phone_speaker_timeout_ms", value).apply() }

    var lyricsEnabled: Boolean
        get() = prefs.getBoolean("lyrics_enabled", true)
        set(value) { prefs.edit().putBoolean("lyrics_enabled", value).apply() }
}
