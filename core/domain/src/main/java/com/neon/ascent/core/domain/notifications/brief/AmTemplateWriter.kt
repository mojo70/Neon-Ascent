package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.BriefAction
import com.neon.ascent.core.domain.notifications.models.BriefCopy
import com.neon.ascent.core.domain.notifications.models.BriefFacts
import com.neon.ascent.core.domain.notifications.models.BriefSessionDetails
import com.neon.ascent.core.domain.notifications.models.BriefStance
import com.neon.ascent.core.domain.notifications.models.OperationalStatus
import com.neon.ascent.core.domain.workout.models.RecoveryStatus
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object AmTemplateWriter {

    private data class DriverResult(
        val status: OperationalStatus,
        val copy: String
    )

    fun write(facts: BriefFacts, stance: BriefStance): BriefCopy {
        val nextSession = facts.nextSession
        val lastSession = facts.lastSession
        val vitals = facts.vitals

        val sleepMin = vitals.sleepMinutes
        val needMin = vitals.needMin

        // 1. Sleep Phrase
        val cardSleepPhrase = buildCardSleepPhrase(sleepMin, needMin)
        val shadeSleepPhrase = buildShadeSleepPhrase(sleepMin, needMin)

        // 2. Driver Phrase & Status Token
        val dayTypeStr = formatDayType(nextSession.dayType)
        val driverResult = resolveDriverOrDayPhrase(facts, stance, dayTypeStr)
        val driverLine = "${driverResult.status.name} — ${driverResult.copy}"

        // 3. Directive Clause (optional)
        val directiveClauseText = facts.directiveClause?.clauseText

        // 4. Lift Phrase (if top set present)
        val liftPhrase = buildLiftPhrase(lastSession)

        // Card Body
        val cardLines = listOfNotNull(
            cardSleepPhrase?.let { "$it." },
            driverLine,
            directiveClauseText,
            liftPhrase
        )
        val cardBody = cardLines.joinToString("\n")

        // Headline
        val headline = (shadeSleepPhrase ?: "NEURAL BRIEF").take(48)

        // Shade Body
        val baseDriverText = if (liftPhrase != null) {
            "$liftPhrase $driverLine"
        } else {
            driverLine
        }
        val shadeBody = listOfNotNull(baseDriverText, directiveClauseText).joinToString("\n")

        // Actions: OPEN OPS if session due; else OPEN DIRECTIVE if named Directive shown; else omit
        val actions = if (!nextSession.isWeeklyTargetMet) {
            listOf(BriefAction("OPEN OPS", "ACTION_OPEN_DECK", "DASHBOARD"))
        } else if (facts.directiveClause?.namedDirectiveId != null) {
            listOf(
                BriefAction(
                    label = "OPEN DIRECTIVE",
                    actionName = "ACTION_OPEN_DIRECTIVE",
                    type = "DIRECTIVE_DETAIL",
                    extraId = facts.directiveClause.namedDirectiveId
                )
            )
        } else {
            emptyList()
        }

        return BriefCopy(
            shadeHeadline = headline,
            shadeBody = shadeBody,
            cardBody = cardBody,
            actions = actions,
            stance = stance,
            shadeAllowed = true,
            status = driverResult.status
        )
    }

    private fun resolveDriverOrDayPhrase(
        facts: BriefFacts,
        stance: BriefStance,
        dayTypeStr: String
    ): DriverResult {
        val vitals = facts.vitals
        val nextSession = facts.nextSession
        val recoveryStatus = facts.recoveryScore?.status

        // 1. RHR last >= +8 bpm vs 7-day mean -> THROTTLE
        val rhrLast = vitals.rhrLast
        val rhrMean = vitals.rhrBaseline7d
        if (rhrLast != null && rhrMean != null && (rhrLast - rhrMean) >= 8.0) {
            val last = rhrLast.roundToInt()
            val mean = rhrMean.roundToInt()
            return DriverResult(
                status = OperationalStatus.THROTTLE,
                copy = "RHR $last (usual $mean) — keep intensity easy"
            )
        }

        // 2. HRV night <= 70 % of 7-day mean -> THROTTLE
        val hrvNight = vitals.hrvNight
        val hrvMean = vitals.hrvBaseline7d
        if (hrvNight != null && hrvMean != null && hrvMean > 0 && hrvNight <= 0.70 * hrvMean) {
            val night = hrvNight.roundToInt()
            val mean = hrvMean.roundToInt()
            return DriverResult(
                status = OperationalStatus.THROTTLE,
                copy = "HRV $night vs $mean — recovery day"
            )
        }

        // 3. SEED band GROUND or HOLD and heavy/C/ME scheduled -> GROUNDED
        val isSeedGroundOrHold = vitals.seedBand in listOf("GROUND", "HOLD") || (vitals.seed != null && vitals.seed < 65)
        if (isSeedGroundOrHold && nextSession.isHeavyOrC) {
            return DriverResult(
                status = OperationalStatus.GROUNDED,
                copy = "Morning tank low — hold the heavy work"
            )
        }

        // 4. Recovery CRITICAL or DELOAD -> GROUNDED
        if (recoveryStatus == RecoveryStatus.CRITICAL || recoveryStatus == RecoveryStatus.DELOAD) {
            return DriverResult(
                status = OperationalStatus.GROUNDED,
                copy = "Recovery says deload"
            )
        }

        // 5. Week hit rate complete -> NOMINAL
        if (nextSession.isWeeklyTargetMet && nextSession.scheduledThisWeek > 0) {
            val done = nextSession.completedThisWeek
            val total = nextSession.scheduledThisWeek.coerceAtLeast(done)
            return DriverResult(
                status = OperationalStatus.NOMINAL,
                copy = "Week is already $done/$total. Easy day."
            )
        }

        // 6. PICKUP open -> NOMINAL
        if (stance == BriefStance.PICKUP) {
            return DriverResult(
                status = OperationalStatus.NOMINAL,
                copy = "You still have $dayTypeStr open."
            )
        }

        // 7. PUSH + scheduled + no prior driver -> OVERDRIVE
        if (stance == BriefStance.PUSH && nextSession.scheduled) {
            return DriverResult(
                status = OperationalStatus.OVERDRIVE,
                copy = "$dayTypeStr is up."
            )
        }

        // 8. Else -> NOMINAL
        val fallbackCopy = when (stance) {
            BriefStance.DELOAD -> "Soft deload on the log. Keep it light."
            BriefStance.LIGHT_DAY -> "Scheduled recovery day."
            BriefStance.MISSING_DATA -> "No overnight telemetry."
            else -> "$dayTypeStr today."
        }
        return DriverResult(
            status = OperationalStatus.NOMINAL,
            copy = fallbackCopy
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
