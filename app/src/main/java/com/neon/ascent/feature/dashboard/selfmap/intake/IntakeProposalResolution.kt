package com.neon.ascent.feature.dashboard.selfmap.intake

import com.neon.ascent.feature.dashboard.selfmap.ProposedItemCandidate

/**
 * A card is actionable until the operator accepts it, rejects it, or dismisses it into the form.
 * Auto-advance runs only when the step has cards and none of them are still actionable.
 */
object IntakeProposalResolution {
    fun signature(proposal: ProposedItemCandidate): String = "${proposal.name}:${proposal.category.name}"

    fun isActionable(
        proposal: ProposedItemCandidate,
        acceptedIds: Set<String>,
        rejectedSignatures: Set<String>,
        dismissedIds: Set<String>,
    ): Boolean {
        if (proposal.id in acceptedIds || proposal.id in dismissedIds) return false
        val signature = signature(proposal)
        if (signature in rejectedSignatures || signature in acceptedIds || proposal.name in rejectedSignatures) return false
        return true
    }

    fun canAdvance(
        proposals: List<ProposedItemCandidate>,
        acceptedIds: Set<String>,
        rejectedSignatures: Set<String>,
        dismissedIds: Set<String>,
    ): Boolean {
        if (proposals.isEmpty()) return false
        return proposals.none { isActionable(it, acceptedIds, rejectedSignatures, dismissedIds) }
    }
}
