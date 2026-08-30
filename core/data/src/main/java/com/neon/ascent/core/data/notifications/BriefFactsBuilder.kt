package com.neon.ascent.core.data.notifications

import com.neon.ascent.core.data.local.dao.InsightDao
import com.neon.ascent.core.domain.notifications.models.BriefFacts
import com.neon.ascent.core.domain.notifications.models.TopSet
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.core.domain.workout.models.SetType
import com.neon.ascent.core.domain.workout.rules.RecoveryEngine
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BriefFactsBuilder @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val insightDao: InsightDao
) {
    suspend fun build(): BriefFacts {
        val now = Instant.now()
        val sessions = workoutRepository.getAllSessions().first()
        val lastSession = sessions.maxByOrNull { it.date }

        val topSets = mutableListOf<TopSet>()
        lastSession?.let { session ->
            val logs = workoutRepository.getLogsForSession(session.id).first()
            logs.flatMap { (log, sets) -> 
                sets.map { set -> log to set } 
            }
            .filter { it.second.isCompleted && it.second.type != SetType.WARMUP }
            .sortedByDescending { it.second.weight }
            .take(3)
            .forEach { (log, set) ->
                topSets.add(TopSet(log.exerciseName, set.weight, set.reps))
            }
        }

        // Recovery Score Calculation
        val recentSessionsWithLogs = sessions.take(5).map { session ->
            session to workoutRepository.getLogsForSession(session.id).first()
        }
        // Assuming progression states are available via repository or derived
        // For P0, we'll use an empty list or fetch if available. 
        // RecoveryEngine.calculateScore needs them for stagnation.
        val recoveryScore = RecoveryEngine.calculateScore(recentSessionsWithLogs, emptyList())

        // Biometrics
        val last24h = now.minus(24, ChronoUnit.HOURS)
        val last7d = now.minus(7, ChronoUnit.DAYS)

        val hrvEvents = insightDao.getBiometricEventsByType("HRV").first()
        val sleepEvents = insightDao.getBiometricEventsByType("SLEEP_DURATION").first()
        val rhrEvents = insightDao.getBiometricEventsByType("RHR").first()

        val hrvCurrent = hrvEvents.firstOrNull { it.timestamp.isAfter(last24h) }?.value
        val hrvMean7d = hrvEvents.filter { it.timestamp.isAfter(last7d) }.map { it.value }.average().takeIf { !it.isNaN() }

        val sleepCurrent = sleepEvents.firstOrNull { it.timestamp.isAfter(last24h) }?.value
        val sleepMean7d = sleepEvents.filter { it.timestamp.isAfter(last7d) }.map { it.value }.average().takeIf { !it.isNaN() }

        val rhrCurrent = rhrEvents.firstOrNull { it.timestamp.isAfter(last24h) }?.value
        val rhrMean7d = rhrEvents.filter { it.timestamp.isAfter(last7d) }.map { it.value }.average().takeIf { !it.isNaN() }

        return BriefFacts(
            lastSession = lastSession,
            topSets = topSets,
            recoveryScore = recoveryScore,
            nextDayType = null, // Logic to be added based on rotation
            hrvCurrent = hrvCurrent,
            hrvMean7d = hrvMean7d,
            sleepHoursCurrent = sleepCurrent,
            sleepHoursMean7d = sleepMean7d,
            rhrCurrent = rhrCurrent,
            rhrMean7d = rhrMean7d
        )
    }
}
