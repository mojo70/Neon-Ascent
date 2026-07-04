package com.neon.ascent.feature.goals.ui.ascension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.feature.goals.domain.usecases.NeonMentorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class DirectiveDetailUiState(
    val directive: AscensionDirective? = null,
    val missions: List<AscensionMission> = emptyList(),
    val directTasks: List<AscensionTask> = emptyList(),
    val isLoading: Boolean = true,
    
    // Mentor Mode Chat
    val chatHistory: List<ChatMessage> = emptyList(),
    val isAskingMentor: Boolean = false,
    val selectedMentorMode: MentorMode = MentorMode.REVIEW
) {
    data class ChatMessage(
        val text: String,
        val isUser: Boolean
    )
}

@HiltViewModel
class AscensionDirectiveDetailViewModel @Inject constructor(
    private val repository: AscensionRepository,
    private val mentorUseCase: NeonMentorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DirectiveDetailUiState())
    val uiState = _uiState.asStateFlow()

    private var directiveId: String? = null

    fun loadDirective(id: String) {
        directiveId = id
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // Find the directive
            val directiveFlow = repository.getAllDirectives().map { list -> list.find { it.id == id } }
            val missionsFlow = repository.getMissionsForDirective(id)
            val tasksFlow = repository.getTasksForParent(id)

            combine(directiveFlow, missionsFlow, tasksFlow) { dir, mis, tsk ->
                if (dir != null) {
                    _uiState.update { 
                        it.copy(
                            directive = dir,
                            missions = mis,
                            directTasks = tsk,
                            selectedMentorMode = dir.aiMentorMode,
                            isLoading = false
                        ) 
                    }
                }
            }.collect()
        }
    }

    fun updateNotes(notes: String) {
        val dir = _uiState.value.directive ?: return
        viewModelScope.launch {
            repository.updateDirective(dir.copy(notes = notes))
        }
    }

    fun updateMentorMode(mode: MentorMode) {
        val dir = _uiState.value.directive ?: return
        _uiState.update { it.copy(selectedMentorMode = mode) }
        viewModelScope.launch {
            repository.updateDirective(dir.copy(aiMentorMode = mode))
        }
    }

    fun updateStatus(status: DirectiveStatus) {
        val dir = _uiState.value.directive ?: return
        viewModelScope.launch {
            repository.updateDirective(dir.copy(status = status))
        }
    }

    fun updateDirectiveDetails(
        title: String,
        description: String,
        visionStatement: String?,
        isQuarterly: Boolean,
        archetypeTag: String?,
        targetEndDate: LocalDate?
    ) {
        val dir = _uiState.value.directive ?: return
        viewModelScope.launch {
            repository.updateDirective(
                dir.copy(
                    title = title,
                    description = description,
                    visionStatement = visionStatement,
                    isQuarterly = isQuarterly,
                    archetypeTag = archetypeTag,
                    targetEndDate = targetEndDate
                )
            )
        }
    }

    fun duplicateDirective() {
        val dir = _uiState.value.directive ?: return
        viewModelScope.launch {
            val duplicate = dir.copy(
                id = UUID.randomUUID().toString(),
                title = "${dir.title} (COPY)",
                createdAt = Instant.now(),
                currentProgress = 0f,
                totalXPContributed = 0L
            )
            repository.insertDirective(duplicate)
        }
    }

    fun addMission(title: String, description: String) {
        val dirId = directiveId ?: return
        viewModelScope.launch {
            val mission = AscensionMission(
                id = UUID.randomUUID().toString(),
                directiveId = dirId,
                title = title,
                description = description,
                status = AscensionMissionStatus.ACTIVE,
                progress = 0f,
                aiGenerated = false,
                contributionWeight = 1.0f
            )
            repository.insertMission(mission)
        }
    }

    fun addStandaloneTask(title: String, description: String) {
        val dirId = directiveId ?: return
        viewModelScope.launch {
            val task = AscensionTask(
                id = UUID.randomUUID().toString(),
                parentId = dirId,
                title = title,
                description = description,
                type = AscensionTaskType.ONE_TIME,
                lastCompleted = null,
                impactWeight = 1.0f
            )
            repository.insertTask(task)
        }
    }

    fun completeStandaloneTask(task: AscensionTask) {
        viewModelScope.launch {
            repository.completeTask(task, "Completed directly", 3, null)
        }
    }

    fun askMentor(message: String) {
        val dir = _uiState.value.directive ?: return
        val currentHistory = _uiState.value.chatHistory
        
        val userMsg = DirectiveDetailUiState.ChatMessage(text = message, isUser = true)
        _uiState.update { 
            it.copy(
                chatHistory = currentHistory + userMsg,
                isAskingMentor = true 
            ) 
        }

        viewModelScope.launch {
            val response = mentorUseCase.getMentorDialogue(
                directive = dir,
                missions = _uiState.value.missions,
                tasks = _uiState.value.directTasks,
                mode = _uiState.value.selectedMentorMode,
                message = message
            )
            val mentorMsg = DirectiveDetailUiState.ChatMessage(text = response.text, isUser = false)
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
}
