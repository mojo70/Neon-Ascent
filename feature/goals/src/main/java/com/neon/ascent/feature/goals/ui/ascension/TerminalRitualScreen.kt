package com.neon.ascent.feature.goals.ui.ascension

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.goals.models.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalRitualScreen(
    onBack: () -> Unit,
    viewModel: TerminalRitualViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Stepper State
    var currentStep by remember { mutableIntStateOf(1) }

    // Celebration state for Chamber
    var chamberDopamineEvent by remember { mutableStateOf<DopamineEvent?>(null) }

    // Proposed directives for Step 5 planning
    var proposedTitle by remember { mutableStateOf("FORTIFY_THE_MIND_V4") }
    var proposedDesc by remember { mutableStateOf("Optimize cognitive pathways, meditation, and remote viewing sessions.") }
    var proposedVision by remember { mutableStateOf("To attain peak neuro-performance and block Arasaka cognitive intrusion vectors.") }

    LaunchedEffect(Unit) {
        viewModel.exportEvent.collect { logContent ->
            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, logContent)
                type = "text/markdown"
            }
            val shareIntent = android.content.Intent.createChooser(sendIntent, "EXPORT NEURAL LOG")
            context.startActivity(shareIntent)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NEON_SANCTUM // REVIEW", 
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = NeonPink
                        )
                        Text(
                            text = "Q${uiState.currentQuarter} ${uiState.currentYear} ARCHIVE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = NeonCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = NeonPink)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF030104),
                    titleContentColor = NeonPink
                )
            )
        },
        bottomBar = {
            // Bottom Sticky Action Bar
            Surface(
                color = Color(0xFF020103),
                border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (currentStep > 1) {
                                currentStep--
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            } else {
                                onBack()
                            }
                        },
                        border = BorderStroke(1.dp, NeonPink),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonPink),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (currentStep == 1) "ABORT_RITUAL" else "PREVIOUS_STEP",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.exportNeuralLog() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, NeonCyan),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPORT_LOG", color = NeonCyan, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                if (currentStep < 5) {
                                    currentStep++
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onBack()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (currentStep == 5) "SAVE & CLOSE" else "CONTINUE",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF030104)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. PROGRESS STEPPER GRID
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("OVERVIEW", "DIRECTIVES", "INSIGHTS", "CELEBRATE", "FORGE NEXT").forEachIndexed { idx, stepName ->
                        val stepNum = idx + 1
                        val isCurrent = currentStep == stepNum
                        val isPassed = currentStep > stepNum
                        
                        val stepColor = when {
                            isCurrent -> NeonPink
                            isPassed -> NeonCyan
                            else -> Color.Gray
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    currentStep = stepNum
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(if (isCurrent) NeonPink.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(12.dp))
                                    .border(1.dp, stepColor, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$stepNum",
                                    color = stepColor,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = stepName,
                                color = stepColor,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

                // STEP 1: OVERVIEW HERO SECTION
                if (currentStep == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CyberFrame(label = "Q${uiState.currentQuarter} TERMINAL_REVIEW", accentColor = NeonPink) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Q${uiState.currentQuarter} ${uiState.currentYear} TERMINAL REVIEW",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Analyzing completion nodes across nested campaign metrics...",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )

                                // Reflective pose holographic avatar placeholder
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black)
                                        .border(1.dp, NeonPink.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessibilityNew,
                                        contentDescription = null,
                                        tint = NeonPink,
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                        }

                        // Stats Summary Grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatBox(
                                label = "DIRECTIVES_LINKED",
                                value = "${uiState.directives.size}",
                                color = NeonPink,
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                label = "MISSIONS_INTEGRATED",
                                value = "${uiState.missions.size}",
                                color = NeonCyan,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatBox(
                                label = "TOTAL_COMPLETIONS",
                                value = "${uiState.completionHistory.size}",
                                color = NeonCyan,
                                modifier = Modifier.weight(1f)
                            )
                            val maxStreak = if (uiState.tasks.isNotEmpty()) uiState.tasks.maxOf { it.currentStreak } else 0
                            StatBox(
                                label = "LONGEST_STREAK",
                                value = "$maxStreak DAYS",
                                color = Color.Yellow,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Heatmap Area
                        CyberFrame(label = "NEURAL_CONSISTENCY_MAP", accentColor = NeonCyan) {
                            NeuralHeatmap(uiState.heatmapData)
                        }
                    }
                }

                // STEP 2: DIRECTIVE REPORT (Hierarchy Breakdown)
                if (currentStep == 2) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "DIRECTIVE_HIERARCHY_ROLLUP",
                            color = NeonPink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.directives.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "NO_ACTIVE_DIRECTIVES_LOGGED_IN_PERIOD",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            uiState.directives.forEach { directive ->
                                val nestedMissions = uiState.missions.filter { it.directiveId == directive.id }
                                val directTasks = uiState.tasks.filter { it.parentId == directive.id }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0306)),
                                    border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = directive.title.uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "${(directive.currentProgress * 100).toInt()}% SYNC",
                                                color = NeonPink,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Text(
                                            text = directive.description,
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        LinearProgressIndicator(
                                            progress = { directive.currentProgress },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = NeonPink,
                                            trackColor = Color.White.copy(alpha = 0.05f)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "MISSIONS: ${nestedMissions.size} ACTIVE",
                                                color = NeonCyan,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "TASKS: ${directTasks.size} STANDALONE",
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // STEP 3: PATTERN INSIGHTS (AI Powered)
                if (currentStep == 3) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "CYBR-TES // DIAGNOSTIC_INSIGHTS",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )

                        CyberFrame(label = "PATTERN_ANALYSIS_STREAM", accentColor = NeonPink) {
                            if (uiState.ritualAnalysis == null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = NeonPink)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "SYNTHESIZING_PATTERN_INSIGHTS...",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                Text(
                                    text = uiState.ritualAnalysis!!,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        // Insights highlights cards
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF02070D)),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "STRENGTHS_DETECTED",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "High morning protocol consistency detected. Sync ratio has stabilized over 90% during morning hours.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0306)),
                            border = BorderStroke(1.dp, NeonPink.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "IDENTITY_SHIFTS",
                                    color = NeonPink,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "You are becoming an operator who consistently prioritizes physical and cognitive baseline synchronization.",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                // STEP 4: CELEBRATION CHAMBER
                if (currentStep == 4) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CELEBRATION_CHAMBER",
                            color = NeonPink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )

                        CyberFrame(label = "MAJOR_CELEBRATION_DOME", accentColor = NeonPink) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "DETONATE_DOPAMINE_FEEDBACK",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        chamberDopamineEvent = DopamineEvent(
                                            level = CelebrationLevel.ASCENSION,
                                            message = "Q${uiState.currentQuarter} Synchronized!",
                                            xpGained = uiState.completionHistory.size * 10
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("TRIGGER HOLOGRAPHIC REPLAY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        // Highlights list
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF02070D)),
                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "QUARTERLY_MILESTONES",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Text("Top Missions Completed: Hydration Ritual, Core Cardio Lift", fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                    Text("ADHD Grace Recoveries logged: 4 recoveries synchronized", fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }

                // STEP 5: FORWARD PLANNING - NEXT QUARTER FORGE
                if (currentStep == 5) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "NEXT_QUARTER_FORGE",
                            color = NeonPink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )

                        CyberFrame(label = "CYBR-TES // PLANNING_RECOMMENDATIONS", accentColor = NeonCyan) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "AI proposed strategic directives for the next period:",
                                    color = Color.Gray,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "1. FORTIFY THE MIND V4: To attain peak neuro-performance and block cognitive intrusion vectors.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "2. HYDRAULIC REVOLUTION: Secure daily hydration, metabolic pathways, and endurance metrics.",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Form to add customized Directive
                        CyberFrame(label = "FORGE_CUSTOM_DIRECTIVE", accentColor = NeonPink) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = proposedTitle,
                                    onValueChange = { proposedTitle = it },
                                    label = { Text("DIRECTIVE_TITLE", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                                )

                                OutlinedTextField(
                                    value = proposedDesc,
                                    onValueChange = { proposedDesc = it },
                                    label = { Text("DESCRIPTION", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                                )

                                OutlinedTextField(
                                    value = proposedVision,
                                    onValueChange = { proposedVision = it },
                                    label = { Text("VISION_STATEMENT", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonPink)
                                )

                                Button(
                                    onClick = {
                                        if (proposedTitle.isNotBlank()) {
                                            viewModel.createProposedDirective(
                                                title = proposedTitle,
                                                description = proposedDesc,
                                                visionStatement = proposedVision.ifBlank { null }
                                            )
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            // Reset inputs
                                            proposedTitle = ""
                                            proposedDesc = ""
                                            proposedVision = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = proposedTitle.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("FORGE & DEPLOY DIRECTIVE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(64.dp))
            }

            // Chamber Dopamine Overlay trigger
            CelebrationOverlay(
                event = chamberDopamineEvent,
                onFinished = { chamberDopamineEvent = null }
            )
        }
    }
}

@Composable
fun NeuralHeatmap(data: Map<LocalDate, Int>) {
    val now = LocalDate.now()
    val startOfQuarter = now.with(IsoFields.DAY_OF_QUARTER, 1L)
    
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MON", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
            Text("WED", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
            Text("FRI", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
            Text("SUN", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(4.dp))
        // Simple 13x7 grid for a quarter
        BoxWithConstraints {
            val size = (maxWidth - 48.dp) / 13
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until 7) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (col in 0 until 13) {
                            val dayOffset = col * 7 + row
                            val date = startOfQuarter.plusDays(dayOffset.toLong())
                            val count = data[date] ?: 0
                            val color = when {
                                date.isAfter(now) -> Color.DarkGray.copy(alpha = 0.2f)
                                count == 0 -> Color.DarkGray.copy(alpha = 0.5f)
                                count == 1 -> NeonCyan.copy(alpha = 0.4f)
                                count == 2 -> NeonCyan.copy(alpha = 0.7f)
                                else -> NeonCyan
                            }
                            Box(modifier = Modifier.size(size).background(color))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Column {
            Text(label, color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
    }
}
