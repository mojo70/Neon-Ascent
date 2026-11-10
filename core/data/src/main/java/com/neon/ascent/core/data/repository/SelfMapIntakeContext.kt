package com.neon.ascent.core.data.repository

import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.domain.selfmap.SelfMapCategory

/**
 * Non-archived map snapshot loaded before the first proposal.
 * [bodyAndSenses] is the pre-seeded Body & Senses set still on the map.
 */
data class SelfMapIntakeContext(
    val activeItems: List<SelfItemEntity>,
    val categoriesInUse: Set<SelfMapCategory>,
    val bodyAndSenses: List<SelfItemEntity>,
) {
    companion object {
        fun fromActiveItems(items: List<SelfItemEntity>): SelfMapIntakeContext {
            val active = items.filter { !it.isArchived }
            return SelfMapIntakeContext(
                activeItems = active,
                categoriesInUse = active.map { it.category }.toSet(),
                bodyAndSenses = active.filter { it.category == SelfMapCategory.BODY_SENSES },
            )
        }
    }
}

/**
 * Lightweight resume record. [transcript] is the visible thread.
 * A session with only zeros and empty sets is a fresh start.
 */
data class SelfMapIntakeSession(
    val stepIndex: Int,
    val acceptedItemIds: Set<String>,
    val rejectedSignatures: Set<String>,
    val dismissedIds: Set<String> = emptySet(),
    val stepAnchor: Int = 0,
    val transcript: String = "",
) {
    fun hasResumeState(): Boolean {
        return transcript.isNotBlank() ||
            stepIndex > 0 ||
            acceptedItemIds.isNotEmpty() ||
            rejectedSignatures.isNotEmpty() ||
            dismissedIds.isNotEmpty()
    }
}
