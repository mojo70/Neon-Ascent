package com.neon.ascent.feature.terminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonGreen
import com.neon.ascent.core.common.NeonRed
import com.neon.ascent.core.common.getNeonColorForAttribute
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
    onRunDiagnostic: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
    healthViewModel: HealthViewModel = hiltViewModel()
) {
    val specialState by viewModel.specialState.collectAsState()
    val healthState by healthViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020202))
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Header with Health Status
        HealthStatusBanner(healthState)

        Spacer(Modifier.height(24.dp))

        Text(
            text = "FULL NEURAL ARCHIVE",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonCyan,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )
        
        Text(
            text = "Deep progression history & neural integrity logs",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f)
        )

        Spacer(Modifier.height(32.dp))

        // History / Trends section
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SpecialType.entries.forEach { type ->
                val attribute = specialState[type]
                ArchiveSummaryItem(
                    type = type,
                    percentile = attribute?.percentile ?: 50,
                    onClick = { onNavigateToHistory(type) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Re-test section
        Text(
            text = "NEURAL RE-CALIBRATION",
            style = MaterialTheme.typography.labelLarge,
            color = NeonGreen.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold
        )
        
        Spacer(Modifier.height(12.dp))

        // Intelligence Test Button
        Button(
            onClick = onRunDiagnostic,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
        ) {
            Text(
                "RUN ADAPTIVE INTELLIGENCE TEST",
                color = NeonCyan
            )
        }

        Spacer(Modifier.height(12.dp))

        // Manual Garmin Sync Button
        OutlinedButton(
            onClick = { healthViewModel.triggerImmediateSync() },
            modifier = Modifier.fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.5f)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
        ) {
            Text("FORCE NEURAL ARCHIVE SYNC", color = Color.Gray)
        }
    }
}

@Composable
private fun ArchiveSummaryItem(
    type: SpecialType,
    percentile: Int,
    onClick: () -> Unit
) {
    val color = getNeonColorForAttribute(type)
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, color.copy(alpha = 0.3f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(type.getIcon(), color = color, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(type.name, color = Color.White, fontWeight = FontWeight.Bold)
                Text("Trend: Stable", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            Text("$percentile%", color = color, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
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
