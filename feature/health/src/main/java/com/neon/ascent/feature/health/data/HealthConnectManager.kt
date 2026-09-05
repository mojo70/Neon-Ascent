package com.neon.ascent.feature.health.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.neon.ascent.core.domain.health.HealthDataSnapshot
import com.neon.ascent.core.domain.health.HealthManager
import com.neon.ascent.core.domain.health.LiveMetrics
import com.neon.ascent.core.domain.special.HealthDataProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processor: HealthDataProcessor
) : HealthManager {

    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /** Required permissions for MVP (expand later) */
    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class)
    )

    /** Check if Health Connect is available and permissions are granted */
    override suspend fun isAvailableAndHasPermissions(): Boolean {
        return try {
            val availability = HealthConnectClient.getSdkStatus(context)
            if (availability != HealthConnectClient.SDK_AVAILABLE) {
                android.util.Log.w("HealthConnectManager", "SDK Status: $availability")
                return false
            }

            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            
            // Core set for "Connected" status (Base 7 required permissions)
            val corePermissions = setOf(
                HealthPermission.getReadPermission(StepsRecord::class),
                HealthPermission.getReadPermission(SleepSessionRecord::class),
                HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
                HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
                HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
                HealthPermission.getReadPermission(DistanceRecord::class),
                HealthPermission.getReadPermission(HeartRateRecord::class)
            )
            val coreGranted = corePermissions.all { it in granted }
            
            if (!coreGranted) {
                val missing = corePermissions - granted
                android.util.Log.w("HealthConnectManager", "Missing CORE permissions: $missing")
            }
            
            coreGranted
        } catch (e: Exception) {
            android.util.Log.e("HealthConnectManager", "Error checking permissions", e)
            false
        }
    }

    /**
     * Get permissions to request.
     */
    override suspend fun getPermissionsToRequest(): Set<String> {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return requiredPermissions - granted
    }

    /**
     * Transparent permission explanation flow
     */
    override fun getPermissionRationale(): Map<String, String> = mapOf(
        StepsRecord::class.simpleName!! to "Steps and movement data power your Agility attribute and daily missions.",
        SleepSessionRecord::class.simpleName!! to "Sleep duration & stages directly improve your Endurance stat.",
        HeartRateVariabilityRmssdRecord::class.simpleName!! to "HRV reflects recovery quality and feeds Endurance + biohacking nodes.",
        ActiveCaloriesBurnedRecord::class.simpleName!! to "Active calories contribute to Strength and real-world benchmarks.",
        TotalCaloriesBurnedRecord::class.simpleName!! to "Total calories (Active + Basal) provide a complete view of your energy expenditure.",
        DistanceRecord::class.simpleName!! to "Distance walked/run boosts Agility progression.",
        HeartRateRecord::class.simpleName!! to "Real-time heart rate monitoring for your neural link stability.",
        RestingHeartRateRecord::class.simpleName!! to "Resting HR is a recovery signal, not live pulse.",
        NutritionRecord::class.simpleName!! to "Logged meals from Fit or other apps vs your TDEE target."
    )

    /** 
     * Read raw data records for the last N days. 
     * NOTE: These are RAW records and must NOT be summed for HUD totals to avoid double-counting.
     */
    override suspend fun readRecentData(days: Int): HealthDataSnapshot {
        val startTime = Instant.now().minusSeconds(days * 86400L)

        return HealthDataSnapshot(
            steps = readRecords<StepsRecord>(startTime),
            sleep = readRecords<SleepSessionRecord>(startTime),
            hrv = readRecords<HeartRateVariabilityRmssdRecord>(startTime),
            activeCalories = readRecords<ActiveCaloriesBurnedRecord>(startTime),
            totalCalories = readRecords<TotalCaloriesBurnedRecord>(startTime),
            distance = readRecords<DistanceRecord>(startTime),
            restingHeartRate = readRecords<RestingHeartRateRecord>(startTime)
        )
    }

    private suspend inline fun <reified T : Record> readRecords(startTime: Instant, endTime: Instant = Instant.now()): List<T> {
        return try {
            val request = ReadRecordsRequest(
                recordType = T::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            withContext(Dispatchers.IO) {
                healthConnectClient.readRecords(request).records
            }
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("HealthConnectManager", "SDK validation error reading ${T::class.simpleName}: ${e.message}")
            emptyList()
        } catch (e: Throwable) {
            android.util.Log.e("HealthConnectManager", "Error reading ${T::class.simpleName}", e)
            emptyList()
        }
    }

    override suspend fun aggregateSteps(start: Instant, end: Instant): Long {
        return try {
            val request = AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val result = withContext(Dispatchers.IO) {
                healthConnectClient.aggregate(request)
            }
            result[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Exception) {
            android.util.Log.e("HealthConnectManager", "Error aggregating steps", e)
            0L
        }
    }

    override suspend fun aggregateTotalCaloriesKcal(start: Instant, end: Instant): Double {
        return try {
            val request = AggregateRequest(
                metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val result = withContext(Dispatchers.IO) {
                healthConnectClient.aggregate(request)
            }
            result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
        } catch (e: Exception) {
            android.util.Log.e("HealthConnectManager", "Error aggregating total calories", e)
            0.0
        }
    }

    override suspend fun aggregateActiveCaloriesKcal(start: Instant, end: Instant): Double {
        return try {
            val request = AggregateRequest(
                metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val result = withContext(Dispatchers.IO) {
                healthConnectClient.aggregate(request)
            }
            result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
        } catch (e: Exception) {
            android.util.Log.e("HealthConnectManager", "Error aggregating active calories", e)
            0.0
        }
    }

    override suspend fun aggregateDistanceMeters(start: Instant, end: Instant): Double {
        return try {
            val request = AggregateRequest(
                metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val result = withContext(Dispatchers.IO) {
                healthConnectClient.aggregate(request)
            }
            result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0
        } catch (e: Exception) {
            android.util.Log.e("HealthConnectManager", "Error aggregating distance", e)
            0.0
        }
    }

    override suspend fun latestRestingHr(start: Instant, end: Instant): Int? {
        return readRecords<RestingHeartRateRecord>(start, end)
            .lastOrNull()?.beatsPerMinute?.toInt()
    }

    override suspend fun latestHrvRmssd(start: Instant, end: Instant): Double? {
        return readRecords<HeartRateVariabilityRmssdRecord>(start, end)
            .filter { it.heartRateVariabilityMillis > 0 }
            .lastOrNull()?.heartRateVariabilityMillis
    }

    override suspend fun latestHeartRate(start: Instant, end: Instant): Int? {
        return readRecords<HeartRateRecord>(start, end)
            .flatMap { it.samples }
            .filter { it.beatsPerMinute > 0 }
            .lastOrNull()?.beatsPerMinute?.toInt()
    }

    override suspend fun sleepSessions(start: Instant, end: Instant): List<SleepSessionRecord> {
        return readRecords<SleepSessionRecord>(start, end)
    }

    override suspend fun aggregateNutritionKcal(start: Instant, end: Instant): Double? {
        return try {
            val request = AggregateRequest(
                metrics = setOf(NutritionRecord.ENERGY_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val result = withContext(Dispatchers.IO) {
                healthConnectClient.aggregate(request)
            }
            val aggregated = result[NutritionRecord.ENERGY_TOTAL]?.inKilocalories
            if (aggregated != null && aggregated > 0.0) {
                aggregated
            } else {
                val records = readRecords<NutritionRecord>(start, end)
                val sum = records.mapNotNull { it.energy?.inKilocalories }.sum()
                if (sum > 0.0) sum else null
            }
        } catch (e: Exception) {
            try {
                val records = readRecords<NutritionRecord>(start, end)
                val sum = records.mapNotNull { it.energy?.inKilocalories }.sum()
                if (sum > 0.0) sum else null
            } catch (e2: Exception) {
                android.util.Log.e("HealthConnectManager", "Error querying nutrition calories", e2)
                null
            }
        }
    }

    override fun parseSleepStages(session: SleepSessionRecord): Map<String, Int> {
        if (session.stages.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, Int>()
        for (stage in session.stages) {
            val minutes = java.time.Duration.between(stage.startTime, stage.endTime).toMinutes().toInt()
            if (minutes <= 0) continue
            val key = when (stage.stage) {
                SleepSessionRecord.STAGE_TYPE_DEEP -> "DEEP"
                SleepSessionRecord.STAGE_TYPE_LIGHT, SleepSessionRecord.STAGE_TYPE_SLEEPING -> "LIGHT"
                SleepSessionRecord.STAGE_TYPE_REM -> "REM"
                SleepSessionRecord.STAGE_TYPE_AWAKE, SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> "AWAKE"
                else -> null
            }
            if (key != null) {
                result[key] = (result[key] ?: 0) + minutes
            }
        }
        return result
    }

    /** One-shot sync that feeds directly into S.P.E.C.I.A.L. */
    override suspend fun performDailySync() {
        if (!isAvailableAndHasPermissions()) return
        // This will be triggered by a WorkManager or manual sync
    }

    /** Reactive flow for real-time dashboard updates */
    override fun liveMetricsFlow(): kotlinx.coroutines.flow.Flow<LiveMetrics> = kotlinx.coroutines.flow.flow {
        while (true) {
            val availability = try {
                HealthConnectClient.getSdkStatus(context)
            } catch (e: Exception) {
                HealthConnectClient.SDK_UNAVAILABLE
            }

            if (availability == HealthConnectClient.SDK_AVAILABLE) {
                val now = Instant.now()
                val startOfDay = now.atZone(java.time.ZoneId.systemDefault()).toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()

                val steps = aggregateSteps(startOfDay, now)

                // Try to get total calories, fall back to active if total is not available/granted
                var calories = aggregateTotalCaloriesKcal(startOfDay, now)
                if (calories <= 0.0) {
                    calories = aggregateActiveCaloriesKcal(startOfDay, now)
                }

                val recentHR = latestHeartRate(now.minusSeconds(300), now)
                val recentHRV = latestHrvRmssd(now.minusSeconds(3600), now)
                val rhr = latestRestingHr(startOfDay, now)
                
                emit(LiveMetrics(
                    heartRate = recentHR,
                    stepsToday = if (steps > 0) steps else null,
                    caloriesToday = if (calories > 0.0) calories else null,
                    heartRateVariability = recentHRV,
                    restingHeartRate = rhr
                ))
            } else {
                emit(LiveMetrics())
            }
            kotlinx.coroutines.delay(30000) // 30s update
        }
    }
}
