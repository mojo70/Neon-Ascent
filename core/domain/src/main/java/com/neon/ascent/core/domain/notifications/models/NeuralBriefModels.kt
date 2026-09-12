package com.neon.ascent.core.domain.notifications.models

import com.neon.ascent.core.domain.workout.models.RecoveryScore
import com.neon.ascent.core.domain.workout.models.WorkoutSession
import java.time.Instant
import java.time.LocalTime

enum class BriefSlot {
    AM, PM
}

enum class BriefStance {
    DELOAD,
    HOLD,
    RECOVER,
    PICKUP,
    PUSH,
    LIGHT_DAY,
    MISSING_DATA
}

data class TopSet(
    val exerciseName: String,
    val weight: Float,
    val reps: Int,
    val isRestPause: Boolean = false
)

data class BriefSessionDetails(
    val id: String,
    val date: Instant,
    val dayType: String?,
    val protocolName: String,
    val topSets: List<TopSet>,
    val notes: String? = null,
    val isCausal: Boolean = false
)

data class BriefVitals(
    val sleepMinutes: Long? = null,
    val needMin: Long = 450L,
    val sanctumScore: Int? = null,
    val sanctumTier: Int? = null,
    val sanctumBand: String? = null,
    val seed: Int? = null,
    val seedBand: String? = null,
    val chargeNow: Int? = null,
    val hrvNight: Double? = null,
    val hrvBaseline7d: Double? = null,
    val rhrLast: Double? = null,
    val rhrBaseline7d: Double? = null,
    val stepsSoFar: Long = 0L
)

data class BriefSchedule(
    val targetWakeWd: LocalTime? = null,
    val targetWakeWe: LocalTime? = null,
    val derivedWakeWd: LocalTime = LocalTime.of(7, 0),
    val derivedWakeWe: LocalTime = LocalTime.of(8, 0),
    val lightsOut: LocalTime = LocalTime.of(22, 30)
) {
    val activeTargetWake: LocalTime
        get() = targetWakeWd ?: derivedWakeWd
}

data class BriefNextSession(
    val scheduled: Boolean = false,
    val dayType: String? = null,
    val liftsDue: List<String> = emptyList(),
    val weightJumpsDue: List<String> = emptyList(),
    val isHeavyOrC: Boolean = false,
    val hasSessionToday: Boolean = false,
    val isWeeklyTargetMet: Boolean = false,
    val completedThisWeek: Int = 0,
    val scheduledThisWeek: Int = 3
)

data class BriefDataQuality(
    val hasSleep: Boolean = false,
    val hasHrv: Boolean = false,
    val hasSession24h: Boolean = false,
    val sources: List<String> = emptyList()
)

data class BriefFacts(
    val slot: BriefSlot,
    val generatedAt: Instant = Instant.now(),
    val lastSession: BriefSessionDetails? = null,
    val recoveryScore: RecoveryScore? = null,
    val vitals: BriefVitals = BriefVitals(),
    val schedule: BriefSchedule = BriefSchedule(),
    val nextSession: BriefNextSession = BriefNextSession(),
    val dataQuality: BriefDataQuality = BriefDataQuality(),
    val leadMetric: String? = null,
    val leadValue: String? = null,
    val isWeekShort: Boolean = false
) {
    val factsHash: String
        get() = "${slot.name}_${leadMetric ?: "none"}_${leadValue ?: "none"}_${lastSession?.id}_${vitals.sanctumScore}_${vitals.seed}_${vitals.chargeNow}"
}

data class BriefAction(
    val label: String,
    val actionName: String,
    val type: String = "DASHBOARD"
)

data class BriefCopy(
    val shadeHeadline: String,
    val shadeBody: String,
    val cardBody: String,
    val actions: List<BriefAction>,
    val stance: BriefStance,
    val shadeAllowed: Boolean = true
) {
    // Compatibility accessors for existing consumers
    val headline: String get() = shadeHeadline
    val body: String get() = shadeBody
}
