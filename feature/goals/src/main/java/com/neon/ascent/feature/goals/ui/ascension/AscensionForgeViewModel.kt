package com.neon.ascent.feature.goals.ui.ascension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.SkillRepository
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

enum class ForgeType { DIRECTIVE, TASK }

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
    // Task specific
    val taskType: AscensionTaskType = AscensionTaskType.ONE_TIME,
    val recurrenceType: RecurrenceTypeV3 = RecurrenceTypeV3.DAILY,
    val recurrenceDays: Set<DayOfWeek> = emptySet(),
    val timeWindows: List<String> = emptyList()
)

@HiltViewModel
class AscensionForgeViewModel @Inject constructor(
    private val repository: AscensionRepository,
    private val mentorUseCase: NeonMentorUseCase,
    private val skillRepository: SkillRepository
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

    fun prefill(attributeName: String?, title: String?, description: String?) {
        _uiState.update { state ->
            val attr = attributeName?.let { 
                try { SpecialType.valueOf(it) } catch(e: Exception) { null } 
            }
            state.copy(
                title = title ?: state.title,
                description = description ?: state.description,
                linkedAttributes = if (attr != null) state.linkedAttributes + attr else state.linkedAttributes,
                useAiMentor = true // Default to true as per spec
            )
        }
    }

    fun save() {
        val state = _uiState.value
        viewModelScope.launch {
            if (state.forgeType == ForgeType.DIRECTIVE) {
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
            } else {
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
    }
}
