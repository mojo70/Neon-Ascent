package com.neon.ascent.feature.dashboard.selfmap.intake

import com.neon.ascent.core.data.repository.SelfMapIntakeSession
import com.neon.ascent.feature.dashboard.selfmap.IntakeStep

/**
 * Rebuilds the terminal from the saved session. A fresh install (no step, no ids, no transcript)
 * returns null so the shell asks the first template.
 */
object IntakeResume {
    fun restore(session: SelfMapIntakeSession, steps: List<IntakeStep>): IntakeUiState? {
        if (!session.hasResumeState()) return null
        val messages = IntakeTranscriptCodec.decode(session.transcript)
        val lastStep = (steps.size - 1).coerceAtLeast(0)
        val anchor = if (messages.isEmpty()) {
            0
        } else {
            session.stepAnchor.coerceIn(0, messages.lastIndex)
        }
        return IntakeUiState(
            isLoading = false,
            currentStepIndex = session.stepIndex.coerceIn(0, lastStep),
            messages = messages,
            acceptedIds = session.acceptedItemIds,
            rejectedIds = session.rejectedSignatures,
            dismissedIds = session.dismissedIds,
            stepAnchor = anchor,
            isCompleted = false,
        )
    }
}
