package com.neon.ascent.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_character")
data class UserCharacter(
    @PrimaryKey val id: Int = 0,
    val name: String,
    val netrunnerName: String? = null,
    val sex: String,
    val dob: String,
    val units: String,
    val heightFeet: String? = null,
    val heightInches: String? = null,
    val heightCm: String? = null,
    val weight: String,
    val somatotype: Float,
    val mbti: String? = null,
    val alignment: String? = null,
    val archetype: String? = null,
    val level: Int = 1,
    val experience: Long = 0,
    val strength: Int? = null,
    val perception: Int? = null,
    val endurance: Int? = null,
    val charisma: Int? = null,
    val agility: Int? = null,
    val luck: Int? = null,
    val holyGhost: Int? = null,
    val avatarPath: String? = null,
    val isCreationComplete: Boolean = false,
    val neuralLoad: Float = 0.2f // Default starting load
)
