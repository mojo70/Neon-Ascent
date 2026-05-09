package com.neon.ascent.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neon.ascent.core.domain.AttributeType

@Entity(tableName = "new_goals") // Using a different name to avoid conflict for now
data class GoalEntity(
    @PrimaryKey val id: String,
    val type: String, // ASPIRATION, MISSION, TASK, HABIT
    val title: String,
    val description: String,
    val currentProgress: Float,
    val targetProgress: Float,
    val percentile: Int?,
    val attributeType: AttributeType,
    val frequency: String? = null, // For habits
    val archetype: String? = null, // For missions
    val isCompleted: Boolean = false,
    val parentGoalId: String? = null // For linking
)
