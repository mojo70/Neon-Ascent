package com.neon.ascent.feature.health.data.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.neon.ascent.core.data.datastore.HealthPreferencesDataStore
import com.neon.ascent.core.domain.health.HealthManager
import com.neon.ascent.feature.health.data.uplink.VitalsRollupWriter
import com.neon.ascent.feature.health.domain.uplink.DeepBiometrics
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.neon.ascent.core.data.repository.BodyLogRepository
import java.util.concurrent.TimeUnit

@HiltWorker
class HealthConnectBackfillWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthManager: HealthManager,
    private val rollupWriter: VitalsRollupWriter,
    private val healthPrefs: HealthPreferencesDataStore,
    private val bodyLogRepository: BodyLogRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("HealthBackfillWorker", "Starting Health Connect historical backfill job")
        try {
            if (!healthManager.isAvailableAndHasPermissions()) {
                Log.w("HealthBackfillWorker", "Health Connect not available or missing core permissions, aborting backfill")
                return@withContext Result.success()
            }

            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val now = Instant.now()

            val hasHistory = healthManager.hasHistoryPermission()
            val maxBackfillDays = if (hasHistory) 180L else 30L
            val startDate = today.minusDays(maxBackfillDays)

            val totalDays = Duration.between(startDate.atStartOfDay(zone).toInstant(), now).toDays()
            val totalWeeks = ((totalDays + 6) / 7).toInt().coerceAtLeast(1)

            Log.i("HealthBackfillWorker", "Backfilling $maxBackfillDays days ($totalWeeks weeks) from $startDate to $today (hasHistory=$hasHistory)")

            var currentWeekStart = startDate
            var weekNum = 1

            while (!currentWeekStart.isAfter(today)) {
                val currentWeekEnd = currentWeekStart.plusDays(6).coerceAtMost(today)
                Log.d("HealthBackfillWorker", "BACKFILL $weekNum / $totalWeeks (Processing $currentWeekStart to $currentWeekEnd)")

                var dayIter = currentWeekStart
                while (!dayIter.isAfter(currentWeekEnd)) {
                    processHistoricalDay(dayIter, zone, now)
                    dayIter = dayIter.plusDays(1)
                }

                currentWeekStart = currentWeekStart.plusDays(7)
                weekNum++
            }

            // Ingest historical body measurements from Health Connect (chunked by week)
            try {
                var bodyWeekStart = startDate
                while (!bodyWeekStart.isAfter(today)) {
                    val bodyWeekEnd = bodyWeekStart.plusDays(6).coerceAtMost(today)
                    val chunkStart = bodyWeekStart.atStartOfDay(zone).toInstant()
                    val chunkEnd = bodyWeekEnd.atTime(23, 59, 59).atZone(zone).toInstant().coerceAtMost(now)
                    bodyLogRepository.ingestHealthConnectBodyData(
                        startDate = chunkStart,
                        endDate = chunkEnd
                    )
                    bodyWeekStart = bodyWeekStart.plusDays(7)
                }
                healthPrefs.setBodyBackfillCompleted(hasHistory)
            } catch (e: Throwable) {
                Log.e("HealthBackfillWorker", "Failed ingesting HC body data during backfill", e)
            }

            Log.i("HealthBackfillWorker", "Health Connect historical backfill completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e("HealthBackfillWorker", "Error executing Health Connect backfill", e)
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun processHistoricalDay(date: LocalDate, zone: ZoneId, now: Instant) {
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.atTime(23, 59, 59).atZone(zone).toInstant().coerceAtMost(now)
        val sleepWindowStart = date.minusDays(1).atTime(18, 0).atZone(zone).toInstant()

        // 1. Daily Aggregates
        val steps = healthManager.aggregateSteps(dayStart, dayEnd).takeIf { it > 0 }
        var calories = healthManager.aggregateTotalCaloriesKcal(dayStart, dayEnd)
        if (calories <= 0.0) {
            calories = healthManager.aggregateActiveCaloriesKcal(dayStart, dayEnd)
        }
        val caloriesVal = calories.takeIf { it > 0 }
        val rhr = healthManager.latestRestingHr(dayStart, dayEnd)

        // 2. Sleep path: Query 18:00 yesterday -> dayEnd
        val sleepSessions = healthManager.sleepSessions(sleepWindowStart, dayEnd)
        val winnerSession = healthManager.pickCoreNight(sleepSessions, zone)

        val tibMinutes = winnerSession?.let {
            Duration.between(it.startTime, it.endTime).toMinutes()
        }

        val stageMap = winnerSession?.let {
            healthManager.stageMinutes(it)
        } ?: emptyMap()

        val asleepMinutes = if (winnerSession != null) {
            val stagedAsleep = (stageMap["DEEP"] ?: 0) + (stageMap["LIGHT"] ?: 0) + (stageMap["REM"] ?: 0) + (stageMap["SLEEPING"] ?: 0)
            if (stagedAsleep > 0) {
                stagedAsleep.toLong()
            } else {
                val awakeSum = (stageMap["AWAKE"] ?: 0) + (stageMap["AWAKE_IN_BED"] ?: 0) + (stageMap["OUT_OF_BED"] ?: 0)
                ((tibMinutes ?: 0L) - awakeSum).coerceAtLeast(0L)
            }
        } else null

        val sourceTag = winnerSession?.let { session ->
            val pkg = session.metadata.dataOrigin.packageName.lowercase()
            when {
                pkg.contains("garmin") -> "HC_GARMIN_WRITE"
                pkg.contains("fitness") || pkg.contains("google") -> "HC_FIT"
                pkg.contains("samsung") || pkg.contains("shealth") -> "HC_SAMSUNG"
                pkg.contains("pixel") -> "HC_PIXEL"
                else -> "HC"
            }
        } ?: "HC"

        // Per-night bounded HR and RMSSD queries (never 180 days at once)
        val sessionHrSamples = if (winnerSession != null) {
            healthManager.heartRateSamples(winnerSession.startTime, winnerSession.endTime)
        } else emptyList()

        val eveningHrSamples = if (winnerSession != null) {
            healthManager.heartRateSamples(winnerSession.startTime.minusSeconds(5400), winnerSession.startTime)
        } else emptyList()

        val sessionHrvSamples = if (winnerSession != null) {
            healthManager.hrvRmssdSamples(winnerSession.startTime, winnerSession.endTime)
        } else emptyList()

        val uiSleepStages = winnerSession?.let {
            healthManager.parseSleepStages(it)
        } ?: emptyMap()

        val deepBiometrics = DeepBiometrics(
            stepsToday = steps,
            caloriesToday = caloriesVal,
            caloriesConsumedToday = null,
            sleepScore = null, // No vendor sleep score
            bodyBattery = null, // No Body Battery
            stressLevel = null,
            restingHeartRate = rhr,
            hrvRmssd = if (sessionHrvSamples.isNotEmpty()) sessionHrvSamples.map { it.second }.average() else null,
            sleepDurationMinutes = asleepMinutes ?: tibMinutes,
            sleepStages = uiSleepStages,
            sessionStartTime = winnerSession?.startTime,
            sessionEndTime = winnerSession?.endTime,
            asleepMinutes = asleepMinutes,
            tibMinutes = tibMinutes,
            sessionHrSamples = sessionHrSamples,
            eveningHrSamples = eveningHrSamples,
            sessionHrvSamples = sessionHrvSamples,
            sleepSourceTag = sourceTag,
            lastSyncTimestamp = System.currentTimeMillis()
        )

        // Write rollups for this historical date via VitalsRollupWriter
        rollupWriter.writeTodayRollup(deepBiometrics)
    }

    companion object {
        private const val UNIQUE_BACKFILL_WORK_NAME = "neon_ascent_health_connect_backfill"

        fun scheduleOneShotBackfill(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = OneTimeWorkRequestBuilder<HealthConnectBackfillWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .addTag("health_backfill")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_BACKFILL_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
