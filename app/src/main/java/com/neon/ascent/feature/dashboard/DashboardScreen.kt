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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.feature.settings.SettingsViewModel
import com.neon.ascent.feature.health.ui.HealthViewModel
import com.neon.ascent.feature.biohacking.BiohackingViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.goals.models.*
import com.neon.ascent.feature.goals.ui.ascension.QuickTaskBottomSheet
import com.neon.ascent.core.common.CelebrationOverlay
import com.neon.ascent.ui.*
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ScrollingMicroLogs(messages: List<String>, modifier: Modifier = Modifier) {
    val logs = if (messages.isNotEmpty()) messages else listOf(
        "NEURAL_LINK_STABLE",
        "SYNC_RATIO: 98.4%",
        "BIOMETRIC_FEED_ACTIVE",
        "CALIBRATING_AGILITY_NODE",
        "STRUCTURAL_INTEGRITY: OPTIMAL",
        "TRACING_PACKETS...",
        "DECRYPTING_DESTINY..."
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "MicroLogs")
    val scrollY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(logs.size * 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Scroll"
    )

    Box(modifier = modifier.height(40.dp).clipToBounds()) {
        Column(modifier = Modifier.graphicsLayer { translationY = -scrollY * (logs.size * 20f) }) {
            (logs + logs).forEach { log ->
                Text(
                    text = "> $log",
                    color = Color(0xFF00FF9C).copy(alpha = 0.4f),
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    healthViewModel: HealthViewModel = hiltViewModel(),
    biohackingViewModel: BiohackingViewModel = hiltViewModel(),
    onAvatarClick: () -> Unit,
    onAttributeSetClick: () -> Unit,
    onStoryClick: () -> Unit,
    onGoalSetClick: () -> Unit,
    onTaskClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onDeusExMachinaClick: () -> Unit
) {
    val userCharacter by viewModel.userCharacter.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val biohackingData by viewModel.biohackingData.collectAsState()
    val systemAdvice by viewModel.systemAdvice.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val isReligionShortcutEnabled by settingsViewModel.isReligionShortcutEnabled.collectAsState()
    val liveMetrics by healthViewModel.liveMetrics.collectAsState()
    val terminalFeed by biohackingViewModel.terminalFeed.collectAsState()
    val currentTime = remember { mutableStateOf(LocalDateTime.now()) }
    
    val neuralLoad = userCharacter?.neuralLoad ?: 0.2f
    
    val systemColor by animateColorAsState(
        targetValue = if (neuralLoad > 0.7f) Color(0xFFFF006E) else Color(0xFF00FF9C),
        animationSpec = tween(500),
        label = "SystemColor"
    )

    var glitchTrigger by remember { mutableIntStateOf(0) }
    
    val triggerGlitch = {
        glitchTrigger++
    }

    var isTerminalExpanded by rememberSaveable { mutableStateOf(false) }
    var showQuickTaskSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(1000)
            currentTime.value = LocalDateTime.now()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF010101))) {
        PerspectiveGrid()
        Scanlines(intensity = neuralLoad)
        StaticNoise(intensity = neuralLoad)
        Vignette()
        FloatingParticles(intensity = neuralLoad)
        
        if (weatherState.isNight) {
            NightCityGlow()
        }
        
        val activeGlitchIntensity = if (glitchTrigger > 0) (neuralLoad + 0.4f).coerceAtMost(1f) else neuralLoad
        GlitchOverlay(intensity = activeGlitchIntensity)

        if (glitchTrigger > 0) {
            LaunchedEffect(glitchTrigger) {
                kotlinx.coroutines.delay(200)
                glitchTrigger = 0
            }
        }

        if (weatherState.isRaining) {
            AcidRainOverlay()
        }
        
        HudCornerAccents(color = systemColor.copy(alpha = 0.2f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- TOP HUD BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(contentAlignment = Alignment.Center) {
                    ResonanceAura(
                        color = state.identity.resonance.getColor(),
                        intensity = state.identity.resonanceIntensity,
                        modifier = Modifier.size(120.dp)
                    )
                    
                    if (userCharacter?.holyGhost != null) {
                        HolyGhostAura()
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CyberButtonShape)
                            .cyberGlitch(intensity = if (neuralLoad > 0.8f) 0.3f else 0f)
                            .neonBorder(state.identity.resonance.getColor(), width = 2.dp, cornerRadius = 12.dp)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onAvatarClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().clip(RectangleShape),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            AvatarImage(
                                character = userCharacter, 
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer(
                                        scaleX = 4.5f,
                                        scaleY = 4.5f,
                                        translationY = 80f 
                                    ),
                                alpha = 1f,
                                contentScale = ContentScale.FillWidth,
                                alignment = Alignment.TopCenter
                            )
                        }
                        
                        Text(
                            text = "SYNC", 
                            color = systemColor.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            ),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)
                    )
                }
                
                ScrollingMicroLogs(
                    messages = state.recentLogMessages,
                    modifier = Modifier
                        .padding(top = 100.dp)
                        .width(120.dp)
                )
            }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isReligionShortcutEnabled) {
                            CyberCrossIcon(onClick = onDeusExMachinaClick)
                            Spacer(Modifier.width(16.dp))
                        }
                        NeuralJackIcon(onClick = onSettingsClick)
                        Spacer(Modifier.width(16.dp))
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = currentTime.value.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                                color = systemColor,
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    shadow = Shadow(color = systemColor.copy(alpha = 0.6f), blurRadius = 10f)
                                ),
                                modifier = Modifier.cyberGlitch(intensity = if (neuralLoad > 0.9f) 0.2f else 0f)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (liveMetrics?.heartRate != null && liveMetrics!!.heartRate!! > 0) {
                                    HeartRatePulse(liveMetrics!!.heartRate!!)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (weatherState.isRaining) Color(0xFF00FFFF) else systemColor, CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${weatherState.temperature}°${weatherState.unitSymbol}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }
                    }
                    Text(
                        text = currentTime.value.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                        color = Color(0xFFFF006E),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.identity.title,
                        color = state.identity.resonance.getColor(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "RANK_0${userCharacter?.level ?: 1}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    )
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(4.dp)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .fillMaxHeight()
                                .background(systemColor)
                                .neonBorder(systemColor, width = 1.dp, glowIntensity = 1f, cornerRadius = 0.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .neonBorder(systemColor.copy(alpha = 0.4f), cornerRadius = 12.dp),
                color = Color.Black.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    NeuralLoadGauge(
                        load = neuralLoad,
                        modifier = Modifier.size(110.dp)
                    )
                    
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (biohackingData?.enableOnDeviceNeuralCore == true) "NEURAL_CORE_UTILIZATION" else "NEURAL_LINK_INTEGRITY",
                            color = systemColor,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = if (neuralLoad > 0.8f) "CRITICAL_STRESS" else if (neuralLoad > 0.5f) "ELEVATED_LOAD" else "SYNC_STABLE",
                            color = if (neuralLoad > 0.7f) Color(0xFFFF006E) else Color.White,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(Color.Black)
                                .border(1.dp, systemColor.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(neuralLoad)
                                    .fillMaxHeight()
                                    .background(Brush.horizontalGradient(listOf(systemColor.copy(alpha = 0.5f), systemColor)))
                                    .neonBorder(systemColor, width = 1.dp, glowIntensity = 0.5f, cornerRadius = 0.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (state.todayTasks.isNotEmpty()) {
                Text(
                    "PRIORITY_GRIND",
                    color = systemColor,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val displayTasks = state.todayTasks.take(3)
                displayTasks.forEach { task ->
                    DashboardTaskItem(
                        task = task,
                        onComplete = { viewModel.markTaskCompleted(task.id) },
                        onClick = { onTaskClick(task.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                if (state.todayTasks.size > 3) {
                    Text(
                        "+ ${state.todayTasks.size - 3} MORE PROTOCOLS IN QUEUE",
                        color = systemColor.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Could navigate to a full task list if we had one, or just expand */ }
                            .padding(vertical = 4.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            CyberFrame(
                label = "SYSTEM_ADVICE // V.01", 
                accentColor = Color(0xFF00FFFF),
                borderColor = Color(0xFF00FFFF).copy(alpha = 0.6f),
                modifier = Modifier.cyberGlitch(intensity = if (neuralLoad > 0.7f) neuralLoad * 0.5f else 0f)
            ) {
                Text(
                    text = "\"$systemAdvice\"",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AiTerminal(
                messages = state.terminalMessages,
                inputValue = state.terminalInput,
                onInputChange = viewModel::updateTerminalInput,
                onSend = viewModel::sendTerminalMessage,
                isExpanded = isTerminalExpanded,
                onToggleExpand = { isTerminalExpanded = !isTerminalExpanded },
                accentColor = systemColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CyberMetricCard(
                    label = "STEPS", 
                    value = (liveMetrics?.stepsToday ?: 0).toString(),
                    subValue = "TARGET: 10K", 
                    color = NeonBlue,
                    modifier = Modifier.weight(1f)
                )
                CyberMetricCard(
                    label = "CALORIES", 
                    value = (liveMetrics?.caloriesToday ?: 0.0).toInt().toString(),
                    subValue = "TARGET: 2.2K", 
                    color = NeonOrange,
                    modifier = Modifier.weight(1f)
                )
                CyberMetricCard(
                    label = "CONSISTENCY", 
                    value = "${state.totalHabitDays}", 
                    subValue = "DAYS 🔥", 
                    color = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            CyberFrame(label = "CORE_SYNC", borderColor = systemColor) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (userCharacter?.strength == null) {
                        Text(
                            text = "BIOMETRIC_INTERFACE_READY",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )

                        CyberActionButton("ATTRIBUTE SCAN", Color(0xFF00FF9C), onClick = {
                            triggerGlitch()
                            onAttributeSetClick()
                        })
                    }
                    if (state.userStory.bio.isBlank()) {
                        CyberActionButton("YOUR STORY", Color(0xFFFF006E), onClick = {
                            triggerGlitch()
                            onStoryClick()
                        })
                    }
                    CyberActionButton("MISSIONS", Color.White, onClick = { 
                        triggerGlitch()
                        onGoalSetClick() 
                    })
                }
            }

            if (state.activeMissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // Show Recovery Missions first
                val recoveryMissions = state.activeMissions.filter { it.status == com.neon.ascent.core.domain.goals.models.AscensionMissionStatus.RECOVERY }
                if (recoveryMissions.isNotEmpty()) {
                    Text(
                        "RECOVERY_PROTOCOLS",
                        color = NeonOrange,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    recoveryMissions.forEach { mission ->
                        DashboardMissionCardV3(mission, accentColor = NeonOrange)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                val standardMissions = state.activeMissions.filter { it.status != com.neon.ascent.core.domain.goals.models.AscensionMissionStatus.RECOVERY }
                if (standardMissions.isNotEmpty()) {
                    Text(
                        "ACTIVE_MISSIONS",
                        color = Color(0xFFFF006E),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    standardMissions.forEach { mission ->
                        DashboardMissionCardV3(mission)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            if (state.bioAgeResult != null) {
                Spacer(modifier = Modifier.height(32.dp))
                BioAgeDashboardCard(state.bioAgeResult!!)
            }

            Spacer(modifier = Modifier.height(64.dp))
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            SmallFloatingActionButton(
                onClick = { viewModel.generateTodaysTasks() },
                containerColor = Color.Black.copy(alpha = 0.6f),
                contentColor = systemColor,
                modifier = Modifier
                    .neonBorder(systemColor.copy(alpha = 0.6f), cornerRadius = 8.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "RECALIBRATE_TASKS")
            }

            FloatingActionButton(
                onClick = { showQuickTaskSheet = true },
                containerColor = systemColor,
                contentColor = Color.Black,
                modifier = Modifier
                    .neonBorder(systemColor, cornerRadius = 16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "QUICK_TASK_CREATION")
            }
        }

        if (showQuickTaskSheet) {
            QuickTaskBottomSheet(
                onDismiss = { showQuickTaskSheet = false }
            )
        }

        CelebrationOverlay(
            event = state.dopamineEvent,
            onFinished = { viewModel.clearDopamineEvent() }
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
    accentColor: Color
) {
    CyberFrame(
        label = "AI_TERMINAL // CYBR-TES", 
        borderColor = accentColor,
        modifier = if (!isExpanded) Modifier.clickable { onToggleExpand() } else Modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
            if (isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CONNECTION_SECURE",
                        color = accentColor.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "[MINIMIZE]",
                        color = accentColor.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onToggleExpand() }
                            .padding(4.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    messages.forEach { msg ->
                        Text(
                            text = if (msg.isFromUser) "> ${msg.text}" else "CYBR-TES: ${msg.text}",
                            color = if (msg.isFromUser) Color.White else accentColor,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.4f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = inputValue,
                        onValueChange = onInputChange,
                        textStyle = TextStyle(
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        ),
                        modifier = Modifier.weight(1f).padding(8.dp),
                        cursorBrush = SolidColor(accentColor),
                        decorationBox = { innerTextField ->
                            if (inputValue.isEmpty()) {
                                Text(
                                    "ENTER_COMMAND...",
                                    color = Color.White.copy(alpha = 0.3f),
                                    fontSize = 12.sp,
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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = messages.lastOrNull()?.text ?: "WAITING_FOR_INPUT...",
                    color = accentColor.copy(alpha = 0.6f),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun DashboardTaskItem(task: AscensionTask, onComplete: () -> Unit, onClick: () -> Unit) {
    val isCompleted = task.lastCompleted != null && 
        task.lastCompleted!!.atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, if (isCompleted) Color(0xFF00FF9C).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f))
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
        Text(
            task.title,
            color = if (isCompleted) Color.Gray else Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                textDecoration = if (isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
            )
        )
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
fun BioAgeDashboardCard(result: com.neon.ascent.model.BioAgeResult) {
    CyberFrame(
        label = "BIOLOGICAL_AGE_SCAN",
        accentColor = Color(0xFF00FFFF),
        borderColor = Color(0xFF00FFFF).copy(alpha = 0.6f)
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
