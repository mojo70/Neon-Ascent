package com.neon.ascent.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val isLongTerm: Boolean = true,
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, FAILED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val questId: String?, // Null if it's a standalone daily task
    val description: String,
    val isCompleted: Boolean = false,
    val isDaily: Boolean = false,
    val dueDate: Long? = null,
    val aiBreakdownNotes: String? = null
)
