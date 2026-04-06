package com.neon.ascent.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sayings")
data class Saying(
    @PrimaryKey val id: String,
    val text: String,
    val category: String,
    val engagementScore: Int,
    val isEnabled: Boolean = true
)
