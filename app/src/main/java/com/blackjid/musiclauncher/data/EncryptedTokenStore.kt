package com.blackjid.musiclauncher.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

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

    fun saveToken(profileId: String, token: String) {
        prefs.edit().putString("token_$profileId", token).apply()
    }

    fun getToken(profileId: String): String? {
        return prefs.getString("token_$profileId", null)
    }

    fun removeToken(profileId: String) {
        prefs.edit().remove("token_$profileId").apply()
    }
}
