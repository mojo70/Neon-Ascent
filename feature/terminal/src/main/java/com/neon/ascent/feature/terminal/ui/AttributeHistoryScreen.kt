package com.neon.ascent.feature.terminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink
import com.neon.ascent.core.common.getNeonColorForAttribute
import com.neon.ascent.core.domain.model.BenchmarkTest
import com.neon.ascent.core.domain.model.SpecialType
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AttributeHistoryScreen(
    attributeType: SpecialType,
    onBack: () -> Unit,
    viewModel: AttributeHistoryViewModel = hiltViewModel()
) {
    val attribute by viewModel.currentAttribute.collectAsState()
    val history by viewModel.benchmarkHistory.collectAsState()
    val percentileEntries by viewModel.percentileTimeSeries.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020202))
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Header
        Text(
            text = "${attributeType.name} ARCHIVE",
            style = MaterialTheme.typography.headlineLarge,
            color = getNeonColorForAttribute(attributeType)
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Real-world progression log • Grounded in verified data",
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(24.dp))

        // Current Status Card
        attribute?.let {
            SpecialAttributeCard(
                attribute = it,
                glowAlpha = (it.percentile ?: 50) / 100f,
                onClick = {}
            )
        }

        Spacer(Modifier.height(32.dp))

        // Trend Chart
        if (percentileEntries.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F001A))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "PERCENTILE TREND", color = NeonCyan, fontWeight = FontWeight.Bold)
                    Chart(
                        chart = lineChart(),
                        model = entryModelOf(percentileEntries),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // History Log
        Text(
            text = "DIAGNOSTIC HISTORY",
            style = MaterialTheme.typography.titleMedium,
            color = NeonPink
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(history) { test ->
                BenchmarkTestRow(test)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.runReTest(attributeType) },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (attributeType == SpecialType.INTELLIGENCE) "RUN NEW COGNITIVE TEST" else "LOG NEW BENCHMARK")
            }

            OutlinedButton(
                onClick = { viewModel.triggerHealthSyncForAttribute(attributeType) },
                modifier = Modifier.weight(1f)
            ) {
                Text("FORCE GARMIN SYNC")
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("RETURN TO TERMINAL", color = Color.Gray)
        }
    }
}

@Composable
private fun BenchmarkTestRow(test: BenchmarkTest) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm").withZone(ZoneId.systemDefault())
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0033))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = test.testType.name.replace("_", " "),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatter.format(test.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${test.percentile}th",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan
                )
                Text(
                    text = test.rawScore.toInt().toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}
