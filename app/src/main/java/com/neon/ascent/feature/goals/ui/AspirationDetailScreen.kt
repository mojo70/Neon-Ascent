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
import androidx.navigation.NavController
import com.neon.ascent.Screen
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.goals.models.Aspiration
import com.neon.ascent.core.domain.goals.models.Mission

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AspirationDetailScreen(
    aspirationId: String,
    navController: NavController,
    viewModel: AspirationDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val aspiration by viewModel.aspiration.collectAsState()
    val linkedMissions by viewModel.linkedMissions.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Text("←", color = NeonCyan, style = MaterialTheme.typography.headlineMedium)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "DIRECTIVE ARCHIVE",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonCyan
                )
            }
        }

        aspiration?.let { asp ->
            // Main Aspiration Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0033)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(asp.title, style = MaterialTheme.typography.headlineLarge, color = NeonCyan)
                        Text(
                            asp.description,
                            color = Color.White.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(Modifier.height(20.dp))

                        LinearProgressIndicator(
                            progress = { asp.progress.current },
                            modifier = Modifier.fillMaxWidth(),
                            color = NeonPink,
                            trackColor = Color.DarkGray
                        )
                        
                        Spacer(Modifier.height(8.dp))

                        Text(
                            "${(asp.progress.current * 100).toInt()}% COMPLETE",
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
                        asp.linkedAttributes.forEach { type ->
                            FilterChip(
                                selected = true,
                                onClick = { },
                                label = { Text("${type.getIcon()} ${type.name}") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = getNeonColorForAttribute(type).copy(alpha = 0.2f),
                                    selectedLabelColor = getNeonColorForAttribute(type)
                                )
                            )
                        }
                    }
                }
            }

            // Supporting Missions
            item {
                Text(
                    text = "SUPPORTING MISSIONS",
                    style = MaterialTheme.typography.titleLarge,
                    color = NeonCyan
                )
            }

            items(linkedMissions) { mission ->
                MissionDetailCard(mission) {
                    navController.navigate(Screen.MissionDetail(mission.id))
                }
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Edit aspiration */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                    ) {
                        Text("EDIT DIRECTIVE")
                    }

                    Button(
                        onClick = { /* Generate more missions */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                    ) {
                        Text("GENERATE NEW MISSIONS")
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
private fun MissionDetailCard(mission: Mission, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F001A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(mission.title, fontWeight = FontWeight.Bold, color = NeonPink)
            Text(mission.description, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { mission.progress.current },
                modifier = Modifier.fillMaxWidth(),
                color = NeonCyan,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            
            Spacer(Modifier.height(8.dp))

            Text(
                text = "Expires: ${mission.expiresAt}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}
