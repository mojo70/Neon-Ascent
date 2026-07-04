package com.neon.ascent.feature.goals.ui.ascension

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.time.temporal.ChronoUnit
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AscensionDirectiveDetailScreen(
    directiveId: String,
    onBack: () -> Unit,
    onMissionClick: (String) -> Unit = {},
    viewModel: AscensionDirectiveDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Dialog & UI triggers
    var showQuickTaskBottomSheet by remember { mutableStateOf(false) }
    var showEditDirectiveDialog by remember { mutableStateOf(false) }
    var showAddMissionDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    var showNotesDialog by remember { mutableStateOf(false) }
    
    // Dropdowns
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Form inputs for editing
    var titleInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var visionInput by remember { mutableStateOf("") }
    var isQuarterlyInput by remember { mutableStateOf(false) }
    var archetypeTagInput by remember { mutableStateOf("") }
    var targetDateInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    LaunchedEffect(directiveId) {
        viewModel.loadDirective(directiveId)
    }

    LaunchedEffect(uiState.directive) {
        uiState.directive?.let { dir ->
            titleInput = dir.title
            descInput = dir.description
            visionInput = dir.visionStatement ?: ""
            isQuarterlyInput = dir.isQuarterly
            archetypeTagInput = dir.archetypeTag ?: ""
            targetDateInput = dir.targetEndDate?.toString() ?: ""
            notesInput = dir.notes ?: ""
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = (uiState.directive?.title ?: "DIRECTIVE_GRID").uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeonPink,
                        style = MaterialTheme.typography.titleLarge.copy(
                            shadow = Shadow(
                                color = NeonPink.copy(alpha = 0.6f),
                                offset = Offset(0f, 0f),
                                blurRadius = 12f
                            )
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonPink)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = !showOverflowMenu }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = NeonPink)
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF090306))
                                .border(1.dp, NeonPink.copy(alpha = 0.5f))
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = NeonPink) },
                                text = { Text("EDIT_DIRECTIVE", fontFamily = FontFamily.Monospace, color = Color.White) },
                                onClick = {
                                    showOverflowMenu = false
                                    showEditDirectiveDialog = true
                                }
                            )
                            val isPaused = uiState.directive?.status == DirectiveStatus.PAUSED
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
                                        text = if (isPaused) "RESUME_GRID" else "PAUSE_GRID",
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.updateStatus(
                                        if (isPaused) DirectiveStatus.ACTIVE else DirectiveStatus.PAUSED
                                    )
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = NeonPink) },
                                text = { Text("DUPLICATE", fontFamily = FontFamily.Monospace, color = Color.White) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.duplicateDirective()
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, tint = NeonPink) },
                                text = { Text("ARCHIVE", fontFamily = FontFamily.Monospace, color = NeonPink) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.updateStatus(DirectiveStatus.ARCHIVED)
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onBack()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF050104),
                    titleContentColor = NeonPink
                )
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Secondary Add Mission FAB
                FloatingActionButton(
                    onClick = { showAddMissionDialog = true },
                    containerColor = Color.Black,
                    contentColor = NeonCyan,
                    modifier = Modifier.neonBorder(color = NeonCyan, width = 1.dp, cornerRadius = 16.dp)
                ) {
                    Icon(Icons.Default.Explore, contentDescription = "New Mission")
                }

                // Primary Quick Task FAB prefilled with current Directive id
                FloatingActionButton(
                    onClick = { showQuickTaskBottomSheet = true },
                    containerColor = NeonPink,
                    contentColor = Color.Black,
                    modifier = Modifier.neonBorder(color = NeonPink, width = 2.dp, cornerRadius = 16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Standalone Pulse")
                }
            }
        },
        containerColor = Color(0xFF050104)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonPink)
                }
            } else {
                val directive = uiState.directive
                if (directive != null) {
                    
                    val daysRemaining = if (directive.targetEndDate != null) {
                        ChronoUnit.DAYS.between(LocalDate.now(), directive.targetEndDate)
                    } else 0

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // 1. HERO VISION SECTOR
                        item {
                            CyberFrame(
                                label = "VISION_CORE_COMMAND",
                                accentColor = NeonPink
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1.3f)) {
                                            Text(
                                                text = "VISION_STATEMENT",
                                                color = Color.Gray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = directive.visionStatement ?: "\"Forge your digital core. Stabilize analog pathways to ascend.\"",
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                fontStyle = FontStyle.Italic,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Circular rolling telemetry progress indicator
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                progress = { directive.currentProgress },
                                                modifier = Modifier.fillMaxSize(),
                                                color = NeonPink,
                                                trackColor = Color.White.copy(alpha = 0.05f),
                                                strokeWidth = 4.dp
                                            )
                                            Text(
                                                text = "${(directive.currentProgress * 100).toInt()}%",
                                                color = NeonPink,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }

                                    // Holographic Reactive Status Grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "ARCHETYPE_PROTOCOLS",
                                                color = Color.Gray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = (directive.archetypeTag ?: "ENDUR-RUNNER").uppercase(),
                                                color = NeonPink,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "UPLINK_STATUS",
                                                color = Color.Gray,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "XP: ${directive.totalXPContributed} PTS",
                                                color = NeonCyan,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (directive.isQuarterly) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(NeonPink.copy(alpha = 0.05f))
                                                .border(1.dp, NeonPink, RoundedCornerShape(4.dp))
                                                .padding(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    "QUARTERLY_GRID_ACTIVE",
                                                    color = NeonPink,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "$daysRemaining DAYS REMAINING",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 2. SUCCESS METRICS SECTOR
                        if (directive.successMetrics.isNotEmpty()) {
                            item {
                                CyberFrame(label = "SUCCESS_METRIC_TELEMETRY", accentColor = NeonPink) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        directive.successMetrics.forEach { metric ->
                                            SuccessMetricRow(metric)
                                        }
                                    }
                                }
                            }
                        }

                        // 3. MISSION OVERVIEW (Active Campaigns Carousel)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "ACTIVE_CAMPAIGNS",
                                    color = NeonCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                OutlinedButton(
                                    onClick = { showAddMissionDialog = true },
                                    border = BorderStroke(1.dp, NeonCyan),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("DEPLOY", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        if (uiState.missions.isEmpty()) {
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
                                            "MISSIONS_SECTOR_AWAITING_FORGE",
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            "This Directive is a blank neural slate.",
                                            color = Color.Gray,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(uiState.missions) { mission ->
                                        Card(
                                            onClick = { onMissionClick(mission.id) },
                                            modifier = Modifier
                                                .width(220.dp)
                                                .height(130.dp)
                                                .neonBorder(color = NeonCyan.copy(alpha = 0.3f), cornerRadius = 8.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF02070D))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text(
                                                        text = mission.title.uppercase(),
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = mission.description,
                                                        color = Color.Gray,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        maxLines = 2,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${(mission.progress * 100).toInt()}% SYNC",
                                                        color = NeonCyan,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                        contentDescription = null,
                                                        tint = NeonCyan,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. DIRECT STANDALONE PULSES
                        item {
                            Text(
                                "DIRECT_STANDALONE_PULSES",
                                color = NeonPink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (uiState.directTasks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.White.copy(alpha = 0.02f))
                                        .border(1.dp, Color.White.copy(alpha = 0.05f))
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "NO_DIRECT_STANDALONE_PULSES_FOUND",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        } else {
                            items(uiState.directTasks) { task ->
                                val completed = task.lastCompleted != null
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0D0306))
                                        .border(1.dp, Color.White.copy(alpha = 0.02f))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        IconButton(
                                            onClick = { if (!completed) viewModel.completeStandaloneTask(task) },
                                            enabled = !completed
                                        ) {
                                            Icon(
                                                imageVector = if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = "Synchronize pulse",
                                                tint = if (completed) Color.Green else Color.Gray
                                            )
                                        }

                                        Column {
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
                                    }

                                    Text(
                                        text = "+${task.xpValue} XP",
                                        color = NeonPink,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 4. OVERALL MOMENTUM
                        item {
                            CyberFrame(label = "SYNC_MOMENTUM_STABILITY", accentColor = NeonCyan) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "Combined stability tracking (past 30 days)",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val today = LocalDate.now()
                                        val dates = (0..29).map { today.minusDays(it.toLong()) }.reversed()
                                        
                                        // Render blocks in compact rows
                                        dates.chunked(10).forEach { chunk ->
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                chunk.forEach { date ->
                                                    val isCompletedAnywhere = uiState.directTasks.any {
                                                        it.lastCompleted?.atZone(ZoneId.systemDefault())?.toLocalDate() == date
                                                    }
                                                    val color = if (isCompletedAnywhere) NeonCyan else Color.White.copy(alpha = 0.05f)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .background(color, RoundedCornerShape(2.dp))
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Average alignment stability: Stable (98.4%). Strong momentum on mornings.",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // 5. NEON MENTOR COMMAND CENTER
                        item {
                            CyberFrame(
                                label = "NEON_MENTOR_COMMAND_CENTER",
                                accentColor = NeonPink
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CYBR-TES ACTIVE PROTOCOLS",
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
                                                text = uiState.selectedMentorMode.name,
                                                color = NeonPink,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = "\"Access dialectic synthesis engine. AI Progress Architect stands ready to deconstruct your vision into atomic missions.\"",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontStyle = FontStyle.Italic,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // Direct quick mode select buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        MentorMode.entries.forEach { mode ->
                                            val isSelected = uiState.selectedMentorMode == mode
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isSelected) NeonPink.copy(alpha = 0.15f) else Color.Transparent)
                                                    .border(1.dp, if (isSelected) NeonPink else Color.Gray, RoundedCornerShape(4.dp))
                                                    .clickable { viewModel.updateMentorMode(mode) }
                                                    .padding(vertical = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = mode.name,
                                                    color = if (isSelected) NeonPink else Color.White,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { showChatDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("ENGAGE NEON MENTOR", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }

                        // 6. OPERATIVE NOTES
                        item {
                            CyberFrame(label = "OPERATIVE_NOTES", accentColor = NeonPink) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = if (directive.notes.isNullOrBlank()) "No journal entries linked." else directive.notes!!,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Button(
                                        onClick = { showNotesDialog = true },
                                        modifier = Modifier.align(Alignment.End),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        border = BorderStroke(1.dp, NeonPink)
                                    ) {
                                        Text("EDIT_NOTES", color = NeonPink, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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

    // 1. Quick Task Bottom Sheet Prefilled with directiveId
    if (showQuickTaskBottomSheet) {
        QuickTaskBottomSheet(
            onDismiss = { showQuickTaskBottomSheet = false },
            prefilledParentId = directiveId
        )
    }

    // 2. Edit Directive Dialog
    if (showEditDirectiveDialog) {
        Dialog(onDismissRequest = { showEditDirectiveDialog = false }) {
            CyberFrame(label = "EDIT_DIRECTIVE_PARAMETERS", accentColor = NeonPink) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("DIRECTIVE_TITLE", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                    )
                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("DIRECTIVE_DESCRIPTION", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                    )
                    OutlinedTextField(
                        value = visionInput,
                        onValueChange = { visionInput = it },
                        label = { Text("VISION_STATEMENT", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                    )
                    OutlinedTextField(
                        value = archetypeTagInput,
                        onValueChange = { archetypeTagInput = it },
                        label = { Text("ARCHETYPE_TAG", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                    )
                    OutlinedTextField(
                        value = targetDateInput,
                        onValueChange = { targetDateInput = it },
                        label = { Text("TARGET_DATE (YYYY-MM-DD)", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = isQuarterlyInput,
                            onCheckedChange = { isQuarterlyInput = it },
                            colors = CheckboxDefaults.colors(checkedColor = NeonPink)
                        )
                        Text(
                            text = "QUARTERLY_DIRECTIVE",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { showEditDirectiveDialog = false }) {
                            Text("CANCEL", fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                val date = try {
                                    LocalDate.parse(targetDateInput.trim())
                                } catch (e: Exception) {
                                    null
                                }
                                viewModel.updateDirectiveDetails(
                                    title = titleInput,
                                    description = descInput,
                                    visionStatement = visionInput.ifBlank { null },
                                    isQuarterly = isQuarterlyInput,
                                    archetypeTag = archetypeTagInput.ifBlank { null },
                                    targetEndDate = date
                                )
                                showEditDirectiveDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black)
                        ) {
                            Text("UPDATE", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    // 3. Deploy New Mission Dialog
    if (showAddMissionDialog) {
        var missionTitle by remember { mutableStateOf("") }
        var missionDesc by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddMissionDialog = false }) {
            CyberFrame(label = "DEPLOY_NEW_MISSION", accentColor = NeonCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = missionTitle,
                        onValueChange = { missionTitle = it },
                        label = { Text("MISSION_TITLE", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                    )
                    OutlinedTextField(
                        value = missionDesc,
                        onValueChange = { missionDesc = it },
                        label = { Text("MISSION_DESCRIPTION", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { showAddMissionDialog = false }) {
                            Text("CANCEL", fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                if (missionTitle.isNotBlank()) {
                                    viewModel.addMission(missionTitle, missionDesc)
                                    showAddMissionDialog = false
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                        ) {
                            Text("DEPLOY", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    // 4. Edit Operative Notes Dialog
    if (showNotesDialog) {
        Dialog(onDismissRequest = { showNotesDialog = false }) {
            CyberFrame(label = "EDIT_OPERATIVE_NOTES", accentColor = NeonPink) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { showNotesDialog = false }) {
                            Text("CANCEL", fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                viewModel.updateNotes(notesInput)
                                showNotesDialog = false
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

    // 5. AI Mentor Dialogue Chat Dialog
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
                    // Mode Selector
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

@Composable
fun SuccessMetricRow(metric: SuccessMetric) {
    val progress = if (metric.targetValue > 0) (metric.currentValue / metric.targetValue).coerceIn(0f, 1f) else 0f
    val color = when (metric.type) {
        MetricType.BIOMETRIC -> NeonCyan
        MetricType.STREAK -> Color.Yellow
        else -> NeonPink
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = when (metric.type) {
                        MetricType.BIOMETRIC -> Icons.Default.Favorite
                        MetricType.STREAK -> Icons.Default.Timeline
                        MetricType.XP -> Icons.Default.Bolt
                        MetricType.MANUAL -> Icons.Default.Analytics
                    },
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = metric.description.uppercase(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${metric.currentValue.toInt()} / ${metric.targetValue.toInt()} ${metric.unit ?: ""}",
                color = color,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.1f)
        )
    }
}
