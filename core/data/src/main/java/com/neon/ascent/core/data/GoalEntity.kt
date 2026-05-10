package com.neon.ascent.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.neon.ascent.core.domain.model.SpecialType

@Entity(
    tableName = "goals",
    indices = [
        Index("type"),
        Index("parentAspirationId"),
        Index("expiresAtMillis"),
        Index("lastCompletedMillis")
    ]
)
data class GoalEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val description: String,
    val linkedAttributes: List<SpecialType>,
    val progressCurrent: Double,
    val progressTarget: Double,
    val xpContributed: Int = 0,
    
    // Aspiration specific
    val targetDateMillis: Long? = null,
    val status: String? = "ACTIVE",
    
    // Mission specific
    val expiresAtMillis: Long? = null,
    val parentAspirationId: String? = null,
    
    // Habit specific
    val recurrenceType: String? = null,
    val recurrenceDays: List<String>? = null,
    val streak: Int = 0,
    val lastCompletedMillis: Long? = null,
    
    // Task specific
    val parentGoalId: String? = null
)
