package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.*
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.max

object RecoveryEngine {

    /**
     * Calculates the logbook-based recovery score (0-100) and status per NeonCharge.md.
     *
     * Scoring Weights:
     * - Session RPE (45% weight): neutral default RPE 6.0.
     * - Joint Health (25% weight): neutral default 2.0 (1-2 optimal/neutral, 3+ deducts).
     * - Stagnation (20% weight): 20 pts minus 5 pts per stalled exercise (consecutiveMisses >= 2).
     * - Set RIR (10% weight): 10 pts max, unused (0 pts) for failure-based protocols like CYBER_CRAPP.
     * - Hours-since-last-hard-session modifier (+5 for >48h rest, -5 for <24h rest).
     *
     * Fixtures:
     * 1) Wipe test + single session RPE 4, joints 1, no stalls:
     *    RPE 4 (45 pts) + Joints 1 (25 pts) + Stalls (20 pts) + Rest modifier (0 pts) -> 90 -> OPTIMAL.
     * 2) Missing inputs (empty joints & empty RPE on 1 session):
     *    Returns totalScore = 80, status = OPTIMAL, summary = "RECOVERY_INPUTS_MISSING: Defaulting to neutral recovery score."
     */
    fun calculateScore(
        recentSessions: List<Pair<WorkoutSession, List<Pair<WorkoutLog, List<SetLog>>>>>,
        progressionStates: List<ProgressionState>
    ): RecoveryScore {
        if (recentSessions.isEmpty()) {
            return RecoveryScore(
                totalScore = 100,
                status = RecoveryStatus.OPTIMAL,
                rirTrend = 1.0f,
                avgJointHealth = 1.0f,
                stagnationCount = 0,
                avgRpe = 5.0f,
                plainLanguageSummary = "NO_LOGBOOK_YET: Optimal recovery. Full intensity protocol recommended."
            )
        }

        val rpeList = recentSessions.mapNotNull { it.first.sessionRpe }
        val jointList = recentSessions.mapNotNull { it.first.jointHealth }

        // Check for missing inputs across all recent sessions
        if (rpeList.isEmpty() && jointList.isEmpty()) {
            return RecoveryScore(
                totalScore = 80,
                status = RecoveryStatus.OPTIMAL,
                rirTrend = 1.0f,
                avgJointHealth = 2.0f,
                stagnationCount = 0,
                avgRpe = 6.0f,
                plainLanguageSummary = "RECOVERY_INPUTS_MISSING: Defaulting to neutral recovery score."
            )
        }

        // 1. Session RPE (45% weight) - Neutral default 6.0
        val avgRpe = if (rpeList.isNotEmpty()) rpeList.average().toFloat() else 6.0f
        val rpeScore = ((10.0f - avgRpe) / 4.0f * 45.0f).coerceIn(0.0f, 45.0f)

        // 2. Joint Health (25% weight) - Neutral default 2.0
        val avgJointHealth = if (jointList.isNotEmpty()) jointList.average().toFloat() else 2.0f
        val jointScore = if (avgJointHealth <= 2.0f) 25.0f else max(0.0f, 25.0f - (avgJointHealth - 2.0f) * 8.33f)

        // 3. Stagnation / Stalls (20% weight) - Each stalled exercise deducts 5 points
        val stagnationCount = progressionStates.count { it.consecutiveMisses >= 2 }
        val stagnationScore = max(0.0f, 20.0f - (stagnationCount * 5.0f))

        // 4. Set RIR (10% weight) - Unused for failure-based protocols like CYBER_CRAPP
        val mostRecentProtocol = recentSessions.firstOrNull()?.first?.protocol
        val isFailureProtocol = mostRecentProtocol == WorkoutProtocol.CYBER_CRAPP
        val rirTrend = calculateRirTrend(recentSessions)
        val rirScore = if (!isFailureProtocol) {
            (rirTrend / 2.0f * 10.0f).coerceIn(0.0f, 10.0f)
        } else {
            0.0f
        }

        // 5. Hours-since-last-hard-session modifier
        val latestSessionDate = recentSessions.firstOrNull()?.first?.date
        val hoursSinceLast = if (latestSessionDate != null) {
            Duration.between(latestSessionDate, Instant.now()).toHours().toFloat()
        } else {
            48.0f
        }
        val hoursModifier = when {
            hoursSinceLast < 24.0f -> -5.0f
            hoursSinceLast > 48.0f -> 5.0f
            else -> 0.0f
        }

        val totalScore = (rpeScore + jointScore + stagnationScore + rirScore + hoursModifier)
            .toInt()
            .coerceIn(0, 100)

        val status = when {
            totalScore >= 70 -> RecoveryStatus.OPTIMAL
            totalScore >= 50 -> RecoveryStatus.CAUTION
            totalScore >= 30 -> RecoveryStatus.DELOAD
            else -> RecoveryStatus.CRITICAL
        }

        val summary = generateSummary(status, stagnationCount, avgJointHealth, avgRpe, rirTrend, isFailureProtocol)

        return RecoveryScore(
            totalScore = totalScore,
            status = status,
            rirTrend = rirTrend,
            avgJointHealth = avgJointHealth,
            stagnationCount = stagnationCount,
            avgRpe = avgRpe,
            plainLanguageSummary = summary
        )
    }

    private fun calculateRirTrend(sessions: List<Pair<WorkoutSession, List<Pair<WorkoutLog, List<SetLog>>>>>): Float {
        val rirs = sessions.flatMap { it.second }.flatMap { it.second }.mapNotNull { it.rir }.takeLast(15)
        if (rirs.isEmpty()) return 1.0f
        return rirs.average().toFloat()
    }

    private fun generateSummary(
        status: RecoveryStatus,
        stagnationCount: Int,
        jointHealth: Float,
        avgRpe: Float,
        rirTrend: Float,
        isFailureProtocol: Boolean
    ): String {
        val issues = mutableListOf<String>()
        if (stagnationCount > 0) issues.add("$stagnationCount stalled exercise(s)")
        if (jointHealth > 2.5f) issues.add("elevated joint discomfort (${String.format(Locale.US, "%.1f", jointHealth)})")
        if (avgRpe >= 8.5f) issues.add("high session RPE (${String.format(Locale.US, "%.1f", avgRpe)})")
        if (!isFailureProtocol && rirTrend < 1.0f) issues.add("consistently low set RIR")

        return when (status) {
            RecoveryStatus.OPTIMAL -> if (issues.isEmpty()) {
                "Optimal recovery. Full intensity protocol recommended."
            } else {
                "Optimal recovery despite ${issues.joinToString(" + ")}."
            }
            RecoveryStatus.CAUTION -> "Mild caution due to ${if (issues.isNotEmpty()) issues.joinToString(" + ") else "accumulated volume"}. Consider slight volume reduction."
            RecoveryStatus.DELOAD -> "Recovery score dropped mainly due to ${if (issues.isNotEmpty()) issues.joinToString(" + ") else "accumulated fatigue"}. Soft Deload suggested."
            RecoveryStatus.CRITICAL -> "Critical fatigue detected from ${if (issues.isNotEmpty()) issues.joinToString(" + ") else "persistent exertion"}. Longer cruise or complete reset recommended."
        }
    }
}

