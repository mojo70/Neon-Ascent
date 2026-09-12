package com.neon.ascent.core.data.local

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UplinkSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun createEncryptedSharedPreferences(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "neon_uplink_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            createEncryptedSharedPreferences()
        } catch (e: Throwable) {
            Log.e("UplinkSecurityManager", "EncryptedSharedPreferences initialization failed. Retrying after clearing prefs file.", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.deleteSharedPreferences("neon_uplink_secure_prefs")
                } else {
                    context.getSharedPreferences("neon_uplink_secure_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                }
                createEncryptedSharedPreferences()
            } catch (retryError: Throwable) {
                Log.e("UplinkSecurityManager", "Retry EncryptedSharedPreferences creation failed. Falling back to in-memory store.", retryError)
                InMemorySharedPreferences()
            }
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

    fun getDatabasePassphrase(keyName: String = "db_passphrase"): ByteArray {
        val key = sharedPreferences.getString(keyName, null)
        return if (key != null) {
            key.toByteArray()
        } else {
            val newKey = UUID.randomUUID().toString()
            sharedPreferences.edit().putString(keyName, newKey).apply()
            newKey.toByteArray()
        }
    }

    companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val map = ConcurrentHashMap<String, Any?>()

        override fun getAll(): Map<String, *> = map
        override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: Set<String>?): Set<String>? =
            @Suppress("UNCHECKED_CAST") (map[key] as? Set<String> ?: defValues)
        override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun edit(): SharedPreferences.Editor = Editor()
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

        private inner class Editor : SharedPreferences.Editor {
            private val tempMap = mutableMapOf<String, Any?>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor { tempMap[key ?: ""] = value; return this }
            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor { tempMap[key ?: ""] = values; return this }
            override fun putInt(key: String?, value: Int): SharedPreferences.Editor { tempMap[key ?: ""] = value; return this }
            override fun putLong(key: String?, value: Long): SharedPreferences.Editor { tempMap[key ?: ""] = value; return this }
            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor { tempMap[key ?: ""] = value; return this }
            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor { tempMap[key ?: ""] = value; return this }
            override fun remove(key: String?): SharedPreferences.Editor { tempMap[key ?: ""] = null; return this }
            override fun clear(): SharedPreferences.Editor { clearAll = true; return this }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() {
                if (clearAll) map.clear()
                tempMap.forEach { (k, v) ->
                    if (v == null) map.remove(k) else map[k] = v
                }
            }
        }
    }
}
