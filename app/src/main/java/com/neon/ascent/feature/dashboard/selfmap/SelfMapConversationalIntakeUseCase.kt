package com.neon.ascent.feature.dashboard.selfmap

import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.domain.ai.AiCore
import com.neon.ascent.core.domain.ai.AiResult
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class MessageSender { GUIDE, OPERATOR }

data class ChatTurnMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val proposals: List<ProposedItemCandidate> = emptyList()
)

data class ProposedItemCandidate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: SelfMapCategory,
    val suggestedImportance: Float,
    val note: String? = null,
    val rationale: String? = null,
    val isAccepted: Boolean = false,
    val isRejected: Boolean = false
)

data class IntakeStep(
    val stepIndex: Int,
    val title: String,
    val promptQuestion: String,
    val targetCategory: SelfMapCategory,
    val defaultProposals: List<ProposedItemCandidate>
)

@Singleton
class SelfMapConversationalIntakeUseCase @Inject constructor(
    private val aiCore: AiCore
) {
    val intakeSteps = listOf(
        IntakeStep(
            stepIndex = 0,
            title = "PHASE 1 // IDENTITY & OVERVIEW",
            promptQuestion = "Tell me a little about who you are right now. What defines your baseline identity or main focus?",
            targetCategory = SelfMapCategory.CUSTOM,
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Core Focus", category = SelfMapCategory.CUSTOM, suggestedImportance = 0.85f, note = "Primary identity anchor", rationale = "Grounded in operator baseline setup"),
                ProposedItemCandidate(name = "Personal Growth Drive", category = SelfMapCategory.CUSTOM, suggestedImportance = 0.75f, note = "Continuous learning and mastery", rationale = "Essential psychological vector")
            )
        ),
        IntakeStep(
            stepIndex = 1,
            title = "PHASE 2 // PEOPLE & CONNECTIONS",
            promptQuestion = "Who are the key people that matter most to you right now—family, partner, close allies, or dependents?",
            targetCategory = SelfMapCategory.PEOPLE,
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Inner Circle & Family", category = SelfMapCategory.PEOPLE, suggestedImportance = 0.85f, note = "Closest personal relationships", rationale = "High emotional attachment weight"),
                ProposedItemCandidate(name = "Core Allies & Team", category = SelfMapCategory.PEOPLE, suggestedImportance = 0.70f, note = "Key professional or operational partners", rationale = "Critical operational support network")
            )
        ),
        IntakeStep(
            stepIndex = 2,
            title = "PHASE 3 // STUFF & ROLES",
            promptQuestion = "What possessions, career roles, or key accomplishments would feel like a major loss if they disappeared?",
            targetCategory = SelfMapCategory.ROLES_ACCOMPLISHMENTS,
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Primary Profession / Role", category = SelfMapCategory.ROLES_ACCOMPLISHMENTS, suggestedImportance = 0.75f, note = "Core identity role and focus", rationale = "Primary societal and capability anchor"),
                ProposedItemCandidate(name = "Key Workspace & Gear", category = SelfMapCategory.STUFF, suggestedImportance = 0.60f, note = "Essential operational tools and possessions", rationale = "Physical leverage tool")
            )
        ),
        IntakeStep(
            stepIndex = 3,
            title = "PHASE 4 // IDEAS & VALUES",
            promptQuestion = "What core values, beliefs, or strong likes/dislikes feel like a foundational part of 'you'?",
            targetCategory = SelfMapCategory.IDEAS_LIKES,
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Core Principles", category = SelfMapCategory.IDEAS_LIKES, suggestedImportance = 0.80f, note = "Guiding ethical compass", rationale = "Internal behavioral governor"),
                ProposedItemCandidate(name = "Autonomy & Mastery", category = SelfMapCategory.IDEAS_LIKES, suggestedImportance = 0.70f, note = "Drive for independence and growth", rationale = "Core intrinsic motivator")
            )
        ),
        IntakeStep(
            stepIndex = 4,
            title = "PHASE 5 // BODY & SENSES CONFIRMATION",
            promptQuestion = "Confirm or adjust your pre-seeded Body & Senses items to anchor your map center.",
            targetCategory = SelfMapCategory.BODY_SENSES,
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Body Vessel", category = SelfMapCategory.BODY_SENSES, suggestedImportance = 0.90f, note = "Core physical vessel and anchor", rationale = "Fundamental physical container"),
                ProposedItemCandidate(name = "Physical Health", category = SelfMapCategory.BODY_SENSES, suggestedImportance = 0.85f, note = "Energy and physical capacity", rationale = "Prerequisite for all operational output")
            )
        )
    )

    suspend fun processUserTurn(
        currentStepIndex: Int,
        userMessage: String,
        existingItems: List<SelfItemEntity> = emptyList()
    ): Pair<String, List<ProposedItemCandidate>> {
        val currentStep = intakeSteps.getOrElse(currentStepIndex) { intakeSteps.first() }
        val categoryCoverage = existingItems.map { it.category }.toSet()
        val isCovered = categoryCoverage.contains(currentStep.targetCategory)

        val existingContext = if (existingItems.isNotEmpty()) {
            "Already Mapped Items (" + existingItems.size + "): " + existingItems.joinToString(", ") { "${it.name} [${it.category.name}, imp: ${it.importance}]" }
        } else {
            "No items mapped yet."
        }

        val baseQuestion = if (isCovered && userMessage.isBlank()) {
            "I notice you already have items mapped in ${currentStep.targetCategory.displayName}. Would you like to refine or add more in this category, or proceed?"
        } else {
            currentStep.promptQuestion
        }

        // Follow-up only for empty / one-word / skip inputs
        val trimmed = userMessage.trim()
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val isSkipOrShort = userMessage.isBlank() || words.size <= 1 || trimmed.lowercase() in listOf("skip", "none", "nothing", "idk", "pass", "no")

        if (isSkipOrShort) {
            val followUp = if (userMessage.isBlank()) {
                baseQuestion
            } else {
                "Understood. Would you like to name one specific landmark in ${currentStep.targetCategory.displayName}, or proceed to the next phase?"
            }
            return Pair(followUp, emptyList())
        }

        // --- 1. DETERMINISTIC ENTITY EXTRACTION (PRIMARY PATH) ---
        val lowerMsg = userMessage.lowercase()
        val deterministicProposals = mutableListOf<ProposedItemCandidate>()

        if (lowerMsg.contains("family") || lowerMsg.contains("wife") || lowerMsg.contains("kids") || lowerMsg.contains("children") || lowerMsg.contains("son") || lowerMsg.contains("daughter")) {
            deterministicProposals.add(
                ProposedItemCandidate(
                    name = "Family & Kin",
                    category = SelfMapCategory.PEOPLE,
                    suggestedImportance = 0.85f,
                    note = "Core family attachment",
                    rationale = "Grounded in user mention of family/wife/kids"
                )
            )
        }
        if (lowerMsg.contains("jesus") || lowerMsg.contains("lord") || lowerMsg.contains("faith") || lowerMsg.contains("prayer") || lowerMsg.contains("scripture") || lowerMsg.contains("christ")) {
            deterministicProposals.add(
                ProposedItemCandidate(
                    name = "Faith & Spiritual Anchor",
                    category = SelfMapCategory.IDEAS_LIKES,
                    suggestedImportance = 0.85f,
                    note = "Spiritual foundation",
                    rationale = "Grounded in user mention of faith/Jesus/Lord"
                )
            )
        }
        if (lowerMsg.contains("coding") || lowerMsg.contains("building") || lowerMsg.contains("creating") || lowerMsg.contains("tinkering") || lowerMsg.contains("dev") || lowerMsg.contains("architect")) {
            deterministicProposals.add(
                ProposedItemCandidate(
                    name = "Creation & Building",
                    category = SelfMapCategory.ROLES_ACCOMPLISHMENTS,
                    suggestedImportance = 0.80f,
                    note = "Maker and engineering focus",
                    rationale = "Grounded in user mention of building/coding/creating"
                )
            )
        }
        if (lowerMsg.contains("lifting") || lowerMsg.contains("weights") || lowerMsg.contains("body") || lowerMsg.contains("mind") || lowerMsg.contains("health") || lowerMsg.contains("workout") || lowerMsg.contains("trainer")) {
            deterministicProposals.add(
                ProposedItemCandidate(
                    name = "Physical Conditioning",
                    category = SelfMapCategory.BODY_SENSES,
                    suggestedImportance = 0.85f,
                    note = "Physical vitality",
                    rationale = "Grounded in user mention of health/weights/body"
                )
            )
        }
        if (lowerMsg.contains("learning") || lowerMsg.contains("reading") || lowerMsg.contains("study") || lowerMsg.contains("growth")) {
            deterministicProposals.add(
                ProposedItemCandidate(
                    name = "Continuous Learning",
                    category = SelfMapCategory.IDEAS_LIKES,
                    suggestedImportance = 0.75f,
                    note = "Intellectual expansion",
                    rationale = "Grounded in user mention of learning/reading"
                )
            )
        }
        if (lowerMsg.contains("work") || lowerMsg.contains("job") || lowerMsg.contains("provider") || lowerMsg.contains("career") || lowerMsg.contains("business")) {
            deterministicProposals.add(
                ProposedItemCandidate(
                    name = "Professional Vector",
                    category = SelfMapCategory.ROLES_ACCOMPLISHMENTS,
                    suggestedImportance = 0.75f,
                    note = "Work and career responsibility",
                    rationale = "Grounded in user mention of work/provider"
                )
            )
        }

        if (deterministicProposals.isNotEmpty()) {
            return Pair("Based on that, two nodes:", deterministicProposals.take(2))
        }

        // --- 2. FALLBACK MODEL CALL (ISOLATED PROMPT) ---
        val prompt = """
            [SYSTEM: Self Map Intake Structured Extraction Engine]
            Role: Strict structured proposal generator for Self Map intake.
            Constraints:
            - NEVER inherit CYBR-TES, Socrates, or Faith Lens. No philosophy, no moralizing, no emotional invention.
            - Tone: Neutral, operational, concise.
            - Task: Emit 1 to 3 structured proposal items grounded strictly in the operator's last message: "$userMessage".
            $existingContext
            - Format strictly as:
            [PROPOSAL: Name | Category | Importance (0.40 to 0.90) | Short Note | Grounded Rationale]
            - Categories: BODY_SENSES, PEOPLE, STUFF, ROLES_ACCOMPLISHMENTS, IDEAS_LIKES, CUSTOM.
            - Do not include conversational monologue or introductory filler.
        """.trimIndent()

        val proposalsToEmit = if (aiCore.isReady()) {
            when (val res = aiCore.generate(prompt, forceLocal = true)) {
                is AiResult.Success -> {
                    val (rawText, proposals) = parseAiResponse(res.text)
                    
                    // Post-validation:
                    // 1. Blocked patterns (CYBR-TES, Socrates, Runner, protocol, vision/stimulus, faith)
                    val blockedPatterns = listOf("cybr-tes", "socrates", "faith", "vision/stimulus", "protocol", "runner")
                    val hasBlocked = blockedPatterns.any { pattern ->
                        rawText.lowercase().contains(pattern) || proposals.any { prop ->
                            (prop.rationale?.lowercase()?.contains(pattern) == true) ||
                            (prop.note?.lowercase()?.contains(pattern) == true) ||
                            prop.name.lowercase().contains(pattern)
                        }
                    }

                    // 2. Generic catch-all check: reject generic names
                    val genericCatchAlls = listOf("core focus", "personal growth drive", "primary identity anchor", "essential psychological vector", "baseline setup")
                    val hasGeneric = proposals.any { prop ->
                        val n = prop.name.lowercase()
                        genericCatchAlls.any { n.contains(it) }
                    }

                    // 3. Grounding check: rationale or proposal name must reference concrete words from userMessage
                    val userWords = trimmed.lowercase().split(Regex("\\s+"))
                        .map { it.replace(Regex("[^a-z0-9]"), "") }
                        .filter { it.length > 2 }
                    val isGrounded = proposals.isNotEmpty() && proposals.all { prop ->
                        val rationale = prop.rationale?.lowercase() ?: ""
                        val note = prop.note?.lowercase() ?: ""
                        val name = prop.name.lowercase()
                        userWords.isEmpty() || userWords.any { rationale.contains(it) || note.contains(it) || name.contains(it) }
                    }

                    if (!hasBlocked && !hasGeneric && isGrounded && proposals.isNotEmpty()) {
                        val templateIntro = "Based on that, two nodes:"
                        return Pair(templateIntro, proposals)
                    } else {
                        // Fall back to short confirmation of what was heard instead of showing bad cards
                        val fallbackMsg = "I heard \"$trimmed\" — want nodes for that?"
                        return Pair(fallbackMsg, emptyList())
                    }
                }
                is AiResult.Failure -> {
                    val fallbackMsg = "I heard \"$trimmed\" — want nodes for that?"
                    return Pair(fallbackMsg, emptyList())
                }
            }
        } else {
            val fallbackMsg = "I heard \"$trimmed\" — want nodes for that?"
            return Pair(fallbackMsg, emptyList())
        }
    }

    private fun parseAiResponse(response: String): Pair<String, List<ProposedItemCandidate>> {
        val proposalRegex = Regex("\\[PROPOSAL: (.*?)\\]")
        val matches = proposalRegex.findAll(response).toList()

        val proposals = matches.mapNotNull { match ->
            val inner = match.groupValues[1]
            val parts = inner.split("|").map { it.trim() }
            if (parts.size < 2) return@mapNotNull null

            val name = parts[0]
            if (name.isBlank()) return@mapNotNull null

            val categoryStr = parts[1].uppercase()
            val category = SelfMapCategory.entries.firstOrNull { it.name == categoryStr } ?: SelfMapCategory.CUSTOM

            val importance = if (parts.size > 2) {
                parts[2].toFloatOrNull()?.coerceIn(0.40f, 0.90f) ?: 0.65f
            } else {
                0.65f
            }

            val note = if (parts.size > 3 && parts[3].isNotBlank()) parts[3] else null
            val rationale = if (parts.size > 4 && parts[4].isNotBlank()) parts[4] else null

            ProposedItemCandidate(
                name = name,
                category = category,
                suggestedImportance = importance,
                note = note,
                rationale = rationale
            )
        }

        val cleanText = response.replace(proposalRegex, "").trim()
        val text = cleanText.ifBlank { "Structured proposal candidates generated." }
        return Pair(text, proposals)
    }

    suspend fun suggestItemForCategory(category: SelfMapCategory): ProposedItemCandidate {
        val prompt = """
            [SYSTEM: Self Map Intake Structured Extraction Engine]
            Role: Strict structured proposal generator for Self Map intake.
            Constraints:
            - NEVER inherit CYBR-TES, Socrates, or Faith Lens. No philosophy, no moralizing, no emotional invention.
            - Tone: Neutral, operational, concise.
            - Task: Suggest 1 relevant, high-value Self Map attachment node for category: ${category.displayName}.
            - Format strictly as:
            [PROPOSAL: Name | ${category.name} | Importance (0.40 to 0.90) | Short Note | Grounded Rationale]
        """.trimIndent()

        if (aiCore.isReady()) {
            when (val res = aiCore.generate(prompt, forceLocal = true)) {
                is AiResult.Success -> {
                    val (rawText, proposals) = parseAiResponse(res.text)
                    val blockedPatterns = listOf("cybr-tes", "socrates", "faith", "vision/stimulus", "protocol", "runner")
                    val hasBlocked = blockedPatterns.any { pattern ->
                        rawText.lowercase().contains(pattern) || proposals.any { prop ->
                            (prop.rationale?.lowercase()?.contains(pattern) == true) ||
                            (prop.note?.lowercase()?.contains(pattern) == true) ||
                            prop.name.lowercase().contains(pattern)
                        }
                    }
                    if (!hasBlocked && proposals.isNotEmpty()) {
                        return proposals.first()
                    }
                }
                is AiResult.Failure -> {}
            }
        }

        return when (category) {
            SelfMapCategory.BODY_SENSES -> ProposedItemCandidate(name = "Physical Health & Energy", category = category, suggestedImportance = 0.85f, note = "Core operating capacity", rationale = "Baseline physiological container")
            SelfMapCategory.PEOPLE -> ProposedItemCandidate(name = "Inner Circle Allies", category = category, suggestedImportance = 0.80f, note = "Primary personal bonds", rationale = "Key social security network")
            SelfMapCategory.STUFF -> ProposedItemCandidate(name = "Primary Workspace", category = category, suggestedImportance = 0.65f, note = "Operational environment", rationale = "Execution leverage")
            SelfMapCategory.ROLES_ACCOMPLISHMENTS -> ProposedItemCandidate(name = "Lead Operational Role", category = category, suggestedImportance = 0.75f, note = "Main focus area", rationale = "Core societal contribution")
            SelfMapCategory.IDEAS_LIKES -> ProposedItemCandidate(name = "Autonomy & Mastery", category = category, suggestedImportance = 0.70f, note = "Core guiding value", rationale = "Intrinsic psychological driver")
            SelfMapCategory.CUSTOM -> ProposedItemCandidate(name = "Personal Landmark", category = category, suggestedImportance = 0.60f, note = "Custom attachment", rationale = "Self-defined pillar")
        }
    }
}
