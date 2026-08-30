package com.neon.ascent.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.core.domain.workout.models.*
import com.neon.ascent.core.domain.character.repository.CharacterRepository
import com.neon.ascent.core.domain.character.models.UserCharacter
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.goals.models.AscensionTask
import com.neon.ascent.core.domain.goals.models.AscensionTaskType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 1,
    val profile: UserWorkoutProfile = UserWorkoutProfile(userId = "default_user"),
    val isSyncing: Boolean = false,
    val hasScanData: Boolean = false,
    val scanStrength: Int? = null,
    val scanEndurance: Int? = null,
    val scanAgility: Int? = null,
    val isComplete: Boolean = false,
    val recommendation: WorkoutRoutine? = null,
    val availableRoutines: List<WorkoutRoutine> = emptyList(),
    val isManualSelection: Boolean = false,
    val showAlternateProtocols: Boolean = false,
    val showReminderDialog: Boolean = false,
    val applyTimeToAll: Boolean = true
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val characterRepository: CharacterRepository,
    private val ascensionRepository: AscensionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        syncInitialData()
    }

    private fun syncInitialData() {
        _uiState.update { it.copy(isSyncing = true) }
        viewModelScope.launch {
            characterRepository.getUserCharacter().collect { character ->
                if (character != null) {
                    val age = calculateAge(character.dob)
                    val isMetric = character.units.equals("metric", ignoreCase = true)
                    val weightKg = character.weight.toFloatOrNull()?.let {
                        if (isMetric) it else it * 0.453592f
                    } ?: 75f
                    val heightCm = character.heightCm?.toFloatOrNull() ?: 175f
                    
                    val somatotype = when {
                        character.somatotype < 3.3f -> Somatotype.ECTOMORPH
                        character.somatotype < 6.6f -> Somatotype.MESOMORPH
                        else -> Somatotype.ENDOMORPH
                    }

                    _uiState.update { state ->
                        state.copy(
                            profile = state.profile.copy(
                                age = age,
                                weightKg = weightKg,
                                heightCm = heightCm,
                                gender = if (character.sex.equals("male", ignoreCase = true)) Gender.MALE else Gender.FEMALE,
                                somatotype = somatotype,
                                unitSystem = if (isMetric) UnitSystem.METRIC else UnitSystem.IMPERIAL
                            ),
                            hasScanData = character.strength != null,
                            scanStrength = character.strength,
                            scanEndurance = character.endurance,
                            scanAgility = character.agility,
                            isSyncing = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSyncing = false) }
                }
            }
        }
    }

    private fun calculateAge(dob: String): Int {
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
            val birthDate = LocalDate.parse(dob, formatter)
            Period.between(birthDate, LocalDate.now()).years
        } catch (e: Exception) {
            25
        }
    }

    fun nextStep() {
        val current = _uiState.value.currentStep
        if (current < 6) {
            _uiState.update { it.copy(currentStep = current + 1) }
            if (current + 1 == 5) {
                generateRecommendation()
            }
        } else {
            completeOnboarding()
        }
    }

    fun prevStep() {
        val current = _uiState.value.currentStep
        if (current > 1) {
            _uiState.update { it.copy(currentStep = current - 1) }
        }
    }

    fun updateExperience(level: ExperienceLevel) {
        _uiState.update { it.copy(profile = it.profile.copy(experienceLevel = level)) }
    }

    fun toggleInjury(bodyPart: String) {
        _uiState.update { state ->
            val current = state.profile.injuries
            val next = if (current.contains(bodyPart)) current - bodyPart else current + bodyPart
            state.copy(profile = state.profile.copy(injuries = next))
        }
    }

    fun updateSchedule(scheduledDays: List<ScheduledDay>) {
        _uiState.update { it.copy(profile = it.profile.copy(scheduledDays = scheduledDays)) }
    }

    fun toggleApplyTimeToAll() {
        _uiState.update { it.copy(applyTimeToAll = !it.applyTimeToAll) }
    }

    fun updateUnitSystem(system: UnitSystem) {
        _uiState.update { it.copy(profile = it.profile.copy(unitSystem = system)) }
    }

    fun showAlternateProtocols() {
        _uiState.update { it.copy(showAlternateProtocols = true) }
    }

    fun hideAlternateProtocols() {
        _uiState.update { it.copy(showAlternateProtocols = false) }
    }

    fun selectProtocol(routine: WorkoutRoutine) {
        val defaultDays = routine.protocol.defaultWeekdays.map { ScheduledDay(it, "09:00") }
        _uiState.update { state ->
            state.copy(
                recommendation = routine,
                isManualSelection = true,
                profile = state.profile.copy(
                    activeProtocol = routine.protocol,
                    scheduledDays = defaultDays
                ),
                showAlternateProtocols = false
            )
        }
    }

    fun showReminderDialog() {
        _uiState.update { it.copy(showReminderDialog = true) }
    }

    fun hideReminderDialog() {
        _uiState.update { it.copy(showReminderDialog = false) }
    }

    fun scheduleAttributeScanReminder(dateTime: LocalDateTime) {
        viewModelScope.launch {
            val task = AscensionTask(
                id = UUID.randomUUID().toString(),
                parentId = null,
                title = "PERFORM ATTRIBUTE SCAN",
                description = "Complete your biometric scan to calibrate workout protocols with maximum accuracy.",
                type = AscensionTaskType.ONE_TIME,
                timeWindows = listOf(dateTime.toLocalTime().toString()),
                reminderEnabled = true,
                xpValue = 50
            )
            ascensionRepository.insertTask(task)
            _uiState.update { it.copy(showReminderDialog = false) }
            // Move to next step or provide feedback
            nextStep()
        }
    }

    private fun generateRecommendation() {
        viewModelScope.launch {
            workoutRepository.getAllRoutines().collect { routinesFromRepo ->
                // Always use latest state for decision making
                val currentState = _uiState.value
                val level = currentState.profile.experienceLevel
                
                val routines = routinesFromRepo.toMutableList()
                
                // Ensure "Do my own thing" (GENERAL) is in the search pool
                if (routines.none { it.protocol == WorkoutProtocol.GENERAL }) {
                    routines.add(
                        WorkoutRoutine(
                            id = "routine_general",
                            name = "Free Training / Ops",
                            description = "No strict protocol. Log your own sets and reps as you go. Ideal for experienced operatives with custom programming.",
                            protocol = WorkoutProtocol.GENERAL
                        )
                    )
                }

                // Filter to distinct protocols for the selection list
                val distinctProtocols = routines.distinctBy { it.protocol.name }
                
                _uiState.update { it.copy(availableRoutines = distinctProtocols) }
                
                // If user already picked one (manually), don't override with auto-rec
                if (_uiState.value.isManualSelection) return@collect

                val recommended = when (level) {
                    ExperienceLevel.NOVICE -> {
                        routines.find { it.protocol == WorkoutProtocol.STARTING_STRENGTH }
                            ?: routines.find { it.protocol == WorkoutProtocol.GENERAL }
                            ?: return@collect // Wait for routines to load
                    }
                    ExperienceLevel.ADVANCED -> {
                        val str = currentState.scanStrength ?: 50
                        val end = currentState.scanEndurance ?: 50
                        val agi = currentState.scanAgility ?: 50

                        when {
                            agi > 70 -> routines.find { it.protocol == WorkoutProtocol.WESTSIDE }
                            str > end + 15 -> routines.find { it.protocol == WorkoutProtocol.FIVE_THREE_ONE }
                            else -> routines.find { it.protocol == WorkoutProtocol.CYBER_CRAPP }
                        } ?: routines.find { it.protocol == WorkoutProtocol.WESTSIDE }
                          ?: routines.find { it.protocol == WorkoutProtocol.CYBER_CRAPP }
                          ?: return@collect
                    }
                    else -> { // Intermediate or any
                        val str = currentState.scanStrength ?: 50
                        val end = currentState.scanEndurance ?: 50

                        when {
                            str > end + 15 -> routines.find { it.protocol == WorkoutProtocol.FIVE_THREE_ONE }
                            end > str + 15 -> routines.find { it.protocol == WorkoutProtocol.HST }
                            else -> routines.find { it.protocol == WorkoutProtocol.CYBER_CRAPP }
                        } ?: routines.find { it.protocol == WorkoutProtocol.CYBER_CRAPP }
                          ?: return@collect
                    }
                }

                val protocol = recommended.protocol
                val defaultDays = protocol.defaultWeekdays.map { ScheduledDay(it, "09:00") }

                _uiState.update { state ->
                    state.copy(
                        recommendation = recommended,
                        profile = state.profile.copy(
                            activeProtocol = protocol,
                            scheduledDays = defaultDays
                        )
                    )
                }
            }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            val state = _uiState.value
            val profile = state.profile
            
            // 1. Save User Profile
            workoutRepository.saveUserProfile(profile)
            
            // 2. Add Protocol Routines to Library
            if (profile.activeProtocol != null) {
                workoutRepository.getAllRoutines().first().filter { it.protocol == profile.activeProtocol }.forEach { routine ->
                    workoutRepository.saveRoutine(routine.copy(isAddedToLibrary = true))
                }
            }
            
            // 3. Schedule Recurring Tasks for training days
            profile.scheduledDays.forEach { scheduled ->
                val dayName = java.time.DayOfWeek.of(scheduled.dayOfWeek).name
                val task = AscensionTask(
                    id = UUID.randomUUID().toString(),
                    parentId = null,
                    title = "TRAINING SESSION: ${profile.activeProtocol?.displayName ?: "GENERAL"}",
                    description = "Sync with the next routine in your protocol rotation.",
                    type = AscensionTaskType.RECURRING,
                    recurrence = com.neon.ascent.core.domain.goals.models.RecurrenceV3(
                        type = com.neon.ascent.core.domain.goals.models.RecurrenceTypeV3.DAYS_OF_WEEK,
                        daysOfWeek = setOf(java.time.DayOfWeek.of(scheduled.dayOfWeek))
                    ),
                    timeWindows = listOf(scheduled.time),
                    reminderEnabled = true,
                    xpValue = 25,
                    tags = listOf("workout_session", "protocol_${profile.activeProtocol?.name}")
                )
                ascensionRepository.insertTask(task)
            }

            _uiState.update { it.copy(isComplete = true) }
        }
    }
}
