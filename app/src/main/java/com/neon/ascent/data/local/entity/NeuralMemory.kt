package com.neon.ascent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "neural_memories")
data class NeuralMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wing: String,       // e.g., "HEALTH", "MISSIONS", "DIALOGUE"
    val room: String,       // e.g., "HRV_STATS", "DIRECTIVE_ALPHA", "SOCRATIC_SESSION"
    val content: String,    // Verbatim storage of the data/interaction
    val importance: Float = 0.5f, // 0.0 to 1.0 importance factor
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String? = null // JSON string for extra contextual tags
)
