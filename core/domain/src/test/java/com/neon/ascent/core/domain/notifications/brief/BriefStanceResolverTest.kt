package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.BriefFacts
import com.neon.ascent.core.domain.notifications.models.BriefStance
import com.neon.ascent.core.domain.workout.models.RecoveryScore
import com.neon.ascent.core.domain.workout.models.RecoveryStatus
import com.neon.ascent.core.domain.workout.models.WorkoutSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class BriefStanceResolverTest {

    private fun createFacts(
        status: RecoveryStatus,
        hrv: Double? = null,
        hrvMean: Double? = null,
        hasSession: Boolean = true
    ): BriefFacts {
        return BriefFacts(
            lastSession = if (hasSession) WorkoutSession(id = "test", date = Instant.now()) else null,
            topSets = emptyList(),
            recoveryScore = RecoveryScore(
                totalScore = 80,
                status = status,
                rirTrend = 1.0f,
                avgJointHealth = 1.0f,
                stagnationCount = 0,
                avgRpe = 5.0f,
                plainLanguageSummary = ""
            ),
            nextDayType = null,
            hrvCurrent = hrv,
            hrvMean7d = hrvMean,
            sleepHoursCurrent = 8.0,
            sleepHoursMean7d = 8.0,
            rhrCurrent = 60.0,
            rhrMean7d = 60.0
        )
    }

    @Test
    fun `HRV drop triggers HOLD even if status is OPTIMAL`() {
        val facts = createFacts(RecoveryStatus.OPTIMAL, hrv = 26.0, hrvMean = 41.0)
        val stance = BriefStanceResolver.resolve(facts)
        assertEquals(BriefStance.HOLD, stance)
    }

    @Test
    fun `Good HRV and status OPTIMAL triggers PUSH`() {
        val facts = createFacts(RecoveryStatus.OPTIMAL, hrv = 45.0, hrvMean = 41.0)
        val stance = BriefStanceResolver.resolve(facts)
        assertEquals(BriefStance.PUSH, stance)
    }

    @Test
    fun `RecoveryStatus DELOAD triggers RECOVER`() {
        val facts = createFacts(RecoveryStatus.DELOAD)
        val stance = BriefStanceResolver.resolve(facts)
        assertEquals(BriefStance.RECOVER, stance)
    }

    @Test
    fun `No session and no biometrics triggers MISSING_DATA`() {
        val facts = createFacts(RecoveryStatus.OPTIMAL, hasSession = false)
        val stance = BriefStanceResolver.resolve(facts)
        assertEquals(BriefStance.MISSING_DATA, stance)
    }
}
