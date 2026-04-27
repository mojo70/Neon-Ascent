package com.neon.ascent.feature.charactercreation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.repository.CharacterRepository
import com.neon.ascent.data.repository.TemplateRepository
import com.neon.ascent.model.UserCharacter
import com.neon.ascent.util.AttributeCalculator
import com.neon.ascent.util.CalculatedScores
import com.neon.ascent.util.RawAttributeInputs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class AttributeScanViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val templateRepository: TemplateRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow(0)
    val currentStep = _currentStep.asStateFlow()

    private val _inputs = MutableStateFlow(RawAttributeInputs())
    val inputs = _inputs.asStateFlow()

    private val _scanResult = MutableStateFlow<CalculatedScores?>(null)
    val scanResult = _scanResult.asStateFlow()

    private val _suggestedTemplateId = MutableStateFlow<String?>(null)
    val suggestedTemplateId = _suggestedTemplateId.asStateFlow()

    private val _selectedTemplateId = MutableStateFlow<String?>(null)
    val selectedTemplateId = _selectedTemplateId.asStateFlow()

    val templates = templateRepository.getTemplates()

    val userCharacter: StateFlow<UserCharacter?> = characterRepository.getUserCharacter()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun nextStep() {
        if (_currentStep.value < 8) {
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

    fun calculateResults(onDone: () -> Unit) {
        viewModelScope.launch {
            // Wait for user character if it's still null, with a timeout or just a short wait
            val user = userCharacter.value ?: characterRepository.getUserCharacter().first()
            
            if (user != null) {
                val results = AttributeCalculator.calculateAll(user, _inputs.value)
                _scanResult.value = results
                
                // Suggest archetype based on closest stat match
                _suggestedTemplateId.value = findBestMatch(results)
                if (_selectedTemplateId.value == null) {
                    _selectedTemplateId.value = _suggestedTemplateId.value
                }
            }
            onDone()
        }
    }

    fun abort(onComplete: () -> Unit) {
        viewModelScope.launch {
            calculateResults {
                viewModelScope.launch {
                    performSave()
                    onComplete()
                }
            }
        }
    }

    private fun findBestMatch(scores: CalculatedScores): String {
        return templates.minByOrNull { template ->
            abs(template.strength - scores.strength) +
            abs(template.agility - scores.agility) +
            abs(template.endurance - scores.endurance) +
            abs(template.intelligence - scores.intelligence) +
            abs(template.perception - scores.perception) +
            abs(template.charisma - scores.charisma) +
            abs(template.luck - scores.luck)
        }?.id ?: "SOLO"
    }

    fun selectTemplate(id: String) {
        _selectedTemplateId.value = id
    }

    fun saveResults() {
        viewModelScope.launch {
            performSave()
        }
    }

    private suspend fun performSave() {
        val results = _scanResult.value ?: return
        val user = userCharacter.value ?: return
        val template = templateRepository.getTemplateById(_selectedTemplateId.value ?: "SOLO")
        
        val updatedUser = user.copy(
            archetype = template?.name ?: user.archetype,
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
