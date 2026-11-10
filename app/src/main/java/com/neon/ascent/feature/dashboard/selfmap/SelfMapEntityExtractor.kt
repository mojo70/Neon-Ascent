package com.neon.ascent.feature.dashboard.selfmap

import com.neon.ascent.core.domain.selfmap.SelfMapCategory

/**
 * First pass for a non-empty reply. A hit returns at most two nodes and does not call a model.
 * The rationale quotes a short span around the matched word, never the whole reply.
 */
object SelfMapEntityExtractor {
    data class Rule(
        val keywords: List<String>,
        val name: String,
        val category: SelfMapCategory,
        val importance: Float,
        val note: String,
    )

    val rules: List<Rule> = listOf(
        Rule(
            keywords = listOf("family", "wife", "kids", "children", "son", "daughter"),
            name = "Family & Kin",
            category = SelfMapCategory.PEOPLE,
            importance = 0.85f,
            note = "People attachment",
        ),
        Rule(
            keywords = listOf("jesus", "lord", "faith", "prayer", "scripture", "christ"),
            name = "Faith & Spiritual Anchor",
            category = SelfMapCategory.IDEAS_LIKES,
            importance = 0.85f,
            note = "Faith attachment",
        ),
        Rule(
            keywords = listOf("coding", "building", "creating", "tinkering", "dev", "architect"),
            name = "Creation & Building",
            category = SelfMapCategory.ROLES_ACCOMPLISHMENTS,
            importance = 0.80f,
            note = "Making attachment",
        ),
        Rule(
            keywords = listOf("lifting", "weights", "body", "mind", "health", "workout", "trainer"),
            name = "Physical Conditioning",
            category = SelfMapCategory.BODY_SENSES,
            importance = 0.85f,
            note = "Body attachment",
        ),
        Rule(
            keywords = listOf("learning", "reading", "study", "growth"),
            name = "Continuous Learning",
            category = SelfMapCategory.IDEAS_LIKES,
            importance = 0.75f,
            note = "Learning attachment",
        ),
        Rule(
            keywords = listOf("work", "job", "provider", "career", "business"),
            name = "Professional Vector",
            category = SelfMapCategory.ROLES_ACCOMPLISHMENTS,
            importance = 0.75f,
            note = "Work attachment",
        ),
    )

    fun extract(message: String): List<ProposedItemCandidate> {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return emptyList()
        return rules.mapNotNull { rule ->
            val keyword = rule.keywords.firstOrNull { containsWord(trimmed, it) } ?: return@mapNotNull null
            val quote = quoteAround(trimmed, keyword)
            ProposedItemCandidate(
                name = rule.name,
                category = rule.category,
                suggestedImportance = rule.importance,
                note = rule.note,
                rationale = "You said \"$quote\".",
            )
        }.take(2)
    }

    fun containsWord(message: String, keyword: String): Boolean {
        return Regex("\\b${Regex.escape(keyword)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(message)
    }

    /**
     * Up to three tokens around the keyword. A longer reply is never copied whole.
     */
    fun quoteAround(message: String, keyword: String): String {
        val tokens = message.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val index = tokens.indexOfFirst { token ->
            token.replace(Regex("[^A-Za-z0-9']"), "").equals(keyword, ignoreCase = true)
        }
        if (index < 0) return keyword
        if (tokens.size <= 1) return tokens[index].trim('"', '\'')
        val from = (index - 1).coerceAtLeast(0)
        val to = (index + 1).coerceAtMost(tokens.lastIndex)
        val span = tokens.subList(from, to + 1).joinToString(" ").trim('"', '\'')
        val whole = tokens.joinToString(" ")
        if (span.equals(whole, ignoreCase = true) || span.length > 48) {
            return tokens[index].trim('"', '\'')
        }
        return span
    }
}
