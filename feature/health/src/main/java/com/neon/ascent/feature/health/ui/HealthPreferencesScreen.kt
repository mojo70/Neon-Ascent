package com.neon.ascent.feature.health.ui

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.model.SpecialType
import kotlinx.coroutines.launch

@Composable
fun HealthPreferencesScreen(
    viewModel: HealthViewModel = hiltViewModel(),
    onNavigateToGarminLogin: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val autoSync by viewModel.autoSyncEnabled.collectAsState()
    val syncInterval by viewModel.syncIntervalHours.collectAsState()
    val enabledAttributes by viewModel.enabledAttributes.collectAsState()
    val showNotifications by viewModel.showSyncNotification.collectAsState()
    val activeUplinks by viewModel.activeUplinks.collectAsState()
    val scope = rememberCoroutineScope()

    val permissionsLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        viewModel.checkPermissions()
    }

    if (state.showRationale) {
        PermissionRationaleDialog(
            rationale = state.rationale,
            onConfirm = {
                viewModel.dismissRationale()
                scope.launch {
                    val permissions = viewModel.healthManager.getPermissionsToRequest()
                    permissionsLauncher.launch(permissions)
                }
            },
            onDismiss = { viewModel.dismissRationale() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020202))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // Header
        Text(
            text = "JACK IN HEALTH CONNECT",
            style = MaterialTheme.typography.headlineLarge,
            color = NeonCyan,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
        )

        Text(
            text = "NEURAL SYNC PROTOCOL // Configure how your real-world biometric data feeds the cyberdeck.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )

        // Neural Uplinks Section
        Text(
            text = "ACTIVE_UPLINKS",
            color = NeonCyan,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )

        activeUplinks.forEach { uplink ->
            val status by uplink.status.collectAsState()
            PreferenceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(uplink.provider.name, fontWeight = FontWeight.Bold)
                        Text(
                            text = when(status) {
                                is com.neon.ascent.feature.health.domain.uplink.UplinkStatus.Connected -> "LINK_ESTABLISHED"
                                is com.neon.ascent.feature.health.domain.uplink.UplinkStatus.Disconnected -> "LINK_OFFLINE"
                                is com.neon.ascent.feature.health.domain.uplink.UplinkStatus.Syncing -> "DATA_MINING_IN_PROGRESS"
                                is com.neon.ascent.feature.health.domain.uplink.UplinkStatus.Error -> "PROTOCOL_FAILURE"
                                else -> "INITIALIZING..."
                            },
                            color = when(status) {
                                is com.neon.ascent.feature.health.domain.uplink.UplinkStatus.Connected -> NeonGreen
                                is com.neon.ascent.feature.health.domain.uplink.UplinkStatus.Error -> NeonRed
                                else -> Color.White.copy(alpha = 0.6f)
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    
                    if (uplink.provider == com.neon.ascent.feature.health.domain.uplink.UplinkProvider.GARMIN) {
                        Button(
                            onClick = onNavigateToGarminLogin,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (status is com.neon.ascent.feature.health.domain.uplink.UplinkStatus.Connected) Color.DarkGray else NeonCyan
                            )
                        ) {
                            Text(
                                if (status is com.neon.ascent.feature.health.domain.uplink.UplinkStatus.Connected) "RE_SYNC" else "JACK_IN",
                                color = if (status is com.neon.ascent.feature.health.domain.uplink.UplinkStatus.Connected) Color.White else Color.Black,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

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
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.requestHealthPermissions() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ESTABLISH NEURAL LINK", color = NeonRed)
                    }
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
