package com.neon.ascent.feature.cyberdeck

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.ui.CyberFrame
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class QuickhackType(val displayName: String, val icon: String, val color: Color) {
    PULSE("Pulse", "⚡", Color(0xFF00B8FF)),
    OVERLOAD("Overload", "🔥", Color(0xFFFF3366)),
    FREEZE("Freeze", "❄️", Color(0xFF00FFFF)),
    VOID("Void", "🌌", Color(0xFFAA00FF)),
    REDIRECT("Redirect", "🔄", Color(0xFF00FF9F)),
    EXPLOIT("Exploit", "⚙️", Color(0xFFFFD700))
}

data class ThreatOrb(
    val id: String,
    val typeName: String,
    val sequence: List<QuickhackType>,
    var currentStep: Int = 0,
    var progress: Float = 0.0f, // 0.0 (outer) to 1.0 (reaches center core)
    val speed: Float,
    val angle: Float // in radians
)

@Composable
fun TraceDefenseScreen(
    onSuccess: () -> Unit,
    onFailure: () -> Unit,
    onDismiss: () -> Unit
) {
    var gameStage by remember { mutableStateOf("WARNING") } // WARNING, PLAYING, WON, LOST
    var heatLevel by remember { mutableFloatStateOf(0.0f) } // 0.0f to 1.0f
    var traceProgress by remember { mutableFloatStateOf(0.0f) } // 0.0f to 1.0f
    var timeLeft by remember { mutableIntStateOf(45) } // 45 seconds survival

    val activeThreats = remember { mutableStateListOf<ThreatOrb>() }
    var selectedThreatId by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf("NETWATCH COGNITIVE UPLINK COMPROMISED") }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val minDimension = minOf(screenWidth, screenHeight)

    // Flashing warning effect
    val warningAlpha = rememberInfiniteTransition(label = "warning").animateFloat(
        initialValue = 0.1f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WarningAlpha"
    )

    // Center core pulse
    val corePulse = rememberInfiniteTransition(label = "core").animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CorePulse"
    )

    // Stage 1: WARNING PHASE (2 seconds)
    LaunchedEffect(gameStage) {
        if (gameStage == "WARNING") {
            delay(2000)
            gameStage = "PLAYING"
        }
    }

    // Stage 2: GAME TIMER & TICKER
    LaunchedEffect(gameStage) {
        if (gameStage == "PLAYING") {
            while (timeLeft > 0 && heatLevel < 1.0f && traceProgress < 1.0f) {
                delay(1000)
                timeLeft--
            }
            if (heatLevel >= 1.0f) {
                gameStage = "LOST"
            } else {
                gameStage = "WON"
            }
        }
    }

    // Threat Spawner
    LaunchedEffect(gameStage) {
        if (gameStage == "PLAYING") {
            while (timeLeft > 0 && heatLevel < 1.0f && traceProgress < 1.0f) {
                // Dynamically determine interval based on current time
                val interval = when {
                    timeLeft > 30 -> 4500L
                    timeLeft > 15 -> 3000L
                    else -> 1800L
                }
                delay(interval)
                // Generate a random threat
                val id = "threat_${System.currentTimeMillis()}"
                val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
                
                // Sequence length based on wave difficulty
                val seqLength = when {
                    timeLeft > 30 -> 2
                    timeLeft > 15 -> 3
                    else -> 4
                }
                
                val seq = List(seqLength) { QuickhackType.entries.random() }
                val name = listOf("DAEMON_PING", "PORT_SCAN", "ICE_BREAKER", "CORE_PROBE").random()
                
                // Adjusted speeds: must take several seconds to travel
                val speed = when {
                    timeLeft > 30 -> 0.0018f // ~10 seconds
                    timeLeft > 15 -> 0.0028f // ~6.5 seconds
                    else -> 0.0040f // ~4.5 seconds
                }
                
                activeThreats.add(
                    ThreatOrb(
                        id = id,
                        typeName = name,
                        sequence = seq,
                        speed = speed,
                        angle = angle
                    )
                )
                
                // Auto-select first threat if none selected
                if (selectedThreatId == null) {
                    selectedThreatId = id
                }
            }
        }
    }

    // Frame Tick (60 FPS Simulation)
    LaunchedEffect(gameStage) {
        if (gameStage == "PLAYING") {
            while (timeLeft > 0 && heatLevel < 1.0f && traceProgress < 1.0f) {
                delay(16) // ~60fps
                
                val toRemove = mutableListOf<ThreatOrb>()
                
                // Use a standard index loop to modify properties safely
                for (i in activeThreats.indices) {
                    val threat = activeThreats[i]
                    threat.progress += threat.speed
                    if (threat.progress >= 1.0f) {
                        toRemove.add(threat)
                    }
                }
                
                if (toRemove.isNotEmpty()) {
                    toRemove.forEach {
                        activeThreats.remove(it)
                        // Damage core: build heat
                        heatLevel = (heatLevel + 0.15f).coerceAtMost(1.0f)
                        statusText = "WARNING: DECK CORE INTEGRITY COMPROMISED (+15% HEAT)"
                        if (selectedThreatId == it.id) {
                            selectedThreatId = activeThreats.firstOrNull()?.id
                        }
                    }
                }
                
                if (heatLevel >= 1.0f) {
                    gameStage = "LOST"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030000))
    ) {
        // Red Grid Alarm Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridColor = Color.Red.copy(alpha = if (gameStage == "WARNING" || heatLevel > 0.7f) warningAlpha.value * 0.15f else 0.05f)
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension * 0.48f
            
            // Draw Concentric Radar Rings
            for (r in 1..4) {
                drawCircle(
                    color = gridColor,
                    radius = maxRadius * (r / 4f),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            
            // Draw Radar sweep lines
            for (i in 0 until 8) {
                val lineAngle = (i * Math.PI / 4).toFloat()
                val targetX = center.x + maxRadius * cos(lineAngle)
                val targetY = center.y + maxRadius * sin(lineAngle)
                drawLine(
                    color = gridColor,
                    start = center,
                    end = Offset(targetX, targetY),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        when (gameStage) {
            "WARNING" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red.copy(alpha = warningAlpha.value * 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = "!!! TRACE INITIATED !!!",
                            color = Color.Red,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.animateContentSize()
                        )
                        Text(
                            text = "NETWATCH LEVEL 3 POLICE AI LOCATING SUBNET",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "DEFUSE THE COGNITIVE PROBES BEFORE INTENSITY CRITICAL",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            "PLAYING" -> {
                // Main radar playfield
                Box(modifier = Modifier.fillMaxSize()) {
                    
                    // Central Cyberdeck Core
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF00B8FF).copy(alpha = 0.4f),
                                        Color.Black
                                    )
                                )
                            )
                            .border(
                                2.dp * corePulse.value,
                                if (heatLevel > 0.7f) Color.Red else Color(0xFF00B8FF),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "CORE",
                                color = if (heatLevel > 0.7f) Color.Red else Color(0xFF00B8FF),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "${(100 - (heatLevel * 100).toInt())}%",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Render Threat Orbs in Polar Coordinates relative to the center core
                    activeThreats.forEach { threat ->
                        val isSelected = selectedThreatId == threat.id
                        
                        // Collapse radius towards core (0f = outer, 1f = core)
                        val maxRadius = minDimension * 0.40f
                        val radius = maxRadius * (1.0f - threat.progress)
                        
                        val x = radius * cos(threat.angle)
                        val y = radius * sin(threat.angle)
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(x = x.dp, y = y.dp)
                                .size(90.dp)
                                .clickable { selectedThreatId = threat.id },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                // Selected Ring
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.75f))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.5.dp,
                                            color = if (isSelected) Color.Yellow else Color.Red,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = threat.typeName.take(4),
                                        color = if (isSelected) Color.Yellow else Color.Red,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                
                                // Sequence Chain display
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    threat.sequence.forEachIndexed { idx, qh ->
                                        val isDone = idx < threat.currentStep
                                        Text(
                                            text = qh.icon,
                                            color = if (isDone) Color.Gray.copy(alpha = 0.3f) else qh.color,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(
                                                    if (isDone) Color.Transparent else qh.color.copy(alpha = 0.1f),
                                                    CircleShape
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isDone) Color.Transparent else qh.color.copy(alpha = 0.4f),
                                                    CircleShape
                                                )
                                                .padding(2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Top Status HUD
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DECRYPTING_DOSSIER // TIME: ${timeLeft}s",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "LEVEL 3 SECURITY",
                                color = Color.Red,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Progress bars: Trace vs Heat
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Trace Meter
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("TRACE DECRYPTION", color = Color(0xFF00FF9F), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("${(traceProgress * 100).toInt()}%", color = Color(0xFF00FF9F), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                LinearProgressIndicator(
                                    progress = { traceProgress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = Color(0xFF00FF9F),
                                    trackColor = Color(0xFF00FF9F).copy(alpha = 0.1f)
                                )
                            }

                            // Heat Meter
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("COGNITIVE CORES HEAT", color = Color.Red, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("${(heatLevel * 100).toInt()}%", color = Color.Red, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                LinearProgressIndicator(
                                    progress = { heatLevel },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = Color.Red,
                                    trackColor = Color.Red.copy(alpha = 0.1f)
                                )
                            }
                        }

                        Text(
                            text = "> $statusText",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Bottom Quickhack Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .align(Alignment.BottomCenter),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CYBERDECK AVAILABLE QUICKHACKS // FOCUS ATTACHED",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            QuickhackType.entries.forEach { qh ->
                                Button(
                                    onClick = {
                                        // Execute quickhack on targeted threat
                                        val targeted = activeThreats.find { it.id == selectedThreatId }
                                        if (targeted != null) {
                                            val expected = targeted.sequence[targeted.currentStep]
                                            if (qh == expected) {
                                                targeted.currentStep++
                                                statusText = "SEQUENCER_SUCCESS: ADVANCED PROBE (${qh.displayName})"
                                                if (targeted.currentStep >= targeted.sequence.size) {
                                                    // Defeated!
                                                    activeThreats.remove(targeted)
                                                    selectedThreatId = activeThreats.firstOrNull()?.id
                                                    
                                                    // Progress rewards
                                                    traceProgress = (traceProgress + 0.18f).coerceAtMost(1.0f)
                                                    statusText = "PROBE DEFUSED // TOTAL DECRYPTION ADVANCED (+18%)"
                                                }
                                            } else {
                                                // Penalty
                                                heatLevel = (heatLevel + 0.08f).coerceAtMost(1.0f)
                                                statusText = "DECRYPTION ERROR: ILLEGAL SYMBOLS IN CHAIN (+8% HEAT)"
                                            }
                                        } else {
                                            statusText = "ERROR: TARGET CORES OFFLINE. LOCK TARGET NODE FIRST."
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .border(1.dp, qh.color.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Black.copy(alpha = 0.6f)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(qh.icon, fontSize = 18.sp, color = qh.color)
                                        Text(qh.displayName.take(5), fontSize = 9.sp, color = qh.color, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text("ABORT RUN (EMERGENCY DETACH)", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            "WON" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CyberFrame(label = "NETWATCH BREACH STATUS // DECRYPTION COMPLETE") {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("ACCESS GRANTED", color = Color(0xFF00FF9F), fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            Text(
                                "Your living shadow dossier has been successfully extracted from the Shadow Archive core servers. NetWatch trace algorithms completely defused.",
                                color = Color.White,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .border(1.dp, Color(0xFF00FF9F).copy(alpha = 0.3f))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("REWARDS EXTRACTED:", color = Color(0xFF00FF9F), fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Text("• FULL CLASS_GHOST RUNNER DOSSIER ACCESS", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("• GHOST PROTOCOL PERMANENT PERK (+25% GRID STEALTH)", color = Color.Magenta, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("• WATCHER BLIND & DOSSIER REWRITE LEGENDARY QUICKHACKS", color = Color.Cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("• +500 XP COGNITIVE TUNING RECON UPGRADE", color = Color.Yellow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onSuccess, // Triggers database rewards & closes
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9F).copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, Color(0xFF00FF9F))
                            ) {
                                Text("ACCESS SHADOW DOSSIER", color = Color(0xFF00FF9F))
                            }
                        }
                    }
                }
            }

            "LOST" -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CyberFrame(label = "NETWATCH BREACH STATUS // EXCLUSION PROTOCOL") {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("CORES COOKED", color = Color.Red, fontSize = 24.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            Text(
                                "Your deck was trace-locked and subjected to extreme voltage dump by NetWatch system controller. Backdoor access excluded.",
                                color = Color.White,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .border(1.dp, Color.Red.copy(alpha = 0.3f))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("DAMAGES ENFORCED:", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Text("• -500 RUNNER COGNITIVE EXPERIENCE (XP LOSS)", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("• NEURAL LINK LOCKED OUT (ALL QUICKHACKS ON 4H COOLDOWN)", color = Color.Yellow, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onFailure, // Triggers database penalty & closes
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, Color.Red)
                            ) {
                                Text("DISCONNECT LINK", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}
