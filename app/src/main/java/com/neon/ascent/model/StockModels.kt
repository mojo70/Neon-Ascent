package com.neon.ascent.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
data class StockCandleResponse(
    val c: List<Float> = emptyList(), // Close
    val h: List<Float> = emptyList(), // High
    val l: List<Float> = emptyList(), // Low
    val o: List<Float> = emptyList(), // Open
    val t: List<Long> = emptyList(),  // Timestamp
    val v: List<Long> = emptyList(),  // Volume
    val s: String = ""                // Status
)

@Serializable
data class StockQuote(
    val c: Float,  // Current price
    val d: Float,  // Change
    val dp: Float, // Percent change
    val h: Float,  // High of the day
    val l: Float,  // Low of the day
    val o: Float,  // Open price of the day
    val pc: Float  // Previous close price
)

@Serializable
data class MarketStatus(
    val isOpen: Boolean,
    val timezone: String,
    val session: String? = null // 'pre-market', 'regular', 'post-market'
)

@Serializable
data class StockFinancials(
    val metric: Map<String, String> = emptyMap(),
    val series: Map<String, Map<String, List<Map<String, String>>>> = emptyMap()
)

@Serializable
data class EarningsResponse(
    val symbol: String,
    val earningsCalendar: List<EarningsEntry> = emptyList()
)

@Serializable
data class EarningsEntry(
    val date: String,
    val epsActual: Float?,
    val epsEstimate: Float?,
    val hour: String?,
    val quarter: Int,
    val year: Int
)

@Entity(tableName = "watchlist")
data class WatchlistItem(
    @PrimaryKey val symbol: String,
    val name: String,
    val isCrypto: Boolean = false
)
