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
    val achievements: List<Achievement>,
    val divisions: List<Division>,
    val stockTicker: StockData,
    val flavorText: List<String>,      // rotating propaganda lines
    val financials: Financials? = null,
    val documents: List<LoreDocumentReference> = emptyList()
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
    val title: String,
    val personality: String,           // "cold_omniscient", "degen_space_bro"
    val gemmaPromptPath: String        // assets/lore/ceo_prompts/panopticon.txt
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
