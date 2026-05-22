package com.neon.ascent.feature.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.scale
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.core.common.*
import com.neon.ascent.feature.health.ui.HealthViewModel
import com.neon.ascent.feature.terminal.ui.TerminalViewModel
import com.neon.ascent.model.UserCharacter
import com.neon.ascent.domain.model.UserStory
import com.neon.ascent.ui.*
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun HolographicAvatarHub(
    viewModel: DashboardViewModel = hiltViewModel(),
    terminalViewModel: TerminalViewModel = hiltViewModel(),
    healthViewModel: HealthViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onUpgradeClick: (String) -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onLoreClick: () -> Unit = {}
) {
    val userCharacter by viewModel.userCharacter.collectAsState()
    val state by viewModel.uiState.collectAsState()
    val specialState by terminalViewModel.specialState.collectAsState()
    val healthState by viewModel.healthState.collectAsState()
    val liveMetrics by healthViewModel.liveMetrics.collectAsState()
    val isNetrunnerMode by viewModel.isNetrunnerMode.collectAsState()
    val isReligionEnabled by viewModel.isReligionShortcutEnabled.collectAsState()

    var selectedBodyPart by remember { mutableStateOf<String?>(null) }
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var showSnapshotPreview by remember { mutableStateOf(false) }

    // Instant glitch burst state for interactive feedback
    var glitchBurstIntensity by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(glitchBurstIntensity) {
        if (glitchBurstIntensity > 0f) {
            delay(150)
            glitchBurstIntensity = 0f
        }
    }

    val neuralLoad = userCharacter?.neuralLoad ?: 0.2f
    val displayLoad = (neuralLoad + glitchBurstIntensity).coerceIn(0f, 1f)

    val systemLogs = remember {
        mutableStateListOf(
            "INITIALIZING NEURAL_LINK...",
            "AVATAR_HOLOGRAPH_STABLE",
            "BIOMETRIC_DATA_LOADED"
        )
    }

    LaunchedEffect(userCharacter?.netrunnerName) {
        editedName = userCharacter?.netrunnerName ?: "RUNNER_UNKNOWN"
    }

    // Dynamic Title Logic
    // Neuromancer triggers ONLY if Holy Ghost is active AND Netrunner Mode is active AND Religion Overlay is enabled
    val isNeuromancer = userCharacter?.holyGhost == 1 && isNetrunnerMode && isReligionEnabled
    val runnerTitle = when {
        isNeuromancer -> "NEUROMANCER"
        isNetrunnerMode -> "NETRUNNER"
        else -> userCharacter?.archetype ?: "OPERATIVE"
    }

    val titleColor = if (isNeuromancer) Color(0xFFFF006E) else Color(0xFF00FF9C).copy(alpha = 0.6f)
    val titleFontWeight = if (isNeuromancer) FontWeight.ExtraBold else FontWeight.Normal

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020202))) {
        PerspectiveGrid()
        Scanlines()
        StaticNoise(intensity = displayLoad)
        Vignette()
        FloatingParticles(intensity = displayLoad)
        GlitchOverlay(intensity = displayLoad)
        HudCornerAccents(color = Color(0xFF00FF9C).copy(alpha = 0.2f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            // --- TOP HUD BAR ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .neonBorder(Color(0xFF00FF9C).copy(alpha = 0.4f), cornerRadius = 8.dp),
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FF9C))
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .neonBorder(Color(0xFF00FF9C), cornerRadius = 22.dp)
                    ) {
                        AvatarImage(userCharacter, modifier = Modifier.fillMaxSize(), alpha = 1f)
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        if (isEditingName) {
                            OutlinedTextField(
                                value = editedName,
                                onValueChange = { editedName = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00FF9C),
                                    unfocusedBorderColor = Color(0xFF00FF9C).copy(alpha = 0.5f),
                                    focusedTextColor = Color.White
                                ),
                                textStyle = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                                trailingIcon = {
                                    TextButton(onClick = { 
                                        viewModel.updateNetrunnerName(editedName)
                                        isEditingName = false 
                                    }) {
                                        Text("SYNC", color = Color(0xFF00FF9C), fontSize = 10.sp)
                                    }
                                }
                            )
                        } else {
                            Text(
                                text = "//${userCharacter?.netrunnerName ?: "RUNNER_UNKNOWN"}",
                                modifier = Modifier.clickable { isEditingName = true },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0xFF00FF9C),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                            )
                        }
                        Text(
                            text = "LEVEL ${userCharacter?.level ?: 1} $runnerTitle // SEC_ID: 0x${(userCharacter?.id ?: 0).toString(16).uppercase()}",
                            color = titleColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = titleFontWeight
                        )
                    }
                    
                    Text(
                        text = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        color = Color(0xFF00FF9C),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // --- CENTRAL Area: Avatar Display ---
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .neonBorder(Color(0xFF00FF9C).copy(alpha = 0.2f), cornerRadius = 12.dp)
                    .background(Color.Black.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                HologramDisplay(userCharacter) { part ->
                    selectedBodyPart = part
                    systemLogs.add(0, "[LOG] SECTOR_ACCESS: $part")
                    glitchBurstIntensity = 0.3f
                }
                
                // Live HR Badge (Top Right)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    LiveHeartRateBadge(
                        bpm = liveMetrics.heartRate,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    HeartbeatTrace(
                        bpm = liveMetrics.heartRate,
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp)
                            .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    )
                }

                // Load Gauge Overlay (Small/Minimal)
                Box(modifier = Modifier.align(Alignment.CenterStart).padding(16.dp)) {
                    NeuralLoadGauge(load = displayLoad, modifier = Modifier.size(100.dp))
                }

                // Archetype Label
                Text(
                    text = runnerTitle,
                    color = titleColor.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = titleFontWeight,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                )

                // Share Snapshot Button
                IconButton(
                    onClick = { showSnapshotPreview = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).border(1.dp, Color(0xFF00FFFF).copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF00FFFF), modifier = Modifier.size(20.dp))
                }
            }

            // --- S.P.E.C.I.A.L. BIOMETRICS ---
            CyberFrame(label = "S.P.E.C.I.A.L._BIO_METRICS", modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val attributes = com.neon.ascent.core.domain.model.SpecialType.entries

                        attributes.forEach { type ->
                            val attr = specialState[type]
                            val value = attr?.currentValue ?: 5
                            Text(
                                text = "[${type.name[0]}] ${type.name.padEnd(12)}: $value",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUpgradeClick(type.name) }
                                    .padding(vertical = 1.dp)
                            )
                        }
                        
                        val rank = userCharacter?.getChessRank() ?: "GHOST_IN_SHELL"
                        Text(
                            "RANK: $rank // HR: ${liveMetrics.heartRate ?: healthState.heartRate} BPM",
                            color = Color(0xFF00FF9C).copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (selectedBodyPart != null) {
                            Button(
                                onClick = { onUpgradeClick(selectedBodyPart!!) },
                                modifier = Modifier.padding(top = 8.dp).height(28.dp).clip(CyberButtonShape),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("UPGRADE_${selectedBodyPart}", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    AttributeRadarChart(
                        stats = mapOf(
                            "STR" to (specialState[com.neon.ascent.core.domain.model.SpecialType.STRENGTH]?.currentValue ?: 5),
                            "AGI" to (specialState[com.neon.ascent.core.domain.model.SpecialType.AGILITY]?.currentValue ?: 5),
                            "END" to (specialState[com.neon.ascent.core.domain.model.SpecialType.ENDURANCE]?.currentValue ?: 5),
                            "PER" to (specialState[com.neon.ascent.core.domain.model.SpecialType.PERCEPTION]?.currentValue ?: 5),
                            "INT" to (specialState[com.neon.ascent.core.domain.model.SpecialType.INTELLIGENCE]?.currentValue ?: 5),
                            "CHA" to (specialState[com.neon.ascent.core.domain.model.SpecialType.CHARISMA]?.currentValue ?: 5),
                            "LUC" to (specialState[com.neon.ascent.core.domain.model.SpecialType.LUCK]?.currentValue ?: 5)
                        ),
                        modifier = Modifier.size(150.dp)
                    )
                }
            }

            // Cyber Lore Section
            if (state.userStory.bio.isNotBlank() && state.cyberLoreSnippet.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                CyberFrame(
                    label = "CYBER_LORE // PROTOCOL_LOG",
                    accentColor = Color(0xFFFF006E),
                    borderColor = Color(0xFFFF006E).copy(alpha = 0.6f),
                    modifier = Modifier.clickable { onLoreClick() }
                ) {
                    Text(
                        text = state.cyberLoreSnippet,
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Intelligence Test & Neural Archive Buttons
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onNavigateToDiagnostics,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .neonBorder(NeonRed, cornerRadius = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NeonRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "SYSTEM DIAGNOSTICS",
                        color = NeonRed,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // --- SYSTEM STATS & TERMINAL ---
            Row(modifier = Modifier.weight(0.6f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CyberFrame(label = "SYSTEM_LOAD", modifier = Modifier.weight(0.8f)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EnergyBar(label = "BATTERY", value = healthState.bodyBattery / 100f)
                        MemorySlotsDisplay(userCharacter)
                    }
                }
                
                CyberFrame(label = "TERMINAL_OUTPUT", accentColor = Color.Gray, modifier = Modifier.weight(1.2f)) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(systemLogs.take(5)) { log ->
                            Text(
                                text = "> $log",
                                color = Color(0xFF00FF9C).copy(alpha = 0.8f),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSnapshotPreview) {
        SnapshotPreviewDialog(userCharacter, viewModel) {
            showSnapshotPreview = false
        }
    }
}

@Composable
fun EnergyBar(label: String, value: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFF00FF9C), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            Text("${(value * 100).toInt()}%", color = Color.White, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(Color.Black)
                .border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF00FF9C).copy(alpha = 0.5f), Color(0xFF00FF9C))))
            )
        }
    }
}

