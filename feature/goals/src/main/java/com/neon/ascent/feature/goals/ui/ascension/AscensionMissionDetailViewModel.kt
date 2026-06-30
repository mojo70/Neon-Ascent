package com.neon.ascent.feature.goals.ui.ascension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.feature.goals.domain.usecases.NeonMentorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class MissionDetailUiState(
    val mission: AscensionMission? = null,
    val parentDirective: AscensionDirective? = null,
    val tasks: List<AscensionTask> = emptyList(),
    val isLoading: Boolean = true,
    
    // Mentor Mode Chat
    val chatHistory: List<ChatMessage> = emptyList(),
    val isAskingMentor: Boolean = false,
    val selectedMentorMode: MentorMode = MentorMode.REVIEW,
    val isGeneratingTasks: Boolean = false
) {
    data class ChatMessage(
        val text: String,
        val isUser: Boolean
    )
}

@HiltViewModel
class AscensionMissionDetailViewModel @Inject constructor(
    private val repository: AscensionRepository,
    private val mentorUseCase: NeonMentorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MissionDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var missionId: String? = null

    // Directives for moving the mission to another directive
    val activeDirectives = repository.getAllDirectives()
        .map { list -> list.filter { it.status == DirectiveStatus.ACTIVE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadMission(id: String) {
        missionId = id
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // Find mission
            val missionFlow = repository.getActiveMissions().map { list -> list.find { it.id == id } }
            val tasksFlow = repository.getTasksForParent(id)
            val directivesFlow = repository.getAllDirectives()

            combine(missionFlow, tasksFlow, directivesFlow) { mis, tsk, dirs ->
                if (mis != null) {
                    val parentDir = dirs.find { it.id == mis.directiveId }
                    _uiState.update {
                        it.copy(
                            mission = mis,
                            parentDirective = parentDir,
                            tasks = tsk,
                            selectedMentorMode = mis.aiMentorMode,
                            isLoading = false
                        )
                    }
                }
            }.collect()
        }
    }

    fun updateNotes(notes: String) {
        val mis = _uiState.value.mission ?: return
        viewModelScope.launch {
            repository.updateMission(mis.copy(notes = notes))
        }
    }

    fun updateMentorMode(mode: MentorMode) {
        val mis = _uiState.value.mission ?: return
        _uiState.update { it.copy(selectedMentorMode = mode) }
        viewModelScope.launch {
            repository.updateMission(mis.copy(aiMentorMode = mode))
        }
    }

    fun updateStatus(status: AscensionMissionStatus) {
        val mis = _uiState.value.mission ?: return
        viewModelScope.launch {
            repository.updateMission(mis.copy(status = status))
        }
    }

    fun updateMissionDetails(
        title: String,
        description: String,
        objective: String?,
        successCriteria: String?,
        targetEndDate: LocalDate?
    ) {
        val mis = _uiState.value.mission ?: return
        viewModelScope.launch {
            repository.updateMission(
                mis.copy(
                    title = title,
                    description = description,
                    objective = objective,
                    successCriteria = successCriteria,
                    targetEndDate = targetEndDate
                )
            )
        }
    }

    fun moveMissionToDirective(newDirectiveId: String?) {
        val mis = _uiState.value.mission ?: return
        viewModelScope.launch {
            repository.updateMission(mis.copy(directiveId = newDirectiveId))
        }
    }

    fun duplicateMission() {
        val mis = _uiState.value.mission ?: return
        viewModelScope.launch {
            val duplicate = mis.copy(
                id = UUID.randomUUID().toString(),
                title = "${mis.title} (COPY)",
                createdAt = Instant.now(),
                progress = 0f,
                totalXPContributed = 0L
            )
            repository.insertMission(duplicate)
        }
    }

    fun addTask(title: String, description: String, isRecurring: Boolean) {
        val misId = missionId ?: return
        viewModelScope.launch {
            val task = AscensionTask(
                id = UUID.randomUUID().toString(),
                parentId = misId,
                title = title,
                description = description,
                type = if (isRecurring) AscensionTaskType.RECURRING else AscensionTaskType.ONE_TIME,
                recurrence = if (isRecurring) RecurrenceV3(type = RecurrenceTypeV3.DAILY) else null,
                lastCompleted = null
            )
            repository.insertTask(task)
            recalculateProgress()
        }
    }

    fun completeTask(task: AscensionTask) {
        viewModelScope.launch {
            repository.completeTask(task, "Completed from Mission context", 3, null)
            recalculateProgress()
        }
    }

    fun askMentor(message: String) {
        val mis = _uiState.value.mission ?: return
        val dir = _uiState.value.parentDirective
        val currentHistory = _uiState.value.chatHistory
        
        val userMsg = MissionDetailUiState.ChatMessage(text = message, isUser = true)
        _uiState.update { 
            it.copy(
                chatHistory = currentHistory + userMsg,
                isAskingMentor = true 
            ) 
        }

        viewModelScope.launch {
            // Re-use Directive-level Dialogue helper by wrapping current context
            val dummyDirective = dir ?: AscensionDirective(
                id = UUID.randomUUID().toString(),
                title = "Parent Directive Protocol",
                description = "Tactical alignment grid"
            )
            
            val response = mentorUseCase.getMentorDialogue(
                directive = dummyDirective,
                missions = listOf(mis),
                tasks = _uiState.value.tasks,
                mode = _uiState.value.selectedMentorMode,
                message = message
            )
            val mentorMsg = MissionDetailUiState.ChatMessage(text = response.text, isUser = false)
            _uiState.update { 
                it.copy(
                    chatHistory = it.chatHistory + mentorMsg,
                    isAskingMentor = false 
                ) 
            }
        }
    }

    fun clearChat() {
        _uiState.update { it.copy(chatHistory = emptyList()) }
    }

    fun expandWithAi() {
        val mis = _uiState.value.mission ?: return
        _uiState.update { it.copy(isGeneratingTasks = true) }
        viewModelScope.launch {
            mentorUseCase.generateTasksForMission(mis)
            _uiState.update { it.copy(isGeneratingTasks = false) }
            recalculateProgress()
        }
    }

    private suspend fun recalculateProgress() {
        val mis = _uiState.value.mission ?: return
        val tasks = _uiState.value.tasks

        val calculatedProgress = if (tasks.isNotEmpty()) {
            val completedCount = tasks.count { it.lastCompleted != null }
            completedCount.toFloat() / tasks.size
        } else {
            0f
        }

        repository.updateMission(mis.copy(progress = calculatedProgress))
        
        // Propagate progress to parent directive
        val dirId = mis.directiveId
        if (dirId != null) {
            val siblingMissions = repository.getMissionsForDirective(dirId).first()
            val directiveTasks = repository.getTasksForParent(dirId).first()
            
            val missionWeight = 0.7f
            val taskWeight = 0.3f

            val missionsProgress = if (siblingMissions.isNotEmpty()) siblingMissions.map { it.progress }.average().toFloat() else 1.0f
            val tasksProgress = if (directiveTasks.isNotEmpty()) {
                val completedCount = directiveTasks.count { it.lastCompleted != null }
                completedCount.toFloat() / directiveTasks.size
            } else {
                1.0f
            }

            val calculatedDirProgress = when {
                siblingMissions.isEmpty() && directiveTasks.isEmpty() -> 0f
                siblingMissions.isEmpty() -> tasksProgress
                directiveTasks.isEmpty() -> missionsProgress
                else -> (missionsProgress * missionWeight) + (tasksProgress * taskWeight)
            }
            
            repository.getAllDirectives().first().find { it.id == dirId }?.let { directive ->
                repository.updateDirective(directive.copy(currentProgress = calculatedDirProgress))
            }
        }
    }
}
