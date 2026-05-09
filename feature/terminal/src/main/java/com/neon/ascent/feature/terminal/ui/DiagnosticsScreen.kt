package com.neon.ascent.feature.terminal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink
import com.neon.ascent.core.common.NeonPurple
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val specialState by viewModel.specialState.collectAsState()
    val isRunningTest by viewModel.isRunningTest.collectAsState()
    val lastTestResult by viewModel.lastTestResult.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Background holographic avatar (semi-transparent when testing)
        HolographicAvatar(
            specialAttributes = specialState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(280.dp)
                .alpha(if (isRunningTest) 0.4f else 1f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 320.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "NEURAL DIAGNOSTICS",
                style = MaterialTheme.typography.headlineMedium,
                color = NeonCyan,
                fontWeight = FontWeight.Bold
            )

            // Current S.P.E.C.I.A.L. Overview
            SpecialGrid(specialState)

            // Intelligence Focus Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0033)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("INTELLIGENCE CORE", color = NeonPink, fontWeight = FontWeight.Bold)
                    Text(
                        "Last cognitive diagnostic: ${lastTestResult?.estimatedPercentile ?: "--"}th percentile",
                        color = Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.runCognitiveDiagnostic()
                        },
                        enabled = !isRunningTest,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isRunningTest) "RUNNING DIAGNOSTIC..." else "RUN ADAPTIVE INTELLIGENCE TEST (≈10 min)")
                    }
                }
            }

            // Full Diagnostics Button (expand later for other attributes)
            Button(
                onClick = { /* TODO: Full physical + wearable sync diagnostics */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("RUN FULL SYSTEM DIAGNOSTICS (Garmin + Physical)")
            }
        }
    }
}

@Composable
fun SpecialGrid(specialAttributes: Map<SpecialType, SpecialAttribute>) {
    // Placeholder for S.P.E.C.I.A.L. grid
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SpecialType.entries.forEach { type ->
            val attr = specialAttributes[type]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = type.name.take(3), color = NeonCyan, style = MaterialTheme.typography.labelSmall)
                Text(text = "${attr?.currentValue ?: "-"}", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
