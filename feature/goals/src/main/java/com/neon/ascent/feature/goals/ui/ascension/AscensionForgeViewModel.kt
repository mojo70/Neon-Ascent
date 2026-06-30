package com.neon.ascent.feature.goals.ui.ascension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.SkillRepository
import com.neon.ascent.core.domain.NeuralPingScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import com.neon.ascent.feature.goals.domain.usecases.NeonMentorUseCase
import com.neon.ascent.core.common.DopamineCoordinator

enum class ForgeType { DIRECTIVE, MISSION, TASK }

data class AscensionForgeUiState(
    val forgeType: ForgeType = ForgeType.DIRECTIVE,
    val title: String = "",
    val description: String = "",
    // Directive specific
    val visionStatement: String = "",
    val isQuarterly: Boolean = false,
    val selectedArchetype: String? = null,
    val aiMentorMode: MentorMode = MentorMode.REVIEW,
    val linkedAttributes: List<SpecialType> = emptyList(),
    val useAiMentor: Boolean = true,
    val selectedSkill: String? = null,
    val isFromInsight: Boolean = false,
    val biometricContext: String? = null,
    val chatHistory: List<MentorUiMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val mentorInput: String = "",
    // Task specific
    val taskType: AscensionTaskType = AscensionTaskType.ONE_TIME,
    val recurrenceType: RecurrenceTypeV3 = RecurrenceTypeV3.DAILY,
    val recurrenceDays: Set<DayOfWeek> = emptySet(),
    val timeWindows: List<String> = emptyList(),
    // Mission specific
    val parentDirectiveId: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AscensionForgeViewModel @Inject constructor(
    private val repository: AscensionRepository,
    private val mentorUseCase: NeonMentorUseCase,
    private val skillRepository: SkillRepository,
    private val dopamineCoordinator: DopamineCoordinator,
    private val neuralPingScheduler: NeuralPingScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(AscensionForgeUiState())
    val uiState = _uiState.asStateFlow()

    fun updateType(type: ForgeType) = _uiState.update { it.copy(forgeType = type) }
    fun updateTitle(title: String) = _uiState.update { it.copy(title = title) }
    fun updateDescription(desc: String) = _uiState.update { it.copy(description = desc) }
    fun updateVisionStatement(vision: String) = _uiState.update { it.copy(visionStatement = vision) }
    fun updateIsQuarterly(quarterly: Boolean) = _uiState.update { it.copy(isQuarterly = quarterly) }
    fun updateSelectedArchetype(archetype: String?) = _uiState.update { it.copy(selectedArchetype = archetype) }
    fun updateAiMentorMode(mode: MentorMode) = _uiState.update { it.copy(aiMentorMode = mode) }
    fun updateSelectedSkill(skill: String?) = _uiState.update { it.copy(selectedSkill = skill) }
    fun updateMentorInput(input: String) = _uiState.update { it.copy(mentorInput = input) }
    
    fun toggleAttribute(type: SpecialType) = _uiState.update { state ->
        val newList = if (state.linkedAttributes.contains(type)) {
            state.linkedAttributes - type
        } else {
            state.linkedAttributes + type
        }
        state.copy(linkedAttributes = newList)
    }

    fun updateUseAiMentor(use: Boolean) = _uiState.update { it.copy(useAiMentor = use) }
    fun updateTaskType(type: AscensionTaskType) = _uiState.update { it.copy(taskType = type) }
    fun updateRecurrence(type: RecurrenceTypeV3) = _uiState.update { it.copy(recurrenceType = type) }
    
    fun toggleDay(day: DayOfWeek) = _uiState.update { state ->
        val newDays = if (state.recurrenceDays.contains(day)) state.recurrenceDays - day else state.recurrenceDays + day
        state.copy(recurrenceDays = newDays, recurrenceType = RecurrenceTypeV3.DAYS_OF_WEEK)
    }

    fun addTimeWindow(window: String) = _uiState.update { it.copy(timeWindows = it.timeWindows + window) }
    fun removeTimeWindow(window: String) = _uiState.update { it.copy(timeWindows = it.timeWindows - window) }
    fun updateParentDirective(id: String?) = _uiState.update { it.copy(parentDirectiveId = id) }

    fun prefill(attributeName: String?, title: String?, description: String?, vision: String? = null, biometricContext: String? = null) {
        _uiState.update { state ->
            val attr = attributeName?.let { 
                try { SpecialType.valueOf(it) } catch(e: Exception) { null } 
            }
            state.copy(
                title = title ?: state.title,
                description = description ?: state.description,
                visionStatement = vision ?: state.visionStatement,
                linkedAttributes = if (attr != null && !state.linkedAttributes.contains(attr)) state.linkedAttributes + attr else state.linkedAttributes,
                useAiMentor = true,
                isFromInsight = true,
                biometricContext = biometricContext
            )
        }
        
        // If prefilled, start a conversation with the mentor automatically
        if (title != null || description != null) {
            startInitialConversation()
        }
    }

    private fun startInitialConversation() {
        val state = _uiState.value
        val context = "Insight Title: ${state.title}\nDescription: ${state.description}\nBiometrics: ${state.biometricContext ?: "None"}"
        val initialPrompt = "OPERATOR_INIT: User is forging a directive from an external insight. Context: $context. Analyze and suggest refinements or mission structures."
        
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val response = mentorUseCase.getMentorDialogue(
                    directive = AscensionDirective(id = "", title = state.title, description = state.description),
                    missions = emptyList(),
                    tasks = emptyList(),
                    mode = MentorMode.GUIDE,
                    message = initialPrompt
                )
                _uiState.update { it.copy(
                    chatHistory = it.chatHistory + MentorUiMessage(response, isFromUser = false),
                    isGenerating = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun sendMentorMessage() {
        val input = _uiState.value.mentorInput
        if (input.isBlank()) return

        val state = _uiState.value
        _uiState.update { it.copy(
            chatHistory = it.chatHistory + MentorUiMessage(input, isFromUser = true),
            mentorInput = "",
            isGenerating = true
        ) }

        viewModelScope.launch {
            try {
                val response = mentorUseCase.getMentorDialogue(
                    directive = AscensionDirective(id = "", title = state.title, description = state.description),
                    missions = emptyList(),
                    tasks = emptyList(),
                    mode = MentorMode.GUIDE,
                    message = input
                )
                _uiState.update { it.copy(
                    chatHistory = it.chatHistory + MentorUiMessage(response, isFromUser = false),
                    isGenerating = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                when (state.forgeType) {
                    ForgeType.DIRECTIVE -> {
                        val directive = AscensionDirective(
                            id = UUID.randomUUID().toString(),
                            title = state.title,
                            description = state.description,
                            visionStatement = state.visionStatement.takeIf { it.isNotBlank() },
                            isQuarterly = state.isQuarterly,
                            archetypeTag = state.selectedArchetype,
                            aiMentorMode = state.aiMentorMode,
                            createdAt = Instant.now()
                        )
                        repository.insertDirective(directive)
                        if (state.useAiMentor) {
                            val skillPrompt = state.selectedSkill?.let { skillRepository.getSkillPrompt(it) }
                            mentorUseCase.generateMissionsForDirective(directive, skillPrompt)
                        }
                    }
                    ForgeType.MISSION -> {
                        val mission = AscensionMission(
                            id = UUID.randomUUID().toString(),
                            directiveId = state.parentDirectiveId,
                            title = state.title,
                            description = state.description,
                            createdAt = Instant.now()
                        )
                        repository.insertMission(mission)
                    }
                    ForgeType.TASK -> {
                        val task = AscensionTask(
                            id = UUID.randomUUID().toString(),
                            parentId = null,
                            title = state.title,
                            description = state.description,
                            type = state.taskType,
                            recurrence = if (state.taskType == AscensionTaskType.RECURRING) {
                                RecurrenceV3(
                                    type = state.recurrenceType,
                                    daysOfWeek = if (state.recurrenceType == RecurrenceTypeV3.DAYS_OF_WEEK) state.recurrenceDays else emptySet()
                                )
                            } else null,
                            timeWindows = state.timeWindows
                        )
                        repository.insertTask(task)
                    }
                }
                
                // Delightful Success flow
                _uiState.update { it.copy(isSuccess = true) }
                dopamineCoordinator.triggerAscension(title = "PROTOCOL_DEPLOYED", xp = 50)
                neuralPingScheduler.scheduleSmartPings()
                
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
