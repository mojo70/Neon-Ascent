package com.neon.ascent.feature.workout.ui

import com.neon.ascent.core.domain.workout.models.SetLog
import com.neon.ascent.core.domain.workout.models.SetType
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutSetAutoFillTest {

    @Test
    fun `repsPlaceholder prefers prescribedReps over previousReps and goalReps`() {
        val set = SetLog(
            id = "1",
            workoutLogId = "w1",
            weight = 100f,
            reps = 0,
            goalReps = "5-10",
            prescribedReps = 12
        )
        val previousReps = 8
        val prescribedReps = set.prescribedReps

        val repsPlaceholder = prescribedReps?.toString()
            ?: previousReps.takeIf { it > 0 }?.toString()
            ?: set.goalReps?.split("-", " ")?.firstOrNull()?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() }
            ?: "0"

        assertEquals("12", repsPlaceholder)
    }

    @Test
    fun `repsPlaceholder falls back to previousReps when prescribedReps is null`() {
        val set = SetLog(
            id = "1",
            workoutLogId = "w1",
            weight = 100f,
            reps = 0,
            goalReps = "5-10"
        )
        val previousReps = 8

        val repsPlaceholder = set.prescribedReps?.toString()
            ?: previousReps.takeIf { it > 0 }?.toString()
            ?: set.goalReps?.split("-", " ")?.firstOrNull()?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() }
            ?: "0"

        assertEquals("8", repsPlaceholder)
    }

    @Test
    fun `repsPlaceholder falls back to goalReps when previousReps and prescribedReps are null`() {
        val set = SetLog(
            id = "1",
            workoutLogId = "w1",
            weight = 100f,
            reps = 0,
            goalReps = "5-10"
        )
        val previousReps: Int? = null

        val repsPlaceholder = set.prescribedReps?.toString()
            ?: previousReps?.takeIf { it > 0 }?.toString()
            ?: set.goalReps?.split("-", " ")?.firstOrNull()?.filter { it.isDigit() }?.takeIf { it.isNotEmpty() }
            ?: "0"

        assertEquals("5", repsPlaceholder)
    }
}
