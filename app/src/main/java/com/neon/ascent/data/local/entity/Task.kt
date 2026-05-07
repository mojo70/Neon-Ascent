package com.neon.ascent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neon.ascent.domain.model.SpecialType
import java.util.UUID

@Entity(tableName = "task_entities")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val goalId: String,                      // Foreign key
    val title: String,
    val description: String = "",
    val frequency: String = "DAILY",         // DAILY, WEEKDAYS, WEEKLY, CUSTOM
    val estimatedMinutes: Int = 10,
    val completedDates: List<String> = emptyList(),       // JSON array of yyyy-MM-dd strings
    val linkedSpecial: SpecialType? = null,               // This task supports testing this stat
    val suggestsRetestAfterDays: Int = 0,                 // e.g. 30 days → "Time to re-test Strength"
    val sortOrder: Int = 0,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
