package com.neon.ascent.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_story")
data class UserStoryEntity(
    @PrimaryKey val id: String = "main_user_story",
    val bio: String = "",                    // Free text story
    val grandAspirations: List<String> = emptyList(),     // JSON array of strings
    val specialAttributes: Map<String, Int> = emptyMap(),    // JSON map e.g. {"Openness": 85, "Focus": 70}
    val cyberLore: String = "",
    val weeklyChapters: List<LoreChapter> = emptyList(),
    val lastWeeklyUpdate: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class LoreChapter(
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isHacked: Boolean = false
)
