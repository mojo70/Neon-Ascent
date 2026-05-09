package com.neon.ascent.feature.habits

import com.neon.ascent.core.domain.GoalRepository
import javax.inject.Inject

class LinkHabitToMissionUseCase @Inject constructor(
    private val goalRepository: GoalRepository
) {
    suspend fun link(habitId: String, missionId: String) {
        goalRepository.linkHabitToMission(habitId, missionId)
    }
}
