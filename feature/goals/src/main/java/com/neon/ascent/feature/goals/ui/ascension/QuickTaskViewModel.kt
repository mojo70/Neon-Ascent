package com.neon.ascent.feature.goals.ui.ascension

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.ai.GemmaClient
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.repository.AscensionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

data class QuickTaskAiSuggestion(
    val title: String = "",
    val description: String = "",
    val type: AscensionTaskType = AscensionTaskType.RECURRING, // mapping helper
    val recurrenceType: RecurrenceTypeV3 = RecurrenceTypeV3.DAILY,
    val timeWindow: String = "Anytime",
    val xpValue: Int = 10,
    val suggestedParentId: String? = null
)

@OptIn(FlowPreview::class)
@HiltViewModel
class QuickTaskViewModel @Inject constructor(
    private val repository: AscensionRepository,
    private val gemmaClient: GemmaClient
) : ViewModel() {

    val activeDirectives: StateFlow<List<AscensionDirective>> = repository.getAllDirectives()
        .map { dirs -> dirs.filter { it.status == DirectiveStatus.ACTIVE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMissions: StateFlow<List<AscensionMission>> = repository.getActiveMissions()
        .map { mis -> mis.filter { it.status == AscensionMissionStatus.ACTIVE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _flavorSuggestion = MutableStateFlow("INIT_NEURAL_LINK...")
    val flavorSuggestion: StateFlow<String> = _flavorSuggestion.asStateFlow()

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    private val _titleInput = MutableStateFlow("")
    val titleInput: StateFlow<String> = _titleInput.asStateFlow()

    init {
        // Handle real-time flavor suggestions based on title typing with debouncing
        viewModelScope.launch {
            _titleInput
                .debounce(600)
                .distinctUntilChanged()
                .collect { title ->
                    if (title.isBlank()) {
                        _flavorSuggestion.value = "READY_FOR_PROTOCOL_INPUT..."
                    } else {
                        generateFlavorSuggestion(title)
                    }
                }
        }
    }

    fun updateTitle(title: String) {
        _titleInput.value = title
    }

    private suspend fun generateFlavorSuggestion(title: String) {
        val deterministicCyberpunkSayings = listOf(
            "This protocol will accelerate your sync efficiency.",
            "A fine addition to your cognitive database.",
            "The Street will remember this upload.",
            "Arasaka won't know what hit them.",
            "Stabilizing neural connections under sector 7...",
            "High impact activity detected. Aligning AI core...",
            "Excellent choice to build your analog resilience."
        )

        try {
            val prompt = """
                [PROTOCOL: NEON_NARRATOR_FLAVOR]
                User is entering a task title: "$title"
                Respond with a single short, motivating cyberpunk-style comment/reaction (max 8 words).
                Keep it immersive, gritty, or netrunner-themed.
                OUTPUT:
            """.trimIndent()

            val response = gemmaClient.generateContent(prompt)
            val cleaned = response.substringAfter("OUTPUT:").trim().replace("\"", "")
            if (cleaned.isNotBlank() && !cleaned.startsWith("ERROR:")) {
                _flavorSuggestion.value = cleaned
            } else {
                _flavorSuggestion.value = deterministicCyberpunkSayings.random()
            }
        } catch (e: Exception) {
            _flavorSuggestion.value = deterministicCyberpunkSayings.random()
        }
    }

    suspend fun letNeonGuideFinishThis(currentTitle: String, currentDesc: String): QuickTaskAiSuggestion? {
        if (currentTitle.isBlank()) return null
        _isAiGenerating.value = true
        return try {
            val directivesContext = activeDirectives.value.joinToString { "[DIR_ID:${it.id}] ${it.title}" }
            val missionsContext = activeMissions.value.joinToString { "[MIS_ID:${it.id}] ${it.title}" }

            val prompt = """
                [PROTOCOL: HABIT_FORGE // ADHD_RUNNER]
                The user has typed a draft task:
                Title: "$currentTitle"
                Description: "$currentDesc"

                We have active goals in the system:
                Directives: $directivesContext
                Missions: $missionsContext

                Optimize this task and suggest standard values. Return your response in this exact format:
                SUGGESTED_TITLE: [Enhanced title, max 5 words]
                SUGGESTED_DESC: [One sentence clear description and success criteria]
                SUGGESTED_RECURRENCE: [DAILY / WEEKDAYS / ONE_TIME]
                SUGGESTED_TIME: [Morning / Midday / Evening / Anytime]
                SUGGESTED_XP: [5 / 10 / 15 / 25]
                SUGGESTED_PARENT_ID: [Directly select one DIR_ID or MIS_ID from above, or 'NONE' if standalone]
                OUTPUT:
            """.trimIndent()

            val response = gemmaClient.generateContent(prompt)
            val lines = response.lines()

            var title = currentTitle
            var desc = currentDesc
            var recurrence = RecurrenceTypeV3.DAILY
            var type = AscensionTaskType.RECURRING
            var time = "Anytime"
            var xp = 10
            var parentId: String? = null

            lines.forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("SUGGESTED_TITLE:") -> {
                        title = trimmed.substringAfter("SUGGESTED_TITLE:").trim()
                    }
                    trimmed.startsWith("SUGGESTED_DESC:") -> {
                        desc = trimmed.substringAfter("SUGGESTED_DESC:").trim()
                    }
                    trimmed.startsWith("SUGGESTED_RECURRENCE:") -> {
                        val recStr = trimmed.substringAfter("SUGGESTED_RECURRENCE:").trim().uppercase()
                        if (recStr == "ONE_TIME") {
                            type = AscensionTaskType.ONE_TIME
                        } else if (recStr == "WEEKDAYS") {
                            type = AscensionTaskType.RECURRING
                            recurrence = RecurrenceTypeV3.WEEKDAYS
                        } else {
                            type = AscensionTaskType.RECURRING
                            recurrence = RecurrenceTypeV3.DAILY
                        }
                    }
                    trimmed.startsWith("SUGGESTED_TIME:") -> {
                        time = trimmed.substringAfter("SUGGESTED_TIME:").trim()
                    }
                    trimmed.startsWith("SUGGESTED_XP:") -> {
                        xp = trimmed.substringAfter("SUGGESTED_XP:").trim().toIntOrNull() ?: 10
                    }
                    trimmed.startsWith("SUGGESTED_PARENT_ID:") -> {
                        val idStr = trimmed.substringAfter("SUGGESTED_PARENT_ID:").trim()
                        if (idStr != "NONE" && idStr.isNotBlank()) {
                            parentId = idStr
                        }
                    }
                }
            }

            QuickTaskAiSuggestion(
                title = title,
                description = desc,
                type = type,
                recurrenceType = recurrence,
                timeWindow = time,
                xpValue = xp,
                suggestedParentId = parentId
            )
        } catch (e: Exception) {
            null
        } finally {
            _isAiGenerating.value = false
        }
    }

    fun createTask(
        title: String,
        description: String,
        parentId: String?,
        isRecurring: Boolean,
        recurrenceType: RecurrenceTypeV3,
        timeWindows: List<String>,
        adaptiveWakeEnabled: Boolean,
        xpValue: Int,
        graceBufferDays: Int,
        tags: List<String>,
        userNotesTemplate: String?
    ) {
        viewModelScope.launch {
            val task = AscensionTask(
                id = UUID.randomUUID().toString(),
                parentId = parentId,
                title = title,
                description = description,
                type = if (isRecurring) AscensionTaskType.RECURRING else AscensionTaskType.ONE_TIME,
                recurrence = if (isRecurring) RecurrenceV3(type = recurrenceType) else null,
                timeWindows = timeWindows,
                adaptiveWakeEnabled = adaptiveWakeEnabled,
                reminderEnabled = true,
                xpValue = xpValue,
                currentStreak = 0,
                longestStreak = 0,
                graceBufferDays = graceBufferDays,
                lastCompleted = null,
                userNotesTemplate = userNotesTemplate
            )
            repository.insertTask(task)
            repository.insertNeuralLog(
                title = "New Protocol Initialized",
                content = "Task initialized successfully: $title [XP: $xpValue]",
                type = "PROTOCOL_INIT"
            )
        }
    }
}
