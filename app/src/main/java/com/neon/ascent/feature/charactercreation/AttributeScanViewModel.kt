package com.neon.ascent.feature.charactercreation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.repository.CharacterRepository
import com.neon.ascent.model.UserCharacter
import com.neon.ascent.util.AttributeCalculator
import com.neon.ascent.util.CalculatedScores
import com.neon.ascent.util.RawAttributeInputs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AttributeScanViewModel @Inject constructor(
    private val characterRepository: CharacterRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep = _currentStep.asStateFlow()

    private val _inputs = MutableStateFlow(RawAttributeInputs())
    val inputs = _inputs.asStateFlow()

    private val _scanResult = MutableStateFlow<CalculatedScores?>(null)
    val scanResult = _scanResult.asStateFlow()

    val userCharacter: StateFlow<UserCharacter?> = characterRepository.getUserCharacter()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun nextStep() {
        if (_currentStep.value < 7) {
            _currentStep.value += 1
        }
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value -= 1
        }
    }

    fun updateInputs(update: (RawAttributeInputs) -> RawAttributeInputs) {
        _inputs.value = update(_inputs.value)
    }

    fun calculateResults() {
        val user = userCharacter.value ?: return
        val results = AttributeCalculator.calculateAll(user, _inputs.value)
        _scanResult.value = results
    }

    fun saveResults() {
        viewModelScope.launch {
            val results = _scanResult.value ?: return@launch
            val user = userCharacter.value ?: return@launch
            val updatedUser = user.copy(
                strength = results.strength,
                endurance = results.endurance,
                agility = results.agility,
                perception = results.perception,
                intelligence = results.intelligence,
                charisma = results.charisma,
                luck = results.luck
            )
            characterRepository.saveCharacter(updatedUser)
        }
    }
}
