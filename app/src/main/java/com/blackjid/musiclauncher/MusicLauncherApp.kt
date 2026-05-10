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

    lateinit var profileRepository: ProfileRepository
        private set

    lateinit var playbackPoller: PlaybackPoller
        private set

    lateinit var spotifyManager: SpotifyManager
        private set

    override fun onCreate() {
        super.onCreate()
        profileRepository = ProfileRepository(
            this,
            clientId = BuildConfig.SPOTIFY_CLIENT_ID,
            clientSecret = BuildConfig.SPOTIFY_CLIENT_SECRET
        )
        playbackPoller = PlaybackPoller(profileRepository, appScope)
        playbackPoller.start()
        spotifyManager = SpotifyManager(this, BuildConfig.SPOTIFY_CLIENT_ID)
        spotifyManager.connect()
    }
}
