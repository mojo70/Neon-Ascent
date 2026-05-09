package com.neon.ascent.feature.health.ui

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.model.SpecialType

@Composable
fun HealthPreferencesScreen(
    viewModel: HealthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val autoSync by viewModel.autoSyncEnabled.collectAsState()
    val syncInterval by viewModel.syncIntervalHours.collectAsState()
    val enabledAttributes by viewModel.enabledAttributes.collectAsState()
    val showNotifications by viewModel.showSyncNotification.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Header
        Text(
            text = "NEURAL SYNC PROTOCOL",
            style = MaterialTheme.typography.headlineLarge,
            color = NeonCyan,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Configure how your real-world biometric data feeds the cyberdeck.",
            color = Color.White.copy(alpha = 0.75f)
        )

        // Auto Sync Toggle
        PreferenceCard {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Automatic Background Sync", fontWeight = FontWeight.Medium)
                        Text(
                            text = "Pull Garmin data every few hours",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = autoSync,
                        onCheckedChange = { viewModel.setAutoSyncEnabled(it) }
                    )
                }
            }
        }

        // Sync Interval
        if (autoSync) {
            PreferenceCard {
                Column {
                    Text("Sync Frequency", fontWeight = FontWeight.Medium)
                    Text(
                        text = "$syncInterval hours",
                        style = MaterialTheme.typography.titleLarge,
                        color = NeonCyan
                    )
                    Slider(
                        value = syncInterval.toFloat(),
                        onValueChange = { viewModel.setSyncIntervalHours(it.toInt()) },
                        valueRange = 4f..24f,
                        steps = 4
                    )
                }
            }
        }

        // Enabled Attributes
        PreferenceCard {
            Column {
                Text("Contributing Attributes", fontWeight = FontWeight.Medium)
                Text(
                    text = "Choose which S.P.E.C.I.A.L. stats receive real-world data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )

                Spacer(Modifier.height(16.dp))

                SpecialType.entries.forEach { type ->
                    val isEnabled = type in enabledAttributes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${type.getIcon()} ${type.name}",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = {
                                val newSet = if (isEnabled) {
                                    enabledAttributes - type
                                } else {
                                    enabledAttributes + type
                                }
                                viewModel.setEnabledAttributes(newSet)
                            }
                        )
                    }
                }
            }
        }

        // Notification Preference
        PreferenceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Sync Completion Notifications")
                    Text(
                        text = "Show deck message when sync completes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = showNotifications,
                    onCheckedChange = { viewModel.setShowSyncNotification(it) }
                )
            }
        }

        // Status
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0033))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("CURRENT STATUS", color = NeonPink, fontWeight = FontWeight.Bold)
                Text(
                    text = "Last successful sync: ${state.lastSyncTime?.toRelativeTime() ?: "Never"}",
                    color = Color.White.copy(alpha = 0.8f)
                )
                if (state.hasPermissions) {
                    Text("🟢 Health Connect Link Active", color = NeonGreen)
                } else {
                    Text("⚠️ Permissions Required", color = NeonRed)
                }
            }
        }

        // Danger Zone
        OutlinedButton(
            onClick = { viewModel.resetHealthPreferences() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("RESET ALL HEALTH PREFERENCES")
        }
    }
}

// Reusable card for preferences
@Composable
private fun PreferenceCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F001A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}
