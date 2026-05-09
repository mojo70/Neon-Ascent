package com.neon.ascent.feature.habits.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.goals.models.CompletionData
import com.neon.ascent.core.domain.goals.models.Habit
import com.neon.ascent.core.domain.goals.models.Mission
import com.neon.ascent.core.domain.goals.usecases.CompleteHabitAndUpdateGoalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
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

    fun completeHabit(habitId: String) {
        viewModelScope.launch {
            completeHabitUseCase(habitId, CompletionData())
        }
    }
}
