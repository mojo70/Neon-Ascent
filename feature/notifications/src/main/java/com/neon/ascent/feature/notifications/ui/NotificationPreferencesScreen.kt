package com.neon.ascent.feature.notifications.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
            .background(Color(0xFF020508))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "NEURAL_PING_PROTOCOLS",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonCyan,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "Configure tactical transmission guidelines. Maintain sensory integrity.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )

        // ADHD Burnout & Fatigue Prevention Status
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (state.burnoutFatigueActive) Color(0xFFFFCC00).copy(alpha = 0.05f) else Color(0xFF00FFCC).copy(alpha = 0.03f)
            ),
            border = BorderStroke(
                1.dp, 
                if (state.burnoutFatigueActive) Color(0xFFFFCC00).copy(alpha = 0.4f) else Color(0xFF00FFCC).copy(alpha = 0.15f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (state.burnoutFatigueActive) Color(0xFFFFCC00) else Color(0xFF00FFCC), shape = RoundedCornerShape(4.dp))
                    )
                    Text(
                        text = if (state.burnoutFatigueActive) "FATIGUE_PREVENTION_ACTIVE" else "COGNITIVE_LOAD_OPTIMAL",
                        color = if (state.burnoutFatigueActive) Color(0xFFFFCC00) else Color(0xFF00FFCC),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = if (state.burnoutFatigueActive) {
                        "Your 7-day completion rate is ${state.completionRate7Day}%. Cyberdeck has auto-widened notification windows, muted redundant alerts, and adjusted CYBR-TES tone to soft guidance to prevent cognitive burnout."
                    } else {
                        "7-day completion rate is ${state.completionRate7Day}%. Signal integrity is optimal. Transmission channels running with standard high-energy dialectic feedback."
                    },
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 15.sp
                )
            }
        }

        // Master Toggle
        PreferenceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(text = "Master Neural Pings", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Text(
                        text = "Global switch for all tactical alert transmissions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
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
            // Ping Budget Selector
            PreferenceCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "PING_BUDGET", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text(
                        text = "Limit daily alert density to preserve focus bandwidth.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("LOW", "MEDIUM", "HIGH").forEach { budget ->
                            val isSelected = state.pingBudget == budget
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.12f) else Color.Transparent)
                                    .border(
                                        1.dp, 
                                        if (isSelected) NeonCyan else Color.White.copy(alpha = 0.1f), 
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { viewModel.setPingBudget(budget) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = budget,
                                    color = if (isSelected) NeonCyan else Color.Gray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Adaptive Wake default setting
            PreferenceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(text = "Adaptive Wake Reminders", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        Text(
                            text = "Shifts morning pings dynamically based on actual sleep/wake biometrics from Health Connect.",
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = state.adaptiveWakeDefault,
                        onCheckedChange = { viewModel.toggleAdaptiveWake(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // Frequency
            PreferenceCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "COGNITIVE_PING_FREQUENCY", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text(text = "${state.frequencyHours} HOURS", style = MaterialTheme.typography.titleLarge, color = NeonCyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Slider(
                        value = state.frequencyHours.toFloat(),
                        onValueChange = { viewModel.setFrequency(it.toInt()) },
                        valueRange = 2f..12f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            // Quiet Hours
            PreferenceCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "QUIET_HOUR_BLOCK", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text(text = "Suppress pings during standard rest cycle:", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "${state.quietStartHour}:00", color = NeonPink, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(text = "—", color = Color.Gray, fontFamily = FontFamily.Monospace)
                        Text(text = "0${state.quietEndHour}:00", color = NeonPink, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Categories
            PreferenceCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "ACTIVE_TRANSMISSION_CHANNELS", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)

                    NotificationCategoryRow(
                        title = "Missions & Habits",
                        enabled = state.missionPingsEnabled,
                        onToggle = viewModel::toggleMissionPings
                    )
                    NotificationCategoryRow(
                        title = "Streak Alerts",
                        enabled = state.streakPingsEnabled,
                        onToggle = viewModel::toggleStreakPings
                    )
                    NotificationCategoryRow(
                        title = "System Diagnostics",
                        enabled = state.systemPingsEnabled,
                        onToggle = viewModel::toggleSystemPings
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(text = "NEURAL_BRIEF_CONFIGURATION", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    
                    NotificationCategoryRow(
                        title = "Neural Brief Protocol",
                        enabled = true,
                        onToggle = { }
                    )
                    Text(
                        text = "Morning pulse is once per day. Opening the app does not ping.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )

                    NotificationCategoryRow(
                        title = "Adaptive Wake Anchor",
                        enabled = state.adaptiveWakeDefault,
                        onToggle = viewModel::toggleAdaptiveWake
                    )
                }
            }

            // Test Actions Box
            PreferenceCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "DIAGNOSTIC_TRANSMISSION_TEST", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    
                    Button(
                        onClick = { viewModel.sendTestPing() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(text = "TEST_SINGLE_PING", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.sendTestBrief() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(text = "TEST_DAILY_NEURAL_BRIEF", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Action Buttons
        OutlinedButton(
            onClick = { viewModel.resetToDefaults() },
            border = BorderStroke(1.dp, NeonRed),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonRed),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "RESET_ALL_PROTOCOLS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(text = "RETURN_TO_DECK", color = Color.White, fontFamily = FontFamily.Monospace)
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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, modifier = Modifier.weight(1f), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F141D)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
