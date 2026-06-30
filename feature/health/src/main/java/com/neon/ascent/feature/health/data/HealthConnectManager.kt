package com.neon.ascent.feature.health.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.neon.ascent.core.domain.health.HealthDataSnapshot
import com.neon.ascent.core.domain.health.HealthManager
import com.neon.ascent.core.domain.health.LiveMetrics
import com.neon.ascent.core.domain.special.HealthDataProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
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
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    /** Check if Health Connect is available and permissions are granted */
    override suspend fun isAvailableAndHasPermissions(): Boolean {
        return try {
            val availability = HealthConnectClient.getSdkStatus(context)
            if (availability != HealthConnectClient.SDK_AVAILABLE) return false

            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            requiredPermissions.all { it in granted }
        } catch (e: Exception) {
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
        HeartRateRecord::class.simpleName!! to "Real-time heart rate monitoring for your neural link stability."
    )

    /** Read data from the last N days (default 7) */
    override suspend fun readRecentData(days: Int): HealthDataSnapshot {
        val startTime = Instant.now().minusSeconds(days * 86400L)

        return HealthDataSnapshot(
            steps = readRecords<StepsRecord>(startTime),
            sleep = readRecords<SleepSessionRecord>(startTime),
            hrv = readRecords<HeartRateVariabilityRmssdRecord>(startTime),
            activeCalories = readRecords<ActiveCaloriesBurnedRecord>(startTime),
            totalCalories = readRecords<TotalCaloriesBurnedRecord>(startTime),
            distance = readRecords<DistanceRecord>(startTime)
        )
    }

    private suspend inline fun <reified T : Record> readRecords(startTime: Instant): List<T> {
        return try {
            val request = ReadRecordsRequest(
                recordType = T::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, Instant.now())
            )
            // Explicitly use Dispatchers.IO to ensure we're off the main thread, 
            // though healthConnectClient should handle this itself.
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                healthConnectClient.readRecords(request).records
            }
        } catch (e: IllegalArgumentException) {
            // Specifically catch the "count must not be less than 1" crash 
            // from the Health Connect SDK when it encountered bad data.
            android.util.Log.e("HealthConnectManager", "SDK validation error reading ${T::class.simpleName}: ${e.message}")
            emptyList()
        } catch (e: Throwable) {
            android.util.Log.e("HealthConnectManager", "Error reading ${T::class.simpleName}", e)
            emptyList()
        }
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

                val steps = try {
                    readRecords<StepsRecord>(startOfDay).sumOf { it.count }
                } catch (e: Throwable) { 0L }

                // Try to get total calories, fall back to active if total is not available/granted
                var calories = try {
                    readRecords<TotalCaloriesBurnedRecord>(startOfDay).sumOf { it.energy.inKilocalories }
                } catch (e: Throwable) { 0.0 }

                if (calories <= 0.0) {
                    calories = try {
                        readRecords<ActiveCaloriesBurnedRecord>(startOfDay).sumOf { it.energy.inKilocalories }
                    } catch (e: Throwable) { 0.0 }
                }

                // For HR, we get the latest entry in the last 5 minutes
                val recentHR = try {
                    readRecords<HeartRateRecord>(now.minusSeconds(300))
                        .flatMap { it.samples }
                        .filter { it.beatsPerMinute > 0 }
                        .lastOrNull()?.beatsPerMinute?.toInt()
                } catch (e: Throwable) { null }

                // For HRV, get the latest entry in the last hour
                val recentHRV = try {
                    readRecords<HeartRateVariabilityRmssdRecord>(now.minusSeconds(3600))
                        .filter { it.heartRateVariabilityMillis > 0 }
                        .lastOrNull()?.heartRateVariabilityMillis
                } catch (e: Throwable) { null }
                
                // Emit what we found. If everything is 0/null, it means either no data or no permissions.
                emit(LiveMetrics(
                    heartRate = recentHR,
                    stepsToday = if (steps > 0) steps else null,
                    caloriesToday = if (calories > 0.0) calories else null,
                    heartRateVariability = recentHRV
                ))
            } else {
                emit(LiveMetrics())
            }
            kotlinx.coroutines.delay(30000) // 30s update
        }
    }
}
