package com.neon.ascent.feature.goals.ui.ascension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.feature.goals.domain.usecases.NeonMentorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: AscensionTask? = null,
    val guideText: String? = null,
    val reflectionText: String? = null,
    val isCompletedToday: Boolean = false,
    val isLoading: Boolean = true,
    val showReflection: Boolean = false
)

@HiltViewModel
class AscensionTaskDetailViewModel @Inject constructor(
    private val repository: AscensionRepository,
    private val mentorUseCase: NeonMentorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState = _uiState.asStateFlow()

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            repository.getAllRecurringTasks().first().find { it.id == taskId }?.let { task ->
                // Check if completed today (simplified)
                val isCompleted = task.lastCompleted != null // Needs better logic for recurring
                _uiState.update { it.copy(task = task, isCompletedToday = isCompleted, isLoading = false) }
                
                // Pre-load guide if not completed
                if (!isCompleted) {
                    val guide = mentorUseCase.getGuide(task)
                    _uiState.update { it.copy(guideText = guide) }
                }
            }
        }
    }

    fun completeTask(notes: String?, mood: Int?) {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            repository.completeTask(currentTask, notes, mood, null)
            _uiState.update { it.copy(isCompletedToday = true, showReflection = true) }
            
            // Generate Dialectic Reflection
            val reflection = mentorUseCase.getReflection(currentTask, notes)
            _uiState.update { it.copy(reflectionText = reflection) }
        }
    }
}
