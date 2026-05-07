package com.neon.ascent.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.data.repository.GoalRepository
import com.neon.ascent.domain.model.Goal
import com.neon.ascent.domain.model.SpecialType
import com.neon.ascent.domain.usecase.SuggestGoalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GoalIntakeViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val suggestGoalsUseCase: SuggestGoalsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalIntakeState())
    val uiState: StateFlow<GoalIntakeState> = _uiState.asStateFlow()

    private val _suggestedGoals = MutableStateFlow<List<Goal>>(emptyList())
    val suggestedGoals: StateFlow<List<Goal>> = _suggestedGoals.asStateFlow()

    init {
        viewModelScope.launch {
            _suggestedGoals.value = suggestGoalsUseCase.suggestGoals()
        }
    }

    fun updateTitle(title: String) { _uiState.update { it.copy(title = title) } }
    fun updateDescription(desc: String) { _uiState.update { it.copy(description = desc) } }
    fun updateTargetValue(value: String) { _uiState.update { it.copy(targetValue = value) } }
    fun updateUnit(unit: String) { _uiState.update { it.copy(unit = unit) } }
    fun updateAspirationLink(link: String) { _uiState.update { it.copy(aspirationLink = link) } }
    fun updateLinkedSpecial(special: SpecialType?) { _uiState.update { it.copy(linkedSpecial = special) } }

    fun applySuggestion(suggestion: Goal) {
        _uiState.update {
            it.copy(
                title = suggestion.title,
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
    val description: String = "",
    val targetValue: String = "",
    val unit: String = "hours",
    val aspirationLink: String = "",
    val linkedSpecial: SpecialType? = null
)
