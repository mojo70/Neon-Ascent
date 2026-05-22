package com.neon.ascent.feature.goals.ui.ascension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.common.DopamineCoordinator
import com.neon.ascent.core.common.DopamineEvent
import com.neon.ascent.core.common.CelebrationLevel
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.feature.goals.domain.usecases.NeonMentorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

data class TaskDetailUiState(
    val task: AscensionTask? = null,
    val guideText: String? = null,
    val reflectionText: String? = null,
    val isCompletedToday: Boolean = false,
    val isLoading: Boolean = true,
    val showReflection: Boolean = false,
    val dopamineEvent: DopamineEvent? = null,
    val completions: List<AscensionTaskCompletion> = emptyList(),
    val parentName: String? = null,
    val parentType: String? = null, // "MISSION" or "DIRECTIVE"
    val mentorAnswer: String? = null,
    val isAskingMentor: Boolean = false
)

@HiltViewModel
class AscensionTaskDetailViewModel @Inject constructor(
    private val repository: AscensionRepository,
    private val mentorUseCase: NeonMentorUseCase,
    private val dopamineCoordinator: DopamineCoordinator
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dopamineCoordinator.events.collect { event ->
                _uiState.update { it.copy(dopamineEvent = event) }
            }
        }
    }

    fun clearDopamineEvent() {
        _uiState.update { it.copy(dopamineEvent = null) }
    }

    fun loadTask(taskId: String) {
        viewModelScope.launch {
            repository.getAllRecurringTasks().first().find { it.id == taskId }?.let { task ->
                val today = LocalDate.now()
                val lastCompletedDate = task.lastCompleted?.atZone(ZoneId.systemDefault())?.toLocalDate()
                val isCompleted = lastCompletedDate == today
                
                _uiState.update { 
                    it.copy(
                        task = task, 
                        isCompletedToday = isCompleted, 
                        isLoading = false
                    ) 
                }
                
                // Pre-load guide if not completed
                if (!isCompleted) {
                    val guide = mentorUseCase.getGuide(task)
                    _uiState.update { it.copy(guideText = guide) }
                }

                // Resolve parent name and type
                val parentId = task.parentId
                if (parentId != null) {
                    val parentMission = repository.getActiveMissions().first().find { it.id == parentId }
                    if (parentMission != null) {
                        _uiState.update { 
                            it.copy(
                                parentName = parentMission.title, 
                                parentType = "MISSION"
                            ) 
                        }
                    } else {
                        val parentDirective = repository.getAllDirectives().first().find { it.id == parentId }
                        if (parentDirective != null) {
                            _uiState.update { 
                                it.copy(
                                    parentName = parentDirective.title, 
                                    parentType = "DIRECTIVE"
                                ) 
                            }
                        }
                    }
                }

                // Collect completions
                repository.getCompletionsForTask(taskId).collect { completions ->
                    _uiState.update { it.copy(completions = completions) }
                }
            }
        }
    }

    fun completeTask(notes: String?, mood: Int?) {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            repository.completeTask(currentTask, notes, mood, null)
            _uiState.update { it.copy(isCompletedToday = true, showReflection = true) }
            
            // Trigger Dopamine Menu
            if (currentTask.type == AscensionTaskType.RECURRING) {
                dopamineCoordinator.triggerSync(xp = currentTask.xpValue)
            } else {
                dopamineCoordinator.triggerSubtle(xp = currentTask.xpValue)
            }

            // Generate Dialectic Reflection
            val reflection = mentorUseCase.getReflection(currentTask, notes)
            _uiState.update { it.copy(reflectionText = reflection) }
        }
    }

    fun askMentor(question: String) {
        val currentTask = _uiState.value.task ?: return
        _uiState.update { it.copy(isAskingMentor = true, mentorAnswer = null) }
        viewModelScope.launch {
            val answer = mentorUseCase.askMentor(
                task = currentTask,
                parentName = _uiState.value.parentName,
                parentType = _uiState.value.parentType,
                question = question
            )
            _uiState.update { it.copy(isAskingMentor = false, mentorAnswer = answer) }
        }
    }

    fun snoozeTask() {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            repository.insertNeuralLog(
                title = "PROTOCOL_SNOOZED: ${currentTask.title}",
                content = "Execution window postponed by 1 hour.",
                type = "SYSTEM_ALERT"
            )
        }
    }

    fun skipWithReflection(reason: String) {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            // Log a special completion representing a skip
            repository.completeTask(currentTask, "[SKIPPED] Reason: $reason", 0, null)
            repository.insertNeuralLog(
                title = "PROTOCOL_SKIPPED: ${currentTask.title}",
                content = "Operator opted to skip execution. Self-reflective reason: $reason",
                type = "SYSTEM_ALERT"
            )
            _uiState.update { it.copy(isCompletedToday = true) }
        }
    }

    fun markCompleteLater() {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            repository.insertNeuralLog(
                title = "SCHEDULE_OVERRIDE: ${currentTask.title}",
                content = "Target execution window deferred to tomorrow.",
                type = "SYSTEM_ALERT"
            )
        }
    }

    fun editTask(title: String, description: String, graceBuffer: Int, xp: Int) {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            val updated = currentTask.copy(
                title = title,
                description = description,
                graceBufferDays = graceBuffer,
                xpValue = xp
            )
            repository.updateTask(updated)
            _uiState.update { it.copy(task = updated) }
        }
    }

    fun archiveTask() {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            repository.deleteTask(currentTask.id)
        }
    }

    fun duplicateTask() {
        val currentTask = _uiState.value.task ?: return
        viewModelScope.launch {
            val duplicate = currentTask.copy(
                id = UUID.randomUUID().toString(),
                title = "${currentTask.title} (COPY)",
                currentStreak = 0,
                longestStreak = 0,
                lastCompleted = null
            )
            repository.insertTask(duplicate)
        }
    }
}
