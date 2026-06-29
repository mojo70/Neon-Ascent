package com.neon.ascent.feature.health.data.remote

import com.neon.ascent.core.data.local.UplinkSecurityManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GarminAuthManager @Inject constructor(
    private val securityManager: UplinkSecurityManager
) : CookieJar {

    private val cookieStore = mutableMapOf<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url.host] = cookies
        // Store session marker in security manager
        if (url.host.contains("garmin.com") && cookies.any { it.name == "SESSION" || it.name == "CASTGC" }) {
            securityManager.saveToken("GARMIN", "HAS_SESSION", "true")
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val stored = cookieStore[url.host]
        if (stored != null) return stored
        
        // Try to sync from system cookie manager if we think we have a session
        if (hasValidSession() && url.host.contains("garmin.com")) {
            syncFromSystemCookieManager(url.toString())
            return cookieStore[url.host] ?: emptyList()
        }
        
        return emptyList()
    }

    /**
     * Injects cookies from the WebView's CookieManager into our custom CookieJar.
     */
    fun syncFromSystemCookieManager(url: String) {
        val cookieManager = android.webkit.CookieManager.getInstance()
        val cookieString = cookieManager.getCookie(url) ?: return
        val httpUrl = url.toHttpUrlOrNull() ?: return
        
        val cookies = cookieString.split(";").mapNotNull {
            Cookie.parse(httpUrl, it.trim())
        }
        
        cookieStore[httpUrl.host] = cookies
        
        if (cookies.any { it.name == "SESSION" || it.name == "CASTGC" }) {
            securityManager.saveToken("GARMIN", "HAS_SESSION", "true")
        }
    }

    fun hasValidSession(): Boolean {
        return securityManager.getToken("GARMIN", "HAS_SESSION") == "true"
    }

    fun logout() {
        cookieStore.clear()
        securityManager.clearProviderTokens("GARMIN")
        android.webkit.CookieManager.getInstance().removeAllCookies(null)
    }
}
