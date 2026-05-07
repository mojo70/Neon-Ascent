package com.neon.ascent.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.repository.GoalRepository
import com.neon.ascent.data.repository.UserStoryRepository
import com.neon.ascent.domain.model.Goal
import com.neon.ascent.domain.model.SpecialType
import com.neon.ascent.domain.usecase.SuggestGoalsUseCase
import com.neon.ascent.feature.biohacking.AiProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class AiMessage(
    val content: String,
    val isUser: Boolean
)

@HiltViewModel
class GoalIntakeViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val suggestGoalsUseCase: SuggestGoalsUseCase,
    private val userStoryRepository: UserStoryRepository,
    private val aiProvider: AiProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalIntakeState())
    val uiState: StateFlow<GoalIntakeState> = _uiState.asStateFlow()

    private val _suggestedGoals = MutableStateFlow<List<Goal>>(emptyList())
    val suggestedGoals: StateFlow<List<Goal>> = _suggestedGoals.asStateFlow()

    init {
        viewModelScope.launch {
            _suggestedGoals.value = suggestGoalsUseCase.suggestGoals()
        }
        viewModelScope.launch {
            userStoryRepository.getMainStory().collect { story ->
                _uiState.update { it.copy(allAspirations = story.grandAspirations) }
            }
        }
    }

    fun updateTitle(title: String) { _uiState.update { it.copy(title = title) } }
    fun updateObjective(objective: String) { _uiState.update { it.copy(objective = objective) } }
    fun updateDescription(desc: String) { _uiState.update { it.copy(description = desc) } }
    fun updateTargetValue(value: String) { _uiState.update { it.copy(targetValue = value) } }
    fun updateUnit(unit: String) { _uiState.update { it.copy(unit = unit) } }
    fun updateAspirationLink(link: String) { _uiState.update { it.copy(aspirationLink = link) } }
    fun updateLinkedSpecial(special: SpecialType?) { _uiState.update { it.copy(linkedSpecial = special) } }

    fun createNewAspiration(aspiration: String) {
        viewModelScope.launch {
            val currentAspirations = _uiState.value.allAspirations
            if (!currentAspirations.contains(aspiration)) {
                userStoryRepository.updateAspirations(currentAspirations + aspiration)
                updateAspirationLink(aspiration)
            }
        }
    }

    fun sendToLocalAI(message: String) {
        val userMsg = AiMessage(message, true)
        _uiState.update { it.copy(aiMessages = it.aiMessages + userMsg) }
        
        viewModelScope.launch {
            val prompt = """
                SYSTEM_MISSION_ADVISOR
                The user is seeking guidance on their mission: "${_uiState.value.title}"
                User Message: $message
                
                Provide a short, cyberpunk-themed suggestion for this mission or its objective.
            """.trimIndent()
            
            val response = aiProvider.generateContent(prompt, forceLocal = true)
            val aiMsg = AiMessage(response, false)
            _uiState.update { it.copy(aiMessages = it.aiMessages + aiMsg) }
        }
    }

    fun applySuggestion(suggestion: Goal) {
        _uiState.update {
            it.copy(
                title = suggestion.title,
                objective = suggestion.objective,
                description = suggestion.description,
                targetValue = suggestion.targetValue.toString(),
                unit = suggestion.unit,
                aspirationLink = suggestion.aspirationLink,
                linkedSpecial = suggestion.linkedSpecial
            )
        }
    }

    fun createGoal() {
        val state = _uiState.value
        val goal = Goal(
            id = UUID.randomUUID().toString(),
            title = state.title,
            objective = state.objective,
            description = state.description,
            aspirationLink = state.aspirationLink,
            targetValue = state.targetValue.toFloatOrNull() ?: 100f,
            currentValue = 0f,
            unit = state.unit,
            linkedSpecial = state.linkedSpecial,
            deadline = null,
            isActive = true
        )
        viewModelScope.launch {
            goalRepository.createGoal(goal)
        }
    }
}

data class GoalIntakeState(
    val title: String = "",
    val objective: String = "",
    val description: String = "",
    val targetValue: String = "",
    val unit: String = "hours",
    val aspirationLink: String = "",
    val allAspirations: List<String> = emptyList(),
    val linkedSpecial: SpecialType? = null,
    val aiMessages: List<AiMessage> = emptyList()
)
