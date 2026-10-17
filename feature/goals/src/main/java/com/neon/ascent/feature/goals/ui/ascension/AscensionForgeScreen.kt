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
import java.time.LocalDate

import androidx.compose.material.icons.automirrored.filled.LibraryBooks

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
    onNavigateToDashboard: () -> Unit = {},
    onBrowseProtocols: () -> Unit = {},
    onNavigateToDirectiveDetail: (String) -> Unit = {},
    viewModel: AscensionForgeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (prefilledAttribute != null || prefilledTitle != null || prefilledDescription != null || prefilledVision != null || prefilledBiometrics != null) {
            viewModel.prefill(prefilledAttribute, prefilledTitle, prefilledDescription, prefilledVision, prefilledBiometrics)
        }
    }

    LaunchedEffect(uiState.currentIntakeMode, uiState.forgeType) {
        if (uiState.currentIntakeMode == DirectiveIntakeMode.MENTOR && uiState.forgeType == ForgeType.DIRECTIVE) {
            viewModel.ensureMentorConversationStarted()
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
                    IconButton(onClick = onBrowseProtocols) {
                        Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = "Protocols", tint = NeonCyan)
                    }
                    IconButton(onClick = onNavigateToGuide) {
                        Icon(Icons.Default.Psychology, contentDescription = "Neon Guide", tint = NeonCyan)
                    }
                    IconButton(onClick = { 
                        viewModel.save()
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
            // When creating a new Directive, mode switch is primary and elevated directly under type row
            val hasCreatedDirectiveInSession = uiState.deployedDirectiveId != null
            val isDirectiveCreation = uiState.forgeType == ForgeType.DIRECTIVE
            val areOtherTabsSecondary = isDirectiveCreation && !hasCreatedDirectiveInSession

            // Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ForgeTypeTab(
                    label = "DIRECTIVE",
                    isSelected = uiState.forgeType == ForgeType.DIRECTIVE,
                    modifier = Modifier.weight(1.2f),
                    isSecondary = false
                ) {
                    viewModel.updateType(ForgeType.DIRECTIVE)
                }
                ForgeTypeTab(
                    label = "MISSION",
                    isSelected = uiState.forgeType == ForgeType.MISSION,
                    modifier = Modifier.weight(if (areOtherTabsSecondary) 0.9f else 1f),
                    isSecondary = areOtherTabsSecondary
                ) {
                    viewModel.updateType(ForgeType.MISSION)
                }
                ForgeTypeTab(
                    label = "PULSE",
                    isSelected = uiState.forgeType == ForgeType.TASK,
                    modifier = Modifier.weight(if (areOtherTabsSecondary) 0.9f else 1f),
                    isSecondary = areOtherTabsSecondary
                ) {
                    viewModel.updateType(ForgeType.TASK)
                }
            }

            // PRIMARY MODE SWITCH (Mentor / Form) when creating a Directive
            if (uiState.forgeType == ForgeType.DIRECTIVE) {
                Surface(
                    color = NeonCyan.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "INTAKE_MODE // ${uiState.currentIntakeMode.name}",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (uiState.currentIntakeMode == DirectiveIntakeMode.MENTOR) "Conversational Socrates AI" else "Manual Fast-Path Form",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Button(
                            onClick = { viewModel.toggleInSessionIntakeMode() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan.copy(alpha = 0.25f),
                                contentColor = NeonCyan
                            ),
                            border = BorderStroke(1.dp, NeonCyan),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (uiState.currentIntakeMode == DirectiveIntakeMode.MENTOR) "SWITCH TO FORM" else "SWITCH TO MENTOR",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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

            // Common Title/Description for Mission/Task or Mentor Mode
            if (uiState.forgeType != ForgeType.DIRECTIVE || uiState.currentIntakeMode == DirectiveIntakeMode.MENTOR) {
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
            }

            if (uiState.forgeType == ForgeType.DIRECTIVE) {
                if (uiState.currentIntakeMode == DirectiveIntakeMode.MENTOR) {
                    MentorConversationalBuilder(uiState, viewModel)
                } else {
                    DirectiveSpecificFields(uiState, viewModel)
                }
            } else if (uiState.forgeType == ForgeType.MISSION) {
                MissionSpecificFields(uiState, viewModel)
            } else {
                TaskSpecificFields(uiState, viewModel)
            }

            uiState.errorMessage?.let { error ->
                Surface(
                    color = Color.Red.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = error,
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.clearErrorMessage() },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss error", tint = Color.Red)
                        }
                    }
                }
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

        uiState.dopamineEvent?.let { event ->
            CelebrationOverlay(
                event = event,
                onFinished = { 
                    viewModel.clearDopamineEvent()
                    if (uiState.isSuccess) {
                        val deployedId = uiState.deployedDirectiveId
                        if (deployedId != null) {
                            onNavigateToDirectiveDetail(deployedId)
                        } else {
                            onBack()
                        }
                    }
                },
                onActionClick = {
                    viewModel.clearDopamineEvent()
                    onNavigateToDashboard()
                }
            )
        }
    }
}

