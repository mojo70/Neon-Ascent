package com.neon.ascent.feature.dashboard

import android.graphics.BitmapFactory
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.model.UserCharacter
import com.neon.ascent.ui.*
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun HolographicAvatarHub(
    viewModel: DashboardViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onUpgradeClick: (String) -> Unit,
    onHacksClick: () -> Unit,
    onAttributeScanClick: () -> Unit,
    onStoryClick: () -> Unit,
    onGoalSettingClick: () -> Unit
) {
    val userCharacter by viewModel.userCharacter.collectAsState()
    val healthState by viewModel.healthState.collectAsState()
    val tickerMessages by viewModel.tickerMessages.collectAsState()
    var selectedBodyPart by remember { mutableStateOf<String?>(null) }
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }

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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020202))) {
        // --- 1. ATMOSPHERIC HUD LAYERS ---
        PerspectiveGrid()
        Scanlines()
        StaticNoise(intensity = displayLoad)
        Vignette()
        FloatingParticles(intensity = displayLoad)
        GlitchOverlay(intensity = displayLoad)
        HudCornerAccents(color = Color(0xFF00FF9C).copy(alpha = 0.2f))

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // --- 2. TOP HUD BAR ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
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
                            text = "LEVEL ${userCharacter?.level ?: 1} OPERATIVE // SEC_ID: 0x${(userCharacter?.id ?: 0).toString(16).uppercase()}",
                            color = Color(0xFF00FF9C).copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
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

            // --- 3. CENTRAL Area: Load Gauge & Avatar ---
            Row(modifier = Modifier.weight(1f)) {
                // Left Column: Gauge & Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NeuralLoadGauge(load = displayLoad, modifier = Modifier.size(220.dp))
                    
                    Spacer(Modifier.height(32.dp))
                    
                    EnergyBar(label = "NEURAL_ENERGY", value = healthState.bodyBattery / 100f)
                    
                    Spacer(Modifier.height(24.dp))
                    
                    //Advice/Status Panel
                    HolographicAdvicePanel(
                        message = tickerMessages.firstOrNull() ?: "ALL SYSTEMS OPERATIONAL",
                        intensity = displayLoad
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Avatar Display Panel
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .neonBorder(Color(0xFF00FF9C).copy(alpha = 0.3f), cornerRadius = 12.dp)
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    HologramDisplay(userCharacter) { part ->
                        selectedBodyPart = part
                        systemLogs.add(0, "[LOG] SECTOR_ACCESS: $part")
                        glitchBurstIntensity = 0.3f
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. BOTTOM HUD Area ---
            Row(modifier = Modifier.height(220.dp)) {
                Column(modifier = Modifier.weight(1.5f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Biometric Data Panel
                    CyberFrame(label = "BIOMETRIC_STATUS", modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                val statsText = when(selectedBodyPart) {
                                    "HEAD" -> {
                                        val rank = userCharacter?.getChessRank() ?: "GHOST_IN_SHELL"
                                        "PERCEPTION: ${userCharacter?.perception ?: "??"}\nRANK: $rank\nELO: ${userCharacter?.chessElo ?: 1000}"
                                    }
                                    "TORSO" -> "ENDURANCE: ${userCharacter?.endurance ?: "??"}\nHEART_RATE: ${healthState.heartRate} BPM"
                                    "ARMS" -> "STRENGTH: ${userCharacter?.strength ?: "??"}\nLOAD_CAP: 85%"
                                    "LEGS" -> "AGILITY: ${userCharacter?.agility ?: "??"}\nREFLEX: ACTIVATED"
                                    else -> "SYSTEM SYNC: OPTIMAL\nALL NODES: OPERATIONAL"
                                }
                                Text(
                                    statsText, 
                                    color = Color.White, 
                                    fontSize = 10.sp, 
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                                
                                if (selectedBodyPart != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = { onUpgradeClick(selectedBodyPart!!) },
                                        modifier = Modifier.height(24.dp).clip(CyberButtonShape),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text("UPGRADE", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            AttributeRadarChart(
                                stats = mapOf(
                                    "STR" to (userCharacter?.strength ?: 0),
                                    "AGI" to (userCharacter?.agility ?: 0),
                                    "END" to (userCharacter?.endurance ?: 0),
                                    "PER" to (userCharacter?.perception ?: 0),
                                    "CHA" to (userCharacter?.charisma ?: 0),
                                    "LUC" to (userCharacter?.luck ?: 0)
                                ),
                                modifier = Modifier.size(110.dp)
                            )
                        }
                    }
                    
                    // Logs Terminal
                    CyberFrame(label = "TERMINAL_OUTPUT", accentColor = Color.Gray, modifier = Modifier.height(80.dp)) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(systemLogs.take(4)) { log ->
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

                Spacer(Modifier.width(16.dp))

                // Action Column
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardActionButton("ATTRIBUTE SCAN", Color(0xFF00FF9C)) { 
                        onAttributeScanClick()
                        glitchBurstIntensity = 0.4f
                    }
                    DashboardActionButton("YOUR STORY", Color(0xFFFF006E)) { 
                        onStoryClick()
                        glitchBurstIntensity = 0.4f
                    }
                    DashboardActionButton("GOAL SETTING", Color.White) { 
                        onGoalSettingClick()
                        glitchBurstIntensity = 0.4f
                    }
                    DashboardActionButton("BIOHACKS", Color(0xFF00FFFF)) { 
                        onHacksClick()
                        glitchBurstIntensity = 0.4f
                    }
                }
            }
        }
    }
}

@Composable
fun HolographicAdvicePanel(message: String, intensity: Float) {
    val jitterOffset = if (intensity > 0.7f) {
        Offset(Random.nextInt(-5, 5).toFloat(), Random.nextInt(-3, 3).toFloat())
    } else Offset.Zero

    Box(
        modifier = Modifier
            .offset(jitterOffset.x.dp, jitterOffset.y.dp)
            .neonBorder(
                color = if (intensity > 0.8f) Color.Red else Color(0xFF00FFFF),
                width = 1.dp,
                glowIntensity = 0.7f,
                cornerRadius = 4.dp
            )
            .background(Color(0xFF00FFFF).copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Text(
            text = message,
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun DashboardActionButton(label: String, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val glowIntensity by animateFloatAsState(
        targetValue = if (isPressed) 0.5f else 1f,
        label = "GlowIntensity"
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(CyberButtonShape)
            .neonBorder(color, width = 2.dp, glowIntensity = glowIntensity),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F).copy(alpha = 0.9f)),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            label, 
            color = color, 
            fontSize = 11.sp, 
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.5.sp
        )
    }
}

@Composable
fun AvatarImage(character: UserCharacter?, modifier: Modifier = Modifier, alpha: Float = 0.7f) {
    val context = LocalContext.current
    val avatarBitmap = remember(character?.avatarPath) {
        if (character?.avatarPath != null && character.avatarPath != "internal_storage_placeholder") {
            try {
                BitmapFactory.decodeFile(character.avatarPath)
            } catch (e: Exception) { null }
        } else { null }
    }

    if (avatarBitmap != null) {
        Image(
            bitmap = avatarBitmap.asImageBitmap(),
            contentDescription = "Avatar",
            modifier = modifier.alpha(alpha),
            contentScale = ContentScale.Crop
        )
    } else {
        PixelatedSilhouette(modifier = modifier.fillMaxSize())
    }
}

@Composable
fun NeuralLoadGauge(load: Float, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "GaugeAnim")
    
    // Pulse scale based on load
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1f + (load * 0.05f),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (1000 / (load + 0.5f).coerceAtLeast(0.1f)).toInt(), easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (5000 / (load + 0.1f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Box(contentAlignment = Alignment.Center, modifier = modifier.graphicsLayer {
        scaleX = pulseScale
        scaleY = pulseScale
    }) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 5.dp.toPx()
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension - strokeWidth) / 2
            
            // Background track
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = radius,
                style = Stroke(width = strokeWidth)
            )

            // Layered Progress Arc with dynamic intensity
            val sweepAngle = load * 360f
            val gaugeColor = if (load > 0.8f) Color(0xFFFF0000) else Color(0xFFFF006E)
            val glowIntensity = 0.5f + (load * 0.5f)
            
            for (i in 0..6) {
                val f = i.toFloat()
                val glowAlpha = (0.25f - f * 0.03f).coerceAtLeast(0f) * glowIntensity
                drawArc(
                    color = gaugeColor.copy(alpha = glowAlpha),
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth + f * (10f + load * 5f), cap = StrokeCap.Round)
                )
            }
            
            drawArc(
                color = gaugeColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Spinning scanning line
            rotate(rotation, center) {
                drawLine(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, gaugeColor.copy(alpha = 0.6f))),
                    start = center,
                    end = Offset(center.x, center.y - radius),
                    strokeWidth = 3.dp.toPx()
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "NEURAL_LOAD",
                color = if (load > 0.8f) Color.Red else Color(0xFFFF006E).copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                "${(load * 100).toInt()}%",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                style = TextStyle(
                    shadow = Shadow(if (load > 0.8f) Color.Red else Color(0xFFFF006E), blurRadius = 15f * load)
                )
            )
        }
    }
}

@Composable
fun EnergyBar(label: String, value: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFF00FFFF), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("${(value * 100).toInt()}%", color = Color(0xFF00FFFF), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Color.Black)
                .neonBorder(Color(0xFF00FFFF).copy(alpha = 0.4f), width = 1.dp, cornerRadius = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF00FFFF).copy(alpha = 0.5f), Color(0xFF00FFFF))
                        )
                    )
            )
        }
    }
}

