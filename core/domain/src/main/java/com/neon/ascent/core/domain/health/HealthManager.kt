package com.neon.ascent.core.domain.health

import androidx.health.connect.client.records.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface HealthManager {
    suspend fun isAvailableAndHasPermissions(): Boolean
    suspend fun getPermissionsToRequest(): Set<String>
    fun getPermissionRationale(): Map<String, String>

    /** 
     * Read raw data records for the last N days. 
     * NOTE: These are RAW records and must NOT be summed for HUD totals to avoid double-counting.
     */
    suspend fun readRecentData(days: Int = 7): HealthDataSnapshot
    
    suspend fun performDailySync()
    fun liveMetricsFlow(): Flow<LiveMetrics>

    // Aggregate and latest-value helpers (off main thread)
    suspend fun aggregateSteps(start: Instant, end: Instant): Long
    suspend fun aggregateTotalCaloriesKcal(start: Instant, end: Instant): Double
    suspend fun aggregateActiveCaloriesKcal(start: Instant, end: Instant): Double
    suspend fun aggregateDistanceMeters(start: Instant, end: Instant): Double
    suspend fun latestRestingHr(start: Instant, end: Instant): Int?
    suspend fun latestHrvRmssd(start: Instant, end: Instant): Double?
    suspend fun latestHeartRate(start: Instant, end: Instant): Int?
    suspend fun heartRateSamples(start: Instant, end: Instant): List<Pair<Instant, Int>>
    suspend fun exerciseSessions(start: Instant, end: Instant): List<Pair<Instant, Instant>>
    suspend fun sleepSessions(start: Instant, end: Instant): List<SleepSessionRecord>
    suspend fun aggregateNutritionKcal(start: Instant, end: Instant): Double?
    fun parseSleepStages(session: SleepSessionRecord): Map<String, Int>
}

data class HealthDataSnapshot(
    val steps: List<StepsRecord>,
    val sleep: List<SleepSessionRecord>,
    val hrv: List<HeartRateVariabilityRmssdRecord>,
    val activeCalories: List<ActiveCaloriesBurnedRecord>,
    val totalCalories: List<TotalCaloriesBurnedRecord>,
    val distance: List<DistanceRecord>,
    val restingHeartRate: List<RestingHeartRateRecord> = emptyList()
)

data class LiveMetrics(
    val heartRate: Int? = null,
    val stepsToday: Long? = null,
    val caloriesToday: Double? = null,
    val heartRateVariability: Double? = null,
    val restingHeartRate: Int? = null
)