@Composable
fun MemorySlotsDisplay(character: UserCharacter?) {
    val totalRam = character?.ramSlots ?: 8
    val usedRam = character?.usedRam ?: 0
    val totalHacks = character?.quickhackSlots ?: 4
    val loadedHacks = character?.getQuickhackList() ?: emptyList()

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Text("CYBERDECK_RAM", color = Color(0xFF00FFFF), fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(totalRam) { i ->
                Box(
                    modifier = Modifier
                        .size(8.dp, 14.dp)
                        .border(1.dp, if (i < usedRam) Color(0xFFFF006E) else Color(0xFF00FFFF))
                        .background(if (i < usedRam) Color(0xFFFF006E).copy(alpha = 0.6f) else Color.Transparent)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text("ACTIVE_QUICKHACKS", color = Color(0xFF00FF9C), fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(totalHacks) { i ->
                val hackName = loadedHacks.getOrNull(i)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .border(1.dp, if (hackName != null) Color(0xFF00FF9C) else Color.Gray.copy(alpha = 0.4f))
                            .background(if (hackName != null) Color(0xFF00FF9C) else Color.Transparent)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = hackName ?: "EMPTY_SLOT",
                        color = if (hackName != null) Color.White else Color.Gray.copy(alpha = 0.5f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun HolographicAdvicePanel(message: String, intensity: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color(0xFF00FF9C).copy(alpha = 0.05f))
            .border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.2f))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color(0xFF00FF9C),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.cyberGlitch(intensity = intensity * 0.2f)
        )
    }
}

@Composable
fun HologramDisplay(character: UserCharacter?, onPartClick: (String) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "HologramAnim")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ScanLine"
    )

    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Flicker"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = flickerAlpha }) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF00FF9C).copy(alpha = 0.15f), Color.Transparent),
                    center = center,
                    radius = size.minDimension / 1.5f
                )
            )

            val y = size.height * scanY
            drawLine(
                color = Color(0xFF00FF9C).copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxHeight(0.85f)
                .fillMaxWidth(0.8f)
                .neonBorder(Color(0xFF00FF9C).copy(alpha = 0.2f), cornerRadius = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            AvatarImage(character, modifier = Modifier.fillMaxSize())
            
            // Equipped Cyberware Indicators
            val equipped = character?.getEquippedList() ?: emptyList()
            equipped.forEach { item ->
                CyberwareLabelOverlay(item)
            }
            
            Column(modifier = Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().clickable { onPartClick("HEAD") })
                Box(Modifier.weight(2f).fillMaxWidth().clickable { onPartClick("TORSO") })
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    Box(Modifier.weight(1f).fillMaxHeight().clickable { onPartClick("ARMS") })
                    Box(Modifier.weight(1f).fillMaxHeight().clickable { onPartClick("ARMS") })
                }
                Box(Modifier.weight(2f).fillMaxWidth().clickable { onPartClick("LEGS") })
            }
        }
    }
}

