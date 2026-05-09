package com.neon.ascent.feature.habits

import com.neon.ascent.core.domain.GoalRepository
import javax.inject.Inject

class CompleteHabitAndUpdateGoalsUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    suspend operator fun invoke(habitId: String) {
        // 1. Mark habit as completed for the day
        goalRepository.completeHabit(habitId)
        
        // 2. Update related goals/missions progress
        // This would involve more complex logic in a real implementation
        // like finding linked missions and updating their progress based on habit completion.
    }
}
