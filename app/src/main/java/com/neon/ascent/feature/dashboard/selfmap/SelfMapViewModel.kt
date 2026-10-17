package com.neon.ascent.feature.dashboard.selfmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.data.local.entity.EoEEntryEntity
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.local.entity.SelfMapMoodEntryEntity
import com.neon.ascent.core.data.repository.SelfMapRepository
import com.neon.ascent.core.domain.chronicle.ChronicleEntry
import com.neon.ascent.core.domain.chronicle.ChronicleRepository
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import com.neon.ascent.core.domain.selfmap.SelfMapIntakeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

data class SelfMapUiState(
    val isOnboardingSeen: Boolean = false,
    val isConversationalIntakeSeen: Boolean = false,
    val intakeModePreference: SelfMapIntakeMode = SelfMapIntakeMode.CHAT,
    val activeSessionIntakeMode: SelfMapIntakeMode = SelfMapIntakeMode.CHAT,
    val isLoading: Boolean = true,
    val activeItems: List<SelfItemEntity> = emptyList(),
    val coreAttachmentLoad: Float = 100f,
    val coreCount: Int = 0,
    val avgImportance: Float = 0f,
    val biometricLockEnabled: Boolean = false,
    val reviewReminderEnabled: Boolean = false,
    val reviewReminderCadenceDays: Int = 14,
    val eoeEntries: List<EoEEntryEntity> = emptyList(),
    val chronicleEntries: List<ChronicleEntry> = emptyList(),
    val latestMoodEntry: SelfMapMoodEntryEntity? = null,
    val showMoodCheckInSheet: Boolean = false,
    val pendingMoodEoeId: Long? = null,
    val currentIntakeStepIndex: Int = 0,
    val isGeneratingIntakeResponse: Boolean = false,
    val intakeChatMessage: String = "",
    val pendingProposals: List<ProposedItemCandidate> = emptyList(),
    val acceptedProposals: List<ProposedItemCandidate> = emptyList(),
    val draftName: String = "",
    val draftCategory: SelfMapCategory = SelfMapCategory.BODY_SENSES,
    val draftImportance: Float = 0.7f,
    val draftNotes: String = "",
    val isSuggestingFromMentor: Boolean = false
)

