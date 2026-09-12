package com.neon.ascent.core.domain.notifications.brief

import com.neon.ascent.core.domain.notifications.models.BriefFacts

object BriefCopyValidator {

    private val uuidRegex = Regex("(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b|\\b[0-9a-f]{7,8}-[0-9a-f]{4}\\b")

    private val forbiddenPhrases = listOf(
        "SESSION ",
        "CYBERCRAPP LOG",
        "**",
        "sprawl",
        "reboot",
        "dangerously",
        "CORE_MALFUNCTION",
        "ERROR:"
    )

    fun isValid(text: String?, facts: BriefFacts? = null): Boolean {
        if (text.isNullOrBlank()) return false

        if (uuidRegex.containsMatchIn(text)) return false

        for (phrase in forbiddenPhrases) {
            if (text.contains(phrase, ignoreCase = true)) return false
        }

        facts?.lastSession?.id?.let { sessionId ->
            if (sessionId.isNotBlank() && text.contains(sessionId, ignoreCase = true)) {
                return false
            }
        }

        return true
    }
}
