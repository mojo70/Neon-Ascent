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
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                "neon_uplink_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            android.util.Log.e("UplinkSecurityManager", "EncryptedSharedPreferences initialization failed", e)
            // Fallback to regular SharedPreferences to prevent crash, 
            // but log it as a serious issue.
            context.getSharedPreferences("neon_uplink_secure_prefs", Context.MODE_PRIVATE)
        }
    }

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

    fun getDatabasePassphrase(): ByteArray {
        return try {
            val key = sharedPreferences.getString("db_passphrase", null)
            if (key != null) {
                key.toByteArray()
            } else {
                val newKey = java.util.UUID.randomUUID().toString()
                sharedPreferences.edit().putString("db_passphrase", newKey).apply()
                newKey.toByteArray()
            }
        } catch (e: Exception) {
            android.util.Log.e("UplinkSecurityManager", "Failed to read db_passphrase", e)
            "fallback_passphrase".toByteArray()
        }
    }

    companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
