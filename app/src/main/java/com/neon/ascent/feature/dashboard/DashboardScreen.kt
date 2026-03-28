package com.neon.ascent.feature.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.feature.settings.SettingsViewModel
import com.neon.ascent.ui.*
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
    val displayLoad = neuralLoad
    
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF010101))) {
        // --- 1. ATMOSPHERIC HUD LAYERS ---
        PerspectiveGrid()
        Scanlines()
        StaticNoise(intensity = displayLoad * 0.4f)
        Vignette()
        FloatingParticles(intensity = displayLoad)
        
        if (weatherState.isNight) {
            NightCityGlow()
        }
        
        GlitchOverlay(intensity = displayLoad * 0.2f)

        if (weatherState.isRaining) {
            AcidRainOverlay()
        }
        
        HudCornerAccents(color = systemColor.copy(alpha = 0.2f))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- 2. TOP HUD BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Avatar Window with Holy Ghost Aura and Neon Border
                Box(contentAlignment = Alignment.Center) {
                    if (userCharacter?.holyGhost != null) {
                        HolyGhostAura()
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CyberButtonShape)
                            .neonBorder(systemColor, width = 2.dp, cornerRadius = 12.dp)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .clickable { onAvatarClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarImage(userCharacter, modifier = Modifier.fillMaxSize(), alpha = 1f)
                        
                        Text(
                            text = "SYNC", 
                            color = systemColor.copy(alpha = 0.4f),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black
                            )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isReligionShortcutEnabled) {
                            CyberCrossIcon(onClick = onReligionClick)
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
                                )
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
                        text = "RANK_0${userCharacter?.level ?: 1}",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    )
                    // Mini Progress Bar
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

            // --- 3. CENTRAL STATUS HUB ---
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeuralLoadGauge(
                        load = neuralLoad,
                        modifier = Modifier.size(110.dp)
                    )
                    
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "NEURAL_LINK_INTEGRITY",
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
                        
                        // Detailed Load Bar with Glow
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

            // --- 4. HOLOGRAPHIC QUOTE PANEL ---
            CyberFrame(
                label = "SYSTEM_ADVICE // V.01", 
                accentColor = Color(0xFF00FFFF),
                borderColor = Color(0xFF00FFFF).copy(alpha = 0.6f)
            ) {
                Text(
                    text = "\"THE SKY ABOVE THE PORT WAS THE COLOR OF TELEVISION, TUNED TO A DEAD CHANNEL.\"",
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

            Spacer(modifier = Modifier.height(24.dp))

            // --- 5. WEARABLE METRICS ---
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CyberMetricCard(
                    label = "STEPS", 
                    value = if (healthState.isConnected) healthState.steps.toString() else "8,432", 
                    subValue = "TARGET: 10K", 
                    color = Color(0xFF00FFFF), 
                    modifier = Modifier.weight(1f)
                )
                CyberMetricCard(
                    label = "CALORIES", 
                    value = "1,840", 
                    subValue = "TARGET: 2.2K", 
                    color = Color(0xFFFF006E), 
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- 6. BOTTOM ACTION GROUP ---
            CyberFrame(label = "CORE_SYNC", borderColor = systemColor) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "BIOMETRIC_INTERFACE_READY", 
                        color = Color.White.copy(alpha = 0.6f), 
                        fontSize = 10.sp, 
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    
                    CyberActionButton("ATTRIBUTE SCAN", Color(0xFF00FF9C), onClick = onAttributeSetClick)
                    CyberActionButton("YOUR STORY", Color(0xFFFF006E), onClick = onStoryClick)
                    CyberActionButton("GOAL SETTING", Color.White, onClick = onGoalSetClick)
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
        targetValue = 1.4f,
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
                .neonBorder(Color(0xFFFF006E), width = 1.dp, glowIntensity = 1f, cornerRadius = 4.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$bpm BPM",
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
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

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.3f)) {
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
                radius = 1.2.dp.toPx(),
                center = Offset(x, baseY)
            )
        }
    }
}

@Composable
fun NightCityGlow() {
    val infiniteTransition = rememberInfiniteTransition(label = "CityGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Glow"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color(0xFF1A0033).copy(alpha = glowAlpha)),
                    startY = size.height * 0.6f,
                    endY = size.height
                )
            )
        }
    }
}

@Composable
fun HolyGhostAura() {
    val infiniteTransition = rememberInfiniteTransition(label = "AuraScale")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Box(
        modifier = Modifier
            .size(90.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = Offset.Unspecified,
                    radius = 180f
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
            
            drawCircle(
                color = colorPrimary.copy(alpha = pulseAlpha),
                radius = size.width / 2.6f,
                style = Stroke(width = strokeWidth)
            )
            
            for (i in 0 until 8) {
                val angle = (i * 45f) * (Math.PI / 180f).toFloat()
                val start = Offset(
                    center.x + (size.width / 2.6f) * kotlin.math.cos(angle),
                    center.y + (size.width / 2.6f) * kotlin.math.sin(angle)
                )
                val end = Offset(
                    center.x + (size.width / 2.1f) * kotlin.math.cos(angle),
                    center.y + (size.width / 2.1f) * kotlin.math.sin(angle)
                )
                drawLine(colorPrimary, start, end, strokeWidth = strokeWidth)
            }
            
            drawRect(
                color = colorSecondary,
                topLeft = Offset(center.x - 4.dp.toPx(), center.y - 6.dp.toPx()),
                size = Size(8.dp.toPx(), 12.dp.toPx())
            )
        }
    }
}
