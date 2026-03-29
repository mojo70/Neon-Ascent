package com.neon.ascent.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val id: String,
    val text: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isHearted: Boolean = false
)
