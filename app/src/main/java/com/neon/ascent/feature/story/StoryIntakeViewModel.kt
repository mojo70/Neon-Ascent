package com.neon.ascent.feature.story

import androidx.lifecycle.ViewModel
import com.neon.ascent.data.repository.UserStoryRepository
import com.neon.ascent.domain.model.UserStory
import com.neon.ascent.domain.usecase.GenerateCyberLoreUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class StoryIntakeViewModel @Inject constructor(
    private val userStoryRepository: UserStoryRepository,
    private val generateCyberLoreUseCase: GenerateCyberLoreUseCase
) : ViewModel() {

    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    private val _answers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val answers: StateFlow<Map<Int, String>> = _answers.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun saveAnswer(questionId: Int, answer: String) {
        _answers.update { it.toMutableMap().apply { this[questionId] = answer } }
    }

    fun nextStep() {
        if (_currentStep.value < 4) _currentStep.value++
    }

    fun previousStep() {
        if (_currentStep.value > 1) _currentStep.value--
    }

    suspend fun finishAndSave(): Boolean {
        _isSaving.value = true
        return try {
            // Generate and cache cyber lore
            val lore = generateCyberLoreUseCase.generateLore(_answers.value)
            
            val story = UserStory(
                bio = buildBioSummary(_answers.value),
                grandAspirations = extractAspirations(_answers.value),
                specialAttributes = extractSpecialAttributes(_answers.value),
                cyberLore = lore
            )

            userStoryRepository.saveStory(story)

            true
        } catch (e: Exception) {
            false
        } finally {
            _isSaving.value = false
        }
    }

    private fun buildBioSummary(answers: Map<Int, String>): String {
        return """
            Origin: ${answers[1] ?: ""}
            Turning Point: ${answers[3] ?: ""}
            Current Path: ${answers[4] ?: ""}
            Proudest Moment: ${answers[5] ?: ""}
            Future Vision: ${answers[7] ?: ""}
        """.trimIndent()
    }

    private fun extractAspirations(answers: Map<Int, String>): List<String> {
        val raw = answers[7] ?: ""
        return raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    private fun extractSpecialAttributes(answers: Map<Int, String>): Map<String, Int> {
        // You can parse slider values here if you add them in step 4
        return emptyMap()
    }
}
