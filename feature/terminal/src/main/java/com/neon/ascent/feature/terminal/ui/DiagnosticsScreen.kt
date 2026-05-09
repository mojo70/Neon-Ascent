package com.neon.ascent.feature.terminal.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonGreen
import com.neon.ascent.core.common.NeonRed
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.feature.health.ui.HealthUiState
import com.neon.ascent.feature.health.ui.HealthViewModel
import com.neon.ascent.feature.health.ui.SyncStatus
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DiagnosticsScreen(
    onNavigateToHistory: (SpecialType) -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
    healthViewModel: HealthViewModel = hiltViewModel()
) {
    val specialState by viewModel.specialState.collectAsState()
    val healthState by healthViewModel.uiState.collectAsState()
    val isRunningTest by viewModel.isRunningTest.collectAsState()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with Health Status
        HealthStatusBanner(healthState)

        Spacer(Modifier.height(24.dp))

        // Holographic Avatar
        HolographicAvatar(
            specialAttributes = specialState,
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "S.P.E.C.I.A.L. CORE",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonCyan
        )

        Spacer(Modifier.height(16.dp))

        // FULL LIVE GRID
        SpecialGrid(
            specialAttributes = specialState,
            modifier = Modifier.weight(1f),
            onAttributeClick = { type ->
                onNavigateToHistory(type)
            }
        )

        Spacer(Modifier.height(16.dp))

        // Intelligence Test Button
        Button(
            onClick = {
                scope.launch { viewModel.runCognitiveDiagnostic() }
            },
            enabled = !isRunningTest,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRunningTest) "RUNNING NEURAL DIAGNOSTIC..." else "RUN ADAPTIVE INTELLIGENCE TEST (~10 min)")
        }

        // Manual Garmin Sync Button
        OutlinedButton(
            onClick = { healthViewModel.triggerImmediateSync() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("FORCE GARMIN NEURAL SYNC")
        }
    }
}

@Composable
private fun HealthStatusBanner(healthState: HealthUiState) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val lastSync = healthState.lastSyncTime?.let {
        "Last sync: ${formatter.format(it)}"
    } ?: "No sync yet"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (healthState.hasPermissions)
                Color(0xFF00220A) else Color(0xFF220A00)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (healthState.hasPermissions) "🟢 NEURAL LINK ACTIVE" else "⚠️ HEALTH LINK OFFLINE",
                color = if (healthState.hasPermissions) NeonGreen else NeonRed,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.weight(1f))
            Text(lastSync, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
        }
    }
}
