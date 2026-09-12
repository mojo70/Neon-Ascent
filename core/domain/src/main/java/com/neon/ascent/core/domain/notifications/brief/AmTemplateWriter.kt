package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.BriefAction
import com.neon.ascent.core.domain.notifications.models.BriefCopy
import com.neon.ascent.core.domain.notifications.models.BriefFacts
import com.neon.ascent.core.domain.notifications.models.BriefNextSession
import com.neon.ascent.core.domain.notifications.models.BriefSessionDetails
import com.neon.ascent.core.domain.notifications.models.BriefStance
import java.util.Locale
import kotlin.math.abs

object AmTemplateWriter {
    fun write(facts: BriefFacts, stance: BriefStance): BriefCopy {
        val nextSession = facts.nextSession
        val lastSession = facts.lastSession
        val vitals = facts.vitals

        val sleepMin = vitals.sleepMinutes
        val needMin = vitals.needMin

        // 1. Sleep Phrase
        val cardSleepPhrase = buildCardSleepPhrase(sleepMin, needMin)
        val shadeSleepPhrase = buildShadeSleepPhrase(sleepMin, needMin)

        // 2. Week or Day Phrase
        val dayTypeStr = formatDayType(nextSession.dayType)
        val cardWeekOrDay = buildCardWeekOrDayPhrase(nextSession, stance, dayTypeStr)
        val shadeWeekOrDay = buildShadeWeekOrDayPhrase(nextSession, stance, dayTypeStr)

        // 3. Lift Phrase (if top set present)
        val liftPhrase = buildLiftPhrase(lastSession)

        // Card Body
        val cardLines = listOfNotNull(
            cardSleepPhrase?.let { "$it." },
            "$cardWeekOrDay.",
            liftPhrase
        )
        val cardBody = cardLines.joinToString("\n")

        // Headline
        val headline = (shadeSleepPhrase ?: "NEURAL BRIEF").take(48)

        // Shade Body
        val shadeBody = if (nextSession.isWeeklyTargetMet) {
            "$headline\n$shadeWeekOrDay"
        } else if (liftPhrase != null) {
            "$liftPhrase $shadeWeekOrDay"
        } else {
            shadeWeekOrDay
        }

        // Actions: omitted if week is complete, else OPEN OPS
        val actions = if (nextSession.isWeeklyTargetMet) {
            emptyList()
        } else {
            listOf(BriefAction("OPEN OPS", "ACTION_OPEN_DECK", "DASHBOARD"))
        }

        return BriefCopy(
            shadeHeadline = headline,
            shadeBody = shadeBody,
            cardBody = cardBody,
            actions = actions,
            stance = stance,
            shadeAllowed = true
        )
    }

    private fun buildCardSleepPhrase(sleepMin: Long?, needMin: Long): String? {
        if (sleepMin == null || sleepMin <= 0) return null
        val diffMin = sleepMin - needMin
        val gotStr = formatHumanHm(sleepMin)
        val needStr = formatHumanHm(needMin)

        return when {
            diffMin <= -45 -> "Short night — $gotStr of $needStr"
            diffMin >= 45 -> "Long night — $gotStr"
            abs(diffMin) <= 20 -> "Slept $gotStr (need $needStr)"
            else -> "Short night — $gotStr of $needStr"
        }
    }

    private fun buildShadeSleepPhrase(sleepMin: Long?, needMin: Long): String? {
        if (sleepMin == null || sleepMin <= 0) return null
        val diffMin = sleepMin - needMin
        val gotStr = formatShadeHm(sleepMin)
        val needStr = formatShadeHm(needMin)

        return when {
            diffMin <= -45 -> "Short night — $gotStr of $needStr"
            diffMin >= 45 -> "Long night — $gotStr"
            abs(diffMin) <= 20 -> "Slept $gotStr (need $needStr)"
            else -> "Short night — $gotStr of $needStr"
        }
    }

    private fun buildCardWeekOrDayPhrase(
        nextSession: BriefNextSession,
        stance: BriefStance,
        dayTypeStr: String
    ): String {
        if (nextSession.isWeeklyTargetMet) {
            val done = nextSession.completedThisWeek
            val total = nextSession.scheduledThisWeek.coerceAtLeast(done)
            return "Week is already $done/$total. Easy day"
        }

        return when (stance) {
            BriefStance.PICKUP -> "You still have $dayTypeStr open"
            BriefStance.HOLD -> "$dayTypeStr today — keep it easy"
            BriefStance.PUSH -> "$dayTypeStr is up"
            BriefStance.RECOVER -> "$dayTypeStr today — keep it easy"
            BriefStance.DELOAD -> "Soft deload on the log. Keep it light"
            BriefStance.LIGHT_DAY -> "Scheduled recovery day"
            BriefStance.MISSING_DATA -> "No overnight telemetry"
        }
    }

    private fun buildShadeWeekOrDayPhrase(
        nextSession: BriefNextSession,
        stance: BriefStance,
        dayTypeStr: String
    ): String {
        if (nextSession.isWeeklyTargetMet) {
            return "Week is done. Easy day."
        }

        return when (stance) {
            BriefStance.PICKUP -> "You still have $dayTypeStr open."
            BriefStance.HOLD -> "$dayTypeStr today — keep it easy."
            BriefStance.PUSH -> "$dayTypeStr is up."
            BriefStance.RECOVER -> "$dayTypeStr today — keep it easy."
            BriefStance.DELOAD -> "Soft deload on the log. Keep it light."
            BriefStance.LIGHT_DAY -> "Scheduled recovery day."
            BriefStance.MISSING_DATA -> "No overnight telemetry."
        }
    }

    private fun buildLiftPhrase(lastSession: BriefSessionDetails?): String? {
        if (lastSession == null || lastSession.topSets.isEmpty()) return null
        val top = lastSession.topSets.first()
        val name = cleanExerciseName(top.exerciseName)
        return "${top.weight.toInt()} $name is in the book."
    }

    private fun cleanExerciseName(name: String): String {
        val clean = name.replace(Regex("(?i)\\s*\\((barbell|dumbbell|cable|machine)\\)"), "").trim().lowercase(Locale.US)
        return when (clean) {
            "back squat" -> "squat"
            "bench press" -> "bench"
            "overhead press" -> "press"
            else -> clean
        }
    }

    private fun formatDayType(dayType: String?): String {
        val clean = dayType?.trim()?.uppercase(Locale.US) ?: return "Training"
        return when (clean) {
            "A", "PUSH" -> "Push"
            "B", "PULL" -> "Pull"
            "C", "LEGS" -> "Legs"
            else -> dayType.trim()
        }
    }

    private fun formatHumanHm(totalMinutes: Long): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
    }

    private fun formatShadeHm(totalMinutes: Long): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return if (mins > 0) "${hours}h${mins.toString().padStart(2, '0')}" else "${hours}h"
    }
}
