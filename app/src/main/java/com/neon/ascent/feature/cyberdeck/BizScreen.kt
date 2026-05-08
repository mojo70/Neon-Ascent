package com.neon.ascent.feature.cyberdeck

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.model.EarningsEntry
import com.neon.ascent.model.StockCandleResponse
import com.neon.ascent.model.StockFinancials
import com.neon.ascent.model.WatchlistItem
import com.neon.ascent.ui.CyberFrame
import java.util.Locale

@Composable
fun BizScreen(viewModel: StockViewModel, netWorthViewModel: NetWorthViewModel) {
    val watchlist by viewModel.watchlist.collectAsState()
    val quoteData by viewModel.quoteData.collectAsState()
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val candleData by viewModel.candleData.collectAsState()
    val financials by viewModel.financials.collectAsState()
    val earnings by viewModel.earnings.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val marketStatus = viewModel.getMarketStatus()
    
    val nwSummary by netWorthViewModel.summary.collectAsState()
    var showingNWDetail by remember { mutableStateOf(false) }

    if (showingNWDetail) {
        AugmentedAssetsScreen(
            viewModel = netWorthViewModel,
            onBack = { showingNWDetail = false }
        )
    } else if (selectedSymbol != null) {
        StockDetailView(
            symbol = selectedSymbol!!,
            candleData = candleData,
            financials = financials,
            earnings = earnings,
            errorMessage = errorMessage,
            onBack = { viewModel.selectSymbol(null) }
        )
    } else {
        MainBizView(
            watchlist = watchlist,
            quoteData = quoteData,
            marketStatus = marketStatus,
            nwSummary = nwSummary,
            onSelect = { symbol, isCrypto -> viewModel.selectSymbol(symbol, isCrypto) },
            onLookup = { symbol -> viewModel.toggleFollow(symbol, symbol) },
            onNWClick = { showingNWDetail = true }
        )
    }
}

