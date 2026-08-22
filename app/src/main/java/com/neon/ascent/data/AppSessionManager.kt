package com.neon.ascent.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks in-memory session initialization state so the splash/loading screen
 * only runs during cold app launch / initial startup and not when returning to an
 * active app session.
 */
@Singleton
class AppSessionManager @Inject constructor() {
    private val _isAppLoaded = MutableStateFlow(false)
    val isAppLoaded: StateFlow<Boolean> = _isAppLoaded.asStateFlow()

    fun markAppLoaded() {
        _isAppLoaded.value = true
    }

    fun resetAppLoaded() {
        _isAppLoaded.value = false
    }
}
