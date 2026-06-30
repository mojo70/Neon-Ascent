package com.neon.ascent.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.Period
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(Vo2MaxRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean {
        return try {
            healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getTodaySteps(): Long {
        if (!hasAllPermissions()) return 0
        
        val startOfDay = ZonedDateTime.now().toLocalDate().atStartOfDay(ZonedDateTime.now().zone).toInstant()
        val endOfDay = Instant.now()
        
        return try {
            val response = healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            )
            response[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Throwable) {
            0L
        }
    }

    suspend fun getLatestHeartRate(): Int {
        if (!hasAllPermissions()) return 0
        
        val end = Instant.now()
        val start = end.minus(1, ChronoUnit.HOURS)
        
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 1
                )
            )
            response.records.firstOrNull()?.samples?.firstOrNull()?.beatsPerMinute?.toInt() ?: 0
        } catch (e: Throwable) {
            0
        }
    }

    suspend fun getLatestVo2Max(): Double {
        if (!hasAllPermissions()) return 0.0
        
        val end = Instant.now()
        val start = end.minus(30, ChronoUnit.DAYS)
        
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = Vo2MaxRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    ascendingOrder = false,
                    pageSize = 1
                )
            )
            // The property name for VO2 max value in the Health Connect SDK 1.1.0 is 'vo2MillilitersPerMinuteKilogram'
            response.records.firstOrNull()?.vo2MillilitersPerMinuteKilogram ?: 0.0
        } catch (e: Throwable) {
            0.0
        }
    }

    suspend fun getSteps(days: Int): List<Pair<Instant, Long>> {
        if (!hasAllPermissions()) return emptyList()
        val now = ZonedDateTime.now()
        val end = now.toLocalDateTime()
        val start = now.minusDays(days.toLong()).toLocalDate().atStartOfDay()
        
        return try {
            val response = healthConnectClient.aggregateGroupByPeriod(
                androidx.health.connect.client.request.AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )
            response.map { 
                it.startTime.atZone(now.zone).toInstant() to (it.result[StepsRecord.COUNT_TOTAL] ?: 0L)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getHrv(days: Int): List<Pair<Instant, Double>> {
        if (!hasAllPermissions()) return emptyList()
        val end = Instant.now()
        val start = Instant.now().minus(days.toLong(), ChronoUnit.DAYS)
        
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateVariabilityRmssdRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records.map { it.time to it.heartRateVariabilityMillis }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSleepDuration(days: Int): List<Pair<Instant, Double>> {
        if (!hasAllPermissions()) return emptyList()
        val end = Instant.now()
        val start = Instant.now().minus(days.toLong(), ChronoUnit.DAYS)
        
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records.map { 
                val duration = java.time.Duration.between(it.startTime, it.endTime).toMinutes() / 60.0
                it.startTime to duration
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getWeeklySteps(): List<Pair<Instant, Long>> = getSteps(7)

    suspend fun getWeeklyHrv(): List<Pair<Instant, Double>> = getHrv(7)

    suspend fun getWeeklySleepDuration(): List<Pair<Instant, Double>> = getSleepDuration(7)
}
