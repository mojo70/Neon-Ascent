package com.neon.ascent.model

import kotlinx.serialization.Serializable

@Serializable
data class CorpoNode(
    val name: String,
    val ticker: String,
    val slogan: String,
    val profile: String,
    val securityTier: DifficultyTier,
    val stockPrice: Double,
    val stockChange: Double,
    val stockChangePercent: Double,
    val marketCap: String,
    val highlights: List<String> = emptyList(),
    val ceo: String = "",
    val earningsReport: String? = null,
    val investorDeck: List<InvestorSlide>? = null
)

@Serializable
data class InvestorSlide(
    val title: String,
    val subtitle: String? = null,
    val body: String
)
