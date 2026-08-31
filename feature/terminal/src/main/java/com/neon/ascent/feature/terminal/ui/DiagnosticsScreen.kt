package com.neon.ascent.feature.terminal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.core.common.CyberGridBackground
import com.neon.ascent.core.common.Scanlines
import com.neon.ascent.core.common.StaticNoise
import com.neon.ascent.core.common.GlitchOverlay
import com.neon.ascent.core.common.cyberGlitch
import com.neon.ascent.core.common.VisualMode
import com.neon.ascent.core.common.LocalVisualMode
import com.neon.ascent.core.domain.model.SpecialType
import com.neon.ascent.feature.health.ui.HealthUiState
import com.neon.ascent.feature.health.ui.HealthViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DiagnosticsScreen(
    onNavigateToHistory: (SpecialType) -> Unit,
    onRunDiagnostic: () -> Unit,
    onReCalibrateAttributes: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel(),
    healthViewModel: HealthViewModel = hiltViewModel()
) {
    val specialState by viewModel.specialState.collectAsState()
    val healthState by healthViewModel.uiState.collectAsState()
    val visualMode = LocalVisualMode.current

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Cyber Effects
        if (visualMode == VisualMode.CYBER) {
            CyberGridBackground(color = NeonCyan.copy(alpha = 0.05f))
            StaticNoise(intensity = 0.1f)
            Scanlines(intensity = 0.15f)
            GlitchOverlay(intensity = 0.1f)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // Header with Health Status
            HealthStatusBanner(healthState)

            Spacer(Modifier.height(24.dp))

            Column(
                modifier = Modifier.cyberGlitch(intensity = 0.01f)
            ) {
                Text(
                    text = "SYSTEM // DIAGNOSTICS",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = NeonCyan
                )
                Text(
                    text = "NEURAL INTEGRITY & PROGRESSION LOGS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    color = NeonCyan.copy(alpha = 0.6f)
                )
            }

            Spacer(Modifier.height(32.dp))

            // History / Trends section
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "ARCHIVE_DATA_LOADED",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = NeonGreen.copy(alpha = 0.5f)
                )
                
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
            Card(
                colors = CardDefaults.cardColors(containerColor = NeonCyan.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "NEURAL RE-CALIBRATION",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = NeonGreen
                    )
                    
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = onRunDiagnostic,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            "RUN ADAPTIVE INTELLIGENCE TEST",
                            color = NeonCyan,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onReCalibrateAttributes,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, NeonGreen),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            "ATTRIBUTE RE-CALIBRATE",
                            color = NeonGreen,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Manual Garmin Sync Button
            OutlinedButton(
                onClick = { healthViewModel.triggerImmediateSync() },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(2.dp)
            ) {
                Text(
                    "FORCE NEURAL ARCHIVE SYNC", 
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(type.getIcon(), color = color, fontSize = 20.sp)
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    text = type.name, 
                    color = MaterialTheme.colorScheme.onSurface, 
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "TREND: STABLE // LOG_OK", 
                    color = color.copy(alpha = 0.6f), 
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$percentile%", 
                    color = color, 
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                )
                Text(
                    text = "PERCENTILE", 
                    color = Color.Gray, 
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                )
            }
        }
    }
}

@Composable
private fun HealthStatusBanner(healthState: HealthUiState) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val lastSync = healthState.lastSyncTime?.let {
        formatter.format(it)
    } ?: "NULL"

    val statusColor = if (healthState.hasPermissions) NeonGreen else NeonRed
    val statusText = if (healthState.hasPermissions) "LINK_ACTIVE" else "LINK_OFFLINE"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(statusColor, CircleShape))
            Spacer(Modifier.width(12.dp))
            Text(
                text = "NEURAL_LINK // $statusText",
                color = statusColor,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "LAST_SYNC: $lastSync", 
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
