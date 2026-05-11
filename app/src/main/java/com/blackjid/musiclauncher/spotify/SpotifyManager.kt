package com.blackjid.musiclauncher.spotify

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.ImageUri
import com.spotify.protocol.types.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpotifyManager(
    private val context: Context,
    val clientId: String
) {
    companion object {
        private const val TAG = "SpotifyManager"
        const val REDIRECT_URI = "com.blackjid.musiclauncher://callback"
    }

    private var appRemote: SpotifyAppRemote? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _playerState = MutableStateFlow(MusicPlayerState())
    val playerState: StateFlow<MusicPlayerState> = _playerState.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Connect to Spotify App Remote without showing auth view.
     * Call this after the user has already authorized via SpotifyAuth from the Activity.
     */
    fun connect() {
        if (appRemote?.isConnected == true) return

        val params = ConnectionParams.Builder(clientId)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(false)
            .build()

        SpotifyAppRemote.connect(context, params, object : Connector.ConnectionListener {
            override fun onConnected(remote: SpotifyAppRemote) {
                Log.d(TAG, "Connected to Spotify")
                appRemote = remote
                _isConnected.value = true
                _error.value = null
                subscribeToPlayerState()
            }

            override fun onFailure(error: Throwable) {
                Log.e(TAG, "Connection failed", error)
                _isConnected.value = false
                _error.value = error.message ?: "Connection failed"
            }
        })
    }

    fun disconnect() {
        appRemote?.let {
            SpotifyAppRemote.disconnect(it)
        }
        appRemote = null
        _isConnected.value = false
        _playerState.value = MusicPlayerState()
    }

    fun play() {
        appRemote?.playerApi?.resume()
    }

    fun pause() {
        appRemote?.playerApi?.pause()
    }

    fun skipNext() {
        appRemote?.playerApi?.skipNext()
    }

    fun skipPrevious() {
        appRemote?.playerApi?.skipPrevious()
    }

    fun togglePlayPause() {
        val state = _playerState.value
        if (state.isPlaying) pause() else play()
    }

    private fun subscribeToPlayerState() {
        appRemote?.playerApi?.subscribeToPlayerState()
            ?.setEventCallback { state -> updatePlayerState(state) }
            ?.setErrorCallback {
                Log.w(TAG, "Player state subscription lost (Spotify disconnected)")
                appRemote = null
                _isConnected.value = false
                _playerState.value = MusicPlayerState()
                lastImageUri = null
            }
    }

    private fun updatePlayerState(state: PlayerState) {
        val track = state.track
        if (track == null) {
            _playerState.value = MusicPlayerState()
            return
        }

        _playerState.value = MusicPlayerState(
            trackName = track.name,
            artistName = track.artist.name,
            albumName = track.album.name,
            albumArt = _playerState.value.albumArt, // keep current until new one loads
            isPlaying = !state.isPaused,
            durationMs = track.duration,
            positionMs = state.playbackPosition,
            isPodcast = track.isPodcast
        )

        // Load album art
        track.imageUri?.let { uri -> loadAlbumArt(uri) }
    }

    private var lastImageUri: ImageUri? = null

    private fun loadAlbumArt(imageUri: ImageUri) {
        if (imageUri == lastImageUri) return
        lastImageUri = imageUri

        appRemote?.imagesApi?.getImage(imageUri)?.setResultCallback { bitmap ->
            _playerState.value = _playerState.value.copy(albumArt = bitmap)
        }
    }
}
