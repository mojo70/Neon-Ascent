package com.neon.ascent.feature.workout.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.time.format.DateTimeFormatter

@Composable
fun WorkoutProgressScreen(
    onBack: () -> Unit,
    viewModel: WorkoutProgressViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "BIOMETRIC ANALYSIS",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00FF9C))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // High Level Stats (Streak & Volume)
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        StatCard(
                            label = "STREAK",
                            value = "${uiState.streakDays}",
                            subValue = "DAYS",
                            icon = Icons.Default.Whatshot,
                            color = Color(0xFFFF5F1F),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "TOTAL LIFTED",
                            value = "%,d".format(uiState.totalVolume),
                            subValue = "LBS",
                            icon = Icons.Default.FitnessCenter,
                            color = Color(0xFF00CCFF),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Volume Chart
                item {
                    SectionHeader("VOLUME TREND")
                    VolumeChart(uiState.volumeHistory)
                }

                // Macro TDEE Card
                item {
                    SectionHeader("FUEL PROTOCOL")
                    MacroCard(uiState)
                }

                // PR History
                item {
                    SectionHeader("PERSONAL RECORDS")
                }

                if (uiState.prs.isEmpty()) {
                    item {
                        Text(
                            "No PR data recorded. Complete protocols to upload biometric results.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(uiState.prs) { pr ->
                        PrItem(pr)
                    }
                }
                
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        color = Color.Gray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun StatCard(label: String, value: String, subValue: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(110.dp),
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text(subValue, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
            }
        }
    }
}

@Composable
fun VolumeChart(history: List<VolumePoint>) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (history.size < 2) {
            Box(contentAlignment = Alignment.Center) {
                Text("Insufficient data for trend analysis", color = Color.DarkGray, fontSize = 12.sp)
            }
            return@Surface
        }

        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val maxVolume = history.maxOf { it.volume }
            val minVolume = history.minOf { it.volume }
            val range = (maxVolume - minVolume).coerceAtLeast(1.0)
            
            val points = history.mapIndexed { index, point ->
                val x = index * (size.width / (history.size - 1))
                val y = size.height - ((point.volume - minVolume) / range * size.height).toFloat()
                androidx.compose.ui.geometry.Offset(x, y)
            }

            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }

            drawPath(
                path = path,
                color = Color(0xFF00CCFF),
                style = Stroke(width = 3.dp.toPx())
            )
        }
    }
}

@Composable
fun MacroCard(state: ProgressUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF00FF9C).copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ESTIMATED TDEE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("${state.tdee} KCAL", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF00FF9C).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF00FF9C))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroStat("PRO", "${state.proteinGrams}G", Color(0xFFFF006E), Modifier.weight(1f))
                MacroStat("CARB", "${state.carbGrams}G", Color(0xFF00FF9C), Modifier.weight(1f))
                MacroStat("FAT", "${state.fatGrams}G", Color(0xFFFFD700), Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Adjusted for ${state.profile?.somatotype?.name ?: "MESOMORPH"} morphology",
                color = Color.DarkGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MacroStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        LinearProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(2.dp).clip(CircleShape),
            color = color,
            trackColor = Color.Transparent
        )
    }
}

@Composable
fun PrItem(pr: PersonalRecord) {
    val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        border = BorderStroke(0.5.dp, Color.DarkGray.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(pr.exerciseName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(pr.date.format(formatter), color = Color.Gray, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    pr.value, 
                    color = if (pr.isCluster) Color(0xFF00FFAA) else Color(0xFF00CCFF), 
                    fontWeight = FontWeight.Black, 
                    fontSize = 16.sp
                )
                if (pr.isCluster) {
                    Text("CLUSTER TOTAL", color = Color(0xFF00FFAA), fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}
