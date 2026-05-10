package com.neon.ascent.feature.habits.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.goals.models.CompletionData
import com.neon.ascent.core.domain.goals.models.Habit
import com.neon.ascent.core.domain.goals.models.Mission
import com.neon.ascent.core.domain.goals.usecases.CompleteHabitAndUpdateGoalsUseCase
import com.neon.ascent.core.domain.model.SpecialType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val completeHabitUseCase: CompleteHabitAndUpdateGoalsUseCase
) : ViewModel() {

    val habits: StateFlow<List<Habit>> = goalRepository.getHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayMissions: StateFlow<List<Mission>> = goalRepository.getActiveMissions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayProgress: StateFlow<String> = combine(habits, todayMissions) { h, m ->
        val completedHabits = h.count { it.progress.current >= 1f }
        val totalHabits = h.size
        val missionProgress = if (m.isEmpty()) 0 else (m.sumOf { it.progress.current.toDouble() } / m.size * 100).toInt()
        "${completedHabits}/${totalHabits} habits • ${missionProgress}% missions"
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")

    fun completeHabit(habitId: String) {
        viewModelScope.launch {
            val habit = habits.value.find { it.id == habitId } ?: return@launch

            val completionData = CompletionData(
                progressDelta = 1f / habit.progress.target,
                attributeContributions = habit.linkedAttributes.associateWith { 25L } // base XP
            )

            completeHabitUseCase(habitId, completionData)
        }
    }

    fun createQuickHabit(title: String, linkedAttributes: List<SpecialType>) {
        viewModelScope.launch {
            // TODO: Call repository create + archetype suggestions later
        }
    }
}
