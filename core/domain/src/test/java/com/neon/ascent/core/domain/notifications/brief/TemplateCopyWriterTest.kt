package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TemplateCopyWriterTest {

    @Test
    fun `AmTemplateWriter PUSH copy generates clear actionable copy`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            lastSession = BriefSessionDetails(
                id = "s1",
                date = Instant.now(),
                dayType = "Push",
                protocolName = "CYBERCRAPP",
                topSets = listOf(TopSet("Bench Press", 225f, 20))
            ),
            vitals = BriefVitals(
                sleepMinutes = 450,
                needMin = 440,
                seed = 85,
                seedBand = "CLEAR"
            ),
            nextSession = BriefNextSession(
                scheduled = true,
                dayType = "Push",
                weightJumpsDue = listOf("230 lbs")
            )
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.PUSH)
        assertTrue(copy.shadeHeadline.contains("SEED 85 CLEAR"))
        assertTrue(copy.shadeBody.contains("weight jump to 230 lbs"))
        assertTrue(copy.shadeAllowed)
    }

    @Test
    fun `PmTemplateWriter NEED_ONLY suppresses shade notification when on track`() {
        val facts = BriefFacts(
            slot = BriefSlot.PM,
            vitals = BriefVitals(
                sleepMinutes = 440,
                needMin = 440,
                seed = 80,
                chargeNow = 75
            )
        )

        val copy = PmTemplateWriter.write(facts, BriefStance.HOLD)
        assertFalse(copy.shadeAllowed)
        assertTrue(copy.cardBody.contains("CHARGE 75"))
    }
}
