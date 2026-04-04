package com.blackjid.musiclauncher

import android.app.Application
import com.blackjid.musiclauncher.profile.ProfileRepository
import com.blackjid.musiclauncher.spotify.PlaybackPoller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MusicLauncherApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var profileRepository: ProfileRepository
        private set

    lateinit var playbackPoller: PlaybackPoller
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
    }
}
