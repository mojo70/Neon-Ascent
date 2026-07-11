package com.neon.ascent.feature.workout.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.domain.workout.models.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
    ) {
        NeuralProgressHeader(
            currentStep = uiState.currentStep,
            totalSteps = 6,
            onBack = { viewModel.prevStep() }
        )

        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "step_transition"
            ) { step ->
                when (step) {
                    1 -> StepSyncing(uiState)
                    2 -> StepExperience(uiState, onSelect = { viewModel.updateExperience(it) })
                    3 -> StepAttributeCalibration(
                        state = uiState,
                        onRemindLater = { viewModel.showReminderDialog() }
                    )
                    4 -> StepHardwareCheck(uiState, onToggle = { viewModel.toggleInjury(it) })
                    5 -> StepChronosCalibration(
                        state = uiState,
                        onUpdateSchedule = { viewModel.updateSchedule(it) },
                        onToggleApplyToAll = { viewModel.toggleApplyTimeToAll() }
                    )
                    6 -> StepProtocolSynthesis(uiState)
                }
            }
        }

        if (uiState.showReminderDialog) {
            AttributeReminderDialog(
                onDismiss = { viewModel.hideReminderDialog() },
                onSchedule = { viewModel.scheduleAttributeScanReminder(it) }
            )
        }

        OnboardingBottomBar(
            uiState = uiState,
            onNext = { viewModel.nextStep() }
        )
    }
}

@Composable
fun NeuralProgressHeader(currentStep: Int, totalSteps: Int, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (currentStep > 1) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Gray)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "NEURAL INITIALIZATION",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            if (currentStep > 1) Box(Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 1..totalSteps) {
                val color = when {
                    i < currentStep -> Color(0xFF00FF9C)
                    i == currentStep -> Color(0xFF00CCFF)
                    else -> Color.DarkGray
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(color, RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

@Composable
fun StepSyncing(state: OnboardingUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Sync, contentDescription = null, tint = Color(0xFF00CCFF), modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Text("SYNCING BIOMETRICS", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
        Text("Establishing neural link with Identity Forge...", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))

        Spacer(modifier = Modifier.height(48.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF00CCFF).copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                SyncRow("AGE", "${state.profile.age}")
                val weightDisplay = if (state.profile.unitSystem == UnitSystem.IMPERIAL) {
                    "${"%.1f".format(state.profile.weightKg * 2.20462f)} LBS"
                } else {
                    "${"%.1f".format(state.profile.weightKg)} KG"
                }
                SyncRow("WEIGHT", weightDisplay)
                SyncRow("SOMATOTYPE", state.profile.somatotype.name)
                SyncRow("UNITS", state.profile.unitSystem.name)
            }
        }
    }
}

@Composable
fun SyncRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color(0xFF00CCFF), fontSize = 14.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun StepExperience(state: OnboardingUiState, onSelect: (ExperienceLevel) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("NEURAL EXPERIENCE", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Calibrate complexity based on training history.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

        ExperienceLevel.entries.forEach { level ->
            val isSelected = state.profile.experienceLevel == level
            val description = when (level) {
                ExperienceLevel.NOVICE -> "Learning fundamental patterns. Focus on form and neural adaptation."
                ExperienceLevel.INTERMEDIATE -> "Established base. Familiar with progressive overload and core protocols."
                ExperienceLevel.ADVANCED -> "High-level operative. Mastery of complex movements and intensity techniques."
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onSelect(level) },
                color = if (isSelected) Color(0xFF00CCFF).copy(alpha = 0.1f) else Color(0xFF1C1C1E),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isSelected) Color(0xFF00CCFF) else Color.Transparent)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isSelected, onClick = { onSelect(level) }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00CCFF)))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(level.name, color = Color.White, fontWeight = FontWeight.Black)
                        Text(description, color = Color.Gray, fontSize = 11.sp, lineHeight = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StepAttributeCalibration(state: OnboardingUiState, onRemindLater: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ATTRIBUTE CALIBRATION", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth())
        Text("Analyzing performance markers...", color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp).fillMaxWidth())

        if (state.hasScanData) {
            Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1C1C1E), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFF00FF9C).copy(alpha = 0.3f))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    AttributeBar("STRENGTH", state.scanStrength ?: 0)
                    AttributeBar("ENDURANCE", state.scanEndurance ?: 0)
                    AttributeBar("AGILITY", state.scanAgility ?: 0)
                }
            }
        } else {
            NoScanPanel(onRemindLater)
        }
    }
}

