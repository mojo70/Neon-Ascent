package com.neon.ascent.feature.charactercreation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.SpecialRepository
import com.neon.ascent.core.domain.model.BenchmarkTest
import com.neon.ascent.core.domain.model.DataSource
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.TestType
import java.util.UUID
import java.time.Instant
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
    private val templateRepository: TemplateRepository,
    private val specialRepository: SpecialRepository
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

    private val _isUpdateMode = MutableStateFlow(false)
    val isUpdateMode = _isUpdateMode.asStateFlow()

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
        onComplete()
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

    fun saveResults(onDone: () -> Unit) {
        viewModelScope.launch {
            performSave()
            onDone()
        }
    }

    fun setUpdateMode(isUpdate: Boolean) {
        _isUpdateMode.value = isUpdate
    }

    private suspend fun performSave() {
        val results = _scanResult.value ?: return
        val user = userCharacter.value ?: characterRepository.getUserCharacter().first() ?: return
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

        // Grounding in history for progression graph
        val specialTypes = listOf(
            com.neon.ascent.core.domain.model.SpecialType.STRENGTH to results.strength,
            com.neon.ascent.core.domain.model.SpecialType.ENDURANCE to results.endurance,
            com.neon.ascent.core.domain.model.SpecialType.AGILITY to results.agility,
            com.neon.ascent.core.domain.model.SpecialType.PERCEPTION to results.perception,
            com.neon.ascent.core.domain.model.SpecialType.INTELLIGENCE to results.intelligence,
            com.neon.ascent.core.domain.model.SpecialType.CHARISMA to results.charisma,
            com.neon.ascent.core.domain.model.SpecialType.LUCK to results.luck
        )

        val percentileMap = mapOf(
            com.neon.ascent.core.domain.model.SpecialType.STRENGTH to (results.strengthPercentile * 100).toInt(),
            com.neon.ascent.core.domain.model.SpecialType.ENDURANCE to (results.endurancePercentile * 100).toInt(),
            com.neon.ascent.core.domain.model.SpecialType.AGILITY to (results.agilityPercentile * 100).toInt(),
            com.neon.ascent.core.domain.model.SpecialType.PERCEPTION to (results.perceptionPercentile * 100).toInt(),
            com.neon.ascent.core.domain.model.SpecialType.INTELLIGENCE to (results.intelligencePercentile * 100).toInt(),
            com.neon.ascent.core.domain.model.SpecialType.CHARISMA to (results.charismaPercentile * 100).toInt(),
            com.neon.ascent.core.domain.model.SpecialType.LUCK to (results.luckPercentile * 100).toInt()
        )

        val updateMode = _isUpdateMode.value

        specialTypes.forEach { (type, score) ->
            val percentile = percentileMap[type] ?: 50
            if (!updateMode) {
                specialRepository.deleteBenchmarkHistory(type)
            }
            saveSpecialBenchmark(type, score.toDouble(), percentile)
        }
    }

    private suspend fun saveSpecialBenchmark(type: com.neon.ascent.core.domain.model.SpecialType, score: Double, percentile: Int) {
        val test = BenchmarkTest(
            id = UUID.randomUUID().toString(),
            attribute = type,
            testType = TestType.PHYSICAL_SELF_REPORT,
            rawScore = score,
            normalizedScore = score / 10.0,
            percentile = percentile,
            timestamp = Instant.now(),
            source = DataSource.INTAKE
        )
        specialRepository.saveBenchmark(test)
        
        // Update the current attribute state
        val currentAttr = specialRepository.getSpecialAttribute(type).first()
        if (currentAttr != null) {
            specialRepository.updateSpecialAttribute(currentAttr.copy(
                currentValue = score.toInt(),
                percentile = percentile,
                lastUpdated = Instant.now()
            ))
        }
    }
}
