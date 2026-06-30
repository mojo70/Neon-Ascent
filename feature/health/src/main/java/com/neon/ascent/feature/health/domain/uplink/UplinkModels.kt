package com.neon.ascent.feature.health.domain.uplink

import kotlinx.coroutines.flow.Flow

data class LiveBiometrics(
    val heartRate: Int? = null,
    val stepsToday: Long? = null,
    val caloriesToday: Double? = null,
    val heartRateVariability: Double? = null,
    val oxygenSaturation: Double? = null,
    val respiratoryRate: Double? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class DeepBiometrics(
    val stepsToday: Long? = null,
    val caloriesToday: Double? = null,
    val sleepScore: Int? = null,
    val bodyBattery: Int? = null,
    val stressLevel: Int? = null,
    val recoveryTimeMinutes: Int? = null,
    val trainingReadiness: Int? = null,
    val vo2Max: Double? = null,
    val sleepStages: Map<String, Int> = emptyMap(), // Stage Name -> Minutes
    val lastSyncTimestamp: Long = System.currentTimeMillis()
)

sealed class UplinkStatus {
    object Disconnected : UplinkStatus()
    object Authenticating : UplinkStatus()
    object Connected : UplinkStatus()
    data class Syncing(val progress: Float) : UplinkStatus()
    data class Error(val message: String) : UplinkStatus()
    object PermissionRequired : UplinkStatus()
    object NeedsReAuth : UplinkStatus()
}

data class UplinkSyncStatus(
    val provider: UplinkProvider,
    val currentStatus: UplinkStatus,
    val lastSuccessfulSync: Long? = null,
    val lastError: String? = null,
    val lastSyncAttempt: Long? = null
)

enum class UplinkProvider {
    GARMIN,
    HEALTH_CONNECT, // Covers Pixel, Samsung, etc.
    FITBIT,
    APPLE_HEALTH,
    MANUAL
}
