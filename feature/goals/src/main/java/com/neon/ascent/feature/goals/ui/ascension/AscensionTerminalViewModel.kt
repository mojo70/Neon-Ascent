package com.neon.ascent.feature.goals.ui.ascension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AscensionUiState(
    val directives: List<AscensionDirective> = emptyList(),
    val activeMissions: List<AscensionMission> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class AscensionTerminalViewModel @Inject constructor(
    private val repository: AscensionRepository
) : ViewModel() {

    val uiState = combine(
        repository.getAllDirectives(),
        repository.getActiveMissions()
    ) { directives, missions ->
        AscensionUiState(
            directives = directives,
            activeMissions = missions,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AscensionUiState(isLoading = true))

    fun completeTask(task: AscensionTask, notes: String? = null, mood: Int? = null) {
        viewModelScope.launch {
            repository.completeTask(task, notes, mood, null)
        }
    }
}
