package com.neon.ascent.feature.dashboard.selfmap

import com.neon.ascent.core.data.local.entity.SelfItemEntity
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

        val baseQuestion = if (isCovered && userMessage.isBlank()) {
            "I notice you already have items mapped in ${currentStep.targetCategory.displayName}. Would you like to refine or add more in this category, or proceed?"
        } else {
            currentStep.promptQuestion
        }

        if (userMessage.isBlank()) {
            return Pair(baseQuestion, currentStep.defaultProposals)
        }

        val prompt = """
            [SYSTEM: Self Map Structured Conversational Agent]
            You are a concise, curious, non-judgmental cyber-mentor assisting the operator in mapping their Self Map attachments.
            Tone: short, objective, respectful, inquisitive, light cyber. No moralizing, no Socrates, no emotional invention.
            Category choices: BODY_SENSES, PEOPLE, STUFF, ROLES_ACCOMPLISHMENTS, IDEAS_LIKES, CUSTOM.
            
            Current Phase: ${currentStep.title}
            Question: "$baseQuestion"
            Operator Input: "$userMessage"
            
            Provide a concise 1-2 sentence conversational response, and propose 1 to 3 attachment items strictly adhering to schema:
            [PROPOSAL: Name | Category | Importance (0.40 to 0.90) | Note | Rationale]
            - Name: required string
            - Category: valid SelfMapCategory enum name
            - Importance: Float between 0.40 and 0.90
            - Note: optional short description
            - Rationale: optional short grounded reason
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

        val fallbackText = "Acknowledged, Operator. Here are structured attachment proposals based on your input."
        return Pair(fallbackText, currentStep.defaultProposals)
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
            [SYSTEM: Self Map Quick Mentor Suggestion]
            Suggest 1 relevant, high-value Self Map attachment node for category: ${category.displayName}.
            Format strictly:
            [PROPOSAL: Name | ${category.name} | Importance (0.40 to 0.90) | Short Note | Grounded Rationale]
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
            SelfMapCategory.BODY_SENSES -> ProposedItemCandidate(name = "Physical Health & Energy", category = category, suggestedImportance = 0.85f, note = "Core operating capacity", rationale = "Baseline physiological container")
            SelfMapCategory.PEOPLE -> ProposedItemCandidate(name = "Inner Circle Allies", category = category, suggestedImportance = 0.80f, note = "Primary personal bonds", rationale = "Key social security network")
            SelfMapCategory.STUFF -> ProposedItemCandidate(name = "Primary Workspace", category = category, suggestedImportance = 0.65f, note = "Operational environment", rationale = "Execution leverage")
            SelfMapCategory.ROLES_ACCOMPLISHMENTS -> ProposedItemCandidate(name = "Lead Operational Role", category = category, suggestedImportance = 0.75f, note = "Main focus area", rationale = "Core societal contribution")
            SelfMapCategory.IDEAS_LIKES -> ProposedItemCandidate(name = "Autonomy & Mastery", category = category, suggestedImportance = 0.70f, note = "Core guiding value", rationale = "Intrinsic psychological driver")
            SelfMapCategory.CUSTOM -> ProposedItemCandidate(name = "Personal Landmark", category = category, suggestedImportance = 0.60f, note = "Custom attachment", rationale = "Self-defined pillar")
        }
    }
}
