package com.blackjid.musiclauncher.spotify

import android.graphics.Bitmap

data class MusicPlayerState(
    val trackName: String = "",
    val artistName: String = "",
    val albumName: String = "",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val isPodcast: Boolean = false
)
