package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.BriefCopy
import com.neon.ascent.core.domain.notifications.models.BriefFacts
import com.neon.ascent.core.domain.notifications.models.BriefStance
import java.util.Locale

object TemplateCopyWriter {
    fun write(facts: BriefFacts, stance: BriefStance): BriefCopy {
        val headline = when (stance) {
            BriefStance.PUSH -> "⚡ PUSH // SYSTEMS_OPTIMAL"
            BriefStance.HOLD -> "⚡ HOLD // STEADY_STATE"
            BriefStance.RECOVER -> "⚡ RECOVER // PROTOCOL_ADAPT"
            BriefStance.MISSING_DATA -> "⚡ SYNC // DATA_REQUIRED"
        }

        val body = buildString {
            // 1. Session Context
            facts.lastSession?.let { session ->
                val dayType = session.protocolDayType?.name ?: "GENERAL"
                append("Last ${session.protocol.displayName} ($dayType) complete. ")
                if (facts.topSets.isNotEmpty()) {
                    val top = facts.topSets.first()
                    append("Top set: ${top.exerciseName} at ${top.weight.toInt()} lbs. ")
                }
            } ?: append("No recent session detected. ")

            // 2. Recovery / Biometrics
            if (facts.hrvCurrent != null) {
                append("HRV is ${facts.hrvCurrent.toInt()} vs 7d mean of ${facts.hrvMean7d?.toInt() ?: "N/A"}. ")
            }

            // 3. Actionable Stance
            when (stance) {
                BriefStance.PUSH -> {
                    val next = facts.nextDayType ?: "next session"
                    append("Recovery is high. Prep for $next with a potential weight jump.")
                }
                BriefStance.HOLD -> {
                    append("Systems stable. Maintain current intensity on the next cycle.")
                }
                BriefStance.RECOVER -> {
                    append("Fatigue detected. Pivot to Soft Deload or active recovery protocols.")
                }
                BriefStance.MISSING_DATA -> {
                    append("Telemetric gaps found. Open OPS to sync biometric baseline.")
                }
            }
        }.trim()

        return BriefCopy(headline, body, stance)
    }
}
