package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.BriefAction
import com.neon.ascent.core.domain.notifications.models.BriefCopy
import com.neon.ascent.core.domain.notifications.models.BriefFacts
import com.neon.ascent.core.domain.notifications.models.BriefStance

object PmTemplateWriter {
    fun write(facts: BriefFacts, stance: BriefStance): BriefCopy {
        val vitals = facts.vitals

        val headline = buildHeadline(facts)
        val shadeBody = buildShadeBody(facts, stance)
        val cardBody = buildCardBody(facts, stance)
        val actions = listOf(BriefAction("OPEN DECK", "ACTION_OPEN_DECK", "DASHBOARD"))

        val chargeNow = vitals.chargeNow
        val seed = vitals.seed
        val isChargeLowOrDropped = chargeNow != null && (chargeNow <= 35 || (seed != null && (seed - chargeNow) >= 30))
        val isNeedTrigger = isChargeLowOrDropped || stance == BriefStance.PICKUP || stance == BriefStance.DELOAD || stance == BriefStance.RECOVER

        return BriefCopy(
            shadeHeadline = headline,
            shadeBody = shadeBody,
            cardBody = cardBody,
            actions = actions,
            stance = stance,
            shadeAllowed = isNeedTrigger
        )
    }

    private fun buildHeadline(facts: BriefFacts): String {
        val vitals = facts.vitals
        val schedule = facts.schedule
        val lightsOutStr = schedule.lightsOut.toString()
        val needStr = formatHoursMinutes(vitals.needMin)

        val chargePart = if (vitals.chargeNow != null) " · CHARGE ${vitals.chargeNow}" else ""
        val candidate = "LIGHTS $lightsOutStr for $needStr$chargePart"
        return candidate.take(48)
    }

    private fun buildShadeBody(facts: BriefFacts, stance: BriefStance): String {
        val vitals = facts.vitals
        val chargeNow = vitals.chargeNow
        val seed = vitals.seed
        val needStr = formatHoursMinutes(vitals.needMin)

        return when {
            chargeNow != null && seed != null && (seed - chargeNow) >= 30 -> {
                val delta = seed - chargeNow
                "Tank dropped $delta from this morning’s seed. Sleep soon if you still want $needStr."
            }
            chargeNow != null && chargeNow <= 35 -> {
                "Tank low at $chargeNow%. Sleep soon if you still want $needStr."
            }
            stance == BriefStance.PICKUP -> {
                "Evening window open for missed session if energy allows."
            }
            else -> {
                "Target lights-out at ${facts.schedule.lightsOut} to meet $needStr sleep need."
            }
        }
    }

    private fun buildCardBody(facts: BriefFacts, stance: BriefStance): String {
        val vitals = facts.vitals
        val schedule = facts.schedule

        val line1 = "LIGHTS ${schedule.lightsOut} for ${formatHoursMinutes(vitals.needMin)} · wake ${schedule.activeTargetWake}"
        val line2 = if (vitals.chargeNow != null) {
            val seedPart = if (vitals.seed != null) " · SEED was ${vitals.seed}" else ""
            "CHARGE ${vitals.chargeNow}$seedPart"
        } else "CHARGE pending"

        val line3 = "Evening stance: $stance."

        return listOf(line1, line2, line3).joinToString("\n")
    }

    private fun formatHoursMinutes(totalMinutes: Long): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return if (mins > 0) "${hours}h${mins.toString().padStart(2, '0')}" else "${hours}h"
    }
}
