package com.neon.ascent.core.domain.notifications.models

import com.neon.ascent.core.domain.workout.models.RecoveryScore
import com.neon.ascent.core.domain.workout.models.WorkoutSession
import java.time.Instant

data class BriefFacts(
    val lastSession: WorkoutSession?,
    val topSets: List<TopSet>,
    val recoveryScore: RecoveryScore,
    val nextDayType: String?,
    val hrvCurrent: Double?,
    val hrvMean7d: Double?,
    val sleepHoursCurrent: Double?,
    val sleepHoursMean7d: Double?,
    val rhrCurrent: Double?,
    val rhrMean7d: Double?,
    val date: Instant = Instant.now()
) {
    val factsHash: String
        get() = "${lastSession?.id}_${recoveryScore.totalScore}_${hrvCurrent}_${sleepHoursCurrent}_${rhrCurrent}"
}

data class TopSet(
    val exerciseName: String,
    val weight: Float,
    val reps: Int,
    val isRestPause: Boolean = false
)

enum class BriefStance {
    PUSH, HOLD, RECOVER, MISSING_DATA
}

data class BriefCopy(
    val headline: String,
    val body: String,
    val stance: BriefStance
)
