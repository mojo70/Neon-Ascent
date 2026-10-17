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
    val showDensityBanner: Boolean = false,
    val densityNoteMessage: String? = null,
    
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
            val activeDirectivesFlow = repository.getActiveDirectives()
            val missionsFlow = repository.getMissionsForDirective(id)
            val tasksFlow = repository.getTasksForParent(id)

            combine(activeDirectivesFlow, missionsFlow, tasksFlow) { activeDirectives, mis, tsk ->
                val dir = activeDirectives.find { it.id == id } ?: repository.getAllDirectives().first().find { it.id == id }
                if (dir != null) {
                    val today = LocalDate.now()
                    val horizon = today.plusDays(90)
                    val nearTermCount = activeDirectives.count { 
                        val endDate = it.targetEndDate
                        endDate != null && !endDate.isAfter(horizon) 
                    }
                    val aTierCount = activeDirectives.count { it.priorityTier == PriorityTier.A }
                    val isDense = (nearTermCount >= 4 || aTierCount >= 3)

                    _uiState.update { 
                        it.copy(
                            directive = dir,
                            missions = mis,
                            directTasks = tsk,
                            selectedMentorMode = dir.aiMentorMode,
                            isLoading = false,
                            showDensityBanner = isDense,
                            densityNoteMessage = if (isDense) "$nearTermCount near-term · $aTierCount A-tier. Review priority?" else null
                        ) 
                    }
                }
            }.collect()
        }
    }

    fun dismissDensityBanner() {
        _uiState.update { it.copy(showDensityBanner = false) }
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
        targetEndDate: LocalDate?,
        priorityTier: PriorityTier? = PriorityTier.B,
        outcomeStatement: String? = null,
        affirmation: String? = null,
        visualUri: String? = null
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
                    targetEndDate = targetEndDate,
                    priorityTier = priorityTier,
                    outcomeStatement = outcomeStatement,
                    affirmation = affirmation,
                    visualUri = visualUri
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
