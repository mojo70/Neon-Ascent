package com.neon.ascent.feature.goals.ui.ascension

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink
import java.time.LocalDate
import java.time.temporal.IsoFields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalRitualScreen(
    onBack: () -> Unit,
    viewModel: TerminalRitualViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.exportEvent.collect { logContent ->
            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, logContent)
                type = "text/markdown"
            }
            val shareIntent = android.content.Intent.createChooser(sendIntent, "EXPORT NEURAL LOG")
            context.startActivity(shareIntent)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("TERMINAL_RITUAL // Q${uiState.currentQuarter}_${uiState.currentYear}", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Heatmap Section
            CyberFrame(label = "NEURAL_CONSISTENCY_MAP", accentColor = NeonCyan) {
                NeuralHeatmap(uiState.heatmapData)
            }

            // AI Analysis Section
            CyberFrame(label = "CYBR-TES // SYNTHESIS", accentColor = NeonPink) {
                if (uiState.isLoading) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = NeonCyan)
                        Spacer(Modifier.height(16.dp))
                        Text("SYNTHESIZING_PERIOD_DATA...", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                } else {
                    Text(
                        text = uiState.ritualAnalysis ?: "NO_DATA_AVAILABLE_FOR_SYNTHESIS",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // Stats Summary
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatBox("TOTAL_SYNC", "${uiState.completionHistory.size}", NeonCyan, Modifier.weight(1f))
                val avgMood = if (uiState.completionHistory.isNotEmpty()) {
                    uiState.completionHistory.mapNotNull { it.mood }.average()
                } else 0.0
                StatBox("AVG_MOOD", String.format("%.1f", avgMood), NeonPink, Modifier.weight(1f))
            }

            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = { viewModel.exportNeuralLog() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan)
            ) {
                Text("EXPORT_NEURAL_LOG", color = NeonCyan, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun NeuralHeatmap(data: Map<LocalDate, Int>) {
    val now = LocalDate.now()
    val startOfQuarter = now.with(IsoFields.DAY_OF_QUARTER, 1L)
    val daysInQuarter = 90 // Approximate
    
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MON", fontSize = 8.sp, color = Color.Gray)
            Text("WED", fontSize = 8.sp, color = Color.Gray)
            Text("FRI", fontSize = 8.sp, color = Color.Gray)
            Text("SUN", fontSize = 8.sp, color = Color.Gray)
        }
        Spacer(Modifier.height(4.dp))
        // Simple 13x7 grid for a quarter
        BoxWithConstraints {
            val size = (maxWidth - 48.dp) / 13
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until 7) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (col in 0 until 13) {
                            val dayOffset = col * 7 + row
                            val date = startOfQuarter.plusDays(dayOffset.toLong())
                            val count = data[date] ?: 0
                            val color = when {
                                date.isAfter(now) -> Color.DarkGray.copy(alpha = 0.2f)
                                count == 0 -> Color.DarkGray.copy(alpha = 0.5f)
                                count == 1 -> NeonCyan.copy(alpha = 0.4f)
                                count == 2 -> NeonCyan.copy(alpha = 0.7f)
                                else -> NeonCyan
                            }
                            Box(modifier = Modifier.size(size).background(color))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Column {
            Text(label, color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
    }
}
