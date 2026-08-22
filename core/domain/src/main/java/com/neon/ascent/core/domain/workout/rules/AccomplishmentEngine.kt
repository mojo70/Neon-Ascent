package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.ExerciseAccomplishments
import com.neon.ascent.core.domain.workout.models.SetLog
import com.neon.ascent.core.domain.workout.models.SetType
import java.time.Instant

object AccomplishmentEngine {

    /**
     * Estimates 1 Rep Max using the standard Epley formula: Weight * (1 + Reps / 30).
     * For 1 rep, it returns the exact weight.
     */
    fun calculateEstimatedOneRepMax(weight: Float, reps: Int): Float {
        if (weight <= 0f || reps <= 0) return 0f
        if (reps == 1) return weight
        return weight * (1f + (reps / 30f))
    }

    /**
     * Evaluates a list of completed sets for a given exercise in a session and produces updated accomplishments.
     */
    fun evaluateAccomplishments(
        exerciseId: String,
        currentAccomplishments: ExerciseAccomplishments?,
        completedSets: List<SetLog>,
        sessionDate: Instant = Instant.now()
    ): ExerciseAccomplishments {
        var base = currentAccomplishments ?: ExerciseAccomplishments(exerciseId = exerciseId)
        val validSets = completedSets.filter { it.isCompleted && it.weight > 0f }
        if (validSets.isEmpty()) return base

        // 1. Session Volume (working sets and non-warmup sets primarily, or all completed sets)
        val sessionVolume = validSets.filter { it.type != SetType.WARMUP }.sumOf { (it.weight * it.reps).toDouble() }.toFloat()
        if (sessionVolume > base.maxSessionVolume) {
            base = base.copy(
                maxSessionVolume = sessionVolume,
                maxSessionVolumeDate = sessionDate
            )
        }

        // 2. Heaviest Weight & Max Reps at top weights
        validSets.forEach { set ->
            if (set.type != SetType.WARMUP) {
                // Heaviest weight
                if (set.weight > base.heaviestWeight) {
                    base = base.copy(
                        heaviestWeight = set.weight,
                        heaviestWeightReps = set.reps,
                        heaviestWeightDate = sessionDate,
                        topWeightForReps = set.weight,
                        maxRepsAtTopWeight = set.reps
                    )
                } else if (set.weight == base.heaviestWeight && set.reps > base.heaviestWeightReps) {
                    base = base.copy(
                        heaviestWeightReps = set.reps,
                        heaviestWeightDate = sessionDate,
                        maxRepsAtTopWeight = set.reps
                    )
                }

                // Estimated 1RM
                val e1rm = calculateEstimatedOneRepMax(set.weight, set.reps)
                if (e1rm > base.maxEstimatedOneRepMax) {
                    base = base.copy(
                        maxEstimatedOneRepMax = e1rm,
                        maxOneRepMaxWeight = set.weight,
                        maxOneRepMaxReps = set.reps,
                        maxOneRepMaxDate = sessionDate
                    )
                }
            }
        }

        // 3. Cluster Performance (Rest-Pause)
        val clusterSets = validSets.filter { it.type == SetType.REST_PAUSE && it.clusterMiniSetIndex != null }
        if (clusterSets.isNotEmpty()) {
            val totalReps = clusterSets.sumOf { it.reps }
            val clusterWeight = clusterSets.first().weight
            if (clusterWeight > base.bestClusterWeight || (clusterWeight == base.bestClusterWeight && totalReps > base.bestClusterReps)) {
                base = base.copy(
                    bestClusterReps = totalReps,
                    bestClusterWeight = clusterWeight,
                    bestClusterDate = sessionDate
                )
            }
        }

        return base
    }
}
