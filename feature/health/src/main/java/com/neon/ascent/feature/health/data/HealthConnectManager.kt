package com.neon.ascent.feature.health.data

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
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
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
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
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class)
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
        } catch (e: Throwable) {
            android.util.Log.e("HealthConnectManager", "Error checking permissions", e)
            false
        }
    }

    /** Check if Nutrition read permission is granted specifically */
    override suspend fun hasNutritionPermission(): Boolean {
        return try {
            val availability = HealthConnectClient.getSdkStatus(context)
            if (availability != HealthConnectClient.SDK_AVAILABLE) {
                return false
            }
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            val nutritionPerm = HealthPermission.getReadPermission(NutritionRecord::class)
            nutritionPerm in granted
        } catch (e: Throwable) {
            Log.e("HealthConnectManager", "Error checking nutrition permission", e)
            false
        }
    }

    override suspend fun hasHistoryPermission(): Boolean {
        return try {
            val availability = HealthConnectClient.getSdkStatus(context)
            if (availability != HealthConnectClient.SDK_AVAILABLE) return false
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            PERMISSION_READ_HEALTH_DATA_HISTORY in granted
        } catch (e: Throwable) {
            false
        }
    }

    @Suppress("OPT_IN_USAGE", "OPT_IN_USAGE_ERROR")
    override suspend fun isHistoryFeatureAvailable(): Boolean {
        return try {
            val availability = HealthConnectClient.getSdkStatus(context)
            if (availability != HealthConnectClient.SDK_AVAILABLE) return false
            val featureStatus = healthConnectClient.features.getFeatureStatus(
                HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY
            )
            featureStatus == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Get permissions to request.
     */
    override suspend fun getPermissionsToRequest(): Set<String> {
        return try {
            val availability = HealthConnectClient.getSdkStatus(context)
            if (availability != HealthConnectClient.SDK_AVAILABLE) {
                return requiredPermissions
            }
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            val missing = requiredPermissions - granted
            if (missing.isEmpty() && isHistoryFeatureAvailable() && PERMISSION_READ_HEALTH_DATA_HISTORY !in granted) {
                setOf(PERMISSION_READ_HEALTH_DATA_HISTORY)
            } else {
                missing
            }
        } catch (e: Throwable) {
            Log.e("HealthConnectManager", "Error getting permissions to request", e)
            requiredPermissions
        }
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
        NutritionRecord::class.simpleName!! to "Logged meals from Fit or other apps vs your TDEE target.",
        ExerciseSessionRecord::class.simpleName!! to "Mask workouts so HR load is not double-counted.",
        PERMISSION_READ_HEALTH_DATA_HISTORY to "Build sleep and heart baselines from nights already on this phone."
    )

    companion object {
        const val PERMISSION_READ_HEALTH_DATA_HISTORY = "android.permission.health.READ_HEALTH_DATA_HISTORY"
    }

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
        return hrvRmssdSamples(start, end)
            .lastOrNull()?.second
    }

    override suspend fun hrvRmssdSamples(start: Instant, end: Instant): List<Pair<Instant, Double>> {
        return readRecords<HeartRateVariabilityRmssdRecord>(start, end)
            .filter { it.heartRateVariabilityMillis > 0 }
            .map { it.time to it.heartRateVariabilityMillis }
    }

    override suspend fun latestHeartRate(start: Instant, end: Instant): Int? {
        return readRecords<HeartRateRecord>(start, end)
            .flatMap { it.samples }
            .filter { it.beatsPerMinute > 0 }
            .lastOrNull()?.beatsPerMinute?.toInt()
    }

    override suspend fun heartRateSamples(start: Instant, end: Instant): List<Pair<Instant, Int>> {
        return readRecords<HeartRateRecord>(start, end)
            .flatMap { record -> record.samples.map { sample -> sample.time to sample.beatsPerMinute.toInt() } }
            .filter { it.second > 0 }
    }

    override suspend fun exerciseSessions(start: Instant, end: Instant): List<Pair<Instant, Instant>> {
        return readRecords<ExerciseSessionRecord>(start, end)
            .map { record -> record.startTime to record.endTime }
    }

    override suspend fun sleepSessions(start: Instant, end: Instant): List<SleepSessionRecord> {
        return readRecords<SleepSessionRecord>(start, end)
    }

    override fun pickCoreNight(sessions: List<SleepSessionRecord>, zone: ZoneId): SleepSessionRecord? {
        val validSessions = sessions.filter { session ->
            Duration.between(session.startTime, session.endTime).toMinutes() >= 25
        }
        if (validSessions.isEmpty()) return null

        // Garmin winner check if stages cover >= 50%
        val garminWinner = validSessions.firstOrNull { session ->
            val pkg = session.metadata.dataOrigin.packageName.lowercase()
            val isGarmin = pkg.contains("garmin")
            if (!isGarmin) return@firstOrNull false
            val durationMin = Duration.between(session.startTime, session.endTime).toMinutes()
            val stageMinSum = session.stages.sumOf { 
                Duration.between(it.startTime, it.endTime).toMinutes()
            }
            durationMin > 0 && (stageMinSum.toDouble() / durationMin) >= 0.50
        }
        if (garminWinner != null) return garminWinner

        // Overlapping 00:00-08:00 local time check
        val nightOverlappingSessions = validSessions.filter { session ->
            val sessionStartDate = session.startTime.atZone(zone).toLocalDate()
            val nightStart = sessionStartDate.atStartOfDay(zone).toInstant()
            val nightEnd = sessionStartDate.atTime(8, 0).atZone(zone).toInstant()
            session.startTime.isBefore(nightEnd) && session.endTime.isAfter(nightStart)
        }

        return if (nightOverlappingSessions.isNotEmpty()) {
            nightOverlappingSessions.maxByOrNull {
                Duration.between(it.startTime, it.endTime).toMillis()
            }
        } else {
            validSessions.maxByOrNull {
                Duration.between(it.startTime, it.endTime).toMillis()
            }
        }
    }

    override fun stageMinutes(session: SleepSessionRecord): Map<String, Int> {
        if (session.stages.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, Int>()
        for (stage in session.stages) {
            val minutes = Duration.between(stage.startTime, stage.endTime).toMinutes().toInt()
            if (minutes <= 0) continue
            val key = when (stage.stage) {
                SleepSessionRecord.STAGE_TYPE_DEEP -> "DEEP"
                SleepSessionRecord.STAGE_TYPE_LIGHT -> "LIGHT"
                SleepSessionRecord.STAGE_TYPE_REM -> "REM"
                SleepSessionRecord.STAGE_TYPE_SLEEPING -> "SLEEPING"
                SleepSessionRecord.STAGE_TYPE_AWAKE -> "AWAKE"
                SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> "AWAKE_IN_BED"
                SleepSessionRecord.STAGE_TYPE_OUT_OF_BED -> "OUT_OF_BED"
                else -> null
            }
            if (key != null) {
                result[key] = (result[key] ?: 0) + minutes
            }
        }
        return result
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
        val stageMap = stageMinutes(session)
        if (stageMap.isEmpty()) return emptyMap()
        val legacyMap = mutableMapOf<String, Int>()
        stageMap.forEach { (key, mins) ->
            val legacyKey = when (key) {
                "SLEEPING" -> "LIGHT"
                "AWAKE_IN_BED", "OUT_OF_BED" -> "AWAKE"
                else -> key
            }
            legacyMap[legacyKey] = (legacyMap[legacyKey] ?: 0) + mins
        }
        return legacyMap
    }

    /** One-shot sync that feeds directly into S.P.E.C.I.A.L. */
    override suspend fun performDailySync() {
        if (!isAvailableAndHasPermissions()) return
        // This will be triggered by a WorkManager or manual sync
    }

    /** Reactive flow for real-time dashboard updates */
    override fun liveMetricsFlow(): kotlinx.coroutines.flow.Flow<LiveMetrics> = kotlinx.coroutines.flow.flow {
        while (true) {
            try {
                val availability = try {
                    HealthConnectClient.getSdkStatus(context)
                } catch (e: Throwable) {
                    HealthConnectClient.SDK_UNAVAILABLE
                }

                if (availability == HealthConnectClient.SDK_AVAILABLE) {
                    val now = Instant.now()
                    val startOfDay = now.atZone(ZoneId.systemDefault()).toLocalDate().atStartOfDay(
                        ZoneId.systemDefault()).toInstant()

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
            } catch (e: Throwable) {
                // Silently swallow background permission or SDK exceptions to prevent crashes
                emit(LiveMetrics())
            }
            kotlinx.coroutines.delay(30000) // 30s update
        }
    }
}