@Composable
fun CyberwareLabelOverlay(item: String) {
    val alignment = when {
        item.contains("NEURAL") || item.contains("KIROSHI") || item.contains("SYNAPTIC") -> Alignment.TopStart
        item.contains("HEART") || item.contains("LUNGS") || item.contains("CORE") -> Alignment.CenterEnd
        item.contains("ARM") || item.contains("HAND") -> Alignment.CenterStart
        else -> Alignment.BottomEnd
    }
    
    Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = alignment) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(4.dp).background(Color(0xFF00FFFF)))
            Spacer(Modifier.width(4.dp))
            Text(
                text = item,
                color = Color(0xFF00FFFF),
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.background(Color.Black.copy(alpha = 0.7f)).padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
fun AttributeRadarChart(stats: Map<String, Int>, modifier: Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2
        val sides = stats.size
        val angleStep = (2 * PI / sides).toFloat()
        
        // Background hex
        val bgPath = Path()
        for (i in 0 until sides) {
            val angle = i * angleStep - PI.toFloat() / 2
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)
            if (i == 0) bgPath.moveTo(x, y) else bgPath.lineTo(x, y)
        }
        bgPath.close()
        drawPath(bgPath, Color.White.copy(alpha = 0.05f))
        drawPath(bgPath, Color.White.copy(alpha = 0.1f), style = Stroke(1f))
        
        // Data path
        val dataPath = Path()
        stats.values.forEachIndexed { i, value ->
            val angle = i * angleStep - PI.toFloat() / 2
            val normValue = (value.toFloat() / 10f).coerceIn(0.1f, 1f)
            val x = center.x + radius * normValue * cos(angle)
            val y = center.y + radius * normValue * sin(angle)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(dataPath, Color(0xFF00FF9C).copy(alpha = 0.3f))
        drawPath(dataPath, Color(0xFF00FF9C), style = Stroke(2f))
    }
}

