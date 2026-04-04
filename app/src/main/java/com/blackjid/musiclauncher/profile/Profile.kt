package com.blackjid.musiclauncher.profile

data class Profile(
    val id: String,              // Spotify user ID
    val name: String,            // Spotify display name
    val avatarColor: Long = DEFAULT_COLORS[0],
    val isActive: Boolean = false
) {
    companion object {
        val DEFAULT_COLORS = listOf(
            0xFF1DB954, // Spotify green
            0xFF1E88E5, // Blue
            0xFFE53935, // Red
            0xFFFB8C00, // Orange
            0xFF8E24AA, // Purple
            0xFF00ACC1  // Cyan
        )

        fun colorForIndex(index: Int): Long =
            DEFAULT_COLORS[index % DEFAULT_COLORS.size]
    }
}
