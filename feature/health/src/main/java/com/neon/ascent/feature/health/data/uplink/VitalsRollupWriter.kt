package com.neon.ascent.feature.health.data.uplink

import com.neon.ascent.core.data.local.dao.DailyVitalRollupDao
import com.neon.ascent.core.data.local.entity.DailyVitalRollupEntity
import com.neon.ascent.feature.health.domain.uplink.DeepBiometrics
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VitalsRollupWriter @Inject constructor(
    private val rollupDao: DailyVitalRollupDao
) {
    suspend fun writeTodayRollup(metrics: DeepBiometrics) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val now = System.currentTimeMillis()
        val rollups = mutableListOf<DailyVitalRollupEntity>()

        metrics.stepsToday?.let {
            rollups.add(DailyVitalRollupEntity(today, "STEPS", it.toDouble(), "HC_AGG", "OK", now))
        }
        
        metrics.caloriesToday?.let {
            rollups.add(DailyVitalRollupEntity(today, "KCAL_TOTAL", it, "HC_AGG", "OK", now))
        }

        metrics.restingHeartRate?.let {
            rollups.add(DailyVitalRollupEntity(today, "RHR", it.toDouble(), "HC", "OK", now))
        }

        metrics.hrvRmssd?.let {
            rollups.add(DailyVitalRollupEntity(today, "HRV_RMSSD", it, "HC", "OK", now))
        }

        metrics.sleepDurationMinutes?.let {
            val source = if (metrics.sleepStages.isNotEmpty()) "GARMIN" else "HC"
            rollups.add(DailyVitalRollupEntity(today, "SLEEP_MIN", it.toDouble(), source, "OK", now))
        }

        metrics.sleepScore?.let {
            // Never write SLEEP_SCORE unless source is GARMIN
            rollups.add(DailyVitalRollupEntity(today, "SLEEP_SCORE", it.toDouble(), "GARMIN", "OK", now))
        }

        metrics.bodyBattery?.let {
            rollups.add(DailyVitalRollupEntity(today, "BODY_BATTERY", it.toDouble(), "GARMIN", "OK", now))
        }

        metrics.stressLevel?.let {
            rollups.add(DailyVitalRollupEntity(today, "STRESS", it.toDouble(), "GARMIN", "OK", now))
        }

        if (rollups.isNotEmpty()) {
            rollupDao.upsertAll(rollups)
        }
    }
}
