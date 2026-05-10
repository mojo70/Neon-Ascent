package com.neon.ascent.feature.habits.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.GoalRepository
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.model.SpecialType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HabitCreationViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitCreationUiState())
    val uiState: StateFlow<HabitCreationUiState> = _uiState.asStateFlow()

    fun updateTitle(title: String) { _uiState.update { it.copy(title = title) } }
    fun updateDescription(desc: String) { _uiState.update { it.copy(description = desc) } }
    fun updateRecurrenceType(type: RecurrenceType) { _uiState.update { it.copy(recurrenceType = type) } }
    fun updateLinkedAttributes(attrs: List<SpecialType>) { _uiState.update { it.copy(linkedAttributes = attrs) } }

    fun saveHabit() {
        val state = _uiState.value
        if (!state.isValid) return

        viewModelScope.launch {
            val newHabit = Habit(
                id = UUID.randomUUID().toString(),
                title = state.title,
                description = state.description,
                recurrence = Recurrence(state.recurrenceType),
                linkedAttributes = state.linkedAttributes,
                progress = GoalProgress(current = 0f, target = 1f),
                streak = 0,
                lastCompleted = null
            )
            goalRepository.saveHabit(newHabit)
        }
    }
}

data class HabitCreationUiState(
    val title: String = "",
    val description: String = "",
    val recurrenceType: RecurrenceType = RecurrenceType.DAILY,
    val linkedAttributes: List<SpecialType> = emptyList()
) {
    val isValid: Boolean = title.isNotBlank() && linkedAttributes.isNotEmpty()
}