@HiltViewModel
class SelfMapViewModel @Inject constructor(
    private val repository: SelfMapRepository,
    private val intakeUseCase: SelfMapConversationalIntakeUseCase,
    private val chronicleRepository: ChronicleRepository
) : ViewModel() {

    private val _intakeStepIndex = MutableStateFlow(0)
    private val _isGeneratingIntake = MutableStateFlow(false)
    private val _intakeChatMessage = MutableStateFlow("")
    private val _pendingProposals = MutableStateFlow<List<ProposedItemCandidate>>(emptyList())
    private val _acceptedProposals = MutableStateFlow<List<ProposedItemCandidate>>(emptyList())
    private val _showMoodCheckInSheet = MutableStateFlow(false)
    private val _pendingMoodEoeId = MutableStateFlow<Long?>(null)
    private val _sessionIntakeMode = MutableStateFlow<SelfMapIntakeMode?>(null)
    private val _draftName = MutableStateFlow("")
    private val _draftCategory = MutableStateFlow(SelfMapCategory.BODY_SENSES)
    private val _draftImportance = MutableStateFlow(0.7f)
    private val _draftNotes = MutableStateFlow("")
    private val _isSuggestingFromMentor = MutableStateFlow(false)

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<SelfMapUiState> = combine(
        repository.onboardingCompleted,
        repository.conversationalIntakeSeen,
        repository.selfMapIntakeMode,
        _sessionIntakeMode,
        repository.activeItems,
        repository.coreAttachmentLoadPercentage,
        repository.coreCount,
        repository.averageImportance,
        repository.biometricLockEnabled,
        repository.reviewReminderEnabled,
        repository.reviewReminderCadenceDays,
        repository.getAllEoEEntries(),
        chronicleRepository.observeChronicle(),
        repository.latestMoodEntry,
        _showMoodCheckInSheet,
        _pendingMoodEoeId,
        _intakeStepIndex,
        _isGeneratingIntake,
        _intakeChatMessage,
        _pendingProposals,
        _acceptedProposals,
        _draftName,
        _draftCategory,
        _draftImportance,
        _draftNotes,
        _isSuggestingFromMentor
    ) { flows: Array<Any?> ->
        val onboardingSeen = flows[0] as Boolean
        val intakeSeen = flows[1] as Boolean
        val intakeModePref = flows[2] as SelfMapIntakeMode
        val sessionModeOverride = flows[3] as SelfMapIntakeMode?
        val items = flows[4] as List<SelfItemEntity>
        val loadPct = flows[5] as Float
        val cores = flows[6] as Int
        val avgImp = flows[7] as Float
        val bioLock = flows[8] as Boolean
        val reminderEnabled = flows[9] as Boolean
        val reminderCadence = flows[10] as Int
        val entries = flows[11] as List<EoEEntryEntity>
        val chronicleMsgs = flows[12] as List<ChronicleEntry>
        val latestMood = flows[13] as SelfMapMoodEntryEntity?
        val showMoodSheet = flows[14] as Boolean
        val moodEoeId = flows[15] as Long?
        val stepIdx = flows[16] as Int
        val isGen = flows[17] as Boolean
        val chatMsg = flows[18] as String
        val pendingProps = flows[19] as List<ProposedItemCandidate>
        val acceptedProps = flows[20] as List<ProposedItemCandidate>
        val dName = flows[21] as String
        val dCat = flows[22] as SelfMapCategory
        val dImp = flows[23] as Float
        val dNotes = flows[24] as String
        val isSuggesting = flows[25] as Boolean

        SelfMapUiState(
            isOnboardingSeen = onboardingSeen,
            isConversationalIntakeSeen = intakeSeen,
            intakeModePreference = intakeModePref,
            activeSessionIntakeMode = sessionModeOverride ?: intakeModePref,
            isLoading = false,
            activeItems = items,
            coreAttachmentLoad = loadPct,
            coreCount = cores,
            avgImportance = avgImp,
            biometricLockEnabled = bioLock,
            reviewReminderEnabled = reminderEnabled,
            reviewReminderCadenceDays = reminderCadence,
            eoeEntries = entries,
            chronicleEntries = chronicleMsgs,
            latestMoodEntry = latestMood,
            showMoodCheckInSheet = showMoodSheet,
            pendingMoodEoeId = moodEoeId,
            currentIntakeStepIndex = stepIdx,
            isGeneratingIntakeResponse = isGen,
            intakeChatMessage = chatMsg,
            pendingProposals = pendingProps,
            acceptedProposals = acceptedProps,
            draftName = dName,
            draftCategory = dCat,
            draftImportance = dImp,
            draftNotes = dNotes,
            isSuggestingFromMentor = isSuggesting
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SelfMapUiState()
    )

    init {
        viewModelScope.launch {
            repository.preseedDefaultItemsIfEmpty()
            val savedStep = repository.intakeStepIndex.first()
            initializeIntakeStep(savedStep)
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(true)
            repository.preseedDefaultItemsIfEmpty()
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(true)
            repository.preseedDefaultItemsIfEmpty()
        }
    }

    // --- Conversational Intake Flow ---

    fun initializeIntakeStep(stepIndex: Int) {
        viewModelScope.launch {
            _intakeStepIndex.value = stepIndex
            repository.setIntakeStepIndex(stepIndex)
            _isGeneratingIntake.value = true
            val (msg, proposals) = intakeUseCase.processUserTurn(stepIndex, "")
            _intakeChatMessage.value = msg
            _pendingProposals.value = proposals
            _isGeneratingIntake.value = false
        }
    }

    fun sendConversationalTurn(userMessage: String) {
        val currentStep = _intakeStepIndex.value
        viewModelScope.launch {
            _isGeneratingIntake.value = true
            val (msg, proposals) = intakeUseCase.processUserTurn(currentStep, userMessage)
            _intakeChatMessage.value = msg
            _pendingProposals.value = proposals
            _isGeneratingIntake.value = false
        }
    }

    fun acceptProposal(
        proposalId: String,
        editedName: String? = null,
        editedCategory: SelfMapCategory? = null,
        editedImportance: Float? = null,
        editedNote: String? = null
    ) {
        val candidate = _pendingProposals.value.firstOrNull { it.id == proposalId } ?: return
        val finalName = editedName ?: candidate.name
        val finalCat = editedCategory ?: candidate.category
        val finalImp = (editedImportance ?: candidate.suggestedImportance).coerceIn(0.1f, 1.0f)
        val finalNote = editedNote ?: candidate.note

        viewModelScope.launch {
            val entity = SelfItemEntity(
                name = finalName,
                category = finalCat,
                importance = finalImp,
                notes = finalNote
            )
            repository.insertSelfItem(entity)

            val updatedCandidate = candidate.copy(
                name = finalName,
                category = finalCat,
                suggestedImportance = finalImp,
                note = finalNote,
                isAccepted = true,
                isRejected = false
            )

            _pendingProposals.update { list ->
                list.map { if (it.id == proposalId) updatedCandidate else it }
            }
            _acceptedProposals.update { list ->
                list.filterNot { it.id == proposalId } + updatedCandidate
            }
        }
    }

    fun rejectProposal(proposalId: String) {
        _pendingProposals.update { list ->
            list.map { if (it.id == proposalId) it.copy(isRejected = true, isAccepted = false) else it }
        }
    }

    fun nextIntakeStep() {
        val nextStep = _intakeStepIndex.value + 1
        if (nextStep < intakeUseCase.intakeSteps.size) {
            initializeIntakeStep(nextStep)
        } else {
            finishConversationalIntake()
        }
    }

    fun skipConversationalIntake() {
        viewModelScope.launch {
            repository.setConversationalIntakeSeen(true)
            repository.setIntakeStepIndex(0)
        }
    }

    fun finishConversationalIntake() {
        viewModelScope.launch {
            repository.setConversationalIntakeSeen(true)
            repository.setIntakeStepIndex(0)
        }
    }

    /**
     * Toggles or sets the active in-session intake mode (CHAT vs FORM).
     * Note: In-session mode switching does NOT write the preference to DataStore
     * unless explicitly saved via Settings.
     */
    fun switchSessionIntakeMode(mode: SelfMapIntakeMode) {
        _sessionIntakeMode.value = mode
    }

    /**
     * Updates in-memory draft form inputs so state is preserved across mode switches.
     */
    fun updateDraftForm(
        name: String? = null,
        category: SelfMapCategory? = null,
        importance: Float? = null,
        notes: String? = null
    ) {
        if (name != null) _draftName.value = name
        if (category != null) _draftCategory.value = category
        if (importance != null) _draftImportance.value = importance
        if (notes != null) _draftNotes.value = notes
    }

    /**
     * Queries the AI guide for a quick recommendation for the specified or draft category
     * without forcing a multi-turn chat sequence.
     */
    fun askMentorForSuggestion(category: SelfMapCategory? = null) {
        viewModelScope.launch {
            _isSuggestingFromMentor.value = true
            val targetCat = category ?: _draftCategory.value
            val suggestion = intakeUseCase.suggestItemForCategory(targetCat)
            _draftName.value = suggestion.name
            _draftCategory.value = suggestion.category
            _draftImportance.value = suggestion.suggestedImportance
            if (!suggestion.note.isNullOrBlank()) {
                _draftNotes.value = suggestion.note
            }
            _isSuggestingFromMentor.value = false
        }
    }

    /**
     * Saves the draft item directly into SelfMapRepository and clears draft form state.
     */
    fun saveDraftFormItem() {
        val name = _draftName.value.trim()
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertSelfItem(
                SelfItemEntity(
                    name = name,
                    category = _draftCategory.value,
                    importance = _draftImportance.value.coerceIn(0.0f, 1.0f),
                    notes = _draftNotes.value.trim().ifEmpty { null }
                )
            )
            _draftName.value = ""
            _draftImportance.value = 0.7f
            _draftNotes.value = ""
        }
    }

    /**
     * Persists the user's preferred default intake mode (CHAT vs FORM) to DataStore Settings.
     */
    fun setSelfMapIntakeModePreference(mode: SelfMapIntakeMode) {
        viewModelScope.launch {
            repository.setSelfMapIntakeMode(mode)
        }
    }

    // --- Mood Check-In ---

    fun triggerMoodCheckIn(eoeId: Long? = null) {
        _pendingMoodEoeId.value = eoeId
        _showMoodCheckInSheet.value = true
    }

    fun dismissMoodCheckIn() {
        _showMoodCheckInSheet.value = false
        _pendingMoodEoeId.value = null
    }

    fun logMood(score: Int, note: String? = null, eoeId: Long? = null) {
        viewModelScope.launch {
            repository.insertMoodEntry(
                score = score.coerceIn(1, 10),
                note = note,
                eoeId = eoeId ?: _pendingMoodEoeId.value
            )
            dismissMoodCheckIn()
        }
    }

    // --- Linked Chronicle Entries ---

    fun saveLinkedChronicleEntry(content: String, selfItemId: Long) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val entry = ChronicleEntry(
                source = "journal",
                sourceId = UUID.randomUUID().toString(),
                content = content.trim(),
                selfItemId = selfItemId
            )
            chronicleRepository.saveEntry(entry)
        }
    }

    // --- Review Reminders Settings ---

    fun setReviewReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setReviewReminderEnabled(enabled)
        }
    }

    fun setReviewReminderCadenceDays(days: Int) {
        viewModelScope.launch {
            repository.setReviewReminderCadenceDays(days)
        }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricLockEnabled(enabled)
        }
    }

    fun addItem(name: String, category: SelfMapCategory, importance: Float, notes: String?) {
        viewModelScope.launch {
            repository.insertSelfItem(
                SelfItemEntity(
                    name = name,
                    category = category,
                    importance = importance.coerceIn(0.0f, 1.0f),
                    notes = notes
                )
            )
        }
    }

    fun updateItem(item: SelfItemEntity, previousImportance: Float? = null) {
        viewModelScope.launch {
            repository.updateSelfItem(item)
            if (previousImportance != null && abs(item.importance - previousImportance) >= 0.25f) {
                triggerMoodCheckIn()
            }
        }
    }

    fun archiveItem(id: Long) {
        viewModelScope.launch {
            repository.archiveSelfItem(id)
            triggerMoodCheckIn()
        }
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            repository.deleteSelfItem(id)
        }
    }

    fun logEoEReaction(
        selfItemId: Long,
        emotionLabel: String,
        epText: String,
        perceptionText: String,
        appraisal: String?,
        intensity: Int?,
        canChangeNotes: String?
    ) {
        viewModelScope.launch {
            val eoeId = repository.insertEoEEntry(
                EoEEntryEntity(
                    selfItemId = selfItemId,
                    emotionLabel = emotionLabel,
                    epText = epText,
                    perceptionText = perceptionText,
                    appraisal = appraisal,
                    intensity = intensity,
                    canChangeNotes = canChangeNotes
                )
            )
            triggerMoodCheckIn(eoeId = eoeId)
        }
    }

    suspend fun exportDataAsJson(): String {
        return repository.exportDataAsJson()
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
        }
    }
}