@Composable
fun NoScanPanel(onRemindLater: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1C1C1E), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f))
            Text("NO SCAN DATA FOUND", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            Text("Attributes unknown. Protocol calibration will be less accurate.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = { /* Navigate to Scan */ }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CCFF))) {
        Text("PERFORM ATTRIBUTE SCAN", color = Color.Black, fontWeight = FontWeight.Black)
    }
    TextButton(onClick = onRemindLater, modifier = Modifier.padding(top = 16.dp)) {
        Text("REMIND ME LATER", color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AttributeBar(label: String, value: Int) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text("$value", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
        LinearProgressIndicator(progress = { value / 100f }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp).height(2.dp).clip(CircleShape), color = Color(0xFF00FF9C), trackColor = Color.DarkGray)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StepHardwareCheck(state: OnboardingUiState, onToggle: (String) -> Unit) {
    val bodyParts = listOf("Left Shoulder", "Right Shoulder", "Lower Back", "Left Knee", "Right Knee", "Neck", "Wrist")
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("HARDWARE CHECK", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Flag physical constraints.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))
        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            bodyParts.forEach { part ->
                val isSelected = state.profile.injuries.contains(part)
                FilterChip(selected = isSelected, onClick = { onToggle(part) }, label = { Text(part.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Black) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.Red.copy(alpha = 0.2f), selectedLabelColor = Color.Red, labelColor = Color.Gray))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepChronosCalibration(state: OnboardingUiState, onUpdateSchedule: (List<ScheduledDay>) -> Unit, onToggleApplyToAll: () -> Unit) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    var showTimePickerForDay by remember { mutableStateOf<Int?>(null) }
    val timePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("CHRONOS CALIBRATION", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text("Schedule training within your neural windows.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("APPLY SAME TIME TO ALL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Switch(checked = state.applyTimeToAll, onCheckedChange = { onToggleApplyToAll() }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00CCFF)))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEachIndexed { index, label ->
                val dayId = index + 1
                val scheduled = state.profile.scheduledDays.find { it.dayOfWeek == dayId }
                val isSelected = scheduled != null
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(modifier = Modifier.size(40.dp).clickable { if (isSelected) onUpdateSchedule(state.profile.scheduledDays.filter { it.dayOfWeek != dayId }) else onUpdateSchedule(state.profile.scheduledDays + ScheduledDay(dayId, state.profile.scheduledDays.firstOrNull()?.time ?: "09:00")) }, color = if (isSelected) Color(0xFF00CCFF).copy(alpha = 0.1f) else Color(0xFF1C1C1E), shape = CircleShape, border = BorderStroke(1.dp, if (isSelected) Color(0xFF00CCFF) else Color.DarkGray)) {
                        Box(contentAlignment = Alignment.Center) { Text(label, color = if (isSelected) Color(0xFF00CCFF) else Color.Gray, fontWeight = FontWeight.Bold) }
                    }
                    if (scheduled != null) Text(scheduled.time, color = Color.Gray, fontSize = 9.sp, modifier = Modifier.padding(top = 4.dp).clickable { showTimePickerForDay = dayId })
                }
            }
        }

        if (showTimePickerForDay != null) {
            AlertDialog(onDismissRequest = { showTimePickerForDay = null }, confirmButton = { TextButton(onClick = { val newTime = "%02d:%02d".format(timePickerState.hour, timePickerState.minute); if (state.applyTimeToAll) onUpdateSchedule(state.profile.scheduledDays.map { it.copy(time = newTime) }) else onUpdateSchedule(state.profile.scheduledDays.map { if (it.dayOfWeek == showTimePickerForDay) it.copy(time = newTime) else it }); showTimePickerForDay = null }) { Text("CONFIRM", color = Color(0xFF00CCFF)) } }, title = { Text("SELECT TIME", color = Color.White) }, text = { TimePicker(state = timePickerState) }, containerColor = Color(0xFF1C1C1E))
        }
    }
}

@Composable
fun StepProtocolSynthesis(state: OnboardingUiState) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("PROTOCOL SYNTHESIS", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.fillMaxWidth())
        state.recommendation?.let { routine ->
            val protocolName = when(routine.protocol) {
                WorkoutProtocol.CYBER_CRAPP -> "CYBERCRAPP"
                WorkoutProtocol.STRAIGHT_SETS -> "STRAIGHT SETS"
                WorkoutProtocol.DUP -> "D.U.P."
                WorkoutProtocol.SUPERSETS -> "SUPERSETS"
                WorkoutProtocol.GENERAL -> "GENERAL"
            }
            
            Surface(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), color = Color(0xFF1C1C1E), shape = RoundedCornerShape(16.dp), border = BorderStroke(2.dp, Color(0xFF00FF9C))) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("RECOMMENDED CORE", color = Color(0xFF00FF9C), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(protocolName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    if (routine.protocol == WorkoutProtocol.CYBER_CRAPP) {
                        ModifierBadge("CYBERCRAPP ENABLED", Color(0xFF00FFAA))
                    }
                }
            }
        }
    }
}

@Composable
fun ModifierBadge(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributeReminderDialog(onDismiss: () -> Unit, onSchedule: (LocalDateTime) -> Unit) {
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now().plusDays(1)) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
    val timePickerState = rememberTimePickerState(initialHour = 9, initialMinute = 0)
    var showTimePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), color = Color(0xFF1C1C1E), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color(0xFF00CCFF).copy(alpha = 0.5f))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("SCHEDULE REMINDER", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                if (!showTimePicker) {
                    DatePicker(state = datePickerState, showModeToggle = false, title = null, headline = null)
                    Button(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) { Text("SELECT TIME") }
                } else {
                    TimePicker(state = timePickerState)
                    Button(onClick = { val date = datePickerState.selectedDateMillis?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()).toLocalDate() } ?: selectedDate; onSchedule(LocalDateTime.of(date, java.time.LocalTime.of(timePickerState.hour, timePickerState.minute))) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("SCHEDULE") }
                }
            }
        }
    }
}

@Composable
fun OnboardingBottomBar(uiState: OnboardingUiState, onNext: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding()) {
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)), shape = RoundedCornerShape(12.dp)) {
            Text(if (uiState.currentStep == 6) "INITIALIZE UPLINK" else "PROCEED", color = Color.Black, fontWeight = FontWeight.Black)
        }
    }
}