@Composable
fun ForgeTypeTab(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isSecondary: Boolean = false,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected -> NeonCyan
        isSecondary -> Color.Gray.copy(alpha = 0.15f)
        else -> Color.Gray.copy(alpha = 0.3f)
    }
    val textColor = when {
        isSelected -> NeonCyan
        isSecondary -> Color.Gray.copy(alpha = 0.45f)
        else -> Color.Gray
    }
    val bgColor = if (isSelected) NeonCyan.copy(alpha = 0.1f) else Color.Transparent

    Box(
        modifier = modifier
            .height(if (isSecondary && !isSelected) 40.dp else 48.dp)
            .background(bgColor)
            .border(1.dp, borderColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            fontSize = if (isSecondary && !isSelected) 10.5.sp else 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionSpecificFields(uiState: AscensionForgeUiState, viewModel: AscensionForgeViewModel) {
    CyberFrame(label = "MISSION_PARAMETERS", accentColor = NeonCyan) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("PARENT_DIRECTIVE_SELECTION", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            
            if (uiState.availableDirectives.isEmpty()) {
                Text(
                    text = "No active directives. Mission will be standalone.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isNoneSelected = uiState.parentDirectiveId == null
                    FilterChip(
                        selected = isNoneSelected,
                        onClick = { viewModel.updateParentDirective(null) },
                        label = { Text("NONE (STANDALONE)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.3f),
                            selectedLabelColor = NeonCyan
                        )
                    )
                    uiState.availableDirectives.forEach { directive ->
                        val isSelected = uiState.parentDirectiveId == directive.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateParentDirective(directive.id) },
                            label = { Text(directive.title, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.3f),
                                selectedLabelColor = NeonCyan
                            )
                        )
                    }
                }
            }

            AnchorToSelfMapSection(uiState, viewModel)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DirectiveSpecificFields(uiState: AscensionForgeUiState, viewModel: AscensionForgeViewModel) {
    var showDatePicker by remember { mutableStateOf(false) }

    CyberFrame(label = "DIRECTIVE_FORM_INTAKE", accentColor = NeonPink) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // 1. Title
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::updateTitle,
                label = { Text("1. TITLE / IDENTIFIER", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace)
            )

            // 2. Description / Objective
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("2. DESCRIPTION / OBJECTIVE_DATA", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace)
            )

            // 3. Target Date (DatePicker)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.targetEndDate?.toString() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("3. TARGET_DATE (OPTIONAL)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Select Target Date", tint = NeonCyan)
                        }
                    },
                    textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace),
                    placeholder = { Text("SELECT_DATE...", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDatePicker = true }
                )
            }

            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = uiState.targetEndDate
                        ?.atStartOfDay(java.time.ZoneOffset.UTC)
                        ?.toInstant()
                        ?.toEpochMilli()
                        ?: System.currentTimeMillis()
                )
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selectedDate = java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneId.of("UTC"))
                                    .toLocalDate()
                                viewModel.updateTargetEndDate(selectedDate)
                            }
                            showDatePicker = false
                        }) {
                            Text("SELECT", color = NeonCyan, fontFamily = FontFamily.Monospace)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            viewModel.updateTargetEndDate(null)
                            showDatePicker = false
                        }) {
                            Text("CLEAR", color = Color.Gray, fontFamily = FontFamily.Monospace)
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            // 4. Priority Tier Chips (A / B / C, default B)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("4. PRIORITY_TIER", color = NeonPink, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PriorityTier.entries.forEach { tier ->
                        val isSelected = uiState.priorityTier == tier
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updatePriorityTier(tier) },
                            label = { Text("TIER ${tier.name}", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPink.copy(alpha = 0.3f),
                                selectedLabelColor = NeonPink
                            )
                        )
                    }
                }
            }

            // 5. Outcome Statement
            OutlinedTextField(
                value = uiState.outcomeStatement,
                onValueChange = viewModel::updateOutcomeStatement,
                label = { Text("5. QUANTIFIED_OUTCOME (OPTIONAL)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace)
            )

            // 6. Affirmation
            OutlinedTextField(
                value = uiState.affirmation,
                onValueChange = viewModel::updateAffirmation,
                label = { Text("6. DAILY_AFFIRMATION (OPTIONAL)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace)
            )

            // Ask Mentor Assist Button
            OutlinedButton(
                onClick = { viewModel.requestMentorFormSuggestions() },
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isGeneratingSuggestions) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = NeonCyan, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("SYNTHESIZING_SUGGESTIONS...", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                } else {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ASK MENTOR // SUGGEST OUTCOME & AFFIRMATION", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // 7. Visual Pick / URI
            OutlinedTextField(
                value = uiState.visualUri,
                onValueChange = viewModel::updateVisualUri,
                label = { Text("7. VISUAL_URI / IMAGE_REF (OPTIONAL)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace)
            )

            // 8. Self Map Anchor Picker
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("8. SELF_MAP_ANCHOR", color = NeonPink, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                AnchorToSelfMapSection(uiState, viewModel)
            }

            // 9. Linked Attributes (S.P.E.C.I.A.L. Chips)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("9. LINKED_S.P.E.C.I.A.L_NODES", color = NeonPink, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AnchorToSelfMapSection(
    uiState: AscensionForgeUiState,
    viewModel: AscensionForgeViewModel
) {
    if (uiState.selfMapItems.isNotEmpty()) {
        Spacer(modifier = Modifier.height(12.dp))
        CyberFrame(label = "ANCHOR TO SELF MAP ITEM (OPTIONAL)", accentColor = Color(0xFF00FF9F)) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Anchor this protocol to a core Self Map attachment to ground motivation.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val isNoneSelected = uiState.selectedAnchoredSelfItemId == null
                    FilterChip(
                        selected = isNoneSelected,
                        onClick = { viewModel.selectAnchoredSelfItem(null) },
                        label = { Text("NONE", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.DarkGray, selectedLabelColor = Color.White)
                    )

                    uiState.selfMapItems.take(6).forEach { item ->
                        val isSelected = uiState.selectedAnchoredSelfItemId == item.id
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) viewModel.selectAnchoredSelfItem(null)
                                else viewModel.selectAnchoredSelfItem(item)
                            },
                            label = {
                                Text(
                                    text = "⚓ ${item.name} (${(item.importance * 100).toInt()}%)",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00FF9F),
                                selectedLabelColor = Color.Black
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
            if (uiState.availableDirectives.isNotEmpty()) {
                Text("PARENT_DIRECTIVE_SELECTION", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isNoneSelected = uiState.parentDirectiveId == null
                    FilterChip(
                        selected = isNoneSelected,
                        onClick = { viewModel.updateParentDirective(null) },
                        label = { Text("NONE", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.3f),
                            selectedLabelColor = NeonCyan
                        )
                    )
                    uiState.availableDirectives.forEach { directive ->
                        val isSelected = uiState.parentDirectiveId == directive.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateParentDirective(directive.id) },
                            label = { Text(directive.title, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.3f),
                                selectedLabelColor = NeonCyan
                            )
                        )
                    }
                }
            }

            // Parent Mission Selection
            val relevantMissions = if (uiState.parentDirectiveId != null) {
                uiState.availableMissions.filter { it.directiveId == uiState.parentDirectiveId }
            } else {
                uiState.availableMissions
            }

            Text("PARENT_MISSION_SELECTION (PREFERRED)", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            if (relevantMissions.isEmpty()) {
                Text(
                    text = if (uiState.parentDirectiveId != null) "No active missions under selected directive." else "No active missions found.",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isNoneSelected = uiState.parentMissionId == null
                    FilterChip(
                        selected = isNoneSelected,
                        onClick = { viewModel.updateParentMission(null) },
                        label = { Text("NONE", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonCyan.copy(alpha = 0.3f),
                            selectedLabelColor = NeonCyan
                        )
                    )
                    relevantMissions.forEach { mission ->
                        val isSelected = uiState.parentMissionId == mission.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateParentMission(mission.id) },
                            label = { Text(mission.title, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan.copy(alpha = 0.3f),
                                selectedLabelColor = NeonCyan
                            )
                        )
                    }
                }
            }

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
                                        onAccept = { viewModel.acceptProposals(msg.proposedMissions, msg.proposedMetrics) },
                                        onRegenerate = { viewModel.regenerateProposals() },
                                        onClarify = { viewModel.askClarification() }
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
    onAccept: () -> Unit,
    onRegenerate: () -> Unit,
    onClarify: () -> Unit
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRegenerate,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = NeonCyan),
                shape = RoundedCornerShape(2.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("REGENERATE", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            Button(
                onClick = onClarify,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = NeonPink),
                shape = RoundedCornerShape(2.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("CLARIFY", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Button(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth(),
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
