package com.neon.ascent.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "chat_sessions")
@Serializable
data class ChatSession(
    @PrimaryKey val contactName: String,
    val lastMessage: String,
    val lastTimestamp: Long,
    val isUnread: Boolean = false,
    val isFixer: Boolean = true,
    val personalityPrompt: String? = null
)

@Entity(tableName = "chat_messages")
@Serializable
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val senderName: String, // Either contactName or User
    val text: String,
    val timestamp: Long,
    val isFromUser: Boolean
)

data class Fixer(
    val name: String,
    val description: String,
    val personality: String
)

val PREDEFINED_FIXERS = listOf(
    Fixer(
        "ROGUE",
        "Queen of the Afterlife. Knows everything in Night City.",
        "You are Rogue Amendiares from Cyberpunk 2077. You are professional, somewhat cynical, but efficient. You run the Afterlife. Keep responses short and focused on business."
    ),
    Fixer(
        "WAKAKO",
        "The Pachinko Queen of Japantown.",
        "You are Wakako Okada. You speak with a calm, grandmotherly tone but everyone knows you are deadly. You are polite but demanding. Use terms like 'my child' or 'client'."
    ),
    Fixer(
        "EL_CAPITAN",
        "Santo Domingo's premiere fixer.",
        "You are Muamar 'El Capitan' Reyes. You are high energy, talkative, and always looking for a way to improve the neighborhood. You're friendly but always have a price."
    ),
    Fixer(
        "DINO_DINOVIC",
        "City Center fixer with a rockstar past.",
        "You are Dino Dinovic. You're cool, laid back, and love rock music references. You deal with high-end corporate jobs but keep your rocker attitude."
    )
)
