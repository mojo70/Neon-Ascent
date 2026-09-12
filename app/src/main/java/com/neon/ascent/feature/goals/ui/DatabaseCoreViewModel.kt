package com.neon.ascent.feature.goals.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.data.local.dao.NeuralMemoryDao
import com.neon.ascent.core.data.local.entity.NeuralMemory
import com.neon.ascent.core.common.DopamineCoordinator
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.feature.goals.domain.usecases.ExportNeuralLogUseCase
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DatabaseCoreViewModel @Inject constructor(
    private val ascensionRepository: AscensionRepository,
    private val workoutRepository: WorkoutRepository,
    private val dopamineCoordinator: DopamineCoordinator,
    private val exportNeuralLogUseCase: ExportNeuralLogUseCase,
    private val neuralMemoryDao: NeuralMemoryDao
) : ViewModel() {

    private val _exportEvent = MutableSharedFlow<String>()
    val exportEvent = _exportEvent.asSharedFlow()

    private val _exportWorkoutEvent = MutableSharedFlow<String>()
    val exportWorkoutEvent = _exportWorkoutEvent.asSharedFlow()

    val aspirations: StateFlow<List<Aspiration>> = MutableStateFlow(emptyList())
    val activeMissions: StateFlow<List<Mission>> = MutableStateFlow(emptyList())

    val ascensionDirectives: StateFlow<List<AscensionDirective>> = ascensionRepository.getAllDirectives()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val ascensionMissions: StateFlow<List<AscensionMission>> = ascensionRepository.getActiveMissions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val ascensionTasks: StateFlow<List<AscensionTask>> = ascensionRepository.getAllRecurringTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val neuralMemories: StateFlow<List<NeuralMemory>> = neuralMemoryDao.getAllMemories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun exportNeuralLog() {
        viewModelScope.launch {
            val logContent = exportNeuralLogUseCase()
            _exportEvent.emit(logContent)
        }
    }

    fun exportWorkoutHistory() {
        viewModelScope.launch {
            val history = workoutRepository.getFullHistory().first()
            val gson = GsonBuilder().setPrettyPrinting().create()
            val json = gson.toJson(history)
            _exportWorkoutEvent.emit(json)
        }
    }

    fun completeAscensionTask(task: AscensionTask) {
        viewModelScope.launch {
            ascensionRepository.completeTask(task, null, null, null)
            dopamineCoordinator.triggerSync(xp = task.xpValue)
        }
    }
}