@Composable
fun MainBizView(
    watchlist: List<WatchlistItem>,
    quoteData: Map<String, com.neon.ascent.model.StockQuote>,
    marketStatus: com.neon.ascent.model.MarketStatus,
    nwSummary: com.neon.ascent.model.NetWorthSummary,
    onSelect: (String, Boolean) -> Unit,
    onLookup: (String) -> Unit,
    onNWClick: () -> Unit
) {
    var searchSymbol by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        NetWorthHeader(nwSummary, onNWClick)
        Spacer(modifier = Modifier.height(24.dp))
        MarketStatusHeader(marketStatus)
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = searchSymbol,
                onValueChange = { searchSymbol = it.uppercase() },
                modifier = Modifier.weight(1f),
                placeholder = { Text("FOLLOW_SYMBOL...", color = Color.Gray, fontSize = 12.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.05f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            )
            IconButton(onClick = { 
                if (searchSymbol.isNotBlank()) {
                    onLookup(searchSymbol)
                    searchSymbol = ""
                }
            }) {
                Icon(Icons.Default.Search, contentDescription = "Add", tint = Color(0xFF00FF9F))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("STOCKS", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            items(watchlist.filter { !it.isCrypto }) { item ->
                WatchlistItemRow(item.symbol, quoteData[item.symbol]) {
                    onSelect(item.symbol, false)
                }
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("CRYPTO", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            items(watchlist.filter { it.isCrypto }) { item ->
                WatchlistItemRow(item.symbol, quoteData[item.symbol]) {
                    onSelect(item.symbol, true)
                }
            }
        }
    }
}

@Composable
fun StockDetailView(
    symbol: String,
    candleData: StockCandleResponse?,
    financials: StockFinancials?,
    earnings: EarningsEntry?,
    errorMessage: String?,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FF9F))
            }
            Text(
                "DATA_STREAM // $symbol",
                color = Color(0xFF00FF9F),
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        CyberFrame(label = "TREND_ANALYSIS") {
            CandleChart(candleData, modifier = Modifier.fillMaxWidth().height(200.dp).padding(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (symbol != "EDS") {
            CyberFrame(label = "FINANCIAL_METRICS") {
                Column(modifier = Modifier.padding(12.dp)) {
                    financials?.metric?.let { m ->
                        MetricRow("52W_HIGH", "$${m["52WeekHigh"] ?: "---"}")
                        MetricRow("52W_LOW", "$${m["52WeekLow"] ?: "---"}")
                        MetricRow("BETA", "${m["beta"] ?: "---"}")
                        MetricRow("MARKET_CAP", "${m["marketCapitalization"] ?: "---"}")
                    }
                    earnings?.let { e ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("LAST_EARNINGS", color = Color(0xFF00FF9F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        MetricRow("DATE", e.date)
                        MetricRow("EPS_ACTUAL", "${e.epsActual}")
                        MetricRow("EPS_ESTIMATE", "${e.epsEstimate}")
                    }
                }
            }
        } else {
            CyberFrame(label = "CURRENCY_INFO") {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("The Eurodollar (EDS) is the primary currency of the New United States and Night City. Known colloquially as 'Eddies'.", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun NetWorthHeader(summary: com.neon.ascent.model.NetWorthSummary, onClick: () -> Unit) {
    CyberFrame(
        label = "AUGMENTED_ASSETS",
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("TOTAL_NETWORTH", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(
                    "$${String.format(Locale.getDefault(), "%,.2f", summary.totalValue)}",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                val color = if (summary.isUp) Color(0xFF00FF9F) else Color.Red
                Icon(
                    imageVector = if (summary.isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    "${String.format(Locale.getDefault(), "%.1f", summary.changePercentage)}%",
                    color = color,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun MarketStatusHeader(status: com.neon.ascent.model.MarketStatus) {
    val color = when (status.session) {
        "REGULAR" -> Color(0xFF00FF9F)
        "PRE-MARKET", "AFTER-HOURS" -> Color(0xFFFFCC00)
        else -> Color.Red
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "MARKET_${status.session} // ${status.timezone}",
            color = color,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CandleChart(data: StockCandleResponse?, modifier: Modifier) {
    if (data == null || data.c.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("LOADING_MARKET_DATA...", color = Color.Gray, fontSize = 10.sp)
        }
        return
    }

    Canvas(modifier = modifier) {
        val minPrice = data.l.minOrNull() ?: 0f
        val maxPrice = data.h.maxOrNull() ?: 100f
        val priceRange = maxPrice - minPrice
        
        val candleWidth = size.width / data.c.size
        
        data.c.forEachIndexed { i, close ->
            val open = data.o[i]
            val high = data.h[i]
            val low = data.l[i]
            
            val isBullish = close >= open
            val color = if (isBullish) Color(0xFF00FF9F) else Color.Red
            
            val x = i * candleWidth + (candleWidth * 0.1f)
            val w = candleWidth * 0.8f
            
            val highY = size.height - ((high - minPrice) / priceRange * size.height)
            val lowY = size.height - ((low - minPrice) / priceRange * size.height)
            drawLine(color, Offset(x + w / 2, highY), Offset(x + w / 2, lowY), strokeWidth = 1.dp.toPx())
            
            val openY = size.height - ((open - minPrice) / priceRange * size.height)
            val closeY = size.height - ((close - minPrice) / priceRange * size.height)
            val bodyTop = minOf(openY, closeY)
            val bodyHeight = maxOf(2f, kotlin.math.abs(openY - closeY))
            
            drawRect(color, Offset(x, bodyTop), Size(w, bodyHeight))
        }
    }
}

@Composable
fun WatchlistItemRow(symbol: String, quote: com.neon.ascent.model.StockQuote?, onClick: () -> Unit) {
    val displaySymbol = symbol.split(":").last().replace("USDT", "")
    CyberFrame(
        label = displaySymbol,
        borderColor = Color.White.copy(alpha = 0.2f),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(displaySymbol, color = Color.White, fontWeight = FontWeight.Bold)
            
            if (quote != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isUp = quote.d >= 0
                    val color = if (isUp) Color(0xFF00FF9F) else Color.Red
                    Icon(
                        imageVector = if (isUp) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "${String.format(Locale.getDefault(), "%.2f", quote.dp)}%",
                        color = color,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "$${String.format(Locale.getDefault(), "%.2f", quote.c)}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Text("---", color = Color.Gray)
            }
        }
    }
}
