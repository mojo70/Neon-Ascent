package com.neon.ascent.feature.goals.ui.ascension

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink
import com.neon.ascent.core.common.neonBorder
import com.neon.ascent.core.domain.goals.models.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

enum class TaskSortOption { DUE_SOON, STREAK, XP_VALUE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AscensionMissionDetailScreen(
    missionId: String,
    onBack: () -> Unit,
    onTaskClick: (String) -> Unit = {},
    onDirectiveClick: (String) -> Unit = {},
    viewModel: AscensionMissionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeDirectives by viewModel.activeDirectives.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Dialog & UI states
    var showQuickTaskBottomSheet by remember { mutableStateOf(false) }
    var showEditMissionDialog by remember { mutableStateOf(false) }
    var showTransferDirectiveDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    var showSuccessCriteriaEditor by remember { mutableStateOf(false) }
    
    // Dropdowns
    var showOverflowMenu by remember { mutableStateOf(false) }
    
    // Sort Options
    var currentSortOption by remember { mutableStateOf(TaskSortOption.DUE_SOON) }
    
    // Form and edits
    var editNotesText by remember { mutableStateOf("") }
    var missionTitleInput by remember { mutableStateOf("") }
    var missionDescInput by remember { mutableStateOf("") }
    var missionObjectiveInput by remember { mutableStateOf("") }
    var missionCriteriaInput by remember { mutableStateOf("") }
    var missionTargetDateInput by remember { mutableStateOf("") }

    LaunchedEffect(missionId) {
        viewModel.loadMission(missionId)
    }

    LaunchedEffect(uiState.mission) {
        uiState.mission?.let { mission ->
            editNotesText = mission.notes ?: ""
            missionTitleInput = mission.title
            missionDescInput = mission.description
            missionObjectiveInput = mission.objective ?: ""
            missionCriteriaInput = mission.successCriteria ?: ""
            missionTargetDateInput = mission.targetEndDate?.toString() ?: ""
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (uiState.mission?.title ?: "MISSION_LOG").uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = NeonCyan
                        )
                        uiState.parentDirective?.let { parent ->
                            Text(
                                text = "DIRECTIVE: ${parent.title.uppercase()}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = NeonPink,
                                modifier = Modifier
                                    .clickable { onDirectiveClick(parent.id) }
                                    .padding(top = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = !showOverflowMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = NeonCyan)
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF070B12))
                                .border(1.dp, NeonCyan.copy(alpha = 0.5f))
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = NeonCyan) },
                                text = { Text("EDIT_MISSION", fontFamily = FontFamily.Monospace, color = Color.White) },
                                onClick = {
                                    showOverflowMenu = false
                                    showEditMissionDialog = true
                                }
                            )
                            val isPaused = uiState.mission?.status == AscensionMissionStatus.PAUSED
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = null,
                                        tint = if (isPaused) Color.Green else Color.Yellow
                                    )
                                },
                                text = {
                                    Text(
                                        text = if (isPaused) "RESUME_MISSION" else "PAUSE_MISSION",
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.updateStatus(
                                        if (isPaused) AscensionMissionStatus.ACTIVE else AscensionMissionStatus.PAUSED
                                    )
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = NeonCyan) },
                                text = { Text("MOVE_DIRECTIVE", fontFamily = FontFamily.Monospace, color = Color.White) },
                                onClick = {
                                    showOverflowMenu = false
                                    showTransferDirectiveDialog = true
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = NeonCyan) },
                                text = { Text("DUPLICATE", fontFamily = FontFamily.Monospace, color = Color.White) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.duplicateMission()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, tint = NeonPink) },
                                text = { Text("ARCHIVE", fontFamily = FontFamily.Monospace, color = NeonPink) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.updateStatus(AscensionMissionStatus.ARCHIVED)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onBack()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF010408),
                    titleContentColor = NeonCyan
                )
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Secondary AI expansion floating button
                FloatingActionButton(
                    onClick = {
                        viewModel.expandWithAi()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    containerColor = Color.Black,
                    contentColor = NeonCyan,
                    modifier = Modifier.neonBorder(color = NeonCyan, width = 1.dp, cornerRadius = 16.dp)
                ) {
                    if (uiState.isGeneratingTasks) {
                        CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Expand with AI")
                    }
                }

                // Primary Quick Task creation floating button
                FloatingActionButton(
                    onClick = { showQuickTaskBottomSheet = true },
                    containerColor = NeonPink,
                    contentColor = Color.Black,
                    modifier = Modifier.neonBorder(color = NeonPink, width = 2.dp, cornerRadius = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task")
                }
            }
        },
        containerColor = Color(0xFF010408)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else {
                val mission = uiState.mission
                if (mission != null) {
                    // Logic to split today's vs other tasks
                    val today = LocalDate.now()
                    val isWeekday = today.dayOfWeek != java.time.DayOfWeek.SATURDAY && today.dayOfWeek != java.time.DayOfWeek.SUNDAY
                    
                    val todaysTasks = uiState.tasks.filter { task ->
                        val isCompletedToday = task.lastCompleted?.atZone(ZoneId.systemDefault())?.toLocalDate() == today
                        if (isCompletedToday) {
                            true
                        } else {
                            when (task.type) {
                                AscensionTaskType.ONE_TIME -> task.lastCompleted == null
                                AscensionTaskType.RECURRING -> {
                                    val recurrence = task.recurrence
                                    if (recurrence == null) {
                                        true
                                    } else {
                                        when (recurrence.type) {
                                            RecurrenceTypeV3.DAILY -> true
                                            RecurrenceTypeV3.WEEKDAYS -> isWeekday
                                            RecurrenceTypeV3.DAYS_OF_WEEK -> recurrence.daysOfWeek.contains(today.dayOfWeek)
                                            else -> true
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val allActiveTasks = uiState.tasks.filter { it.lastCompleted == null }
                    
                    // Sorting active tasks
                    val sortedActiveTasks = when (currentSortOption) {
                        TaskSortOption.DUE_SOON -> allActiveTasks.sortedBy { it.id } // Default sequence
                        TaskSortOption.STREAK -> allActiveTasks.sortedByDescending { it.currentStreak }
                        TaskSortOption.XP_VALUE -> allActiveTasks.sortedByDescending { it.xpValue }
                    }

                    // Parse success criteria outcomes
                    val criteriaList = mission.successCriteria
                        ?.split("\n")
                        ?.map { it.trim().removePrefix("-").removePrefix("*").trim() }
                        ?.filter { it.isNotBlank() }
                        ?: listOf(
                            "Execute 100% of sub-linked task arrays",
                            "Maintain stable neural sync with primary directive"
                        )

                    // Dates and streaks calculations
                    val daysActive = ChronoUnit.DAYS.between(mission.startDate, LocalDate.now()) + 1
                    val totalDays = mission.targetEndDate?.let { ChronoUnit.DAYS.between(mission.startDate, it) } ?: 30
                    val maxStreak = uiState.tasks.maxOfOrNull { it.currentStreak } ?: 0

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. HERO SECTION (Campaign Center Hub)
                        item {
                            CyberFrame(
                                label = "HERO_COMMAND_SECTOR",
                                accentColor = NeonCyan
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "OBJECTIVE",
                                                color = Color.Gray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = mission.objective ?: "Establish standard tactical link parameter.",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        // Large Neon Progress Circle
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(60.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                progress = { mission.progress },
                                                modifier = Modifier.fillMaxSize(),
                                                color = NeonCyan,
                                                trackColor = Color.White.copy(alpha = 0.05f),
                                                strokeWidth = 4.dp
                                            )
                                            Text(
                                                text = "${(mission.progress * 100).toInt()}%",
                                                color = NeonCyan,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    // Time metrics
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "CAMPAIGN_DURATION",
                                                color = Color.Gray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Day $daysActive of $totalDays",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            mission.targetEndDate?.let {
                                                Text(
                                                    text = "Target limit: $it",
                                                    color = Color.Gray,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "STREAK_GRID",
                                                color = Color.Gray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "$maxStreak DAYS 🔥",
                                                    color = Color.Yellow,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(NeonCyan, RoundedCornerShape(2.dp))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. SUCCESS CRITERIA MATRIX
                        item {
                            CyberFrame(label = "SUCCESS_CRITERIA_MATRIX", accentColor = NeonPink) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "MEASURABLE_OUTCOMES",
                                            color = NeonPink,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = { showSuccessCriteriaEditor = true }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Criteria",
                                                tint = NeonPink,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    criteriaList.forEach { criterion ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = NeonPink.copy(alpha = 0.8f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = criterion,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. TODAY'S TASKS / PROTOCOLS
                        item {
                            val xpAvailable = todaysTasks.sumOf { it.xpValue }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "TODAY'S PROTOCOLS",
                                    color = NeonCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    modifier = Modifier
                                        .background(NeonCyan.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                        .border(1.dp, NeonCyan, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$xpAvailable XP",
                                        color = NeonCyan,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (todaysTasks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.02f))
                                        .border(1.dp, Color.White.copy(alpha = 0.05f))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "NO_PROTOCOLS_DUE_TODAY",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.expandWithAi()
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            },
                                            border = BorderStroke(1.dp, NeonCyan),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                                        ) {
                                            Text("GENERATE NEW PROTOCOLS", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        } else {
                            items(todaysTasks) { task ->
                                val completed = task.lastCompleted != null &&
                                        task.lastCompleted!!.atZone(ZoneId.systemDefault()).toLocalDate() == today
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF070B12))
                                        .border(1.dp, if (completed) Color.Green.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                                        .clickable { onTaskClick(task.id) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (!completed) {
                                                viewModel.completeTask(task)
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        enabled = !completed
                                    ) {
                                        Icon(
                                            imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Complete Task",
                                            tint = if (completed) Color.Green else Color.Gray
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            color = if (completed) Color.Gray else Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (task.description.isNotBlank()) {
                                            Text(
                                                text = task.description,
                                                color = Color.Gray,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "+${task.xpValue} XP",
                                            color = NeonCyan,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (task.currentStreak > 0) {
                                            Text(
                                                text = "${task.currentStreak} 🔥",
                                                color = Color.Yellow,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 4. ALL ACTIVE TASKS (Sortable List)
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "ALL_ACTIVE_CAMPAIGNS",
                                        color = NeonPink,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    
                                    // Sort chip selectors
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        TaskSortOption.entries.forEach { option ->
                                            val isSelected = currentSortOption == option
                                            Box(
                                                modifier = Modifier
                                                    .background(if (isSelected) NeonPink.copy(alpha = 0.2f) else Color.Transparent)
                                                    .border(1.dp, if (isSelected) NeonPink else Color.Gray)
                                                    .clickable { currentSortOption = option }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = option.name.replace("_", " ").uppercase(),
                                                    color = if (isSelected) NeonPink else Color.White,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (sortedActiveTasks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "NO_ACTIVE_TASKS_AVAILABLE",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else {
                            items(sortedActiveTasks) { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF03060C))
                                        .border(1.dp, Color.White.copy(alpha = 0.02f))
                                        .clickable { onTaskClick(task.id) }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(NeonPink)
                                        )
                                        Column {
                                            Text(
                                                text = task.title,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "GRACE_BUFFER: ${task.graceBufferDays} DAYS",
                                                color = Color.Gray,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${task.xpValue} XP",
                                            color = NeonCyan,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        // 5. PROGRESS & HISTORY (HEATMAP + TERMINAL COMPLETIONS LOG)
                        item {
                            CyberFrame(label = "SYNC_HEATMAP_GRID", accentColor = NeonCyan) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "Biometric completion and grid alignment history (past 14 days)",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val dates = (0..13).map { today.minusDays(it.toLong()) }.reversed()
                                        dates.forEach { date ->
                                            val isCompleted = uiState.tasks.any { task ->
                                                task.lastCompleted?.atZone(ZoneId.systemDefault())?.toLocalDate() == date
                                            }
                                            val color = if (isCompleted) NeonCyan else Color.White.copy(alpha = 0.05f)
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .background(color, RoundedCornerShape(2.dp))
                                                    .border(1.dp, if (isCompleted) NeonCyan else Color.Transparent)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                                    Text(
                                        "SYNC_LOG_STREAM",
                                        color = NeonCyan,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    // Simulated Terminal Completions Stream
                                    val completedList = uiState.tasks.filter { it.lastCompleted != null }
                                    if (completedList.isEmpty()) {
                                        Text(
                                            text = "> NO_COMPLETIONS_LOGGED_IN_SECTOR",
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    } else {
                                        completedList.take(3).forEach { t ->
                                            val instantText = t.lastCompleted?.atZone(ZoneId.systemDefault())
                                                ?.format(DateTimeFormatter.ofPattern("HH:mm:ss")) ?: "00:00:00"
                                            Text(
                                                text = "> [$instantText] SYNC_SUCCESS // ${t.title.uppercase()} (+${t.xpValue} XP)",
                                                color = Color.Green.copy(alpha = 0.8f),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 6. NEON MENTOR ADVICE SEC
                        item {
                            CyberFrame(label = "NEON_MENTOR_LINK", accentColor = NeonPink) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CYBR-TES MODE: ${uiState.selectedMentorMode.name}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(NeonPink.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
                                                .border(1.dp, NeonPink, RoundedCornerShape(2.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "ACTIVE",
                                                color = NeonPink,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    Text(
                                        text = "\"Dialectic command link initialized. Access AI Socrates to perform structured expansion or question your progress metrics.\"",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontStyle = FontStyle.Italic,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Button(
                                        onClick = { showChatDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("OPEN NEON MENTOR LINK", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }

                        // 7. NOTES & JOURNAL
                        item {
                            CyberFrame(label = "REFLECTIVE_MEMORIES", accentColor = NeonCyan) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = if (mission.notes.isNullOrBlank()) "No reflection array logged." else mission.notes!!,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Button(
                                        onClick = { showNotesDialog = true },
                                        modifier = Modifier.align(Alignment.End),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        border = BorderStroke(1.dp, NeonCyan)
                                    ) {
                                        Text("EDIT_REFLECTIONS", color = NeonCyan, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }

    // --- MODAL DIALOGS ---

    // 1. Quick Task Bottom Sheet Integration
    if (showQuickTaskBottomSheet) {
        QuickTaskBottomSheet(
            onDismiss = { showQuickTaskBottomSheet = false },
            prefilledParentId = missionId
        )
    }

    // 2. Edit Mission Dialog
    if (showEditMissionDialog) {
        Dialog(onDismissRequest = { showEditMissionDialog = false }) {
            CyberFrame(label = "EDIT_MISSION_PARAMETERS", accentColor = NeonCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = missionTitleInput,
                        onValueChange = { missionTitleInput = it },
                        label = { Text("MISSION_TITLE", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                    )
                    OutlinedTextField(
                        value = missionDescInput,
                        onValueChange = { missionDescInput = it },
                        label = { Text("MISSION_DESCRIPTION", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                    )
                    OutlinedTextField(
                        value = missionObjectiveInput,
                        onValueChange = { missionObjectiveInput = it },
                        label = { Text("STRATEGIC_OBJECTIVE", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                    )
                    OutlinedTextField(
                        value = missionTargetDateInput,
                        onValueChange = { missionTargetDateInput = it },
                        label = { Text("TARGET_DATE (YYYY-MM-DD)", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { showEditMissionDialog = false }) {
                            Text("CANCEL", fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                val date = try {
                                    LocalDate.parse(missionTargetDateInput.trim())
                                } catch (e: Exception) {
                                    null
                                }
                                viewModel.updateMissionDetails(
                                    title = missionTitleInput,
                                    description = missionDescInput,
                                    objective = missionObjectiveInput.ifBlank { null },
                                    successCriteria = missionCriteriaInput.ifBlank { null },
                                    targetEndDate = date
                                )
                                showEditMissionDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                        ) {
                            Text("UPDATE", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    // 3. Edit Success Criteria Dialog
    if (showSuccessCriteriaEditor) {
        Dialog(onDismissRequest = { showSuccessCriteriaEditor = false }) {
            CyberFrame(label = "EDIT_SUCCESS_CRITERIA", accentColor = NeonPink) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Define measurable outcomes (one per line):",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedTextField(
                        value = missionCriteriaInput,
                        onValueChange = { missionCriteriaInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { showSuccessCriteriaEditor = false }) {
                            Text("CANCEL", fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                viewModel.updateMissionDetails(
                                    title = missionTitleInput,
                                    description = missionDescInput,
                                    objective = missionObjectiveInput.ifBlank { null },
                                    successCriteria = missionCriteriaInput.ifBlank { null },
                                    targetEndDate = try { LocalDate.parse(missionTargetDateInput.trim()) } catch(e: Exception) { null }
                                )
                                showSuccessCriteriaEditor = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black)
                        ) {
                            Text("SAVE", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    // 4. Move Mission to Directive Dialog
    if (showTransferDirectiveDialog) {
        Dialog(onDismissRequest = { showTransferDirectiveDialog = false }) {
            CyberFrame(label = "TRANSFER_DIRECTIVE_PATH", accentColor = NeonCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "SELECT TARGET DIRECTIVE LINK:",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Column(
                        modifier = Modifier
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        activeDirectives.forEach { directive ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.moveMissionToDirective(directive.id)
                                        showTransferDirectiveDialog = false
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = directive.title.uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }

                    OutlinedButton(
                        onClick = { showTransferDirectiveDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("CANCEL", fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }

    // 5. Edit reflections/Notes
    if (showNotesDialog) {
        Dialog(onDismissRequest = { showNotesDialog = false }) {
            CyberFrame(label = "EDIT_REFLECTIVE_MEMORIES", accentColor = NeonCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = editNotesText,
                        onValueChange = { editNotesText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { showNotesDialog = false }) {
                            Text("CANCEL", fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                viewModel.updateNotes(editNotesText)
                                showNotesDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                        ) {
                            Text("SAVE_CHANGES", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    // 6. Dialogue AI Mentor Box Dialog
    if (showChatDialog) {
        var chatInput by remember { mutableStateOf("") }
        
        Dialog(onDismissRequest = { showChatDialog = false }) {
            CyberFrame(
                label = "CYBR-TES // ADVISORY_MATRIX",
                accentColor = NeonPink,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "MODE:",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        MentorMode.entries.forEach { mode ->
                            val isSelected = uiState.selectedMentorMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) NeonPink.copy(alpha = 0.15f) else Color.Transparent)
                                    .border(1.dp, if (isSelected) NeonPink else Color.Gray.copy(alpha = 0.3f), shape = RoundedCornerShape(2.dp))
                                    .clickable { viewModel.updateMentorMode(mode) }
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode.name,
                                    color = if (isSelected) NeonPink else Color.Gray,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.3f))
                            .border(1.dp, Color.White.copy(alpha = 0.05f))
                            .padding(8.dp)
                    ) {
                        if (uiState.chatHistory.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "INITIATE ADVISORY FEEDBACK.\nSELECT COMMAND LINK AND TRANSMIT QUERY.",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            val scrollState = rememberScrollState()
                            LaunchedEffect(uiState.chatHistory.size) {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.chatHistory.forEach { msg ->
                                    val bubbleBg = if (msg.isUser) NeonPink.copy(alpha = 0.08f) else NeonCyan.copy(alpha = 0.08f)
                                    val bubbleBorder = if (msg.isUser) NeonPink.copy(alpha = 0.3f) else NeonCyan.copy(alpha = 0.3f)
                                    val align = if (msg.isUser) Alignment.End else Alignment.Start
                                    
                                    Column(modifier = Modifier.align(align).widthIn(max = 240.dp)) {
                                        Text(
                                            text = if (msg.isUser) "OPERATOR //" else "CYBR-TES //",
                                            color = if (msg.isUser) NeonPink else NeonCyan,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(bubbleBg, shape = RoundedCornerShape(4.dp))
                                                .border(1.dp, bubbleBorder, shape = RoundedCornerShape(4.dp))
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = msg.text,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Query AI advisor...", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPink,
                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                            ),
                            maxLines = 2
                        )
                        
                        Button(
                            onClick = {
                                if (chatInput.isNotBlank() && !uiState.isAskingMentor) {
                                    viewModel.askMentor(chatInput)
                                    chatInput = ""
                                }
                            },
                            enabled = chatInput.isNotBlank() && !uiState.isAskingMentor,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                            shape = RoundedCornerShape(4.dp),
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.size(48.dp)
                        ) {
                            if (uiState.isAskingMentor) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
