package com.neon.ascent.feature.goals.ui.ascension

import androidx.compose.foundation.background
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
import com.neon.ascent.core.common.*
import androidx.compose.material.icons.filled.Psychology

import androidx.compose.material.icons.automirrored.filled.LibraryBooks

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AscensionTerminalScreen(
    onBack: () -> Unit,
    onDirectiveClick: (String) -> Unit,
    onTaskClick: (String) -> Unit,
    onForgeClick: () -> Unit,
    onReviewClick: (String) -> Unit,
    onRitualClick: () -> Unit,
    onBrowseProtocols: () -> Unit,
    viewModel: AscensionTerminalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = LocalNeonTheme.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("NEURAL ASCENSION TERMINAL") },
                actions = {
                    IconButton(onClick = onBrowseProtocols) {
                        Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "BROWSE_PROTOCOLS", tint = theme.accent)
                    }
                    IconButton(onClick = onRitualClick) {
                        Icon(Icons.Default.Terminal, contentDescription = "SYSTEM_RITUAL", tint = theme.accent)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = theme.canvas,
                    titleContentColor = theme.accent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onForgeClick,
                containerColor = theme.accent,
                contentColor = theme.canvas
            ) {
                Icon(Icons.Default.Add, contentDescription = "FORGE_DIRECTIVE")
            }
        },
        containerColor = theme.canvas
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(theme.canvas)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "OPERATIONAL DIRECTIVES", 
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.ink
                    )
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
                    Text(
                        "ACTIVE MISSIONS", 
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.ink
                    )
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
    val theme = LocalNeonTheme.current
    Card(
        onClick = onClick, 
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.surfaceRaised)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    directive.title, 
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.ink
                )
                Row {
                    IconButton(onClick = onReviewClick) {
                        Icon(Icons.Default.Psychology, contentDescription = "DIALECTIC_REVIEW", tint = theme.accent)
                    }
                    if (directive.currentProgress >= 0.9f) {
                        IconButton(onClick = onCompleteClick) {
                            Icon(Icons.Default.Check, contentDescription = "COMPLETE_DIRECTIVE", tint = theme.accent)
                        }
                    }
                }
            }
            LinearProgressIndicator(
                progress = { directive.currentProgress },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                color = theme.accent,
                trackColor = theme.ink.copy(alpha = 0.1f)
            )
            Text(
                "${(directive.currentProgress * 100).toInt()}% COMPLETE", 
                style = MaterialTheme.typography.labelSmall,
                color = theme.inkMuted
            )
        }
    }
}

@Composable
fun MissionCard(mission: AscensionMission) {
    val theme = LocalNeonTheme.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = theme.surfaceRaised)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                mission.title, 
                style = MaterialTheme.typography.titleMedium,
                color = theme.ink
            )
            Text(
                mission.description, 
                style = MaterialTheme.typography.bodySmall,
                color = theme.inkMuted
            )
        }
    }
}
