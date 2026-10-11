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
            title = "PHASE 1 // CORE BODY & SENSES",
            promptQuestion = "Tell me a little about who you are right now. Let's start with your core physical vessel items to anchor your map center.",
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Body", category = SelfMapCategory.BODY_SENSES, suggestedImportance = 0.9f, note = "Core physical vessel and anchor"),
                ProposedItemCandidate(name = "Physical Health", category = SelfMapCategory.BODY_SENSES, suggestedImportance = 0.85f, note = "Energy and physical operating capacity"),
                ProposedItemCandidate(name = "Appearance & Form", category = SelfMapCategory.BODY_SENSES, suggestedImportance = 0.75f, note = "Visual presentation and body identity")
            )
        ),
        IntakeStep(
            stepIndex = 1,
            title = "PHASE 2 // PEOPLE & CONNECTIONS",
            promptQuestion = "Who are the key people that matter most to you right now—family, partner, close allies, or dependents?",
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Inner Circle & Family", category = SelfMapCategory.PEOPLE, suggestedImportance = 0.85f, note = "Closest personal relationships"),
                ProposedItemCandidate(name = "Core Allies & Team", category = SelfMapCategory.PEOPLE, suggestedImportance = 0.7f, note = "Key professional or operational partners")
            )
        ),
        IntakeStep(
            stepIndex = 2,
            title = "PHASE 3 // STUFF & ROLES",
            promptQuestion = "What possessions, career roles, or key accomplishments would feel like a major loss if they disappeared?",
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Primary Profession / Role", category = SelfMapCategory.ROLES_ACCOMPLISHMENTS, suggestedImportance = 0.75f, note = "Core identity role and focus"),
                ProposedItemCandidate(name = "Key Workspace & Gear", category = SelfMapCategory.STUFF, suggestedImportance = 0.6f, note = "Essential operational tools and possessions")
            )
        ),
        IntakeStep(
            stepIndex = 3,
            title = "PHASE 4 // IDEAS & VALUES",
            promptQuestion = "What core values, beliefs, or strong likes/dislikes feel like a foundational part of 'you'?",
            defaultProposals = listOf(
                ProposedItemCandidate(name = "Core Principles & Faith", category = SelfMapCategory.IDEAS_LIKES, suggestedImportance = 0.8f, note = "Guiding ethical and spiritual compass"),
                ProposedItemCandidate(name = "Autonomy & Mastery", category = SelfMapCategory.IDEAS_LIKES, suggestedImportance = 0.7f, note = "Drive for independence and continuous growth")
            )
        ),
        IntakeStep(
            stepIndex = 4,
            title = "PHASE 5 // SYNTHESIS",
            promptQuestion = "Your initial Self Map synthesis is ready. Review your accepted items below and place them on your holographic canvas.",
            defaultProposals = emptyList()
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
            You are an empathetic, insightful cyber-mentor assisting the operator in mapping their mind's Self Map attachments.
            Category choices: BODY_SENSES, PEOPLE, STUFF, ROLES_ACCOMPLISHMENTS, IDEAS_LIKES, CUSTOM.
            
            Current Phase: ${currentStep.title}
            Question: "${currentStep.promptQuestion}"
            Operator Input: "$userMessage"
            
            Provide a concise 2-sentence conversational response, and propose 1 to 3 items using format:
            [PROPOSAL: Name | Category | Importance (0.1 to 0.95) | Short Note]
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
                    // Fallback to default proposals with polite message
                }
            }
        }

        val fallbackText = "Noted, Operator. Here are suggested attachment nodes based on your input. You can accept, edit, or reject each before placing them on your canvas."
        return Pair(fallbackText, currentStep.defaultProposals)
    }

    private fun parseAiResponse(response: String): Pair<String, List<ProposedItemCandidate>> {
        val proposalRegex = Regex("\\[PROPOSAL: (.*?) \\| (.*?) \\| (.*?) \\| (.*?)\\]")
        val proposals = proposalRegex.findAll(response).mapNotNull { match ->
            val name = match.groupValues[1].trim()
            val categoryStr = match.groupValues[2].trim().uppercase()
            val importance = match.groupValues[3].trim().toFloatOrNull()?.coerceIn(0.1f, 0.95f) ?: 0.6f
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
}
