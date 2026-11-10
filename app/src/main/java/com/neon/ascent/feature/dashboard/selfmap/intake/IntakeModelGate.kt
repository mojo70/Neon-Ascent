package com.neon.ascent.feature.dashboard.selfmap.intake

import com.neon.ascent.feature.dashboard.selfmap.ProposedItemCandidate

/**
 * Post-validation for the isolated intake call.
 * A hit here drops the model output and the shell falls back to a short question.
 */
object IntakeModelGate {
    /** Rationales over this many words are rejected. Locked step questions are not passed through here. */
    const val MAX_RATIONALE_WORDS = 12

    private val blockedPatterns = listOf(
        "cybr-tes",
        "socrates",
        "faith",
        "vision/stimulus",
        "protocol",
        "runner",
    )
    private val genericCatchAlls = listOf(
        "core focus",
        "personal growth drive",
        "primary identity anchor",
        "essential psychological vector",
        "baseline setup",
    )

    fun accepted(
        rawText: String,
        proposals: List<ProposedItemCandidate>,
        anchor: String,
        seedName: String?,
    ): List<ProposedItemCandidate> {
        if (proposals.isEmpty()) return emptyList()
        val hasBlocked = blockedPatterns.any { pattern ->
            rawText.lowercase().contains(pattern) || proposals.any { prop ->
                prop.rationale?.lowercase()?.contains(pattern) == true ||
                    prop.note?.lowercase()?.contains(pattern) == true ||
                    prop.name.lowercase().contains(pattern)
            }
        }
        val hasGeneric = proposals.any { prop ->
            genericCatchAlls.any { prop.name.lowercase().contains(it) }
        }
        val anchors = anchorWords(anchor) + anchorWords(seedName.orEmpty())
        val isGrounded = proposals.all { prop ->
            val haystack = listOfNotNull(prop.rationale, prop.note, prop.name).joinToString(" ").lowercase()
            anchors.isEmpty() || anchors.any { haystack.contains(it) }
        }
        val echoes = proposals.any { prop ->
            echoesAll(prop.rationale, anchor) || echoesAll(prop.note, anchor)
        }
        val tooLong = proposals.any { rationaleTooLong(it) }
        if (hasBlocked || hasGeneric || !isGrounded || echoes || tooLong) return emptyList()
        return proposals.take(2)
    }

    private fun rationaleTooLong(prop: ProposedItemCandidate): Boolean {
        val words = prop.rationale
            ?.trim()
            ?.split(Regex("\\s+"))
            ?.count { it.isNotBlank() }
            ?: 0
        return words > MAX_RATIONALE_WORDS
    }

    private fun anchorWords(text: String): Set<String> {
        val stop = setOf("more", "like", "this", "refine", "another", "node", "give", "the", "and")
        return text.lowercase().split(Regex("\\s+"))
            .map { it.replace(Regex("[^a-z0-9]"), "") }
            .filter { it.length > 2 && it !in stop }
            .toSet()
    }

    private fun echoesAll(text: String?, source: String): Boolean {
        if (text.isNullOrBlank() || source.length < 24) return false
        val value = text.trim().trim('"')
        return value.equals(source.trim(), ignoreCase = true) || value.contains(source.trim(), ignoreCase = true)
    }
}
