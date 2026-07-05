package com.neon.ascent.feature.goals.ui.ascension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.common.DopamineCoordinator
import com.neon.ascent.core.common.DopamineEvent
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.ProtocolRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AscensionUiState(
    val directives: List<AscensionDirective> = emptyList(),
    val activeMissions: List<AscensionMission> = emptyList(),
    val isLoading: Boolean = false,
    val dopamineEvent: DopamineEvent? = null
)

@HiltViewModel
class AscensionTerminalViewModel @Inject constructor(
    private val repository: AscensionRepository,
    private val protocolRepository: ProtocolRepository,
    private val dopamineCoordinator: DopamineCoordinator
) : ViewModel() {

    private val _dopamineEvent = MutableStateFlow<DopamineEvent?>(null)

    val uiState = combine(
        repository.getAllDirectives(),
        repository.getActiveMissions(),
        _dopamineEvent
    ) { directives, missions, dopamine ->
        AscensionUiState(
            directives = directives,
            activeMissions = missions,
            isLoading = false,
            dopamineEvent = dopamine
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AscensionUiState(isLoading = true))

    init {
        viewModelScope.launch {
            protocolRepository.seedDefaultProtocols()
            dopamineCoordinator.events.collect { event ->
                _dopamineEvent.value = event
            }
        }
    }

    fun clearDopamineEvent() {
        _dopamineEvent.value = null
    }

    fun completeTask(task: AscensionTask, notes: String? = null, mood: Int? = null) {
        viewModelScope.launch {
            repository.completeTask(task, notes, mood, null)
            dopamineCoordinator.triggerSync(xp = task.xpValue)
        }
    }

    fun markDirectiveCompleted(directive: AscensionDirective) {
        viewModelScope.launch {
            val updated = directive.copy(status = DirectiveStatus.COMPLETED, currentProgress = 1f)
            repository.updateDirective(updated)
            dopamineCoordinator.triggerAscension(title = directive.title, xp = 500)
        }
    }
}
