package com.neon.ascent.feature.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.feature.charactercreation.CyberButtonShape
import com.neon.ascent.feature.charactercreation.CyberFrame
import com.neon.ascent.feature.charactercreation.CyberGridBackground
import com.neon.ascent.feature.charactercreation.GlitchOverlay
import com.neon.ascent.feature.settings.SettingsViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onAvatarClick: () -> Unit,
    onAttributeSetClick: () -> Unit,
    onStoryClick: () -> Unit,
    onGoalSetClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onReligionClick: () -> Unit
) {
    val userCharacter by viewModel.userCharacter.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val healthState by viewModel.healthState.collectAsState()
    val isReligionShortcutEnabled by settingsViewModel.isReligionShortcutEnabled.collectAsState()
    val currentTime = remember { mutableStateOf(LocalDateTime.now()) }
    
    val neuralLoad = userCharacter?.neuralLoad ?: 0.2f
    val systemColor by animateColorAsState(
        targetValue = if (neuralLoad > 0.7f) Color(0xFFFF006E) else Color(0xFF00FF9C),
        animationSpec = tween(500),
        label = "SystemColor"
    )

    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(1000)
            currentTime.value = LocalDateTime.now()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(if (weatherState.isNight) Color(0xFF010101) else Color(0xFF020202))) {
        CyberGridBackground()
        
        if (weatherState.isNight) {
            NightCityGlow()
        }
        
        GlitchOverlay(intensity = neuralLoad)

        if (weatherState.isRaining) {
            AcidRainOverlay()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row: Avatar, Level, Time + Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Avatar Window with Holy Ghost Aura
                Box(contentAlignment = Alignment.Center) {
                    if (userCharacter?.holyGhost != null) {
                        HolyGhostAura()
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CyberButtonShape)
                            .border(2.dp, systemColor, CyberButtonShape)
                            .background(Color(0xFF0A0A0A))
                            .clickable { onAvatarClick() }
                    ) {
                        Text(
                            "AVATAR", 
                            color = systemColor.copy(alpha = 0.3f),
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isReligionShortcutEnabled) {
                            CyberCrossIcon(onClick = onReligionClick)
                            Spacer(Modifier.width(12.dp))
                        }
                        NeuralJackIcon(onClick = onSettingsClick)
                        Spacer(Modifier.width(12.dp))
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                currentTime.value.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                                color = systemColor,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (healthState.isConnected && healthState.heartRate > 0) {
                                    HeartRatePulse(healthState.heartRate)
                                    Spacer(Modifier.width(8.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (weatherState.isRaining) Color(0xFF00FFFF) else systemColor, CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "${weatherState.temperature}°${weatherState.unitSymbol}",
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Text(
                        currentTime.value.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")),
                        color = Color(0xFFFF006E),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "LEVEL: ${userCharacter?.level ?: 1}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    LinearProgressIndicator(
                        progress = { 0.4f },
                        modifier = Modifier.width(100.dp).height(4.dp),
                        color = systemColor,
                        trackColor = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Neural Load Meter & Quick Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeuralLoadMeter(
                    load = neuralLoad,
                    modifier = Modifier.size(120.dp)
                )
                
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "NEURAL_LOAD",
                        color = systemColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (neuralLoad > 0.8f) "CRITICAL_STRESS" else if (neuralLoad > 0.5f) "ELEVATED_LOAD" else "SYNC_STABLE",
                        color = if (neuralLoad > 0.7f) Color(0xFFFF006E) else Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black
                    )
                    LinearProgressIndicator(
                        progress = { neuralLoad },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = systemColor,
                        trackColor = systemColor.copy(alpha = 0.1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cyberpunk Quote of the Day
            CyberFrame(label = "SYSTEM_ADVICE // V.01") {
                Text(
                    "\"THE SKY ABOVE THE PORT WAS THE COLOR OF TELEVISION, TUNED TO A DEAD CHANNEL.\"",
                    color = systemColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wearable Display
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard(
                    label = "STEPS", 
                    value = if (healthState.isConnected) healthState.steps.toString() else "8,432", 
                    subValue = "GOAL: 10K", 
                    color = systemColor, 
                    modifier = Modifier.weight(1f)
                )
                MetricCard(label = "CALORIES", value = "1,840", subValue = "GOAL: 2.2K", color = systemColor, modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Character Stats Summary
            CyberFrame(label = "CORE_SYNC") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CHARACTER PROFILE DETECTED...", color = Color.White, fontSize = 12.sp)
                    Text("TAP AVATAR TO INITIALIZE BIOMETRIC INTERFACE", color = systemColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DashboardButton("ATTRIBUTE SCAN", systemColor, onAttributeSetClick)
                    DashboardButton("YOUR STORY", Color(0xFFFF006E), onStoryClick)
                    DashboardButton("GOAL SETTING", Color.White, onGoalSetClick)
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun HeartRatePulse(bpm: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "HeartPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000 / bpm / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(Color(0xFFFF006E), CircleShape)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "$bpm BPM",
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun AcidRainOverlay() {
    val rainDrops = remember { List(15) { Random.nextFloat() to Random.nextFloat() } }
    val infiniteTransition = rememberInfiniteTransition(label = "RainAnim")
    
    val rainY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RainY"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.4f)) {
        rainDrops.forEach { (xMult, yOffset) ->
            val x = xMult * size.width
            val baseY = ((rainY + yOffset) % 1f) * size.height
            
            // Trail
            drawLine(
                color = Color(0xFF00FF9C).copy(alpha = 0.2f),
                start = Offset(x, baseY - 40.dp.toPx()),
                end = Offset(x, baseY),
                strokeWidth = 1.dp.toPx()
            )
            // Droplet
            drawCircle(
                color = Color(0xFF00FF9C),
                radius = 1.5.dp.toPx(),
                center = Offset(x, baseY)
            )
        }
    }
}

@Composable
fun NightCityGlow() {
    val infiniteTransition = rememberInfiniteTransition(label = "CityGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Bottom-up purple/blue glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xFF1A0033).copy(alpha = glowAlpha)),
                    startY = size.height * 0.5f,
                    endY = size.height
                )
            )
            
            // Scattered "Building" lights at bottom
            repeat(20) {
                val x = Random.nextFloat() * size.width
                val y = size.height - (Random.nextFloat() * 100.dp.toPx())
                val size = Random.nextFloat() * 4.dp.toPx()
                drawRect(
                    color = listOf(Color(0xFF00FF9C), Color(0xFFFF006E), Color.Yellow).random().copy(alpha = 0.5f),
                    topLeft = Offset(x, y),
                    size = Size(size, size)
                )
            }
        }
    }
}

@Composable
fun NeuralLoadMeter(load: Float, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "LoadPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (load > 0.8f) 1.1f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (load > 0.8f) 500 else 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(modifier = modifier.graphicsLayer { 
        scaleX = pulseScale
        scaleY = pulseScale
    }, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 8.dp.toPx()
            val color = if (load > 0.7f) Color(0xFFFF006E) else Color(0xFF00FF9C)
            
            // Background Circle
            drawCircle(
                color = color.copy(alpha = 0.1f),
                style = Stroke(width = strokeWidth)
            )
            
            // Load Arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * load,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Inner decorative lines
            repeat(12) { i ->
                val angle = (i * 30f) * (Math.PI / 180f).toFloat()
                val lineLen = 10.dp.toPx()
                val start = Offset(
                    center.x + (size.width / 2.8f) * kotlin.math.cos(angle),
                    center.y + (size.width / 2.8f) * kotlin.math.sin(angle)
                )
                val end = Offset(
                    center.x + (size.width / 2.8f - lineLen) * kotlin.math.cos(angle),
                    center.y + (size.width / 2.8f - lineLen) * kotlin.math.sin(angle)
                )
                drawLine(
                    color = color.copy(alpha = 0.3f),
                    start = start,
                    end = end,
                    strokeWidth = 2f
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(load * 100).toInt()}%",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                "LOAD",
                color = if (load > 0.7f) Color(0xFFFF006E) else Color(0xFF00FF9C),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun HolyGhostAura() {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraScale")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Box(
        modifier = Modifier
            .size(100.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .blur(20.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = Offset.Unspecified,
                    radius = 200f
                ),
                alpha = alpha
            )
    )
}

@Composable
fun CyberCrossIcon(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(32.dp)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val color = Color(0xFF00FF9C)
            val strokeWidth = 2.dp.toPx()
            
            // Vertical bar
            drawLine(
                color = color,
                start = Offset(center.x, 4.dp.toPx()),
                end = Offset(center.x, size.height - 4.dp.toPx()),
                strokeWidth = strokeWidth
            )
            // Horizontal bar
            drawLine(
                color = color,
                start = Offset(4.dp.toPx(), center.y - 4.dp.toPx()),
                end = Offset(size.width - 4.dp.toPx(), center.y - 4.dp.toPx()),
                strokeWidth = strokeWidth
            )
            
            // Decorative corners (cyber feel)
            drawRect(color = color, topLeft = Offset(0f, 0f), size = Size(4.dp.toPx(), 4.dp.toPx()))
            drawRect(color = color, topLeft = Offset(size.width - 4.dp.toPx(), 0f), size = Size(4.dp.toPx(), 4.dp.toPx()))
        }
    }
}

@Composable
fun NeuralJackIcon(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "JackPulse")
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val colorPrimary = Color(0xFF00FF9C)
            val colorSecondary = Color(0xFFFF006E)
            
            // Gear Outer (Circuit Board themed)
            drawCircle(
                color = colorPrimary.copy(alpha = pulseAlpha),
                radius = size.width / 2.5f,
                style = Stroke(width = strokeWidth)
            )
            
            // Circuit teeth
            for (i in 0 until 8) {
                val angle = (i * 45f) * (Math.PI / 180f).toFloat()
                val start = Offset(
                    center.x + (size.width / 2.5f) * kotlin.math.cos(angle),
                    center.y + (size.width / 2.5f) * kotlin.math.sin(angle)
                )
                val end = Offset(
                    center.x + (size.width / 2f) * kotlin.math.cos(angle),
                    center.y + (size.width / 2f) * kotlin.math.sin(angle)
                )
                drawLine(colorPrimary, start, end, strokeWidth = strokeWidth)
            }
            
            // The "Jack" Plug
            val jackWidth = 8.dp.toPx()
            val jackHeight = 12.dp.toPx()
            drawRect(
                color = colorSecondary,
                topLeft = Offset(center.x - jackWidth / 2, center.y - jackHeight / 2),
                size = Size(jackWidth, jackHeight)
            )
            
            // Flickering "RUN" dot
            if (Random.nextFloat() > 0.3f) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(center.x - 1.dp.toPx(), center.y - 1.dp.toPx()),
                    size = Size(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, subValue: String, color: Color = Color(0xFF00FF9C), modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CyberButtonShape)
            .background(Color(0xFF0F0F0F))
            .border(1.dp, color.copy(alpha = 0.3f), CyberButtonShape)
            .padding(16.dp)
    ) {
        Column {
            Text(label, color = Color(0xFFFF006E), style = MaterialTheme.typography.labelSmall)
            Text(value, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(subValue, color = Color.Gray, fontSize = 10.sp)
        }
    }
}

@Composable
fun AttributeRow(label: String, value: Int?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF00FF9C).copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
        Text(value?.toString() ?: ":???", color = if (value != null) Color.White else Color(0xFFFF006E))
    }
}

@Composable
fun DashboardButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(CyberButtonShape)
            .border(1.dp, color, CyberButtonShape),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F0F0F))
    ) {
        Text(label, color = color, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
    }
}
