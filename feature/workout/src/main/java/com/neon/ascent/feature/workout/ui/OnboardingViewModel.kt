package com.neon.ascent.feature.workout.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.repository.WorkoutRepository
import com.neon.ascent.core.domain.workout.models.*
import com.neon.ascent.core.domain.character.repository.CharacterRepository
import com.neon.ascent.core.domain.character.models.UserCharacter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
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
    val recommendation: WorkoutRoutine? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val characterRepository: CharacterRepository
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
                    val weightKg = if (character.units == "METRIC") character.weight.toFloat() else character.weight.toFloat() * 0.453592f
                    val heightCm = character.heightCm?.toFloat() ?: 175f
                    
                    val somatotype = when {
                        character.somatotype < 0.33f -> Somatotype.ECTOMORPH
                        character.somatotype < 0.66f -> Somatotype.MESOMORPH
                        else -> Somatotype.ENDOMORPH
                    }

                    _uiState.update { state ->
                        state.copy(
                            profile = state.profile.copy(
                                age = age,
                                weightKg = weightKg,
                                heightCm = heightCm,
                                gender = if (character.sex == "MALE") Gender.MALE else Gender.FEMALE,
                                somatotype = somatotype,
                                unitSystem = if (character.units == "METRIC") UnitSystem.METRIC else UnitSystem.IMPERIAL
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
            val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            val birthDate = LocalDate.parse(dob, formatter)
            Period.between(birthDate, LocalDate.now()).years
        } catch (e: Exception) {
            25
        }
    }

    fun nextStep() {
        val current = _uiState.value.currentStep
        if (current < 5) {
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

    fun updateSchedule(days: List<Int>, minutes: Int) {
        _uiState.update { it.copy(profile = it.profile.copy(preferredDays = days, timePerSessionMinutes = minutes)) }
    }

    fun updateUnitSystem(system: UnitSystem) {
        _uiState.update { it.copy(profile = it.profile.copy(unitSystem = system)) }
    }

    private fun generateRecommendation() {
        val state = _uiState.value
        val level = state.profile.experienceLevel
        
        viewModelScope.launch {
            workoutRepository.getAllRoutines().collect { routines ->
                val recommended = when (level) {
                    ExperienceLevel.NOVICE -> routines.find { it.id == "routine_linear_fullbody" } ?: routines.firstOrNull()
                    ExperienceLevel.ADVANCED -> routines.find { it.id == "routine_cybercrapp_a" }
                    else -> routines.find { it.id == "routine_cybercrapp_a" } // Default to CC A for Intermediates too
                }
                _uiState.update { it.copy(recommendation = recommended) }
            }
        }
    }

    private fun completeOnboarding() {
        viewModelScope.launch {
            workoutRepository.saveUserProfile(_uiState.value.profile)
            _uiState.update { it.copy(isComplete = true) }
        }
    }
}
