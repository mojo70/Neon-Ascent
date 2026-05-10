package com.neon.ascent.feature.notifications.ui

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
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink
import com.neon.ascent.core.common.NeonRed

@Composable
fun NotificationPreferencesScreen(
    onBack: () -> Unit,
    viewModel: NotificationPreferencesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        Text(
            text = "NEURAL PING PROTOCOLS",
            style = MaterialTheme.typography.headlineLarge,
            color = NeonCyan,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Configure how the deck communicates with you.",
            color = Color.White.copy(alpha = 0.75f)
        )

        // Master Toggle
        PreferenceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = "Enable Neural Pings", fontWeight = FontWeight.Medium, color = Color.White)
                    Text(
                        text = "Mission reminders, streak alerts, and system status",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Switch(
                    checked = state.masterEnabled,
                    onCheckedChange = { viewModel.toggleMaster(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonCyan,
                        checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                    )
                )
            }
        }

        if (state.masterEnabled) {
            // Frequency
            PreferenceCard {
                Column {
                    Text(text = "Ping Frequency", fontWeight = FontWeight.Medium, color = Color.White)
                    Text(text = "${state.frequencyHours} hours", style = MaterialTheme.typography.titleLarge, color = NeonCyan)
                    Slider(
                        value = state.frequencyHours.toFloat(),
                        onValueChange = { viewModel.setFrequency(it.toInt()) },
                        valueRange = 2f..12f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = NeonCyan.copy(alpha = 0.2f)
                        )
                    )
                }
            }

            // Quiet Hours
            PreferenceCard {
                Column {
                    Text(text = "Quiet Hours", fontWeight = FontWeight.Medium, color = Color.White)
                    Text(text = "No pings between", color = Color.White.copy(alpha = 0.7f))
                    Row {
                        Text(text = "${state.quietStartHour}:00", color = NeonPink)
                        Text(text = " — ", color = Color.White.copy(alpha = 0.5f))
                        Text(text = "0${state.quietEndHour}:00", color = NeonPink)
                    }
                }
            }

            // Categories
            PreferenceCard {
                Column {
                    Text(text = "Ping Categories", fontWeight = FontWeight.Medium, color = Color.White)

                    NotificationCategoryRow(
                        title = "Mission & Habit Reminders",
                        enabled = state.missionPingsEnabled,
                        onToggle = viewModel::toggleMissionPings
                    )
                    NotificationCategoryRow(
                        title = "Streak Protection",
                        enabled = state.streakPingsEnabled,
                        onToggle = viewModel::toggleStreakPings
                    )
                    NotificationCategoryRow(
                        title = "System & Level-Up Alerts",
                        enabled = state.systemPingsEnabled,
                        onToggle = viewModel::toggleSystemPings
                    )
                }
            }

            // Test Button
            Button(
                onClick = { viewModel.sendTestPing() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
            ) {
                Text(text = "SEND TEST NEURAL PING", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }

        // Danger Zone
        OutlinedButton(
            onClick = { viewModel.resetToDefaults() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "RESET ALL NOTIFICATION SETTINGS")
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Text(text = "BACK TO SETTINGS", color = Color.White)
        }
    }
}

@Composable
private fun NotificationCategoryRow(
    title: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, modifier = Modifier.weight(1f), color = Color.White)
        Switch(
            checked = enabled, 
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
            )
        )
    }
}

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
