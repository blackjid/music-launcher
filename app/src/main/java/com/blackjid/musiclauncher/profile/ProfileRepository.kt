package com.blackjid.musiclauncher.profile

import android.content.Context
import com.blackjid.musiclauncher.data.EncryptedTokenStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileRepository(context: Context) {

    private val prefs = context.getSharedPreferences("profiles", Context.MODE_PRIVATE)
    private val tokenStore = EncryptedTokenStore(context)
    private val gson = Gson()

    private val _profiles = MutableStateFlow<List<Profile>>(emptyList())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow<Profile?>(null)
    val activeProfile: StateFlow<Profile?> = _activeProfile.asStateFlow()

    init {
        loadProfiles()
    }

    /**
     * Create or update a profile from a Spotify auth result.
     * If a profile with this Spotify user ID already exists, update the name and token.
     * Otherwise create a new one.
     */
    fun upsertFromSpotify(spotifyUserId: String, displayName: String, token: String): Profile {
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

        tokenStore.saveToken(spotifyUserId, token)
        saveProfiles(current)
        setActiveProfile(spotifyUserId)
        return profile
    }

    fun removeProfile(profileId: String) {
        val current = _profiles.value.toMutableList()
        current.removeAll { it.id == profileId }
        tokenStore.removeToken(profileId)
        saveProfiles(current)
        if (_activeProfile.value?.id == profileId) {
            _activeProfile.value = null
        }
    }

    fun setActiveProfile(profileId: String) {
        val current = _profiles.value.map { it.copy(isActive = it.id == profileId) }
        saveProfiles(current)
        _activeProfile.value = current.find { it.isActive }
    }

    fun getToken(profileId: String): String? {
        return tokenStore.getToken(profileId)
    }

    private fun loadProfiles() {
        val json = prefs.getString("profile_list", null) ?: return
        val type = object : TypeToken<List<Profile>>() {}.type
        val loaded: List<Profile> = gson.fromJson(json, type)
        _profiles.value = loaded
        _activeProfile.value = loaded.find { it.isActive }
    }

    private fun saveProfiles(profiles: List<Profile>) {
        _profiles.value = profiles
        prefs.edit().putString("profile_list", gson.toJson(profiles)).apply()
    }
}
