package com.neon.ascent.feature.cyberdeck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neon.ascent.BuildConfig
import com.neon.ascent.data.local.StockDao
import com.neon.ascent.data.remote.StockApi
import com.neon.ascent.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class StockViewModel @Inject constructor(
    private val stockApi: StockApi,
    private val stockDao: StockDao
) : ViewModel() {

    private val _candleData = MutableStateFlow<StockCandleResponse?>(null)
    val candleData: StateFlow<StockCandleResponse?> = _candleData.asStateFlow()

    private val _financials = MutableStateFlow<StockFinancials?>(null)
    val financials: StateFlow<StockFinancials?> = _financials.asStateFlow()

    private val _earnings = MutableStateFlow<EarningsEntry?>(null)
    val earnings: StateFlow<EarningsEntry?> = _earnings.asStateFlow()

    private val _selectedSymbol = MutableStateFlow<String?>(null)
    val selectedSymbol: StateFlow<String?> = _selectedSymbol.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _quoteData = MutableStateFlow<Map<String, StockQuote>>(emptyMap())
    val quoteData: StateFlow<Map<String, StockQuote>> = _quoteData.asStateFlow()

    val watchlist = stockDao.getWatchlist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectSymbol(symbol: String?, isCrypto: Boolean = false) {
        _errorMessage.value = null
        if (symbol == null || symbol == "null") {
            _selectedSymbol.value = null
            _candleData.value = null
            _financials.value = null
            _earnings.value = null
            return
        }
        
        _selectedSymbol.value = symbol
        // Clear previous data to show loading state
        _candleData.value = null
        _financials.value = null
        _earnings.value = null
        
        fetchCandles(symbol, "D", 30)
        fetchExtraData(symbol)
    }

    private fun fetchExtraData(symbol: String) {
        if (symbol == "EDS") return
        viewModelScope.launch {
            try {
                _financials.value = stockApi.getFinancials(symbol, token = BuildConfig.FINNHUB_API_KEY)
                val earningsResp = stockApi.getEarnings(symbol, token = BuildConfig.FINNHUB_API_KEY)
                _earnings.value = earningsResp.earningsCalendar.firstOrNull()
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Financial Data Error: ${e.message}"
            }
        }
    }

    fun fetchCandles(symbol: String, resolution: String, daysBack: Long) {
        if (symbol == "EDS" || symbol == "null") {
            if (symbol == "EDS") _candleData.value = generateEdsCandles()
            return
        }
        viewModelScope.launch {
            try {
                _errorMessage.value = null
                val to = Instant.now().epochSecond
                val from = Instant.now().minus(daysBack, ChronoUnit.DAYS).epochSecond
                val response = stockApi.getCandles(symbol, resolution, from, to, BuildConfig.FINNHUB_API_KEY)
                _candleData.value = response
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Market Data Error: ${e.message}"
            }
        }
    }

    private fun generateEdsCandles(): StockCandleResponse {
        val count = 30
        val base = 1.42f
        return StockCandleResponse(
            c = List(count) { base + (it * 0.001f) },
            h = List(count) { base + (it * 0.001f) + 0.005f },
            l = List(count) { base + (it * 0.001f) - 0.005f },
            o = List(count) { base + (it * 0.001f) - 0.002f },
            t = List(count) { Instant.now().minus((count - it).toLong(), ChronoUnit.DAYS).epochSecond },
            s = "ok"
        )
    }

    fun fetchWatchlistQuotes() {
        viewModelScope.launch {
            watchlist.value.forEach { item ->
                if (item.symbol == "EDS") {
                    _quoteData.update { it + ("EDS" to StockQuote(1.42f, 0.05f, 3.6f, 1.45f, 1.38f, 1.42f, 1.37f)) }
                    return@forEach
                }
                try {
                    val quote = stockApi.getQuote(item.symbol, BuildConfig.FINNHUB_API_KEY)
                    _quoteData.update { it + (item.symbol to quote) }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun toggleFollow(symbol: String, name: String, isCrypto: Boolean = false) {
        viewModelScope.launch {
            val isFollowing = stockDao.isFollowing(symbol)
            if (isFollowing) {
                stockDao.removeFromWatchlist(WatchlistItem(symbol, name, isCrypto))
            } else {
                stockDao.addToWatchlist(WatchlistItem(symbol, name, isCrypto))
                fetchWatchlistQuotes()
            }
        }
    }

    fun getMarketStatus(): MarketStatus {
        val now = LocalDateTime.now(ZoneId.of("America/New_York"))
        val hour = now.hour
        val day = now.dayOfWeek.value // 1 (Mon) to 7 (Sun)

        val isOpen = day in 1..5 && hour >= 4 && hour < 20
        val session = when {
            day > 5 -> "CLOSED"
            hour in 4..8 -> "PRE-MARKET"
            hour in 9..15 -> "REGULAR"
            hour in 16..19 -> "AFTER-HOURS"
            else -> "CLOSED"
        }

        return MarketStatus(isOpen, "EST", session)
    }

    init {
        // Pre-populate defaults
        viewModelScope.launch {
            val current = watchlist.first()
            if (current.isEmpty()) {
                val defaults = listOf(
                    WatchlistItem("AAPL", "Apple Inc."),
                    WatchlistItem("NVDA", "Nvidia Corp."),
                    WatchlistItem("GOOGL", "Alphabet Inc."),
                    WatchlistItem("HOOD", "Robinhood Markets"),
                    WatchlistItem("TSLA", "Tesla Inc."),
                    WatchlistItem("BINANCE:BTCUSDT", "Bitcoin", true),
                    WatchlistItem("BINANCE:ETHUSDT", "Ethereum", true),
                    WatchlistItem("BINANCE:SOLUSDT", "Solana", true),
                    WatchlistItem("EDS", "Eurodollars", true)
                )
                defaults.forEach { stockDao.addToWatchlist(it) }
            }
            fetchWatchlistQuotes()
        }
    }
}
