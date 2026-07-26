package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.*
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

object RecoveryEngine {
    
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
                plainLanguageSummary = "Optimal recovery. Full intensity protocol recommended."
            )
        }

        // 1. RIR Trend (40%)
        // High average RIR (easier) raises score; consistently low (0-1) lowers it.
        val rirTrend = calculateRirTrend(recentSessions)
        val rirScore = (rirTrend / 2.0f * 40f).coerceIn(0f, 40f)

        // 2. Joint Health (30%)
        // 1-2 = neutral/positive. 3+ starts deducting aggressively.
        val avgJointHealth = recentSessions.mapNotNull { it.first.jointHealth }.average().toFloat()
        val jointScore = if (avgJointHealth <= 2.0f) 30f else max(0f, 30f - (avgJointHealth - 2f) * 10f)

        // 3. Stagnation (20%)
        // Each exercise on 2 misses deducts points.
        val stagnationCount = progressionStates.count { it.consecutiveMisses >= 2 }
        val stagnationScore = max(0f, 20f - (stagnationCount * 5f))

        // 4. Session RPE (10%)
        // Only penalizes sustained very high RPE (9-10) when combined with low RIR or rising joints.
        val avgRpe = recentSessions.mapNotNull { it.first.sessionRpe }.average().toFloat()
        val rpeScore = if (avgRpe < 9.0f) 10f else if (rirTrend < 1.0f || avgJointHealth > 2.5f) 0f else 5f

        val totalScore = (rirScore + jointScore + stagnationScore + rpeScore).toInt()
        
        val status = when {
            totalScore >= 70 -> RecoveryStatus.OPTIMAL
            totalScore >= 50 -> RecoveryStatus.CAUTION
            totalScore >= 30 -> RecoveryStatus.DELOAD
            else -> RecoveryStatus.CRITICAL
        }

        val summary = generateSummary(status, stagnationCount, avgJointHealth, rirTrend)

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

    private fun generateSummary(status: RecoveryStatus, stagnationCount: Int, jointHealth: Float, rirTrend: Float): String {
        val issues = mutableListOf<String>()
        if (stagnationCount > 0) issues.add("$stagnationCount stalled exercises")
        if (jointHealth > 2.5f) issues.add("rising joint pain")
        if (rirTrend < 1.0f) issues.add("consistently low RIR")

        return when (status) {
            RecoveryStatus.OPTIMAL -> "Optimal recovery. Full intensity protocol recommended."
            RecoveryStatus.CAUTION -> "Mild caution. Listen to joints and consider slight volume reduction."
            RecoveryStatus.DELOAD -> "Recovery score dropped mainly due to ${issues.joinToString(" + ")}. Soft Deload suggested."
            RecoveryStatus.CRITICAL -> "Critical fatigue detected. Longer cruise or complete reset recommended."
        }
    }
}
