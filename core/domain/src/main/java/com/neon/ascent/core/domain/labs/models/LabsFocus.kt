package com.neon.ascent.core.domain.labs.models

enum class OutcomeDomain(val displayName: String) {
    LONGEVITY_HEALTHSPAN("Longevity / Healthspan"),
    PHYSICAL_PERFORMANCE_RECOVERY("Physical Performance / Recovery"),
    MENTAL_ENERGY_FATIGUE("Mental Energy / Fatigue Resistance"),
    SLEEP_ARCHITECTURE("Sleep Architecture"),
    STRESS_RESILIENCE_MOOD("Stress Resilience / Mood"),
    ATTENTION_FOCUS("Attention / Focus");

    companion object {
        fun fromDisplayName(displayName: String?): OutcomeDomain? {
            if (displayName == null) return null
            return entries.firstOrNull { it.displayName.equals(displayName, ignoreCase = true) }
        }
    }
}

data class LabsFocus(
    val primary: OutcomeDomain? = null,
    val supporting: Set<OutcomeDomain> = emptySet()
) {
    init {
        if (primary != null) {
            require(primary !in supporting) {
                "Primary domain $primary cannot also be in supporting domains $supporting"
            }
        }
    }

    fun sanitized(): LabsFocus {
        return if (primary != null && primary in supporting) {
            copy(supporting = supporting - primary)
        } else {
            this
        }
    }

    companion object {
        fun createSanitized(
            primary: OutcomeDomain?,
            supporting: Set<OutcomeDomain>
        ): LabsFocus {
            val cleanSupporting = if (primary != null) supporting - primary else supporting
            return LabsFocus(primary = primary, supporting = cleanSupporting)
        }
    }
}

enum class FocusMatchLevel {
    PRIMARY,
    SUPPORTING,
    NONE
}

fun LabsFocus.matchLevel(outcomeDomains: List<String>): FocusMatchLevel {
    val primaryDisplay = primary?.displayName
    if (primaryDisplay != null && outcomeDomains.any { it.equals(primaryDisplay, ignoreCase = true) }) {
        return FocusMatchLevel.PRIMARY
    }
    val supportingDisplays = supporting.map { it.displayName }
    if (supportingDisplays.isNotEmpty() && outcomeDomains.any { domain ->
            supportingDisplays.any { it.equals(domain, ignoreCase = true) }
        }) {
        return FocusMatchLevel.SUPPORTING
    }
    return FocusMatchLevel.NONE
}

fun <T> LabsFocus.rankItems(
    items: List<T>,
    getOutcomeDomains: (T) -> List<String>
): List<T> {
    if (primary == null && supporting.isEmpty()) return items
    return items.sortedByDescending { item ->
        when (matchLevel(getOutcomeDomains(item))) {
            FocusMatchLevel.PRIMARY -> 2
            FocusMatchLevel.SUPPORTING -> 1
            FocusMatchLevel.NONE -> 0
        }
    }
}
