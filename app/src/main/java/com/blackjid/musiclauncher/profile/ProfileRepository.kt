package com.blackjid.musiclauncher.profile

import android.content.Context
import android.util.Log
import com.blackjid.musiclauncher.data.EncryptedTokenStore
import com.blackjid.musiclauncher.data.StoredTokens
import com.blackjid.musiclauncher.spotify.SpotifyTokenManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileRepository(
    context: Context,
    private val clientId: String,
    private val clientSecret: String
) {
    companion object {
        private const val TAG = "ProfileRepository"
    }

    private val prefs = context.getSharedPreferences("profiles", Context.MODE_PRIVATE)
    private val tokenStore = EncryptedTokenStore(context)
    private val gson = Gson()

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    init {
        loadProfiles()
    }

    fun upsertFromSpotify(spotifyUserId: String, displayName: String, tokens: StoredTokens): Profile {
        val current = _profiles.value.toMutableList()
        val existing = current.find { it.id == spotifyUserId }

        val profile = if (existing != null) {
            val updated = existing.copy(name = displayName)
            current[current.indexOf(existing)] = updated
            updated
        } else {
            val newProfile = Profile(
                id = spotifyUserId,
                name = displayName,
                avatarColor = Profile.colorForIndex(current.size)
            )
            current.add(newProfile)
            newProfile
        }

        tokenStore.saveTokens(spotifyUserId, tokens)
        saveProfiles(current)
        return profile
    }

    fun removeProfile(profileId: String) {
        val current = _profiles.value.toMutableList()
        current.removeAll { it.id == profileId }
        tokenStore.removeTokens(profileId)
        saveProfiles(current)
    }

    /**
     * Returns a valid (non-expired) access token for the given profile,
     * auto-refreshing if needed. Returns null if refresh fails.
     */
    suspend fun getValidToken(profileId: String): String? {
        val stored = tokenStore.getTokens(profileId) ?: return null

        if (!tokenStore.isExpired(profileId)) {
            return stored.accessToken
        }

        Log.d(TAG, "Token expired for $profileId, refreshing...")
        val refreshed = SpotifyTokenManager.refreshToken(stored.refreshToken, clientId, clientSecret)

        return if (refreshed != null) {
            // Preserve old refresh token if new one wasn't returned
            val newTokens = if (refreshed.refreshToken.isEmpty()) {
                refreshed.copy(refreshToken = stored.refreshToken)
            } else {
                refreshed
            }
            tokenStore.saveTokens(profileId, newTokens)
            Log.d(TAG, "Token refreshed for $profileId")
            newTokens.accessToken
        } else {
            Log.e(TAG, "Token refresh failed for $profileId")
            null
        }
    }

    private fun loadProfiles() {
        val json = prefs.getString("profile_list", null) ?: return
        val type = object : TypeToken<List<Profile>>() {}.type
        val loaded: List<Profile> = gson.fromJson(json, type)
        _profiles.value = loaded
    }

    private fun saveProfiles(profiles: List<Profile>) {
        _profiles.value = profiles
        prefs.edit().putString("profile_list", gson.toJson(profiles)).apply()
    }
}
