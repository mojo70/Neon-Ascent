package com.neon.ascent.feature.dashboard.selfmap.intake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.repository.SelfMapIntakeSession
import com.neon.ascent.core.data.repository.SelfMapRepository
import com.neon.ascent.feature.dashboard.selfmap.ChatTurnMessage
import com.neon.ascent.feature.dashboard.selfmap.MessageSender
import com.neon.ascent.feature.dashboard.selfmap.ProposedItemCandidate
import com.neon.ascent.feature.dashboard.selfmap.SelfMapConversationalIntakeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class IntakeUiState(
    val isLoading: Boolean = true,
    val currentStepIndex: Int = 0,
    val messages: List<ChatTurnMessage> = emptyList(),
    val activeItems: List<SelfItemEntity> = emptyList(),
    val acceptedIds: Set<String> = emptySet(),
    val rejectedIds: Set<String> = emptySet(),
    val dismissedIds: Set<String> = emptySet(),
    val stepAnchor: Int = 0,
    val isCompleted: Boolean = false
)

@HiltViewModel
class SelfMapIntakeViewModel @Inject constructor(
    private val repository: SelfMapRepository,
    private val intakeUseCase: SelfMapConversationalIntakeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(IntakeUiState())
    val uiState: StateFlow<IntakeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val context = repository.loadIntakeContext()
            val session = repository.loadIntakeSession()
            val restored = IntakeResume.restore(session, intakeUseCase.intakeSteps)

            if (restored != null && restored.messages.isNotEmpty()) {
                _uiState.value = restored.copy(activeItems = context.activeItems)
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentStepIndex = session.stepIndex.coerceIn(0, intakeUseCase.intakeSteps.lastIndex),
                        activeItems = context.activeItems,
                        acceptedIds = session.acceptedItemIds,
                        rejectedIds = session.rejectedSignatures,
                        dismissedIds = session.dismissedIds,
                        stepAnchor = session.stepAnchor,
                    )
                }
                if (_uiState.value.messages.isEmpty()) {
                    askCurrentStepQuestion()
                }
            }
            persistSession()
        }
        viewModelScope.launch {
            repository.mapCleared.collect {
                val context = repository.loadIntakeContext()
                _uiState.value = IntakeUiState(isLoading = false, activeItems = context.activeItems)
                askCurrentStepQuestion()
                persistSession()
            }
        }
    }

    /** Guided start reopens the saved thread instead of bouncing off a finished flag. */
    fun resumeForEntry() {
        _uiState.update { it.copy(isCompleted = false) }
    }

    private fun askCurrentStepQuestion() {
        val stepIndex = _uiState.value.currentStepIndex
        if (stepIndex >= intakeUseCase.intakeSteps.size) {
            completeIntake()
            return
        }

        val step = intakeUseCase.intakeSteps[stepIndex]
        val question = ChatTurnMessage(
            sender = MessageSender.GUIDE,
            text = step.promptQuestion,
        )
        _uiState.update { state ->
            val messages = state.messages + question
            state.copy(messages = messages, stepAnchor = messages.lastIndex)
        }
    }

    fun submitUserReply(reply: String) {
        if (reply.isBlank()) return
        if (isDonePhrase(reply)) {
            val userMsg = ChatTurnMessage(sender = MessageSender.OPERATOR, text = reply.trim())
            _uiState.update { it.copy(messages = it.messages + userMsg) }
            completeIntake()
            return
        }

        val userMsg = ChatTurnMessage(sender = MessageSender.OPERATOR, text = reply)
        _uiState.update { it.copy(messages = it.messages + userMsg) }

        viewModelScope.launch {
            val trimmed = reply.trim().lowercase().trimEnd('.')
            if (trimmed in setOf("skip", "none", "nothing", "idk", "pass", "no")) {
                _uiState.update { state ->
                    IntakeAutoAdvance.toStep(state, intakeUseCase.intakeSteps, state.currentStepIndex + 1)
                }
                finishIfNeeded()
                persistSession()
                return@launch
            }

            val stepIndex = _uiState.value.currentStepIndex
            val context = repository.loadIntakeContext()
            _uiState.update { it.copy(activeItems = context.activeItems) }

            val (guideText, rawProposals) = intakeUseCase.processUserTurn(stepIndex, reply, context.activeItems)

            val filteredProposals = rawProposals.filter { proposal ->
                actionable(proposal)
            }

            val guideMsg = ChatTurnMessage(
                sender = MessageSender.GUIDE,
                text = guideText,
                proposals = filteredProposals
            )
            _uiState.update { state ->
                IntakeAutoAdvance.afterResolution(
                    state.copy(messages = state.messages + guideMsg),
                    intakeUseCase.intakeSteps,
                )
            }
            finishIfNeeded()
            persistSession()
        }
    }

    fun acceptProposal(proposal: ProposedItemCandidate) {
        viewModelScope.launch {
            if (!actionable(proposal)) return@launch
            val item = SelfItemEntity(
                name = proposal.name,
                category = proposal.category,
                importance = proposal.suggestedImportance,
                notes = proposal.note,
            )
            val insertId = repository.insertSelfItem(item)
            val state = _uiState.value
            val newAccepted = state.acceptedIds + proposal.id + insertId.toString() + IntakeProposalResolution.signature(proposal)
            val context = repository.loadIntakeContext()

            _uiState.update { state ->
                IntakeAutoAdvance.afterResolution(
                    state.copy(
                        acceptedIds = newAccepted,
                        activeItems = context.activeItems,
                        messages = state.messages + ChatTurnMessage(
                            sender = MessageSender.GUIDE,
                            text = "Added ${proposal.name}.",
                        ),
                    ),
                    intakeUseCase.intakeSteps,
                )
            }
            finishIfNeeded()
            persistSession()
        }
    }

    /**
     * Leaves the card and opens the form. The form save is the insert.
     */
    fun dismissProposal(proposal: ProposedItemCandidate) {
        if (!actionable(proposal)) return
        _uiState.update { state ->
            IntakeAutoAdvance.afterResolution(
                state.copy(dismissedIds = state.dismissedIds + proposal.id),
                intakeUseCase.intakeSteps,
            )
        }
        viewModelScope.launch {
            finishIfNeeded()
            persistSession()
        }
    }

    fun rejectProposal(proposal: ProposedItemCandidate) {
        viewModelScope.launch {
            if (!actionable(proposal)) return@launch
            val newRejected = _uiState.value.rejectedIds + IntakeProposalResolution.signature(proposal)
            _uiState.update { state ->
                IntakeAutoAdvance.afterResolution(
                    state.copy(rejectedIds = newRejected),
                    intakeUseCase.intakeSteps,
                )
            }
            finishIfNeeded()
            persistSession()
        }
    }

    fun requestMoreLikeThis(proposal: ProposedItemCandidate) {
        val userMsg = ChatTurnMessage(sender = MessageSender.OPERATOR, text = "Give me another node like ${proposal.name}.")
        _uiState.update { it.copy(messages = it.messages + userMsg) }

        viewModelScope.launch {
            val stepIndex = _uiState.value.currentStepIndex
            val context = repository.loadIntakeContext()
            val priorUtterance = _uiState.value.messages.lastOrNull { message ->
                message.sender == MessageSender.OPERATOR && !message.text.startsWith("Give me another node")
            }?.text ?: proposal.name

            val seedName = proposal.name
            val (guideText, rawProposals) = intakeUseCase.moreLikeThis(
                stepIndex,
                priorUtterance,
                seedName,
                context.activeItems,
            )
            
            val filteredProposals = rawProposals.filter { proposal ->
                proposal.name != seedName && actionable(proposal)
            }

            val guideMsg = ChatTurnMessage(
                sender = MessageSender.GUIDE,
                text = guideText,
                proposals = filteredProposals
            )
            _uiState.update { state ->
                IntakeAutoAdvance.afterResolution(
                    state.copy(messages = state.messages + guideMsg),
                    intakeUseCase.intakeSteps,
                )
            }
            finishIfNeeded()
            persistSession()
        }
    }

    private fun actionable(proposal: ProposedItemCandidate): Boolean {
        val state = _uiState.value
        return IntakeProposalResolution.isActionable(
            proposal,
            state.acceptedIds,
            state.rejectedIds,
            state.dismissedIds,
        )
    }

    fun skipCurrentStep() {
        _uiState.update { state ->
            IntakeAutoAdvance.toStep(state, intakeUseCase.intakeSteps, state.currentStepIndex + 1)
        }
        viewModelScope.launch {
            finishIfNeeded()
            persistSession()
        }
    }

    private suspend fun finishIfNeeded() {
        if (_uiState.value.isCompleted) {
            repository.setConversationalIntakeSeen(true)
        }
    }

    fun completeIntake() {
        viewModelScope.launch {
            repository.setConversationalIntakeSeen(true)
            _uiState.update { it.copy(isCompleted = true, currentStepIndex = 0) }
            persistSession()
        }
    }

    private fun isDonePhrase(reply: String): Boolean {
        val text = reply.trim().lowercase().trimEnd('.')
        return text in setOf("done", "i'm done", "im done", "i am done", "finished", "i'm finished")
    }

    private suspend fun persistSession() {
        val state = _uiState.value
        repository.saveIntakeSession(
            SelfMapIntakeSession(
                stepIndex = state.currentStepIndex,
                acceptedItemIds = state.acceptedIds,
                rejectedSignatures = state.rejectedIds,
                dismissedIds = state.dismissedIds,
                stepAnchor = state.stepAnchor,
                transcript = IntakeTranscriptCodec.encode(state.messages),
            )
        )
    }
}
