package com.neon.ascent.feature.goals.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.goals.models.Habit
import com.neon.ascent.core.domain.goals.models.Mission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MissionDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val missionId: String = checkNotNull(savedStateHandle["id"])

    val mission: StateFlow<Mission?> = goalRepository.getMissionById(missionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val contributingHabits: StateFlow<List<Habit>> = goalRepository.getHabitsForMission(missionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
