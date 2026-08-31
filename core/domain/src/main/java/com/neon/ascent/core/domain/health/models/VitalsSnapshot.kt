package com.neon.ascent.core.domain.health.models

/**
 * A merged snapshot of biometric data from all active uplinks.
 * Enforces field-priority and identifies the source of truth.
 */
data class VitalsSnapshot(
    val steps: Long? = null,
    val calories: Double? = null,
    val distance: Double? = null,
    val sleepDurationMinutes: Long? = null,
    val sleepScore: Int? = null,
    val sleepStages: Map<String, Int> = emptyMap(), // Keys: DEEP, LIGHT, REM, AWAKE
    val hrvRmssd: Double? = null,
    val restingHeartRate: Int? = null,
    val liveHeartRate: Int? = null,
    val bodyBattery: Int? = null,
    val stressLevel: Int? = null,
    val sourceFooter: String = "HC", // "HC" | "GARMIN" | "HC+GARMIN_HR"
    val timestamp: Long = System.currentTimeMillis()
)
