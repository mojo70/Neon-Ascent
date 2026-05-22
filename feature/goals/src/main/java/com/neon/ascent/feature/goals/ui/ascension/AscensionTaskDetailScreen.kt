package com.neon.ascent.feature.goals.ui.ascension

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Psychology
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AscensionTaskDetailScreen(
    taskId: String,
    onBack: () -> Unit,
    viewModel: AscensionTaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var notes by remember { mutableStateOf("") }
    var mood by remember { mutableFloatStateOf(3f) }

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("TASK_DETAILS", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NeonCyan)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Task Header
                Column {
                    Text(
                        uiState.task?.title ?: "Unknown Task",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        uiState.task?.description ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (!uiState.isCompletedToday) {
                    // AI Guide (Elenchus Mode)
                    CyberFrame(label = "CYBR-TES // ELENCHUS_GUIDE", accentColor = NeonCyan) {
                        Text(
                            uiState.guideText ?: "Sourcing dialectic breakdown...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Log Section
                    CyberFrame(label = "LOG_COMPLETION", accentColor = NeonPink) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                label = { Text("REFLECTIVE_NOTES", fontSize = 10.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                            )
                            
                            Column {
                                Text("MOOD_SYNC: ${mood.toInt()}", color = NeonPink, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                Slider(
                                    value = mood,
                                    onValueChange = { mood = it },
                                    valueRange = 1f..5f,
                                    steps = 3,
                                    colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonPink)
                                )
                            }

                            Button(
                                onClick = { viewModel.completeTask(notes, mood.toInt()) },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("LOG_COMPLETE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Dialectic Reflection UI
                AnimatedVisibility(
                    visible = uiState.showReflection,
                    enter = fadeIn() + expandVertically()
                ) {
                    CyberFrame(label = "CYBR-TES // DIALECTIC_REFLECTION", accentColor = Color.Green) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.Green)
                                Spacer(Modifier.width(8.dp))
                                Text("ANALYSIS_COMPLETE", color = Color.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            
                            Text(
                                uiState.reflectionText ?: "Analyzing neural logs...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )

                            Button(
                                onClick = onBack,
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Green)
                            ) {
                                Text("ACKNOWLEDGE", color = Color.Green, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
                
                if (uiState.isCompletedToday && !uiState.showReflection) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("PROTOCOL_EXECUTED_FOR_TODAY", color = Color.Green, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}
