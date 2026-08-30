package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.BriefFacts
import com.neon.ascent.core.domain.notifications.models.BriefStance
import com.neon.ascent.core.domain.notifications.models.TopSet
import com.neon.ascent.core.domain.workout.models.*
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TemplateCopyWriterTest {

    private fun createFacts(
        topSets: List<TopSet> = emptyList(),
        hrv: Double? = null,
        hrvMean: Double? = null
    ): BriefFacts {
        return BriefFacts(
            lastSession = WorkoutSession(
                id = "test", 
                date = Instant.now(),
                protocol = WorkoutProtocol.CYBER_CRAPP,
                protocolDayType = ProtocolDayType.CC_A
            ),
            topSets = topSets,
            recoveryScore = RecoveryScore(
                totalScore = 80,
                status = RecoveryStatus.OPTIMAL,
                rirTrend = 1.0f,
                avgJointHealth = 1.0f,
                stagnationCount = 0,
                avgRpe = 5.0f,
                plainLanguageSummary = ""
            ),
            nextDayType = "CC_B",
            hrvCurrent = hrv,
            hrvMean7d = hrvMean,
            sleepHoursCurrent = 8.0,
            sleepHoursMean7d = 8.0,
            rhrCurrent = 60.0,
            rhrMean7d = 60.0
        )
    }

    @Test
    fun `PUSH copy mentions high recovery and next session`() {
        val facts = createFacts(
            topSets = listOf(TopSet("Squat", 320f, 5)),
            hrv = 45.0,
            hrvMean = 41.0
        )
        val copy = TemplateCopyWriter.write(facts, BriefStance.PUSH)
        
        assertTrue(copy.headline.contains("PUSH"))
        assertTrue(copy.body.contains("Squat at 320 lbs"))
        assertTrue(copy.body.contains("Recovery is high"))
    }

    @Test
    fun `RECOVER copy mentions fatigue and deload`() {
        val facts = createFacts(hrv = 26.0, hrvMean = 41.0)
        val copy = TemplateCopyWriter.write(facts, BriefStance.RECOVER)
        
        assertTrue(copy.headline.contains("RECOVER"))
        assertTrue(copy.body.contains("Fatigue detected"))
        assertTrue(copy.body.contains("Soft Deload"))
    }

    @Test
    fun `Empty facts still produces citeable session copy`() {
        val facts = createFacts()
        val copy = TemplateCopyWriter.write(facts, BriefStance.HOLD)
        
        assertTrue(copy.body.contains("CYBERCRAPP"))
        assertTrue(copy.body.contains("Systems stable"))
    }
}
