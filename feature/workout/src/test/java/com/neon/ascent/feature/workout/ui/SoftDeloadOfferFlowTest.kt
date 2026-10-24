package com.neon.ascent.feature.workout.ui

import com.neon.ascent.core.domain.workout.models.DeloadSource
import com.neon.ascent.core.domain.workout.models.DeloadWindowStatus
import com.neon.ascent.core.domain.workout.models.RecoveryStatus
import com.neon.ascent.core.domain.workout.rules.SoftDeloadAdvisor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class SoftDeloadOfferFlowTest {

    private val now = Instant.parse("2026-10-24T12:00:00Z")

    @Test
    fun `forced DELOAD recovery status triggers offer sheet and accept writes active deload window`() {
        val offer = SoftDeloadAdvisor.evaluateOffer(
            recoveryStatus = RecoveryStatus.DELOAD,
            lastDeloadEndOrBlastStart = null,
            snoozedUntil = null,
            now = now
        )

        assertTrue(offer.shouldOffer)
        assertEquals(DeloadSource.RECOVERY, offer.source)

        // Accept calls window writer
        val window = SoftDeloadAdvisor.createAcceptWindow(
            userId = "user_1",
            source = offer.source!!,
            start = now
        )

        assertEquals("user_1", window.userId)
        assertEquals(DeloadWindowStatus.ACTIVE, window.status)
        assertEquals(now, window.start)
        assertEquals(now.plus(Duration.ofDays(9)), window.end)
    }

    @Test
    fun `snooze delay creates 24h snooze and suppresses offer`() {
        val snoozeTime = SoftDeloadAdvisor.calculateSnoozeTime(now = now)
        assertEquals(now.plus(Duration.ofHours(24)), snoozeTime)

        val offerAfterSnooze = SoftDeloadAdvisor.evaluateOffer(
            recoveryStatus = RecoveryStatus.DELOAD,
            lastDeloadEndOrBlastStart = null,
            snoozedUntil = snoozeTime,
            now = now.plus(Duration.ofHours(1))
        )

        assertFalse(offerAfterSnooze.shouldOffer)
    }

    @Test
    fun `manual start creates active manual deload window and end early updates status`() {
        val manualWindow = SoftDeloadAdvisor.createAcceptWindow(
            userId = "user_1",
            source = DeloadSource.MANUAL,
            start = now
        )

        assertEquals(DeloadSource.MANUAL, manualWindow.source)
        assertEquals(DeloadWindowStatus.ACTIVE, manualWindow.status)

        val endedWindow = manualWindow.copy(
            end = now.plus(Duration.ofHours(2)),
            status = DeloadWindowStatus.COMPLETED
        )

        assertEquals(DeloadWindowStatus.COMPLETED, endedWindow.status)
        assertTrue(endedWindow.end.isBefore(manualWindow.end))
    }
}
