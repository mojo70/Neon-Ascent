package com.neon.ascent.feature.goals.ui.ascension

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Terminal
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.domain.goals.models.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.graphics.Color
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.CelebrationOverlay
import androidx.compose.material.icons.filled.Psychology

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AscensionTerminalScreen(
    onBack: () -> Unit,
    onDirectiveClick: (String) -> Unit,
    onTaskClick: (String) -> Unit,
    onForgeClick: () -> Unit,
    onReviewClick: (String) -> Unit,
    onRitualClick: () -> Unit,
    viewModel: AscensionTerminalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NEURAL ASCENSION TERMINAL") },
                actions = {
                    IconButton(onClick = onRitualClick) {
                        Icon(Icons.Default.Terminal, contentDescription = "SYSTEM_RITUAL", tint = NeonCyan)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onForgeClick) {
                Icon(Icons.Default.Add, contentDescription = "FORGE_DIRECTIVE")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("OPERATIONAL DIRECTIVES", style = MaterialTheme.typography.titleLarge)
                }
                
                items(uiState.directives.filter { it.status == DirectiveStatus.ACTIVE }) { directive ->
                    DirectiveCard(
                        directive = directive, 
                        onClick = { onDirectiveClick(directive.id) },
                        onReviewClick = { onReviewClick(directive.id) },
                        onCompleteClick = { viewModel.markDirectiveCompleted(directive) }
                    )
                }

                item {
                    Text("ACTIVE MISSIONS", style = MaterialTheme.typography.titleLarge)
                }

                items(uiState.activeMissions) { mission ->
                    MissionCard(mission = mission)
                }
            }

            CelebrationOverlay(
                event = uiState.dopamineEvent,
                onFinished = { viewModel.clearDopamineEvent() }
            )
        }
    }
}

@Composable
fun DirectiveCard(
    directive: AscensionDirective, 
    onClick: () -> Unit, 
    onReviewClick: () -> Unit,
    onCompleteClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(directive.title, style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = onReviewClick) {
                        Icon(Icons.Default.Psychology, contentDescription = "DIALECTIC_REVIEW", tint = NeonCyan)
                    }
                    if (directive.currentProgress >= 0.9f) {
                        IconButton(onClick = onCompleteClick) {
                            Icon(Icons.Default.Check, contentDescription = "COMPLETE_DIRECTIVE", tint = Color.Green)
                        }
                    }
                }
            }
            LinearProgressIndicator(
                progress = { directive.currentProgress },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            Text("${(directive.currentProgress * 100).toInt()}% COMPLETE", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun MissionCard(mission: AscensionMission) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(mission.title, style = MaterialTheme.typography.titleMedium)
            Text(mission.description, style = MaterialTheme.typography.bodySmall)
        }
    }
}
