package com.neon.ascent.feature.dashboard.selfmap

import com.neon.ascent.core.domain.ai.AiCore
import com.neon.ascent.core.domain.ai.AiResult
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class ProposedItemCandidate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: SelfMapCategory,
    val suggestedImportance: Float,
    val note: String? = null,
    val isAccepted: Boolean = false,
    val isRejected: Boolean = false
)

data class IntakeStep(
    val stepIndex: Int,
    val title: String,
    val promptQuestion: String,
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
            promptQuestion = "Tell me a little about who you are right now. What defines your baseline identity or main focus in this moment?",
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Core Focus", category = SelfMapCategory.CUSTOM, suggestedImportance = 0.85f, note = "Primary identity anchor"),
                ProposedItemCandidate(name = "Personal Growth Drive", category = SelfMapCategory.CUSTOM, suggestedImportance = 0.75f, note = "Continuous learning and mastery")
            )
        ),
        IntakeStep(
            stepIndex = 1,
            title = "PHASE 2 // PEOPLE & CONNECTIONS",
            promptQuestion = "Who are the key people that matter most to you right now—family, partner, close allies, or dependents?",
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Inner Circle & Family", category = SelfMapCategory.PEOPLE, suggestedImportance = 0.85f, note = "Closest personal relationships"),
                ProposedItemCandidate(name = "Core Allies & Team", category = SelfMapCategory.PEOPLE, suggestedImportance = 0.70f, note = "Key professional or operational partners")
            )
        ),
        IntakeStep(
            stepIndex = 2,
            title = "PHASE 3 // STUFF & ROLES",
            promptQuestion = "What possessions, career roles, or key accomplishments would feel like a major loss if they disappeared?",
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Primary Profession / Role", category = SelfMapCategory.ROLES_ACCOMPLISHMENTS, suggestedImportance = 0.75f, note = "Core identity role and focus"),
                ProposedItemCandidate(name = "Key Workspace & Gear", category = SelfMapCategory.STUFF, suggestedImportance = 0.60f, note = "Essential operational tools and possessions")
            )
        ),
        IntakeStep(
            stepIndex = 3,
            title = "PHASE 4 // IDEAS & VALUES",
            promptQuestion = "What core values, beliefs, or strong likes/dislikes feel like a foundational part of 'you'?",
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Core Principles & Faith", category = SelfMapCategory.IDEAS_LIKES, suggestedImportance = 0.80f, note = "Guiding ethical and spiritual compass"),
                ProposedItemCandidate(name = "Autonomy & Mastery", category = SelfMapCategory.IDEAS_LIKES, suggestedImportance = 0.70f, note = "Drive for independence and continuous growth")
            )
        ),
        IntakeStep(
            stepIndex = 4,
            title = "PHASE 5 // BODY & SENSES CONFIRMATION",
            promptQuestion = "Confirm or adjust your pre-seeded Body & Senses items to anchor your map center before finalizing your canvas.",
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Body Vessel", category = SelfMapCategory.BODY_SENSES, suggestedImportance = 0.90f, note = "Core physical vessel and anchor"),
                ProposedItemCandidate(name = "Physical Health", category = SelfMapCategory.BODY_SENSES, suggestedImportance = 0.85f, note = "Energy and physical capacity")
            )
        )
    )

    suspend fun processUserTurn(
        currentStepIndex: Int,
        userMessage: String
    ): Pair<String, List<ProposedItemCandidate>> {
        val currentStep = intakeSteps.getOrElse(currentStepIndex) { intakeSteps.first() }

        if (userMessage.isBlank()) {
            return Pair(currentStep.promptQuestion, currentStep.defaultProposals)
        }

        val prompt = """
            [SYSTEM: Self Map Conversational Guide]
            You are a curious, non-judgmental cyber-mentor assisting the operator in mapping their mind's Self Map attachments.
            Maintain an objective, respectful, inquisitive tone. Do not invent emotion scores, happiness ratings, or arbitrary metrics.
            Category choices: BODY_SENSES, PEOPLE, STUFF, ROLES_ACCOMPLISHMENTS, IDEAS_LIKES, CUSTOM.
            
            Current Phase: ${currentStep.title}
            Question: "${currentStep.promptQuestion}"
            Operator Input: "$userMessage"
            
            Provide a concise 2-sentence conversational response, and propose 1 to 3 attachment items using format:
            [PROPOSAL: Name | Category | Importance (0.40 to 0.90) | Short Note]
        """.trimIndent()

        if (aiCore.isReady()) {
            when (val res = aiCore.generate(prompt, forceLocal = true)) {
                is AiResult.Success -> {
                    val (text, proposals) = parseAiResponse(res.text)
                    if (proposals.isNotEmpty()) {
                        return Pair(text, proposals)
                    }
                    return Pair(text, currentStep.defaultProposals)
                }
                is AiResult.Failure -> {
                    // Fallback to default proposals
                }
            }
        }

        val fallbackText = "Noted, Operator. Here are proposed attachment nodes based on your input. You can accept, edit, or reject each before placing them on your canvas."
        return Pair(fallbackText, currentStep.defaultProposals)
    }

    private fun parseAiResponse(response: String): Pair<String, List<ProposedItemCandidate>> {
        val proposalRegex = Regex("\\[PROPOSAL: (.*?) \\| (.*?) \\| (.*?) \\| (.*?)\\]")
        val proposals = proposalRegex.findAll(response).mapNotNull { match ->
            val name = match.groupValues[1].trim()
            val categoryStr = match.groupValues[2].trim().uppercase()
            val importance = match.groupValues[3].trim().toFloatOrNull()?.coerceIn(0.40f, 0.90f) ?: 0.65f
            val note = match.groupValues[4].trim()

            val cat = SelfMapCategory.entries.firstOrNull { it.name == categoryStr }
                ?: SelfMapCategory.CUSTOM

            ProposedItemCandidate(
                name = name,
                category = cat,
                suggestedImportance = importance,
                note = note
            )
        }.toList()

        val cleanText = response.replace(proposalRegex, "").trim()
        val text = cleanText.ifBlank { "I've structured potential attachments based on your response." }
        return Pair(text, proposals)
    }

    suspend fun suggestItemForCategory(category: SelfMapCategory): ProposedItemCandidate {
        val prompt = """
            [SYSTEM: Self Map Quick Mentor Suggestion]
            Suggest 1 relevant, high-value Self Map attachment node for category: ${category.displayName} (${category.description}).
            Format strictly:
            [PROPOSAL: Name | ${category.name} | Importance (0.40 to 0.90) | Short Note]
        """.trimIndent()

        if (aiCore.isReady()) {
            when (val res = aiCore.generate(prompt, forceLocal = true)) {
                is AiResult.Success -> {
                    val (_, proposals) = parseAiResponse(res.text)
                    if (proposals.isNotEmpty()) {
                        return proposals.first()
                    }
                }
                is AiResult.Failure -> {}
            }
        }

        return when (category) {
            SelfMapCategory.BODY_SENSES -> ProposedItemCandidate(name = "Physical Health & Energy", category = category, suggestedImportance = 0.85f, note = "Core operating capacity")
            SelfMapCategory.PEOPLE -> ProposedItemCandidate(name = "Inner Circle Allies", category = category, suggestedImportance = 0.80f, note = "Primary personal bonds")
            SelfMapCategory.STUFF -> ProposedItemCandidate(name = "Primary Workspace", category = category, suggestedImportance = 0.65f, note = "Operational environment")
            SelfMapCategory.ROLES_ACCOMPLISHMENTS -> ProposedItemCandidate(name = "Lead Operational Role", category = category, suggestedImportance = 0.75f, note = "Main focus area")
            SelfMapCategory.IDEAS_LIKES -> ProposedItemCandidate(name = "Autonomy & Mastery", category = category, suggestedImportance = 0.70f, note = "Core guiding value")
            SelfMapCategory.CUSTOM -> ProposedItemCandidate(name = "Personal Landmark", category = category, suggestedImportance = 0.60f, note = "Custom attachment")
        }
    }
}
