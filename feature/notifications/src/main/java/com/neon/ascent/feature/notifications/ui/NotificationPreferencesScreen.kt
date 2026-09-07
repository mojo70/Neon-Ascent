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
            // TARGET_WAKE Section
            PreferenceCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "TARGET_WAKE_ANCHORS", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    Text(
                        text = "Set target wake times. Unset fields default to 14-day USUAL times.",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "MON–FRI", color = NeonCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = state.targetWakeWd,
                                onValueChange = { viewModel.setTargetWakeWd(it) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedBorderColor = NeonCyan
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "SAT–SUN", color = NeonPink, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = state.targetWakeWe,
                                onValueChange = { viewModel.setTargetWakeWe(it) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedBorderColor = NeonPink
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Text(
                        text = "Morning shade waits 20 minutes after wake. Opening the app does not ping.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // PULSE_PM_MODE Section
            PreferenceCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "PULSE_PM_MODE", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    
                    val pmOptions = listOf("NEED_ONLY", "WEEKDAY_CLOCK", "CUSTOM")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        pmOptions.forEach { mode ->
                            val isSelected = state.pulsePmMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) NeonPink.copy(alpha = 0.12f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonPink else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { viewModel.setPulsePmMode(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    color = if (isSelected) NeonPink else Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    val pmDescription = when (state.pulsePmMode) {
                        "WEEKDAY_CLOCK" -> "Mon–Fri evening pulse at lights-out minus 90m (19:30–21:30)."
                        "CUSTOM" -> "Custom schedule at ${state.pulsePmCustomTime} on ${state.pulsePmCustomDays}."
                        else -> "Evening ping only when you are short on sleep, the tank dumped, or a session is still open"
                    }

                    Text(
                        text = pmDescription,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 15.sp
                    )
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
                }
            }

            // Diagnostic Test Actions Box
            PreferenceCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "DIAGNOSTIC_TRANSMISSION_TEST", fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.sendTestAmBrief() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(text = "TEST AM BRIEF", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { viewModel.sendTestPmBrief() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(text = "TEST PM BRIEF", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.sendTestPing() },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(text = "TEST SINGLE PING", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
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
