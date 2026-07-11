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
    val exploreRoutines: List<WorkoutRoutine> = emptyList(),
    val augments: List<WorkoutAugment> = emptyList(),
    val exploreAugments: List<WorkoutAugment> = emptyList(),
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
    val editingRoutineId: String? = null,
    val isReorderingExercises: Boolean = false,
    val newRoutineName: String = "",
    val newRoutineExercises: List<RoutineExercise> = emptyList(),
    val newRoutineAugments: List<WorkoutAugment> = emptyList(),

    // Augment Creation State
    val isCreatingAugment: Boolean = false,
    val newAugmentName: String = "",
    val newAugmentBodyPart: String = "",
    val newAugmentExercises: List<RoutineExercise> = emptyList(),

    val activeSessionError: String? = null,

    // Finish Workout Dialogs
    val activeRoutine: WorkoutRoutine? = null,
    val showUncompletedSetsDialog: Boolean = false,
    val showSaveRoutineChangesDialog: Boolean = false,

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
    private var sessionJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            repository.seedStarterExercises()
            loadExercises()
            loadRoutines()
            loadAugments()
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
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
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
        _uiState.update { it.copy(isCreatingRoutine = true, editingRoutineId = null, newRoutineName = "", newRoutineExercises = emptyList()) }
    }

    fun editRoutine(routine: WorkoutRoutine) {
        _uiState.update { it.copy(
            isCreatingRoutine = true,
            editingRoutineId = routine.id,
            newRoutineName = routine.name,
            newRoutineExercises = routine.exercises,
            newRoutineAugments = routine.augments
        ) }
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
        _uiState.update { it.copy(isCreatingRoutine = false, editingRoutineId = null) }
    }

    fun updateNewRoutineName(name: String) {
        _uiState.update { it.copy(newRoutineName = name) }
    }

    fun addExerciseToNewRoutine(exercise: Exercise) {
        val routineExercise = RoutineExercise(
            exercise = exercise,
            sets = listOf(RoutineSet(type = SetType.NORMAL)) // Default with one set
        )
        if (_uiState.value.isCreatingAugment) {
            _uiState.update { it.copy(newAugmentExercises = it.newAugmentExercises + routineExercise) }
        } else {
            _uiState.update { it.copy(newRoutineExercises = it.newRoutineExercises + routineExercise) }
        }
    }

    fun removeExerciseFromNewRoutine(routineExercise: RoutineExercise) {
        if (_uiState.value.isCreatingAugment) {
            _uiState.update { it.copy(newAugmentExercises = it.newAugmentExercises - routineExercise) }
        } else {
            _uiState.update { it.copy(newRoutineExercises = it.newRoutineExercises - routineExercise) }
        }
    }

    fun updateRoutineExerciseSet(routineExercise: RoutineExercise, setIndex: Int, type: SetType? = null, weight: Float? = null, reps: Int? = null, goalReps: String? = null) {
        val updateFunc = { exercises: List<RoutineExercise> ->
            exercises.map { re ->
                if (re === routineExercise) {
                    val updatedSets = re.sets.toMutableList()
                    if (setIndex in updatedSets.indices) {
                        val currentSet = updatedSets[setIndex]
                        val newType = type ?: currentSet.type
                        val newGoalReps = if (type == SetType.WIDOWMAKER && currentSet.type != SetType.WIDOWMAKER) {
                            "20"
                        } else {
                            goalReps ?: currentSet.goalReps
                        }

                        updatedSets[setIndex] = currentSet.copy(
                            type = newType,
                            weight = weight ?: currentSet.weight,
                            reps = reps ?: currentSet.reps,
                            goalReps = newGoalReps
                        )
                    }
                    re.copy(sets = updatedSets)
                } else re
            }
        }
        
        if (_uiState.value.isCreatingAugment) {
            _uiState.update { it.copy(newAugmentExercises = updateFunc(it.newAugmentExercises)) }
        } else {
            _uiState.update { it.copy(newRoutineExercises = updateFunc(it.newRoutineExercises)) }
        }
    }

    fun addSetToRoutineExercise(routineExercise: RoutineExercise) {
        val updateFunc = { exercises: List<RoutineExercise> ->
            exercises.map { re ->
                if (re === routineExercise) {
                    re.copy(sets = re.sets + RoutineSet(type = SetType.NORMAL))
                } else re
            }
        }
        
        if (_uiState.value.isCreatingAugment) {
            _uiState.update { it.copy(newAugmentExercises = updateFunc(it.newAugmentExercises)) }
        } else {
            _uiState.update { it.copy(newRoutineExercises = updateFunc(it.newRoutineExercises)) }
        }
    }

    fun removeSetFromRoutineExercise(routineExercise: RoutineExercise, setIndex: Int) {
        val updateFunc = { exercises: List<RoutineExercise> ->
            exercises.map { re ->
                if (re === routineExercise) {
                    re.copy(sets = re.sets.filterIndexed { index, _ -> index != setIndex })
                } else re
            }
        }
        
        if (_uiState.value.isCreatingAugment) {
            _uiState.update { it.copy(newAugmentExercises = updateFunc(it.newAugmentExercises)) }
        } else {
            _uiState.update { it.copy(newRoutineExercises = updateFunc(it.newRoutineExercises)) }
        }
    }

    fun removeAugmentFromNewRoutine(augment: WorkoutAugment) {
        _uiState.update { it.copy(newRoutineAugments = it.newRoutineAugments - augment) }
    }

    fun addAugmentToNewRoutine(augment: WorkoutAugment) {
        _uiState.update { it.copy(newRoutineAugments = it.newRoutineAugments + augment) }
    }

    fun saveRoutine() {
        val state = _uiState.value
        if (state.newRoutineName.isBlank()) return
        
        val routine = WorkoutRoutine(
            id = state.editingRoutineId ?: UUID.randomUUID().toString(),
            name = state.newRoutineName,
            exercises = state.newRoutineExercises,
            augments = state.newRoutineAugments
        )
        
        viewModelScope.launch {
            repository.saveRoutine(routine)
            _uiState.update { it.copy(isCreatingRoutine = false, editingRoutineId = null) }
        }
    }

    fun startCreateAugment() {
        _uiState.update { it.copy(isCreatingAugment = true, newAugmentName = "", newAugmentExercises = emptyList(), newAugmentBodyPart = "") }
    }

    fun cancelCreateAugment() {
        _uiState.update { it.copy(isCreatingAugment = false) }
    }

    fun updateNewAugmentName(name: String) {
        _uiState.update { it.copy(newAugmentName = name) }
    }

    fun updateNewAugmentBodyPart(bodyPart: String) {
        _uiState.update { it.copy(newAugmentBodyPart = bodyPart) }
    }

    fun saveAugment() {
        val state = _uiState.value
        if (state.newAugmentName.isBlank()) return
        
        val augment = WorkoutAugment(
            id = UUID.randomUUID().toString(),
            name = state.newAugmentName,
            description = null,
            focusBodyPart = state.newAugmentBodyPart,
            exercises = state.newAugmentExercises,
            colorHex = "#00CCFF" // Neon blue default
        )
        
        viewModelScope.launch {
            repository.saveAugment(augment)
            _uiState.update { it.copy(isCreatingAugment = false) }
        }
    }

    fun deleteAugment(augment: WorkoutAugment) {
        viewModelScope.launch {
            repository.deleteAugment(augment.id)
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
                _uiState.update { it.copy(
                    routines = routines.filter { r -> r.isAddedToLibrary },
                    exploreRoutines = routines.filter { r -> r.isSystem && !r.isAddedToLibrary }
                ) }
            }
        }
    }

    private fun loadAugments() {
        viewModelScope.launch {
            repository.getAllAugments().collect { augments ->
                _uiState.update { it.copy(
                    augments = augments.filter { a -> a.isAddedToLibrary },
                    exploreAugments = augments.filter { a -> a.isSystem && !a.isAddedToLibrary }
                ) }
            }
        }
    }

    fun toggleAugmentLibrary(augment: WorkoutAugment) {
        viewModelScope.launch {
            repository.saveAugment(augment.copy(isAddedToLibrary = !augment.isAddedToLibrary))
        }
    }

    fun toggleRoutineLibrary(routine: WorkoutRoutine) {
        viewModelScope.launch {
            repository.saveRoutine(routine.copy(isAddedToLibrary = !routine.isAddedToLibrary))
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
        
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
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
            activeRoutine = routine,
            isLoading = true, 
            workoutDurationSeconds = 0, 
            isPaused = false,
            previousLogs = emptyMap()
        ) }

        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            repository.saveSession(session)
            startWorkoutTimer()
            
            var currentOrder = 0
            
            // Pre-populate logs with exercises from routine
            routine.exercises.forEach { routineExercise ->
                val workoutLog = WorkoutLog(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    exerciseId = routineExercise.exercise.id,
                    order = currentOrder++,
                    exerciseName = routineExercise.exercise.name
                )
                repository.saveWorkoutLog(workoutLog)
                
                // Use defined sets from routine
                routineExercise.sets.forEach { routineSet ->
                    val setLog = SetLog(
                        id = UUID.randomUUID().toString(),
                        workoutLogId = workoutLog.id,
                        weight = routineSet.weight,
                        reps = routineSet.reps,
                        type = routineSet.type,
                        goalReps = routineSet.goalReps
                    )
                    repository.saveSetLog(setLog)
                }
                
                loadPreviousData(routineExercise.exercise.id)
            }

            // Add augments from routine
            routine.augments.forEach { augment ->
                augment.exercises.forEach { routineExercise ->
                    val workoutLog = WorkoutLog(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        exerciseId = routineExercise.exercise.id,
                        order = currentOrder++,
                        exerciseName = routineExercise.exercise.name,
                        augmentId = augment.id,
                        augmentName = augment.name,
                        augmentColor = augment.colorHex
                    )
                    repository.saveWorkoutLog(workoutLog)
                    
                    // Use defined sets from augment
                    routineExercise.sets.forEach { routineSet ->
                        val setLog = SetLog(
                            id = UUID.randomUUID().toString(),
                            workoutLogId = workoutLog.id,
                            weight = routineSet.weight,
                            reps = routineSet.reps,
                            type = routineSet.type,
                            goalReps = routineSet.goalReps
                        )
                        repository.saveSetLog(setLog)
                    }
                    
                    loadPreviousData(routineExercise.exercise.id)
                }
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

    fun injectAugment(augment: WorkoutAugment) {
        val session = _uiState.value.session ?: return
        val baseOrder = _uiState.value.logs.size
        viewModelScope.launch {
            augment.exercises.forEachIndexed { index, routineExercise ->
                val workoutLog = WorkoutLog(
                    id = UUID.randomUUID().toString(),
                    sessionId = session.id,
                    exerciseId = routineExercise.exercise.id,
                    order = baseOrder + index,
                    exerciseName = routineExercise.exercise.name,
                    augmentId = augment.id,
                    augmentName = augment.name,
                    augmentColor = augment.colorHex,
                    showGoalReps = augment.isSystem // Mandatory for system augments
                )
                repository.saveWorkoutLog(workoutLog)
                loadPreviousData(routineExercise.exercise.id)
            }
        }
    }

    fun toggleGoalReps(workoutLog: WorkoutLog) {
        viewModelScope.launch {
            repository.updateShowGoalReps(workoutLog.id, !workoutLog.showGoalReps)
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
            goalReps = if (type == SetType.WIDOWMAKER) "20" else null,
            clusterMiniSetIndex = if (session.protocol == WorkoutProtocol.CYBER_CRAPP && type == SetType.REST_PAUSE) _uiState.value.currentClusterIndex else null
        )

        viewModelScope.launch {
            repository.saveSetLog(setLog)
            
            if (session.protocol == WorkoutProtocol.CYBER_CRAPP && type == SetType.REST_PAUSE) {
                handleCyberCrappLogic()
            }
        }
    }

    fun updateSet(setLog: SetLog, weight: Float? = null, reps: Int? = null, type: SetType? = null, goalReps: String? = null, isCompleted: Boolean? = null) {
        val newType = type ?: setLog.type
        val newGoalReps = if (type == SetType.WIDOWMAKER && setLog.type != SetType.WIDOWMAKER) {
            "20"
        } else {
            goalReps ?: setLog.goalReps
        }

        val updatedSet = setLog.copy(
            weight = weight ?: setLog.weight,
            reps = reps ?: setLog.reps,
            type = newType,
            goalReps = newGoalReps,
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
            repository.updateSupersetId(log1.id, supersetId)
            repository.updateSupersetId(log2.id, supersetId)
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
        val currentLogs = _uiState.value.logs
        val hasUncompletedSets = currentLogs.any { (_, sets) -> sets.any { !it.isCompleted } }

        if (hasUncompletedSets) {
            _uiState.update { it.copy(showUncompletedSetsDialog = true) }
        } else {
            checkRoutineModificationsAndFinish()
        }
    }

    fun dismissUncompletedSetsDialog(discard: Boolean) {
        _uiState.update { it.copy(showUncompletedSetsDialog = false) }
        if (discard) {
            checkRoutineModificationsAndFinish(isDiscardingUncompleted = true)
        }
    }

    private fun checkRoutineModificationsAndFinish(isDiscardingUncompleted: Boolean = false) {
        val activeRoutine = _uiState.value.activeRoutine
        val currentLogs = _uiState.value.logs
        
        if (activeRoutine != null && checkIfRoutineModified(activeRoutine, currentLogs, isDiscardingUncompleted)) {
            _uiState.update { it.copy(showSaveRoutineChangesDialog = true) }
        } else {
            performFinalFinish()
        }
    }

    private fun checkIfRoutineModified(
        routine: WorkoutRoutine, 
        currentLogs: List<Pair<WorkoutLog, List<SetLog>>>,
        isDiscardingUncompleted: Boolean
    ): Boolean {
        // 1. Check if exercises changed (added, removed, or reordered)
        val routineExerciseIds = routine.exercises.map { it.exercise.id } + routine.augments.flatMap { it.exercises }.map { it.exercise.id }
        val currentExerciseIds = currentLogs.map { it.first.exerciseId }
        
        if (routineExerciseIds != currentExerciseIds) return true
        
        // 2. Check if set counts changed compared to routine definition
        val routineSetsPerExercise = routine.exercises.associateBy({ it.exercise.id }, { it.sets })
        val augmentSetsPerExercise = routine.augments.flatMap { it.exercises }.associateBy({ it.exercise.id }, { it.sets })
        
        val allSetsPerExercise = routineSetsPerExercise + augmentSetsPerExercise

        val anySetCountChanged = currentLogs.any { (log, sets) ->
            val effectiveSetCount = if (isDiscardingUncompleted) {
                sets.count { it.isCompleted }
            } else {
                sets.size
            }
            val definedSetCount = allSetsPerExercise[log.exerciseId]?.size ?: 0
            effectiveSetCount != definedSetCount
        }
        
        if (anySetCountChanged) return true

        return false
    }

    fun confirmSaveRoutineChanges(save: Boolean) {
        if (save) {
            saveCurrentWorkoutAsRoutineUpdate()
        }
        _uiState.update { it.copy(showSaveRoutineChangesDialog = false) }
        performFinalFinish()
    }

    private fun saveCurrentWorkoutAsRoutineUpdate() {
        val state = _uiState.value
        val activeRoutine = state.activeRoutine ?: return
        val currentLogs = state.logs
        
        // Safety check: Don't wipe the routine if for some reason we have no logs 
        if (currentLogs.isEmpty()) return

        // Extract exercises and their ACTUAL completed sets
        val updatedExercises = currentLogs
            .filter { it.first.augmentId == null }
            .mapNotNull { (log, sets) ->
                val exercise = activeRoutine.exercises.find { it.exercise.id == log.exerciseId }?.exercise
                    ?: state.availableExercises.find { it.id == log.exerciseId }
                
                if (exercise != null) {
                    RoutineExercise(
                        exercise = exercise,
                        sets = sets.filter { it.isCompleted }.map { setLog ->
                            RoutineSet(
                                type = setLog.type,
                                weight = setLog.weight,
                                reps = setLog.reps,
                                goalReps = setLog.goalReps
                            )
                        }
                    )
                } else null
            }
        
        val hadNonAugmentExercises = currentLogs.any { it.first.augmentId == null }
        if (hadNonAugmentExercises && updatedExercises.isEmpty()) return

        val updatedRoutine = activeRoutine.copy(
            exercises = updatedExercises
        )
        
        viewModelScope.launch {
            repository.saveRoutine(updatedRoutine)
        }
    }

    private fun performFinalFinish() {
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
            _uiState.update { it.copy(
                session = null, 
                workoutDurationSeconds = 0, 
                activeRoutine = null,
                logs = emptyList(), // Clear logs for next session
                previousLogs = emptyMap()
            ) }
            timerJob?.cancel()
            sessionJob?.cancel()
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
