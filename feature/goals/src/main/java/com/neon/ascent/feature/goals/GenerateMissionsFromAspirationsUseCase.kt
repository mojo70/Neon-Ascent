package com.neon.ascent.feature.goals

import com.neon.ascent.core.domain.Goal
import com.neon.ascent.core.domain.GoalRepository
import javax.inject.Inject

class GenerateMissionsFromAspirationsUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    suspend fun generate() {
        // 1. Get all aspirations
        // 2. For each aspiration, use AI or procedural logic to generate missions
        // 3. Save missions to repository
    }
}
