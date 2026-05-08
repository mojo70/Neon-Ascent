package com.neon.ascent.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
enum class Rarity {
    COMMON, RARE, EPIC, LEGENDARY
}

@Entity(tableName = "quick_hack_components")
@Serializable
data class QuickHackComponent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rarity: Rarity,
    val quantity: Int = 0
)

@Serializable
enum class QuickHackType {
    ICE_BREAKER, // Common
    GHOST_PULSE, // Rare
    NEURAL_OVERRIDE, // Epic
    SYSTEM_BURN // Legendary
}

@Entity(tableName = "quick_hacks")
@Serializable
data class QuickHack(
    @PrimaryKey val id: String,
    val name: String,
    val type: QuickHackType,
    val rarity: Rarity,
    val description: String,
    val cooldownHours: Int,
    val lastUsedTimestamp: Long = 0,
    val isOneTimeUse: Boolean = false
)

data class HackingReward(
    val xp: Long,
    val eddies: Int,
    val fragments: Int,
    val components: List<Pair<Rarity, Int>>,
    val craftingAvailable: Boolean = false
)
