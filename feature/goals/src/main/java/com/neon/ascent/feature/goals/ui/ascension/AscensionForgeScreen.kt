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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.core.domain.model.SpecialType
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
    viewModel: AscensionForgeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                ForgeTypeTab("TASK", uiState.forgeType == ForgeType.TASK, Modifier.weight(1f)) {
                    viewModel.updateType(ForgeType.TASK)
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
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                    OutlinedTextField(
                        value = uiState.description,
                        onValueChange = viewModel::updateDescription,
                        label = { Text("OBJECTIVE_DATA", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }

            if (uiState.forgeType == ForgeType.DIRECTIVE) {
                DirectiveSpecificFields(uiState, viewModel)
            } else {
                TaskSpecificFields(uiState, viewModel)
            }

            Button(
                onClick = { 
                    viewModel.save()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("INITIATE_DEPLOYMENT", fontFamily = FontFamily.Monospace)
            }
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
    CyberFrame(label = "TASK_EXECUTION_LOGIC", accentColor = NeonCyan) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
