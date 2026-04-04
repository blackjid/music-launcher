package com.blackjid.musiclauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

data class StoredTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long  // System.currentTimeMillis() + expires_in * 1000
)

class EncryptedTokenStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "spotify_tokens",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveTokens(profileId: String, tokens: StoredTokens) {
        prefs.edit()
            .putString("access_$profileId", tokens.accessToken)
            .putString("refresh_$profileId", tokens.refreshToken)
            .putLong("expires_$profileId", tokens.expiresAt)
            .apply()
    }

    fun getTokens(profileId: String): StoredTokens? {
        val access = prefs.getString("access_$profileId", null) ?: return null
        val refresh = prefs.getString("refresh_$profileId", null) ?: return null
        val expires = prefs.getLong("expires_$profileId", 0L)
        return StoredTokens(access, refresh, expires)
    }

    fun removeTokens(profileId: String) {
        prefs.edit()
            .remove("access_$profileId")
            .remove("refresh_$profileId")
            .remove("expires_$profileId")
            .apply()
    }

    fun isExpired(profileId: String): Boolean {
        val expires = prefs.getLong("expires_$profileId", 0L)
        // Refresh 2 minutes before actual expiry
        return System.currentTimeMillis() > expires - 120_000L
    }
}
