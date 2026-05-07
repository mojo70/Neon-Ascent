package com.neon.ascent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habit_metrics")
data class HabitMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,           // yyyy-MM-dd
    val taskId: String,
    val goalId: String,
    val completed: Boolean,
    val minutesSpent: Int = 0,
    val notes: String = ""
)
