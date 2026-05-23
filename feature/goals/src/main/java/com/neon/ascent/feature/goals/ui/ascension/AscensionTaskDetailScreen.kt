package com.neon.ascent.feature.goals.ui.ascension

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink
import com.neon.ascent.core.common.CelebrationOverlay
import com.neon.ascent.core.common.neonBorder
import com.neon.ascent.core.common.cyberGlitch
import com.neon.ascent.core.domain.goals.models.*
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AscensionTaskDetailScreen(
    taskId: String,
    prefillAction: String? = null,
    onBack: () -> Unit,
    viewModel: AscensionTaskDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Quick log states
    var notes by remember { mutableStateOf("") }
    var mood by remember { mutableFloatStateOf(3f) }
    var amountString by remember { mutableStateOf("") }
    
    // Collapsible sections
    var isLoggingExpanded by remember { mutableStateOf(true) }
    var isDetailsExpanded by remember { mutableStateOf(false) }
    var isHistoryExpanded by remember { mutableStateOf(false) }
    var isMentorExpanded by remember { mutableStateOf(true) }
    
    // Modals Triggers
    var showEditDialog by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var mentorQuestion by remember { mutableStateOf("") }

    LaunchedEffect(taskId) {
        viewModel.loadTask(taskId)
    }

    LaunchedEffect(uiState.task, prefillAction) {
        if (prefillAction == "complete" && uiState.task != null && !uiState.isCompletedToday) {
            viewModel.completeTask("AUTO_SYNCED_VIA_DEEP_LINK", 5)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        uiState.task?.title ?: "TASK_DETAILS", 
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonCyan)
                    }
                },
                actions = {
                    val statusText = if (uiState.isCompletedToday) "COMPLETED" else "ACTIVE"
                    val statusColor = if (uiState.isCompletedToday) Color.Green else NeonCyan
                    
                    Box(
                        modifier = Modifier
                            .background(statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp))
                            .border(1.dp, statusColor, shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = NeonCyan)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF0F141D))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Task", color = Color.White, fontFamily = FontFamily.Monospace) },
                                onClick = {
                                    showMenu = false
                                    showEditDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate Task", color = Color.White, fontFamily = FontFamily.Monospace) },
                                onClick = {
                                    showMenu = false
                                    viewModel.duplicateTask()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Archive Task", color = Color.Red, fontFamily = FontFamily.Monospace) },
                                onClick = {
                                    showMenu = false
                                    viewModel.archiveTask()
                                    onBack()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF020508),
                    titleContentColor = NeonCyan
                )
            )
        },
        bottomBar = {
            // Persistent Bottom Bar for Snooze, Skip, and Mark Complete Later
            if (!uiState.isCompletedToday) {
                Surface(
                    color = Color(0xFF020508),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { viewModel.snoozeTask() }) {
                            Icon(Icons.Default.Snooze, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("SNOOZE", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        
                        TextButton(onClick = { showSkipDialog = true }) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = NeonPink, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("SKIP_REFLECT", color = NeonPink, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                        
                        TextButton(onClick = { viewModel.markCompleteLater() }) {
                            Icon(Icons.Default.Update, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("MARK_LATER", color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF020508)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    // Hero Section
                    CyberFrame(
                        label = "LINK_HERO // DIAGNOSTICS",
                        accentColor = NeonCyan
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Immersive Holographic Icon Placeholder
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .border(1.dp, NeonCyan, shape = RoundedCornerShape(36.dp))
                                    .background(NeonCyan.copy(alpha = 0.05f), shape = RoundedCornerShape(36.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val lastCompletedDate = uiState.task?.lastCompleted?.atZone(ZoneId.systemDefault())?.toLocalDate()
                                val todayLocalDate = LocalDate.now()
                                val isCompletedToday = lastCompletedDate == todayLocalDate
                                val isCompletedYesterday = lastCompletedDate == todayLocalDate.minusDays(1)
                                val inGracePeriod = lastCompletedDate != null && !isCompletedToday && !isCompletedYesterday && 
                                    lastCompletedDate.isAfter(todayLocalDate.minusDays((uiState.task?.graceBufferDays ?: 1).toLong() + 1))

                                val chainGlowColor = if (inGracePeriod) Color(0xFFFFCC00) else NeonPink
                                val chainGlowText = if (inGracePeriod) "FLICKERING (GRACE_BUFFER)" else "STABLE"

                                Text(
                                    "${uiState.task?.currentStreak ?: 0} DAY NEON CHAIN",
                                    color = chainGlowColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.cyberGlitch(intensity = if (inGracePeriod) 0.3f else 0f)
                                )
                                Text(
                                    "CHAIN_STATUS: $chainGlowText",
                                    color = chainGlowColor.copy(alpha = 0.6f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                
                                if (uiState.parentName != null) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Contributes to: ${uiState.parentName} • ${(uiState.task?.xpValue ?: 10) * 5}%",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Main Action Area: LOG COMPLETE Button
                    if (!uiState.isCompletedToday) {
                        Button(
                            onClick = {
                                val finalNotes = if (amountString.isNotBlank()) {
                                    "[Amount: $amountString] $notes"
                                } else {
                                    notes
                                }
                                viewModel.completeTask(finalNotes, mood.toInt())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .neonBorder(color = NeonPink, width = 2.dp, cornerRadius = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonPink,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "LOG COMPLETE +${uiState.task?.xpValue ?: 10} XP", 
                                fontFamily = FontFamily.Monospace, 
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Accordion: Parameter / Log inputs
                    if (!uiState.isCompletedToday) {
                        CyberFrame(label = "INPUT_LOG_PARAMETERS", accentColor = NeonCyan) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isLoggingExpanded = !isLoggingExpanded }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "EDIT_LOG_PARAMETERS",
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = if (isLoggingExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                AnimatedVisibility(visible = isLoggingExpanded) {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        OutlinedTextField(
                                            value = notes,
                                            onValueChange = { notes = it },
                                            label = { Text("REFLECTIVE_NOTES", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                            placeholder = { Text(uiState.task?.userNotesTemplate ?: "Enter notes...", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonPink,
                                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                                focusedLabelColor = NeonPink
                                            )
                                        )

                                        OutlinedTextField(
                                            value = amountString,
                                            onValueChange = { amountString = it },
                                            label = { Text("AMOUNT_LOGGED (e.g. 16 oz, 5 reps)", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                            modifier = Modifier.fillMaxWidth(),
                                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = NeonPink,
                                                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                                focusedLabelColor = NeonPink
                                            )
                                        )

                                        Column {
                                            Text(
                                                "MOOD_SYNC: ${mood.toInt()} // Emoji scale",
                                                color = NeonPink,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Slider(
                                                value = mood,
                                                onValueChange = { mood = it },
                                                valueRange = 1f..5f,
                                                steps = 3,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = NeonPink,
                                                    activeTrackColor = NeonPink,
                                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Accordion: Task Details
                    CyberFrame(label = "TASK_DECRYPTION // DATA", accentColor = NeonCyan) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isDetailsExpanded = !isDetailsExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "VIEW_SPECIFICATIONS",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (isDetailsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            AnimatedVisibility(visible = isDetailsExpanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = uiState.task?.description ?: "No specifications mapped.",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "RECURRENCE: ${uiState.task?.type?.name ?: "ONE_TIME"} • Window: ${uiState.task?.timeWindows?.joinToString(", ") ?: "Anytime"}",
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "GRACE_BUFFER: ${uiState.task?.graceBufferDays ?: 1} Days",
                                        color = NeonPink,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Accordion: History Heatmap
                    CyberFrame(label = "NEURAL_LOG_HISTORY // VISUAL", accentColor = NeonPink) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isHistoryExpanded = !isHistoryExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "EXPAND_HEATMAP_GRID",
                                    color = NeonPink,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (isHistoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = NeonPink,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            AnimatedVisibility(visible = isHistoryExpanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // 14-day grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        val today = LocalDate.now()
                                        val dates = (0..13).map { today.minusDays(it.toLong()) }.reversed()
                                        
                                        dates.forEach { date ->
                                            val isDone = uiState.completions.any { 
                                                it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == date 
                                            }
                                            val blockColor = if (isDone) Color(0xFF00FFAA) else Color.White.copy(alpha = 0.08f)
                                            
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(blockColor, shape = RoundedCornerShape(2.dp))
                                                    .border(1.dp, if (isDone) Color(0xFF00FFAA).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f))
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Recent 5 completions
                                    Text(
                                        "RECENT_LOG_ENTRIES",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    if (uiState.completions.isEmpty()) {
                                        Text(
                                            "No completions recorded.",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    } else {
                                        uiState.completions.take(5).forEach { completion ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    completion.notes ?: "No notes recorded.",
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Text(
                                                    "MOOD_0${completion.mood ?: 3}",
                                                    color = NeonPink,
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
                    }

                    // Accordion: Neon AI Mentor
                    CyberFrame(label = "AI_STRATEGIST // COGNITIVE_OVERRIDE", accentColor = NeonCyan) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isMentorExpanded = !isMentorExpanded }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "ASK_NEON_MENTOR",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (isMentorExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            AnimatedVisibility(visible = isMentorExpanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "Query CYBR-TES on tactical execution of this protocol.",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    
                                    OutlinedTextField(
                                        value = mentorQuestion,
                                        onValueChange = { mentorQuestion = it },
                                        placeholder = { Text("How do I start? / Analyze my blocker...", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                                        modifier = Modifier.fillMaxWidth(),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeonCyan,
                                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                                            focusedLabelColor = NeonCyan
                                        )
                                    )
                                    
                                    Button(
                                        onClick = { 
                                            if (mentorQuestion.isNotBlank()) {
                                                viewModel.askMentor(mentorQuestion)
                                            }
                                        },
                                        enabled = mentorQuestion.isNotBlank() && !uiState.isAskingMentor,
                                        modifier = Modifier.align(Alignment.End),
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        if (uiState.isAskingMentor) {
                                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp))
                                        } else {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("ASK_MENTOR", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    uiState.mentorAnswer?.let { answer ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color.Black.copy(alpha = 0.4f))
                                                .border(1.dp, NeonCyan.copy(alpha = 0.3f))
                                                .padding(12.dp)
                                        ) {
                                            Text(
                                                answer,
                                                color = NeonCyan,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Dialectic Reflection UI
                    AnimatedVisibility(
                        visible = uiState.showReflection,
                        enter = fadeIn() + expandVertically()
                    ) {
                        CyberFrame(label = "CYBR-TES // DIALECTIC_REFLECTION", accentColor = Color.Green) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Psychology, contentDescription = null, tint = Color.Green)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "ANALYSIS_COMPLETE",
                                        color = Color.Green,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                
                                Text(
                                    uiState.reflectionText ?: "Analyzing neural logs...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )

                                Button(
                                    onClick = onBack,
                                    modifier = Modifier.align(Alignment.End),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    border = BorderStroke(1.dp, Color.Green)
                                ) {
                                    Text("ACKNOWLEDGE", color = Color.Green, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }

            CelebrationOverlay(
                event = uiState.dopamineEvent,
                onFinished = { viewModel.clearDopamineEvent() }
            )
        }
    }

    // Modal: Skip with Reflection Reason
    if (showSkipDialog) {
        var skipReason by remember { mutableStateOf("") }
        Dialog(onDismissRequest = { showSkipDialog = false }) {
            CyberFrame(label = "SKIP_REFLECTION_LOG", accentColor = NeonPink) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Explain the deviation from protocol execution today. What glitched?",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedTextField(
                        value = skipReason,
                        onValueChange = { skipReason = it },
                        placeholder = { Text("Reason for skip...", color = Color.Gray, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { showSkipDialog = false }) {
                            Text("CANCEL", fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                if (skipReason.isNotBlank()) {
                                    viewModel.skipWithReflection(skipReason)
                                    showSkipDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                            enabled = skipReason.isNotBlank()
                        ) {
                            Text("SUBMIT", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }

    // Modal: Edit Task Specifications
    if (showEditDialog) {
        val currentTask = uiState.task
        var editTitle by remember { mutableStateOf(currentTask?.title ?: "") }
        var editDesc by remember { mutableStateOf(currentTask?.description ?: "") }
        var editBuffer by remember { mutableFloatStateOf(currentTask?.graceBufferDays?.toFloat() ?: 1f) }
        var editXp by remember { mutableFloatStateOf(currentTask?.xpValue?.toFloat() ?: 10f) }
        
        Dialog(onDismissRequest = { showEditDialog = false }) {
            CyberFrame(label = "EDIT_TASK_SPECIFICATIONS", accentColor = NeonCyan) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("TASK_TITLE", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                    )
                    
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("TASK_DESCRIPTION", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan)
                    )
                    
                    Column {
                        Text(
                            "GRACE_BUFFER: ${editBuffer.toInt()} Days",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Slider(
                            value = editBuffer,
                            onValueChange = { editBuffer = it },
                            valueRange = 1f..3f,
                            steps = 1,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    Column {
                        Text(
                            "XP_VALUE: ${editXp.toInt()} XP",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Slider(
                            value = editXp,
                            onValueChange = { editXp = it },
                            valueRange = 5f..30f,
                            steps = 4,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        OutlinedButton(onClick = { showEditDialog = false }) {
                            Text("CANCEL", fontFamily = FontFamily.Monospace)
                        }
                        Button(
                            onClick = {
                                if (editTitle.isNotBlank()) {
                                    viewModel.editTask(editTitle, editDesc, editBuffer.toInt(), editXp.toInt())
                                    showEditDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                            enabled = editTitle.isNotBlank()
                        ) {
                            Text("SAVE", fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
