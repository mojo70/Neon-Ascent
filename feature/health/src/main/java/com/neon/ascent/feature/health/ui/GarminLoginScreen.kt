package com.neon.ascent.feature.health.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.neon.ascent.feature.health.data.remote.GarminAuthManager

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GarminLoginScreen(
    authManager: GarminAuthManager,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("GARMIN_UPLINK_AUTH") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                url?.let {
                                    // Check if we are on the post-login dashboard or specific success URL
                                    if (it.contains("connect.garmin.com/modern") || it.contains("signin/success")) {
                                        authManager.syncFromSystemCookieManager(it)
                                        if (authManager.hasValidSession()) {
                                            onSuccess()
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Clear cookies to ensure fresh login
                        CookieManager.getInstance().removeAllCookies(null)
                        
                        loadUrl("https://connect.garmin.com/signin")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
