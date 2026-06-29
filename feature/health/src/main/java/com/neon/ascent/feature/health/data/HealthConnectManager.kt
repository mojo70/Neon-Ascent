package com.neon.ascent.feature.health.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.neon.ascent.core.domain.special.HealthDataProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val processor: HealthDataProcessor
) {

    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /** Required permissions for MVP (expand later) */
    private val requiredPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),   // for Strength
        HealthPermission.getReadPermission(DistanceRecord::class),         // Agility support
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    /** Check if Health Connect is available and permissions are granted */
    suspend fun isAvailableAndHasPermissions(): Boolean {
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
    suspend fun getPermissionsToRequest(): Set<String> {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return requiredPermissions - granted
    }

    /**
     * Transparent permission explanation flow
     */
    fun getPermissionRationale(): Map<String, String> = mapOf(
        StepsRecord::class.simpleName!! to "Steps and movement data power your Agility attribute and daily missions.",
        SleepSessionRecord::class.simpleName!! to "Sleep duration & stages directly improve your Endurance stat.",
        HeartRateVariabilityRmssdRecord::class.simpleName!! to "HRV reflects recovery quality and feeds Endurance + biohacking nodes.",
        ActiveCaloriesBurnedRecord::class.simpleName!! to "Active calories contribute to Strength and real-world benchmarks.",
        DistanceRecord::class.simpleName!! to "Distance walked/run boosts Agility progression.",
        HeartRateRecord::class.simpleName!! to "Real-time heart rate monitoring for your neural link stability."
    )

    /** Read data from the last N days (default 7) */
    suspend fun readRecentData(days: Int = 7): HealthDataSnapshot {
        val startTime = Instant.now().minusSeconds(days * 86400L)

        return HealthDataSnapshot(
            steps = readRecords<StepsRecord>(startTime),
            sleep = readRecords<SleepSessionRecord>(startTime),
            hrv = readRecords<HeartRateVariabilityRmssdRecord>(startTime),
            activeCalories = readRecords<ActiveCaloriesBurnedRecord>(startTime),
            distance = readRecords<DistanceRecord>(startTime)
        )
    }

    private suspend inline fun <reified T : Record> readRecords(startTime: Instant): List<T> {
        val request = ReadRecordsRequest(
            recordType = T::class,
            timeRangeFilter = TimeRangeFilter.between(startTime, Instant.now())
        )
        return healthConnectClient.readRecords(request).records
    }

    /** One-shot sync that feeds directly into S.P.E.C.I.A.L. */
    suspend fun performDailySync() {
        if (!isAvailableAndHasPermissions()) return
        // This will be triggered by a WorkManager or manual sync
    }

    /** Reactive flow for real-time dashboard updates */
    fun liveMetricsFlow(): kotlinx.coroutines.flow.Flow<LiveMetrics> = kotlinx.coroutines.flow.flow {
        while (true) {
            if (isAvailableAndHasPermissions()) {
                val now = Instant.now()
                val startOfDay = now.atZone(java.time.ZoneId.systemDefault()).toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()

                val steps = try {
                    readRecords<StepsRecord>(startOfDay).sumOf { it.count }
                } catch (e: Exception) { 0L }

                val calories = try {
                    readRecords<ActiveCaloriesBurnedRecord>(startOfDay).sumOf { it.energy.inKilocalories }
                } catch (e: Exception) { 0.0 }
                
                // For HR, we get the latest entry in the last 5 minutes
                val recentHR = try {
                    readRecords<HeartRateRecord>(now.minusSeconds(300))
                        .flatMap { it.samples }
                        .lastOrNull()?.beatsPerMinute?.toInt()
                } catch (e: Exception) { null }

                // For HRV, get the latest entry in the last hour
                val recentHRV = try {
                    readRecords<HeartRateVariabilityRmssdRecord>(now.minusSeconds(3600))
                        .lastOrNull()?.heartRateVariabilityMillis
                } catch (e: Exception) { null }

                emit(LiveMetrics(
                    heartRate = recentHR,
                    stepsToday = steps,
                    caloriesToday = calories,
                    heartRateVariability = recentHRV
                ))
            } else {
                emit(LiveMetrics())
            }
            kotlinx.coroutines.delay(30000) // 30s update
        }
    }
}

/** Simple container for batched data */
data class HealthDataSnapshot(
    val steps: List<StepsRecord>,
    val sleep: List<SleepSessionRecord>,
    val hrv: List<HeartRateVariabilityRmssdRecord>,
    val activeCalories: List<ActiveCaloriesBurnedRecord>,
    val distance: List<DistanceRecord>
)

data class LiveMetrics(
    val heartRate: Int? = null,
    val stepsToday: Long = 0,
    val caloriesToday: Double = 0.0,
    val heartRateVariability: Double? = null
)
