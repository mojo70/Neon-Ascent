package com.neon.ascent.core.domain.chronicle

data class ChronicleEntry(
    val source: String,       // "journal", "shard", "fragment", "chat"
    val sourceId: String,     // ID from the originating app domain model
    val wing: String = "CHRONICLE",
    val room: String = "ORIGIN",
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val hearted: Boolean = false,
    val metadata: String? = null
)
