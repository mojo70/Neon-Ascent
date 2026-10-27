package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.*
import com.neon.ascent.core.domain.workout.models.RecoveryScore
import com.neon.ascent.core.domain.workout.models.RecoveryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TemplateCopyWriterTest {

    @Test
    fun `PROMPT 4 Case 1 Short night plus RHR plus 8 bpm plus named Directive`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            vitals = BriefVitals(
                sleepMinutes = 327, // 5h 27m
                needMin = 440,      // 7h 20m
                rhrLast = 62.0,
                rhrBaseline7d = 54.0
            ),
            nextSession = BriefNextSession(
                scheduled = true,
                dayType = "Push",
                isWeeklyTargetMet = false
            ),
            directiveClause = BriefDirectiveClause(
                clauseText = "Ship protocol — 12d left",
                namedDirectiveId = "dir_1"
            )
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.HOLD)

        assertEquals("Short night — 5h27 of 7h20", copy.shadeHeadline)
        assertTrue("Shade body mentions RHR driver", copy.shadeBody.contains("THROTTLE — RHR 62 (usual 54) — keep intensity easy"))
        assertTrue("Shade body contains Directive clause", copy.shadeBody.contains("Ship protocol — 12d left"))
        assertFalse("Shade body does not contain bare keep it easy", copy.shadeBody.contains("Push today — keep it easy"))
        assertEquals(OperationalStatus.THROTTLE, copy.status)
        assertEquals(1, copy.actions.size)
        assertEquals("OPEN OPS", copy.actions.first().label)

        // Confirm shade never contains SANCTUM, SEED, WATCH, or second button
        assertFalse(copy.shadeHeadline.contains("SANCTUM"))
        assertFalse(copy.shadeBody.contains("SANCTUM"))
        assertFalse(copy.shadeHeadline.contains("SEED"))
        assertFalse(copy.shadeBody.contains("SEED"))
        assertFalse(copy.shadeHeadline.contains("WATCH"))
        assertFalse(copy.shadeBody.contains("WATCH"))
        assertTrue(copy.actions.size <= 1)
    }

    @Test
    fun `PROMPT 4 Case 2 On-target sleep plus HRV low plus A-tier Directive`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            vitals = BriefVitals(
                sleepMinutes = 430, // 7h 10m
                needMin = 440,      // 7h 20m
                hrvNight = 28.0,
                hrvBaseline7d = 41.0
            ),
            nextSession = BriefNextSession(
                scheduled = true,
                dayType = "Legs",
                isWeeklyTargetMet = true
            ),
            directiveClause = BriefDirectiveClause(
                clauseText = "Become netrunner — A-tier, 48d",
                namedDirectiveId = "dir_netrunner"
            )
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.RECOVER)

        assertEquals("Slept 7h10 (need 7h20)", copy.shadeHeadline)
        assertTrue("Shade body contains HRV driver", copy.shadeBody.contains("THROTTLE — HRV 28 vs 41 — recovery day"))
        assertTrue("Shade body contains A-tier Directive clause", copy.shadeBody.contains("Become netrunner — A-tier, 48d"))
        assertEquals(OperationalStatus.THROTTLE, copy.status)
        assertEquals(1, copy.actions.size)
        assertEquals("OPEN DIRECTIVE", copy.actions.first().label)
        assertEquals("dir_netrunner", copy.actions.first().extraId)

        assertFalse(copy.shadeHeadline.contains("SANCTUM"))
        assertFalse(copy.shadeBody.contains("SANCTUM"))
        assertFalse(copy.shadeBody.contains("SEED"))
        assertFalse(copy.shadeBody.contains("WATCH"))
        assertTrue(copy.actions.size <= 1)
    }

    @Test
    fun `PROMPT 4 Case 3 Dense near-term Directives outputs count clause only`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            vitals = BriefVitals(sleepMinutes = 440, needMin = 440),
            directiveClause = BriefDirectiveClause(
                clauseText = "4 near-term Directives open.",
                isCountOnly = true,
                count = 4
            )
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.HOLD)

        assertTrue("Shade body contains count clause", copy.shadeBody.contains("4 near-term Directives open."))
        assertNull("No single title directive ID when count clause used", facts.directiveClause?.namedDirectiveId)

        assertFalse(copy.shadeBody.contains("SANCTUM"))
        assertFalse(copy.shadeBody.contains("SEED"))
        assertFalse(copy.shadeBody.contains("WATCH"))
        assertTrue(copy.actions.size <= 1)
    }

    @Test
    fun `PROMPT 4 Case 4 No driver week complete outputs Week is already n-n Easy day`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            vitals = BriefVitals(sleepMinutes = 440, needMin = 440),
            nextSession = BriefNextSession(
                scheduled = true,
                dayType = "A",
                isWeeklyTargetMet = true,
                completedThisWeek = 3,
                scheduledThisWeek = 3
            )
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.HOLD)

        assertEquals(OperationalStatus.NOMINAL, copy.status)
        assertTrue("Week complete output allowed", copy.shadeBody.contains("NOMINAL — Week is already 3/3. Easy day."))

        assertFalse(copy.shadeBody.contains("SANCTUM"))
        assertFalse(copy.shadeBody.contains("SEED"))
        assertFalse(copy.shadeBody.contains("WATCH"))
        assertTrue(copy.actions.size <= 1)
    }

    @Test
    fun `Week target met with zero scheduled count does not emit 0-0 Easy day`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            vitals = BriefVitals(sleepMinutes = 440, needMin = 440),
            nextSession = BriefNextSession(
                scheduled = true,
                dayType = "A",
                isWeeklyTargetMet = true,
                completedThisWeek = 0,
                scheduledThisWeek = 0
            )
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.HOLD)

        assertEquals(OperationalStatus.NOMINAL, copy.status)
        assertFalse("Does not print 0/0", copy.shadeBody.contains("0/0"))
        assertTrue("Falls back to day phrase", copy.shadeBody.contains("NOMINAL — Push today."))
    }

    @Test
    fun `PROMPT 4 Case 5 Missing RHR and HRV skips those drivers and still emits template`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            vitals = BriefVitals(
                sleepMinutes = 440,
                needMin = 440,
                rhrLast = null,
                hrvNight = null
            ),
            nextSession = BriefNextSession(scheduled = true, dayType = "Pull")
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.HOLD)

        assertEquals(OperationalStatus.NOMINAL, copy.status)
        assertTrue("Template emits day-type fallback when vitals missing", copy.shadeBody.contains("NOMINAL — Pull today."))

        assertFalse(copy.shadeBody.contains("SANCTUM"))
        assertFalse(copy.shadeBody.contains("SEED"))
        assertFalse(copy.shadeBody.contains("WATCH"))
        assertTrue(copy.actions.size <= 1)
    }

    @Test
    fun `AmTemplateWriter matching screenshot facts outputs status token and driver`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            lastSession = BriefSessionDetails(
                id = "squat_325",
                date = Instant.now(),
                dayType = "C",
                protocolName = "CYBERCRAPP",
                topSets = listOf(TopSet("Back Squat (Barbell)", 325f, 5))
            ),
            vitals = BriefVitals(
                sleepMinutes = 327, // 5h 27m
                needMin = 440,      // 7h 20m
                seed = 73,
                seedBand = "WATCH"
            ),
            nextSession = BriefNextSession(
                scheduled = true,
                dayType = "C",
                hasSessionToday = false,
                isWeeklyTargetMet = true,
                completedThisWeek = 3,
                scheduledThisWeek = 3
            )
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.HOLD)

        assertEquals("Short night — 5h27 of 7h20", copy.shadeHeadline)
        assertEquals("325 squat is in the book. NOMINAL — Week is already 3/3. Easy day.", copy.shadeBody)
        assertEquals(
            "Short night — 5h 27m of 7h 20m.\nNOMINAL — Week is already 3/3. Easy day.\n325 squat is in the book.",
            copy.cardBody
        )
        assertEquals(OperationalStatus.NOMINAL, copy.status)
        assertTrue("No action buttons when week target is met", copy.actions.isEmpty())
        assertFalse("Card body does not contain SEED or SANCTUM", copy.cardBody.contains("SANCTUM") || copy.cardBody.contains("SEED"))
        assertFalse("Headline does not contain SEED or SANCTUM", copy.shadeHeadline.contains("SANCTUM") || copy.shadeHeadline.contains("SEED"))
    }

    @Test
    fun `Driver 3 SEED band HOLD and heavy C day triggers GROUNDED status`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            vitals = BriefVitals(
                sleepMinutes = 372,
                needMin = 440,
                seedBand = "HOLD"
            ),
            nextSession = BriefNextSession(
                scheduled = true,
                dayType = "C",
                isHeavyOrC = true
            )
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.HOLD)

        assertEquals(OperationalStatus.GROUNDED, copy.status)
        assertTrue("Shade body contains tank low driver", copy.shadeBody.contains("GROUNDED — Morning tank low — hold the heavy work"))
    }

    @Test
    fun `Driver 4 Recovery DELOAD triggers GROUNDED status`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            recoveryScore = RecoveryScore(
                totalScore = 30,
                status = RecoveryStatus.DELOAD,
                rirTrend = 1.0f,
                avgJointHealth = 3.0f,
                stagnationCount = 0,
                avgRpe = 8.0f,
                plainLanguageSummary = "Deload required"
            )
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.DELOAD)

        assertEquals(OperationalStatus.GROUNDED, copy.status)
        assertTrue("Shade body contains recovery deload driver", copy.shadeBody.contains("GROUNDED — Recovery says deload"))
    }

    @Test
    fun `Driver 7 PUSH scheduled generates OVERDRIVE status`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
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

        assertEquals(OperationalStatus.OVERDRIVE, copy.status)
        assertTrue(copy.cardBody.contains("OVERDRIVE — Push is up."))
        assertTrue(copy.shadeBody.contains("OVERDRIVE — Push is up."))
        assertTrue(copy.shadeAllowed)
    }

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
    fun `AmTemplateWriter produces human sleep headline without SANCTUM or SEED`() {
        val facts = BriefFacts(
            slot = BriefSlot.AM,
            vitals = BriefVitals(
                sleepMinutes = 424, // 7h 04m
                needMin = 440,      // 7h 20m
                seed = 70,
                seedBand = "WATCH"
            ),
            nextSession = BriefNextSession(dayType = "C")
        )

        val copy = AmTemplateWriter.write(facts, BriefStance.HOLD)
        assertEquals("Slept 7h04 (need 7h20)", copy.shadeHeadline)
        assertFalse("Headline must not contain SEED 70", copy.shadeHeadline.contains("SEED"))
        assertFalse("Headline must not contain SANCTUM", copy.shadeHeadline.contains("SANCTUM"))
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
        assertTrue(BriefCopyValidator.isValid("305 bent-over row still on the log. Legs today.", facts))
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
