package com.neon.ascent.feature.dashboard.selfmap.intake

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.repository.SelfMapRepository
import com.neon.ascent.feature.dashboard.selfmap.ChatTurnMessage
import com.neon.ascent.feature.dashboard.selfmap.MessageSender
import com.neon.ascent.feature.dashboard.selfmap.ProposedItemCandidate
import com.neon.ascent.feature.dashboard.selfmap.SelfMapConversationalIntakeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
            // Context Load: Pre-seed if empty, load active items
            repository.preseedDefaultItemsIfEmpty()
            val items = repository.activeItems.first()

            // Load lightweight session state
            val savedStep = repository.intakeStepIndex.first()
            val accepted = repository.acceptedItemIds.first()
            val rejected = repository.rejectedSignatures.first()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentStepIndex = savedStep,
                    activeItems = items,
                    acceptedIds = accepted,
                    rejectedIds = rejected
                )
            }

            // Initialize chat with the first question if empty
            if (_uiState.value.messages.isEmpty()) {
                askCurrentStepQuestion()
            }
        }
    }

    private fun askCurrentStepQuestion() {
        val stepIndex = _uiState.value.currentStepIndex
        if (stepIndex >= intakeUseCase.intakeSteps.size) {
            completeIntake()
            return
        }

        val step = intakeUseCase.intakeSteps[stepIndex]
        val isCovered = _uiState.value.activeItems.any { it.category == step.targetCategory }

        val question = if (isCovered && stepIndex != 0) {
            "I notice you already have items mapped in ${step.targetCategory.displayName}. Would you like to refine or add more in this category, or proceed?"
        } else {
            step.promptQuestion
        }

        val newMsg = ChatTurnMessage(
            sender = MessageSender.GUIDE,
            text = question,
            proposals = emptyList()
        )

        _uiState.update { it.copy(messages = it.messages + newMsg) }
    }

    fun submitUserReply(reply: String) {
        if (reply.isBlank()) return

        val userMsg = ChatTurnMessage(sender = MessageSender.OPERATOR, text = reply)
        _uiState.update { it.copy(messages = it.messages + userMsg) }

        viewModelScope.launch {
            val stepIndex = _uiState.value.currentStepIndex
            val items = repository.getAllItemsOnce()

            val (guideText, rawProposals) = intakeUseCase.processUserTurn(stepIndex, reply, items)

            // Filter out already accepted/rejected proposals
            val filteredProposals = rawProposals.filterNot {
                _uiState.value.acceptedIds.contains(it.id) || _uiState.value.rejectedIds.contains(it.name)
            }

            val guideMsg = ChatTurnMessage(
                sender = MessageSender.GUIDE,
                text = guideText,
                proposals = filteredProposals
            )
            _uiState.update { it.copy(messages = it.messages + guideMsg) }

            if (filteredProposals.isEmpty()) {
                // If no proposals or all filtered, we can auto-advance if it was a pure skip
                val trimmed = reply.trim().lowercase()
                val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
                val isSkip = words.size <= 1 || trimmed in listOf("skip", "none", "nothing", "idk", "pass", "no")
                if (isSkip) {
                    advanceToNextStep()
                }
            }
        }
    }

    fun acceptProposal(proposal: ProposedItemCandidate) {
        viewModelScope.launch {
            val item = SelfItemEntity(
                name = proposal.name,
                category = proposal.category,
                importance = proposal.suggestedImportance,
                notes = proposal.note,
                baselineMassContributor = true
            )
            repository.insertSelfItem(item)
            
            val newAccepted = _uiState.value.acceptedIds + proposal.id
            repository.setAcceptedItemIds(newAccepted)
            
            val confirmMsg = ChatTurnMessage(
                sender = MessageSender.GUIDE,
                text = "Confirmed: [${proposal.name}] added to Self Map."
            )

            _uiState.update { it.copy(
                acceptedIds = newAccepted,
                activeItems = repository.getAllItemsOnce(),
                messages = it.messages + confirmMsg
            ) }
            
            checkStepCompletion()
        }
    }

    fun rejectProposal(proposal: ProposedItemCandidate) {
        viewModelScope.launch {
            val newRejected = _uiState.value.rejectedIds + proposal.name
            repository.setRejectedSignatures(newRejected)
            _uiState.update { it.copy(rejectedIds = newRejected) }
            
            checkStepCompletion()
        }
    }

    fun requestMoreLikeThis(proposal: ProposedItemCandidate) {
        val userMsg = ChatTurnMessage(sender = MessageSender.OPERATOR, text = "Give me another node like ${proposal.name}.")
        _uiState.update { it.copy(messages = it.messages + userMsg) }

        viewModelScope.launch {
            val stepIndex = _uiState.value.currentStepIndex
            val items = repository.getAllItemsOnce()
            
            val (guideText, rawProposals) = intakeUseCase.processUserTurn(stepIndex, "more like ${proposal.name}", items)
            
            val filteredProposals = rawProposals.filterNot { 
                _uiState.value.acceptedIds.contains(it.id) || _uiState.value.rejectedIds.contains(it.name) 
            }

            val guideMsg = ChatTurnMessage(
                sender = MessageSender.GUIDE,
                text = guideText,
                proposals = filteredProposals
            )
            _uiState.update { it.copy(messages = it.messages + guideMsg) }
            
            if (filteredProposals.isEmpty()) {
                checkStepCompletion()
            }
        }
    }

    private fun checkStepCompletion() {
        val state = _uiState.value
        val currentStep = intakeUseCase.intakeSteps.getOrNull(state.currentStepIndex) ?: return
        
        // 1. Identify all messages since the Guide asked the current step's question
        val lastQuestionIndex = state.messages.indexOfLast { msg ->
            msg.sender == MessageSender.GUIDE && 
            (msg.text == currentStep.promptQuestion || msg.text.contains("Would you like to refine"))
        }

        if (lastQuestionIndex != -1) {
            val messagesSinceQuestion = state.messages.drop(lastQuestionIndex + 1)
            
            // 2. Gather every proposal emitted in this step
            val allProposalsInStep = messagesSinceQuestion.flatMap { it.proposals }
            
            if (allProposalsInStep.isEmpty()) return // No proposals to resolve yet
            
            // 3. Check if any proposal is still actionable (neither accepted nor rejected)
            val hasUnresolved = allProposalsInStep.any { prop ->
                !state.acceptedIds.contains(prop.id) && !state.rejectedIds.contains(prop.name)
            }

            // 4. Force auto-advance: if proposals exist and ALL are resolved, move immediately.
            if (!hasUnresolved) {
                advanceToNextStep()
            }
        }
    }

    fun skipCurrentStep() {
        advanceToNextStep()
    }

    private fun advanceToNextStep() {
        val nextStep = _uiState.value.currentStepIndex + 1
        viewModelScope.launch {
            repository.setIntakeStepIndex(nextStep)
            _uiState.update { it.copy(currentStepIndex = nextStep) }
            askCurrentStepQuestion()
        }
    }

    fun completeIntake() {
        viewModelScope.launch {
            repository.setConversationalIntakeSeen(true)
            repository.setIntakeStepIndex(0)
            _uiState.update { it.copy(isCompleted = true) }
        }
    }
}
