package com.neon.ascent.feature.dashboard

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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
    val isReligionShortcutEnabled by settingsViewModel.isReligionShortcutEnabled.collectAsState()
    val currentTime = remember { mutableStateOf(LocalDateTime.now()) }
    
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(1000)
            currentTime.value = LocalDateTime.now()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020202))) {
        CyberGridBackground()
        GlitchOverlay()

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
                            .border(2.dp, Color(0xFF00FF9C), CyberButtonShape)
                            .background(Color(0xFF0A0A0A))
                            .clickable { onAvatarClick() }
                    ) {
                        Text(
                            "AVATAR", 
                            color = Color(0xFF00FF9C).copy(alpha = 0.3f),
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
                        Text(
                            currentTime.value.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                            color = Color(0xFF00FF9C),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black
                        )
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
                        color = Color(0xFF00FF9C),
                        trackColor = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Cyberpunk Quote of the Day
            CyberFrame(label = "SYSTEM_ADVICE // V.01") {
                Text(
                    "\"THE SKY ABOVE THE PORT WAS THE COLOR OF TELEVISION, TUNED TO A DEAD CHANNEL.\"",
                    color = Color(0xFF00FF9C),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wearable Display
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MetricCard(label = "STEPS", value = "8,432", subValue = "GOAL: 10K", modifier = Modifier.weight(1f))
                MetricCard(label = "CALORIES", value = "1,840", subValue = "GOAL: 2.2K", modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Character Stats
            CyberFrame(label = "CORE_ATTRIBUTES") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttributeRow("STRENGTH", userCharacter?.strength)
                    AttributeRow("PERCEPTION", userCharacter?.perception)
                    AttributeRow("ENDURANCE", userCharacter?.endurance)
                    AttributeRow("CHARISMA", userCharacter?.charisma)
                    AttributeRow("AGILITY", userCharacter?.agility)
                    AttributeRow("LUCK", userCharacter?.luck)
                    if (userCharacter?.holyGhost != null) {
                        AttributeRow("HOLY_GHOST", userCharacter?.holyGhost)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardButton("ATTRIBUTE SCAN", Color(0xFF00FF9C), onAttributeSetClick)
                DashboardButton("YOUR STORY", Color(0xFFFF006E), onStoryClick)
                DashboardButton("GOAL SETTING", Color.White, onGoalSetClick)
            }
            
            Spacer(modifier = Modifier.height(40.dp))
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
            drawRect(color = color, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 4.dp.toPx()))
            drawRect(color = color, topLeft = Offset(size.width - 4.dp.toPx(), 0f), size = androidx.compose.ui.geometry.Size(4.dp.toPx(), 4.dp.toPx()))
        }
    }
}

@Composable
fun NeuralJackIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                size = androidx.compose.ui.geometry.Size(jackWidth, jackHeight)
            )
            
            // Flickering "RUN" dot
            if (Random.nextFloat() > 0.3f) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(center.x - 1.dp.toPx(), center.y - 1.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, subValue: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CyberButtonShape)
            .background(Color(0xFF0F0F0F))
            .border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.3f), CyberButtonShape)
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