@Composable
fun MetricSmall(label: String, value: String, unit: String) {
    Column {
        Text(label, color = Color.Gray, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.width(2.dp))
            Text(unit, color = Color(0xFF00FF9C), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun HeartbeatLine(bpm: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "Heartbeat")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000 / bpm.coerceAtLeast(1), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Phase"
    )

    Canvas(modifier = Modifier.size(60.dp, 20.dp)) {
        val path = Path()
        val width = size.width
        val height = size.height
        val centerY = height / 2

        path.moveTo(0f, centerY)
        path.lineTo(width * 0.3f, centerY)
        path.lineTo(width * 0.4f, centerY - height * 0.4f)
        path.lineTo(width * 0.5f, centerY + height * 0.4f)
        path.lineTo(width * 0.6f, centerY)
        path.lineTo(width, centerY)

        drawPath(
            path = path,
            color = Color(0xFFFF006E).copy(alpha = 0.2f),
            style = Stroke(width = 2.dp.toPx())
        )

        val drawPath = Path()
        drawPath.moveTo(0f, centerY)
        if (phase > 0.3f) drawPath.lineTo(width * 0.3f, centerY)
        if (phase > 0.4f) drawPath.lineTo(width * 0.4f, centerY - height * 0.4f)
        if (phase > 0.5f) drawPath.lineTo(width * 0.5f, centerY + height * 0.4f)
        if (phase > 0.6f) drawPath.lineTo(width * 0.6f, centerY)
        if (phase > 0.9f) drawPath.lineTo(width, centerY)

        drawPath(
            path = drawPath,
            color = Color(0xFFFF006E),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun AttributeRadarChart(
    stats: Map<String, Int>,
    modifier: Modifier = Modifier,
    maxValue: Int = 10
) {
    val labels = listOf("STR", "AGI", "END", "PER", "CHA", "LUC")
    val values = listOf(
        stats["STR"] ?: 0,
        stats["AGI"] ?: 0,
        stats["END"] ?: 0,
        stats["PER"] ?: 0,
        stats["CHA"] ?: 0,
        stats["LUC"] ?: 0
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.minDimension / 2.5f

        for (i in 1..5) {
            val f = i.toFloat()
            val currentRadius = radius * (f / 5f)
            val path = Path()
            for (j in 0 until 6) {
                val angle = (j * 60f - 90f) * (Math.PI / 180f).toFloat()
                val x = centerX + currentRadius * cos(angle)
                val y = centerY + currentRadius * sin(angle)
                if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(
                path = path,
                color = Color(0xFF00FF9C).copy(alpha = 0.1f),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        for (i in 0 until 6) {
            val angle = (i * 60f - 90f) * (Math.PI / 180f).toFloat()
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            drawLine(
                color = Color(0xFF00FF9C).copy(alpha = 0.1f),
                start = Offset(centerX, centerY),
                end = Offset(x, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val dataPath = Path()
        for (i in 0 until 6) {
            val angle = (i * 60f - 90f) * (Math.PI / 180f).toFloat()
            val value = values[i].coerceIn(0, maxValue)
            val currentRadius = radius * (value.toFloat() / maxValue)
            val x = centerX + currentRadius * cos(angle)
            val y = centerY + currentRadius * sin(angle)
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(
            path = dataPath,
            color = Color(0xFF00FF9C).copy(alpha = 0.3f)
        )
        drawPath(
            path = dataPath,
            color = Color(0xFF00FF9C),
            style = Stroke(width = 2.dp.toPx())
        )
        
        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 8.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
        }
        
        for (i in 0 until 6) {
            val angle = (i * 60f - 90f) * (Math.PI / 180f).toFloat()
            val x = centerX + (radius + 12.dp.toPx()) * cos(angle)
            val y = centerY + (radius + 12.dp.toPx()) * sin(angle)
            drawContext.canvas.nativeCanvas.drawText(
                labels[i],
                x,
                y + 4.dp.toPx(),
                textPaint
            )
        }
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
fun PixelatedSilhouette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val color = Color(0xFF00FF9C).copy(alpha = 0.3f)
        val pixelSize = 10.dp.toPx()
        
        drawRect(color, Offset(center.x - pixelSize*2, pixelSize*2), Size(pixelSize*4, pixelSize*4))
        drawRect(color, Offset(center.x - pixelSize, pixelSize*6), Size(pixelSize*2, pixelSize*2))
        drawRect(color, Offset(center.x - pixelSize*4, pixelSize*8), Size(pixelSize*8, pixelSize*12))
        drawRect(color, Offset(center.x - pixelSize*6, pixelSize*8), Size(pixelSize*2, pixelSize*10))
        drawRect(color, Offset(center.x + pixelSize*4, pixelSize*8), Size(pixelSize*2, pixelSize*10))
        drawRect(color, Offset(center.x - pixelSize*3, pixelSize*20), Size(pixelSize*2, pixelSize*12))
        drawRect(color, Offset(center.x + pixelSize, pixelSize*20), Size(pixelSize*2, pixelSize*12))
    }
}
