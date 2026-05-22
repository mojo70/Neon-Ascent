package com.neon.ascent.core.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UplinkSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "neon_uplink_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(provider: String, key: String, value: String) {
        sharedPreferences.edit().putString("${provider}_$key", value).apply()
    }

    fun getToken(provider: String, key: String): String? {
        return sharedPreferences.getString("${provider}_$key", null)
    }

    fun deleteToken(provider: String, key: String) {
        sharedPreferences.edit().remove("${provider}_$key").apply()
    }

    fun clearProviderTokens(provider: String) {
        val editor = sharedPreferences.edit()
        sharedPreferences.all.keys.filter { it.startsWith("${provider}_") }.forEach {
            editor.remove(it)
        }
        editor.apply()
    }

    companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
