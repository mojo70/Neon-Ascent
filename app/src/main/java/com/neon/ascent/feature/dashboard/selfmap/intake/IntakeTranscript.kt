package com.neon.ascent.feature.dashboard.selfmap.intake

import com.neon.ascent.core.domain.selfmap.SelfMapCategory
import com.neon.ascent.feature.dashboard.selfmap.ChatTurnMessage
import com.neon.ascent.feature.dashboard.selfmap.MessageSender
import com.neon.ascent.feature.dashboard.selfmap.ProposedItemCandidate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Compact thread stored with the intake session so a reopen shows the same lines and cards.
 */
object IntakeTranscriptCodec {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun encode(messages: List<ChatTurnMessage>): String {
        if (messages.isEmpty()) return ""
        val wire = messages.map { message ->
            WireMessage(
                sender = message.sender.name,
                text = message.text,
                proposals = message.proposals.map { proposal ->
                    WireProposal(
                        id = proposal.id,
                        name = proposal.name,
                        category = proposal.category.name,
                        importance = proposal.suggestedImportance,
                        note = proposal.note,
                        rationale = proposal.rationale,
                    )
                },
            )
        }
        return json.encodeToString(wire)
    }

    fun decode(raw: String): List<ChatTurnMessage> {
        if (raw.isBlank()) return emptyList()
        return try {
            json.decodeFromString<List<WireMessage>>(raw).map { message ->
                val sender = runCatching { MessageSender.valueOf(message.sender) }.getOrDefault(MessageSender.GUIDE)
                ChatTurnMessage(
                    sender = sender,
                    text = message.text,
                    proposals = message.proposals.map { proposal ->
                        val category = SelfMapCategory.entries.firstOrNull { it.name == proposal.category }
                            ?: SelfMapCategory.CUSTOM
                        ProposedItemCandidate(
                            id = proposal.id,
                            name = proposal.name,
                            category = category,
                            suggestedImportance = proposal.importance,
                            note = proposal.note,
                            rationale = proposal.rationale,
                        )
                    },
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Serializable
    private data class WireMessage(
        val sender: String,
        val text: String,
        val proposals: List<WireProposal> = emptyList(),
    )

    @Serializable
    private data class WireProposal(
        val id: String,
        val name: String,
        val category: String,
        val importance: Float,
        val note: String? = null,
        val rationale: String? = null,
    )
}
