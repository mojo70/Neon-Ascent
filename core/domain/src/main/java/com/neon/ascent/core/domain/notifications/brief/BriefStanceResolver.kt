package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.BriefFacts
import com.neon.ascent.core.domain.notifications.models.BriefStance
import com.neon.ascent.core.domain.workout.models.RecoveryStatus
import java.time.LocalTime
import java.time.ZoneId

object BriefStanceResolver {
    fun resolve(facts: BriefFacts, nowLocalTime: LocalTime = LocalTime.now(ZoneId.systemDefault())): BriefStance {
        val recoveryStatus = facts.recoveryScore?.status ?: RecoveryStatus.OPTIMAL
        val vitals = facts.vitals
        val nextSession = facts.nextSession
        val lastSession = facts.lastSession

        // 1. Recovery status CRITICAL or DELOAD
        if (recoveryStatus == RecoveryStatus.CRITICAL || recoveryStatus == RecoveryStatus.DELOAD) {
            return BriefStance.DELOAD
        }

        // 2. SEED GROUND/HOLD and today is heavy / C / ME
        val isSeedGroundOrHold = vitals.seedBand in listOf("GROUND", "HOLD") || (vitals.seed != null && vitals.seed < 65)
        if (isSeedGroundOrHold && nextSession.isHeavyOrC) {
            return BriefStance.HOLD
        }

        // 3. HRV_N <= 70% of 7-day mean AND last session was heavy legs / high RPE
        val hrv = vitals.hrvNight
        val hrvBase = vitals.hrvBaseline7d
        if (hrv != null && hrvBase != null && hrvBase > 0 && hrv <= 0.70 * hrvBase) {
            return BriefStance.RECOVER
        }

        // 4. Sleep / SANCTUM wreck and next session is C / ME / heavy
        val isSleepWreck = (vitals.sleepMinutes != null && vitals.sleepMinutes < (0.75 * vitals.needMin).toLong()) ||
                (vitals.sanctumScore != null && vitals.sanctumScore < 50)
        if (isSleepWreck && nextSession.isHeavyOrC) {
            return BriefStance.HOLD
        }

        // 5. PICKUP gates
        val isSeedOkForPickup = vitals.seedBand in listOf("CLEAR", "WATCH") || (vitals.seed != null && vitals.seed >= 65) || (vitals.seed == null && vitals.seedBand == null)
        val isSanctumOkForPickup = vitals.sanctumScore == null || vitals.sanctumScore >= 60 || (vitals.sleepMinutes != null && vitals.sleepMinutes >= (0.85 * vitals.needMin).toLong())
        val isRecoveryOkForPickup = recoveryStatus != RecoveryStatus.CAUTION
        val isBefore1600 = nowLocalTime.isBefore(LocalTime.of(16, 0))
        val noSessionToday = !facts.dataQuality.hasSession24h

        if (facts.isWeekShort && isSeedOkForPickup && isSanctumOkForPickup && isRecoveryOkForPickup && noSessionToday && isBefore1600) {
            return BriefStance.PICKUP
        }

        // 6. Weight jump due + OPTIMAL recovery + SEED CLEAR/WATCH
        if (nextSession.weightJumpsDue.isNotEmpty() && recoveryStatus == RecoveryStatus.OPTIMAL && (vitals.seedBand in listOf("CLEAR", "WATCH") || vitals.seedBand == null)) {
            return BriefStance.PUSH
        }

        // 7. Scheduled rest / no miss + vitals fine
        if (!nextSession.scheduled && !facts.isWeekShort && vitals.sanctumScore != null && vitals.sanctumScore >= 60) {
            return BriefStance.LIGHT_DAY
        }

        // 8. No night and no session in 48h
        val noSleep = vitals.sleepMinutes == null && !facts.dataQuality.hasSleep
        val noSession = lastSession == null
        if (noSleep && noSession) {
            return BriefStance.MISSING_DATA
        }

        // 9. Else
        return BriefStance.HOLD
    }
}
