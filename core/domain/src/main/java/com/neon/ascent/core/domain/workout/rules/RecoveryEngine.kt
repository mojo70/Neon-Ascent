package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.*
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.max

/**
 * Calculates logbook-based recovery score (0-100) per NeonCharge.md and Recovery.md.
 *
 * KDoc Fixture:
 * Last session 6h ago with sessionRpe = 9 -> hoursModifier = -10 pts.
 * Score is at least 10 points below the identical session logged 24h ago.
 *
 * Scoring Weights:
 * - Session RPE (45% weight): neutral default RPE 6.0.
 * - Joint Health (25% weight): neutral default 2.0 (1-2 optimal/neutral, 3+ deducts).
 * - Stagnation (20% weight): 20 pts minus 5 pts per stalled exercise (consecutiveMisses >= 2).
 * - Set RIR (10% weight): 10 pts max, unused (0 pts) for failure-based protocols like CYBER_CRAPP.
 * - Hours modifier: -10 pts if < 12h after a session with sessionRpe >= 8; otherwise 0 pts.
 */
object RecoveryEngine {

    fun calculateScore(
        recentSessions: List<Pair<WorkoutSession, List<Pair<WorkoutLog, List<SetLog>>>>>,
        progressionStates: List<ProgressionState>,
        now: Instant = Instant.now()
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
        val rpeScore = (((10.0f - avgRpe) / 4.0f) * 45.0f).coerceIn(0.0f, 45.0f)

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

        // 5. Hours-since-last-hard-session modifier: < 12h after RPE >= 8 cuts 10 pts
        val lastRpe8Session = recentSessions.map { it.first }
            .filter { (it.sessionRpe ?: 0) >= 8 }
            .maxByOrNull { it.date }

        val hoursSinceHardSession = if (lastRpe8Session?.date != null) {
            Duration.between(lastRpe8Session.date, now).toMinutes() / 60.0f
        } else null

        val hoursModifier = if (hoursSinceHardSession != null && hoursSinceHardSession < 12.0f) {
            -10.0f
        } else {
            0.0f
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
        val driverTokens = mutableListOf<String>()
        if (avgRpe >= 8.0f) driverTokens.add("SESSION_RPE_${avgRpe.toInt()}")
        if (jointHealth >= 3.0f) driverTokens.add("JOINTS_${jointHealth.toInt()}")
        if (stagnationCount > 0) driverTokens.add("STALLS_$stagnationCount")

        val tokenPrefix = if (driverTokens.isNotEmpty()) driverTokens.joinToString(" ") + ": " else ""

        val explanations = mutableListOf<String>()
        if (stagnationCount > 0) explanations.add("$stagnationCount stalled exercise(s)")
        if (jointHealth >= 3.0f) explanations.add("joint discomfort (${String.format(Locale.US, "%.1f", jointHealth)})")
        if (avgRpe >= 8.0f) explanations.add("high session RPE (${String.format(Locale.US, "%.1f", avgRpe)})")
        if (!isFailureProtocol && rirTrend < 1.0f) explanations.add("low set RIR")

        val bodyText = when (status) {
            RecoveryStatus.OPTIMAL -> if (explanations.isEmpty()) {
                "Optimal recovery. Full intensity protocol recommended."
            } else {
                "Optimal recovery despite ${explanations.joinToString(" + ")}."
            }
            RecoveryStatus.CAUTION -> "Mild caution due to ${if (explanations.isNotEmpty()) explanations.joinToString(" + ") else "accumulated volume"}. Consider slight volume reduction."
            RecoveryStatus.DELOAD -> "Recovery score dropped due to ${if (explanations.isNotEmpty()) explanations.joinToString(" + ") else "accumulated fatigue"}. Soft Deload suggested."
            RecoveryStatus.CRITICAL -> "Significant exertion overload from ${if (explanations.isNotEmpty()) explanations.joinToString(" + ") else "persistent load"}. Reduced intensity recommended."
        }

        return "$tokenPrefix$bodyText"
    }
}
