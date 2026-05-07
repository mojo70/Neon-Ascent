package com.neon.ascent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val aspirationLink: String = "",         // Reference to grand aspiration
    val targetValue: Float,
    val currentValue: Float = 0f,
    val unit: String = "hours",              // hours, days, sessions, years_reduced, etc.
    val deadline: Long? = null,              // Timestamp or null for open-ended
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
