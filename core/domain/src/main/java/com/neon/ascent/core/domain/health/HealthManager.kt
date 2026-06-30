package com.neon.ascent.core.domain.health

import androidx.health.connect.client.records.*
import kotlinx.coroutines.flow.Flow

interface HealthManager {
    suspend fun isAvailableAndHasPermissions(): Boolean
    suspend fun getPermissionsToRequest(): Set<String>
    fun getPermissionRationale(): Map<String, String>
    suspend fun readRecentData(days: Int = 7): HealthDataSnapshot
    suspend fun performDailySync()
    fun liveMetricsFlow(): Flow<LiveMetrics>
}

data class HealthDataSnapshot(
    val steps: List<StepsRecord>,
    val sleep: List<SleepSessionRecord>,
    val hrv: List<HeartRateVariabilityRmssdRecord>,
    val activeCalories: List<ActiveCaloriesBurnedRecord>,
    val totalCalories: List<TotalCaloriesBurnedRecord>,
    val distance: List<DistanceRecord>
)

data class LiveMetrics(
    val heartRate: Int? = null,
    val stepsToday: Long? = null,
    val caloriesToday: Double? = null,
    val heartRateVariability: Double? = null
)
