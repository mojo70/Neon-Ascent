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
            if (current + 1 == 6) {
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
        val state = _uiState.value
        val level = state.profile.experienceLevel
        
        viewModelScope.launch {
            workoutRepository.getAllRoutines().collect { routines ->
                val recommended = when (level) {
                    ExperienceLevel.NOVICE -> routines.find { it.id == "routine_linear_fullbody" } ?: routines.firstOrNull()
                    else -> routines.find { it.id == "routine_cybercrapp_a" }
                }
                _uiState.update { it.copy(
                    recommendation = recommended,
                    profile = it.profile.copy(activeProtocol = recommended?.protocol)
                ) }
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
                    title = "TRAINING SESSION: ${profile.activeProtocol ?: "GENERAL"}",
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
