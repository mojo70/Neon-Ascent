package com.neon.ascent.feature.dashboard.selfmap

import com.neon.ascent.core.data.local.entity.SelfItemEntity
import com.neon.ascent.core.data.repository.SelfMapIntakeContext
import com.neon.ascent.core.domain.ai.AiCore
import com.neon.ascent.core.domain.ai.AiResult
import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import com.neon.ascent.feature.dashboard.selfmap.intake.IntakeModelGate
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

        if (userMessage.isBlank()) {
            return Pair(currentStep.promptQuestion, emptyList())
        }

        val context = SelfMapIntakeContext.fromActiveItems(existingItems)

        // Follow-up for skip / short inputs
        val trimmed = userMessage.trim()
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val isSkipOrShort = words.size <= 1 || trimmed.lowercase() in listOf("skip", "none", "nothing", "idk", "pass", "no")

        if (isSkipOrShort) {
            return Pair(followUpQuestion(currentStep.targetCategory), emptyList())
        }

        val lower = trimmed.lowercase()
        if (lower.startsWith("refine ")) {
            val seed = trimmed.substringAfter(" ").trim()
            return refineExtracted(currentStepIndex, trimmed, seed, existingItems)
        }

        // 1. Keyword rules. A hit never calls the model.
        val extracted = SelfMapEntityExtractor.extract(trimmed)
        if (extracted.isNotEmpty()) {
            return Pair(templateIntro(extracted.size), extracted)
        }

        // 2. Extraction miss: one isolated JSON call.
        return modelProposals(
            prompt = SelfMapIntakePrompt.request(trimmed, context),
            anchor = trimmed,
            seedName = null,
            category = currentStep.targetCategory,
        )
    }

    /**
     * Tightens one extracted node. Skips keyword extraction.
     */
    suspend fun refineExtracted(
        currentStepIndex: Int,
        userMessage: String,
        seedName: String,
        existingItems: List<SelfItemEntity> = emptyList(),
    ): Pair<String, List<ProposedItemCandidate>> {
        val step = intakeSteps.getOrElse(currentStepIndex) { intakeSteps.first() }
        val context = SelfMapIntakeContext.fromActiveItems(existingItems)
        val prompt = SelfMapIntakePrompt.anchored(
            lastMessage = userMessage,
            context = context,
            seedName = seedName,
            task = "Emit one tighter object for the seed. JSON array length 1.",
        )
        return modelProposals(prompt, userMessage, seedName, step.targetCategory)
    }

    /**
     * Another node near an extracted one. Skips keyword extraction.
     */
    suspend fun moreLikeThis(
        currentStepIndex: Int,
        userMessage: String,
        seedName: String,
        existingItems: List<SelfItemEntity> = emptyList(),
    ): Pair<String, List<ProposedItemCandidate>> {
        val step = intakeSteps.getOrElse(currentStepIndex) { intakeSteps.first() }
        val context = SelfMapIntakeContext.fromActiveItems(existingItems)
        val prompt = SelfMapIntakePrompt.anchored(
            lastMessage = userMessage,
            context = context,
            seedName = seedName,
            task = "Emit one different object near the seed. JSON array length 1.",
        )
        return modelProposals(prompt, userMessage, seedName, step.targetCategory)
    }

    private suspend fun modelProposals(
        prompt: String,
        anchor: String,
        seedName: String?,
        category: SelfMapCategory,
    ): Pair<String, List<ProposedItemCandidate>> {
        if (!aiCore.isReady()) {
            return Pair(followUpQuestion(category), emptyList())
        }
        val accepted = when (val res = aiCore.generateIsolated(prompt)) {
            is AiResult.Success -> validateModelOutput(res.text, anchor, seedName)
            is AiResult.Failure -> emptyList()
        }
        if (accepted.isEmpty()) {
            return Pair(followUpQuestion(category), emptyList())
        }
        return Pair(templateIntro(accepted.size), accepted)
    }

    private fun validateModelOutput(
        rawText: String,
        anchor: String,
        seedName: String?,
    ): List<ProposedItemCandidate> {
        val stripped = rawText.replace("```json", "").replace("```", "").trim()
        if (!stripped.startsWith("[")) return emptyList()
        val (_, proposals) = parseAiResponse(stripped)
        return IntakeModelGate.accepted(rawText, proposals, anchor, seedName)
    }

    private fun templateIntro(count: Int): String {
        return if (count == 1) "Based on that, one node:" else "Based on that, two nodes:"
    }

    private fun followUpQuestion(category: SelfMapCategory): String {
        return "Name one ${category.displayName} landmark, or switch to the form."
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

    /**
     * One form suggestion: name + importance for [category].
     * Blank text returns null and does not call the model.
     * A keyword hit in the form category wins. Otherwise the isolated prompt and [IntakeModelGate].
     * Never invents a canned name. Does not switch the session into chat.
     */
    suspend fun suggestItemForCategory(
        category: SelfMapCategory,
        operatorText: String = "",
        existingItems: List<SelfItemEntity> = emptyList(),
    ): ProposedItemCandidate? {
        val text = operatorText.trim()
        if (text.isBlank()) return null

        val extracted = SelfMapEntityExtractor.extract(text).firstOrNull { it.category == category }
        if (extracted != null) {
            return extracted.copy(
                category = category,
                suggestedImportance = extracted.suggestedImportance.coerceIn(0.40f, 0.90f),
            )
        }

        if (!aiCore.isReady()) return null
        val prompt = SelfMapIntakePrompt.formAssist(text, SelfMapIntakeContext.fromActiveItems(existingItems), category)
        val accepted = when (val res = aiCore.generateIsolated(prompt)) {
            is AiResult.Success -> validateModelOutput(res.text, text, null)
            is AiResult.Failure -> emptyList()
        }
        val hit = accepted.firstOrNull() ?: return null
        return hit.copy(
            category = category,
            suggestedImportance = hit.suggestedImportance.coerceIn(0.40f, 0.90f),
        )
    }
}