@Composable
fun LiveHeartRateBadge(bpm: Int?, modifier: Modifier = Modifier) {
    val pulseScale by animateFloatAsState(
        targetValue = if (bpm != null && bpm > 60) 1.25f else 1f,
        animationSpec = infiniteRepeatable(tween(480), RepeatMode.Reverse),
        label = "pulse"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = NeonRed,
            modifier = Modifier
                .size(32.dp)
                .scale(pulseScale)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${bpm ?: "--"} BPM",
            style = MaterialTheme.typography.headlineMedium,
            color = NeonRed,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        if (bpm != null) {
            Text(
                " LIVE",
                color = NeonRed.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun HeartbeatTrace(bpm: Int?, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "EKG")
    val xOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "xOffset"
    )

    val baseBpm = bpm?.toFloat() ?: 70f
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val step = 4.dp.toPx()
        
        val path = Path()
        var currentX = 0f
        
        while (currentX < width) {
            val relativeX = (currentX / width + xOffset) % 1f
            val phase = (relativeX * baseBpm / 10f) % 1f
            
            val y = if (phase in 0.1f..0.2f) {
                // The QRS complex spike
                val spikePhase = (phase - 0.1f) / 0.1f
                val spikeY = if (spikePhase < 0.5f) {
                    -height * 0.4f * (spikePhase * 2)
                } else {
                    -height * 0.4f * (1 - (spikePhase - 0.5f) * 2)
                }
                height / 2f + spikeY
            } else if (phase in 0.4f..0.6f) {
                // The T wave
                val tPhase = (phase - 0.4f) / 0.2f
                val tY = -height * 0.15f * sin(tPhase * PI.toFloat())
                height / 2f + tY
            } else {
                height / 2f + (Random.nextFloat() - 0.5f) * 4f // Ambient noise
            }
            
            if (currentX == 0f) path.moveTo(currentX, y) else path.lineTo(currentX, y)
            currentX += step
        }
        
        drawPath(
            path = path,
            color = NeonRed.copy(alpha = 0.8f),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2f, 10f), 0f)
            )
        )
        
        // Leading glow dot
        drawCircle(
            color = NeonRed,
            radius = 3.dp.toPx(),
            center = Offset(width, height / 2f)
        )
    }
}

@Composable
fun SnapshotPreviewDialog(character: UserCharacter?, viewModel: DashboardViewModel, onDismiss: () -> Unit) {
    val snapshotSaying by viewModel.snapshotSaying.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .neonBorder(Color(0xFF00FF9C), cornerRadius = 12.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SNAPSHOT_PREVIEW", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(16.dp))
            
            // "Clean" image with saying overlay
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(Color(0xFF050505))
                    .border(1.dp, Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                AvatarImage(character, modifier = Modifier.fillMaxSize())
                
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = "\"$snapshotSaying\"",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(12.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "@${character?.netrunnerName ?: "RUNNER"} // ${character?.archetype ?: "OPERATIVE"} // NEON_ASCENT",
                        color = Color(0xFF00FF9C),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            TextButton(onClick = { viewModel.refreshSnapshotSaying() }) {
                Text("RE-ROLL SAYING", color = Color(0xFF00FFFF), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).clip(CyberButtonShape),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("CANCEL", color = Color.White, fontSize = 10.sp)
                }
                Button(
                    onClick = { /* Implement sharing logic */ onDismiss() },
                    modifier = Modifier.weight(1f).clip(CyberButtonShape),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
                ) {
                    Text("POST TO X", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
