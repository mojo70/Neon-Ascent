package com.neon.ascent.data.repository

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
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
        HealthPermission.getReadPermission(Vo2MaxRecord::class)
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            0.0
        }
    }
}
