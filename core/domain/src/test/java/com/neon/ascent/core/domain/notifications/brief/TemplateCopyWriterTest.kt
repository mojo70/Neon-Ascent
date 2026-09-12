package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.*
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TemplateCopyWriterTest {

    @Test
    fun `facts with session id present must not appear in copy`() {
        val sessionId = "31acf5b-9902-4211-a3f1-e89a02f928a3"
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            lastSession = BriefSessionDetails(
                id = sessionId,
                date = Instant.now(),
                dayType = "C",
                protocolName = "CYBERCRAPP",
                topSets = listOf(TopSet("Bent-Over Row", 305f, 5))
            ),
            vitals = BriefVitals(
                sleepMinutes = 424,
                needMin = 440,
                seed = 70,
                seedBand = "WATCH"
            ),
            nextSession = BriefNextSession(
                scheduled = true,
                dayType = "C"
            )
        )

        val amCopy = AmTemplateWriter.write(facts, BriefStance.HOLD)
        val pmCopy = PmTemplateWriter.write(facts, BriefStance.HOLD)

        assertFalse("AM shade headline contains session ID", amCopy.shadeHeadline.contains(sessionId))
        assertFalse("AM shade body contains session ID", amCopy.shadeBody.contains(sessionId))
        assertFalse("AM card body contains session ID", amCopy.cardBody.contains(sessionId))
        assertFalse("PM shade body contains session ID", pmCopy.shadeBody.contains(sessionId))

        assertTrue("Validator validates AM copy without session ID", BriefCopyValidator.isValid(amCopy.shadeBody, facts))
        assertFalse("Validator rejects text containing session ID", BriefCopyValidator.isValid("SESSION $sessionId LOG", facts))
    }

    @Test
    fun `AmTemplateWriter starts headline with SLEEP when night exists`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            vitals = BriefVitals(
                sleepMinutes = 424, // 7h04m
                needMin = 440,      // 7h20m
                seed = 70,
                seedBand = "WATCH"
            ),
            nextSession = BriefNextSession(dayType = "C")
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.HOLD)
        assertTrue("Headline starts with SLEEP", copy.shadeHeadline.startsWith("SLEEP 7h04 / 7h20"))
        assertTrue("Headline contains SEED 70 WATCH", copy.shadeHeadline.contains("SEED 70 WATCH"))
    }

    @Test
    fun `AmTemplateWriter converts dayType C to Legs on shade`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            lastSession = BriefSessionDetails(
                id = "s305",
                date = Instant.now(),
                dayType = "C",
                protocolName = "CYBERCRAPP",
                topSets = listOf(TopSet("Bent-Over Row", 305f, 5))
            ),
            vitals = BriefVitals(sleepMinutes = 424, needMin = 440, seed = 70, seedBand = "WATCH"),
            nextSession = BriefNextSession(dayType = "C")
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.HOLD)
        assertFalse("Shade must not contain bare 'Today is C'", copy.shadeBody.contains("Today is C"))
        assertTrue("Shade body contains 'Legs today — keep it easy'", copy.shadeBody.contains("Legs today — keep it easy"))
    }

    @Test
    fun `PmTemplateWriter NEED dump formats tank drop message correctly`() {
        val facts = BriefFacts(
            slot = BriefSlot.PM,
            vitals = BriefVitals(
                sleepMinutes = 424,
                needMin = 440,
                seed = 72,
                chargeNow = 22
            )
        )

        val copy = PmTemplateWriter.write(facts, BriefStance.HOLD)
        assertTrue(copy.shadeAllowed)
        assertTrue("Headline contains LIGHTS and CHARGE", copy.shadeHeadline.contains("LIGHTS") && copy.shadeHeadline.contains("CHARGE 22"))
        assertTrue("Body contains tank drop message", copy.shadeBody.contains("Tank dropped 50 from this morning’s seed"))
        assertTrue("Body contains sleep soon message", copy.shadeBody.contains("Sleep soon if you still want 7h20"))
    }

    @Test
    fun `BriefCopyValidator rejects forbidden UUIDs, LOGs, and keywords`() {
        val facts = BriefFacts(slot = BriefSlot.AM)

        assertFalse(BriefCopyValidator.isValid("[CYBERCRAPP LOG: SESSION 31acf5b-9902]", facts))
        assertFalse(BriefCopyValidator.isValid("Execute the schedule; the sprawl waits for no one.", facts))
        assertFalse(BriefCopyValidator.isValid("Recovery OPTIMAL, but RHR baseline dangerously low.", facts))
        assertFalse(BriefCopyValidator.isValid("System needs immediate reboot.", facts))
        assertFalse(BriefCopyValidator.isValid("CORE_MALFUNCTION detected", facts))
        assertFalse(BriefCopyValidator.isValid("ERROR: Invalid payload", facts))
        assertFalse(BriefCopyValidator.isValid("This text has **markdown bold**", facts))
        assertTrue(BriefCopyValidator.isValid("305 bent-over row still on the log. Legs today — keep it easy.", facts))
    }

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
