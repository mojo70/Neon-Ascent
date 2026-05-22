package com.neon.ascent.feature.health.data.remote

import com.neon.ascent.core.data.local.UplinkSecurityManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GarminAuthManager @Inject constructor(
    private val securityManager: UplinkSecurityManager
) : CookieJar {

    private val cookieStore = mutableMapOf<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url.host] = cookies
        // We could persist these cookies to UplinkSecurityManager if needed
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }

    fun hasValidSession(): Boolean {
        // Simple check if we have cookies for garmin.com
        return cookieStore.containsKey("connect.garmin.com")
    }

    fun logout() {
        cookieStore.clear()
        securityManager.clearProviderTokens("GARMIN")
    }
}
