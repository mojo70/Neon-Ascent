package com.neon.ascent.core.domain.workout.models

import java.time.Instant

data class ExerciseAccomplishments(
    val exerciseId: String,
    val heaviestWeight: Float = 0f,
    val heaviestWeightReps: Int = 0,
    val heaviestWeightDate: Instant? = null,
    val maxEstimatedOneRepMax: Float = 0f,
    val maxOneRepMaxWeight: Float = 0f,
    val maxOneRepMaxReps: Int = 0,
    val maxOneRepMaxDate: Instant? = null,
    val maxSessionVolume: Float = 0f,
    val maxSessionVolumeDate: Instant? = null,
    val maxRepsAtTopWeight: Int = 0,
    val topWeightForReps: Float = 0f,
    val bestClusterReps: Int = 0,
    val bestClusterWeight: Float = 0f,
    val bestClusterDate: Instant? = null
)
