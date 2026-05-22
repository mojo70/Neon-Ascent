package com.neon.ascent.feature.health.domain.uplink

import kotlinx.coroutines.flow.StateFlow

/**
 * Core interface for biometric data providers.
 * Part of the Neon Ascent "Neural Uplink" architecture.
 */
interface NeuralUplink {
    val provider: UplinkProvider
    val status: StateFlow<UplinkStatus>
    
    /**
     * High-frequency biometric stream (e.g. BLE HR).
     * May return null if the provider doesn't support real-time broadcasting.
     */
    fun getLiveStream(): StateFlow<LiveBiometrics?>

    /**
     * Low-frequency proprietary metrics (e.g. Body Battery, Sleep Score).
     */
    suspend fun fetchDeepMetrics(): DeepBiometrics

    /**
     * Provider-specific authentication flow.
     */
    suspend fun authenticate()

    /**
     * Disconnect and clear session data.
     */
    suspend fun disconnect()
}
