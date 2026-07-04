package com.neon.ascent.feature.goals.ui

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
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.goals.models.Habit
import com.neon.ascent.core.domain.goals.models.Mission
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionDetailScreen(
    missionId: String,
    viewModel: MissionDetailViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onCompleteMission: () -> Unit
) {
    val mission by viewModel.mission.collectAsState()
    val contributingHabits by viewModel.contributingHabits.collectAsState()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy").withZone(ZoneId.systemDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text("←", style = MaterialTheme.typography.headlineMedium, color = NeonCyan)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "MISSION FILE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonPink
                )
            }
        }

        mission?.let { m ->
            // Mission Header Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0033)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            text = m.title,
                            style = MaterialTheme.typography.headlineLarge,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = m.description,
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("EXPIRES: ", color = NeonPink, fontWeight = FontWeight.Medium)
                            Text(
                                dateFormatter.format(m.expiresAt),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Progress
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F001A))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("MISSION INTEGRITY", color = NeonCyan, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { m.progress.current },
                            modifier = Modifier.fillMaxWidth(),
                            color = NeonPink,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${(m.progress.current * 100).toInt()}% COMPLETE",
                            color = NeonPink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Linked Attributes
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LINKED ATTRIBUTES", color = NeonPink, fontWeight = FontWeight.Medium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        m.linkedAttributes.forEach { type ->
                            FilterChip(
                                selected = true,
                                onClick = {},
                                label = { Text("${type.getIcon()} ${type.name}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = getNeonColorForAttribute(type).copy(alpha = 0.25f),
                                    selectedLabelColor = getNeonColorForAttribute(type)
                                )
                            )
                        }
                    }
                }
            }

            // Contributing Habits
            if (contributingHabits.isNotEmpty()) {
                item {
                    Text(
                        text = "CONTRIBUTING PULSES",
                        style = MaterialTheme.typography.titleMedium,
                        color = NeonCyan
                    )
                }

                items(contributingHabits) { habit ->
                    PulseContributionCard(habit)
                }
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Log manual progress */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                    ) {
                        Text("LOG PROGRESS")
                    }

                    Button(
                        onClick = onCompleteMission,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black)
                    ) {
                        Text("MARK MISSION COMPLETE")
                    }
                }
            }
        } ?: run {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            }
        }
        
        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun PulseContributionCard(habit: Habit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0033)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(habit.title, fontWeight = FontWeight.Medium, color = Color.White)
                Text("${habit.streak} day pulse streak", color = NeonOrange, style = MaterialTheme.typography.labelSmall)
            }

            Text(
                "${(habit.progress.current * 100).toInt()}%",
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
