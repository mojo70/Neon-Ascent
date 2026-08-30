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
    onPerformScan: () -> Unit,
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
                        onRemindLater = { viewModel.showReminderDialog() },
                        onPerformScan = onPerformScan
                    )
                    4 -> StepHardwareCheck(uiState, onToggle = { viewModel.toggleInjury(it) })
                    5 -> StepProtocolSynthesis(
                        state = uiState,
                        onViewAlternate = { viewModel.showAlternateProtocols() }
                    )
                    6 -> StepChronosCalibration(
                        state = uiState,
                        onUpdateSchedule = { viewModel.updateSchedule(it) },
                        onToggleApplyToAll = { viewModel.toggleApplyTimeToAll() }
                    )
                }
            }
        }

        if (uiState.showReminderDialog) {
            AttributeReminderDialog(
                onDismiss = { viewModel.hideReminderDialog() },
                onSchedule = { viewModel.scheduleAttributeScanReminder(it) }
            )
        }

        if (uiState.showAlternateProtocols) {
            AlternateProtocolsDialog(
                routines = uiState.availableRoutines,
                onDismiss = { viewModel.hideAlternateProtocols() },
                onSelect = { viewModel.selectProtocol(it) }
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

        ExperienceLevel.entries.filter { it != ExperienceLevel.ANY }.forEach { level ->
            val isSelected = state.profile.experienceLevel == level
            val description = when (level) {
                ExperienceLevel.NOVICE -> "Learning fundamental patterns. Focus on form and neural adaptation."
                ExperienceLevel.INTERMEDIATE -> "Established base. Familiar with progressive overload and core protocols."
                ExperienceLevel.ADVANCED -> "High-level operative. Mastery of complex movements and intensity techniques."
                else -> ""
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
fun StepAttributeCalibration(state: OnboardingUiState, onRemindLater: () -> Unit, onPerformScan: () -> Unit) {
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
            NoScanPanel(onRemindLater, onPerformScan)
        }
    }
}

@Composable
fun NoScanPanel(onRemindLater: () -> Unit, onPerformScan: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFF1C1C1E), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red.copy(alpha = 0.7f))
            Text("NO SCAN DATA FOUND", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
            Text("Attributes unknown. Protocol calibration will be less accurate.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onPerformScan, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CCFF))) {
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

    val protocol = state.profile.activeProtocol
    val title = if (protocol != null) {
        "REMINDERS · ${protocol.displayName} · ${protocol.defaultWeekdays.size}× / WEEK"
    } else {
        "CHRONOS CALIBRATION"
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
        
        if (protocol != null) {
            val selectedCount = state.profile.scheduledDays.size
            val recommendedDays = protocol.defaultWeekdays.size
            
            val warnText = when {
                protocol == WorkoutProtocol.HST && selectedCount > recommendedDays -> "LADDER ASSUMES 3×. EXTRA DAYS COMPRESS THE WAVE."
                protocol == WorkoutProtocol.STARTING_STRENGTH && selectedCount != recommendedDays -> "3× IS THE LINEAR PROGRESSION CADENCE."
                protocol == WorkoutProtocol.FIVE_THREE_ONE && selectedCount < recommendedDays -> "4TH MAIN WILL LAG THIS CYCLE."
                protocol == WorkoutProtocol.WESTSIDE && selectedCount != recommendedDays -> "MISSING A ME OR DE DAY."
                else -> null
            }
            
            warnText?.let {
                Surface(
                    color = Color(0xFFFF006E).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF006E).copy(alpha = 0.5f)),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF006E), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = Color(0xFFFF006E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Text("Schedule training within your neural windows.", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("APPLY SAME TIME TO ALL", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Switch(checked = state.applyTimeToAll, onCheckedChange = { onToggleApplyToAll() }, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00CCFF)))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEachIndexed { index, label ->
                val dayId = index + 1
                val scheduled = state.profile.scheduledDays.find { it.dayOfWeek == dayId }
                val isSelected = scheduled != null
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(46.dp)
                            .clickable {
                                if (isSelected) {
                                    onUpdateSchedule(state.profile.scheduledDays.filter { it.dayOfWeek != dayId })
                                } else {
                                    onUpdateSchedule(
                                        state.profile.scheduledDays + ScheduledDay(
                                            dayId,
                                            state.profile.scheduledDays.firstOrNull()?.time ?: "09:00"
                                        )
                                    )
                                }
                            },
                        color = if (isSelected) Color(0xFF00CCFF).copy(alpha = 0.15f) else Color(0xFF1C1C1E),
                        shape = CircleShape,
                        border = BorderStroke(1.5.dp, if (isSelected) Color(0xFF00CCFF) else Color.DarkGray)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                label,
                                color = if (isSelected) Color(0xFF00CCFF) else Color.Gray,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                    if (scheduled != null) {
                        Surface(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .clickable { showTimePickerForDay = dayId },
                            color = Color(0xFF2C2C2E),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                scheduled.time,
                                color = Color(0xFF00CCFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        if (showTimePickerForDay != null) {
            val initialTime = state.profile.scheduledDays.find { it.dayOfWeek == showTimePickerForDay }?.time ?: "09:00"
            val initialHour = initialTime.split(":")[0].toIntOrNull() ?: 9
            val initialMinute = initialTime.split(":")[1].toIntOrNull() ?: 0

            val dialogTimePickerState = rememberTimePickerState(
                initialHour = initialHour,
                initialMinute = initialMinute
            )

            AlertDialog(
                onDismissRequest = { showTimePickerForDay = null },
                confirmButton = {
                    TextButton(onClick = {
                        val newTime = "%02d:%02d".format(dialogTimePickerState.hour, dialogTimePickerState.minute)
                        if (state.applyTimeToAll) {
                            onUpdateSchedule(state.profile.scheduledDays.map { it.copy(time = newTime) })
                        } else {
                            onUpdateSchedule(state.profile.scheduledDays.map {
                                if (it.dayOfWeek == showTimePickerForDay) it.copy(time = newTime) else it
                            })
                        }
                        showTimePickerForDay = null
                    }) {
                        Text("CONFIRM", color = Color(0xFF00CCFF), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePickerForDay = null }) {
                        Text("CANCEL", color = Color.Gray)
                    }
                },
                title = {
                    Text(
                        "TRAINING TIME WINDOW",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TimePicker(state = dialogTimePickerState)
                    }
                },
                containerColor = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun StepProtocolSynthesis(state: OnboardingUiState, onViewAlternate: () -> Unit) {
    var showTenantsOverview by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "PROTOCOL SYNTHESIS",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth()
        )
        
        state.recommendation?.let { routine ->
            val protocolName = routine.protocol.displayName
            
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                color = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, Color(0xFF00FF9C))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("RECOMMENDED CORE", color = Color(0xFF00FF9C), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            Text(protocolName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                        if (routine.protocol == WorkoutProtocol.CYBER_CRAPP) {
                            ModifierBadge("REST-PAUSE PROTOCOL", Color(0xFF00FFAA))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        routine.protocol.description,
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showTenantsOverview = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF00FF9C))
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = Color(0xFF00FF9C), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "REVIEW PROTOCOL TENANTS & EXECUTION",
                            color = Color(0xFF00FF9C),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF141416),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF2C2C2E))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "CORE METHODOLOGY",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        routine.protocol.methodology,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            TextButton(onClick = onViewAlternate, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "VIEW ALTERNATE PROTOCOLS",
                    color = Color(0xFF00CCFF),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }

    if (showTenantsOverview && state.recommendation != null) {
        ProtocolOverviewModal(
            protocol = state.recommendation.protocol,
            onDismiss = { showTenantsOverview = false }
        )
    }
}

@Composable
fun ProtocolOverviewModal(
    protocol: WorkoutProtocol,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            color = Color(0xFF0D0D0D),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF00FF9C).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "PROTOCOL PROTO-INTEL",
                            color = Color(0xFF00FF9C),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            protocol.displayName,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (protocol == WorkoutProtocol.CYBER_CRAPP) {
                    CyberCrappTenantsContent()
                } else {
                    GenericProtocolTenantsContent(protocol)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("I UNDERSTAND THE SYSTEM", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CyberCrappTenantsContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TenantCard(
            title = "1. REST-PAUSE CLUSTERS (THE ENGINE)",
            badge = "CORE MECHANIC",
            badgeColor = Color(0xFF00FFAA),
            explanation = "Instead of 3-4 standard sets, you perform 1 brutal cluster consisting of 3 mini-sets to failure with 15 seconds rest between mini-sets:\n\n" +
                    "• Mini-Set 1: Take the target weight to failure (0-1 RIR, typically 6-10 reps).\n" +
                    "• Rest 15 seconds: Deep diaphragmatic breaths.\n" +
                    "• Mini-Set 2: Pick the same weight back up and push to failure (typically 2-4 reps).\n" +
                    "• Rest 15 seconds: Focus neural drive.\n" +
                    "• Mini-Set 3: Final push to failure (typically 1-3 reps).\n\n" +
                    "Goal: Maximize 'Effective Reps' in minimal session time."
        )

        TenantCard(
            title = "2. CYBER FINISHER (LENGTHENED PARTIALS)",
            badge = "HYPERTROPHY MULTIPLIER",
            badgeColor = Color(0xFF00CCFF),
            explanation = "Immediately following your 3rd mini-set, drop the load or keep dumbbells and perform 3 to 5 controlled partial reps strictly in the stretched (lengthened) range of motion. This triggers stretch-mediated hypertrophy without joint damage."
        )

        TenantCard(
            title = "3. LOADED ACTIVE STRETCHES",
            badge = "RECOVERY & FASCIA",
            badgeColor = Color(0xFFFF006E),
            explanation = "Perform a 30 to 45 second loaded stretch targeting the worked muscle (e.g. deep dumbbell fly stretch for chest, hang for back). The app provides automated vibration cues to pace your breathing."
        )

        TenantCard(
            title = "4. BEAT THE LOGBOOK (PROGRESSIVE OVERLOAD)",
            badge = "RULE OF LAW",
            badgeColor = Color(0xFFFFD700),
            explanation = "Keep the weight constant and chase higher total cluster reps (Mini 1 + 2 + 3). Once you exceed your target rep range (e.g., 15 total reps), the system automatically prompts you to bump the weight on your next session."
        )

        TenantCard(
            title = "5. AUTO-ROTATION ON STALL",
            badge = "GUARDRAIL",
            badgeColor = Color(0xFFFF5555),
            explanation = "If you fail to beat your previous session's reps or weight twice in a row, the app flags a stagnation and recommends rotating to a safe alternative movement so your nervous system never adapts."
        )
    }
}

@Composable
private fun GenericProtocolTenantsContent(protocol: WorkoutProtocol) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        protocol.tenants.forEachIndexed { index, tenant ->
            TenantCard(
                title = "TENANT 0${index + 1}",
                badge = "PRINCIPLE",
                badgeColor = Color(0xFF00FF9C),
                explanation = tenant
            )
        }
    }
}

@Composable
private fun TenantCard(
    title: String,
    badge: String,
    badgeColor: Color,
    explanation: String
) {
    Surface(
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Surface(
                    color = badgeColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(0.5.dp, badgeColor)
                ) {
                    Text(
                        badge,
                        color = badgeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(explanation, color = Color(0xFFD0D0D0), fontSize = 12.sp, lineHeight = 17.sp)
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

@Composable
fun AlternateProtocolsDialog(
    routines: List<WorkoutRoutine>,
    onDismiss: () -> Unit,
    onSelect: (WorkoutRoutine) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            color = Color(0xFF0D0D0D),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF00CCFF).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxHeight(0.8f)
            ) {
                Text(
                    "PROTOCOL REPLACEMENT",
                    color = Color(0xFF00CCFF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    "Select a different operational core for your frame.",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    routines.forEach { routine ->
                        ProtocolSelectionCard(routine) { onSelect(routine) }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CANCEL", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProtocolSelectionCard(routine: WorkoutRoutine, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color(0xFF1C1C1E),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFF2C2C2E))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    routine.protocol.displayName,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                val level = routine.protocol.recommendation
                if (level != null) {
                    Text(
                        level.uppercase(),
                        color = Color(0xFF00CCFF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                routine.protocol.description,
                color = Color.Gray,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
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
