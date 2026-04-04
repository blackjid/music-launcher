package com.blackjid.musiclauncher

import android.app.Application
import com.blackjid.musiclauncher.profile.ProfileRepository
import com.blackjid.musiclauncher.spotify.PlaybackPoller
import com.blackjid.musiclauncher.spotify.SpotifyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MusicLauncherApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var spotifyManager: SpotifyManager
        private set

    lateinit var profileRepository: ProfileRepository
        private set

    lateinit var playbackPoller: PlaybackPoller
        private set

    override fun onCreate() {
        super.onCreate()
        profileRepository = ProfileRepository(this)
        spotifyManager = SpotifyManager(this, clientId = BuildConfig.SPOTIFY_CLIENT_ID)
        playbackPoller = PlaybackPoller(profileRepository, spotifyManager, appScope)
        playbackPoller.start()
    }
}
