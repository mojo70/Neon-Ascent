package com.neon.ascent.feature.dashboard

import com.neon.ascent.domain.model.Goal
import com.neon.ascent.domain.model.Task
import com.neon.ascent.domain.model.UserStory
import com.neon.ascent.model.BioAgeResult

data class DashboardUiState(
    val userStory: UserStory = UserStory(),
    val cyberLoreSnippet: String = "",
    val activeGoals: List<Goal> = emptyList(),
    val todayTasks: List<Task> = emptyList(),
    val bioAgeResult: BioAgeResult? = null,
    val totalHabitDays: Int = 0,
    val isLoading: Boolean = true
)
