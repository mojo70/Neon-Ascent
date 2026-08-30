package com.neon.ascent.feature.workout.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.common.HapticService
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.core.domain.workout.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.data.datastore.HealthPreferencesDataStore
import com.neon.ascent.core.domain.goals.models.AscensionTask
import com.neon.ascent.core.domain.goals.models.AscensionTaskType
import com.neon.ascent.core.domain.workout.rules.CyberCrappRules
import com.neon.ascent.feature.workout.services.WorkoutTimerService
import com.neon.ascent.core.domain.workout.protocol.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.*
import javax.inject.Inject

data class WorkoutUiState(
    val session: WorkoutSession? = null,
    val currentExercise: Exercise? = null,
    val logs: List<Pair<WorkoutLog, List<SetLog>>> = emptyList(),
    val previousLogs: Map<String, List<SetLog>> = emptyMap(), // exerciseId -> sets
    val progressionStates: Map<String, ProgressionState> = emptyMap(),
    val accomplishments: Map<String, ExerciseAccomplishments> = emptyMap(),
    val availableExercises: List<Exercise> = emptyList(),
    val routines: List<WorkoutRoutine> = emptyList(),
    val exploreRoutines: List<WorkoutRoutine> = emptyList(),
    val augments: List<WorkoutAugment> = emptyList(),
    val exploreAugments: List<WorkoutAugment> = emptyList(),
    val augmentActivations: List<AugmentActivation> = emptyList(),
    val isLoading: Boolean = true,
    val isResting: Boolean = false,
    val restTimeRemaining: Int = 0,
    val restTimerTotalSeconds: Int = 60,
    val restTimerMode: RestTimerMode = RestTimerMode.BOTH,
    val lastCompletedSetId: String? = null,
    val workSetRestTime: Int = 120,
    val warmupSetRestTime: Int = 60,
    val dropSetRestTime: Int = 30,
    val defaultRestTime: Int = 60,
    val isAutoStartTimerEnabled: Boolean = true,
    val currentClusterIndex: Int? = null, // 1, 2, 3 for CC
    val showCyberFinisher: Boolean = false,
    val showLoadedStretch: Boolean = false,
    val stretchTimeRemaining: Int = 45,
    val workoutDurationSeconds: Long = 0,
    val isPaused: Boolean = false,
    val zoomLevel: Float = 1.0f,
    
    // Routine Creation State
    val isCreatingRoutine: Boolean = false,
    val editingRoutineId: String? = null,
    val isReorderingExercises: Boolean = false,
    val newRoutineName: String = "",
    val newRoutineExercises: List<RoutineExercise> = emptyList(),
    val newRoutineAugments: List<WorkoutAugment> = emptyList(),

    // Augment Creation State
    val isCreatingAugment: Boolean = false,
    val editingAugmentId: String? = null,
    val newAugmentName: String = "",
    val newAugmentBodyPart: String = "",
    val newAugmentExercises: List<RoutineExercise> = emptyList(),

    val activeSessionError: String? = null,

    // Phase Management
    val workoutPhase: RestPausePhase = RestPausePhase.NOT_ACTIVE,
    val useSomatotypeInfluence: Boolean = true,
    val somatotypeNudgeText: String? = null,
    val comparisonText: String? = null,
    val userProfile: UserWorkoutProfile? = null,

    // Finish Workout Dialogs
    val activeRoutine: WorkoutRoutine? = null,
    val showUncompletedSetsDialog: Boolean = false,
    val showSaveRoutineChangesDialog: Boolean = false,

    // Exercise Picker State
    val exerciseSearchQuery: String = "",
    val selectedFamilyIdInPicker: String? = null,
    val isShowingAllVariants: Boolean = false,
    val selectedEquipment: String? = null,
    val selectedMuscleGroup: String? = null,
    val selectedExerciseForDetail: Exercise? = null,
    val isExploringProtocols: Boolean = false,
    val selectedProtocolForDetail: WorkoutProtocol? = null,
    val selectedRoutineForPreview: WorkoutRoutine? = null,
    val showDeactivateProtocolDialog: Boolean = false,
    val configuringProtocol: WorkoutProtocol? = null,
    val tempConfigProfile: UserWorkoutProfile? = null,

    // Progression & Recovery
    val recoveryScore: RecoveryScore? = null,
    val exerciseLastRir: Map<String, Int> = emptyMap(), // exerciseId -> RIR
    val showSubstitutionDialog: Boolean = false,
    val showPostWorkoutCheckIn: Boolean = false,
    val isShowingSettings: Boolean = false,
    val tempSettingsProfile: UserWorkoutProfile? = null,
    val nextSequencedRoutine: WorkoutRoutine? = null,
    val showSequenceOverrideDialog: Boolean = false,
    val pendingSequenceRoutine: WorkoutRoutine? = null,
    val showInjuryWarningDialog: Boolean = false,
    val injuredExercises: List<Pair<Exercise, List<Exercise>>> = emptyList(),
    val pendingInjuryRoutine: WorkoutRoutine? = null,
    val exerciseToSubstitute: String? = null, // exerciseId
    val recommendedSubstitutes: List<Exercise> = emptyList(),
    val activeCyberCrappLogId: String? = null,
    val stagnantExerciseId: String? = null,
    val repTargets: List<ProtocolRepTarget> = emptyList(),
    val currentUiMode: ProtocolUiMode = ProtocolUiMode.LINEAR,
    val protocolIntakeNeeded: WorkoutProtocol? = null,
    val isStrategicDeconditioningActive: Boolean = false,
    val showProtocolChangeReminderDialog: WorkoutProtocol? = null,
    val allExerciseMaxes: List<ExerciseMax> = emptyList()
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val repository: WorkoutRepository,
    private val hapticService: HapticService,
    private val ascensionRepository: AscensionRepository,
    private val healthPrefs: HealthPreferencesDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState = _uiState.asStateFlow()

    private val timerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                WorkoutTimerService.ACTION_TIMER_TICK -> {
                    val remaining = intent.getIntExtra(WorkoutTimerService.EXTRA_REMAINING, 0)
                    _uiState.update { it.copy(restTimeRemaining = remaining, isResting = true) }
                }
                WorkoutTimerService.ACTION_TIMER_FINISHED -> {
                    val isClusterTimer = _uiState.value.workoutPhase == RestPausePhase.MINI_SET_2 || 
                                         _uiState.value.workoutPhase == RestPausePhase.MINI_SET_3
                    if (isClusterTimer) {
                        hapticService.clusterTimerBuzz()
                    }
                    _uiState.update { it.copy(restTimeRemaining = 0, isResting = false) }
                }
            }
        }
    }

    private val updateJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    private val pendingUpdates = mutableMapOf<String, SetLog>()

    val exerciseFamilies = _uiState.map { state ->
        val query = state.exerciseSearchQuery.trim()
        val allExercises = state.availableExercises
        
        allExercises
            .groupBy { it.familyId }
            .mapNotNull { (familyId, variants) ->
                val primary = variants.find { it.isPrimaryVariant } ?: variants.firstOrNull() ?: return@mapNotNull null
                val familyName = primary.familyName.ifBlank { primary.name }
                
                // Matches if search query is empty or matches family name, variant names, muscles or equipment
                val matchesQuery = query.isEmpty() ||
                        familyName.contains(query, ignoreCase = true) ||
                        variants.any { v ->
                            v.name.contains(query, ignoreCase = true) ||
                            v.muscleGroups.any { it.contains(query, ignoreCase = true) } ||
                            v.implement.name.contains(query, ignoreCase = true)
                        }

                val matchesEquipment = state.selectedEquipment == null || variants.any { it.equipment.contains(state.selectedEquipment) }
                val matchesMuscle = state.selectedMuscleGroup == null || variants.any { it.muscleGroups.contains(state.selectedMuscleGroup) }

                if (matchesQuery && matchesEquipment && matchesMuscle) {
                    ExerciseFamily(
                        id = familyId,
                        name = familyName,
                        movementType = primary.movementType,
                        variants = variants.sortedWith(compareByDescending<Exercise> { it.isPrimaryVariant }.thenBy { it.name })
                    )
                } else null
            }
            .sortedBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var workoutDurationJob: kotlinx.coroutines.Job? = null
    private var stretchTimerJob: kotlinx.coroutines.Job? = null
    private var sessionJob: kotlinx.coroutines.Job? = null
    private var zoomUpdateJob: kotlinx.coroutines.Job? = null

    private val startingStrengthEngine by lazy { 
        StartingStrengthEngine(_uiState.value.userProfile?.unitSystem ?: UnitSystem.IMPERIAL) 
    }

    private val hstEngine by lazy {
        HSTEngine(
            _uiState.value.userProfile?.unitSystem ?: UnitSystem.IMPERIAL,
            _uiState.value.userProfile?.weightIncrementCompound ?: 2.5f
        )
    }

    private val fiveThreeOneEngine by lazy {
        FiveThreeOneEngine(_uiState.value.userProfile?.weightIncrementCompound ?: 2.5f)
    }

    private val dupEngine by lazy {
        DUPEngine(_uiState.value.userProfile?.weightIncrementCompound ?: 2.5f)
    }

    private val westsideEngine by lazy {
        WestsideEngine(_uiState.value.userProfile?.weightIncrementCompound ?: 2.5f)
    }

    private fun getEngine(protocol: WorkoutProtocol): ProtocolEngine? = when (protocol) {
        WorkoutProtocol.STARTING_STRENGTH -> startingStrengthEngine
        WorkoutProtocol.HST -> hstEngine
        WorkoutProtocol.FIVE_THREE_ONE -> fiveThreeOneEngine
        WorkoutProtocol.DUP -> dupEngine
        WorkoutProtocol.WESTSIDE -> westsideEngine
        WorkoutProtocol.CYBER_CRAPP -> CyberCrappEngine()
        WorkoutProtocol.GENERAL, WorkoutProtocol.STRAIGHT_SETS, WorkoutProtocol.SUPERSETS -> GeneralEngine(protocol)
        else -> null
    }

    fun isIntakeNeededForProtocol(protocol: WorkoutProtocol): Boolean {
        val maxes = _uiState.value.allExerciseMaxes
        val families = when (protocol) {
            WorkoutProtocol.STARTING_STRENGTH -> listOf("squat", "bench_press", "overhead_press", "deadlift")
            WorkoutProtocol.FIVE_THREE_ONE -> listOf("overhead_press", "deadlift", "bench_press", "squat")
            WorkoutProtocol.WESTSIDE -> listOf("squat", "bench_press", "deadlift")
            WorkoutProtocol.DUP -> listOf("squat", "bench_press", "rows")
            WorkoutProtocol.HST -> listOf("squat", "bench_press", "rows", "overhead_press", "deadlift")
            else -> emptyList()
        }
        
        return families.any { familyId ->
            val max = maxes.find { it.familyId == familyId }
            if (protocol == WorkoutProtocol.HST) {
                max?.rm15 == null || max.rm10 == null || max.rm5 == null
            } else if (protocol == WorkoutProtocol.WESTSIDE) {
                max?.oneRepMax == 0f
            } else {
                max?.trainingMax == null
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.seedStarterExercises()
            loadExercises()
            loadRoutines()
            loadAugments()
            loadUserProfile()
            checkForActiveSession()
            
            launch {
                repository.getProtocolRepTargets().collect { targets ->
                    _uiState.update { it.copy(repTargets = targets) }
                }
            }
            
            launch {
                repository.getAllExerciseMaxes().collect { maxes ->
                    _uiState.update { it.copy(allExerciseMaxes = maxes) }
                }
            }

            launch {
                repository.getRecoveryScore().collect { score ->
                    _uiState.update { it.copy(recoveryScore = score) }
                }
            }

            launch {
                repository.getAugmentActivations("default_user").collect { activations ->
                    _uiState.update { it.copy(augmentActivations = activations) }
                }
            }
            
            launch {
                healthPrefs.workoutZoomLevel.collect { zoom ->
                    // Only update from preferences if we're not actively zooming/updating
                    if (_uiState.value.zoomLevel != zoom) {
                        _uiState.update { it.copy(zoomLevel = zoom) }
                    }
                }
            }
            launch {
                healthPrefs.defaultRestTime.collect { time ->
                    _uiState.update { it.copy(defaultRestTime = time) }
                }
            }
            launch {
                healthPrefs.autoStartRestTimer.collect { enabled ->
                    _uiState.update { it.copy(isAutoStartTimerEnabled = enabled) }
                }
            }
            launch {
                healthPrefs.workSetRestTime.collect { time ->
                    _uiState.update { it.copy(workSetRestTime = time) }
                }
            }
            launch {
                healthPrefs.warmupSetRestTime.collect { time ->
                    _uiState.update { it.copy(warmupSetRestTime = time) }
                }
            }
            launch {
                healthPrefs.dropSetRestTime.collect { time ->
                    _uiState.update { it.copy(dropSetRestTime = time) }
                }
            }
            launch {
                healthPrefs.restTimerMode.collect { mode ->
                    _uiState.update { it.copy(restTimerMode = mode) }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(WorkoutTimerService.ACTION_TIMER_TICK)
            addAction(WorkoutTimerService.ACTION_TIMER_FINISHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(timerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(timerReceiver, filter)
        }
    }

    override fun onCleared() {
        super.onCleared()
        context.unregisterReceiver(timerReceiver)
    }

    fun updateDefaultRestTime(seconds: Int) {
        viewModelScope.launch {
            healthPrefs.setDefaultRestTime(seconds)
        }
    }

    fun toggleAutoStartTimer() {
        viewModelScope.launch {
            healthPrefs.setAutoStartRestTimer(!_uiState.value.isAutoStartTimerEnabled)
        }
    }

    fun toggleSomatotypeInfluence() {
        _uiState.update { it.copy(useSomatotypeInfluence = !it.useSomatotypeInfluence) }
    }

    fun startManualRestTimer() {
        WorkoutTimerService.start(context, _uiState.value.defaultRestTime)
    }

    fun stopRestTimer() {
        WorkoutTimerService.stop(context)
        _uiState.update { it.copy(isResting = false, restTimeRemaining = 0, lastCompletedSetId = null) }
    }

    private fun triggerRestTimer(setLog: SetLog, customDuration: Int? = null) {
        val duration = customDuration ?: when (setLog.type) {
            SetType.WARMUP -> _uiState.value.warmupSetRestTime
            SetType.DROP -> _uiState.value.dropSetRestTime
            else -> _uiState.value.workSetRestTime
        }
        
        if (duration > 0) {
            _uiState.update { it.copy(
                isResting = true,
                restTimerTotalSeconds = duration,
                restTimeRemaining = duration,
                lastCompletedSetId = setLog.id
            ) }
            WorkoutTimerService.start(context, duration)
        }
    }

    fun skipRestTimer() {
        stopRestTimer()
    }

    fun adjustRestTimer(seconds: Int) {
        val intent = Intent(context, WorkoutTimerService::class.java).apply {
            action = WorkoutTimerService.ACTION_ADD_TIME
            putExtra(WorkoutTimerService.EXTRA_SECONDS, seconds)
        }
        context.startService(intent)
    }

    fun updateWorkSetRestTime(seconds: Int) {
        viewModelScope.launch { healthPrefs.setWorkSetRestTime(seconds) }
    }

    fun updateWarmupSetRestTime(seconds: Int) {
        viewModelScope.launch { healthPrefs.setWarmupSetRestTime(seconds) }
    }

    fun updateDropSetRestTime(seconds: Int) {
        viewModelScope.launch { healthPrefs.setDropSetRestTime(seconds) }
    }

    fun updateRestTimerMode(mode: RestTimerMode) {
        viewModelScope.launch { healthPrefs.setRestTimerMode(mode) }
    }

    fun updateZoomLevel(zoom: Float) {
        _uiState.update { it.copy(zoomLevel = zoom) }
        
        // Debounce persistent storage write to avoid gesture lag
        zoomUpdateJob?.cancel()
        zoomUpdateJob = viewModelScope.launch {
            delay(1000)
            healthPrefs.setWorkoutZoomLevel(zoom)
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            // Assuming a default user ID for now, or fetch from auth
            repository.getUserProfile("default_user").collect { profile ->
                _uiState.update { it.copy(userProfile = profile, isLoading = false) }
                updateSomatotypeNudge()
                updateSequencerState()
            }
        }
    }

    private fun updateSomatotypeNudge() {
        val state = _uiState.value
        if (!state.useSomatotypeInfluence || state.userProfile == null) {
            _uiState.update { it.copy(somatotypeNudgeText = null) }
            return
        }

        val nudge = when (state.userProfile.somatotype) {
            Somatotype.ECTOMORPH -> "Ecto-Optimization: +10% Volume Nudge Active"
            Somatotype.ENDOMORPH -> "Endo-Optimization: Shorter Stretches Active"
            Somatotype.MESOMORPH -> "Meso-Optimization: Balanced Protocol"
        }
        _uiState.update { it.copy(somatotypeNudgeText = nudge) }
    }

    private fun checkForActiveSession() {
        viewModelScope.launch {
            // Cleanup expired activations
            val now = java.time.Instant.now()
            val activations = repository.getActiveAugmentActivations().first()
            activations.forEach { act ->
                if (act.windowEnd != null && now.isAfter(act.windowEnd)) {
                    endAugmentActivation(act)
                }
            }

            repository.getActiveSession().collect { session ->
                if (session != null && _uiState.value.session == null) {
                    resumeExistingSession(session)
                }
            }
        }
    }

    private fun resumeExistingSession(session: WorkoutSession) {
        val uiMode = when (session.protocol) {
            WorkoutProtocol.CYBER_CRAPP -> ProtocolUiMode.CLUSTER
            WorkoutProtocol.DUP -> if (session.protocolDayType == ProtocolDayType.DUP_POWER) ProtocolUiMode.DYNAMIC else ProtocolUiMode.LINEAR
            WorkoutProtocol.WESTSIDE -> if (session.protocolDayType == ProtocolDayType.WS_ME_LOWER || session.protocolDayType == ProtocolDayType.WS_ME_UPPER) ProtocolUiMode.MAX_EFFORT else ProtocolUiMode.DYNAMIC
            else -> ProtocolUiMode.LINEAR
        }
        _uiState.update { it.copy(session = session, isLoading = true, currentUiMode = uiMode) }
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
            val id = UUID.randomUUID().toString()
            val imp = when {
                equipment.contains("Dumbbell", ignoreCase = true) -> Implement.DUMBBELL
                equipment.contains("Kettlebell", ignoreCase = true) -> Implement.KETTLEBELL
                equipment.contains("Cable", ignoreCase = true) -> Implement.CABLE
                equipment.contains("Machine", ignoreCase = true) -> Implement.MACHINE
                equipment.contains("Smith", ignoreCase = true) -> Implement.SMITH
                equipment.contains("Plate", ignoreCase = true) -> Implement.PLATE_LOADED
                equipment.contains("Bodyweight", ignoreCase = true) -> Implement.BODYWEIGHT
                equipment.contains("EZ", ignoreCase = true) -> Implement.EZ_BAR
                equipment.contains("Band", ignoreCase = true) -> Implement.BAND
                else -> Implement.BARBELL
            }
            val exercise = Exercise(
                id = id,
                name = name,
                muscleGroups = listOf(muscleGroup),
                equipment = listOf(equipment),
                description = description,
                cues = emptyList(),
                familyId = id,
                familyName = name,
                implement = imp,
                stance = Stance.STANDARD,
                isPrimaryVariant = true
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

    fun editAugment(augment: WorkoutAugment) {
        _uiState.update { it.copy(
            isCreatingAugment = true,
            editingAugmentId = augment.id,
            newAugmentName = augment.name,
            newAugmentExercises = augment.exercises,
            newAugmentBodyPart = augment.focusBodyPart
        ) }
    }

    fun duplicateAugment(augment: WorkoutAugment) {
        val newAugment = augment.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${augment.name} (Copy)",
            isSystem = false,
            isAddedToLibrary = true
        )
        viewModelScope.launch {
            repository.saveAugment(newAugment)
        }
    }

    fun shareAugment(augment: WorkoutAugment) {
        // Implementation for sharing (e.g., via intent)
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
        _uiState.update { it.copy(isCreatingAugment = false, editingAugmentId = null) }
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

        // Determine ID: If editing a system augment, create a new ID to keep library version intact.
        // If editing a custom augment, reuse ID. If creating new, new ID.
        val existingAugment = state.augments.find { it.id == state.editingAugmentId }
        val isSystem = existingAugment?.isSystem == true
        
        val augmentId = if (state.editingAugmentId == null || isSystem) {
            UUID.randomUUID().toString()
        } else {
            state.editingAugmentId
        }
        
        val augment = WorkoutAugment(
            id = augmentId,
            name = state.newAugmentName,
            description = null,
            focusBodyPart = state.newAugmentBodyPart,
            exercises = state.newAugmentExercises,
            colorHex = existingAugment?.colorHex ?: "#00CCFF",
            isSystem = false, // Edited versions are always user versions
            isAddedToLibrary = true
        )
        
        viewModelScope.launch {
            repository.saveAugment(augment)
            
            // If we were editing a system augment, "replace" it in user's library by disabling original
            if (isSystem && state.editingAugmentId != null) {
                val systemAugment = (state.augments + state.exploreAugments).find { it.id == state.editingAugmentId }
                if (systemAugment != null) {
                    repository.saveAugment(systemAugment.copy(isAddedToLibrary = false))
                }
            }
            
            _uiState.update { it.copy(isCreatingAugment = false, editingAugmentId = null) }
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
        _uiState.update { it.copy(exerciseSearchQuery = query, selectedFamilyIdInPicker = null) }
    }

    fun selectFamilyInPicker(familyId: String?) {
        _uiState.update { it.copy(selectedFamilyIdInPicker = familyId) }
    }

    fun toggleShowAllVariants(show: Boolean) {
        _uiState.update { it.copy(isShowingAllVariants = show, selectedFamilyIdInPicker = null) }
    }

    fun setEquipmentFilter(equipment: String?) {
        _uiState.update { it.copy(selectedEquipment = equipment) }
    }

    fun setMuscleGroupFilter(muscle: String?) {
        _uiState.update { it.copy(selectedMuscleGroup = muscle) }
    }

    fun showExerciseDetail(exercise: Exercise) {
        _uiState.update { it.copy(selectedExerciseForDetail = exercise) }
    }

    fun hideExerciseDetail() {
        _uiState.update { it.copy(selectedExerciseForDetail = null) }
    }

    fun showSettings() {
        _uiState.update { it.copy(isShowingSettings = true, tempSettingsProfile = it.userProfile) }
    }

    fun hideSettings() {
        _uiState.update { it.copy(isShowingSettings = false, tempSettingsProfile = null) }
    }

    fun updateTempSettingsProfile(profile: UserWorkoutProfile) {
        _uiState.update { it.copy(tempSettingsProfile = profile) }
    }

    fun updateBreathingVibrationEnabled(enabled: Boolean) {
        val profile = _uiState.value.userProfile ?: return
        viewModelScope.launch {
            repository.saveUserProfile(profile.copy(breathingVibrationEnabled = enabled))
        }
    }

    fun saveWorkoutSettings() {
        val profile = _uiState.value.tempSettingsProfile ?: return
        viewModelScope.launch {
            repository.saveUserProfile(profile)
            syncWorkoutReminders(profile)
            _uiState.update { it.copy(userProfile = profile, isShowingSettings = false, tempSettingsProfile = null) }
        }
    }

    fun resetWorkoutProfile() {
        viewModelScope.launch {
            repository.deleteUserProfile("default_user")
            
            // 1. Delete old training tasks from Ascension Forge
            try {
                ascensionRepository.getAllRecurringTasks().first()
                    .filter { it.tags.contains("workout_session") }
                    .forEach { task ->
                        ascensionRepository.deleteTask(task.id)
                    }
            } catch (e: Exception) {
                android.util.Log.e("WorkoutViewModel", "Failed to clear training tasks during reset", e)
            }

            // 2. Clear routines from library (reset to system defaults)
            try {
                repository.getAllRoutines().first()
                    .filter { it.isAddedToLibrary }
                    .forEach { routine ->
                        repository.saveRoutine(routine.copy(isAddedToLibrary = false))
                    }
            } catch (e: Exception) {
                android.util.Log.e("WorkoutViewModel", "Failed to clear routine library during reset", e)
            }

            _uiState.update { it.copy(userProfile = null, isShowingSettings = false, tempSettingsProfile = null) }
        }
    }

    fun startExploreProtocols() {
        _uiState.update { it.copy(isExploringProtocols = true, selectedProtocolForDetail = null) }
    }

    fun hideExploreProtocols() {
        _uiState.update { it.copy(isExploringProtocols = false, selectedProtocolForDetail = null) }
    }

    fun showProtocolDetail(protocol: WorkoutProtocol) {
        _uiState.update { it.copy(
            selectedProtocolForDetail = protocol,
            isExploringProtocols = false
        ) }
    }

    fun hideProtocolDetail() {
        _uiState.update { it.copy(
            selectedProtocolForDetail = null,
            isExploringProtocols = true
        ) }
    }

    fun showRoutinePreview(routine: WorkoutRoutine) {
        _uiState.update { it.copy(selectedRoutineForPreview = routine) }
    }

    fun hideRoutinePreview() {
        _uiState.update { it.copy(selectedRoutineForPreview = null) }
    }

    fun resumeUserProfile() {
        loadUserProfile()
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
                    exploreRoutines = routines.filter { r -> r.isSystem }
                ) }
                updateSequencerState()
            }
        }
    }

    private fun updateSequencerState() {
        val state = _uiState.value
        val profile = state.userProfile ?: return
        if (!profile.sequencerEnabled) {
            _uiState.update { it.copy(nextSequencedRoutine = null) }
            return
        }

        val nextRoutine = if (profile.activeProtocol != null) {
            val protocolRoutines = state.routines.filter { it.protocol == profile.activeProtocol }
            if (protocolRoutines.isNotEmpty()) {
                protocolRoutines[profile.rotationIndex % protocolRoutines.size]
            } else null
        } else if (profile.customSequenceIds.isNotEmpty()) {
            val nextId = profile.customSequenceIds[profile.rotationIndex % profile.customSequenceIds.size]
            state.routines.find { it.id == nextId }
        } else null

        _uiState.update { it.copy(nextSequencedRoutine = nextRoutine) }
    }

    private fun loadAugments() {
        viewModelScope.launch {
            repository.getAllAugments().collect { augments ->
                _uiState.update { it.copy(
                    augments = augments.filter { a -> a.isAddedToLibrary },
                    exploreAugments = augments.filter { a -> a.isSystem }
                ) }
            }
        }
    }

    fun saveAugmentActivation(activation: AugmentActivation) {
        viewModelScope.launch {
            repository.saveAugmentActivation(activation)
            syncAugmentReminders(activation)
        }
    }

    fun pauseAugmentActivation(activation: AugmentActivation) {
        viewModelScope.launch {
            repository.saveAugmentActivation(activation.copy(status = AugmentActivationStatus.PAUSED))
            syncAugmentReminders(activation.copy(status = AugmentActivationStatus.PAUSED))
        }
    }

    fun endAugmentActivation(activation: AugmentActivation) {
        viewModelScope.launch {
            repository.endAugmentActivation(activation.id)
            // Clear reminders
            val existingTasks = ascensionRepository.getAllRecurringTasks().first()
            existingTasks.filter { task ->
                task.tags.contains("augment_act_${activation.id}")
            }.forEach { task ->
                ascensionRepository.deleteTask(task.id)
            }
        }
    }

    private suspend fun syncAugmentReminders(activation: AugmentActivation) {
        val augment = (uiState.value.augments + uiState.value.exploreAugments).find { it.id == activation.augmentId } ?: return

        // 1. Clear existing tasks for THIS activation
        val existingTasks = ascensionRepository.getAllRecurringTasks().first()
        existingTasks.filter { task ->
            task.tags.contains("augment_act_${activation.id}")
        }.forEach { task ->
            ascensionRepository.deleteTask(task.id)
        }

        if (activation.status != AugmentActivationStatus.ACTIVE || activation.mode != AugmentRunMode.INDEPENDENT || !activation.reminderEnabled) {
            return
        }

        // 2. Write new tasks
        activation.scheduledDays.forEach { scheduled ->
            val task = AscensionTask(
                id = UUID.randomUUID().toString(),
                parentId = null,
                title = "SUB-PROTOCOL: ${augment.name}",
                description = "Dedicated training window for ${augment.focusBodyPart}.",
                type = AscensionTaskType.RECURRING,
                recurrence = com.neon.ascent.core.domain.goals.models.RecurrenceV3(
                    type = com.neon.ascent.core.domain.goals.models.RecurrenceTypeV3.DAYS_OF_WEEK,
                    daysOfWeek = setOf(java.time.DayOfWeek.of(scheduled.dayOfWeek))
                ),
                timeWindows = listOf(scheduled.time),
                reminderEnabled = true,
                xpValue = 15,
                tags = listOf("workout_session", "augment_act_${activation.id}", "independent_start")
            )
            ascensionRepository.insertTask(task)
        }
    }

    fun startIndependentAugmentSession(activation: AugmentActivation) {
        val augment = (uiState.value.augments + uiState.value.exploreAugments).find { it.id == activation.augmentId } ?: return
        
        if (_uiState.value.session != null) {
            _uiState.update { it.copy(activeSessionError = "A workout session is already in progress.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val sessionId = UUID.randomUUID().toString()
            
            // INDEPENDENT sessions use mapped protocol or GENERAL
            val sessionProtocol = when (activation.loggingStyle) {
                AugmentLoggingStyle.CYBER_CRAPP -> WorkoutProtocol.CYBER_CRAPP
                AugmentLoggingStyle.GENERAL -> WorkoutProtocol.GENERAL
                AugmentLoggingStyle.INHERIT -> WorkoutProtocol.GENERAL // No host to inherit from
            }

            val session = WorkoutSession(
                id = sessionId,
                protocol = sessionProtocol,
                primaryAugmentId = activation.augmentId
            )
            repository.saveSession(session)

            var globalSetTimestamp = java.time.Instant.now()
            augment.exercises.forEachIndexed { index, routineExercise ->
                val workoutLog = WorkoutLog(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    exerciseId = routineExercise.exercise.id,
                    order = index,
                    exerciseName = routineExercise.exercise.name,
                    augmentId = augment.id,
                    augmentName = augment.name,
                    augmentColor = augment.colorHex,
                    showGoalReps = true,
                    // If loggingStyle is CYBER_CRAPP, we force it on the log too
                    protocolOverride = if (activation.loggingStyle == AugmentLoggingStyle.CYBER_CRAPP) WorkoutProtocol.CYBER_CRAPP else null
                )
                repository.saveWorkoutLog(workoutLog)

                // Build sets. If loggingStyle or protocol is CYBER_CRAPP and type is REST_PAUSE, expand.
                routineExercise.sets.forEach { routineSet ->
                    val isClusterType = (sessionProtocol == WorkoutProtocol.CYBER_CRAPP || activation.loggingStyle == AugmentLoggingStyle.CYBER_CRAPP) && 
                                       routineSet.type == SetType.REST_PAUSE
                    
                    if (isClusterType) {
                        for (i in 1..3) {
                            globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                            val setLog = SetLog(
                                id = UUID.randomUUID().toString(),
                                workoutLogId = workoutLog.id,
                                weight = routineSet.weight,
                                reps = 0,
                                type = SetType.REST_PAUSE,
                                goalReps = routineSet.goalReps,
                                clusterMiniSetIndex = i,
                                timestamp = globalSetTimestamp
                            )
                            repository.saveSetLog(setLog)
                        }
                    } else {
                        globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                        val setLog = SetLog(
                            id = UUID.randomUUID().toString(),
                            workoutLogId = workoutLog.id,
                            weight = routineSet.weight,
                            reps = routineSet.reps,
                            type = routineSet.type,
                            goalReps = routineSet.goalReps,
                            timestamp = globalSetTimestamp
                        )
                        repository.saveSetLog(setLog)
                    }
                }
                loadPreviousData(routineExercise.exercise.id)
            }

            _uiState.update { it.copy(
                session = session,
                workoutDurationSeconds = 0,
                isPaused = false,
                previousLogs = emptyMap(),
                workoutPhase = if (sessionProtocol == WorkoutProtocol.CYBER_CRAPP) RestPausePhase.MINI_SET_1 else RestPausePhase.NOT_ACTIVE,
                isLoading = false
            ) }
            startWorkoutTimer()
        }
    }

    fun handleAdHocAugment(augment: WorkoutAugment) {
        val currentSession = _uiState.value.session
        if (currentSession != null) {
            // BOLT ON to open session
            injectAugment(augment)
        } else {
            // Start independent ad hoc session
            val tempActivation = AugmentActivation(
                augmentId = augment.id,
                mode = AugmentRunMode.AD_HOC,
                loggingStyle = if (augment.name == "Gorilla Arms") AugmentLoggingStyle.CYBER_CRAPP else AugmentLoggingStyle.INHERIT
            )
            startIndependentAugmentSession(tempActivation)
        }
    }

    fun handleTaskLaunch(taskId: String) {
        viewModelScope.launch {
            val task = ascensionRepository.getTaskById(taskId).first() ?: return@launch
            
            // Check for independent augment start
            val actTag = task.tags.find { it.startsWith("augment_act_") }
            val isIndependent = task.tags.contains("independent_start")
            
            if (isIndependent && actTag != null) {
                val actId = actTag.removePrefix("augment_act_")
                val activation = _uiState.value.augmentActivations.find { it.id == actId }
                if (activation != null) {
                    startIndependentAugmentSession(activation)
                    return@launch
                }
            }
            
            // Fallback: Start normal session or resume
            if (_uiState.value.session == null) {
                // If the task has a protocol tag, start that session
                val protocolTag = task.tags.find { it.startsWith("protocol_") }
                if (protocolTag != null) {
                    val protocolName = protocolTag.removePrefix("protocol_")
                    runCatching { WorkoutProtocol.valueOf(protocolName) }.getOrNull()?.let { protocol ->
                        startSession(protocol)
                    }
                }
            }
        }
    }

    fun updateAugmentSchedule(augment: WorkoutAugment, scheduledDays: List<ScheduledDay>) {
        viewModelScope.launch {
            val updatedAugment = augment.copy(scheduledDays = scheduledDays)
            repository.saveAugment(updatedAugment)

            // Sync with Ascension tasks / reminders
            val existingTasks = ascensionRepository.getAllRecurringTasks().first()
            existingTasks.filter { task ->
                task.tags.contains("augment_${augment.id}")
            }.forEach { task ->
                ascensionRepository.deleteTask(task.id)
            }

            scheduledDays.forEach { scheduled ->
                val task = AscensionTask(
                    id = UUID.randomUUID().toString(),
                    parentId = null,
                    title = "SUB-PROTOCOL: ${augment.name}",
                    description = "Dedicated training window for ${augment.focusBodyPart}.",
                    type = AscensionTaskType.RECURRING,
                    recurrence = com.neon.ascent.core.domain.goals.models.RecurrenceV3(
                        type = com.neon.ascent.core.domain.goals.models.RecurrenceTypeV3.DAYS_OF_WEEK,
                        daysOfWeek = setOf(java.time.DayOfWeek.of(scheduled.dayOfWeek))
                    ),
                    timeWindows = listOf(scheduled.time),
                    reminderEnabled = true,
                    xpValue = 15,
                    tags = listOf("workout_session", "augment_${augment.id}")
                )
                ascensionRepository.insertTask(task)
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
            val routines = repository.getAllRoutines().first()
            val fullRoutine = routines.find { it.id == routine.id } ?: routine
            repository.saveRoutine(fullRoutine.copy(isAddedToLibrary = !fullRoutine.isAddedToLibrary))
        }
    }

    fun addProtocolToLibrary(protocol: WorkoutProtocol) {
        val currentProfile = _uiState.value.userProfile ?: return
        
        if (currentProfile.activeProtocol != null && currentProfile.activeProtocol != protocol) {
            _uiState.update { it.copy(showProtocolChangeReminderDialog = protocol) }
        } else {
            // Direct activation for new/same protocols
            viewModelScope.launch {
                val updatedProfile = currentProfile.copy(activeProtocol = protocol)
                repository.saveUserProfile(updatedProfile)
                
                // Immediately update local state to avoid UI lag
                _uiState.update { it.copy(userProfile = updatedProfile) }
                
                proceedWithProtocolConfig(protocol, resetReminders = true)
            }
        }
    }

    fun confirmProtocolChange(resetReminders: Boolean) {
        val protocol = _uiState.value.showProtocolChangeReminderDialog ?: return
        val currentProfile = _uiState.value.userProfile ?: return
        
        _uiState.update { it.copy(showProtocolChangeReminderDialog = null) }
        
        viewModelScope.launch {
            val updatedProfile = currentProfile.copy(activeProtocol = protocol)
            repository.saveUserProfile(updatedProfile)
            
            // Immediately update local state
            _uiState.update { it.copy(userProfile = updatedProfile) }
            
            proceedWithProtocolConfig(protocol, resetReminders)
        }
    }

    fun cancelProtocolChange() {
        _uiState.update { it.copy(showProtocolChangeReminderDialog = null) }
    }

    private fun proceedWithProtocolConfig(protocol: WorkoutProtocol, resetReminders: Boolean) {
        val currentProfile = _uiState.value.userProfile ?: return
        val engine = getEngine(protocol)
        
        val newSchedule = if (resetReminders && engine != null) {
            val baseTime = currentProfile.scheduledDays.firstOrNull()?.time ?: "09:00"
            engine.defaultWeekdays.map { ScheduledDay(it, baseTime) }
        } else {
            currentProfile.scheduledDays
        }

        _uiState.update { it.copy(
            configuringProtocol = protocol,
            tempConfigProfile = currentProfile.copy(
                activeProtocol = protocol,
                scheduledDays = newSchedule
            ),
            selectedProtocolForDetail = null,
            isExploringProtocols = false
        ) }
    }

    fun updateConfigSchedule(scheduledDays: List<ScheduledDay>) {
        _uiState.update { it.copy(
            tempConfigProfile = it.tempConfigProfile?.copy(scheduledDays = scheduledDays)
        ) }
    }

    fun saveProtocolConfiguration() {
        val protocol = _uiState.value.configuringProtocol ?: return
        val profile = _uiState.value.tempConfigProfile ?: return

        viewModelScope.launch {
            // 1. Add all routines of this protocol to library
            repository.getAllRoutines().first()
                .filter { it.protocol == protocol }
                .forEach { routine ->
                    repository.saveRoutine(routine.copy(isAddedToLibrary = true))
                }

            // 2. Update user profile with new active protocol and schedule
            repository.saveUserProfile(profile)

            syncWorkoutReminders(profile)

            _uiState.update { it.copy(configuringProtocol = null, tempConfigProfile = null, userProfile = profile) }
        }
    }

    private suspend fun appendAttachedAugments(sessionId: String, protocol: WorkoutProtocol, dayType: ProtocolDayType?, startingOrder: Int): Int {
        val now = java.time.Instant.now()
        val activations = repository.getActiveAugmentActivations().first().filter { 
            it.isLive(now, protocol, dayType) && 
            (it.mode == AugmentRunMode.ATTACHED_ONGOING || it.mode == AugmentRunMode.ATTACHED_WINDOW)
        }
        
        var currentOrder = startingOrder
        var globalTimestamp = java.time.Instant.now()

        activations.forEach { activation ->
            val augment = (uiState.value.augments + uiState.value.exploreAugments).find { it.id == activation.augmentId } ?: return@forEach
            val groupSupersetId = if (augment.exercises.size > 1) java.util.UUID.randomUUID().toString() else null
            
            augment.exercises.forEach { routineExercise ->
                val workoutLog = WorkoutLog(
                    id = java.util.UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    exerciseId = routineExercise.exercise.id,
                    order = currentOrder++,
                    exerciseName = routineExercise.exercise.name,
                    augmentId = augment.id,
                    augmentName = augment.name,
                    augmentColor = augment.colorHex,
                    supersetId = groupSupersetId,
                    showGoalReps = true,
                    protocolOverride = if (activation.loggingStyle == AugmentLoggingStyle.CYBER_CRAPP) WorkoutProtocol.CYBER_CRAPP else null
                )
                repository.saveWorkoutLog(workoutLog)

                routineExercise.sets.forEach { routineSet ->
                    val isClusterType = (activation.loggingStyle == AugmentLoggingStyle.CYBER_CRAPP || (activation.loggingStyle == AugmentLoggingStyle.INHERIT && protocol == WorkoutProtocol.CYBER_CRAPP)) && 
                                       routineSet.type == SetType.REST_PAUSE
                    
                    if (isClusterType) {
                         for (i in 1..3) {
                            globalTimestamp = globalTimestamp.plusMillis(1)
                            repository.saveSetLog(SetLog(
                                id = java.util.UUID.randomUUID().toString(),
                                workoutLogId = workoutLog.id,
                                weight = routineSet.weight,
                                reps = 0,
                                type = SetType.REST_PAUSE,
                                goalReps = routineSet.goalReps,
                                clusterMiniSetIndex = i,
                                timestamp = globalTimestamp
                            ))
                        }
                    } else {
                        globalTimestamp = globalTimestamp.plusMillis(1)
                        repository.saveSetLog(SetLog(
                            id = java.util.UUID.randomUUID().toString(),
                            workoutLogId = workoutLog.id,
                            weight = routineSet.weight,
                            reps = routineSet.reps,
                            type = routineSet.type,
                            goalReps = routineSet.goalReps,
                            timestamp = globalTimestamp
                        ))
                    }
                }
                loadPreviousData(routineExercise.exercise.id)
            }
        }
        return currentOrder
    }

    private suspend fun syncWorkoutReminders(profile: UserWorkoutProfile) {
        val protocol = profile.activeProtocol ?: return

        // 1. Clear existing workout schedule/reminders to avoid duplicates
        val existingTasks = ascensionRepository.getAllRecurringTasks().first()
        existingTasks.filter { task ->
            task.tags.contains("workout_session") || task.tags.any { it.startsWith("protocol_") }
        }.forEach { task ->
            ascensionRepository.deleteTask(task.id)
        }

        val activeCycle = repository.getActiveCycle(profile.userId).first()
        
        // 2. HST SD logic: disable or skip training reminders until sdUntil
        if (protocol == WorkoutProtocol.HST && activeCycle != null && hstEngine.isSdActive(activeCycle)) {
            val config = hstEngine.parseConfig(activeCycle.configJson)
            if (config.sdUntil != null) {
                val retestTask = AscensionTask(
                    id = UUID.randomUUID().toString(),
                    parentId = null,
                    title = "RETEST WINDOW: HST NEURAL RECALIBRATION",
                    description = "Neural recovery complete. Perform new RM testing today.",
                    type = AscensionTaskType.ONE_TIME,
                    timeWindows = listOf("09:00"),
                    reminderEnabled = true,
                    xpValue = 50,
                    tags = listOf("workout_session", "protocol_HST", "sd_retest")
                )
                ascensionRepository.insertTask(retestTask)
            }
            return
        }

        // 3. Normal schedule reminders
        profile.scheduledDays.forEach { scheduled ->
            val task = AscensionTask(
                id = UUID.randomUUID().toString(),
                parentId = null,
                title = "TRAINING SESSION: ${protocol.displayName}",
                description = "Sync with the next routine in your protocol rotation.",
                type = AscensionTaskType.RECURRING,
                recurrence = com.neon.ascent.core.domain.goals.models.RecurrenceV3(
                    type = com.neon.ascent.core.domain.goals.models.RecurrenceTypeV3.DAYS_OF_WEEK,
                    daysOfWeek = setOf(java.time.DayOfWeek.of(scheduled.dayOfWeek))
                ),
                timeWindows = listOf(scheduled.time),
                reminderEnabled = true,
                xpValue = 25,
                tags = listOf("workout_session", "protocol_${protocol.name}")
            )
            ascensionRepository.insertTask(task)
        }
    }

    fun cancelProtocolConfiguration() {
        _uiState.update { it.copy(configuringProtocol = null, tempConfigProfile = null) }
    }

    fun initiateDeactivateProtocol() {
        _uiState.update { it.copy(showDeactivateProtocolDialog = true) }
    }

    fun confirmDeactivateProtocol(removeRoutines: Boolean) {
        val profile = _uiState.value.userProfile ?: return
        val protocol = profile.activeProtocol ?: return

        viewModelScope.launch {
            if (removeRoutines) {
                repository.getAllRoutines().first()
                    .filter { it.protocol == protocol }
                    .forEach { routine ->
                        repository.saveRoutine(routine.copy(isAddedToLibrary = false))
                    }
            }

            // Clear scheduling / tasks associated with this protocol or workout sessions
            val existingTasks = ascensionRepository.getAllRecurringTasks().first()
            existingTasks.filter { task ->
                task.tags.contains("workout_session") || task.tags.any { it.startsWith("protocol_") }
            }.forEach { task ->
                ascensionRepository.deleteTask(task.id)
            }

            val updatedProfile = profile.copy(activeProtocol = null)
            repository.saveUserProfile(updatedProfile)
            _uiState.update { it.copy(showDeactivateProtocolDialog = false, userProfile = updatedProfile) }
        }
    }

    fun cancelDeactivateProtocol() {
        _uiState.update { it.copy(showDeactivateProtocolDialog = false) }
    }

    private fun startWorkoutTimer() {
        workoutDurationJob?.cancel()
        workoutDurationJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                if (!_uiState.value.isPaused) {
                    _uiState.update { it.copy(workoutDurationSeconds = it.workoutDurationSeconds + 1) }
                }
            }
        }
    }

    fun startSession(protocol: WorkoutProtocol, isDeload: Boolean = false) {
        if (_uiState.value.session != null) {
            _uiState.update { it.copy(activeSessionError = "A workout session is already in progress. Please finish or discard it before starting a new one.") }
            return
        }
        val sessionId = UUID.randomUUID().toString()
        val session = WorkoutSession(
            id = sessionId, 
            protocol = protocol,
            isDeload = isDeload
        )
        _uiState.update { it.copy(
            session = session, 
            isLoading = true, 
            workoutDurationSeconds = 0, 
            isPaused = false,
            previousLogs = emptyMap(),
            workoutPhase = if (protocol == WorkoutProtocol.CYBER_CRAPP && !isDeload) RestPausePhase.MINI_SET_1 else RestPausePhase.NOT_ACTIVE
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
        if (_uiState.value.previousLogs.containsKey(exerciseId) && 
            _uiState.value.progressionStates.containsKey(exerciseId) &&
            _uiState.value.accomplishments.containsKey(exerciseId)) return
        
        viewModelScope.launch {
            val setsFlow = repository.getLatestSetsForExercise(exerciseId, currentSessionId)
            val stateFlow = repository.getProgressionState(exerciseId)
            val accomplishmentFlow = repository.getAccomplishments(exerciseId)

            combine(setsFlow, stateFlow, accomplishmentFlow) { sets, state, accomplishment ->
                Triple(sets, state, accomplishment)
            }.collect { (sets, state, accomplishment) ->
                _uiState.update { 
                    val newLogs = it.previousLogs.toMutableMap()
                    newLogs[exerciseId] = sets
                    
                    val newStates = it.progressionStates.toMutableMap()
                    if (state != null) {
                        newStates[exerciseId] = state
                    }

                    val newAccomplishments = it.accomplishments.toMutableMap()
                    if (accomplishment != null) {
                        newAccomplishments[exerciseId] = accomplishment
                    }
                    
                    it.copy(
                        previousLogs = newLogs,
                        progressionStates = newStates,
                        accomplishments = newAccomplishments
                    )
                }
            }
        }
    }

    fun startStartingStrengthCycle(initialWeights: Map<String, Float>) {
        val userId = _uiState.value.userProfile?.userId ?: return
        viewModelScope.launch {
            // Deactivate any existing active cycles for this user
            repository.getActiveCycle(userId).first()?.let {
                repository.saveProtocolCycle(it.copy(status = CycleStatus.COMPLETED))
            }

            // 1. Create ExerciseMax entries
            initialWeights.forEach { (familyId, weight) ->
                repository.upsertExerciseMax(
                    ExerciseMax(
                        familyId = familyId,
                        testedAt = java.time.Instant.now(),
                        oneRepMax = 0f,
                        trainingMax = weight,
                        source = MaxSource.MANUAL
                    )
                )
            }

            // 2. Start Cycle
            val cycle = ProtocolCycle(
                id = UUID.randomUUID().toString(),
                userId = userId,
                protocol = WorkoutProtocol.STARTING_STRENGTH,
                startedAt = java.time.Instant.now(),
                status = CycleStatus.ACTIVE,
                currentWeek = 1,
                currentDayIndex = 0,
                configJson = "{ \"strikes\": {} }"
            )
            repository.saveProtocolCycle(cycle)
            
            // 3. Update profile
            _uiState.value.userProfile?.let {
                repository.saveUserProfile(it.copy(activeProtocol = WorkoutProtocol.STARTING_STRENGTH))
            }

            kotlinx.coroutines.delay(100)
            startStartingStrengthSession()
        }
    }

    fun startHstCycle(rms: Map<String, Triple<Float?, Float?, Float?>>) {
        val userId = _uiState.value.userProfile?.userId ?: return
        viewModelScope.launch {
            // Deactivate any existing active cycles for this user
            repository.getActiveCycle(userId).first()?.let {
                repository.saveProtocolCycle(it.copy(status = CycleStatus.COMPLETED))
            }

            rms.forEach { (familyId, rmValues) ->
                repository.upsertExerciseMax(
                    ExerciseMax(
                        familyId = familyId,
                        testedAt = java.time.Instant.now(),
                        oneRepMax = 0f,
                        rm15 = rmValues.first,
                        rm10 = rmValues.second,
                        rm5 = rmValues.third,
                        source = MaxSource.MANUAL
                    )
                )
            }

            val cycle = ProtocolCycle(
                id = UUID.randomUUID().toString(),
                userId = userId,
                protocol = WorkoutProtocol.HST,
                startedAt = java.time.Instant.now(),
                status = CycleStatus.ACTIVE,
                currentWeek = 1,
                currentDayIndex = 0,
                configJson = "{ \"holds\": {} }"
            )
            repository.saveProtocolCycle(cycle)
            
            _uiState.value.userProfile?.let {
                repository.saveUserProfile(it.copy(activeProtocol = WorkoutProtocol.HST))
            }

            kotlinx.coroutines.delay(100)
            startHstSession()
        }
    }

    fun startFiveThreeOneCycle(maxes: Map<String, Float>) {
        val userId = _uiState.value.userProfile?.userId ?: return
        viewModelScope.launch {
            // Deactivate any existing active cycles for this user
            repository.getActiveCycle(userId).first()?.let {
                repository.saveProtocolCycle(it.copy(status = CycleStatus.COMPLETED))
            }

            maxes.forEach { (familyId, oneRM) ->
                repository.upsertExerciseMax(
                    ExerciseMax(
                        familyId = familyId,
                        testedAt = java.time.Instant.now(),
                        oneRepMax = oneRM,
                        trainingMax = oneRM * 0.9f,
                        source = MaxSource.MANUAL
                    )
                )
            }

            val cycle = ProtocolCycle(
                id = UUID.randomUUID().toString(),
                userId = userId,
                protocol = WorkoutProtocol.FIVE_THREE_ONE,
                startedAt = java.time.Instant.now(),
                status = CycleStatus.ACTIVE,
                currentWeek = 1,
                currentDayIndex = 0
            )
            repository.saveProtocolCycle(cycle)
            
            _uiState.value.userProfile?.let {
                repository.saveUserProfile(it.copy(activeProtocol = WorkoutProtocol.FIVE_THREE_ONE))
            }

            // Small delay to ensure DB Flow propagation before gate check
            kotlinx.coroutines.delay(100)
            startFiveThreeOneSession()
        }
    }

    fun startWestsideCycle(maxes: Map<String, Float>) {
        val userId = _uiState.value.userProfile?.userId ?: return
        viewModelScope.launch {
            // Deactivate any existing active cycles for this user
            repository.getActiveCycle(userId).first()?.let {
                repository.saveProtocolCycle(it.copy(status = CycleStatus.COMPLETED))
            }

            maxes.forEach { (familyId, oneRM) ->
                repository.upsertExerciseMax(
                    ExerciseMax(
                        familyId = familyId,
                        testedAt = java.time.Instant.now(),
                        oneRepMax = oneRM,
                        trainingMax = oneRM, // TM is same as 1RM for Westside
                        source = MaxSource.MANUAL
                    )
                )
            }

            val cycle = ProtocolCycle(
                id = UUID.randomUUID().toString(),
                userId = userId,
                protocol = WorkoutProtocol.WESTSIDE,
                startedAt = java.time.Instant.now(),
                status = CycleStatus.ACTIVE,
                currentWeek = 1,
                currentDayIndex = 0,
                configJson = "{ \"indices\": {} }"
            )
            repository.saveProtocolCycle(cycle)
            
            _uiState.value.userProfile?.let {
                repository.saveUserProfile(it.copy(activeProtocol = WorkoutProtocol.WESTSIDE))
            }

            kotlinx.coroutines.delay(100)
            startWestsideSession()
        }
    }

    fun startWestsideSession(dayType: ProtocolDayType? = null) {
        if (_uiState.value.session != null) return
        
        // Optimistically clear the intake gate to prevent flashing loops
        _uiState.update { it.copy(protocolIntakeNeeded = null) }

        viewModelScope.launch {
            val userProfile = _uiState.value.userProfile ?: return@launch
            val activeCycle = repository.getActiveCycle(userProfile.userId).first()
            val engine = westsideEngine

            // GATE: Check for missing data
            val requiredFamilies = listOf("squat", "bench_press", "deadlift")
            val missingData = requiredFamilies.any { familyId ->
                repository.getExerciseMax(familyId).first()?.oneRepMax == 0f
            }

            if (activeCycle == null || missingData) {
                _uiState.update { it.copy(protocolIntakeNeeded = WorkoutProtocol.WESTSIDE) }
                return@launch
            }

            val actualDayType = dayType ?: when (activeCycle.currentDayIndex % 4) {
                0 -> ProtocolDayType.WS_ME_LOWER
                1 -> ProtocolDayType.WS_DE_UPPER
                2 -> ProtocolDayType.WS_ME_UPPER
                else -> ProtocolDayType.WS_DE_LOWER
            }
            
            val exerciseId = engine.getCurrentVariant(activeCycle.configJson, actualDayType) ?: "back_squat"
            val mainExercise = repository.getExerciseById(exerciseId) ?: return@launch
            
            val sessionId = UUID.randomUUID().toString()
            val session = WorkoutSession(
                id = sessionId,
                protocol = WorkoutProtocol.WESTSIDE,
                cycleId = activeCycle.id,
                protocolDayType = actualDayType
            )
            
            val uiMode = if (actualDayType == ProtocolDayType.WS_ME_LOWER || actualDayType == ProtocolDayType.WS_ME_UPPER) {
                ProtocolUiMode.MAX_EFFORT
            } else {
                ProtocolUiMode.DYNAMIC
            }
            
            repository.saveSession(session)
            _uiState.update { it.copy(
                session = session, 
                previousLogs = emptyMap(), 
                isLoading = true, 
                currentUiMode = uiMode,
                protocolIntakeNeeded = null
            ) }

            var currentOrder = 0
            var globalSetTimestamp = java.time.Instant.now()

            val workoutLog = WorkoutLog(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                exerciseId = mainExercise.id,
                order = currentOrder++,
                exerciseName = mainExercise.name
            )
            repository.saveWorkoutLog(workoutLog)

            val exerciseMax = repository.getExerciseMax(mainExercise.familyId).first()
            
            val prescribedSets = mutableListOf<PrescribedSet>()
            if (uiMode == ProtocolUiMode.MAX_EFFORT) {
                for (i in 0 until 6) {
                    prescribedSets.add(engine.prescribe(activeCycle, actualDayType, mainExercise, i, false, exerciseMax))
                }
            } else {
                val setCount = if (actualDayType == ProtocolDayType.WS_DE_LOWER) 10 else 9
                // 3 Warmups
                for (i in 0 until 3) {
                    prescribedSets.add(engine.prescribe(activeCycle, actualDayType, mainExercise, i, false, exerciseMax))
                }
                // Work Sets
                for (i in 0 until setCount) {
                    prescribedSets.add(engine.prescribe(activeCycle, actualDayType, mainExercise, 3, false, exerciseMax))
                }
            }

            prescribedSets.forEach { ps ->
                globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                repository.saveSetLog(SetLog(
                    id = UUID.randomUUID().toString(),
                    workoutLogId = workoutLog.id,
                    weight = ps.weight,
                    reps = 0,
                    type = ps.setType,
                    goalReps = ps.reps.toString(),
                    prescribedWeight = ps.weight,
                    prescribedReps = ps.reps,
                    percentOfMax = ps.percentOfMax,
                    timestamp = globalSetTimestamp
                ))
            }

            appendAttachedAugments(sessionId, session.protocol, session.protocolDayType, currentOrder)

            // Accessories for WS_RE day or as extras?
            // Prompt says: WS_RE: 3 accessories straight sets 3x10-15.
            if (actualDayType == ProtocolDayType.WS_RE) {
                 // Build a session with just accessories
            }

            loadPreviousData(mainExercise.id)

            sessionJob?.cancel()
            sessionJob = viewModelScope.launch {
                repository.getLogsForSession(sessionId).collect { logs ->
                    _uiState.update { it.copy(logs = logs, isLoading = false) }
                }
            }
            startWorkoutTimer()
        }
    }

    fun startDupCycle(initialWeights: Map<String, Float>) {
        val userId = _uiState.value.userProfile?.userId ?: return
        viewModelScope.launch {
            // Deactivate any existing active cycles for this user
            repository.getActiveCycle(userId).first()?.let {
                repository.saveProtocolCycle(it.copy(status = CycleStatus.COMPLETED))
            }

            initialWeights.forEach { (familyId, weight) ->
                repository.upsertExerciseMax(
                    ExerciseMax(
                        familyId = familyId,
                        testedAt = java.time.Instant.now(),
                        oneRepMax = 0f,
                        trainingMax = weight,
                        source = MaxSource.MANUAL
                    )
                )
            }

            val cycle = ProtocolCycle(
                id = UUID.randomUUID().toString(),
                userId = userId,
                protocol = WorkoutProtocol.DUP,
                startedAt = java.time.Instant.now(),
                status = CycleStatus.ACTIVE,
                currentWeek = 1,
                currentDayIndex = 0
            )
            repository.saveProtocolCycle(cycle)
            
            _uiState.value.userProfile?.let {
                repository.saveUserProfile(it.copy(activeProtocol = WorkoutProtocol.DUP))
            }

            kotlinx.coroutines.delay(100)
            startDupSession()
        }
    }

    fun startDupSession(providedRoutine: WorkoutRoutine? = null) {
        if (_uiState.value.session != null) return
        
        // Optimistically clear the intake gate to prevent flashing loops
        _uiState.update { it.copy(protocolIntakeNeeded = null) }

        viewModelScope.launch {
            val userProfile = _uiState.value.userProfile ?: return@launch
            val activeCycle = repository.getActiveCycle(userProfile.userId).first() ?: return@launch
            val engine = dupEngine
            
            val nextDayType = engine.getDayType(activeCycle.currentDayIndex)
            
            // GATE: Check for missing data
            val requiredFamilies = listOf("squat", "bench_press", "rows")
            val missingData = requiredFamilies.any { familyId ->
                repository.getExerciseMax(familyId).first()?.trainingMax == null
            }
            
            if (missingData) {
                _uiState.update { it.copy(protocolIntakeNeeded = WorkoutProtocol.DUP) }
                return@launch
            }

            // Lifts: Use provided routine if present, else default DUP trio
            val exercises = if (providedRoutine != null && providedRoutine.exercises.isNotEmpty()) {
                providedRoutine.exercises.map { it.exercise }
            } else {
                listOf("back_squat", "bench_press", "bent_over_row").mapNotNull { repository.getExerciseById(it) }
            }
            
            val sessionId = UUID.randomUUID().toString()
            val session = WorkoutSession(
                id = sessionId,
                protocol = WorkoutProtocol.DUP,
                cycleId = activeCycle.id,
                protocolDayType = nextDayType
            )
            
            val uiMode = if (nextDayType == ProtocolDayType.DUP_POWER) ProtocolUiMode.DYNAMIC else ProtocolUiMode.LINEAR
            
            repository.saveSession(session)
            _uiState.update { it.copy(
                session = session, 
                previousLogs = emptyMap(), 
                isLoading = true, 
                currentUiMode = uiMode,
                protocolIntakeNeeded = null
            ) }

            var globalSetTimestamp = java.time.Instant.now()

            exercises.forEachIndexed { logIndex, exercise ->
                val workoutLog = WorkoutLog(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    order = logIndex,
                    exerciseName = exercise.name
                )
                repository.saveWorkoutLog(workoutLog)

                val exerciseMax = repository.getExerciseMax(exercise.familyId).first()
                val (setCount, defaultGoal) = when (nextDayType) {
                    ProtocolDayType.DUP_HYPERTROPHY -> 3 to "8-12"
                    ProtocolDayType.DUP_STRENGTH -> 4 to "3-5"
                    ProtocolDayType.DUP_POWER -> 6 to "2-3"
                    else -> 3 to "8"
                }
                
                // Warmups (1-2)
                for (i in 0 until 2) {
                    val ps = engine.prescribe(activeCycle, nextDayType, exercise, i, false, exerciseMax)
                    globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                    repository.saveSetLog(SetLog(
                        id = UUID.randomUUID().toString(),
                        workoutLogId = workoutLog.id,
                        weight = ps.weight,
                        reps = 0,
                        type = SetType.WARMUP,
                        goalReps = ps.reps.toString(),
                        prescribedWeight = ps.weight,
                        prescribedReps = ps.reps,
                        timestamp = globalSetTimestamp
                    ))
                }

                // Work Sets
                for (i in 0 until setCount) {
                    val ps = engine.prescribe(activeCycle, nextDayType, exercise, 2, false, exerciseMax)
                    globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                    repository.saveSetLog(SetLog(
                        id = UUID.randomUUID().toString(),
                        workoutLogId = workoutLog.id,
                        weight = ps.weight,
                        reps = 0,
                        type = ps.setType,
                        goalReps = defaultGoal,
                        prescribedWeight = ps.weight,
                        prescribedReps = ps.reps,
                        timestamp = globalSetTimestamp
                    ))
                }
                loadPreviousData(exercise.id)
            }

            appendAttachedAugments(sessionId, session.protocol, session.protocolDayType, exercises.size)

            sessionJob?.cancel()
            sessionJob = viewModelScope.launch {
                repository.getLogsForSession(sessionId).collect { logs ->
                    _uiState.update { it.copy(logs = logs, isLoading = false) }
                }
            }
            startWorkoutTimer()
        }
    }

    fun startFiveThreeOneSession() {
        if (_uiState.value.session != null) return
        
        // Optimistically clear the intake gate to prevent flashing loops
        _uiState.update { it.copy(protocolIntakeNeeded = null) }

        viewModelScope.launch {
            val userProfile = _uiState.value.userProfile ?: return@launch
            val activeCycle = repository.getActiveCycle(userProfile.userId).first()

            // GATE: Check for missing data
            val requiredFamilies = listOf("overhead_press", "deadlift", "bench_press", "squat")
            val missingData = requiredFamilies.any { familyId ->
                repository.getExerciseMax(familyId).first()?.trainingMax == null
            }

            if (activeCycle == null || missingData) {
                _uiState.update { it.copy(protocolIntakeNeeded = WorkoutProtocol.FIVE_THREE_ONE) }
                return@launch
            }

            val engine = fiveThreeOneEngine
            val nextDayType = engine.getDayType(activeCycle.currentDayIndex)
            val mainExerciseId = engine.getMainExerciseId(activeCycle.currentDayIndex)
            val mainExercise = repository.getExerciseById(mainExerciseId) ?: return@launch
            
            val sessionId = UUID.randomUUID().toString()
            val session = WorkoutSession(
                id = sessionId,
                protocol = WorkoutProtocol.FIVE_THREE_ONE,
                cycleId = activeCycle.id,
                protocolDayType = nextDayType
            )
            
            repository.saveSession(session)
            _uiState.update { it.copy(
                session = session, 
                previousLogs = emptyMap(), 
                isLoading = true, 
                currentUiMode = ProtocolUiMode.LINEAR,
                protocolIntakeNeeded = null
            ) }

            var currentOrder = 0
            var globalSetTimestamp = java.time.Instant.now()

            // 1. Main Lift
            val workoutLog = WorkoutLog(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                exerciseId = mainExercise.id,
                order = currentOrder++,
                exerciseName = mainExercise.name
            )
            repository.saveWorkoutLog(workoutLog)

            val mainMax = repository.getExerciseMax(mainExercise.familyId).first()
            
            // 3 Warmups + 3 Work Sets
            for (i in 0 until 6) {
                val ps = engine.prescribe(activeCycle, nextDayType, mainExercise, i, false, mainMax)
                globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                repository.saveSetLog(SetLog(
                    id = UUID.randomUUID().toString(),
                    workoutLogId = workoutLog.id,
                    weight = ps.weight,
                    reps = 0,
                    type = ps.setType,
                    goalReps = if (ps.isAmrap) "${ps.reps}+" else ps.reps.toString(),
                    prescribedWeight = ps.weight,
                    prescribedReps = ps.reps,
                    percentOfMax = ps.percentOfMax,
                    isAmrap = ps.isAmrap,
                    timestamp = globalSetTimestamp
                ))
            }

            // 2. BBB Assistance (Boring But Big)
            val complementaryId = engine.getComplementaryExerciseId(mainExerciseId)
            val compExercise = complementaryId?.let { repository.getExerciseById(it) }
            val compMax = compExercise?.let { repository.getExerciseMax(it.familyId).first() }
            
            if (compExercise != null && compMax != null) {
                val compLog = WorkoutLog(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    exerciseId = compExercise.id,
                    order = currentOrder++,
                    exerciseName = compExercise.name
                )
                repository.saveWorkoutLog(compLog)
                
                val bbbWeight = (compMax.trainingMax ?: (compMax.oneRepMax * 0.9f)) * 0.5f
                val roundedBBB = (Math.round(bbbWeight / 2.5f) * 2.5f).toFloat()
                
                for (i in 0 until 5) {
                    globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                    repository.saveSetLog(SetLog(
                        id = UUID.randomUUID().toString(),
                        workoutLogId = compLog.id,
                        weight = roundedBBB,
                        reps = 0,
                        type = SetType.NORMAL,
                        goalReps = "10",
                        prescribedWeight = roundedBBB,
                        prescribedReps = 10,
                        percentOfMax = 0.5f,
                        timestamp = globalSetTimestamp
                    ))
                }
            }

            appendAttachedAugments(sessionId, session.protocol, session.protocolDayType, currentOrder)

            loadPreviousData(mainExercise.id)
            if (compExercise != null) loadPreviousData(compExercise.id)

            appendAttachedAugments(sessionId, session.protocol, session.protocolDayType, currentOrder)

            sessionJob?.cancel()
            sessionJob = viewModelScope.launch {
                repository.getLogsForSession(sessionId).collect { logs ->
                    _uiState.update { it.copy(logs = logs, isLoading = false) }
                }
            }
            startWorkoutTimer()
        }
    }

    fun startHstSession() {
        if (_uiState.value.session != null) return
        
        // Optimistically clear the intake gate to prevent flashing loops
        _uiState.update { it.copy(protocolIntakeNeeded = null) }

        viewModelScope.launch {
            val userProfile = _uiState.value.userProfile ?: return@launch
            val activeCycle = repository.getActiveCycle(userProfile.userId).first()
            val engine = hstEngine

            // GATE: Check for missing data (use familyId, not exercise id)
            val requiredFamilies = listOf("squat", "bench_press", "rows", "overhead_press", "deadlift")
            val missingData = requiredFamilies.any { familyId ->
                val max = repository.getExerciseMax(familyId).first()
                max?.rm15 == null || max.rm10 == null || max.rm5 == null
            }

            if (activeCycle == null || missingData) {
                _uiState.update { it.copy(protocolIntakeNeeded = WorkoutProtocol.HST) }
                return@launch
            }

            val nextDayType = engine.getDayType(activeCycle.currentDayIndex)
            
            if (nextDayType == ProtocolDayType.HST_SD || engine.isSdActive(activeCycle)) {
                _uiState.update { it.copy(isStrategicDeconditioningActive = true) }
                return@launch
            }

            // Seeds for HST session
            val exerciseIds = listOf("back_squat", "bench_press", "bent_over_row", "military_press", "romanian_deadlift")
            val exercises = exerciseIds.mapNotNull { id -> repository.getExerciseById(id) }
            
            val sessionId = UUID.randomUUID().toString()
            val session = WorkoutSession(
                id = sessionId,
                protocol = WorkoutProtocol.HST,
                cycleId = activeCycle.id,
                protocolDayType = nextDayType
            )
            
            repository.saveSession(session)
            _uiState.update { it.copy(
                session = session, 
                previousLogs = emptyMap(), 
                isLoading = true, 
                currentUiMode = ProtocolUiMode.LINEAR,
                protocolIntakeNeeded = null // Clear gate
            ) }

            var currentOrder = 0
            var globalSetTimestamp = java.time.Instant.now()

            exercises.forEach { exercise ->
                val workoutLog = WorkoutLog(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    order = currentOrder++,
                    exerciseName = exercise.name
                )
                repository.saveWorkoutLog(workoutLog)

                val exerciseMax = repository.getExerciseMax(exercise.familyId).first()
                val workSetCount = if (exercise.familyId == "romanian_deadlift" || exercise.familyId == "deadlift") 1 else 2
                
                // Warmup
                val warmupSet = engine.prescribe(activeCycle, nextDayType, exercise, 0, false, exerciseMax)
                globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                repository.saveSetLog(SetLog(
                    id = UUID.randomUUID().toString(),
                    workoutLogId = workoutLog.id,
                    weight = warmupSet.weight,
                    reps = 0,
                    type = SetType.WARMUP,
                    goalReps = warmupSet.reps.toString(),
                    prescribedWeight = warmupSet.weight,
                    prescribedReps = warmupSet.reps,
                    timestamp = globalSetTimestamp
                ))

                // Work Sets
                for (i in 0 until workSetCount) {
                    val ps = engine.prescribe(activeCycle, nextDayType, exercise, 2, false, exerciseMax)
                    globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                    repository.saveSetLog(SetLog(
                        id = UUID.randomUUID().toString(),
                        workoutLogId = workoutLog.id,
                        weight = ps.weight,
                        reps = 0,
                        type = SetType.NORMAL,
                        goalReps = ps.reps.toString(),
                        prescribedWeight = ps.weight,
                        prescribedReps = ps.reps,
                        timestamp = globalSetTimestamp
                    ))
                }
                loadPreviousData(exercise.id)
            }

            appendAttachedAugments(sessionId, session.protocol, session.protocolDayType, exercises.size)

            sessionJob?.cancel()
            sessionJob = viewModelScope.launch {
                repository.getLogsForSession(sessionId).collect { logs ->
                    _uiState.update { it.copy(logs = logs, isLoading = false) }
                }
            }
            startWorkoutTimer()
        }
    }

    fun startStartingStrengthSession() {
        if (_uiState.value.session != null) return
        
        // Optimistically clear the intake gate to prevent flashing loops
        _uiState.update { it.copy(protocolIntakeNeeded = null) }

        viewModelScope.launch {
            val userProfile = _uiState.value.userProfile ?: return@launch
            val activeCycle = repository.getActiveCycle(userProfile.userId).first()
            
            // GATE: Check for missing data
            val requiredFamilies = listOf("squat", "bench_press", "overhead_press", "deadlift")
            val missingData = requiredFamilies.any { familyId ->
                repository.getExerciseMax(familyId).first()?.trainingMax == null
            }
            
            if (activeCycle == null || missingData) {
                _uiState.update { it.copy(protocolIntakeNeeded = WorkoutProtocol.STARTING_STRENGTH) }
                return@launch
            }

            val engine = startingStrengthEngine
            val nextDayType = if (activeCycle.currentDayIndex % 2 == 0) ProtocolDayType.SS_A else ProtocolDayType.SS_B
            
            // Check last DL success
            val lastSession = repository.getAllSessions().first().find { it.protocol == WorkoutProtocol.STARTING_STRENGTH }
            val lastDlFailed = if (lastSession != null) {
                val dlLog = repository.getLogsForSession(lastSession.id).first().find { it.first.exerciseId == "deadlift" }
                dlLog?.let { !engine.sessionSucceeded(it.second) } ?: false
            } else false

            val exerciseIds = engine.getExercisesForDay(nextDayType, lastDlFailed)
            val exercises = exerciseIds.mapNotNull { id -> repository.getExerciseById(id) }
            
            val sessionId = UUID.randomUUID().toString()
            val session = WorkoutSession(
                id = sessionId,
                protocol = WorkoutProtocol.STARTING_STRENGTH,
                cycleId = activeCycle.id,
                protocolDayType = nextDayType
            )
            
            repository.saveSession(session)
            _uiState.update { it.copy(
                session = session, 
                previousLogs = emptyMap(), 
                isLoading = true, 
                currentUiMode = ProtocolUiMode.LINEAR,
                protocolIntakeNeeded = null
            ) }

            var currentOrder = 0
            var globalSetTimestamp = java.time.Instant.now()

            exercises.forEach { exercise ->
                val workoutLog = WorkoutLog(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    order = currentOrder++,
                    exerciseName = exercise.name
                )
                repository.saveWorkoutLog(workoutLog)

                val exerciseMax = repository.getExerciseMax(exercise.familyId).first()
                val workSetCount = if (exercise.familyId == "deadlift") 1 else 3
                
                for (i in 0 until 4) {
                    val ps = engine.prescribe(activeCycle, nextDayType, exercise, i, false, exerciseMax)
                    globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                    repository.saveSetLog(SetLog(
                        id = UUID.randomUUID().toString(),
                        workoutLogId = workoutLog.id,
                        weight = ps.weight,
                        reps = 0,
                        type = ps.setType,
                        goalReps = ps.reps.toString(),
                        prescribedWeight = ps.weight,
                        prescribedReps = ps.reps,
                        timestamp = globalSetTimestamp
                    ))
                }
                for (i in 0 until workSetCount) {
                    val ps = engine.prescribe(activeCycle, nextDayType, exercise, 4, false, exerciseMax)
                    globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                    repository.saveSetLog(SetLog(
                        id = UUID.randomUUID().toString(),
                        workoutLogId = workoutLog.id,
                        weight = ps.weight,
                        reps = 0,
                        type = ps.setType,
                        goalReps = ps.reps.toString(),
                        prescribedWeight = ps.weight,
                        prescribedReps = ps.reps,
                        timestamp = globalSetTimestamp
                    ))
                }
                loadPreviousData(exercise.id)
            }

            appendAttachedAugments(sessionId, session.protocol, session.protocolDayType, exercises.size)

            sessionJob?.cancel()
            sessionJob = viewModelScope.launch {
                repository.getLogsForSession(sessionId).collect { logs ->
                    _uiState.update { it.copy(logs = logs, isLoading = false) }
                }
            }
            startWorkoutTimer()
        }
    }

    fun startRoutine(routine: WorkoutRoutine, isDeload: Boolean = false) {
        if (_uiState.value.session != null) {
            _uiState.update { it.copy(activeSessionError = "A workout session is already in progress. Please finish or discard it before starting a new one.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Ensure we have the full routine data with exercises and sets
            val latestRoutines = repository.getAllRoutines().first()
            var fullRoutine = latestRoutines.find { it.id == routine.id } ?: routine

            if (fullRoutine.exercises.isEmpty()) {
                android.util.Log.e("WorkoutVM", "CRITICAL: Starting routine ${fullRoutine.name} with NO EXERCISES. Seeding fallback.")
                repository.seedStarterExercises()
                val refreshedRoutines = repository.getAllRoutines().first()
                fullRoutine = refreshedRoutines.find { it.id == routine.id } ?: routine
            }

            val sessionId = UUID.randomUUID().toString()
            val userProfile = _uiState.value.userProfile

            // Determine Day Type for Protocols
            val activeCycle = userProfile?.let { repository.getActiveCycle(it.userId).first() }
            val engine = getEngine(fullRoutine.protocol)
            val dayType = if (engine != null && activeCycle != null && activeCycle.protocol == fullRoutine.protocol) {
                (engine as? StartingStrengthEngine)?.let { ss ->
                    ss.getNextDayType(null) // Simple for now: just start with A if we don't track well
                }
            } else null

            val session = WorkoutSession(
                id = sessionId, 
                protocol = fullRoutine.protocol,
                isDeload = isDeload,
                cycleId = activeCycle?.id,
                protocolDayType = dayType
            )

            // 1. Create the session first in DB and UI state
            repository.saveSession(session)
            val uiMode = if (fullRoutine.protocol == WorkoutProtocol.CYBER_CRAPP) ProtocolUiMode.CLUSTER else ProtocolUiMode.LINEAR
            _uiState.update { it.copy(session = session, previousLogs = emptyMap(), currentUiMode = uiMode) }

            var currentOrder = 0
            var globalSetTimestamp = java.time.Instant.now()

            // 2. Batch insert the logs and sets
            fullRoutine.exercises.forEach { routineExercise ->
                val workoutLog = WorkoutLog(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    exerciseId = routineExercise.exercise.id,
                    order = currentOrder++,
                    exerciseName = routineExercise.exercise.name
                )
                repository.saveWorkoutLog(workoutLog)

                val progressionState = _uiState.value.progressionStates[routineExercise.exercise.id]
                val afterWeightJump = progressionState != null && progressionState.bestClusterReps == 0 && progressionState.currentWeight > 0

                val exerciseMax = repository.getExerciseMax(routineExercise.exercise.familyId).first()

                // Engine-based set generation (Excluding CyberCrapp which uses manual expansion below)
                if (engine != null && !isDeload && fullRoutine.protocol != WorkoutProtocol.CYBER_CRAPP) {
                    val prescribedSets = mutableListOf<PrescribedSet>()
                    // Generate 4 warmups + 3 work sets (or 1 for DL)
                    val workSetCount = if (routineExercise.exercise.familyId == "deadlift") 1 else 3
                    
                    for (i in 0 until 4) {
                        prescribedSets.add(engine.prescribe(activeCycle, dayType, routineExercise.exercise, i, afterWeightJump, exerciseMax))
                    }
                    for (i in 0 until workSetCount) {
                        prescribedSets.add(engine.prescribe(activeCycle, dayType, routineExercise.exercise, 4, afterWeightJump, exerciseMax))
                    }

                    prescribedSets.forEach { ps ->
                        globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                        val setLog = SetLog(
                            id = UUID.randomUUID().toString(),
                            workoutLogId = workoutLog.id,
                            weight = ps.weight,
                            reps = 0,
                            type = ps.setType,
                            goalReps = ps.reps.toString(),
                            prescribedWeight = ps.weight,
                            prescribedReps = ps.reps,
                            timestamp = globalSetTimestamp
                        )
                        repository.saveSetLog(setLog)
                    }
                } else {
                    // Fallback to manual routine sets or CC logic
                    routineExercise.sets.forEach { routineSet ->
                        val goalReps = if (routineSet.goalReps != null) {
                            routineSet.goalReps
                        } else if (isDeload && routineSet.type == SetType.REST_PAUSE) {
                            "DELOAD (RIR 3-4)"
                        } else {
                            CyberCrappRules.resolve(
                                protocol = session.protocol,
                                exercise = routineExercise.exercise,
                                setType = routineSet.type,
                                afterWeightJump = afterWeightJump,
                                targets = _uiState.value.repTargets
                            ).label
                        }
                        
                        val setWeight = routineSet.weight

                        if (session.protocol == WorkoutProtocol.CYBER_CRAPP && routineSet.type == SetType.REST_PAUSE) {
                            val typeToSave = if (isDeload) SetType.NORMAL else SetType.REST_PAUSE
                            for (i in 1..3) {
                                globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                                val setLog = SetLog(
                                    id = UUID.randomUUID().toString(),
                                    workoutLogId = workoutLog.id,
                                    weight = setWeight,
                                    reps = if (isDeload) 0 else routineSet.reps,
                                    type = typeToSave,
                                    goalReps = goalReps,
                                    clusterMiniSetIndex = if (isDeload) null else i,
                                    timestamp = globalSetTimestamp,
                                    rir = if (isDeload) 4 else null
                                )
                                repository.saveSetLog(setLog)
                            }
                        } else {
                            globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                            val setLog = SetLog(
                                id = UUID.randomUUID().toString(),
                                workoutLogId = workoutLog.id,
                                weight = setWeight,
                                reps = routineSet.reps,
                                type = routineSet.type,
                                goalReps = goalReps,
                                timestamp = globalSetTimestamp
                            )
                            repository.saveSetLog(setLog)
                        }
                    }
                }
                loadPreviousData(routineExercise.exercise.id)
            }

            // 2b. Append attached augments
            appendAttachedAugments(sessionId, session.protocol, dayType, currentOrder)

            // 3. Start collecting logs
            sessionJob?.cancel()
            sessionJob = viewModelScope.launch {
                repository.getLogsForSession(sessionId).collect { logs ->
                    _uiState.update { it.copy(logs = logs, isLoading = false) }
                }
            }

            _uiState.update { it.copy(
                activeRoutine = fullRoutine,
                workoutDurationSeconds = 0,
                isPaused = false,
                workoutPhase = if (fullRoutine.protocol == WorkoutProtocol.CYBER_CRAPP && !isDeload) RestPausePhase.MINI_SET_1 else RestPausePhase.NOT_ACTIVE
            ) }

            startWorkoutTimer()
        }
    }

    fun startAugment(augment: WorkoutAugment) {
        if (_uiState.value.session != null) {
            _uiState.update { it.copy(activeSessionError = "A workout session is already in progress. Please finish or discard it before starting a new one.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val sessionId = UUID.randomUUID().toString()
            val session = WorkoutSession(id = sessionId) // GENERAL protocol
            repository.saveSession(session)

            var globalSetTimestamp = java.time.Instant.now()

            augment.exercises.forEachIndexed { index, routineExercise ->
                val workoutLog = WorkoutLog(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    exerciseId = routineExercise.exercise.id,
                    order = index,
                    exerciseName = routineExercise.exercise.name,
                    augmentId = augment.id,
                    augmentName = augment.name,
                    augmentColor = augment.colorHex,
                    showGoalReps = augment.isSystem
                )
                repository.saveWorkoutLog(workoutLog)

                routineExercise.sets.forEach { routineSet ->
                    globalSetTimestamp = globalSetTimestamp.plusMillis(1)
                    val setLog = SetLog(
                        id = UUID.randomUUID().toString(),
                        workoutLogId = workoutLog.id,
                        weight = routineSet.weight,
                        reps = routineSet.reps,
                        type = routineSet.type,
                        goalReps = routineSet.goalReps,
                        timestamp = globalSetTimestamp
                    )
                    repository.saveSetLog(setLog)
                }
                loadPreviousData(routineExercise.exercise.id)
            }

            // Start collecting logs
            sessionJob?.cancel()
            sessionJob = launch {
                repository.getLogsForSession(sessionId).collect { logs ->
                    _uiState.update { it.copy(logs = logs, isLoading = false) }
                }
            }

            _uiState.update { it.copy(
                session = session,
                workoutDurationSeconds = 0,
                isPaused = false,
                previousLogs = emptyMap(),
                workoutPhase = RestPausePhase.NOT_ACTIVE
            ) }

            startWorkoutTimer()
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
            _uiState.update { it.copy(
                session = null,
                logs = emptyList(),
                activeRoutine = null,
                isLoading = false,
                isPaused = false,
                workoutDurationSeconds = 0,
                workoutPhase = RestPausePhase.NOT_ACTIVE
            ) }
            workoutDurationJob?.cancel()
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
        
        viewModelScope.launch {
            // If it's a system augment not in library, add it
            if (augment.isSystem && !augment.isAddedToLibrary) {
                toggleAugmentLibrary(augment)
            }

            val currentLogs = repository.getLogsForSession(session.id).first()
            val baseOrder = currentLogs.size
            val now = java.time.Instant.now()
            val groupSupersetId = if (augment.exercises.size > 1) java.util.UUID.randomUUID().toString() else null

            augment.exercises.forEachIndexed { index, routineExercise ->
                val workoutLog = WorkoutLog(
                    id = java.util.UUID.randomUUID().toString(),
                    sessionId = session.id,
                    exerciseId = routineExercise.exercise.id,
                    order = baseOrder + index,
                    exerciseName = routineExercise.exercise.name,
                    augmentId = augment.id,
                    augmentName = augment.name,
                    augmentColor = augment.colorHex,
                    supersetId = groupSupersetId,
                    showGoalReps = augment.isSystem
                )
                repository.saveWorkoutLog(workoutLog)

                // Save prescribed sets for the augment
                routineExercise.sets.forEach { routineSet ->
                    val setLog = SetLog(
                        id = java.util.UUID.randomUUID().toString(),
                        workoutLogId = workoutLog.id,
                        weight = routineSet.weight,
                        reps = routineSet.reps,
                        type = routineSet.type,
                        goalReps = routineSet.goalReps,
                        timestamp = now
                    )
                    repository.saveSetLog(setLog)
                }

                loadPreviousData(routineExercise.exercise.id)
            }
        }
    }

    fun toggleGoalReps(workoutLog: WorkoutLog) {
        viewModelScope.launch {
            repository.updateShowGoalReps(workoutLog.id, !workoutLog.showGoalReps)
        }
    }

    fun updateExerciseNotes(exerciseId: String, notes: String) {
        viewModelScope.launch {
            val exercise = _uiState.value.availableExercises.find { it.id == exerciseId } ?: return@launch
            repository.saveExerciseDefinition(exercise.copy(notes = notes))
        }
    }

    fun logSet(workoutLog: WorkoutLog, weight: Float, reps: Int, type: SetType = SetType.NORMAL) {
        val session = _uiState.value.session ?: return
        
        val phase = _uiState.value.workoutPhase
        val clusterIndex = if (type == SetType.REST_PAUSE) {
            when (phase) {
                RestPausePhase.NOT_ACTIVE -> 1 // Start new cluster
                RestPausePhase.MINI_SET_1 -> 1
                RestPausePhase.MINI_SET_2 -> 2
                RestPausePhase.MINI_SET_3 -> 3
                else -> 1 // Fallback to 1 if we're in a weird state
            }
        } else null

        val exercise = _uiState.value.availableExercises.find { it.id == workoutLog.exerciseId }
        val progressionState = _uiState.value.progressionStates[workoutLog.exerciseId]
        val afterWeightJump = progressionState != null && progressionState.bestClusterReps == 0 && progressionState.currentWeight > 0

        val goalReps = if (type == SetType.WIDOWMAKER) {
            "20"
        } else {
            exercise?.let {
                CyberCrappRules.resolve(
                    protocol = session.protocol,
                    exercise = it,
                    setType = type,
                    afterWeightJump = afterWeightJump,
                    targets = _uiState.value.repTargets
                ).label
            }
        }

        val setWeight = weight

        val setLog = SetLog(
            id = UUID.randomUUID().toString(),
            workoutLogId = workoutLog.id,
            weight = setWeight,
            reps = reps,
            type = type,
            goalReps = goalReps,
            clusterMiniSetIndex = clusterIndex
        )
        
        // Immediate UI feedback and state consistency for sequential updates (e.g. Phase change)
        _uiState.update { state ->
            val newLogs = state.logs.map { (log, sets) ->
                if (log.id == workoutLog.id) {
                    log to (sets + setLog).sortedWith(compareBy({ it.timestamp }, { it.id }))
                } else {
                    log to sets
                }
            }
            state.copy(logs = newLogs)
        }

        viewModelScope.launch {
            repository.saveSetLog(setLog)
            updateComparisonText(workoutLog.exerciseId)
            
            if (type == SetType.REST_PAUSE) {
                expandToCluster(setLog)
                handleRestPauseLogic(setLog)
            }
        }
    }

    private fun updateComparisonText(exerciseId: String) {
        val previousSets = _uiState.value.previousLogs[exerciseId] ?: return
        val currentLogPair = _uiState.value.logs.find { it.first.exerciseId == exerciseId } ?: return
        val currentSets = currentLogPair.second
        
        val isCyberCrapp = _uiState.value.session?.protocol == WorkoutProtocol.CYBER_CRAPP
        
        if (isCyberCrapp) {
            val prevClusterTotal = previousSets.filter { it.type == SetType.REST_PAUSE }.sumOf { it.reps }
            val currentClusterTotal = currentSets.filter { it.type == SetType.REST_PAUSE }.sumOf { it.reps }
            
            if (currentClusterTotal > 0 && prevClusterTotal > 0) {
                val diff = currentClusterTotal - prevClusterTotal
                val sign = if (diff >= 0) "+" else ""
                val text = "$sign$diff vs last 🔥"
                _uiState.update { it.copy(comparisonText = text) }
            }
        } else {
            // For general, compare top set
            val prevMaxWeight = previousSets.maxOfOrNull { it.weight } ?: 0f
            val currentMaxWeight = currentSets.maxOfOrNull { it.weight } ?: 0f
            
            if (currentMaxWeight > prevMaxWeight) {
                _uiState.update { it.copy(comparisonText = "New Weight PR! 🚀") }
            } else if (currentMaxWeight == prevMaxWeight && currentMaxWeight > 0) {
                val prevMaxReps = previousSets.filter { it.weight == prevMaxWeight }.maxOfOrNull { it.reps } ?: 0
                val currentMaxReps = currentSets.filter { it.weight == currentMaxWeight }.maxOfOrNull { it.reps } ?: 0
                if (currentMaxReps > prevMaxReps) {
                    _uiState.update { it.copy(comparisonText = "+${currentMaxReps - prevMaxReps} reps vs last 🔥") }
                }
            }
        }
    }

    fun updateSet(setLog: SetLog, weight: Float? = null, reps: Int? = null, type: SetType? = null, goalReps: String? = null, isCompleted: Boolean? = null, rir: Int? = null) {
        val currentBase = pendingUpdates[setLog.id] ?: setLog
        val newType = type ?: currentBase.type
        val newGoalReps = if (type == SetType.WIDOWMAKER && currentBase.type != SetType.WIDOWMAKER) {
            "20"
        } else {
            goalReps ?: currentBase.goalReps
        }

        // If changing AWAY from RestPause, clear the index
        val newClusterIndex = if (type != null && type != SetType.REST_PAUSE) {
            null
        } else if (type == SetType.REST_PAUSE && currentBase.type != SetType.REST_PAUSE) {
            1
        } else {
            currentBase.clusterMiniSetIndex
        }

        val updatedSet = currentBase.copy(
            weight = weight ?: currentBase.weight,
            reps = reps ?: currentBase.reps,
            type = newType,
            goalReps = newGoalReps,
            isCompleted = isCompleted ?: currentBase.isCompleted,
            rir = rir ?: currentBase.rir,
            clusterMiniSetIndex = newClusterIndex
        )

        // UX: If user entered reps but weight is still 0, auto-fill from history/bodyweight
        val finalWeight = if (reps != null && reps > 0 && updatedSet.weight == 0f) {
            getAutoFillWeight(updatedSet)
        } else {
            updatedSet.weight
        }

        val finalizedSet = updatedSet.copy(weight = finalWeight)
        
        // Immediate UI feedback and state consistency for sequential updates (e.g. Phase change)
        _uiState.update { state ->
            val newLogs = state.logs.map { (log, sets) ->
                if (log.id == finalizedSet.workoutLogId) {
                    log to sets.map { s -> if (s.id == finalizedSet.id) finalizedSet else s }
                } else {
                    log to sets
                }
            }
            state.copy(logs = newLogs)
        }

        pendingUpdates[setLog.id] = finalizedSet
        
        updateJobs[setLog.id]?.cancel()
        updateJobs[setLog.id] = viewModelScope.launch {
            // Delay only for text input fields, not for checkboxes or type selectors
            if (isCompleted == null && type == null) {
                kotlinx.coroutines.delay(300)
            }
            
            repository.saveSetLog(finalizedSet)
            pendingUpdates.remove(setLog.id)
            
            val log = _uiState.value.logs.find { it.second.any { s -> s.id == setLog.id } }?.first
            if (log != null) {
                updateComparisonText(log.exerciseId)
            }

            // Cleanup extra sets if we moved away from RP
            if (type != null && type != SetType.REST_PAUSE && currentBase.type == SetType.REST_PAUSE) {
                val setsForLog = _uiState.value.logs.find { it.first.id == finalizedSet.workoutLogId }?.second ?: emptyList()
                setsForLog.filter { it.id != finalizedSet.id && it.clusterMiniSetIndex != null }.forEach {
                    repository.deleteSetLog(it.id)
                }
            }

            if (finalizedSet.type == SetType.REST_PAUSE) {
                handleRestPauseLogic(finalizedSet)
            }

            if (finalizedSet.type == SetType.GS && isCompleted == true) {
                handleGiantSetLogic(finalizedSet)
            }

            // Auto-start rest timer for appropriate sets if enabled
            if (isCompleted == true && _uiState.value.isAutoStartTimerEnabled && finalizedSet.clusterMiniSetIndex == null) {
                if (finalizedSet.type != SetType.PARTIAL && finalizedSet.type != SetType.STRETCH) {
                    triggerRestTimer(finalizedSet)
                }
            }

            if (type == SetType.REST_PAUSE && currentBase.type != SetType.REST_PAUSE) {
                expandToCluster(finalizedSet)
            }
        }
    }

    private fun getAutoFillWeight(setLog: SetLog): Float {
        val state = _uiState.value
        val log = state.logs.find { it.first.id == setLog.workoutLogId }?.first ?: return 0f
        val prevSets = state.previousLogs[log.exerciseId] ?: emptyList()
        val progressionState = state.progressionStates[log.exerciseId]

        return if (setLog.clusterMiniSetIndex != null) {
            val prevClusterSets = prevSets.filter { it.clusterMiniSetIndex != null }
            prevClusterSets.firstOrNull()?.weight 
                ?: progressionState?.currentWeight 
                ?: 0f
        } else {
            val currentSets = state.logs.find { it.first.id == setLog.workoutLogId }?.second ?: emptyList()
            val typeSets = currentSets.filter { it.type == setLog.type && it.clusterMiniSetIndex == null }
            val index = typeSets.indexOfFirst { it.id == setLog.id }
            
            val prevTypeSets = prevSets.filter { it.type == setLog.type && it.clusterMiniSetIndex == null }
            val prevSet = prevTypeSets.getOrNull(index)
            
            val prevWFallback = if (setLog.type != SetType.WARMUP) {
                progressionState?.currentWeight ?: 0f
            } else {
                0f
            }
            
            if (prevSet != null && prevSet.weight > 0) prevSet.weight else prevWFallback
        }
    }

    private suspend fun expandToCluster(setLog: SetLog) {
        // Use the logs already present in UI state to avoid repo Flow delay
        val currentLogs = _uiState.value.logs
        val logPair = currentLogs.find { it.first.id == setLog.workoutLogId }
        val setsForLog = logPair?.second ?: emptyList()
        
        if (setsForLog.count { it.clusterMiniSetIndex != null } >= 3) return

        val log = logPair?.first
        val exercise = _uiState.value.availableExercises.find { it.id == log?.exerciseId }
        val progressionState = exercise?.let { _uiState.value.progressionStates[it.id] }
        val afterWeightJump = progressionState != null && progressionState.bestClusterReps == 0 && progressionState.currentWeight > 0
        
        val goalReps = if (log != null && exercise != null) {
            CyberCrappRules.resolve(
                protocol = _uiState.value.session?.protocol ?: WorkoutProtocol.GENERAL,
                exercise = exercise,
                setType = SetType.REST_PAUSE,
                afterWeightJump = afterWeightJump,
                targets = _uiState.value.repTargets
            ).label
        } else setLog.goalReps

        // Update original to be index 1 (already done in updateSet, but ensure here too)
        if (setLog.clusterMiniSetIndex != 1) {
            repository.saveSetLog(setLog.copy(clusterMiniSetIndex = 1, type = SetType.REST_PAUSE, goalReps = goalReps))
        }
        
        // Create 2 and 3 with increasing timestamps to maintain order
        var currentTimestamp = setLog.timestamp
        val newSets = mutableListOf<SetLog>()
        for (i in 2..3) {
            // Double check existing indices in case state updated while running
            if (setsForLog.any { it.clusterMiniSetIndex == i }) continue
            
            currentTimestamp = currentTimestamp.plusMillis(1)
            val newSet = SetLog(
                id = UUID.randomUUID().toString(),
                workoutLogId = setLog.workoutLogId,
                weight = setLog.weight,
                reps = setLog.reps,
                type = SetType.REST_PAUSE,
                clusterMiniSetIndex = i,
                goalReps = goalReps,
                timestamp = currentTimestamp
            )
            newSets.add(newSet)
            repository.saveSetLog(newSet)
        }

        if (newSets.isNotEmpty()) {
            _uiState.update { state ->
                val updatedLogs = state.logs.map { (log, sets) ->
                    if (log.id == setLog.workoutLogId) {
                        log to (sets + newSets).distinctBy { it.id }.sortedWith(compareBy({ it.timestamp }, { it.id }))
                    } else {
                        log to sets
                    }
                }
                state.copy(logs = updatedLogs)
            }
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
            checkRoutineModificationsAndPostCheckIn()
        }
    }

    fun dismissUncompletedSetsDialog(discard: Boolean) {
        _uiState.update { it.copy(showUncompletedSetsDialog = false) }
        if (discard) {
            checkRoutineModificationsAndPostCheckIn(isDiscardingUncompleted = true)
        }
    }

    private fun checkRoutineModificationsAndPostCheckIn(isDiscardingUncompleted: Boolean = false) {
        val activeRoutine = _uiState.value.activeRoutine
        val currentLogs = _uiState.value.logs
        
        if (activeRoutine != null && checkIfRoutineModified(activeRoutine, currentLogs, isDiscardingUncompleted)) {
            _uiState.update { it.copy(showSaveRoutineChangesDialog = true) }
        } else {
            _uiState.update { it.copy(showPostWorkoutCheckIn = true) }
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
        _uiState.update { it.copy(showSaveRoutineChangesDialog = false, showPostWorkoutCheckIn = true) }
    }

    fun submitPostWorkoutCheckIn(rpe: Int, jointHealth: Int) {
        val session = _uiState.value.session ?: return
        viewModelScope.launch {
            repository.saveSession(session.copy(
                sessionRpe = rpe,
                jointHealth = jointHealth
            ))
            _uiState.update { it.copy(showPostWorkoutCheckIn = false) }
            performFinalFinish()
        }
    }

    fun cancelPostWorkoutCheckIn() {
        _uiState.update { it.copy(showPostWorkoutCheckIn = false) }
        performFinalFinish()
    }

    fun handleRoutineSelection(routine: WorkoutRoutine, isDeload: Boolean = false) {
        if (routine.protocol == WorkoutProtocol.STARTING_STRENGTH) {
            startStartingStrengthSession()
            return
        }
        if (routine.protocol == WorkoutProtocol.HST) {
            startHstSession()
            return
        }
        if (routine.protocol == WorkoutProtocol.FIVE_THREE_ONE) {
            startFiveThreeOneSession()
            return
        }
        if (routine.protocol == WorkoutProtocol.DUP) {
            startDupSession(routine)
            return
        }
        if (routine.protocol == WorkoutProtocol.WESTSIDE) {
            startWestsideSession()
            return
        }
        val state = _uiState.value
        val profile = state.userProfile
        
        // 1. Check for Injuries first
        val profileInjuries = profile?.injuries ?: emptyList()
        val injured = routine.exercises.filter { re -> 
            re.exercise.dangerousFor.any { it in profileInjuries }
        }.map { re ->
            val safeAlternatives = state.availableExercises.filter { alt ->
                alt.movementType == re.exercise.movementType && 
                alt.id != re.exercise.id &&
                alt.dangerousFor.none { it in profileInjuries }
            }.take(2)
            re.exercise to safeAlternatives
        }

        if (injured.isNotEmpty()) {
            _uiState.update { it.copy(
                showInjuryWarningDialog = true, 
                injuredExercises = injured,
                pendingInjuryRoutine = routine 
            ) }
            return
        }

        // 2. Check for sequence override
        if (profile?.sequencerEnabled == true && state.nextSequencedRoutine != null && routine.id != state.nextSequencedRoutine.id) {
            _uiState.update { it.copy(showSequenceOverrideDialog = true, pendingSequenceRoutine = routine) }
        } else {
            startRoutine(routine, isDeload)
        }
    }

    fun confirmInjuryAutoSwap() {
        val state = _uiState.value
        val routine = state.pendingInjuryRoutine ?: return
        
        val updatedExercises = routine.exercises.map { re ->
            val injuredMatch = state.injuredExercises.find { it.first.id == re.exercise.id }
            if (injuredMatch != null && injuredMatch.second.isNotEmpty()) {
                re.copy(exercise = injuredMatch.second.first())
            } else {
                re
            }
        }
        
        val safeRoutine = routine.copy(exercises = updatedExercises)
        _uiState.update { it.copy(showInjuryWarningDialog = false, pendingInjuryRoutine = null, injuredExercises = emptyList()) }
        
        // Now proceed to sequence check with the safe routine
        handleRoutineSelection(safeRoutine)
    }

    fun ignoreInjuryWarning() {
        val routine = _uiState.value.pendingInjuryRoutine ?: return
        _uiState.update { it.copy(showInjuryWarningDialog = false, pendingInjuryRoutine = null, injuredExercises = emptyList()) }
        
        // Proceed with original routine
        val state = _uiState.value
        val profile = state.userProfile
        if (profile?.sequencerEnabled == true && state.nextSequencedRoutine != null && routine.id != state.nextSequencedRoutine.id) {
            _uiState.update { it.copy(showSequenceOverrideDialog = true, pendingSequenceRoutine = routine) }
        } else {
            startRoutine(routine)
        }
    }

    fun confirmSequenceOverride(updateBaseline: Boolean) {
        val routine = _uiState.value.pendingSequenceRoutine ?: return
        val profile = _uiState.value.userProfile ?: return

        viewModelScope.launch {
            if (updateBaseline) {
                // Find index of this routine in the sequence
                val sequence = if (profile.activeProtocol != null) {
                    _uiState.value.routines.filter { it.protocol == profile.activeProtocol }
                } else {
                    profile.customSequenceIds.mapNotNull { id -> _uiState.value.routines.find { it.id == id } }
                }
                
                val newIndex = sequence.indexOfFirst { it.id == routine.id }.takeIf { it != -1 } ?: profile.rotationIndex
                val updatedProfile = profile.copy(rotationIndex = newIndex)
                repository.saveUserProfile(updatedProfile)
                _uiState.update { it.copy(userProfile = updatedProfile) }
                updateSequencerState()
            }
            
            _uiState.update { it.copy(showSequenceOverrideDialog = false, pendingSequenceRoutine = null) }
            startRoutine(routine)
        }
    }

    fun dismissSequenceOverride() {
        _uiState.update { it.copy(showSequenceOverrideDialog = false, pendingSequenceRoutine = null) }
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
                        sets = sets.map { setLog ->
                            RoutineSet(
                                type = setLog.type,
                                weight = setLog.weight,
                                reps = if (setLog.isCompleted) setLog.reps else {
                                    // If not completed, try to extract min from goalReps (e.g. "11-20" -> 11)
                                    setLog.goalReps?.split("-")?.firstOrNull()?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                                },
                                goalReps = setLog.goalReps
                            )
                        }
                    )
                } else null
            }
        
        val hadNonAugmentExercises = currentLogs.any { it.first.augmentId == null }
        if (hadNonAugmentExercises && updatedExercises.isEmpty()) return

        val updatedRoutine = activeRoutine.copy(
            exercises = updatedExercises,
            isAddedToLibrary = true // Ensure modified system routines persist in user library
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

            // Blast tracking logic
            val profile = _uiState.value.userProfile
            if (profile != null) {
                var updatedProfile = profile
                if (session.isDeload) {
                    // Reset blast on deload
                    updatedProfile = updatedProfile.copy(lastBlastStartDate = null)
                } else if (profile.lastBlastStartDate == null) {
                    // Start new blast on first high-intensity session
                    updatedProfile = updatedProfile.copy(lastBlastStartDate = java.time.Instant.now())
                }
                
                // Advance rotation if sequencer is active and this was the expected routine
                if (profile.sequencerEnabled) {
                    val wasSequenced = _uiState.value.activeRoutine?.id == _uiState.value.nextSequencedRoutine?.id
                    if (wasSequenced) {
                        val nextIndex = (profile.rotationIndex + 1)
                        updatedProfile = updatedProfile.copy(rotationIndex = nextIndex)
                        _uiState.update { it.copy(userProfile = updatedProfile) }
                    }
                }
                
                if (updatedProfile != profile) {
                    repository.saveUserProfile(updatedProfile)
                }

                // Advance Protocol Cycle
                if (session.cycleId != null) {
                    repository.getActiveCycle(profile.userId).first()?.let { cycle ->
                        repository.saveProtocolCycle(cycle.copy(currentDayIndex = cycle.currentDayIndex + 1))
                    }
                }
            }

            // Update progression states and accomplishments for all exercises
            currentLogs.forEach { (log, sets) ->
                if (session.protocol == WorkoutProtocol.CYBER_CRAPP) {
                    updateProgressionForExercise(log, sets)
                } else if (session.protocol == WorkoutProtocol.STARTING_STRENGTH) {
                    updateStartingStrengthProgression(log, sets, session)
                } else if (session.protocol == WorkoutProtocol.HST) {
                    updateHstProgression(log, sets, session)
                } else if (session.protocol == WorkoutProtocol.FIVE_THREE_ONE) {
                    updateFiveThreeOneProgression(log, sets, session)
                } else if (session.protocol == WorkoutProtocol.DUP) {
                    updateDupProgression(log, sets, session)
                } else if (session.protocol == WorkoutProtocol.WESTSIDE) {
                    updateWestsideProgression(log, sets, session)
                }
                updateAccomplishmentsForExercise(log.exerciseId, sets, session.date)
            }

            _uiState.update { it.copy(
                session = null, 
                workoutDurationSeconds = 0, 
                activeRoutine = null,
                logs = emptyList(), // Clear logs for next session
                previousLogs = emptyMap(),
                workoutPhase = RestPausePhase.NOT_ACTIVE
            ) }
            workoutDurationJob?.cancel()
            sessionJob?.cancel()
        }
    }

    fun dismissProtocolIntake() {
        _uiState.update { it.copy(protocolIntakeNeeded = null) }
    }

    fun dismissSdWindow() {
        _uiState.update { it.copy(isStrategicDeconditioningActive = false) }
    }

    fun getBlastWeek(): Int? {
        val startDate = _uiState.value.userProfile?.lastBlastStartDate ?: return null
        val now = java.time.Instant.now()
        val days = java.time.Duration.between(startDate, now).toDays()
        return (days / 7).toInt() + 1
    }

    private fun updateWestsideProgression(log: WorkoutLog, sets: List<SetLog>, session: WorkoutSession) {
        val engine = westsideEngine
        val exerciseId = log.exerciseId
        val uiMode = _uiState.value.currentUiMode
        
        if (uiMode != ProtocolUiMode.MAX_EFFORT) return

        val workSets = sets.filter { it.type == SetType.MAX_EFFORT }
        if (workSets.isEmpty()) return

        val bestMEWeight = workSets.filter { it.isCompleted }.maxOfOrNull { it.weight } ?: 0f
        
        viewModelScope.launch {
            val exercise = repository.getExerciseById(exerciseId) ?: return@launch
            val familyId = exercise.familyId
            val activeCycle = repository.getActiveCycle(_uiState.value.userProfile?.userId ?: "").first()
            
            if (activeCycle != null && activeCycle.protocol == WorkoutProtocol.WESTSIDE) {
                val currentMax = repository.getExerciseMax(familyId).first()
                if (currentMax != null && bestMEWeight > currentMax.oneRepMax) {
                    repository.upsertExerciseMax(currentMax.copy(oneRepMax = bestMEWeight, trainingMax = bestMEWeight, testedAt = java.time.Instant.now()))
                }
                
                // Rotate variant in config for next time
                val newConfig = engine.rotateVariant(activeCycle.configJson, session.protocolDayType ?: ProtocolDayType.WS_ME_LOWER)
                repository.saveProtocolCycle(activeCycle.copy(configJson = newConfig))
            }
        }
    }

    private fun updateDupProgression(log: WorkoutLog, sets: List<SetLog>, session: WorkoutSession) {
        val engine = dupEngine
        val workSets = sets.filter { it.type != SetType.WARMUP }
        if (workSets.isEmpty()) return

        val succeeded = engine.sessionSucceeded(workSets)
        val exerciseId = log.exerciseId
        
        viewModelScope.launch {
            val exercise = repository.getExerciseById(exerciseId) ?: return@launch
            val familyId = exercise.familyId
            val activeCycle = repository.getActiveCycle(_uiState.value.userProfile?.userId ?: "").first()
            
            // Progression only on STRENGTH day
            if (activeCycle != null && activeCycle.protocol == WorkoutProtocol.DUP && 
                session.protocolDayType == ProtocolDayType.DUP_STRENGTH) {
                
                if (succeeded) {
                    val currentMax = repository.getExerciseMax(familyId).first()
                    if (currentMax != null) {
                        val currentWeight = workSets.first().prescribedWeight ?: workSets.first().weight
                        val nextWeight = engine.nextLoad(currentWeight, true, exercise)
                        repository.upsertExerciseMax(currentMax.copy(trainingMax = nextWeight, testedAt = java.time.Instant.now()))
                    }
                }
            }
        }
    }

    private fun updateFiveThreeOneProgression(log: WorkoutLog, sets: List<SetLog>, session: WorkoutSession) {
        val engine = fiveThreeOneEngine
        val amrapSet = sets.find { it.isAmrap }
        if (amrapSet == null || !amrapSet.isCompleted) return

        val exerciseId = log.exerciseId
        val extras = amrapSet.reps - (amrapSet.prescribedReps ?: 0)
        
        viewModelScope.launch {
            val exercise = repository.getExerciseById(exerciseId) ?: return@launch
            val activeCycle = repository.getActiveCycle(_uiState.value.userProfile?.userId ?: "").first()
            
            // Progression only after Week 3 (FTV_W3)
            if (activeCycle != null && activeCycle.protocol == WorkoutProtocol.FIVE_THREE_ONE && 
                session.protocolDayType == ProtocolDayType.FTV_W3) {
                
                if (extras >= 5) {
                    val currentMax = repository.getExerciseMax(exercise.familyId).first()
                    if (currentMax != null) {
                        val increment = when (exercise.movementType) {
                            MovementType.QUAD_DOMINANT, MovementType.DEADLIFT -> 10f
                            else -> 5f
                        }
                        val newTM = (currentMax.trainingMax ?: (currentMax.oneRepMax * 0.9f)) + increment
                        repository.upsertExerciseMax(currentMax.copy(trainingMax = newTM, testedAt = java.time.Instant.now()))
                    }
                }
            }
        }
    }

    private fun updateHstProgression(log: WorkoutLog, sets: List<SetLog>, session: WorkoutSession) {
        val engine = hstEngine
        val workSets = sets.filter { it.type == SetType.NORMAL }
        if (workSets.isEmpty()) return

        val succeeded = engine.sessionSucceeded(workSets)
        val exerciseId = log.exerciseId
        
        viewModelScope.launch {
            val exercise = repository.getExerciseById(exerciseId) ?: return@launch
            val familyId = exercise.familyId
            val activeCycle = repository.getActiveCycle(_uiState.value.userProfile?.userId ?: "").first()
            
            if (activeCycle != null && activeCycle.protocol == WorkoutProtocol.HST) {
                val newConfig = engine.updateConfigWithMiss(activeCycle.configJson, familyId, !succeeded)
                
                // Advance day index globally happens in performFinalFinish, but let's check SD end of cycle
                // currentDayIndex at this point is before the increment in performFinalFinish.
                // If it's session 17 (last of 5s), trigger SD.
                if (activeCycle.currentDayIndex >= 17 && session.protocolDayType == ProtocolDayType.HST_5) {
                    val sdConfig = engine.startSd(newConfig)
                    val updatedCycle = activeCycle.copy(configJson = sdConfig)
                    repository.saveProtocolCycle(updatedCycle)
                    
                    _uiState.value.userProfile?.let { syncWorkoutReminders(it) }
                } else {
                    repository.saveProtocolCycle(activeCycle.copy(configJson = newConfig))
                }
            }
        }
    }

    private fun updateStartingStrengthProgression(log: WorkoutLog, sets: List<SetLog>, session: WorkoutSession) {
        val engine = startingStrengthEngine
        val workSets = sets.filter { it.type == SetType.NORMAL }
        if (workSets.isEmpty()) return

        val succeeded = engine.sessionSucceeded(workSets)
        val exerciseId = log.exerciseId
        
        viewModelScope.launch {
            val exercise = repository.getExerciseById(exerciseId) ?: return@launch
            val familyId = exercise.familyId
            val activeCycle = repository.getActiveCycle(_uiState.value.userProfile?.userId ?: "").first()
            
            if (activeCycle != null && activeCycle.protocol == WorkoutProtocol.STARTING_STRENGTH) {
                val config = activeCycle.configJson
                val strikes = engine.parseStrikes(config).toMutableMap()
                val currentStrikes = strikes[familyId] ?: 0
                
                val currentWeight = workSets.first().prescribedWeight ?: workSets.first().weight

                if (!succeeded) {
                    val newStrikes = currentStrikes + 1
                    if (newStrikes >= 3) {
                        // Stall: 10% deload
                        val newWeight = (currentWeight * 0.9f).let { (Math.round(it / 2.5f) * 2.5f).toFloat() }
                        repository.upsertExerciseMax(
                            ExerciseMax(familyId, java.time.Instant.now(), 0f, trainingMax = newWeight, source = MaxSource.ESTIMATED)
                        )
                        val newConfig = engine.updateStrikes(config, familyId, false)
                        repository.saveProtocolCycle(activeCycle.copy(configJson = newConfig))
                    } else {
                        val newConfig = engine.updateStrikes(config, familyId, true)
                        repository.saveProtocolCycle(activeCycle.copy(configJson = newConfig))
                    }
                } else {
                    // Success: Increment
                    val nextWeight = engine.nextLoad(currentWeight, true, exercise)
                    repository.upsertExerciseMax(
                        ExerciseMax(familyId, java.time.Instant.now(), 0f, trainingMax = nextWeight, source = MaxSource.ESTIMATED)
                    )
                    val newConfig = engine.updateStrikes(config, familyId, false)
                    repository.saveProtocolCycle(activeCycle.copy(configJson = newConfig))
                }
            }
        }
    }

    private fun handleRestPauseLogic(set: SetLog) {
        if (!set.isCompleted) return
        
        val index = set.clusterMiniSetIndex ?: return
        val state = _uiState.value
        val isCyberCrapp = state.session?.protocol == WorkoutProtocol.CYBER_CRAPP
        val currentPhase = state.workoutPhase
        
        // Prevent re-triggering finisher flow if already active, completed, or if we're past it
        if (index == 3 && isCyberCrapp) {
            if (currentPhase == RestPausePhase.FINISHER || currentPhase == RestPausePhase.LOADED_STRETCH) return
            
            val logPair = state.logs.find { it.first.id == set.workoutLogId }
            val hasFinisher = logPair?.second?.any { it.type == SetType.PARTIAL || it.type == SetType.STRETCH } ?: false
            if (hasFinisher) return
        }
        
        val nextPhase = when (index) {
            1 -> RestPausePhase.MINI_SET_2
            2 -> RestPausePhase.MINI_SET_3
            3 -> if (isCyberCrapp) RestPausePhase.FINISHER else RestPausePhase.NOT_ACTIVE
            else -> return
        }

        // Only progress the phase or stay if it's a rest-pause rest timer tick
        _uiState.update { it.copy(workoutPhase = nextPhase) }

        when (nextPhase) {
            RestPausePhase.MINI_SET_2, RestPausePhase.MINI_SET_3 -> {
                triggerRestTimer(set, 15)
            }
            RestPausePhase.FINISHER -> {
                val log = _uiState.value.logs.find { it.second.any { s -> s.id == set.id } }?.first
                _uiState.update { it.copy(
                    showCyberFinisher = true, 
                    isResting = false,
                    activeCyberCrappLogId = log?.id
                ) }
                WorkoutTimerService.stop(context)
            }
            else -> {}
        }
    }

    private fun handleGiantSetLogic(set: SetLog) {
        val state = _uiState.value
        val log = state.logs.find { it.second.any { s -> s.id == set.id } }?.first ?: return
        
        // Giant Sets (like Gorilla Arms) auto-start the rest timer after the LAST exercise in the circuit
        val augmentId = log.augmentId ?: return
        val logsInGroup = state.logs.filter { it.first.augmentId == augmentId }
        val maxOrder = logsInGroup.maxOfOrNull { it.first.order } ?: return
        
        if (log.order == maxOrder) {
            // "20 deep breaths" is calibrated to 60 seconds (1 minute)
            triggerRestTimer(set, 60)
        }
    }

    private fun calculateStretchDuration(): Int {
        val state = _uiState.value
        if (!state.useSomatotypeInfluence) return 45
        
        return when (state.userProfile?.somatotype) {
            Somatotype.ENDOMORPH -> 30
            Somatotype.ECTOMORPH -> 60
            else -> 45
        }
    }

    private fun startStretchTimer() {
        stretchTimerJob?.cancel()
        stretchTimerJob = viewModelScope.launch {
            while (_uiState.value.stretchTimeRemaining > 0) {
                kotlinx.coroutines.delay(1000)
                val remaining = _uiState.value.stretchTimeRemaining - 1
                
                // Breathing Vibration (Pulse on BREATHE IN)
                val isBreathingIn = (remaining % 6) >= 3
                if (isBreathingIn && (remaining % 6) == 5) { // At the start of the 3s inhale
                    if (_uiState.value.userProfile?.breathingVibrationEnabled == true) {
                        hapticService.breathingPulse()
                    }
                }
                
                _uiState.update { it.copy(stretchTimeRemaining = remaining) }
            }
            hapticService.syncSuccess()
            
            // Save Loaded Stretch log
            val logId = _uiState.value.activeCyberCrappLogId
            if (logId != null) {
                val stretchSet = SetLog(
                    id = UUID.randomUUID().toString(),
                    workoutLogId = logId,
                    weight = 0f,
                    reps = calculateStretchDuration(),
                    type = SetType.STRETCH,
                    isLoadedStretch = true,
                    stretchDurationSeconds = calculateStretchDuration(),
                    isCompleted = true
                )
                
                _uiState.update { state ->
                    val newLogs = state.logs.map { (log, sets) ->
                        if (log.id == logId) {
                            log to (sets + stretchSet).sortedWith(compareBy({ it.timestamp }, { it.id }))
                        } else {
                            log to sets
                        }
                    }
                    state.copy(logs = newLogs)
                }
                
                repository.saveSetLog(stretchSet)
            }
            
            _uiState.update { it.copy(
                showLoadedStretch = false, 
                workoutPhase = RestPausePhase.NOT_ACTIVE,
                activeCyberCrappLogId = null
            ) }
        }
    }

    fun startStretch(partialReps: Int) {
        val stretchDuration = calculateStretchDuration()
        val logId = _uiState.value.activeCyberCrappLogId
        
        viewModelScope.launch {
            if (logId != null) {
                val partialSet = SetLog(
                    id = UUID.randomUUID().toString(),
                    workoutLogId = logId,
                    weight = 0f,
                    reps = partialReps,
                    type = SetType.PARTIAL,
                    isLengthenedPartial = true,
                    isCompleted = true
                )
                
                _uiState.update { state ->
                    val newLogs = state.logs.map { (log, sets) ->
                        if (log.id == logId) {
                            log to (sets + partialSet).sortedWith(compareBy({ it.timestamp }, { it.id }))
                        } else {
                            log to sets
                        }
                    }
                    state.copy(logs = newLogs)
                }
                
                repository.saveSetLog(partialSet)
            }
            
            _uiState.update { it.copy(
                showLoadedStretch = true, 
                showCyberFinisher = false, 
                stretchTimeRemaining = stretchDuration,
                workoutPhase = RestPausePhase.LOADED_STRETCH
            ) }
            startStretchTimer()
        }
    }

    fun forceRotateStagnant(exerciseId: String) {
        startSubstitution(exerciseId)
    }

    fun startSubstitution(exerciseId: String) {
        val exercise = _uiState.value.availableExercises.find { it.id == exerciseId } ?: return
        val allOther = _uiState.value.availableExercises.filter { it.id != exerciseId }

        // Sort rules:
        // 1. Same familyId, different id
        // 2. Same movementType
        // 3. Everything else
        val recommendations = allOther.sortedWith(
            compareByDescending<Exercise> { it.familyId == exercise.familyId }
                .thenByDescending { it.movementType == exercise.movementType && it.movementType != MovementType.UNDEFINED }
                .thenBy { it.name }
        ).take(3)

        _uiState.update { it.copy(
            showSubstitutionDialog = true,
            exerciseToSubstitute = exerciseId,
            recommendedSubstitutes = recommendations
        ) }
    }

    fun substituteExercise(oldExerciseId: String, newExercise: Exercise) {
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            val logs = _uiState.value.logs
            val logPair = logs.find { it.first.exerciseId == oldExerciseId } ?: return@launch
            val logToReplace = logPair.first
            val oldSets = logPair.second

            // Update the log in database
            val updatedLog = logToReplace.copy(
                exerciseId = newExercise.id,
                exerciseName = newExercise.name
            )
            
            // Immediate UI feedback to avoid race conditions when finishing
            _uiState.update { state ->
                val newLogs = state.logs.map { (log, sets) ->
                    if (log.id == updatedLog.id) {
                        updatedLog to emptyList<SetLog>() // Sets will be updated below
                    } else {
                        log to sets
                    }
                }
                state.copy(logs = newLogs)
            }

            repository.saveWorkoutLog(updatedLog)
            
            // Delete existing sets for this log as it's a new exercise
            oldSets.forEach { set ->
                repository.deleteSetLog(set.id)
            }
            
            val isCC = session.protocol == WorkoutProtocol.CYBER_CRAPP
            
            val progressionState = _uiState.value.progressionStates[newExercise.id]
            val afterWeightJump = progressionState != null && progressionState.bestClusterReps == 0 && progressionState.currentWeight > 0

            var globalTimestamp = java.time.Instant.now()
            val newSets = mutableListOf<SetLog>()

            if (isCC) {
                // For CyberCrapp, rebuild the standard structure: 2 Warmups + 1 Rest-Pause Cluster (3 mini-sets)
                // or 3 Normal Sets if Deload
                
                val warmupGoal = CyberCrappRules.resolve(session.protocol, newExercise, SetType.WARMUP, false, _uiState.value.repTargets).label
                val workingGoal = if (session.isDeload) {
                    "DELOAD (RIR 3-4)"
                } else {
                    CyberCrappRules.resolve(session.protocol, newExercise, SetType.REST_PAUSE, afterWeightJump, _uiState.value.repTargets).label
                }

                // Warmup 1
                globalTimestamp = globalTimestamp.plusMillis(1)
                SetLog(
                    id = UUID.randomUUID().toString(),
                    workoutLogId = updatedLog.id,
                    weight = 0f,
                    reps = 0,
                    type = SetType.WARMUP,
                    goalReps = warmupGoal,
                    timestamp = globalTimestamp
                ).also { newSets.add(it) }
                
                // Warmup 2
                globalTimestamp = globalTimestamp.plusMillis(1)
                SetLog(
                    id = UUID.randomUUID().toString(),
                    workoutLogId = updatedLog.id,
                    weight = 0f,
                    reps = 0,
                    type = SetType.WARMUP,
                    goalReps = warmupGoal,
                    timestamp = globalTimestamp
                ).also { newSets.add(it) }
                
                // Cluster sets (3 mini-sets) or deload sets
                val typeToSave = if (session.isDeload) SetType.NORMAL else SetType.REST_PAUSE
                for (i in 1..3) {
                    globalTimestamp = globalTimestamp.plusMillis(1)
                    SetLog(
                        id = UUID.randomUUID().toString(),
                        workoutLogId = updatedLog.id,
                        weight = 0f,
                        reps = 0,
                        type = typeToSave,
                        goalReps = workingGoal,
                        clusterMiniSetIndex = if (session.isDeload) null else i,
                        timestamp = globalTimestamp,
                        rir = if (session.isDeload) 4 else null
                    ).also { newSets.add(it) }
                }
            } else {
                // If old sets existed, preserve the count/types, otherwise add 3 standard sets
                val baseSets = if (oldSets.isNotEmpty()) oldSets else listOf(
                    SetLog(id = UUID.randomUUID().toString(), workoutLogId = updatedLog.id, weight = 0f, reps = 0, type = SetType.NORMAL),
                    SetLog(id = UUID.randomUUID().toString(), workoutLogId = updatedLog.id, weight = 0f, reps = 0, type = SetType.NORMAL),
                    SetLog(id = UUID.randomUUID().toString(), workoutLogId = updatedLog.id, weight = 0f, reps = 0, type = SetType.NORMAL)
                )
                baseSets.forEach { oldSet ->
                    val setGoal = CyberCrappRules.resolve(session.protocol, newExercise, oldSet.type, afterWeightJump, _uiState.value.repTargets).label
                    globalTimestamp = globalTimestamp.plusMillis(1)
                    SetLog(
                        id = UUID.randomUUID().toString(),
                        workoutLogId = updatedLog.id,
                        weight = 0f,
                        reps = 0,
                        type = oldSet.type,
                        goalReps = setGoal,
                        timestamp = globalTimestamp
                    ).also { newSets.add(it) }
                }
            }

            newSets.forEach { repository.saveSetLog(it) }
            
            // Final UI update with all new sets to ensure consistency
            _uiState.update { state ->
                val finalizedLogs = state.logs.map { (log, sets) ->
                    if (log.id == updatedLog.id) {
                        updatedLog to newSets.sortedWith(compareBy({ it.timestamp }, { it.id }))
                    } else {
                        log to sets
                    }
                }
                state.copy(
                    logs = finalizedLogs,
                    showSubstitutionDialog = false,
                    exerciseToSubstitute = null,
                    recommendedSubstitutes = emptyList()
                )
            }

            loadPreviousData(newExercise.id)
        }
    }

    fun dismissSubstitution() {
        _uiState.update { it.copy(
            showSubstitutionDialog = false,
            exerciseToSubstitute = null,
            recommendedSubstitutes = emptyList()
        ) }
    }

    fun getHintForExercise(exerciseId: String): String? {
        val state = _uiState.value
        val score = state.recoveryScore ?: return null
        val profile = state.userProfile ?: return null
        if (!profile.coachingHintsEnabled) return null

        val exercise = state.availableExercises.find { it.id == exerciseId } ?: return null
        val isCyberCrapp = state.session?.protocol == WorkoutProtocol.CYBER_CRAPP
        
        return when {
            score.totalScore > 85 -> {
                if (isCyberCrapp && (exercise.movementType == MovementType.ISOLATION_UPPER || exercise.movementType == MovementType.CALVES)) {
                    "⚡ Optimal recovery: Add a Cyber Finisher (Partials) to this movement?"
                } else if (!isCyberCrapp) {
                    "⚡ Uplink strong: Feeling strong? Add an extra set to this exercise."
                } else {
                    "⚡ Uplink strong: High intensity effort recommended."
                }
            }
            score.totalScore < 40 -> {
                if (isCyberCrapp) {
                    "⚠️ Fatigue detected: Suggest skipping the Loaded Stretch here."
                } else {
                    "⚠️ Fatigue detected: Recovery compromised. Consider dropping the final set."
                }
            }
            score.totalScore > 70 && exercise.id == "jerry_curl" -> {
                "⚡ Neural Link stable: Focus on a 2s pause at the bottom stretch."
            }
            else -> null
        }
    }

    private suspend fun updateProgressionForExercise(log: WorkoutLog, sets: List<SetLog>) {
        val completedSets = sets.filter { it.isCompleted }
        if (completedSets.isEmpty()) return

        val currentState = repository.getProgressionState(log.exerciseId).first() ?: ProgressionState(log.exerciseId)
        val profile = _uiState.value.userProfile
        
        // For CyberCrapp, we mainly care about the cluster (REST_PAUSE)
        val currentClusterTotal = completedSets.filter { it.type == SetType.REST_PAUSE }.sumOf { it.reps }
        val currentWeight = completedSets.firstOrNull { it.type == SetType.REST_PAUSE }?.weight ?: 0f
        
        if (currentClusterTotal == 0) return 

        var misses = currentState.consecutiveMisses
        var bestReps = currentState.bestClusterReps
        var bestWeight = currentState.weightAtBest
        var nextWeight = currentWeight
        
        val exercise = _uiState.value.availableExercises.find { it.id == log.exerciseId }
        val repRange = exercise?.movementType?.let { CyberCrappRules.getRepRange(it) }

        if (currentWeight > currentState.weightAtBest) {
            // New weight PR
            bestReps = currentClusterTotal
            bestWeight = currentWeight
            misses = 0
        } else if (currentWeight == currentState.weightAtBest) {
            if (currentClusterTotal > currentState.bestClusterReps) {
                // Improved reps at same weight
                bestReps = currentClusterTotal
                misses = 0
            } else {
                // Failed to beat best
                misses++
            }
        }

        // Auto-increment logic
        if (profile?.autoWeightIncrement == true && repRange != null && bestReps >= repRange.max) {
            val isCompound = exercise.movementType == MovementType.COMPOUND_UPPER || 
                             exercise.movementType == MovementType.QUAD_DOMINANT || 
                             exercise.movementType == MovementType.POSTERIOR_CHAIN ||
                             exercise.movementType == MovementType.DEADLIFT
            
            val increment = if (isCompound) profile.weightIncrementCompound else profile.weightIncrementIsolation
            nextWeight += increment
            // Reset best reps for the new weight
            bestReps = 0 
            bestWeight = nextWeight
        }

        repository.saveProgressionState(currentState.copy(
            bestClusterReps = bestReps,
            weightAtBest = bestWeight,
            consecutiveMisses = misses,
            currentWeight = nextWeight
        ))

        if (misses >= 2) {
            _uiState.update { it.copy(stagnantExerciseId = log.exerciseId) }
        }
    }

    private suspend fun updateAccomplishmentsForExercise(exerciseId: String, sets: List<SetLog>, sessionDate: Instant) {
        val currentAcc = repository.getAccomplishments(exerciseId).first()
        val updatedAcc = com.neon.ascent.core.domain.workout.rules.AccomplishmentEngine.evaluateAccomplishments(
            exerciseId = exerciseId,
            currentAccomplishments = currentAcc,
            completedSets = sets,
            sessionDate = sessionDate
        )
        if (updatedAcc != currentAcc) {
            repository.saveAccomplishments(updatedAcc)
        }
    }
}
