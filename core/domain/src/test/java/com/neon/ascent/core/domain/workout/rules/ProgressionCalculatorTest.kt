package com.neon.ascent.core.domain.workout.rules

import com.neon.ascent.core.domain.workout.models.MovementType
import com.neon.ascent.core.domain.workout.models.ProgressionMode
import com.neon.ascent.core.domain.workout.models.UserWorkoutProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgressionCalculatorTest {

    private val baseProfile = UserWorkoutProfile(
        userId = "test_user",
        weightIncrementUpperCompound = 5.0f,
        weightIncrementLowerCompound = 10.0f,
        weightIncrementIsolation = 5.0f,
        roundingIncrement = 5.0f
    )

    @Test
    fun `fixed mode upper body compound moves up by 5 lbs`() {
        val next = ProgressionCalculator.calculateNextWeight(
            currentWeight = 135f,
            movementType = MovementType.COMPOUND_UPPER,
            profile = baseProfile.copy(progressionMode = ProgressionMode.FIXED_INCREMENT)
        )
        assertEquals(140f, next, 0.01f)
    }

    @Test
    fun `fixed mode lower body compound moves up by 10 lbs`() {
        val next = ProgressionCalculator.calculateNextWeight(
            currentWeight = 225f,
            movementType = MovementType.QUAD_DOMINANT,
            profile = baseProfile.copy(progressionMode = ProgressionMode.FIXED_INCREMENT)
        )
        assertEquals(235f, next, 0.01f)
    }

    @Test
    fun `fixed mode isolation moves up by 5 lbs`() {
        val next = ProgressionCalculator.calculateNextWeight(
            currentWeight = 30f,
            movementType = MovementType.ISOLATION_UPPER,
            profile = baseProfile.copy(progressionMode = ProgressionMode.FIXED_INCREMENT)
        )
        assertEquals(35f, next, 0.01f)
    }

    @Test
    fun `percentage mode rounds to nearest standard 5 lbs plate step`() {
        val profile = baseProfile.copy(
            progressionMode = ProgressionMode.PERCENTAGE,
            progressionPercentage = 2.5f,
            roundingIncrement = 5.0f
        )
        // 185 + 2.5% = 189.625 -> rounded to nearest 5 lbs = 190.0
        val next = ProgressionCalculator.calculateNextWeight(
            currentWeight = 185f,
            movementType = MovementType.COMPOUND_UPPER,
            profile = profile
        )
        assertEquals(190f, next, 0.01f)
    }

    @Test
    fun `percentage mode rounds to 2,5 lbs micro plates when configured`() {
        val profile = baseProfile.copy(
            progressionMode = ProgressionMode.PERCENTAGE,
            progressionPercentage = 2.5f,
            roundingIncrement = 2.5f
        )
        // 100 + 2.5% = 102.5 -> rounded to nearest 2.5 lbs = 102.5
        val next = ProgressionCalculator.calculateNextWeight(
            currentWeight = 100f,
            movementType = MovementType.COMPOUND_UPPER,
            profile = profile
        )
        assertEquals(102.5f, next, 0.01f)
    }
}
