package com.blackjid.musiclauncher

import android.app.Application
import android.content.Intent
import com.blackjid.musiclauncher.data.LyricsRepository
import com.blackjid.musiclauncher.data.SettingsStore
import com.blackjid.musiclauncher.profile.ProfileRepository
import com.blackjid.musiclauncher.spotify.PlaybackPoller
import com.blackjid.musiclauncher.spotify.SpeakerMonitor
import com.blackjid.musiclauncher.spotify.SpotifyManager
import com.blackjid.musiclauncher.ui.overlay.SpeakerWarningActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MusicLauncherApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var profileRepository: ProfileRepository
        private set

    lateinit var playbackPoller: PlaybackPoller
        private set

    lateinit var spotifyManager: SpotifyManager
        private set

    lateinit var settingsStore: SettingsStore
        private set

    lateinit var speakerMonitor: SpeakerMonitor
        private set

    lateinit var lyricsRepository: LyricsRepository
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
        settingsStore = SettingsStore(this)
        speakerMonitor = SpeakerMonitor(this, spotifyManager, settingsStore, appScope)
        speakerMonitor.start()
        lyricsRepository = LyricsRepository()

        appScope.launch {
            speakerMonitor.showWarning.collect { show ->
                if (show) {
                    startActivity(
                        Intent(this@MusicLauncherApp, SpeakerWarningActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
    }
}
