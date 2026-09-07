package com.neon.ascent.feature.health.data.uplink

import com.neon.ascent.core.data.local.dao.DailyVitalRollupDao
import com.neon.ascent.core.data.local.entity.DailyVitalRollupEntity
import com.neon.ascent.core.domain.health.SanctumEngine
import com.neon.ascent.core.domain.health.SanctumInput
import com.neon.ascent.core.domain.health.SanctumResult
import com.neon.ascent.feature.health.domain.uplink.DeepBiometrics
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VitalsRollupWriter @Inject constructor(
    private val rollupDao: DailyVitalRollupDao
) {
    suspend fun writeTodayRollup(metrics: DeepBiometrics): SanctumResult {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val zone = ZoneId.systemDefault()
        val nightLocalDateStr = (metrics.sessionEndTime ?: Instant.now()).atZone(zone).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val now = System.currentTimeMillis()
        val rollups = mutableListOf<DailyVitalRollupEntity>()

        // 1. Daily Activity Metrics (today)
        metrics.stepsToday?.let {
            rollups.add(DailyVitalRollupEntity(today, "STEPS", it.toDouble(), "HC_AGG", "OK", now))
        }
        
        metrics.caloriesToday?.let {
            rollups.add(DailyVitalRollupEntity(today, "KCAL_TOTAL", it, "HC_AGG", "OK", now))
        }

        metrics.caloriesConsumedToday?.takeIf { it > 0.0 }?.let {
            rollups.add(DailyVitalRollupEntity(today, "KCAL_EATEN", it, "HC", "OK", now))
        }

        metrics.restingHeartRate?.let {
            rollups.add(DailyVitalRollupEntity(today, "RHR", it.toDouble(), "HC", "OK", now))
        }

        metrics.hrvRmssd?.let {
            rollups.add(DailyVitalRollupEntity(today, "HRV_RMSSD", it, "HC", "OK", now))
        }

        // Calculate HR_LOAD_MIN for today if coverage & baseline exist
        val dayHrSamples = metrics.sessionHrSamples
        val hrSpanMinToday = if (dayHrSamples.size >= 2) {
            Duration.between(dayHrSamples.first().first, dayHrSamples.last().first).toMinutes()
        } else 0L
        val rhrVal = metrics.restingHeartRate?.toDouble()
        if (dayHrSamples.size >= 20 && hrSpanMinToday >= 120 && rhrVal != null && rhrVal > 0.0) {
            val hrThreshold = maxOf(rhrVal + 25.0, 100.0)
            val elevatedBins = dayHrSamples
                .filter { it.second.toDouble() >= hrThreshold }
                .map { it.first.epochSecond / 60 }
                .toSet()
            if (elevatedBins.isNotEmpty()) {
                rollups.add(DailyVitalRollupEntity(today, "HR_LOAD_MIN", elevatedBins.size.toDouble(), "HC", "OK", now))
            }
        }

        // 2. Fetch history for SanctumEngine
        val fourteenDaysAgo = LocalDate.now(zone).minusDays(14).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val sevenDaysAgo = LocalDate.now(zone).minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val yesterday = LocalDate.now(zone).minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

        val last14AsleepMin = rollupDao.getRangeList("SLEEP_MIN", fourteenDaysAgo, yesterday).map { it.value.toLong() }
        val last7HrvNight = rollupDao.getRangeList("HRV_NIGHT", sevenDaysAgo, yesterday).map { it.value }

        // 3. Sanctum Engine
        val sanctumInput = SanctumInput(
            sessionStart = metrics.sessionStartTime,
            sessionEnd = metrics.sessionEndTime,
            stageMinutes = metrics.sleepStages.mapValues { it.value.toLong() },
            hrInSession = metrics.sessionHrSamples,
            hrPreSleep90 = metrics.eveningHrSamples,
            rmssdInSession = metrics.sessionHrvSamples,
            last14AsleepMin = last14AsleepMin,
            last14Midpoints = emptyList(),
            last7HrvNight = last7HrvNight,
            userSleepNeedMin = null,
            sourceTag = metrics.sleepSourceTag
        )

        val sanctumResult = SanctumEngine.calculateSanctum(sanctumInput)

        // 4. Sleep & Sanctum Rollups for the night that just ended
        val asleepMin = metrics.asleepMinutes ?: metrics.sleepDurationMinutes
        if (asleepMin != null && asleepMin > 0) {
            rollups.add(DailyVitalRollupEntity(nightLocalDateStr, "SLEEP_MIN", asleepMin.toDouble(), metrics.sleepSourceTag, "OK", now))
        }

        val score = sanctumResult.score
        if (score != null) {
            rollups.add(DailyVitalRollupEntity(nightLocalDateStr, "SANCTUM", score.toDouble(), metrics.sleepSourceTag, "OK", now))
            rollups.add(DailyVitalRollupEntity(nightLocalDateStr, "SLEEP_TIER", sanctumResult.tier.toDouble(), metrics.sleepSourceTag, "OK", now))

            // Stage breakdown ONLY if tier >= 1. Omit rows on T0. Do not write zeros.
            if (sanctumResult.tier >= 1) {
                metrics.sleepStages["DEEP"]?.takeIf { it > 0 }?.let {
                    rollups.add(DailyVitalRollupEntity(nightLocalDateStr, "SLEEP_DEEP_MIN", it.toDouble(), metrics.sleepSourceTag, "OK", now))
                }
                metrics.sleepStages["REM"]?.takeIf { it > 0 }?.let {
                    rollups.add(DailyVitalRollupEntity(nightLocalDateStr, "SLEEP_REM_MIN", it.toDouble(), metrics.sleepSourceTag, "OK", now))
                }
                metrics.sleepStages["LIGHT"]?.takeIf { it > 0 }?.let {
                    rollups.add(DailyVitalRollupEntity(nightLocalDateStr, "SLEEP_LIGHT_MIN", it.toDouble(), metrics.sleepSourceTag, "OK", now))
                }
                val awakeSum = (metrics.sleepStages["AWAKE"] ?: 0) + (metrics.sleepStages["AWAKE_IN_BED"] ?: 0) + (metrics.sleepStages["OUT_OF_BED"] ?: 0)
                if (awakeSum > 0) {
                    rollups.add(DailyVitalRollupEntity(nightLocalDateStr, "SLEEP_AWAKE_MIN", awakeSum.toDouble(), metrics.sleepSourceTag, "OK", now))
                }
            }
        }

        // HRV_NIGHT = mean RMSSD in session if samples exist
        if (metrics.sessionHrvSamples.isNotEmpty()) {
            val meanRmssd = metrics.sessionHrvSamples.map { it.second }.average()
            if (!meanRmssd.isNaN()) {
                rollups.add(DailyVitalRollupEntity(nightLocalDateStr, "HRV_NIGHT", meanRmssd, metrics.sleepSourceTag, "OK", now))
            }
        }

        // HR_SLEEP_MEAN = mean HR in session if coverage exists (>= 20 samples & span >= 2h)
        val hrSpanMin = if (metrics.sessionHrSamples.size >= 2) {
            Duration.between(metrics.sessionHrSamples.first().first, metrics.sessionHrSamples.last().first).toMinutes()
        } else 0L
        if (metrics.sessionHrSamples.size >= 20 && hrSpanMin >= 120) {
            val meanHr = metrics.sessionHrSamples.map { it.second }.average()
            if (!meanHr.isNaN()) {
                rollups.add(DailyVitalRollupEntity(nightLocalDateStr, "HR_SLEEP_MEAN", meanHr, metrics.sleepSourceTag, "OK", now))
            }
        }

        if (rollups.isNotEmpty()) {
            rollupDao.upsertAll(rollups)
        }

        return sanctumResult
    }
}
