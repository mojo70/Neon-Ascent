package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.Exercise
import com.neon.ascent.core.domain.workout.models.Implement
import com.neon.ascent.core.domain.workout.models.MovementType
import com.neon.ascent.core.domain.workout.models.Stance
import org.junit.Assert.assertEquals
import org.junit.Test

class ExerciseRankingTest {

    @Test
    fun `rankAlternatives ranks same family first, then same slot movement type`() {
        val target = Exercise(
            id = "bench_barbell",
            name = "Bench Press (Barbell)",
            description = "",
            cues = emptyList(),
            muscleGroups = listOf("Chest"),
            equipment = listOf("Barbell"),
            movementType = MovementType.COMPOUND_UPPER,
            familyId = "bench_press",
            familyName = "Bench Press",
            implement = Implement.BARBELL
        )

        val sameFamilyDb = Exercise(
            id = "bench_dumbbell",
            name = "Bench Press (Dumbbell)",
            description = "",
            cues = emptyList(),
            muscleGroups = listOf("Chest"),
            equipment = listOf("Dumbbell"),
            movementType = MovementType.COMPOUND_UPPER,
            familyId = "bench_press",
            familyName = "Bench Press",
            implement = Implement.DUMBBELL
        )

        val sameSlotOtherFamily = Exercise(
            id = "dip_chest",
            name = "Chest Dip",
            description = "",
            cues = emptyList(),
            muscleGroups = listOf("Chest"),
            equipment = listOf("Bodyweight"),
            movementType = MovementType.COMPOUND_UPPER,
            familyId = "dip",
            familyName = "Dip",
            implement = Implement.BODYWEIGHT
        )

        val unrelatedExercise = Exercise(
            id = "bicep_curl",
            name = "Barbell Curl",
            description = "",
            cues = emptyList(),
            muscleGroups = listOf("Biceps"),
            equipment = listOf("Barbell"),
            movementType = MovementType.ISOLATION_UPPER,
            familyId = "curl",
            familyName = "Curl",
            implement = Implement.BARBELL
        )

        val candidates = listOf(unrelatedExercise, sameSlotOtherFamily, sameFamilyDb, target)

        val ranked = ExerciseRanking.rankAlternatives(target, candidates)

        // Target should be excluded
        assertEquals(3, ranked.size)
        // 1st: same family (Dumbbell Bench Press)
        assertEquals("bench_dumbbell", ranked[0].id)
        // 2nd: same slot (Chest Dip)
        assertEquals("dip_chest", ranked[1].id)
        // 3rd: unrelated (Barbell Curl)
        assertEquals("bicep_curl", ranked[2].id)
    }
}
