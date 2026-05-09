package com.neon.ascent.core.domain.goals.usecases

import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.goals.models.CompletionData
import javax.inject.Inject

class CompleteHabitAndUpdateGoalsUseCase @Inject constructor(
    private val goalRepository: GoalRepository,
    private val specialRepository: SpecialRepository
) {
    suspend operator fun invoke(habitId: String, completionData: CompletionData) {
        // 1. Mark habit complete + update streak
        goalRepository.completeHabit(habitId, completionData)
        
        // 2. Update linked S.P.E.C.I.A.L. attributes
        // This will be expanded with logic to map habit completion to XP/percentile gains
        
        // 3. Check & advance parent Missions + Aspirations
        
        // 4. Trigger notifications + level-up effects on avatar (handled via repository flows)
    }
}
