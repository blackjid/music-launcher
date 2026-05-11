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
    val isPodcast: Boolean = false,
    val isShuffling: Boolean = false,
    val repeatMode: Int = 0  // 0=off, 1=context, 2=track
)
