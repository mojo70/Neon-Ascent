package com.neon.ascent.feature.goals.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.goals.models.Aspiration
import com.neon.ascent.core.domain.goals.models.Mission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AspirationDetailViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val aspirationId: String = checkNotNull(savedStateHandle["id"])

    val aspiration: StateFlow<Aspiration?> = goalRepository.getAspirationById(aspirationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val linkedMissions: StateFlow<List<Mission>> = goalRepository.getMissionsForAspiration(aspirationId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
