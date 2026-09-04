package com.neon.ascent.feature.workout.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.domain.workout.models.*

@Composable
fun WorkoutSettingsScreen(
    uiState: WorkoutUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onUpdateProfile: (UserWorkoutProfile) -> Unit,
    onResetProfile: () -> Unit,
    onUpdateRestTimerMode: (RestTimerMode) -> Unit
) {
    val tempProfile = uiState.tempSettingsProfile ?: uiState.userProfile ?: return
    var showResetConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text(
                "WORKOUT SETTINGS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            TextButton(onClick = onSave) {
                Text("SAVE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Category: INTELLIGENT SEQUENCE
            item {
                SettingsCategoryHeader("INTELLIGENT SEQUENCE", MaterialTheme.colorScheme.primary)
                
                SettingsToggleRow(
                    label = "Dashboard Sequencer",
                    description = "Highlights the next routine due in your rotation (A/B/C).",
                    checked = tempProfile.sequencerEnabled,
                    onCheckedChange = { onUpdateProfile(tempProfile.copy(sequencerEnabled = it)) }
                )

                if (tempProfile.sequencerEnabled) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "ROTATION PREFERENCE",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    if (tempProfile.activeProtocol != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Adjust, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("Protocol Managed", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Rotation index: ${tempProfile.rotationIndex}", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                    } else {
                        // Custom Sequence Builder
                        Text(
                            "Drag routines into your preferred rotation order in the Library to enable auto-sequencing.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        // Simplified custom sequence IDs management for now
                        // In a real app we'd have a drag-drop list of user's routines here
                        tempProfile.customSequenceIds.forEachIndexed { index, id ->
                            val routineName = uiState.routines.find { it.id == id }?.name ?: "Unknown Routine"
                            Surface(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                color = Color(0xFF1C1C1E),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, modifier = Modifier.width(24.dp))
                                    Text(routineName, color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Category: PROGRESSION BRAIN
            item {
                SettingsCategoryHeader("PROGRESSION BRAIN", Color(0xFF00CCFF))
                
                SettingsToggleRow(
                    label = "Auto Weight Increment",
                    description = "Automatically set the LBS placeholder for your next session when target reps are hit.",
                    checked = tempProfile.autoWeightIncrement,
                    onCheckedChange = { onUpdateProfile(tempProfile.copy(autoWeightIncrement = it)) }
                )

                if (tempProfile.autoWeightIncrement) {
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IncrementInput(
                            label = "Compound (+)",
                            value = tempProfile.weightIncrementCompound,
                            onUpdate = { onUpdateProfile(tempProfile.copy(weightIncrementCompound = it)) },
                            modifier = Modifier.weight(1f)
                        )
                        IncrementInput(
                            label = "Isolation (+)",
                            value = tempProfile.weightIncrementIsolation,
                            onUpdate = { onUpdateProfile(tempProfile.copy(weightIncrementIsolation = it)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Category: REST & INTENSITY
            item {
                SettingsCategoryHeader("REST & INTENSITY", Color(0xFFFF006E))
                
                Text("REST TIMER MODE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RestTimerMode.entries.filter { it != RestTimerMode.NONE }.forEach { mode ->
                        val isSelected = uiState.restTimerMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateRestTimerMode(mode) },
                            color = if (isSelected) Color(0xFFFF006E).copy(alpha = 0.2f) else Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFFF006E) else Color.Transparent)
                        ) {
                            Text(
                                mode.name,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                SettingsToggleRow(
                    label = "Granular RIR Capture",
                    description = "Record intensity after every mini-set instead of once per cluster.",
                    checked = tempProfile.rirCapturePerMiniSet,
                    onCheckedChange = { onUpdateProfile(tempProfile.copy(rirCapturePerMiniSet = it)) }
                )

                SettingsToggleRow(
                    label = "Breathing Vibration",
                    description = "Phone pulses on 'BREATHE IN' during CyberCrapp stretches.",
                    checked = tempProfile.breathingVibrationEnabled,
                    onCheckedChange = { onUpdateProfile(tempProfile.copy(breathingVibrationEnabled = it)) }
                )
            }

            // Category: CYBER FIDELITY
            item {
                SettingsCategoryHeader("CYBER FIDELITY", Color(0xFFFFA500))
                
                SettingsToggleRow(
                    label = "Neural Uplink Hints",
                    description = "Show unobtrusive coaching cues based on your recovery score.",
                    checked = tempProfile.coachingHintsEnabled,
                    onCheckedChange = { onUpdateProfile(tempProfile.copy(coachingHintsEnabled = it)) }
                )
            }

            // Category: DANGER ZONE
            item {
                SettingsCategoryHeader("DANGER ZONE", Color.Red)
                
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showResetConfirmation = true },
                    color = Color.Red.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color.Red)
                            Spacer(Modifier.width(16.dp))
                            Text("RESET UPLINK / REDO INTAKE", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "This will clear your physical operational profile and trigger a fresh intake sequence. History and custom routines are preserved.",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        if (showResetConfirmation) {
            AlertDialog(
                onDismissRequest = { showResetConfirmation = false },
                title = { Text("CONFIRM RESET?", color = Color.White, fontWeight = FontWeight.Black) },
                text = { Text("This will wipe your current biometrics and protocol settings. You will need to redo the neural onboarding.", color = Color.Gray) },
                confirmButton = {
                    TextButton(onClick = { 
                        showResetConfirmation = false
                        onResetProfile() 
                    }) {
                        Text("RESET EVERYTHING", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirmation = false }) {
                        Text("CANCEL", color = Color.White)
                    }
                },
                containerColor = Color(0xFF1C1C1E)
            )
        }
    }
}

@Composable
fun SettingsCategoryHeader(title: String, color: Color) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            title,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        HorizontalDivider(color = color.copy(alpha = 0.3f), modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
fun SettingsToggleRow(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Color.Gray, fontSize = 11.sp, lineHeight = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
fun IncrementInput(label: String, value: Float, onUpdate: (Float) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text("${value} lbs", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(onClick = { onUpdate((value - 2.5f).coerceAtLeast(0f)) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { onUpdate(value + 2.5f) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
