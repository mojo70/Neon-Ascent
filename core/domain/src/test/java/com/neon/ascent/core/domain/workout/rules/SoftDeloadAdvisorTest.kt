package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.DeloadSource
import com.neon.ascent.core.domain.workout.models.DeloadWindowStatus
import com.neon.ascent.core.domain.workout.models.RecoveryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class SoftDeloadAdvisorTest {

    private val now = Instant.parse("2026-10-24T12:00:00Z")

    @Test
    fun `evaluateOffer - triggers on DELOAD status regardless of duration`() {
        val lastStart = now.minus(Duration.ofDays(10))
        val offer = SoftDeloadAdvisor.evaluateOffer(
            recoveryStatus = RecoveryStatus.DELOAD,
            lastDeloadEndOrBlastStart = lastStart,
            now = now
        )

        assertTrue(offer.shouldOffer)
        assertEquals(DeloadSource.RECOVERY, offer.source)
        assertTrue(offer.reason.contains("DELOAD"))
    }

    @Test
    fun `evaluateOffer - triggers on CRITICAL status regardless of duration`() {
        val lastStart = now.minus(Duration.ofDays(5))
        val offer = SoftDeloadAdvisor.evaluateOffer(
            recoveryStatus = RecoveryStatus.CRITICAL,
            lastDeloadEndOrBlastStart = lastStart,
            now = now
        )

        assertTrue(offer.shouldOffer)
        assertEquals(DeloadSource.RECOVERY, offer.source)
        assertTrue(offer.reason.contains("CRITICAL"))
    }

    @Test
    fun `evaluateOffer - triggers when 28 or more days since last blast start even if OPTIMAL`() {
        val lastStart = now.minus(Duration.ofDays(28))
        val offer = SoftDeloadAdvisor.evaluateOffer(
            recoveryStatus = RecoveryStatus.OPTIMAL,
            lastDeloadEndOrBlastStart = lastStart,
            now = now
        )

        assertTrue(offer.shouldOffer)
        assertEquals(DeloadSource.TIME, offer.source)
        assertTrue(offer.reason.contains("28 days"))
    }

    @Test
    fun `evaluateOffer - no offer when OPTIMAL and under 28 days`() {
        val lastStart = now.minus(Duration.ofDays(27))
        val offer = SoftDeloadAdvisor.evaluateOffer(
            recoveryStatus = RecoveryStatus.OPTIMAL,
            lastDeloadEndOrBlastStart = lastStart,
            now = now
        )

        assertFalse(offer.shouldOffer)
        assertNull(offer.source)
    }

    @Test
    fun `evaluateOffer - no offer when CAUTION and under 28 days`() {
        val lastStart = now.minus(Duration.ofDays(14))
        val offer = SoftDeloadAdvisor.evaluateOffer(
            recoveryStatus = RecoveryStatus.CAUTION,
            lastDeloadEndOrBlastStart = lastStart,
            now = now
        )

        assertFalse(offer.shouldOffer)
        assertNull(offer.source)
    }

    @Test
    fun `evaluateOffer - no offer when snoozed in the future`() {
        val snoozedUntil = now.plus(Duration.ofHours(12))
        val offer = SoftDeloadAdvisor.evaluateOffer(
            recoveryStatus = RecoveryStatus.DELOAD,
            lastDeloadEndOrBlastStart = null,
            snoozedUntil = snoozedUntil,
            now = now
        )

        assertFalse(offer.shouldOffer)
        assertTrue(offer.reason.contains("snoozed"))
    }

    @Test
    fun `createAcceptWindow - writes 9 day window by default`() {
        val userId = "user_123"
        val window = SoftDeloadAdvisor.createAcceptWindow(
            userId = userId,
            source = DeloadSource.RECOVERY,
            start = now
        )

        assertEquals(userId, window.userId)
        assertEquals(now, window.start)
        assertEquals(now.plus(Duration.ofDays(9)), window.end)
        assertEquals(DeloadSource.RECOVERY, window.source)
        assertEquals(DeloadWindowStatus.ACTIVE, window.status)
        assertNull(window.snoozedUntil)
        assertEquals(9L, Duration.between(window.start, window.end).toDays())
    }

    @Test
    fun `calculateSnoozeTime - adds 24 hours by default`() {
        val snooze = SoftDeloadAdvisor.calculateSnoozeTime(now = now)
        assertEquals(now.plus(Duration.ofHours(24)), snooze)
    }
}
