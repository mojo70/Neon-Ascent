package com.neon.ascent.core.lore.data

import kotlinx.serialization.Serializable

@Serializable
data class Megacorp(
    val id: String,                    // e.g. "panopticon"
    val name: String,
    val slogan: String,                // "Do Evil."
    val motto: String?,
    val description: String,
    val ceo: CEO,
    val achievements: List<Achievement> = emptyList(),
    val divisions: List<Division> = emptyList(),
    val stockTicker: StockData,
    val flavorText: List<String> = emptyList(),      // rotating propaganda lines
    val financials: Financials? = null,
    val documents: List<LoreDocumentReference> = emptyList(),
    val dossier: Dossier? = null,
    val hackingDifficulty: String? = null,
    val hasNativeBlackICE: Boolean = false
)

@Serializable
data class Dossier(
    val psychProfile: String? = null,
    val attackVectors: String? = null,
    val leakedMemos: List<String> = emptyList(),
    val rivalShade: List<RivalShade> = emptyList(),
    val quickhackVault: List<QuickhackReward> = emptyList(),
    val blackmail: List<String> = emptyList(),
    val executiveBackdoor: String? = null,
    val oneTimeEddies: Int? = null,
    val permanentPerk: String? = null,
    val iceBlueprints: List<String> = emptyList()
)

@Serializable
data class RivalShade(
    val targetCorpoId: String,
    val intel: String
)

@Serializable
data class QuickhackReward(
    val id: String? = null,
    val name: String,
    val description: String? = null,
    val effect: String? = null,
    val rarity: String? = null,
    val tier: String? = null,
    val type: String? = null,
    val flavor: String? = null
)

@Serializable
data class Financials(
    val revenue: String,
    val profit: String,
    val eps: String,
    val growthYoY: String
)

@Serializable
data class LoreDocumentReference(
    val id: String,
    val title: String,
    val type: String,
    val path: String
)

@Serializable
data class CEO(
    val name: String,
    val netHandle: String? = null,
    val title: String,
    val personality: String,           // "cold_omniscient", "degen_space_bro"
    val gemmaPromptPath: String? = null, // assets/lore/ceo_prompts/panopticon.txt
    val gemmaPrompt: String? = null
)

@Serializable
data class Division(
    val name: String,
    val focus: String,
    val revenueShare: Float,
    val tagline: String? = null,
    val revenue: String? = null,
    val growth: String? = null,
    val notes: String? = null
)

@Serializable
data class Achievement(
    val year: String,
    val title: String,
    val description: String
)

@Serializable
data class StockData(
    val symbol: String,
    val price: Double,
    val change: Double,
    val volume: Long
)
