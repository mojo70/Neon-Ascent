package com.neon.ascent.core.data.notifications

import com.neon.ascent.core.data.local.dao.DailyVitalRollupDao
import com.neon.ascent.core.data.local.dao.InsightDao
import com.neon.ascent.core.data.local.entity.DailyVitalRollupEntity
import com.neon.ascent.core.domain.health.HealthManager
import com.neon.ascent.core.domain.health.NeonChargeEngine
import com.neon.ascent.core.domain.health.NeonChargeInput
import com.neon.ascent.core.domain.health.SanctumEngine
import com.neon.ascent.core.domain.health.SanctumInput
import com.neon.ascent.core.domain.notifications.models.*
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.core.domain.workout.models.SetType
import com.neon.ascent.core.domain.workout.rules.RecoveryEngine
import com.neon.ascent.core.data.datastore.BriefPreferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BriefFactsBuilder @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val insightDao: InsightDao,
    private val dailyVitalRollupDao: DailyVitalRollupDao,
    private val healthManager: HealthManager,
    private val briefPrefs: BriefPreferencesDataStore
) {
    suspend fun build(slot: BriefSlot = BriefSlot.AM): BriefFacts {
        val now = Instant.now()
        val last24h = now.minus(24, ChronoUnit.HOURS)
        val last48h = now.minus(48, ChronoUnit.HOURS)
        val last7d = now.minus(7, ChronoUnit.DAYS)

        // 1. Sessions & Top Sets
        val sessions = workoutRepository.getAllSessions().first()
        val rawLastSession = sessions.maxByOrNull { it.date }

        val topSets = mutableListOf<TopSet>()
        var sessionDetails: BriefSessionDetails? = null

        rawLastSession?.let { session ->
            // Recency cap: consider session relevant if within last 36h
            val isRecent = session.date.isAfter(now.minus(36, ChronoUnit.HOURS))
            if (isRecent) {
                val logs = workoutRepository.getLogsForSession(session.id).first()
                logs.flatMap { (log, sets) -> sets.map { set -> log to set } }
                    .filter { it.second.isCompleted && it.second.type != SetType.WARMUP }
                    .sortedByDescending { it.second.weight }
                    .take(3)
                    .forEach { (log, set) ->
                        topSets.add(TopSet(log.exerciseName, set.weight, set.reps))
                    }

                sessionDetails = BriefSessionDetails(
                    id = session.id,
                    date = session.date,
                    dayType = session.protocolDayType?.name,
                    protocolName = session.protocol.displayName,
                    topSets = topSets,
                    notes = session.notes,
                    isCausal = isRecent
                )
            }
        }

        // 2. Recovery Score
        val recentSessionsWithLogs = sessions.take(5).map { session ->
            session to workoutRepository.getLogsForSession(session.id).first()
        }
        val recoveryScore = RecoveryEngine.calculateScore(recentSessionsWithLogs, emptyList())

        // 3. Biometrics Rollup & Sleep Winner Session
        val zone = ZoneId.systemDefault()
        val sleepWindowStart = LocalDate.now(zone).minusDays(1).atTime(18, 0).atZone(zone).toInstant()

        val (winnerSession, winnerAsleepMin) = if (healthManager.isAvailableAndHasPermissions()) {
            val sleepSessions = try { healthManager.sleepSessions(sleepWindowStart, now) } catch (_: Exception) { emptyList() }
            val winner = healthManager.pickCoreNight(sleepSessions, zone)
            if (winner != null) {
                val stageMap = healthManager.stageMinutes(winner)
                val tibMinutes = Duration.between(winner.startTime, winner.endTime).toMinutes()
                val stagedAsleep = (stageMap["DEEP"] ?: 0) + (stageMap["LIGHT"] ?: 0) + (stageMap["REM"] ?: 0) + (stageMap["SLEEPING"] ?: 0)
                val asleep = if (stagedAsleep > 0) {
                    stagedAsleep.toLong()
                } else {
                    val awakeSum = (stageMap["AWAKE"] ?: 0) + (stageMap["AWAKE_IN_BED"] ?: 0) + (stageMap["OUT_OF_BED"] ?: 0)
                    (tibMinutes - awakeSum).coerceAtLeast(0L)
                }
                winner to asleep
            } else null to null
        } else null to null

        val todayStr = LocalDate.now(zone).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val yesterdayStr = LocalDate.now(zone).minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

        val persistedSleepMin = if (winnerAsleepMin == null) {
            val rollups = dailyVitalRollupDao.getRangeList("SLEEP_MIN", yesterdayStr, todayStr)
            rollups.lastOrNull()?.value?.toLong()
        } else null

        val hrvEvents = insightDao.getBiometricEventsByType("HRV").first()
        val sleepEvents = insightDao.getBiometricEventsByType("SLEEP_DURATION").first()
        val rhrEvents = insightDao.getBiometricEventsByType("RHR").first()

        val lastSleepEvent = sleepEvents.firstOrNull { it.timestamp.isAfter(last48h) }
        val sleepMinutes = winnerAsleepMin ?: persistedSleepMin ?: lastSleepEvent?.value?.toLong()

        // Persist SLEEP_MIN rollup if winner session was found
        if (winnerAsleepMin != null && winnerAsleepMin > 0) {
            val nightLocalDateStr = (winnerSession?.endTime ?: now).atZone(zone).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
            dailyVitalRollupDao.upsert(
                DailyVitalRollupEntity(
                    localDate = nightLocalDateStr,
                    metric = "SLEEP_MIN",
                    value = winnerAsleepMin.toDouble(),
                    source = "HC",
                    quality = "OK",
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        val hrvCurrent = hrvEvents.firstOrNull { it.timestamp.isAfter(last24h) }?.value
        val hrvMean7d = hrvEvents.filter { it.timestamp.isAfter(last7d) }.map { it.value }.average().takeIf { !it.isNaN() }

        val rhrCurrent = rhrEvents.firstOrNull { it.timestamp.isAfter(last24h) }?.value
        val rhrMean7d = rhrEvents.filter { it.timestamp.isAfter(last7d) }.map { it.value }.average().takeIf { !it.isNaN() }

        // 4. Sanctum Engine
        val sanctumResult = if (sleepMinutes != null && sleepMinutes > 0) {
            val sessionEnd = winnerSession?.endTime ?: lastSleepEvent?.timestamp ?: now
            val sessionStart = winnerSession?.startTime ?: sessionEnd.minus(sleepMinutes, ChronoUnit.MINUTES)
            SanctumEngine.calculateSanctum(
                SanctumInput(
                    sessionStart = sessionStart,
                    sessionEnd = sessionEnd,
                    userSleepNeedMin = 440L // 7h20m
                )
            )
        } else null

        // 5. Neon Charge Engine
        val chargeResult = NeonChargeEngine.calculateCharge(
            NeonChargeInput(
                sleepMinutesLastNight = sleepMinutes,
                sanctumScore = sanctumResult?.score,
                sleepEndedAt = winnerSession?.endTime ?: lastSleepEvent?.timestamp,
                rhrToday = rhrCurrent,
                rhr7d = if (rhrMean7d != null) listOf(rhrMean7d, rhrMean7d, rhrMean7d, rhrMean7d, rhrMean7d) else emptyList(),
                hrvToday = hrvCurrent,
                hrv7d = if (hrvMean7d != null) listOf(hrvMean7d, hrvMean7d, hrvMean7d, hrvMean7d, hrvMean7d) else emptyList(),
                stepsToday = 0L,
                now = now
            )
        )

        val hasSleepData = sleepMinutes != null && sleepMinutes > 0

        // 6. Build Vitals
        val vitals = BriefVitals(
            sleepMinutes = sleepMinutes,
            needMin = sanctumResult?.needMin ?: 440L, // 7h20m
            sanctumScore = sanctumResult?.score,
            sanctumTier = sanctumResult?.tier,
            sanctumBand = sanctumResult?.band,
            seed = if (hasSleepData) chargeResult.wakeSeed else null,
            seedBand = if (hasSleepData) {
                when {
                    chargeResult.wakeSeed >= 80 -> "CLEAR"
                    chargeResult.wakeSeed >= 65 -> "WATCH"
                    chargeResult.wakeSeed >= 50 -> "HOLD"
                    else -> "GROUND"
                }
            } else null,
            chargeNow = chargeResult.value,
            hrvNight = hrvCurrent,
            hrvBaseline7d = hrvMean7d,
            rhrLast = rhrCurrent,
            rhrBaseline7d = rhrMean7d,
            stepsSoFar = 0L
        )

        // 7. Schedule
        val targetWdStr = briefPrefs.targetWakeWd.first()
        val targetWeStr = briefPrefs.targetWakeWe.first()
        val targetWd = targetWdStr?.let { try { LocalTime.parse(it) } catch (_: Exception) { null } }
        val targetWe = targetWeStr?.let { try { LocalTime.parse(it) } catch (_: Exception) { null } }

        val needMin = sanctumResult?.needMin ?: 440L
        val activeWake = targetWd ?: LocalTime.of(7, 0)
        val rawLightsOut = activeWake.minusMinutes(needMin)
        val roundedMinute = (rawLightsOut.minute / 5) * 5
        val lightsOut = rawLightsOut.withMinute(roundedMinute)

        val schedule = BriefSchedule(
            targetWakeWd = targetWd,
            targetWakeWe = targetWe,
            derivedWakeWd = LocalTime.of(7, 0),
            derivedWakeWe = LocalTime.of(8, 0),
            lightsOut = lightsOut
        )

        // 8. Next Session
        val nextSession = BriefNextSession(
            scheduled = true,
            dayType = "C",
            isHeavyOrC = true
        )

        val dataQuality = BriefDataQuality(
            hasSleep = sleepMinutes != null,
            hasHrv = hrvCurrent != null,
            hasSession24h = rawLastSession?.date?.isAfter(last24h) == true,
            sources = listOf("ROOM", "HEALTH_CONNECT")
        )

        val leadMetric = if (sleepMinutes != null) "SLEEP" else if (sessionDetails != null) "SESSION" else "NONE"
        val leadValue = if (sleepMinutes != null) "$sleepMinutes" else sessionDetails?.id ?: "none"

        return BriefFacts(
            slot = slot,
            generatedAt = now,
            lastSession = sessionDetails,
            recoveryScore = recoveryScore,
            vitals = vitals,
            schedule = schedule,
            nextSession = nextSession,
            dataQuality = dataQuality,
            leadMetric = leadMetric,
            leadValue = leadValue,
            isWeekShort = false
        )
    }
}
