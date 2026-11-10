package com.neon.ascent.feature.dashboard.selfmap.intake

import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import com.neon.ascent.feature.dashboard.selfmap.ProposedItemCandidate

/** Fields copied into the wizard when a card is accepted-and-edited. The form save is the insert. */
data class IntakeFormFields(
    val name: String,
    val category: SelfMapCategory,
    val importance: Float,
    val notes: String,
)

object IntakeFormHandoff {
    fun draftFrom(proposal: ProposedItemCandidate) = IntakeFormFields(
        name = proposal.name,
        category = proposal.category,
        importance = proposal.suggestedImportance,
        notes = proposal.note ?: "",
    )
}
