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
    val previousLogs: Map<String, List<SetLog>> = emptyMap(), // exerciseId -> sets
    val availableExercises: List<Exercise> = emptyList(),
    val routines: List<WorkoutRoutine> = emptyList(),
    val isLoading: Boolean = false,
    val isResting: Boolean = false,
    val restTimeRemaining: Int = 15,
    val currentClusterIndex: Int? = null, // 1, 2, 3 for CC
    val showCyberFinisher: Boolean = false,
    val showLoadedStretch: Boolean = false,
    val stretchTimeRemaining: Int = 45,
    val workoutDurationSeconds: Long = 0,
    val isPaused: Boolean = false,
    
    // Routine Creation State
    val isCreatingRoutine: Boolean = false,
    val isReorderingExercises: Boolean = false,
    val newRoutineName: String = "",
    val newRoutineExercises: List<Exercise> = emptyList(),

    val activeSessionError: String? = null,

    // Exercise Picker State
    val exerciseSearchQuery: String = "",
    val selectedEquipment: String? = null,
    val selectedMuscleGroup: String? = null
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState = _uiState.asStateFlow()

    private val updateJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    val filteredExercises = _uiState.map { state ->
        state.availableExercises.filter { exercise ->
            val matchesQuery = exercise.name.contains(state.exerciseSearchQuery, ignoreCase = true) ||
                               exercise.muscleGroups.any { it.contains(state.exerciseSearchQuery, ignoreCase = true) }
            val matchesEquipment = state.selectedEquipment == null || exercise.equipment.contains(state.selectedEquipment)
            val matchesMuscle = state.selectedMuscleGroup == null || exercise.muscleGroups.contains(state.selectedMuscleGroup)
            
            matchesQuery && matchesEquipment && matchesMuscle
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            repository.seedStarterExercises()
            loadExercises()
            loadRoutines()
            checkForActiveSession()
        }
    }

    private fun checkForActiveSession() {
        viewModelScope.launch {
            repository.getActiveSession().collect { session ->
                if (session != null && _uiState.value.session == null) {
                    resumeExistingSession(session)
                }
            }
        }
    }

    private fun resumeExistingSession(session: WorkoutSession) {
        _uiState.update { it.copy(session = session, isLoading = true) }
        viewModelScope.launch {
            repository.getLogsForSession(session.id).collect { logs ->
                _uiState.update { it.copy(logs = logs, isLoading = false) }
                logs.forEach { (log, _) -> loadPreviousData(log.exerciseId) }
            }
        }
        startWorkoutTimer()
    }

    fun saveCustomExercise(name: String, muscleGroup: String, equipment: String, description: String) {
        viewModelScope.launch {
            val exercise = Exercise(
                id = UUID.randomUUID().toString(),
                name = name,
                muscleGroups = listOf(muscleGroup),
                equipment = listOf(equipment),
                description = description,
                cues = emptyList()
            )
            repository.saveExerciseDefinition(exercise)
        }
    }

    fun startCreateRoutine() {
        _uiState.update { it.copy(isCreatingRoutine = true, newRoutineName = "", newRoutineExercises = emptyList()) }
    }

    fun startReordering() {
        _uiState.update { it.copy(isReorderingExercises = true) }
    }

    fun stopReordering() {
        _uiState.update { it.copy(isReorderingExercises = false) }
    }

    fun moveWorkoutLog(fromIndex: Int, toIndex: Int) {
        val currentLogs = _uiState.value.logs.toMutableList()
        if (fromIndex !in currentLogs.indices || toIndex !in currentLogs.indices) return
        
        val item = currentLogs.removeAt(fromIndex)
        currentLogs.add(toIndex, item)
        
        _uiState.update { it.copy(logs = currentLogs) }
        
        // Persist new order
        viewModelScope.launch {
            currentLogs.forEachIndexed { index, pair ->
                repository.updateWorkoutLogOrder(pair.first.id, index)
            }
        }
    }

    fun cancelCreateRoutine() {
        _uiState.update { it.copy(isCreatingRoutine = false) }
    }

    fun updateNewRoutineName(name: String) {
        _uiState.update { it.copy(newRoutineName = name) }
    }

    fun addExerciseToNewRoutine(exercise: Exercise) {
        _uiState.update { it.copy(newRoutineExercises = it.newRoutineExercises + exercise) }
    }

    fun removeExerciseFromNewRoutine(exercise: Exercise) {
        _uiState.update { it.copy(newRoutineExercises = it.newRoutineExercises - exercise) }
    }

    fun saveRoutine() {
        val state = _uiState.value
        if (state.newRoutineName.isBlank()) return
        
        val routine = WorkoutRoutine(
            id = UUID.randomUUID().toString(),
            name = state.newRoutineName,
            exercises = state.newRoutineExercises
        )
        
        viewModelScope.launch {
            repository.saveRoutine(routine)
            _uiState.update { it.copy(isCreatingRoutine = false) }
        }
    }

    fun deleteRoutine(routine: WorkoutRoutine) {
        viewModelScope.launch {
            repository.deleteRoutine(routine.id)
        }
    }

    fun duplicateRoutine(routine: WorkoutRoutine) {
        val newRoutine = routine.copy(
            id = UUID.randomUUID().toString(),
            name = "${routine.name} (Copy)"
        )
        viewModelScope.launch {
            repository.saveRoutine(newRoutine)
        }
    }

    fun shareRoutine(routine: WorkoutRoutine) {
        // Implementation for sharing (e.g., via intent)
    }

    fun updateExerciseSearch(query: String) {
        _uiState.update { it.copy(exerciseSearchQuery = query) }
    }

    fun setEquipmentFilter(equipment: String?) {
        _uiState.update { it.copy(selectedEquipment = equipment) }
    }

    fun setMuscleGroupFilter(muscle: String?) {
        _uiState.update { it.copy(selectedMuscleGroup = muscle) }
    }

    private fun loadExercises() {
        viewModelScope.launch {
            repository.getExerciseDefinitions().collect { exercises ->
                _uiState.update { it.copy(availableExercises = exercises) }
            }
        }
    }

    private fun loadRoutines() {
        viewModelScope.launch {
            repository.getAllRoutines().collect { routines ->
                _uiState.update { it.copy(routines = routines) }
            }
        }
    }

    private fun startWorkoutTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (!_uiState.value.isPaused) {
                    _uiState.update { it.copy(workoutDurationSeconds = it.workoutDurationSeconds + 1) }
                }
            }
        }
    }

    fun startSession(protocol: WorkoutProtocol) {
        if (_uiState.value.session != null) {
            _uiState.update { it.copy(activeSessionError = "A workout session is already in progress. Please finish or discard it before starting a new one.") }
            return
        }
        val sessionId = UUID.randomUUID().toString()
        val session = WorkoutSession(id = sessionId, protocol = protocol)
        _uiState.update { it.copy(
            session = session, 
            isLoading = true, 
            workoutDurationSeconds = 0, 
            isPaused = false,
            previousLogs = emptyMap()
        ) }
        
        viewModelScope.launch {
            repository.saveSession(session)
            startWorkoutTimer()
            repository.getLogsForSession(sessionId).collect { logs ->
                _uiState.update { it.copy(logs = logs, isLoading = false) }
                // Load previous data for each exercise in the current logs
                logs.forEach { (log, _) ->
                    loadPreviousData(log.exerciseId)
                }
            }
        }
    }

    private fun loadPreviousData(exerciseId: String) {
        val currentSessionId = _uiState.value.session?.id ?: return
        if (_uiState.value.previousLogs.containsKey(exerciseId)) return
        
        viewModelScope.launch {
            repository.getLatestSetsForExercise(exerciseId, currentSessionId).collect { sets ->
                _uiState.update { 
                    val newMap = it.previousLogs.toMutableMap()
                    newMap[exerciseId] = sets
                    it.copy(previousLogs = newMap)
                }
            }
        }
    }

    fun startRoutine(routine: WorkoutRoutine) {
        if (_uiState.value.session != null) {
            _uiState.update { it.copy(activeSessionError = "A workout session is already in progress. Please finish or discard it before starting a new one.") }
            return
        }
        val sessionId = UUID.randomUUID().toString()
        val session = WorkoutSession(id = sessionId, protocol = routine.protocol)
        _uiState.update { it.copy(
            session = session, 
            isLoading = true, 
            workoutDurationSeconds = 0, 
            isPaused = false,
            previousLogs = emptyMap()
        ) }

        viewModelScope.launch {
            repository.saveSession(session)
            startWorkoutTimer()
            // Pre-populate logs with exercises from routine
            routine.exercises.forEachIndexed { index, exercise ->
                val workoutLog = WorkoutLog(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    order = index,
                    exerciseName = exercise.name
                )
                repository.saveWorkoutLog(workoutLog)
                loadPreviousData(exercise.id)
            }
            
            repository.getLogsForSession(sessionId).collect { logs ->
                _uiState.update { it.copy(logs = logs, isLoading = false) }
            }
        }
    }

    fun clearActiveSessionError() {
        _uiState.update { it.copy(activeSessionError = null) }
    }

    fun pauseWorkout() {
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeWorkout() {
        _uiState.update { it.copy(isPaused = false) }
    }

    fun discardWorkout() {
        val sessionId = _uiState.value.session?.id ?: return
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            _uiState.update { WorkoutUiState(availableExercises = it.availableExercises, routines = it.routines) }
            timerJob?.cancel()
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
            loadPreviousData(exercise.id)
        }
    }

    fun logSet(workoutLog: WorkoutLog, weight: Float, reps: Int, type: SetType = SetType.NORMAL) {
        val session = _uiState.value.session ?: return
        
        val setLog = SetLog(
            id = UUID.randomUUID().toString(),
            workoutLogId = workoutLog.id,
            weight = weight,
            reps = reps,
            type = type,
            clusterMiniSetIndex = if (session.protocol == WorkoutProtocol.CYBER_CRAPP && type == SetType.REST_PAUSE) _uiState.value.currentClusterIndex else null
        )

        viewModelScope.launch {
            repository.saveSetLog(setLog)
            
            if (session.protocol == WorkoutProtocol.CYBER_CRAPP && type == SetType.REST_PAUSE) {
                handleCyberCrappLogic()
            }
        }
    }

    fun updateSet(setLog: SetLog, weight: Float? = null, reps: Int? = null, type: SetType? = null, isCompleted: Boolean? = null) {
        val updatedSet = setLog.copy(
            weight = weight ?: setLog.weight,
            reps = reps ?: setLog.reps,
            type = type ?: setLog.type,
            isCompleted = isCompleted ?: setLog.isCompleted
        )
        
        updateJobs[setLog.id]?.cancel()
        updateJobs[setLog.id] = viewModelScope.launch {
            // Tiny delay to batch fast keystrokes and reduce database churn
            kotlinx.coroutines.delay(100)
            repository.saveSetLog(updatedSet)
        }
    }

    fun removeWorkoutLog(workoutLog: WorkoutLog) {
        viewModelScope.launch {
            repository.deleteWorkoutLog(workoutLog.id)
        }
    }

    fun createSuperset(log1: WorkoutLog, log2: WorkoutLog) {
        val supersetId = UUID.randomUUID().toString()
        viewModelScope.launch {
            repository.saveWorkoutLog(log1.copy(supersetId = supersetId))
            repository.saveWorkoutLog(log2.copy(supersetId = supersetId))
        }
    }

    fun replaceWorkoutLog(oldLog: WorkoutLog, newExercise: Exercise) {
        viewModelScope.launch {
            // Simplest replacement: delete old, add new at same order
            repository.deleteWorkoutLog(oldLog.id)
            val newLog = WorkoutLog(
                id = UUID.randomUUID().toString(),
                sessionId = oldLog.sessionId,
                exerciseId = newExercise.id,
                order = oldLog.order,
                exerciseName = newExercise.name
            )
            repository.saveWorkoutLog(newLog)
            loadPreviousData(newExercise.id)
        }
    }

    fun removeSet(setLog: SetLog) {
        viewModelScope.launch {
            repository.deleteSetLog(setLog.id)
        }
    }

    fun finishWorkout() {
        val session = _uiState.value.session ?: return
        val finalDuration = _uiState.value.workoutDurationSeconds
        val currentLogs = _uiState.value.logs

        viewModelScope.launch {
            // Clean up: Remove any uncompleted sets from the database
            currentLogs.forEach { (_, sets) ->
                sets.forEach { set ->
                    if (!set.isCompleted) {
                        repository.deleteSetLog(set.id)
                    }
                }
            }

            repository.saveSession(session.copy(durationSeconds = finalDuration))
            _uiState.update { it.copy(session = null, workoutDurationSeconds = 0) }
            timerJob?.cancel()
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
