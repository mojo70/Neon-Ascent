package com.neon.ascent.domain.model

import com.neon.ascent.data.local.entity.UserStoryEntity

data class UserStory(
    val bio: String = "",
    val grandAspirations: List<String> = emptyList(),
    val specialAttributes: Map<String, Int> = emptyMap(),
    val cyberLore: String = ""
)

fun UserStoryEntity.toDomain() = UserStory(
    bio = bio,
    grandAspirations = grandAspirations,
    specialAttributes = specialAttributes,
    cyberLore = cyberLore
)

fun UserStory.toEntity() = UserStoryEntity(
    bio = bio,
    grandAspirations = grandAspirations,
    specialAttributes = specialAttributes,
    cyberLore = cyberLore
)
