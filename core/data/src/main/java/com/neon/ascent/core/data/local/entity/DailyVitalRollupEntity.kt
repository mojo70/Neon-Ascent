package com.neon.ascent.core.data.local.entity

import androidx.room.Entity

@Entity(tableName = "daily_vital_rollups", primaryKeys = ["localDate", "metric"])
data class DailyVitalRollupEntity(
    val localDate: String, // yyyy-MM-dd
    val metric: String,    // STEPS, KCAL_TOTAL, KCAL_ACTIVE, RHR, HRV_RMSSD, SLEEP_MIN, SLEEP_SCORE, BODY_BATTERY, STRESS, DISTANCE_M
    val value: Double,
    val source: String,    // HC_AGG, HC, GARMIN
    val quality: String,   // OK, MISSING, ESTIMATED
    val updatedAt: Long
)
