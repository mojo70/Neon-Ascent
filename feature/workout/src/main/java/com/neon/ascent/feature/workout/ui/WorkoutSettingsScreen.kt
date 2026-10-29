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
    onUpdateRestTimerMode: (RestTimerMode) -> Unit,
    onUpdateLayoutMode: ((WorkoutLayoutMode) -> Unit)? = null,
    onUpdateCustomNumpad: ((Boolean) -> Unit)? = null,
    onUpdateCyberCrappRotationMode: ((CyberCrappRotationMode) -> Unit)? = null,
    onUpdateRoutineSchedule: ((String, List<ScheduledDay>) -> Unit)? = null,
    onStartDeload: () -> Unit = {},
    onEndDeloadEarly: () -> Unit = {}
) {
    val tempProfile = uiState.tempSettingsProfile ?: uiState.userProfile ?: return
    var showResetConfirmation by remember { mutableStateOf(false) }
    var editingScheduleTarget by remember { mutableStateOf<ScheduleTarget?>(null) }

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
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Adjust, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text("Protocol Managed", color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(
                                            uiState.nextSequencedRoutine?.name?.let { "Next session: $it" }
                                                ?: "Next session follows your last workout.",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                if (uiState.activeDeloadWindow != null && uiState.activeDeloadWindow.status == DeloadWindowStatus.ACTIVE) {
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = onEndDeloadEarly,
                                        modifier = Modifier.fillMaxWidth().height(40.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB703)),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("END SOFT DELOAD EARLY", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                } else if (tempProfile.activeProtocol == WorkoutProtocol.CYBER_CRAPP) {
                                    Spacer(Modifier.height(12.dp))
                                    Text("CYBERCRAPP ROTATION MODE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CyberCrappRotationMode.entries.forEach { mode ->
                                            val isSelected = uiState.cyberCrappRotationMode == mode
                                            Surface(
                                                onClick = { onUpdateCyberCrappRotationMode?.invoke(mode) },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2C2C2E),
                                                border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = if (mode == CyberCrappRotationMode.STRAIGHT) "STRAIGHT" else "A / B",
                                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (uiState.cyberCrappRotationMode == CyberCrappRotationMode.STRAIGHT)
                                            "Straight keeps the same Push, Pull, and Legs lifts."
                                        else
                                            "A/B keeps those three days and alternates each variant.",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = onStartDeload,
                                        modifier = Modifier.fillMaxWidth().height(40.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("START SOFT DELOAD", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
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

            // Category: WORKOUT SCHEDULES
            item {
                SettingsCategoryHeader("WORKOUT SCHEDULES", Color(0xFFFFB703))

                val activeProtocol = tempProfile.activeProtocol
                val cyberCrappManaged = activeProtocol == WorkoutProtocol.CYBER_CRAPP
                Text(
                    if (cyberCrappManaged) {
                        "Set the days you train. CyberCrapp chooses the session from your last workout."
                    } else {
                        "View and configure weekly workout schedules for active protocols and routines."
                    },
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                ScheduleItemCard(
                    title = "Training days",
                    subtitle = if (cyberCrappManaged) {
                        "Profile schedule. Sessions rotate on these days."
                    } else {
                        "Weekly target schedule"
                    },
                    scheduledDays = tempProfile.scheduledDays,
                    onEdit = {
                        editingScheduleTarget = ScheduleTarget.Profile
                    }
                )

                if (cyberCrappManaged) {
                    Spacer(Modifier.height(12.dp))
                    CyberCrappSessionSummary(
                        straight = uiState.cyberCrappRotationMode == CyberCrappRotationMode.STRAIGHT
                    )
                }

                val individualRoutines = WorkoutSchedulePolicy.routinesWithIndividualSchedules(
                    routines = uiState.allRoutines,
                    activeProtocol = activeProtocol
                )

                if (individualRoutines.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (cyberCrappManaged) "OTHER ROUTINES" else "ACTIVE PROTOCOL & SUB-ROUTINE SCHEDULES",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    individualRoutines.forEach { routine ->
                        ScheduleItemCard(
                            title = routine.name,
                            subtitle = routine.protocol.displayName,
                            scheduledDays = routine.scheduledDays,
                            onEdit = {
                                editingScheduleTarget = ScheduleTarget.Routine(routine)
                            }
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                } else if (!cyberCrappManaged) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            "No active routines found for current protocol.",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
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
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "PROGRESSION ENGINE MODE",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isFixed = tempProfile.progressionMode == ProgressionMode.FIXED_INCREMENT
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateProfile(tempProfile.copy(progressionMode = ProgressionMode.FIXED_INCREMENT)) },
                            color = if (isFixed) Color(0xFF00CCFF).copy(alpha = 0.2f) else Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isFixed) Color(0xFF00CCFF) else Color.Transparent)
                        ) {
                            Text(
                                "FIXED (+LBS)",
                                color = if (isFixed) Color.White else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        val isPercent = tempProfile.progressionMode == ProgressionMode.PERCENTAGE
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateProfile(tempProfile.copy(progressionMode = ProgressionMode.PERCENTAGE)) },
                            color = if (isPercent) Color(0xFF00CCFF).copy(alpha = 0.2f) else Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isPercent) Color(0xFF00CCFF) else Color.Transparent)
                        ) {
                            Text(
                                "PERCENTAGE (%)",
                                color = if (isPercent) Color.White else Color.Gray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (tempProfile.progressionMode == ProgressionMode.FIXED_INCREMENT) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IncrementInput(
                                    label = "Upper Compound (+)",
                                    value = tempProfile.weightIncrementUpperCompound,
                                    onUpdate = { onUpdateProfile(tempProfile.copy(weightIncrementUpperCompound = it, weightIncrementCompound = it)) },
                                    modifier = Modifier.weight(1f)
                                )
                                IncrementInput(
                                    label = "Lower Compound (+)",
                                    value = tempProfile.weightIncrementLowerCompound,
                                    onUpdate = { onUpdateProfile(tempProfile.copy(weightIncrementLowerCompound = it)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            IncrementInput(
                                label = "Isolation / Accessory (+)",
                                value = tempProfile.weightIncrementIsolation,
                                onUpdate = { onUpdateProfile(tempProfile.copy(weightIncrementIsolation = it)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                StepInput(
                                    label = "Progression Step",
                                    valueText = "${"%.1f".format(tempProfile.progressionPercentage)}%",
                                    onDecrement = { onUpdateProfile(tempProfile.copy(progressionPercentage = (tempProfile.progressionPercentage - 0.5f).coerceAtLeast(0.5f))) },
                                    onIncrement = { onUpdateProfile(tempProfile.copy(progressionPercentage = tempProfile.progressionPercentage + 0.5f)) },
                                    modifier = Modifier.weight(1f)
                                )
                                StepInput(
                                    label = "Bar Load Step",
                                    valueText = "${"%.1f".format(tempProfile.roundingIncrement)} lbs",
                                    onDecrement = { onUpdateProfile(tempProfile.copy(roundingIncrement = if (tempProfile.roundingIncrement <= 2.5f) 2.5f else tempProfile.roundingIncrement - 2.5f)) },
                                    onIncrement = { onUpdateProfile(tempProfile.copy(roundingIncrement = tempProfile.roundingIncrement + 2.5f)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text(
                                if (tempProfile.roundingIncrement <= 2.5f) "Micro-plates enabled (1.25 lb micro-plates required for 2.5 lb total steps)."
                                else "Standard plate step (5.0 lb step = two 2.5 lb plates).",
                                color = Color.Gray,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
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

                Spacer(Modifier.height(16.dp))
                Text("WORKOUT LAYOUT MODE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkoutLayoutMode.entries.forEach { mode ->
                        val isSelected = uiState.layoutMode == mode
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateLayoutMode?.invoke(mode) },
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
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
                    label = "Focus Mode Custom Keypad",
                    description = "Display an oversized custom numeric pad at the bottom in Focus Mode instead of the system soft keyboard.",
                    checked = uiState.useCustomNumpadInFocusMode,
                    onCheckedChange = { onUpdateCustomNumpad?.invoke(it) }
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

        editingScheduleTarget?.let { target ->
            val title = when (target) {
                ScheduleTarget.Profile -> "EDIT PROFILE SCHEDULE"
                is ScheduleTarget.Routine -> "EDIT SCHEDULE: ${target.routine.name}"
            }
            val initialDays = when (target) {
                ScheduleTarget.Profile -> tempProfile.scheduledDays
                is ScheduleTarget.Routine -> target.routine.scheduledDays
            }

            ScheduleEditorDialog(
                title = title,
                initialScheduledDays = initialDays,
                onDismiss = { editingScheduleTarget = null },
                onSave = { newDays ->
                    when (target) {
                        ScheduleTarget.Profile -> onUpdateProfile(tempProfile.copy(scheduledDays = newDays))
                        is ScheduleTarget.Routine -> onUpdateRoutineSchedule?.invoke(target.routine.id, newDays)
                    }
                    editingScheduleTarget = null
                }
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
    StepInput(
        label = label,
        valueText = "${value} lbs",
        onDecrement = { onUpdate((value - 2.5f).coerceAtLeast(0f)) },
        onIncrement = { onUpdate(value + 2.5f) },
        modifier = modifier
    )
}

@Composable
fun StepInput(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                Text(valueText, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(onClick = onDecrement, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Remove, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onIncrement, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

sealed interface ScheduleTarget {
    object Profile : ScheduleTarget
    data class Routine(val routine: WorkoutRoutine) : ScheduleTarget
}

@Composable
private fun CyberCrappSessionSummary(straight: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "PUSH \u2192 PULL \u2192 LEGS",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (straight) {
                    "One rotation. Straight repeats the same lifts for each day. The B variants are not separate sessions."
                } else {
                    "One rotation. A/B swaps the variant inside Push, Pull, and Legs. Those variants are not extra days on the calendar."
                },
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ScheduleItemCard(
    title: String,
    subtitle: String,
    scheduledDays: List<ScheduledDay>,
    onEdit: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (scheduledDays.isNotEmpty()) Color(0xFFFFB703).copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = Color.Gray, fontSize = 10.sp)

                Spacer(Modifier.height(6.dp))

                if (scheduledDays.isEmpty()) {
                    Surface(
                        color = Color(0xFFFF453A).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "UNSCHEDULED",
                            color = Color(0xFFFF453A),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val dayNames = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        val daysFormatted = scheduledDays.sortedBy { it.dayOfWeek }.joinToString(", ") { dayNames.getOrElse(it.dayOfWeek) { "" } }
                        val timeStr = scheduledDays.firstOrNull()?.time ?: ""

                        Text(
                            "$daysFormatted at $timeStr",
                            color = Color(0xFFFFB703),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Button(
                onClick = onEdit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (scheduledDays.isEmpty()) MaterialTheme.colorScheme.primary else Color(0xFF2C2C2E)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (scheduledDays.isEmpty()) "ADD SCHEDULE" else "EDIT",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ScheduleEditorDialog(
    title: String,
    initialScheduledDays: List<ScheduledDay>,
    onDismiss: () -> Unit,
    onSave: (List<ScheduledDay>) -> Unit
) {
    var selectedDays by remember {
        mutableStateOf(initialScheduledDays.map { it.dayOfWeek }.toSet())
    }
    var selectedTime by remember {
        mutableStateOf(initialScheduledDays.firstOrNull()?.time ?: "18:00")
    }

    val dayLabels = listOf("MON" to 1, "TUE" to 2, "WED" to 3, "THU" to 4, "FRI" to 5, "SAT" to 6, "SUN" to 7)
    val quickTimes = listOf("06:00", "08:00", "12:00", "17:00", "18:00", "20:00")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        title = {
            Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("ACTIVE DAYS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dayLabels.forEach { (label, dayNum) ->
                        val isSelected = selectedDays.contains(dayNum)
                        Surface(
                            onClick = {
                                selectedDays = if (isSelected) selectedDays - dayNum else selectedDays + dayNum
                            },
                            modifier = Modifier.weight(1f).height(36.dp),
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF2C2C2E),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(label, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Text("NOTIFICATION / WORKOUT TIME", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    quickTimes.forEach { timeStr ->
                        val isSelected = selectedTime == timeStr
                        Surface(
                            onClick = { selectedTime = timeStr },
                            modifier = Modifier.weight(1f).height(32.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else Color(0xFF2C2C2E)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(timeStr, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = selectedDays.sorted().map { dayNum ->
                        ScheduledDay(dayOfWeek = dayNum, time = selectedTime)
                    }
                    onSave(result)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("SAVE SCHEDULE", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { onSave(emptyList()) }) {
                    Text("CLEAR", color = Color(0xFFFF453A), fontSize = 12.sp)
                }
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    )
}
