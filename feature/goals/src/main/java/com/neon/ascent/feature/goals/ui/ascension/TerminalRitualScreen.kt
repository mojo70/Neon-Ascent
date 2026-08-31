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
    val theme = LocalNeonTheme.current
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
                            color = theme.secondary
                        )
                        Text(
                            text = "Q${uiState.currentQuarter} ${uiState.currentYear} ARCHIVE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = theme.accent
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.secondary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = theme.canvas,
                    titleContentColor = theme.secondary
                )
            )
        },
        bottomBar = {
            // Bottom Sticky Action Bar
            Surface(
                color = theme.canvas,
                border = BorderStroke(1.dp, theme.secondary.copy(alpha = 0.2f)),
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
                        border = BorderStroke(1.dp, theme.secondary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.secondary),
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
                            border = BorderStroke(1.dp, theme.accent),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = theme.accent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPORT_LOG", color = theme.accent, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
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
                            colors = ButtonDefaults.buttonColors(containerColor = theme.secondary, contentColor = theme.canvas),
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                            isCurrent -> theme.secondary
                            isPassed -> theme.accent
                            else -> theme.inkMuted
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
                                    .background(if (isCurrent) theme.secondary.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(12.dp))
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

                HorizontalDivider(color = theme.ink.copy(alpha = 0.05f))

                // STEP 1: OVERVIEW HERO SECTION
                if (currentStep == 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CyberFrame(label = "Q${uiState.currentQuarter} TERMINAL_REVIEW", accentColor = theme.secondary) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Q${uiState.currentQuarter} ${uiState.currentYear} TERMINAL REVIEW",
                                    color = theme.ink,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )

                                Text(
                                    text = "Analyzing completion nodes across nested campaign metrics...",
                                    color = theme.inkMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                )

                                // Reflective pose holographic avatar placeholder
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(theme.surface)
                                        .border(1.dp, theme.secondary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessibilityNew,
                                        contentDescription = null,
                                        tint = theme.secondary,
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
                                color = theme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                label = "MISSIONS_INTEGRATED",
                                value = "${uiState.missions.size}",
                                color = theme.accent,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            StatBox(
                                label = "TOTAL_COMPLETIONS",
                                value = "${uiState.completionHistory.size}",
                                color = theme.accent,
                                modifier = Modifier.weight(1f)
                            )
                            val maxStreak = if (uiState.tasks.isNotEmpty()) uiState.tasks.maxOf { it.currentStreak } else 0
                            StatBox(
                                label = "LONGEST_STREAK",
                                value = "$maxStreak DAYS",
                                color = if (theme.mode == VisualMode.STEVE) theme.ink else Color.Yellow,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Heatmap Area
                        CyberFrame(label = "NEURAL_CONSISTENCY_MAP", accentColor = theme.accent) {
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
                                    colors = CardDefaults.cardColors(containerColor = theme.surfaceRaised),
                                    border = BorderStroke(1.dp, theme.secondary.copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = directive.title.uppercase(),
                                                color = theme.ink,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "${(directive.currentProgress * 100).toInt()}% SYNC",
                                                color = theme.secondary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }

                                        Text(
                                            text = directive.description,
                                            color = theme.inkMuted,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )

                                        LinearProgressIndicator(
                                            progress = { directive.currentProgress },
                                            modifier = Modifier.fillMaxWidth(),
                                            color = theme.secondary,
                                            trackColor = theme.ink.copy(alpha = 0.05f)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "MISSIONS: ${nestedMissions.size} ACTIVE",
                                                color = theme.accent,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = "TASKS: ${directTasks.size} STANDALONE",
                                                color = theme.ink,
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
                            color = theme.accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )

                        CyberFrame(label = "PATTERN_ANALYSIS_STREAM", accentColor = theme.secondary) {
                            if (uiState.ritualAnalysis == null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = theme.secondary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "SYNTHESIZING_PATTERN_INSIGHTS...",
                                        color = theme.inkMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                Text(
                                    text = uiState.ritualAnalysis!!,
                                    color = theme.ink,
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
                            colors = CardDefaults.cardColors(containerColor = theme.surfaceRaised),
                            border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "STRENGTHS_DETECTED",
                                    color = theme.accent,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "High morning protocol consistency detected. Sync ratio has stabilized over 90% during morning hours.",
                                    color = theme.ink,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = theme.surfaceRaised),
                            border = BorderStroke(1.dp, theme.secondary.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "IDENTITY_SHIFTS",
                                    color = theme.secondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "You are becoming an operator who consistently prioritizes physical and cognitive baseline synchronization.",
                                    color = theme.ink,
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

                        CyberFrame(label = "MAJOR_CELEBRATION_DOME", accentColor = theme.secondary) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "DETONATE_DOPAMINE_FEEDBACK",
                                    color = theme.ink,
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
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.secondary, contentColor = theme.canvas),
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
                            colors = CardDefaults.cardColors(containerColor = theme.surfaceRaised),
                            border = BorderStroke(1.dp, theme.accent.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    "QUARTERLY_MILESTONES",
                                    color = theme.accent,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
                                    Text("Top Missions Completed: Hydration Ritual, Core Cardio Lift", fontSize = 11.sp, color = theme.ink, fontFamily = FontFamily.Monospace)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = theme.accent, modifier = Modifier.size(16.dp))
                                    Text("ADHD Grace Recoveries logged: 4 recoveries synchronized", fontSize = 11.sp, color = theme.ink, fontFamily = FontFamily.Monospace)
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

                        CyberFrame(label = "CYBR-TES // PLANNING_RECOMMENDATIONS", accentColor = theme.accent) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "AI proposed strategic directives for the next period:",
                                    color = theme.inkMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "1. FORTIFY THE MIND V4: To attain peak neuro-performance and block cognitive intrusion vectors.",
                                    color = theme.ink,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "2. HYDRAULIC REVOLUTION: Secure daily hydration, metabolic pathways, and endurance metrics.",
                                    color = theme.ink,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // Form to add customized Directive
                        CyberFrame(label = "FORGE_CUSTOM_DIRECTIVE", accentColor = theme.secondary) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = proposedTitle,
                                    onValueChange = { proposedTitle = it },
                                    label = { Text("DIRECTIVE_TITLE", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = theme.ink, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = theme.secondary,
                                        unfocusedBorderColor = theme.inkMuted.copy(alpha = 0.3f),
                                        unfocusedTextColor = theme.ink,
                                        focusedTextColor = theme.ink
                                    )
                                )

                                OutlinedTextField(
                                    value = proposedDesc,
                                    onValueChange = { proposedDesc = it },
                                    label = { Text("DESCRIPTION", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = theme.ink, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = theme.secondary,
                                        unfocusedBorderColor = theme.inkMuted.copy(alpha = 0.3f),
                                        unfocusedTextColor = theme.ink,
                                        focusedTextColor = theme.ink
                                    )
                                )

                                OutlinedTextField(
                                    value = proposedVision,
                                    onValueChange = { proposedVision = it },
                                    label = { Text("VISION_STATEMENT", fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(color = theme.ink, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = theme.secondary,
                                        unfocusedBorderColor = theme.inkMuted.copy(alpha = 0.3f),
                                        unfocusedTextColor = theme.ink,
                                        focusedTextColor = theme.ink
                                    )
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
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.secondary, contentColor = theme.canvas),
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
    val theme = LocalNeonTheme.current
    val now = LocalDate.now()
    val startOfQuarter = now.with(IsoFields.DAY_OF_QUARTER, 1L)
    
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("MON", fontSize = 8.sp, color = theme.inkMuted, fontFamily = FontFamily.Monospace)
            Text("WED", fontSize = 8.sp, color = theme.inkMuted, fontFamily = FontFamily.Monospace)
            Text("FRI", fontSize = 8.sp, color = theme.inkMuted, fontFamily = FontFamily.Monospace)
            Text("SUN", fontSize = 8.sp, color = theme.inkMuted, fontFamily = FontFamily.Monospace)
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
                                date.isAfter(now) -> theme.inkMuted.copy(alpha = 0.2f)
                                count == 0 -> theme.inkMuted.copy(alpha = 0.5f)
                                count == 1 -> theme.accent.copy(alpha = 0.4f)
                                count == 2 -> theme.accent.copy(alpha = 0.7f)
                                else -> theme.accent
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
    val theme = LocalNeonTheme.current
    Box(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Column {
            Text(label, color = color, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(value, color = theme.ink, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
    }
}
