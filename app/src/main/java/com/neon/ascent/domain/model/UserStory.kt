package com.neon.ascent.domain.model

import com.neon.ascent.data.local.entity.LoreChapter
import com.neon.ascent.data.local.entity.UserStoryEntity

data class UserStory(
    val bio: String = "",
    val grandAspirations: List<String> = emptyList(),
    val specialAttributes: Map<String, Int> = emptyMap(),
    val cyberLore: String = "",
    val weeklyChapters: List<LoreChapter> = emptyList(),
    val lastWeeklyUpdate: Long = 0L
)

fun UserStoryEntity.toDomain() = UserStory(
    bio = bio,
    grandAspirations = grandAspirations,
    specialAttributes = specialAttributes,
    cyberLore = cyberLore,
    weeklyChapters = weeklyChapters,
    lastWeeklyUpdate = lastWeeklyUpdate
)

fun UserStory.toEntity() = UserStoryEntity(
    bio = bio,
    grandAspirations = grandAspirations,
    specialAttributes = specialAttributes,
    cyberLore = cyberLore,
    weeklyChapters = weeklyChapters,
    lastWeeklyUpdate = lastWeeklyUpdate
)
