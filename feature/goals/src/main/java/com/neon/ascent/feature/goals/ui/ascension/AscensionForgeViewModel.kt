package com.neon.ascent.feature.goals.ui.ascension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.repository.SelfMapRepository
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.core.domain.repository.AscensionRepository
import com.neon.ascent.core.domain.repository.SkillRepository
import com.neon.ascent.core.domain.NeuralPingScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import com.neon.ascent.core.data.datastore.DirectivePreferencesDataStore
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import com.neon.ascent.feature.goals.domain.usecases.NeonMentorUseCase
import com.neon.ascent.core.common.DopamineCoordinator
import com.neon.ascent.core.common.DopamineEvent

enum class ForgeType { DIRECTIVE, MISSION, TASK }

data class AscensionForgeUiState(
    val forgeType: ForgeType = ForgeType.DIRECTIVE,
    val title: String = "",
    val description: String = "",
    // Intake mode state
    val directiveIntakeModePreference: DirectiveIntakeMode = DirectiveIntakeMode.MENTOR,
    val currentIntakeMode: DirectiveIntakeMode = DirectiveIntakeMode.MENTOR,
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
    val expandedMissions: Set<String> = emptySet(),
    val dopamineEvent: DopamineEvent? = null,
    val selfMapItems: List<SelfItemEntity> = emptyList(),
    val selectedAnchoredSelfItemId: Long? = null,
    val selectedAnchoredSelfItemName: String? = null,
    val targetEndDate: LocalDate? = null,
    val priorityTier: PriorityTier = PriorityTier.B,
    val outcomeStatement: String = "",
    val affirmation: String = "",
    val visualUri: String = "",
    // Task specific
    val taskType: AscensionTaskType = AscensionTaskType.ONE_TIME,
    val recurrenceType: RecurrenceTypeV3 = RecurrenceTypeV3.DAILY,
    val recurrenceDays: Set<DayOfWeek> = emptySet(),
    val timeWindows: List<String> = emptyList(),
    // Mission specific
    val parentDirectiveId: String? = null,
    val parentMissionId: String? = null,
    val isSuccess: Boolean = false,
    val deployedDirectiveId: String? = null,
    val isGeneratingSuggestions: Boolean = false,
    val errorMessage: String? = null,
    val availableDirectives: List<AscensionDirective> = emptyList(),
    val availableMissions: List<AscensionMission> = emptyList()
)

