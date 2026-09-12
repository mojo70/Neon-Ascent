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

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // Cookie scrape persistence disabled per security lock
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        // Do not load or inject system cookies for OkHttp requests
        return emptyList()
    }

    /**
     * Cookie scrape sync disabled per security lock.
     */
    fun syncFromSystemCookieManager(url: String) {
        // No-op: WebView cookie copying into OkHttp disabled
    }

    fun hasValidSession(): Boolean {
        return false
    }

    fun logout() {
        securityManager.clearProviderTokens("GARMIN")
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
    }
}
