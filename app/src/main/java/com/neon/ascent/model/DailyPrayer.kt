package com.neon.ascent.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_prayers")
data class DailyPrayer(
    @PrimaryKey val day: Int,
    val prayer: String,
    val scripture: String,
    val reflectionPrompt: String = "How does this strengthen your firewall today?"
)
