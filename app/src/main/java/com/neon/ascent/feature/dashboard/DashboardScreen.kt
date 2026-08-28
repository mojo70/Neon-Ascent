package com.neon.ascent.feature.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.feature.settings.SettingsViewModel
import com.neon.ascent.feature.health.ui.HealthViewModel
import com.neon.ascent.feature.biohacking.BiohackingViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.character.models.UserCharacter
import com.neon.ascent.feature.health.domain.uplink.LiveBiometrics
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.feature.goals.ui.ascension.QuickTaskBottomSheet
import com.neon.ascent.core.common.CelebrationOverlay
import com.neon.ascent.ui.*
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@Composable
fun CyberCutFrame(
    modifier: Modifier = Modifier,
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    backgroundColor: Color = Color.Black.copy(alpha = 0.4f),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(backgroundColor, CyberCutShape)
            .border(1.dp, borderColor, CyberCutShape)
            .padding(16.dp)
    ) {
        content()
    }
}


@Composable
fun SlimChromeHeader(
    character: UserCharacter?,
    identity: OperatorIdentity,
    weatherState: WeatherState,
    liveMetrics: LiveBiometrics?,
    currentTime: LocalDateTime,
    onAvatarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    systemColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left Section: Avatar and Info
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bracketed Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clickable { onAvatarClick() }
            ) {
                HudBracket(Modifier.align(Alignment.TopStart), systemColor)
                HudBracket(Modifier.align(Alignment.TopEnd), systemColor, rotate = 90f)
                HudBracket(Modifier.align(Alignment.BottomStart), systemColor, rotate = 270f)
                HudBracket(Modifier.align(Alignment.BottomEnd), systemColor, rotate = 180f)
                
                AvatarImage(
                    character = character,
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column {
                Text(
                    text = "RUNNER // ${character?.netrunnerName?.uppercase() ?: "UNKNOWN"}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "RANK_0${character?.level ?: 1}",
                    color = Color(0xFFFF006E),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Right: Status Cluster
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.End
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    color = systemColor,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${weatherState.temperature}°",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (liveMetrics?.heartRate != null && liveMetrics.heartRate!! > 0) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = Color(0xFFFF006E),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))
            
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(28.dp).padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Adjust,
                    contentDescription = "JACK IN",
                    tint = Color(0xFFFF006E),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}


@Composable
fun HudBracket(modifier: Modifier = Modifier, color: Color, rotate: Float = 0f) {
    Canvas(modifier = modifier.size(8.dp).graphicsLayer { rotationZ = rotate }) {
        drawLine(color, Offset(0f, 0f), Offset(size.width, 0f), 2.dp.toPx())
        drawLine(color, Offset(0f, 0f), Offset(0f, size.height), 2.dp.toPx())
    }
}

@Composable
fun NeuralBriefCard(
    insight: String,
    neuralLoad: Float,
    primaryActionTask: AscensionTask?,
    onActionClick: (String) -> Unit,
    onWorkoutClick: (String) -> Unit,
    systemColor: Color
) {
    val isWorkout = primaryActionTask?.tags?.any { 
        it.contains("workout", ignoreCase = true) || it.contains("lift", ignoreCase = true) 
    } == true
    
    CyberCutFrame(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TODAY'S INTELLIGENCE // NEURAL_BRIEF /",
                        color = Color(0xFF00CCFF),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                    Text(
                        text = "Insight: $insight",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontStyle = FontStyle.Italic,
                        lineHeight = 18.sp
                    )
                }
                
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(start = 16.dp)) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        drawArc(
                            color = Color.White.copy(alpha = 0.1f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx())
                        )
                        drawArc(
                            color = systemColor,
                            startAngle = -90f,
                            sweepAngle = neuralLoad * 360f,
                            useCenter = false,
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        "${(neuralLoad * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (primaryActionTask != null) {
                Spacer(Modifier.height(24.dp))
                val buttonText = if (isWorkout) {
                    "LOG WORKOUT // ${primaryActionTask.title.uppercase()}"
                } else {
                    "COMPLETE PULSE // ${primaryActionTask.title.uppercase()}"
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .border(1.dp, systemColor, CyberCutShape)
                        .clickable { 
                            if (isWorkout) onWorkoutClick(primaryActionTask.id)
                            else onActionClick(primaryActionTask.id) 
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = buttonText,
                        color = systemColor,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun HeroPulseRow(
    task: AscensionTask,
    onComplete: () -> Unit,
    systemColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.1f), CyberCutShape)
            .background(Color.Black.copy(alpha = 0.4f), CyberCutShape)
            .padding(vertical = 16.dp, horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp).clickable { onComplete() }
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = "DIURNAL_PULSE • ${task.title.uppercase()}",
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "AM",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 14.dp)
        )
        Text(
            text = "LOG >>",
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MetricPillRow(
    steps: Int,
    kcal: Int,
    streak: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricPill(
            label = "STEPS",
            value = if (steps > 1000) String.format("%.1fK", steps / 1000f) else steps.toString(),
            color = Color(0xFF00CCFF),
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            modifier = Modifier.weight(1f).height(86.dp)
        )
        MetricPill(
            label = "KCAL",
            value = if (kcal > 1000) String.format("%.1fK", kcal / 1000f) else kcal.toString(),
            color = Color(0xFFFF8C00),
            icon = Icons.Default.LocalFireDepartment,
            modifier = Modifier.weight(1f).height(86.dp)
        )
        MetricPill(
            label = "STREAK",
            value = streak.toString(),
            color = Color(0xFF00FF9C),
            icon = Icons.Default.Whatshot,
            modifier = Modifier.weight(1f).height(86.dp)
        )
    }
}

@Composable
fun MetricPill(label: String, value: String, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.4f), CyberCutShape)
            .background(Color.Black.copy(alpha = 0.2f), CyberCutShape)
            .padding(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(value, color = color, fontSize = 22.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
    }
}

@Composable
fun MissionChipRow(missions: List<AscensionMission>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "MISSION_CHIPS //",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(end = 8.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            missions.forEach { mission ->
                val chipColor = if (mission.status == AscensionMissionStatus.RECOVERY) Color(0xFFFF8C00) else Color(0xFFFF006E)
                Box(
                    modifier = Modifier
                        .border(1.dp, chipColor.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${mission.title.uppercase()} ${(mission.progress * 100).toInt()}%",
                        color = chipColor,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (missions.size > 2) {
                Text(
                    "+${missions.size - 2}",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    healthViewModel: HealthViewModel = hiltViewModel(),
    onAvatarClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onNavigateToWorkout: (String?) -> Unit = {},
    onNavigateToGuide: () -> Unit = {}
) {
    val userCharacter by viewModel.userCharacter.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val systemAdvice by viewModel.systemAdvice.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val liveMetrics by healthViewModel.liveMetrics.collectAsState()
    val currentTime = remember { mutableStateOf(LocalDateTime.now()) }
    
    val neuralLoad = userCharacter?.neuralLoad ?: 0.2f
    
    val systemColor by animateColorAsState(
        targetValue = if (neuralLoad > 0.7f) Color(0xFFFF006E) else Color(0xFF00FF9C),
        animationSpec = tween(500),
        label = "SystemColor"
    )

    var isTerminalExpanded by rememberSaveable { mutableStateOf(false) }
    var showQuickTaskSheet by remember { mutableStateOf(false) }
    var showAllTasksDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(1000)
            currentTime.value = LocalDateTime.now()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF010101))) {
        SoftGridBackground()
        Vignette()
        
        HudCornerAccents(color = systemColor.copy(alpha = 0.1f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            
            SlimChromeHeader(
                character = userCharacter,
                identity = state.identity,
                weatherState = weatherState,
                liveMetrics = liveMetrics,
                currentTime = currentTime.value,
                onAvatarClick = onAvatarClick,
                onSettingsClick = onSettingsClick,
                systemColor = systemColor
            )

            Spacer(Modifier.height(24.dp))

            NeuralBriefCard(
                insight = systemAdvice,
                neuralLoad = neuralLoad,
                primaryActionTask = state.todayPulses.firstOrNull(),
                onActionClick = { id -> viewModel.completePulse(id) },
                onWorkoutClick = { id -> onNavigateToWorkout(id) },
                systemColor = systemColor
            )

            Spacer(Modifier.height(24.dp))

            state.todayPulses.firstOrNull()?.let { heroTask ->
                HeroPulseRow(
                    task = heroTask,
                    onComplete = { viewModel.completePulse(heroTask.id) },
                    systemColor = systemColor
                )
                Spacer(Modifier.height(24.dp))
            }

            MetricPillRow(
                steps = liveMetrics?.stepsToday?.toInt() ?: 0,
                kcal = liveMetrics?.caloriesToday?.toInt() ?: 0,
                streak = state.totalHabitDays
            )

            Spacer(Modifier.height(24.dp))

            if (state.activeMissions.isNotEmpty()) {
                MissionChipRow(missions = state.activeMissions)
                Spacer(Modifier.height(24.dp))
            }

            AiTerminal(
                messages = state.terminalMessages,
                inputValue = state.terminalInput,
                onInputChange = viewModel::updateTerminalInput,
                onSend = viewModel::sendTerminalMessage,
                isExpanded = isTerminalExpanded,
                onToggleExpand = { isTerminalExpanded = !isTerminalExpanded },
                accentColor = systemColor,
                onGuideClick = onNavigateToGuide
            )

            Spacer(Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Manual Session Log Chip
                Box(
                    modifier = Modifier
                        .border(1.dp, systemColor.copy(alpha = 0.6f), CircleShape)
                        .clickable { onNavigateToWorkout(null) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⚡ LOG SESSION",
                        color = systemColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(Modifier.width(12.dp))

                SmallFloatingActionButton(
                    onClick = { viewModel.generateTodaysTasks() },
                    containerColor = Color.Transparent,
                    contentColor = systemColor,
                    modifier = Modifier.size(40.dp).border(1.dp, systemColor.copy(alpha = 0.6f), CircleShape),
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "RECALIBRATE_TASKS", modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(16.dp))
                FloatingActionButton(
                    onClick = { showQuickTaskSheet = true },
                    containerColor = Color.Transparent,
                    contentColor = systemColor,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp).border(1.dp, systemColor, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "QUICK_TASK_CREATION", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(100.dp)) // Padding for bottom bar
        }

        CelebrationOverlay(
            event = state.dopamineEvent,
            onFinished = { viewModel.clearDopamineEvent() }
        )
    }

    if (showQuickTaskSheet) {
        QuickTaskBottomSheet(
            onDismiss = { showQuickTaskSheet = false }
        )
    }

    if (showAllTasksDialog) {
        AllTasksDialog(
            tasks = state.todayPulses,
            onComplete = { viewModel.completePulse(it) },
            onTaskClick = { onTaskClick(it) },
            onNavigateToWorkout = onNavigateToWorkout,
            onDismiss = { showAllTasksDialog = false },
            systemColor = systemColor
        )
    }
}

@Composable
fun AiTerminal(
    messages: List<TerminalMessage>,
    inputValue: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    accentColor: Color,
    onGuideClick: () -> Unit = {}
) {
    CyberCutFrame(
        modifier = if (!isExpanded) Modifier.clickable { onToggleExpand() } else Modifier,
        borderColor = accentColor.copy(alpha = 0.2f)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Terminal indicator dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(Color(0xFF00F5FF), CircleShape)
                    .align(Alignment.TopEnd)
            )

            Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Text(
                    text = "AI_TERMINAL // CYBR-TES",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                if (isExpanded) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "CONNECTION_SECURE",
                                color = accentColor.copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "[NEON_GUIDE]",
                                color = Color(0xFF00FFFF),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .clickable { onGuideClick() }
                                    .padding(6.dp)
                            )
                        }
                        Text(
                            "[MINIMIZE]",
                            color = accentColor.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onToggleExpand() }
                                .padding(6.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(10.dp)
                    ) {
                        messages.forEach { msg ->
                            Text(
                                text = if (msg.isFromUser) "> ${msg.text}" else "CYBR-TES: ${msg.text}",
                                color = if (msg.isFromUser) Color.White else accentColor,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                            .background(Color.Black.copy(alpha = 0.4f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = inputValue,
                            onValueChange = onInputChange,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.weight(1f).padding(10.dp),
                            cursorBrush = SolidColor(accentColor),
                            decorationBox = { innerTextField ->
                                if (inputValue.isEmpty()) {
                                    Text(
                                        "ENTER_COMMAND...",
                                        color = Color.White.copy(alpha = 0.3f),
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                innerTextField()
                            }
                        )
                        IconButton(onClick = onSend) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "SEND",
                                tint = accentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = messages.lastOrNull()?.text ?: "WAITING_FOR_INPUT...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AllTasksDialog(
    tasks: List<AscensionTask>,
    onComplete: (String) -> Unit,
    onTaskClick: (String) -> Unit,
    onNavigateToWorkout: (String?) -> Unit,
    onDismiss: () -> Unit,
    systemColor: Color
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF0F0F0F))
                .border(2.dp, systemColor, RoundedCornerShape(8.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
            ) {
                Text(
                    "DIURNAL_PULSES",
                    color = systemColor,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tasks.forEach { task ->
                        val isWorkout = task.tags.any { it.contains("workout", ignoreCase = true) || it.contains("lift", ignoreCase = true) }
                        DashboardTaskItem(
                            task = task,
                            onComplete = { onComplete(task.id) },
                            onClick = { 
                                if (isWorkout) onNavigateToWorkout(task.id)
                                else onTaskClick(task.id)
                            },
                            isWorkout = isWorkout
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = systemColor)
                ) {
                    Text("CLOSE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DashboardTaskItem(task: AscensionTask, onComplete: () -> Unit, onClick: () -> Unit, isWorkout: Boolean = false) {
    val isCompleted = task.lastCompleted != null && 
        task.lastCompleted!!.atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, if (isWorkout) Color(0xFF00CCFF).copy(alpha = 0.5f) else if (isCompleted) Color(0xFF00FF9C).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
            .clickable { if (!isCompleted) onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { if (!isCompleted) onComplete() },
            modifier = Modifier.size(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(1.dp, if (isCompleted) Color(0xFF00FF9C) else Color.White.copy(alpha = 0.4f))
                    .background(if (isCompleted) Color(0xFF00FF9C).copy(alpha = 0.2f) else Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFF00FF9C), modifier = Modifier.size(12.dp))
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val displayTitle = if (task.timeWindows.isNotEmpty()) {
                "${task.title} (${task.timeWindows.first()})"
            } else {
                task.title
            }
            
            Text(
                displayTitle,
                color = if (isCompleted) Color.Gray else Color.White,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                )
            )
            if (isWorkout && !isCompleted) {
                Text(
                    "LOG SESSION >>",
                    color = Color(0xFF00CCFF),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DashboardMissionCardV3(mission: AscensionMission, accentColor: Color = Color(0xFFFF006E)) {
    CyberFrame(
        label = "MISSION // ${mission.title.uppercase()}",
        borderColor = accentColor.copy(alpha = 0.4f)
    ) {
        Column {
            Text(
                mission.description,
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "STATUS: ${mission.status}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                )
                Text(
                    "${(mission.progress * 100).toInt()}%",
                    color = accentColor,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(mission.progress)
                        .fillMaxHeight()
                        .background(accentColor)
                )
            }
        }
    }
}

@Composable
fun BioAgeDashboardCard(result: com.neon.ascent.model.BioAgeResult, modifier: Modifier = Modifier) {
    CyberFrame(
        label = "BIOLOGICAL_AGE_SCAN",
        accentColor = Color(0xFF00FFFF),
        borderColor = Color(0xFF00FFFF).copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(java.util.Locale.US, "%.1f", result.biologicalAge),
                    color = Color(0xFF00FFFF),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "YEARS_OLD",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
            Text(
                result.explanation.take(100) + "...",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
