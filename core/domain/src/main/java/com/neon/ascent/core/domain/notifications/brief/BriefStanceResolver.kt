package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.BriefFacts
import com.neon.ascent.core.domain.notifications.models.BriefStance
import com.neon.ascent.core.domain.workout.models.RecoveryStatus

object BriefStanceResolver {
    fun resolve(facts: BriefFacts): BriefStance {
        if (facts.lastSession == null && facts.hrvCurrent == null) return BriefStance.MISSING_DATA

        val status = facts.recoveryScore.status
        
        return when (status) {
            RecoveryStatus.CRITICAL, RecoveryStatus.DELOAD -> BriefStance.RECOVER
            RecoveryStatus.CAUTION -> BriefStance.HOLD
            RecoveryStatus.OPTIMAL -> {
                // Biometric override
                val hrvCurrent = facts.hrvCurrent
                val hrvMean = facts.hrvMean7d
                if (hrvCurrent != null && hrvMean != null && hrvCurrent < hrvMean * 0.85) {
                    BriefStance.HOLD
                } else {
                    BriefStance.PUSH
                }
            }
        }
    }
}
