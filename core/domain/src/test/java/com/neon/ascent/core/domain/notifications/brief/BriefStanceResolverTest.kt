package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.*
import com.neon.ascent.core.domain.workout.models.RecoveryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalTime

class BriefStanceResolverTest {

    @Test
    fun `6h12 vs need 7h20 + SEED 68 HOLD + C-day resolves to HOLD`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            lastSession = BriefSessionDetails(
                id = "s1",
                date = Instant.now(),
                dayType = "C",
                protocolName = "CYBERCRAPP",
                topSets = listOf(TopSet("Squat", 320f, 5))
            ),
            vitals = BriefVitals(
                sleepMinutes = 372, // 6h 12m
                needMin = 440,     // 7h 20m
                sanctumScore = 61,
                seed = 68,
                seedBand = "HOLD"
            ),
            nextSession = BriefNextSession(
                scheduled = true,
                dayType = "C",
                isHeavyOrC = true
            )
        )

        val stance = BriefStanceResolver.resolve(facts)
        assertEquals(BriefStance.HOLD, stance)

        val copy = AmTemplateWriter.write(facts, stance)
        assertEquals(BriefStance.HOLD, copy.stance)
        assertEquals("SLEEP 6h12 / 7h20 · SEED 68 HOLD", copy.shadeHeadline)
        assertEquals("320 squat still on the log. Legs today — keep it easy.", copy.shadeBody)
    }

    @Test
    fun `SEED 84 CLEAR + missed Pull + before 1600 + week short resolves to PICKUP and names Pull`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            vitals = BriefVitals(
                sleepMinutes = 440,
                needMin = 440,
                sanctumScore = 82,
                seed = 84,
                seedBand = "CLEAR"
            ),
            nextSession = BriefNextSession(
                scheduled = true,
                dayType = "Pull"
            ),
            isWeekShort = true
        )

        val stance = BriefStanceResolver.resolve(facts, nowLocalTime = LocalTime.of(10, 0))
        assertEquals(BriefStance.PICKUP, stance)

        val copy = AmTemplateWriter.write(facts, stance)
        assertEquals("Pull day is outstanding. Window is open to grab it today.", copy.shadeBody)
    }

    @Test
    fun `PM on track CHARGE 70 SEED 79 has shadeAllowed false`() {
        val facts = BriefFacts(
            slot = BriefSlot.PM,
            vitals = BriefVitals(
                sleepMinutes = 440,
                needMin = 440,
                seed = 79,
                chargeNow = 70
            ),
            schedule = BriefSchedule(
                lightsOut = LocalTime.of(22, 30)
            )
        )

        val stance = BriefStanceResolver.resolve(facts)
        val copy = PmTemplateWriter.write(facts, stance)

        assertEquals(false, copy.shadeAllowed)
        assertEquals("LIGHTS 22:30 for 7h20 · CHARGE 70", copy.shadeHeadline)
    }

    @Test
    fun `empty night + empty session resolves to MISSING_DATA with no invented SANCTUM`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            lastSession = null,
            vitals = BriefVitals(
                sleepMinutes = null,
                sanctumScore = null
            )
        )

        val stance = BriefStanceResolver.resolve(facts)
        assertEquals(BriefStance.MISSING_DATA, stance)
        assertNull(facts.vitals.sanctumScore)

        val copy = AmTemplateWriter.write(facts, stance)
        assertEquals("NEURAL BRIEF · MISSING_DATA", copy.shadeHeadline)
        assertEquals("No overnight telemetry recorded. Open DECK to sync baseline.", copy.shadeBody)
    }
}
