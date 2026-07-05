package com.neon.ascent.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.core.domain.workout.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class WorkoutUiState(
    val session: WorkoutSession? = null,
    val currentExercise: Exercise? = null,
    val logs: List<Pair<WorkoutLog, List<SetLog>>> = emptyList(),
    val availableExercises: List<Exercise> = emptyList(),
    val isLoading: Boolean = false,
    val isResting: Boolean = false,
    val restTimeRemaining: Int = 15,
    val currentClusterIndex: Int? = null, // 1, 2, 3 for CC
    val showCyberFinisher: Boolean = false,
    val showLoadedStretch: Boolean = false,
    val stretchTimeRemaining: Int = 45
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadExercises()
    }

    private fun loadExercises() {
        viewModelScope.launch {
            repository.getExerciseDefinitions().collect { exercises ->
                _uiState.update { it.copy(availableExercises = exercises) }
            }
        }
    }

    fun startSession(protocol: WorkoutProtocol) {
        val sessionId = UUID.randomUUID().toString()
        val session = WorkoutSession(id = sessionId, protocol = protocol)
        _uiState.update { it.copy(session = session, isLoading = true) }
        
        viewModelScope.launch {
            repository.saveSession(session)
            repository.getLogsForSession(sessionId).collect { logs ->
                _uiState.update { it.copy(logs = logs, isLoading = false) }
            }
        }
    }

    fun selectExercise(exercise: Exercise) {
        val session = _uiState.value.session ?: return
        val workoutLog = WorkoutLog(
            id = UUID.randomUUID().toString(),
            sessionId = session.id,
            exerciseId = exercise.id,
            order = _uiState.value.logs.size,
            exerciseName = exercise.name
        )
        
        viewModelScope.launch {
            repository.saveWorkoutLog(workoutLog)
            _uiState.update { it.copy(currentExercise = exercise) }
        }
    }

    fun logSet(weight: Float, reps: Int, rir: Int? = null) {
        val currentLog = _uiState.value.logs.lastOrNull()?.first ?: return
        val session = _uiState.value.session ?: return
        
        val setLog = SetLog(
            id = UUID.randomUUID().toString(),
            workoutLogId = currentLog.id,
            weight = weight,
            reps = reps,
            rir = rir,
            clusterMiniSetIndex = _uiState.value.currentClusterIndex
        )

        viewModelScope.launch {
            repository.saveSetLog(setLog)
            
            if (session.protocol == WorkoutProtocol.CYBER_CRAPP) {
                handleCyberCrappLogic()
            }
        }
    }

    private fun handleCyberCrappLogic() {
        val currentIndex = _uiState.value.currentClusterIndex ?: 1
        if (currentIndex < 3) {
            _uiState.update { it.copy(currentClusterIndex = currentIndex + 1, isResting = true) }
            startRestTimer()
        } else {
            // Cluster finished
            _uiState.update { it.copy(currentClusterIndex = null, showCyberFinisher = true) }
        }
    }

    private fun startRestTimer() {
        // Implementation for countdown
    }

    fun startStretch() {
        _uiState.update { it.copy(showLoadedStretch = true, showCyberFinisher = false) }
    }
}
