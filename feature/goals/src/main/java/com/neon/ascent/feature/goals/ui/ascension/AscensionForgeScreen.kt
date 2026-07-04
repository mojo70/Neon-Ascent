package com.neon.ascent.feature.goals.ui.ascension

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink
import com.neon.ascent.core.common.neonBorder
import com.neon.ascent.core.common.CelebrationOverlay
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.model.SpecialType
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.Composable
import java.time.DayOfWeek

@Composable
fun CyberFrame(
    label: String,
    accentColor: Color = NeonCyan,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF0A080C),
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .border(1.dp, accentColor.copy(alpha = 0.5f))
            .padding(1.dp)
            .border(1.dp, accentColor.copy(alpha = 0.1f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(accentColor.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = label,
                color = accentColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Box(modifier = Modifier.padding(8.dp)) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AscensionForgeScreen(
    onBack: () -> Unit,
    prefilledAttribute: String? = null,
    prefilledTitle: String? = null,
    prefilledDescription: String? = null,
    prefilledVision: String? = null,
    prefilledBiometrics: String? = null,
    onNavigateToGuide: () -> Unit = {},
    viewModel: AscensionForgeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (prefilledAttribute != null || prefilledTitle != null || prefilledDescription != null || prefilledVision != null || prefilledBiometrics != null) {
            viewModel.prefill(prefilledAttribute, prefilledTitle, prefilledDescription, prefilledVision, prefilledBiometrics)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("FORGE // PROTOCOL_INTAKE", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToGuide) {
                        Icon(Icons.Default.Psychology, contentDescription = "Neon Guide", tint = NeonCyan)
                    }
                    IconButton(onClick = { 
                        viewModel.save()
                        onBack()
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save", tint = NeonCyan)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Type Selector
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ForgeTypeTab("DIRECTIVE", uiState.forgeType == ForgeType.DIRECTIVE, Modifier.weight(1f)) {
                    viewModel.updateType(ForgeType.DIRECTIVE)
                }
                ForgeTypeTab("MISSION", uiState.forgeType == ForgeType.MISSION, Modifier.weight(1f)) {
                    viewModel.updateType(ForgeType.MISSION)
                }
                ForgeTypeTab("PULSE", uiState.forgeType == ForgeType.TASK, Modifier.weight(1f)) {
                    viewModel.updateType(ForgeType.TASK)
                }
            }

            if (uiState.isFromInsight) {
                Surface(
                    color = NeonCyan.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Text(
                            "PROTO-SYNTHESIS: STARTED FROM EXTERNAL_INSIGHT",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Common Title/Description
            CyberFrame(label = "CORE_DEFINITION") {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = uiState.title,
                        onValueChange = viewModel::updateTitle,
                        label = { Text("IDENTIFIER", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace)
                    )
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::updateDescription,
                        label = { Text("OBJECTIVE_DATA", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace)
                    )
                }
            }

            if (uiState.useAiMentor && uiState.forgeType == ForgeType.DIRECTIVE) {
                MentorConversationalBuilder(uiState, viewModel)
            }

            if (uiState.forgeType == ForgeType.DIRECTIVE) {
                DirectiveSpecificFields(uiState, viewModel)
            } else if (uiState.forgeType == ForgeType.MISSION) {
                MissionSpecificFields(uiState, viewModel)
            } else {
                TaskSpecificFields(uiState, viewModel)
            }

            Button(
                onClick = { 
                    viewModel.save()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("INITIATE_DEPLOYMENT", fontFamily = FontFamily.Monospace)
            }
        }

        if (uiState.isSuccess) {
            CelebrationOverlay(
                event = com.neon.ascent.core.common.DopamineEvent(
                    level = com.neon.ascent.core.common.CelebrationLevel.ASCENSION,
                    message = "PROTOCOL_ACTIVE // NEURAL_PINGS_ONLINE",
                    xpGained = 50
                ),
                onFinished = { onBack() }
            )
        }
    }
}

@Composable
fun ForgeTypeTab(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(if (isSelected) NeonCyan.copy(alpha = 0.1f) else Color.Transparent)
            .border(1.dp, if (isSelected) NeonCyan else Color.Gray.copy(alpha = 0.3f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isSelected) NeonCyan else Color.Gray,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
    }
}

@Composable
fun MissionSpecificFields(uiState: AscensionForgeUiState, viewModel: AscensionForgeViewModel) {
    CyberFrame(label = "MISSION_PARAMETERS", accentColor = NeonCyan) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("MISSIONS are mid-level campaigns that belong to a DIRECTIVE.", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            
            // In a real app, we'd have a dropdown of directives here.
            // For now, we'll just show it's a mission.
            OutlinedTextField(
                value = uiState.parentDirectiveId ?: "",
                onValueChange = viewModel::updateParentDirective,
                label = { Text("PARENT_DIRECTIVE_ID (OPTIONAL)", fontSize = 10.sp) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DirectiveSpecificFields(uiState: AscensionForgeUiState, viewModel: AscensionForgeViewModel) {
    CyberFrame(label = "DIRECTIVE_PARAMETERS", accentColor = NeonPink) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("LINKED_S.P.E.C.I.A.L_NODES", color = NeonPink, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpecialType.entries.forEach { type ->
                    val isSelected = uiState.linkedAttributes.contains(type)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleAttribute(type) },
                        label = { Text(type.name, fontSize = 10.sp) }
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = uiState.useAiMentor, onCheckedChange = viewModel::updateUseAiMentor)
                Text("ACTIVATE_NEON_MENTOR_GENESIS", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            if (uiState.useAiMentor) {
                Text(
                    "AI will procedurally generate Missions and Tasks based on this Directive objective.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 12.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("NEURAL_EXPERT_MATRIX", color = NeonPink, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val skills = listOf("AUTO_SELECT", "BIOHACKING", "MEDITATION", "REMOTE_VIEWING", "BUSINESS_BUILDING", "TRADING")
                    skills.forEach { skill ->
                        val isSelected = (uiState.selectedSkill == skill) || (uiState.selectedSkill == null && skill == "AUTO_SELECT")
                        FilterChip(
                            selected = isSelected,
                            onClick = { 
                                viewModel.updateSelectedSkill(if (skill == "AUTO_SELECT") null else skill) 
                            },
                            label = { Text(skill, fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPink.copy(alpha = 0.2f),
                                selectedLabelColor = NeonPink
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskSpecificFields(uiState: AscensionForgeUiState, viewModel: AscensionForgeViewModel) {
    CyberFrame(label = "PULSE_EXECUTION_LOGIC", accentColor = NeonCyan) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("LINKED_S.P.E.C.I.A.L_CALIBRATION", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SpecialType.entries.forEach { type ->
                    val isSelected = uiState.linkedAttributes.contains(type)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleAttribute(type) },
                        label = { Text(type.name, fontSize = 10.sp) }
                    )
                }
            }
            
            // Recurring toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = uiState.taskType == AscensionTaskType.ONE_TIME,
                    onClick = { viewModel.updateTaskType(AscensionTaskType.ONE_TIME) }
                )
                Text("ONE_TIME", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.width(8.dp))
                RadioButton(
                    selected = uiState.taskType == AscensionTaskType.RECURRING,
                    onClick = { viewModel.updateTaskType(AscensionTaskType.RECURRING) }
                )
                Text("RECURRING", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }

            if (uiState.taskType == AscensionTaskType.RECURRING) {
                Text("RECURRENCE_PATTERN", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurrenceChip("DAILY", uiState.recurrenceType == RecurrenceTypeV3.DAILY) {
                        viewModel.updateRecurrence(RecurrenceTypeV3.DAILY)
                    }
                    RecurrenceChip("WEEKDAYS", uiState.recurrenceType == RecurrenceTypeV3.WEEKDAYS) {
                        viewModel.updateRecurrence(RecurrenceTypeV3.WEEKDAYS)
                    }
                    RecurrenceChip("CUSTOM_DAYS", uiState.recurrenceType == RecurrenceTypeV3.DAYS_OF_WEEK) {
                        viewModel.updateRecurrence(RecurrenceTypeV3.DAYS_OF_WEEK)
                    }
                }

                if (uiState.recurrenceType == RecurrenceTypeV3.DAYS_OF_WEEK) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            val isSelected = uiState.recurrenceDays.contains(day)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.toggleDay(day) },
                                label = { Text(day.name.take(3), fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }

            Text("TIME_WINDOWS", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            
            var newWindow by remember { mutableStateOf("") }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newWindow,
                    onValueChange = { newWindow = it },
                    label = { Text("Add Window (e.g. 08:00, wake)", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                )
                IconButton(onClick = { 
                    if (newWindow.isNotBlank()) {
                        viewModel.addTimeWindow(newWindow)
                        newWindow = ""
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = NeonCyan)
                }
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.timeWindows.forEach { window ->
                    AssistChip(
                        onClick = { viewModel.removeTimeWindow(window) },
                        label = { Text(window, fontSize = 10.sp) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }
    }
}

@Composable
fun MentorConversationalBuilder(uiState: AscensionForgeUiState, viewModel: AscensionForgeViewModel) {
    CyberFrame(label = "NEURAL_MENTOR // REFINEMENT_SYNAPSE", accentColor = NeonCyan) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "CYBR-TES is analyzing your protocol for optimal mission structure.",
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            // Chat History Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, Color.White.copy(alpha = 0.05f))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (uiState.chatHistory.isEmpty()) {
                        Text(
                            "Waiting for neural handshake...",
                            color = Color.DarkGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        uiState.chatHistory.forEach { msg ->
                            val label = if (msg.isFromUser) "OPERATOR" else "CYBR-TES"
                            val color = if (msg.isFromUser) NeonPink else NeonCyan
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    label,
                                    color = color.copy(alpha = 0.6f),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    msg.text,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 14.sp
                                )

                                if (msg.proposedMissions.isNotEmpty() || msg.proposedMetrics.isNotEmpty()) {
                                    ProposalTreeView(
                                        missions = msg.proposedMissions,
                                        metrics = msg.proposedMetrics,
                                        expandedMissions = uiState.expandedMissions,
                                        onToggleExpansion = viewModel::toggleMissionExpansion,
                                        onAccept = { viewModel.acceptProposals(msg.proposedMissions, msg.proposedMetrics) }
                                    )
                                }
                            }
                        }
                    }
                    
                    if (uiState.isGenerating) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(10.dp), color = NeonCyan, strokeWidth = 1.dp)
                            Text("DECRYPTING...", color = NeonCyan.copy(alpha = 0.5f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            // Chat Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = uiState.mentorInput,
                    onValueChange = { viewModel.updateMentorInput(it) },
                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                    modifier = Modifier.weight(1f),
                    cursorBrush = SolidColor(NeonCyan),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { viewModel.sendMentorMessage() }),
                    decorationBox = { innerTextField ->
                        if (uiState.mentorInput.isEmpty()) {
                            Text("Refine with AI...", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        innerTextField()
                    }
                )
                IconButton(onClick = { viewModel.sendMentorMessage() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = NeonCyan, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun ProposalTreeView(
    missions: List<ProposedMission>,
    metrics: List<SuccessMetric>,
    expandedMissions: Set<String>,
    onToggleExpansion: (String) -> Unit,
    onAccept: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "PROPOSED_STRUCTURE",
            color = NeonCyan,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        if (metrics.isNotEmpty()) {
            Text("SUCCESS_METRICS", color = NeonPink, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
            metrics.forEach { metric ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Analytics, null, tint = NeonPink, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${metric.description}: ${metric.targetValue} ${metric.unit ?: ""}", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.1f))
        }

        missions.forEach { mission ->
            val isExpanded = expandedMissions.contains(mission.title)
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleExpansion(mission.title) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        mission.title,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isExpanded) {
                    Column(
                        modifier = Modifier.padding(start = 24.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            mission.description,
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 12.sp
                        )
                        mission.tasks.forEach { task ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("•", color = NeonCyan, modifier = Modifier.padding(end = 8.dp))
                                Column {
                                    Text(
                                        task.title,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    val recurrence = task.recurrence
                                    if (recurrence != null) {
                                        Text(
                                            "Pulse Pattern: ${recurrence.type}",
                                            color = NeonCyan.copy(alpha = 0.7f),
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
            shape = RoundedCornerShape(2.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text("ACCEPT_ALL_PARAMETERS", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun RecurrenceChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (isSelected) NeonCyan else Color.Transparent)
            .border(1.dp, NeonCyan)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (isSelected) Color.Black else NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}
