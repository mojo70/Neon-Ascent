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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

data class SelfMapUiState(
    val isOnboardingSeen: Boolean = false,
    val isConversationalIntakeSeen: Boolean = false,
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
    val acceptedProposals: List<ProposedItemCandidate> = emptyList()
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

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<SelfMapUiState> = combine(
        repository.onboardingCompleted,
        repository.conversationalIntakeSeen,
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
        _acceptedProposals
    ) { flows: Array<Any?> ->
        val onboardingSeen = flows[0] as Boolean
        val intakeSeen = flows[1] as Boolean
        val items = flows[2] as List<SelfItemEntity>
        val loadPct = flows[3] as Float
        val cores = flows[4] as Int
        val avgImp = flows[5] as Float
        val bioLock = flows[6] as Boolean
        val reminderEnabled = flows[7] as Boolean
        val reminderCadence = flows[8] as Int
        val entries = flows[9] as List<EoEEntryEntity>
        val chronicleMsgs = flows[10] as List<ChronicleEntry>
        val latestMood = flows[11] as SelfMapMoodEntryEntity?
        val showMoodSheet = flows[12] as Boolean
        val moodEoeId = flows[13] as Long?
        val stepIdx = flows[14] as Int
        val isGen = flows[15] as Boolean
        val chatMsg = flows[16] as String
        val pendingProps = flows[17] as List<ProposedItemCandidate>
        val acceptedProps = flows[18] as List<ProposedItemCandidate>

        SelfMapUiState(
            isOnboardingSeen = onboardingSeen,
            isConversationalIntakeSeen = intakeSeen,
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
            acceptedProposals = acceptedProps
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SelfMapUiState()
    )

    init {
        viewModelScope.launch {
            repository.preseedDefaultItemsIfEmpty()
            initializeIntakeStep(0)
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
        }
    }

    fun finishConversationalIntake() {
        viewModelScope.launch {
            repository.setConversationalIntakeSeen(true)
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
