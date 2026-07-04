package com.neon.ascent.feature.goals.ui.ascension

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink
import com.neon.ascent.core.common.neonBorder
import com.neon.ascent.core.domain.goals.models.AscensionDirective
import com.neon.ascent.core.domain.goals.models.AscensionMission
import com.neon.ascent.core.domain.goals.models.RecurrenceTypeV3
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTaskBottomSheet(
    onDismiss: () -> Unit,
    prefilledParentId: String? = null,
    onTaskCreated: (String) -> Unit = {},
    viewModel: QuickTaskViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(true) }
    var recurrenceType by remember { mutableStateOf(RecurrenceTypeV3.DAILY) }
    var selectedTimeWindow by remember { mutableStateOf("Anytime") }
    var adaptiveWakeEnabled by remember { mutableStateOf(false) }
    var xpValue by remember { mutableFloatStateOf(10f) }
    var selectedParentId by remember { mutableStateOf<String?>(prefilledParentId) }

    // Advanced Section
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var graceBufferDays by remember { mutableFloatStateOf(2f) }
    var tagsInput by remember { mutableStateOf("") }
    var userNotesTemplate by remember { mutableStateOf("") }
    var linkedArchetype by remember { mutableStateOf("") }

    // Dropdowns
    var showParentDropdown by remember { mutableStateOf(false) }

    // UI Feedback
    val flavorSuggestion by viewModel.flavorSuggestion.collectAsState()
    val isAiGenerating by viewModel.isAiGenerating.collectAsState()
    val directives by viewModel.activeDirectives.collectAsState()
    val missions by viewModel.activeMissions.collectAsState()

    val focusRequester = remember { FocusRequester() }

    // Typing effect for visual polish on suggestions
    var typedSuggestion by remember { mutableStateOf("") }
    LaunchedEffect(flavorSuggestion) {
        typedSuggestion = ""
        flavorSuggestion.forEach { char ->
            typedSuggestion += char
            delay(15)
        }
    }

    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF020508),
        scrimColor = Color.Black.copy(alpha = 0.85f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(40.dp, 4.dp)
                    .background(NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NEW PULSE",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = NeonPink,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    )
                    Text(
                        text = "INITIALIZING_PULSE_SYNC_V3",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = NeonCyan.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NeonCyan)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MAIN TITLE FIELD
            CyberFrame(label = "CORE_IDENTIFIER", accentColor = NeonPink) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            viewModel.updateTitle(it)
                        },
                        placeholder = {
                            Text(
                                "e.g. Drink 16oz water upon waking",
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonPink,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            cursorColor = NeonPink
                        ),
                        singleLine = true
                    )

                    // Flavor Suggestion Box (NEON_NARRATOR)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = typedSuggestion,
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // DESCRIPTION (Optional)
            CyberFrame(label = "INTERFACE_DESCR", accentColor = NeonCyan) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = {
                        Text(
                            "Enter sub-system objectives...",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        cursorColor = NeonCyan
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PARENT HIERARCHY SELECTOR
            CyberFrame(label = "DIRECTIVE_AFFINITY", accentColor = NeonCyan) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    val parentName = when {
                        selectedParentId == null -> "Standalone (Neural Node)"
                        directives.any { it.id == selectedParentId } -> {
                            "Directive: " + directives.first { it.id == selectedParentId }.title
                        }
                        missions.any { it.id == selectedParentId } -> {
                            "Mission: " + missions.first { it.id == selectedParentId }.title
                        }
                        else -> "Standalone (Neural Node)"
                    }

                    OutlinedButton(
                        onClick = { showParentDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = parentName.uppercase(),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (selectedParentId == null) Color.Gray else NeonCyan
                            )
                            Icon(
                                imageVector = if (showParentDropdown) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showParentDropdown,
                        onDismissRequest = { showParentDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .background(Color(0xFF050B14))
                            .border(1.dp, NeonCyan)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "NEURAL_NODE (STANDALONE)",
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            },
                            onClick = {
                                selectedParentId = null
                                showParentDropdown = false
                            }
                        )

                        if (directives.isNotEmpty()) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text(
                                    "DIRECTIVES",
                                    color = NeonPink,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            directives.forEach { directive ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            directive.title.uppercase(),
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    },
                                    onClick = {
                                        selectedParentId = directive.id
                                        showParentDropdown = false
                                    }
                                )
                            }
                        }

                        if (missions.isNotEmpty()) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Text(
                                    "MISSIONS",
                                    color = NeonCyan,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            missions.forEach { mission ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            mission.title.uppercase(),
                                            fontFamily = FontFamily.Monospace,
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    },
                                    onClick = {
                                        selectedParentId = mission.id
                                        showParentDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // RECURRENCE & TIMING PRESETS
            CyberFrame(label = "PULSE_PATTERN", accentColor = NeonPink) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Segmented Button Mode selector (One-Time / Recurring Daily / Weekdays)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("Daily", true, RecurrenceTypeV3.DAILY),
                            Triple("Weekdays", true, RecurrenceTypeV3.WEEKDAYS),
                            Triple("One-Time", false, RecurrenceTypeV3.DAILY)
                        ).forEach { (label, recurring, type) ->
                            val isSelected = isRecurring == recurring && (!recurring || recurrenceType == type)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) NeonPink.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(
                                        1.dp,
                                        if (isSelected) NeonPink else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        isRecurring = recurring
                                        if (recurring) {
                                            recurrenceType = type
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label.uppercase(),
                                    color = if (isSelected) NeonPink else Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                    // TIME WINDOWS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "TIME_WINDOW:",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Morning", "Midday", "Evening", "Anytime").forEach { window ->
                                val isSelected = selectedTimeWindow == window
                                Box(
                                    modifier = Modifier
                                        .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                                        .border(
                                            1.dp,
                                            if (isSelected) NeonCyan else Color.White.copy(alpha = 0.1f),
                                            RoundedCornerShape(2.dp)
                                        )
                                        .clickable { selectedTimeWindow = window }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = window.uppercase(),
                                        color = if (isSelected) NeonCyan else Color.Gray,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // ADAPTIVE WAKE TOGGLE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.02f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "ADAPTIVE_WAKE",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Align schedule to neural bio-rhythms",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Switch(
                            checked = adaptiveWakeEnabled,
                            onCheckedChange = { adaptiveWakeEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Black
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // XP ALLOCATION
            CyberFrame(label = "XP_ALLOCATION // MATRIX_VALUE", accentColor = NeonCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "XP_VALUE: ${xpValue.toInt()} XP",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(5f, 10f, 15f, 25f).forEach { xp ->
                                val isSelected = xpValue == xp
                                Box(
                                    modifier = Modifier
                                        .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                        .border(1.dp, if (isSelected) NeonCyan else Color.Gray)
                                        .clickable { xpValue = xp }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "${xp.toInt()}",
                                        color = if (isSelected) NeonCyan else Color.White,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                    Slider(
                        value = xpValue,
                        onValueChange = { xpValue = it },
                        valueRange = 5f..30f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ADVANCED COLLAPSIBLE SECTION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.White.copy(alpha = 0.05f))
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = NeonPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ADVANCED_PARAMETERS",
                            color = NeonPink,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        if (isAdvancedExpanded) "CLOSE" else "OPEN",
                        color = Color.Gray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                AnimatedVisibility(
                    visible = isAdvancedExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Grace Buffer Days
                        Column {
                            Text(
                                "GRACE_BUFFER_DAYS: ${graceBufferDays.toInt()} DAYS",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Slider(
                                value = graceBufferDays,
                                onValueChange = { graceBufferDays = it },
                                valueRange = 1f..7f,
                                steps = 5,
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonPink,
                                    activeTrackColor = NeonPink,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                )
                            )
                        }

                        // Tags Input
                        OutlinedTextField(
                            value = tagsInput,
                            onValueChange = { tagsInput = it },
                            label = { Text("TAGS (COMMA SEPARATED)", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                        )

                        // Notes Template
                        OutlinedTextField(
                            value = userNotesTemplate,
                            onValueChange = { userNotesTemplate = it },
                            label = { Text("REFLECTIVE NOTES TEMPLATE", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                        )

                        // Linked Archetype (Flavor)
                        OutlinedTextField(
                            value = linkedArchetype,
                            onValueChange = { linkedArchetype = it },
                            label = { Text("LINKED ARCHETYPE TAG", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ACTIONS SECTION + FLOATING AI ACCELERATOR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                // SAVE / CLOSE BUTTONS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 64.dp), // Make space for floating AI accelerator on right
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (title.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.createTask(
                                    title = title,
                                    description = description,
                                    parentId = selectedParentId,
                                    isRecurring = isRecurring,
                                    recurrenceType = recurrenceType,
                                    timeWindows = listOf(selectedTimeWindow),
                                    adaptiveWakeEnabled = adaptiveWakeEnabled,
                                    xpValue = xpValue.toInt(),
                                    graceBufferDays = graceBufferDays.toInt(),
                                    tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                    userNotesTemplate = userNotesTemplate.ifBlank { null }
                                )
                                // Add Another - Clear basic fields
                                title = ""
                                description = ""
                            }
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, NeonPink),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPink),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "+ ANOTHER",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.createTask(
                                    title = title,
                                    description = description,
                                    parentId = selectedParentId,
                                    isRecurring = isRecurring,
                                    recurrenceType = recurrenceType,
                                    timeWindows = listOf(selectedTimeWindow),
                                    adaptiveWakeEnabled = adaptiveWakeEnabled,
                                    xpValue = xpValue.toInt(),
                                    graceBufferDays = graceBufferDays.toInt(),
                                    tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                    userNotesTemplate = userNotesTemplate.ifBlank { null }
                                )
                                scope.launch {
                                    sheetState.hide()
                                    onTaskCreated(title)
                                    onDismiss()
                                }
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp),
                        enabled = title.isNotBlank()
                    ) {
                        Text(
                            "SAVE & CLOSE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // AI ACCELERATOR BUTTON (Floating bottom-right)
                FloatingActionButton(
                    onClick = {
                        if (title.isNotBlank()) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            scope.launch {
                                val suggestion = viewModel.letNeonGuideFinishThis(title, description)
                                if (suggestion != null) {
                                    title = suggestion.title
                                    description = suggestion.description
                                    isRecurring = suggestion.type == com.neon.ascent.core.domain.goals.models.AscensionTaskType.RECURRING
                                    recurrenceType = suggestion.recurrenceType
                                    selectedTimeWindow = suggestion.timeWindow
                                    xpValue = suggestion.xpValue.toFloat()
                                    selectedParentId = suggestion.suggestedParentId ?: selectedParentId
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            }
                        }
                    },
                    containerColor = NeonCyan,
                    contentColor = Color.Black,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(48.dp)
                        .neonBorder(color = NeonCyan, width = 1.dp, cornerRadius = 24.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    if (isAiGenerating) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Let Neon Guide Finish This",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
