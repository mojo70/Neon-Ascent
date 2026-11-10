package com.neon.ascent.feature.dashboard.selfmap.intake

import com.neon.ascent.feature.dashboard.selfmap.ChatTurnMessage
import com.neon.ascent.feature.dashboard.selfmap.IntakeStep
import com.neon.ascent.feature.dashboard.selfmap.MessageSender

/**
 * Accept, reject, and dismiss call this directly. The next template is appended
 * in the same state update when every card after the step question is resolved.
 */
object IntakeAutoAdvance {
    fun afterResolution(state: IntakeUiState, steps: List<IntakeStep>): IntakeUiState {
        val proposals = state.messages
            .drop((state.stepAnchor + 1).coerceAtLeast(0))
            .flatMap { it.proposals }
        val resolved = IntakeProposalResolution.canAdvance(
            proposals,
            state.acceptedIds,
            state.rejectedIds,
            state.dismissedIds,
        )
        if (!resolved) return state
        return toStep(state, steps, state.currentStepIndex + 1)
    }

    fun toStep(state: IntakeUiState, steps: List<IntakeStep>, nextStep: Int): IntakeUiState {
        if (nextStep >= steps.size) {
            return state.copy(isCompleted = true, currentStepIndex = 0)
        }
        val question = ChatTurnMessage(
            sender = MessageSender.GUIDE,
            text = steps[nextStep].promptQuestion,
        )
        val messages = state.messages + question
        return state.copy(
            currentStepIndex = nextStep,
            messages = messages,
            stepAnchor = messages.lastIndex,
        )
    }
}
