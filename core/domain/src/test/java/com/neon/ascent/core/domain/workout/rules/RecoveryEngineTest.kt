package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.ProgressionState
import com.neon.ascent.core.domain.workout.models.RecoveryStatus
import com.neon.ascent.core.domain.workout.models.SetLog
import com.neon.ascent.core.domain.workout.models.WorkoutLog
import com.neon.ascent.core.domain.workout.models.WorkoutProtocol
import com.neon.ascent.core.domain.workout.models.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class RecoveryEngineTest {

    @Test
    fun `fixture 1 - 0 sessions returns 100 OPTIMAL NO_LOGBOOK_YET`() {
        val score = RecoveryEngine.calculateScore(emptyList(), emptyList())
        assertEquals(100, score.totalScore)
        assertEquals(RecoveryStatus.OPTIMAL, score.status)
        assertTrue(score.plainLanguageSummary.startsWith("NO_LOGBOOK_YET"))
    }

    @Test
    fun `fixture 2 - 1 session missing RPE and joints returns score 80 RECOVERY_INPUTS_MISSING and is never 0 or NaN`() {
        val session = WorkoutSession(id = "1", date = Instant.now(), sessionRpe = null, jointHealth = null)
        val score = RecoveryEngine.calculateScore(listOf(session to emptyList()), emptyList())
        assertEquals(80, score.totalScore)
        assertEquals(RecoveryStatus.OPTIMAL, score.status)
        assertTrue(score.plainLanguageSummary.startsWith("RECOVERY_INPUTS_MISSING"))
        assertNotEquals(0, score.totalScore)
        assertFalse(score.avgRpe.isNaN())
        assertFalse(score.avgJointHealth.isNaN())
        assertFalse(score.rirTrend.isNaN())
    }

    @Test
    fun `fixture 3 - 1 session RPE 4 joints 1 no stalls returns OPTIMAL with score at least 85`() {
        val session = WorkoutSession(id = "1", date = Instant.now(), sessionRpe = 4, jointHealth = 1)
        val score = RecoveryEngine.calculateScore(listOf(session to emptyList()), emptyList())
        assertEquals(RecoveryStatus.OPTIMAL, score.status)
        assertTrue("Expected score >= 85, got ${score.totalScore}", score.totalScore >= 85)
    }

    @Test
    fun `fixture 4 - CYBER_CRAPP protocol set RIR 0 does not drop score vs RIR omitted`() {
        val session = WorkoutSession(id = "1", date = Instant.now(), sessionRpe = 6, jointHealth = 2, protocol = WorkoutProtocol.CYBER_CRAPP)
        val logRir0 = WorkoutLog(id = "l1", sessionId = "1", exerciseId = "ex1", order = 1, exerciseName = "Press")
        val setRir0 = SetLog(id = "s1", workoutLogId = "l1", weight = 100f, reps = 10, rir = 0)

        val logRirOmitted = WorkoutLog(id = "l2", sessionId = "1", exerciseId = "ex1", order = 1, exerciseName = "Press")
        val setRirOmitted = SetLog(id = "s2", workoutLogId = "l2", weight = 100f, reps = 10, rir = null)

        val scoreWithRir0 = RecoveryEngine.calculateScore(
            listOf(session to listOf(logRir0 to listOf(setRir0))),
            emptyList()
        )
        val scoreWithRirOmitted = RecoveryEngine.calculateScore(
            listOf(session to listOf(logRirOmitted to listOf(setRirOmitted))),
            emptyList()
        )

        assertEquals(scoreWithRirOmitted.totalScore, scoreWithRir0.totalScore)
    }

    @Test
    fun `fixture 5 - last session 6h ago RPE 9 scores at least 10 points lower than same 24h ago`() {
        val now = Instant.now()
        val session6h = WorkoutSession(id = "1", date = now.minusSeconds(6 * 3600), sessionRpe = 9, jointHealth = 1)
        val session24h = WorkoutSession(id = "2", date = now.minusSeconds(24 * 3600), sessionRpe = 9, jointHealth = 1)

        val score6h = RecoveryEngine.calculateScore(listOf(session6h to emptyList()), emptyList(), now)
        val score24h = RecoveryEngine.calculateScore(listOf(session24h to emptyList()), emptyList(), now)

        assertTrue(
            "Expected score at 6h (${score6h.totalScore}) to be at least 10 points lower than at 24h (${score24h.totalScore})",
            score24h.totalScore - score6h.totalScore >= 10
        )
    }

    @Test
    fun `fixture 6 - 2 or more stalls includes STALLS token in summary`() {
        val now = Instant.now()
        val session = WorkoutSession(id = "1", date = now.minusSeconds(24 * 3600), sessionRpe = 6, jointHealth = 2)
        val progressionStates = listOf(
            ProgressionState(exerciseId = "ex1", consecutiveMisses = 2),
            ProgressionState(exerciseId = "ex2", consecutiveMisses = 3)
        )

        val score = RecoveryEngine.calculateScore(listOf(session to emptyList()), progressionStates, now)

        assertTrue(score.plainLanguageSummary.contains("STALLS_2"))
    }
}

