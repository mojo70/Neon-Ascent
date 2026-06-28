package com.neon.ascent.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.*

@Composable
fun NotificationSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isNeuralBriefEnabled by viewModel.isNeuralBriefEnabled.collectAsState()
    val quietHoursStart by viewModel.quietHoursStart.collectAsState()
    val quietHoursEnd by viewModel.quietHoursEnd.collectAsState()
    val briefFrequency by viewModel.briefFrequency.collectAsState()
    val insightDepth by viewModel.insightDepth.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0A0A0A))) {
        MatrixRainBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FF9C))
                }
                Text(
                    "//NEURAL_PING_CONFIG",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = Color(0xFF00FF9C),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            CyberFrame(label = "DAILY_BRIEF_PROTOCOLS") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ToggleSetting(
                        label = "ENABLE NEURAL BRIEF",
                        checked = isNeuralBriefEnabled,
                        onCheckedChange = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.setNeuralBriefEnabled(it) 
                        }
                    )

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.debugTriggerTestBrief()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp).border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.5f), CyberButtonShape),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("SEND TEST BRIEF", color = Color(0xFF00FF9C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    AnimatedVisibility(visible = isNeuralBriefEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            HorizontalDivider(color = Color(0xFF00FF9C).copy(alpha = 0.1f))

                            Text("QUIET HOURS (LOCAL_TIME)", color = Color(0xFF00FF9C).copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                TimeField(
                                    label = "START",
                                    time = quietHoursStart,
                                    modifier = Modifier.weight(1f),
                                    onTimeSelected = { viewModel.setQuietHoursStart(it) }
                                )
                                TimeField(
                                    label = "END",
                                    time = quietHoursEnd,
                                    modifier = Modifier.weight(1f),
                                    onTimeSelected = { viewModel.setQuietHoursEnd(it) }
                                )
                            }

                            Text("SYNC_FREQUENCY", color = Color(0xFF00FF9C).copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                CyberTabButton(
                                    selected = briefFrequency == "DAILY",
                                    onClick = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setBriefFrequency("DAILY") 
                                    },
                                    label = "DAILY",
                                    modifier = Modifier.weight(1f)
                                )
                                CyberTabButton(
                                    selected = briefFrequency == "BI_DAILY",
                                    onClick = { 
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.setBriefFrequency("BI_DAILY") 
                                    },
                                    label = "BI-DAILY",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Text("INSIGHT_DEPTH", color = Color(0xFF00FF9C).copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Column {
                                Slider(
                                    value = if (insightDepth == "LIGHT") 0f else 1f,
                                    onValueChange = { 
                                        val newDepth = if (it < 0.5f) "LIGHT" else "DETAILED"
                                        if (newDepth != insightDepth) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.setInsightDepth(newDepth)
                                        }
                                    },
                                    steps = 0,
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFF00FF9C), activeTrackColor = Color(0xFF00FF9C))
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("LIGHT", color = if (insightDepth == "LIGHT") Color(0xFF00FF9C) else Color.Gray, fontSize = 10.sp)
                                    Text("DETAILED", color = if (insightDepth == "DETAILED") Color(0xFF00FF9C) else Color.Gray, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                "NOTE: High-value recovery flags will bypass quiet hours if 'CRITICAL_PINGS' is active in System Kernel.",
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun TimeField(label: String, time: String, modifier: Modifier = Modifier, onTimeSelected: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    var tempTime by remember { mutableStateOf(time) }

    Column(modifier = modifier) {
        Text(label, color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF1A1A1A))
                .border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.3f))
                .clickable { showDialog = true }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(time, color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        }
    }

    if (showDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F0F0F))
                    .border(1.dp, Color(0xFF00FF9C))
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("MANUAL_TIME_ENTRY", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempTime,
                        onValueChange = { tempTime = it },
                        placeholder = { Text("HH:mm", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF9C),
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White
                        )
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = {
                            onTimeSelected(tempTime)
                            showDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
                    ) {
                        Text("SET_TIME", color = Color.Black)
                    }
                }
            }
        }
    }
}
