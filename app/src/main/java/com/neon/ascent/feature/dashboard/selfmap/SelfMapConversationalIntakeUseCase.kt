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
            promptQuestion = "Who are the people who matter most to you right now? People nodes are usually the attachments that produce the strongest reactions when threatened.",
            targetCategory = SelfMapCategory.PEOPLE,
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Inner Circle & Family", category = SelfMapCategory.PEOPLE, suggestedImportance = 0.85f, note = "Closest personal relationships", rationale = "High emotional attachment weight"),
                ProposedItemCandidate(name = "Core Allies & Team", category = SelfMapCategory.PEOPLE, suggestedImportance = 0.70f, note = "Key professional or operational partners", rationale = "Critical operational support network")
            )
        ),
        IntakeStep(
            stepIndex = 2,
            title = "PHASE 3 // STUFF & ROLES",
            promptQuestion = "What stuff or roles would you miss if they were gone? These are the identities and possessions the mind often defends.",
            targetCategory = SelfMapCategory.ROLES_ACCOMPLISHMENTS,
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Primary Profession / Role", category = SelfMapCategory.ROLES_ACCOMPLISHMENTS, suggestedImportance = 0.75f, note = "Core identity role and focus", rationale = "Primary societal and capability anchor"),
                ProposedItemCandidate(name = "Key Workspace & Gear", category = SelfMapCategory.STUFF, suggestedImportance = 0.60f, note = "Essential operational tools and possessions", rationale = "Physical leverage tool")
            )
        ),
        IntakeStep(
            stepIndex = 3,
            title = "PHASE 4 // IDEAS & VALUES",
            promptQuestion = "What ideas, values, or opinions feel like part of you? These shape expectations and how you appraise events.",
            targetCategory = SelfMapCategory.IDEAS_LIKES,
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Core Principles", category = SelfMapCategory.IDEAS_LIKES, suggestedImportance = 0.80f, note = "Guiding ethical compass", rationale = "Internal behavioral governor"),
                ProposedItemCandidate(name = "Autonomy & Mastery", category = SelfMapCategory.IDEAS_LIKES, suggestedImportance = 0.70f, note = "Drive for independence and growth", rationale = "Core intrinsic motivator")
            )
        ),
        IntakeStep(
            stepIndex = 4,
            title = "PHASE 5 // BODY & SENSES CONFIRMATION",
            promptQuestion = "Want to confirm or adjust the Body & Senses items already on your map? These sit near the center by default.",
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

        if (userMessage.isBlank()) {
            return Pair(baseQuestion, emptyList())
        }

        // Follow-up for skip / short inputs
        val trimmed = userMessage.trim()
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val isSkipOrShort = words.size <= 1 || trimmed.lowercase() in listOf("skip", "none", "nothing", "idk", "pass", "no")

        if (isSkipOrShort) {
            val followUp = "Understood. Would you like to name one specific landmark in ${currentStep.targetCategory.displayName}, or proceed to the next phase?"
            return Pair(followUp, emptyList())
        }

        // --- 1. DETERMINISTIC ENTITY EXTRACTION (PRIMARY PATH) ---
        val lowerMsg = userMessage.lowercase()
        val deterministicProposals = mutableListOf<ProposedItemCandidate>()

        fun extractIfMatch(keywords: List<String>, name: String, category: SelfMapCategory, importance: Float, note: String) {
            val matchedWord = keywords.firstOrNull { lowerMsg.contains(it) }
            if (matchedWord != null) {
                // Find the word in the original casing for a better quote
                val originalQuote = userMessage.split(Regex("\\W+")).firstOrNull { it.equals(matchedWord, ignoreCase = true) } ?: matchedWord
                deterministicProposals.add(
                    ProposedItemCandidate(
                        name = name,
                        category = category,
                        suggestedImportance = importance,
                        note = note,
                        rationale = "You mentioned \"$originalQuote\""
                    )
                )
            }
        }

        extractIfMatch(listOf("family", "wife", "kids", "children", "son", "daughter"), "Family & Kin", SelfMapCategory.PEOPLE, 0.85f, "Core family attachment")
        extractIfMatch(listOf("jesus", "lord", "faith", "prayer", "scripture", "christ"), "Faith & Spiritual Anchor", SelfMapCategory.IDEAS_LIKES, 0.85f, "Spiritual foundation")
        extractIfMatch(listOf("coding", "building", "creating", "tinkering", "dev", "architect"), "Creation & Building", SelfMapCategory.ROLES_ACCOMPLISHMENTS, 0.80f, "Maker and engineering focus")
        extractIfMatch(listOf("lifting", "weights", "body", "mind", "health", "workout", "trainer"), "Physical Conditioning", SelfMapCategory.BODY_SENSES, 0.85f, "Physical vitality")
        extractIfMatch(listOf("learning", "reading", "study", "growth"), "Continuous Learning", SelfMapCategory.IDEAS_LIKES, 0.75f, "Intellectual expansion")
        extractIfMatch(listOf("work", "job", "provider", "career", "business"), "Professional Vector", SelfMapCategory.ROLES_ACCOMPLISHMENTS, 0.75f, "Work and career responsibility")

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
            - Format strictly as a valid JSON array of objects. Keys required: "name" (string), "category" (string), "importance" (float 0.40 to 0.90), "note" (string), "rationale" (string).
            - Categories must be one of: BODY_SENSES, PEOPLE, STUFF, ROLES_ACCOMPLISHMENTS, IDEAS_LIKES, CUSTOM.
            - Output ONLY raw JSON array. No markdown code blocks, no conversational monologue, no introductory filler.
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
                        val fallbackMsg = "I heard \"${trimmed.take(30)}\"... want to try again, or switch to the form?"
                        return Pair(fallbackMsg, emptyList())
                    }
                }
                is AiResult.Failure -> {
                    val fallbackMsg = "I heard \"${trimmed.take(30)}\"... want to try again, or switch to the form?"
                    return Pair(fallbackMsg, emptyList())
                }
            }
        } else {
            val fallbackMsg = "I heard \"${trimmed.take(30)}\"... want to try again, or switch to the form?"
            return Pair(fallbackMsg, emptyList())
        }
    }

    private fun parseAiResponse(response: String): Pair<String, List<ProposedItemCandidate>> {
        val cleanText = response.replace("```json", "").replace("```", "").trim()
        val proposals = mutableListOf<ProposedItemCandidate>()
        try {
            val jsonArray = org.json.JSONArray(cleanText)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val name = obj.optString("name")
                if (name.isBlank()) continue
                
                val categoryStr = obj.optString("category").uppercase()
                val category = SelfMapCategory.entries.firstOrNull { it.name == categoryStr } ?: SelfMapCategory.CUSTOM
                
                val importance = obj.optDouble("importance", 0.65).toFloat().coerceIn(0.40f, 0.90f)
                val note = obj.optString("note").takeIf { it.isNotBlank() }
                val rationale = obj.optString("rationale").takeIf { it.isNotBlank() }
                
                proposals.add(
                    ProposedItemCandidate(
                        name = name,
                        category = category,
                        suggestedImportance = importance,
                        note = note,
                        rationale = rationale
                    )
                )
            }
        } catch (e: Exception) {
            // Parsing failed, return empty list
        }

        return Pair("Structured proposal candidates generated.", proposals)
    }

    suspend fun suggestItemForCategory(category: SelfMapCategory): ProposedItemCandidate {
        val prompt = """
            [SYSTEM: Self Map Intake Structured Extraction Engine]
            Role: Strict structured proposal generator for Self Map intake.
            Constraints:
            - NEVER inherit CYBR-TES, Socrates, or Faith Lens. No philosophy, no moralizing, no emotional invention.
            - Tone: Neutral, operational, concise.
            - Task: Suggest 1 relevant, high-value Self Map attachment node for category: ${category.displayName}.
            - Format strictly as a valid JSON array containing exactly one object. Keys required: "name" (string), "category" (string), "importance" (float 0.40 to 0.90), "note" (string), "rationale" (string).
            - The category field must be exactly: ${category.name}
            - Output ONLY raw JSON array. No markdown code blocks, no conversational monologue, no introductory filler.
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
