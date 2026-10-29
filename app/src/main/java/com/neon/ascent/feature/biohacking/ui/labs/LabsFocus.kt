package com.neon.ascent.feature.biohacking.ui.labs

enum class LabsSection {
    Today,
    Log,
    Tools,
    Connect,
    CheckIn,
    Records,
    Analyze
}

/**
 * Maps a Labs deep-link focus string onto the section that should expand and scroll into view.
 * Unknown values, including null, land on Today.
 */
fun labsSectionForFocus(focus: String?): LabsSection = when (focus) {
    null, "vitals", "nutrition", "macros", "hrv", "biometrics" -> LabsSection.Today
    "body_log" -> LabsSection.Log
    "calculators" -> LabsSection.Tools
    "uplink" -> LabsSection.Connect
    "lifestyle", "recovery", "goals" -> LabsSection.CheckIn
    "baselines", "trends" -> LabsSection.Records
    "hologram", "insight", "attributes" -> LabsSection.Analyze
    else -> LabsSection.Today
}

/** Female, non-binary, other, and missing sex still see the pregnancy checkbox. Male does not. */
fun showsPregnancyCheck(sex: String?): Boolean =
    !sex?.trim().equals("MALE", ignoreCase = true)

fun buildLabsScanPrompt(
    sector: String,
    archetype: String?,
    mbti: String?,
    energyScore: Int,
    moodScore: Int,
    focusScore: Int,
    sex: String?,
    pregnancyFlag: Boolean,
    primaryObjective: String?,
    contraindications: String?
): String = buildString {
    appendLine("SYSTEM_SCAN_REQUEST: Sector $sector")
    appendLine("USER_PROFILE: ${archetype.orEmpty()} / ${mbti.orEmpty()}")
    appendLine("BIOMETRICS: Energy=$energyScore, Mood=$moodScore, Focus=$focusScore")
    if (showsPregnancyCheck(sex) && pregnancyFlag) {
        appendLine("PREGNANCY_FLAG: set")
    }
    val objective = primaryObjective?.trim().orEmpty()
    if (objective.isNotEmpty()) {
        appendLine("PRIMARY_OBJECTIVE: $objective")
    }
    val limits = contraindications?.trim().orEmpty()
    if (limits.isNotEmpty()) {
        appendLine("CONTRAINDICATIONS: $limits")
    }
    appendLine("OBJECTIVE: Generate a high-impact cyberpunk biohacking protocol for this sector.")
    append("FORMAT: Concise, technical, neon-noir style. Max 100 words.")
}
