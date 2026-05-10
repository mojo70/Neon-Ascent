package com.neon.ascent.feature.terminal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonGreen
import com.neon.ascent.core.common.NeonPink
import com.neon.ascent.core.domain.model.CognitiveItem
import com.neon.ascent.core.domain.model.CognitiveTestSession

@Composable
fun CognitiveTestScreen(
    onTestComplete: (CognitiveTestSession) -> Unit,
    viewModel: CognitiveTestViewModel = hiltViewModel()
) {
    val state by viewModel.testState.collectAsState()

    when (val currentState = state) {
        is TestState.NotStarted -> {
            TestStartScreen(onStart = { viewModel.startTest() })
        }
        is TestState.InProgress -> {
            QuestionScreen(
                item = currentState.currentItem,
                progress = currentState.progress,
                onAnswer = { viewModel.recordAnswer(it) }
            )
        }
        is TestState.Complete -> {
            val result = currentState.session
            TestResultsScreen(result = result, onDone = { onTestComplete(result) })
        }
    }
}

@Composable
private fun QuestionScreen(
    item: CognitiveItem,
    progress: Float,
    onAnswer: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Progress
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth(),
            color = NeonCyan
        )

        Text(
            text = "NEURAL DIAGNOSTIC // ${item.subTest.name.replace("_", " ")}",
            color = NeonPink,
            style = MaterialTheme.typography.titleMedium
        )

        // Question
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0033)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = item.question,
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
        }

        // Options
        val options = item.options
        if (options != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                options.forEach { option ->
                    Button(
                        onClick = { onAnswer(option) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A0044))
                    ) {
                        Text(option, color = NeonCyan)
                    }
                }
            }
        } else {
            // For future visual items (matrix, spatial) – placeholder
            Text("Visual item rendering coming soon...", color = Color.White.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun TestStartScreen(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ADAPTIVE INTELLIGENCE DIAGNOSTIC",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonCyan
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "≈ 8–12 minutes • Adaptive difficulty • No time pressure",
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("BEGIN NEURAL DIAGNOSTIC")
        }
    }
}

@Composable
private fun TestResultsScreen(
    result: CognitiveTestSession,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "DIAGNOSTIC COMPLETE",
            style = MaterialTheme.typography.headlineLarge,
            color = NeonCyan
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "${result.estimatedPercentile}th PERCENTILE",
            style = MaterialTheme.typography.displayLarge,
            color = NeonPink,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Intelligence Core Updated",
            color = NeonGreen,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(48.dp))

        Button(onClick = onDone, modifier = Modifier.fillMaxWidth(0.7f)) {
            Text("RETURN TO DECK")
        }
    }
}
