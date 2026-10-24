package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.DeloadSource
import com.neon.ascent.core.domain.workout.models.DeloadWindow
import com.neon.ascent.core.domain.workout.models.DeloadWindowStatus
import com.neon.ascent.core.domain.workout.models.RecoveryStatus
import com.neon.ascent.core.domain.workout.models.SoftDeloadOffer
import java.time.Duration
import java.time.Instant

object SoftDeloadAdvisor {

    /**
     * Evaluates whether a Soft Deload offer should be presented to the user.
     *
     * Rules:
     * 1. Offer if RecoveryStatus is DELOAD or CRITICAL.
     * 2. Offer if elapsed time since last deload end (or last blast start) is >= 28 days.
     * 3. Do not offer if recovery is OPTIMAL/CAUTION and time elapsed is < 28 days.
     * 4. Do not offer if currently snoozed.
     */
    fun evaluateOffer(
        recoveryStatus: RecoveryStatus,
        lastDeloadEndOrBlastStart: Instant?,
        snoozedUntil: Instant? = null,
        now: Instant = Instant.now()
    ): SoftDeloadOffer {
        if (snoozedUntil != null && now.isBefore(snoozedUntil)) {
            return SoftDeloadOffer(
                shouldOffer = false,
                reason = "Deload offer is snoozed until $snoozedUntil"
            )
        }

        if (recoveryStatus == RecoveryStatus.DELOAD || recoveryStatus == RecoveryStatus.CRITICAL) {
            return SoftDeloadOffer(
                shouldOffer = true,
                source = DeloadSource.RECOVERY,
                reason = "Logbook recovery status is $recoveryStatus"
            )
        }

        if (lastDeloadEndOrBlastStart != null) {
            val daysSince = Duration.between(lastDeloadEndOrBlastStart, now).toDays()
            if (daysSince >= 28) {
                return SoftDeloadOffer(
                    shouldOffer = true,
                    source = DeloadSource.TIME,
                    reason = "$daysSince days since last deload or blast start (threshold is 28 days)"
                )
            } else {
                return SoftDeloadOffer(
                    shouldOffer = false,
                    reason = "Recovery is $recoveryStatus and only $daysSince days elapsed (< 28 days)"
                )
            }
        }

        return SoftDeloadOffer(
            shouldOffer = false,
            reason = "Recovery is $recoveryStatus and no previous deload date recorded"
        )
    }

    /**
     * Creates a new [DeloadWindow] starting now with a default duration of 9 days (~7-10 days window).
     */
    fun createAcceptWindow(
        userId: String,
        source: DeloadSource,
        start: Instant = Instant.now(),
        durationDays: Long = 9
    ): DeloadWindow {
        return DeloadWindow(
            userId = userId,
            start = start,
            end = start.plus(Duration.ofDays(durationDays)),
            source = source,
            status = DeloadWindowStatus.ACTIVE,
            snoozedUntil = null
        )
    }

    /**
     * Calculates the snooze timestamp for a delayed offer (default 24 hours).
     */
    fun calculateSnoozeTime(now: Instant = Instant.now(), snoozeHours: Long = 24): Instant {
        return now.plus(Duration.ofHours(snoozeHours))
    }
}