@HiltViewModel
class AscensionForgeViewModel @Inject constructor(
    private val repository: AscensionRepository,
    private val mentorUseCase: NeonMentorUseCase,
    private val skillRepository: SkillRepository,
    private val dopamineCoordinator: DopamineCoordinator,
    private val neuralPingScheduler: NeuralPingScheduler,
    private val selfMapRepository: SelfMapRepository,
    private val directivePreferencesDataStore: DirectivePreferencesDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AscensionForgeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val defaultMode = directivePreferencesDataStore.directiveIntakeMode.first()
            _uiState.update {
                it.copy(
                    directiveIntakeModePreference = defaultMode,
                    currentIntakeMode = defaultMode
                )
            }
        }
        viewModelScope.launch {
            // Ensure imported_quests directive exists if not already present
            val directives = repository.getAllDirectives().first()
            if (directives.none { it.id == "imported_quests" }) {
                val missions = repository.getMissionsForDirective("imported_quests").first()
                if (missions.isNotEmpty()) {
                    repository.insertDirective(
                        AscensionDirective(
                            id = "imported_quests",
                            title = "IMPORTED_QUESTS",
                            description = "Imported quests directive from legacy AppDatabase",
                            status = DirectiveStatus.ACTIVE
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            dopamineCoordinator.events.collect { event ->
                _uiState.update { it.copy(dopamineEvent = event) }
            }
        }
        viewModelScope.launch {
            selfMapRepository.activeItems.collect { items ->
                val sorted = items.sortedByDescending { it.importance }
                _uiState.update { it.copy(selfMapItems = sorted) }
            }
        }
        viewModelScope.launch {
            repository.getActiveDirectives().collect { directives ->
                _uiState.update { it.copy(availableDirectives = directives) }
            }
        }
        viewModelScope.launch {
            repository.getActiveMissions().collect { missions ->
                _uiState.update { it.copy(availableMissions = missions) }
            }
        }
    }

    fun clearDopamineEvent() = _uiState.update { it.copy(dopamineEvent = null) }
    fun clearErrorMessage() = _uiState.update { it.copy(errorMessage = null) }

    fun toggleInSessionIntakeMode() {
        _uiState.update { state ->
            val nextMode = if (state.currentIntakeMode == DirectiveIntakeMode.MENTOR) {
                DirectiveIntakeMode.FORM
            } else {
                DirectiveIntakeMode.MENTOR
            }
            state.copy(currentIntakeMode = nextMode)
        }
    }

    fun saveDefaultIntakeModePreference(mode: DirectiveIntakeMode) {
        viewModelScope.launch {
            directivePreferencesDataStore.setDirectiveIntakeMode(mode)
            _uiState.update { it.copy(directiveIntakeModePreference = mode) }
        }
    }

    fun updateType(type: ForgeType) = _uiState.update { it.copy(forgeType = type) }
    fun updateTitle(title: String) = _uiState.update { it.copy(title = title) }
    fun updateDescription(desc: String) = _uiState.update { it.copy(description = desc) }
    fun updateVisionStatement(vision: String) = _uiState.update { it.copy(visionStatement = vision) }
    fun updateIsQuarterly(quarterly: Boolean) = _uiState.update { it.copy(isQuarterly = quarterly) }
    fun updateSelectedArchetype(archetype: String?) = _uiState.update { it.copy(selectedArchetype = archetype) }
    fun updateAiMentorMode(mode: MentorMode) = _uiState.update { it.copy(aiMentorMode = mode) }
    fun updateSelectedSkill(skill: String?) = _uiState.update { it.copy(selectedSkill = skill) }
    fun updateMentorInput(input: String) = _uiState.update { it.copy(mentorInput = input) }
    fun updateTargetEndDate(date: LocalDate?) = _uiState.update { it.copy(targetEndDate = date) }
    fun updatePriorityTier(tier: PriorityTier) = _uiState.update { it.copy(priorityTier = tier) }
    fun updateOutcomeStatement(statement: String) = _uiState.update { it.copy(outcomeStatement = statement) }
    fun updateAffirmation(affirmation: String) = _uiState.update { it.copy(affirmation = affirmation) }
    fun updateVisualUri(uri: String?) = _uiState.update { it.copy(visualUri = uri ?: "") }

    fun selectAnchoredSelfItem(item: SelfItemEntity?) {
        _uiState.update {
            it.copy(
                selectedAnchoredSelfItemId = item?.id,
                selectedAnchoredSelfItemName = item?.name
            )
        }
    }
    
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
    fun updateParentDirective(id: String?) = _uiState.update { state ->
        val currentMissionStillValid = state.parentMissionId?.let { mId ->
            state.availableMissions.find { it.id == mId }?.directiveId == id
        } ?: true
        state.copy(
            parentDirectiveId = id,
            parentMissionId = if (currentMissionStillValid) state.parentMissionId else null
        )
    }

    fun updateParentMission(id: String?) = _uiState.update { it.copy(parentMissionId = id) }

    fun toggleMissionExpansion(missionTitle: String) = _uiState.update { state ->
        val newExpanded = if (state.expandedMissions.contains(missionTitle)) {
            state.expandedMissions - missionTitle
        } else {
            state.expandedMissions + missionTitle
        }
        state.copy(expandedMissions = newExpanded)
    }

    fun prefill(attributeName: String?, title: String?, description: String?, vision: String? = null, biometricContext: String? = null) {
        _uiState.update { state ->
            val attr = attributeName?.let { 
                try { SpecialType.valueOf(it) } catch(e: Exception) { null } 
            }
            val matchedAnchor = state.selfMapItems.firstOrNull { item ->
                (title != null && title.contains(item.name, ignoreCase = true)) ||
                (description != null && description.contains(item.name, ignoreCase = true))
            }
            state.copy(
                title = title ?: state.title,
                description = description ?: state.description,
                visionStatement = vision ?: state.visionStatement,
                linkedAttributes = if (attr != null && !state.linkedAttributes.contains(attr)) state.linkedAttributes + attr else state.linkedAttributes,
                useAiMentor = true,
                isFromInsight = true,
                biometricContext = biometricContext,
                selectedAnchoredSelfItemId = matchedAnchor?.id ?: state.selectedAnchoredSelfItemId,
                selectedAnchoredSelfItemName = matchedAnchor?.name ?: state.selectedAnchoredSelfItemName
            )
        }
        
        if (title != null || description != null) {
            ensureMentorConversationStarted()
        }
    }

    fun ensureMentorConversationStarted() {
        val state = _uiState.value
        if (state.chatHistory.isNotEmpty() || state.isGenerating) return

        val initialPrompt = """
[INTAKE_GENESIS_INIT]
Context Title: ${if (state.title.isNotBlank()) state.title else "Unspecified"}
Context Description: ${if (state.description.isNotBlank()) state.description else "Unspecified"}
Biometric Context: ${state.biometricContext ?: "None"}

Operator is initiating a new Directive intake.
Mandate:
1. Ask 1-2 sharp clarifying questions covering:
   - WHAT specific outcome is targeted?
   - BY WHEN is the target completion date or horizon?
   - WHY it matters (suggesting anchoring to a Self Map item or linked S.P.E.C.I.A.L. stats).
2. DO NOT output any [PROPOSAL] or [METRICS] section in this first turn. Keep it strictly conversational and dialectic.
""".trimIndent()

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val response = mentorUseCase.getMentorDialogue(
                    directive = AscensionDirective(
                        id = "",
                        title = state.title.ifBlank { "DIRECTIVE_INTAKE" },
                        description = state.description
                    ),
                    missions = emptyList(),
                    tasks = emptyList(),
                    mode = MentorMode.GUIDE,
                    message = initialPrompt
                )
                val finalMsg = if (response.text.isBlank()) {
                    MentorUiMessage(
                        text = "CYBR-TES // INTAKE PROTOCOL ONLINE.\n\n1. WHAT specific outcome are you forging?\n2. BY WHEN do you target completion?\n3. WHY does this matter (Self Map anchor or S.P.E.C.I.A.L. stat link)?",
                        isFromUser = false
                    )
                } else response

                _uiState.update {
                    it.copy(
                        chatHistory = it.chatHistory + finalMsg,
                        isGenerating = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        chatHistory = it.chatHistory + MentorUiMessage(
                            text = "CYBR-TES // INTAKE PROTOCOL ONLINE.\n\n1. WHAT specific outcome are you forging?\n2. BY WHEN do you target completion?\n3. WHY does this matter (Self Map anchor or S.P.E.C.I.A.L. stat link)?",
                            isFromUser = false
                        ),
                        isGenerating = false
                    )
                }
            }
        }
    }

    private fun startInitialConversation() {
        ensureMentorConversationStarted()
    }

    fun sendMentorMessage() {
        val input = _uiState.value.mentorInput
        if (input.isBlank()) return
        sendMessage(input)
    }

    fun regenerateProposals() {
        sendMessage("REGENERATE_PROTOCOL: Provide more granular detail and alternative missions based on my profile.")
    }

    fun askClarification() {
        sendMessage("CLARIFY_PROTOCOL: Ask me 2-3 specific questions to better understand my constraints and vision before proceeding.")
    }

    fun requestMentorFormSuggestions() {
        val state = _uiState.value
        _uiState.update { it.copy(isGeneratingSuggestions = true) }

        viewModelScope.launch {
            try {
                val prompt = """
[FORM_ASSIST_REQUEST]
Directive Title: ${state.title.ifBlank { "Unspecified" }}
Directive Description: ${state.description.ifBlank { "Unspecified" }}

Task: Suggest a single quantified Outcome Statement and a single daily Affirmation for this Directive.
Format response strictly as:
[OUTCOME]
Your suggested outcome statement here.

[AFFIRMATION]
Your suggested affirmation statement here.
""".trimIndent()

                val dialogue = mentorUseCase.getMentorDialogue(
                    directive = AscensionDirective(id = "", title = state.title, description = state.description),
                    missions = emptyList(),
                    tasks = emptyList(),
                    mode = MentorMode.GUIDE,
                    message = prompt
                )

                val text = dialogue.text
                val outcome = text.substringAfter("[OUTCOME]", "").substringBefore("[AFFIRMATION]").trim()
                val affirmation = text.substringAfter("[AFFIRMATION]", "").trim()

                _uiState.update { current ->
                    current.copy(
                        outcomeStatement = if (outcome.isNotBlank()) outcome else current.outcomeStatement,
                        affirmation = if (affirmation.isNotBlank()) affirmation else current.affirmation,
                        isGeneratingSuggestions = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGeneratingSuggestions = false) }
            }
        }
    }

    private fun sendMessage(input: String) {
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
                    chatHistory = it.chatHistory + response,
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
                var deployedId: String? = null
                when (state.forgeType) {
                    ForgeType.DIRECTIVE -> {
                        val directiveId = UUID.randomUUID().toString()
                        deployedId = directiveId
                        val directiveTitle = state.title.ifBlank { "NEW_DIRECTIVE_${System.currentTimeMillis() % 10000}" }
                        val directive = AscensionDirective(
                            id = directiveId,
                            title = directiveTitle,
                            description = state.description,
                            visionStatement = state.visionStatement.takeIf { it.isNotBlank() },
                            isQuarterly = state.isQuarterly,
                            linkedAttributes = state.linkedAttributes,
                            createdAt = Instant.now(),
                            successMetrics = emptyList(),
                            aiMentorMode = state.aiMentorMode,
                            archetypeTag = state.selectedArchetype,
                            anchoredSelfItemId = state.selectedAnchoredSelfItemId,
                            anchoredSelfItemName = state.selectedAnchoredSelfItemName,
                            targetEndDate = state.targetEndDate,
                            priorityTier = state.priorityTier,
                            outcomeStatement = state.outcomeStatement.takeIf { it.isNotBlank() },
                            affirmation = state.affirmation.takeIf { it.isNotBlank() },
                            visualUri = state.visualUri.takeIf { it.isNotBlank() }
                        )
                        repository.insertDirective(directive)
                    }
                    ForgeType.MISSION -> {
                        val mission = AscensionMission(
                            id = UUID.randomUUID().toString(),
                            directiveId = state.parentDirectiveId,
                            title = state.title,
                            description = state.description,
                            linkedAttributes = state.linkedAttributes,
                            createdAt = Instant.now(),
                            contributionWeight = 1.0f,
                            anchoredSelfItemId = state.selectedAnchoredSelfItemId,
                            anchoredSelfItemName = state.selectedAnchoredSelfItemName
                        )
                        repository.insertMission(mission)
                    }
                    ForgeType.TASK -> {
                        val effectiveParentId = state.parentMissionId ?: state.parentDirectiveId
                        val task = AscensionTask(
                            id = UUID.randomUUID().toString(),
                            parentId = effectiveParentId,
                            title = state.title,
                            description = state.description,
                            type = state.taskType,
                            isPulse = true,
                            recurrence = if (state.taskType == AscensionTaskType.RECURRING) {
                                RecurrenceV3(
                                    type = state.recurrenceType,
                                    daysOfWeek = state.recurrenceDays
                                )
                            } else null,
                            timeWindows = state.timeWindows,
                            linkedAttributes = state.linkedAttributes,
                            impactWeight = 1.0f,
                            xpValue = 20
                        )
                        repository.insertTask(task)
                    }
                }
                _uiState.update { it.copy(isSuccess = true, deployedDirectiveId = deployedId, errorMessage = null) }
                dopamineCoordinator.triggerAscension(
                    title = "PROTOCOL_DEPLOYED",
                    message = "${state.title.ifBlank { "NEW_DIRECTIVE" }.uppercase()} INTEGRATED",
                    xp = 50,
                    actionLabel = "View in Dashboard"
                )
                neuralPingScheduler.scheduleSmartPings()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSuccess = false, 
                        errorMessage = "DEPLOYMENT_FAILED: ${e.message ?: "Unknown database error"}"
                    ) 
                }
            }
        }
    }

    fun acceptProposals(proposedMissions: List<ProposedMission>, proposedMetrics: List<SuccessMetric>) {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                val directiveId = UUID.randomUUID().toString()
                val directiveTitle = state.title.ifBlank {
                    proposedMissions.firstOrNull()?.title?.let { "DIRECTIVE // $it" } ?: "NEW_DIRECTIVE_${System.currentTimeMillis() % 10000}"
                }
                val directive = AscensionDirective(
                    id = directiveId,
                    title = directiveTitle,
                    description = state.description,
                    visionStatement = state.visionStatement.takeIf { it.isNotBlank() },
                    isQuarterly = state.isQuarterly,
                    linkedAttributes = state.linkedAttributes,
                    createdAt = Instant.now(),
                    successMetrics = proposedMetrics,
                    aiGenerated = true,
                    archetypeTag = state.selectedArchetype,
                    aiMentorMode = state.aiMentorMode,
                    anchoredSelfItemId = state.selectedAnchoredSelfItemId,
                    anchoredSelfItemName = state.selectedAnchoredSelfItemName,
                    targetEndDate = state.targetEndDate,
                    priorityTier = state.priorityTier,
                    outcomeStatement = state.outcomeStatement.takeIf { it.isNotBlank() },
                    affirmation = state.affirmation.takeIf { it.isNotBlank() },
                    visualUri = state.visualUri.takeIf { it.isNotBlank() }
                )
                repository.insertDirective(directive)

                proposedMissions.forEach { pMission ->
                    val missionId = UUID.randomUUID().toString()
                    val mission = AscensionMission(
                        id = missionId,
                        directiveId = directiveId,
                        title = pMission.title,
                        description = pMission.description,
                        aiGenerated = true,
                        contributionWeight = pMission.contributionWeight,
                        linkedAttributes = pMission.linkedAttributes,
                        anchoredSelfItemId = state.selectedAnchoredSelfItemId,
                        anchoredSelfItemName = state.selectedAnchoredSelfItemName
                    )
                    repository.insertMission(mission)
                    
                    pMission.tasks.forEach { pTask ->
                        val task = AscensionTask(
                            id = UUID.randomUUID().toString(),
                            parentId = missionId,
                            title = pTask.title,
                            description = pTask.description,
                            type = pTask.type,
                            isPulse = pTask.isPulse,
                            recurrence = pTask.recurrence,
                            timeWindows = pTask.timeWindows,
                            linkedAttributes = pTask.linkedAttributes,
                            impactWeight = pTask.impactWeight,
                            xpValue = if (pTask.isPulse) 15 else 10
                        )
                        repository.insertTask(task)
                    }
                }

                runSoftDensityCheck()

                _uiState.update { it.copy(isSuccess = true, deployedDirectiveId = directiveId, errorMessage = null) }
                dopamineCoordinator.triggerAscension(
                    title = "PROTOCOL_DEPLOYED",
                    message = "${directiveTitle.uppercase()} INTEGRATED",
                    xp = 100,
                    actionLabel = "View in Dashboard"
                )
                neuralPingScheduler.scheduleSmartPings()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isSuccess = false, 
                        deployedDirectiveId = null,
                        errorMessage = "CASCADE_DEPLOY_FAILED: ${e.message ?: "Unknown database error"}"
                    ) 
                }
            }
        }
    }

    private suspend fun runSoftDensityCheck() {
        try {
            repository.getActiveDirectives().first()
        } catch (_: Exception) {
            // Non-blocking
        }
    }
}
