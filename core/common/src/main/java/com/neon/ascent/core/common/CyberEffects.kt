package com.neon.ascent.core.common

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.random.Random

@Composable
fun ResonanceAura(
    modifier: Modifier = Modifier,
    color: Color = NeonCyan,
    intensity: Float = 0.5f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ResonanceAura")
    
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f * intensity,
        targetValue = 0.4f * intensity,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = pulseAlpha), Color.Transparent),
                        center = center,
                        radius = size.maxDimension / 2f
                    )
                )
                
                // Add a faint geometric grid if intensity is high
                if (intensity > 0.6f) {
                    val gridAlpha = (pulseAlpha * 0.5f)
                    val step = 20.dp.toPx()
                    for (x in 0..size.width.toInt() step step.toInt()) {
                        drawLine(color.copy(alpha = gridAlpha), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height))
                    }
                    for (y in 0..size.height.toInt() step step.toInt()) {
                        drawLine(color.copy(alpha = gridAlpha), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()))
                    }
                }
            }
    )
}
data class CyberParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float,
    val alpha: Float,
    val life: Int
)

/**
 * Celebration Overlay for Dopamine Menu V3
 */
@Composable
fun CelebrationOverlay(
    event: DopamineEvent?,
    onFinished: () -> Unit,
    onActionClick: (() -> Unit)? = null
) {
    if (event == null) return

    // Auto-dismiss duration scales based on tier/importance
    // Only auto-dismiss if there's no action button, or give more time
    LaunchedEffect(event) {
        if (event.actionLabel != null) return@LaunchedEffect

        val duration = when(event.level) {
            CelebrationLevel.SUBTLE -> 1600L
            CelebrationLevel.SYNC -> 2500L
            CelebrationLevel.STREAK_RECOVERY -> 3000L
            CelebrationLevel.MISSION_COMPLETE -> 4500L
            CelebrationLevel.DIRECTIVE_MILESTONE -> 5000L
            CelebrationLevel.ASCENSION -> 5000L
        }
        delay(duration)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (event.level == CelebrationLevel.MISSION_COMPLETE || event.level == CelebrationLevel.DIRECTIVE_MILESTONE || event.level == CelebrationLevel.ASCENSION) {
                    Color.Black.copy(alpha = 0.85f)
                } else {
                    Color.Transparent
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (event.level) {
            CelebrationLevel.SUBTLE -> SubtleCelebration(event)
            CelebrationLevel.SYNC -> SyncCelebration(event)
            CelebrationLevel.STREAK_RECOVERY -> StreakRecoveryCelebration(event)
            CelebrationLevel.MISSION_COMPLETE -> MissionCompleteCelebration(event, onFinished)
            CelebrationLevel.DIRECTIVE_MILESTONE -> DirectiveMilestoneCelebration(event)
            CelebrationLevel.ASCENSION -> AscensionCelebration(event, onFinished, onActionClick)
        }
    }
}

@Composable
private fun SubtleCelebration(event: DopamineEvent) {
    val anim = remember { Animatable(0f) }
    val particles = remember { mutableStateListOf<CyberParticle>() }
    
    LaunchedEffect(Unit) {
        // Spawn active neon particles
        repeat(20) {
            particles.add(
                CyberParticle(
                    x = 0.5f,
                    y = 0.5f,
                    vx = ((Math.random() - 0.5f) * 12f).toFloat(),
                    vy = ((Math.random() - 0.5f) * 12f).toFloat(),
                    color = if (Math.random() > 0.5) NeonCyan else NeonPink,
                    size = (Math.random() * 6 + 3).toFloat(),
                    alpha = 1f,
                    life = (Math.random() * 20 + 15).toInt()
                )
            )
        }

        // Animate particles & float text
        launch {
            anim.animateTo(1f, animationSpec = tween(1200, easing = LinearOutSlowInEasing))
        }

        while (particles.isNotEmpty()) {
            withFrameMillis {
                val iterator = particles.listIterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    if (p.life <= 0) {
                        iterator.remove()
                    } else {
                        iterator.set(
                            p.copy(
                                x = p.x + p.vx * 0.015f,
                                y = p.y + p.vy * 0.015f,
                                alpha = p.alpha * 0.92f,
                                life = p.life - 1
                            )
                        )
                    }
                }
            }
        }
    }

    val alpha = (1f - anim.value).coerceIn(0f, 1f)
    val yOffset = (-80).dp * anim.value

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Draw physical particle canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawCircle(
                    color = p.color,
                    radius = p.size,
                    center = Offset(p.x * size.width, p.y * size.height),
                    alpha = p.alpha
                )
            }
            
            // Rim Flash screen light
            drawRect(
                color = NeonCyan.copy(alpha = 0.1f * alpha),
                size = size
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .offset(y = yOffset)
                .alpha(alpha)
        ) {
            Text(
                text = "+${event.xpGained} XP",
                color = NeonCyan,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge.copy(
                    shadow = Shadow(
                        color = NeonCyan.copy(alpha = 0.8f),
                        offset = Offset(0f, 0f),
                        blurRadius = 15f
                    )
                )
            )
            Text(
                text = event.message ?: "PROTOCOL_EXECUTED",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SyncCelebration(event: DopamineEvent) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(1f) }
    val particles = remember { mutableStateListOf<CyberParticle>() }

    LaunchedEffect(Unit) {
        repeat(30) {
            particles.add(
                CyberParticle(
                    x = 0.5f,
                    y = 0.5f,
                    vx = ((Math.random() - 0.5f) * 16f).toFloat(),
                    vy = ((Math.random() - 0.5f) * 16f).toFloat(),
                    color = NeonCyan,
                    size = (Math.random() * 8 + 4).toFloat(),
                    alpha = 1f,
                    life = (Math.random() * 30 + 20).toInt()
                )
            )
        }

        launch {
            scale.animateTo(1.2f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            delay(1200)
            alpha.animateTo(0f, animationSpec = tween(800))
        }

        while (particles.isNotEmpty()) {
            withFrameMillis {
                val iterator = particles.listIterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    if (p.life <= 0) {
                        iterator.remove()
                    } else {
                        iterator.set(
                            p.copy(
                                x = p.x + p.vx * 0.012f,
                                y = p.y + p.vy * 0.012f,
                                alpha = p.alpha * 0.94f,
                                life = p.life - 1
                            )
                        )
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawCircle(
                    color = p.color,
                    radius = p.size,
                    center = Offset(p.x * size.width, p.y * size.height),
                    alpha = p.alpha
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            Text(
                text = "NEURAL_SYNC_SUCCESS",
                color = NeonCyan,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium.copy(
                    shadow = Shadow(color = NeonCyan, blurRadius = 15f)
                )
            )
            Text(
                text = "+${event.xpGained} XP // STREAK_INTACT: ${event.streakCount} DAYS",
                color = NeonCyan.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StreakRecoveryCelebration(event: DopamineEvent) {
    val transitionScale = remember { Animatable(0.4f) }
    val amberToCyanProgress = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        transitionScale.animateTo(1.1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
        // Transition from Amber to glowing Cyan to represent signal recovery
        amberToCyanProgress.animateTo(1f, animationSpec = tween(1800, easing = FastOutSlowInEasing))
        delay(600)
        textAlpha.animateTo(0f, animationSpec = tween(600))
    }

    val currentGlowColor = Color(
        red = (1f - amberToCyanProgress.value) * 1f + amberToCyanProgress.value * 0f,
        green = (1f - amberToCyanProgress.value) * 0.6f + amberToCyanProgress.value * 1f,
        blue = (1f - amberToCyanProgress.value) * 0f + amberToCyanProgress.value * 0.61f,
        alpha = 1f
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw visual locking chain link shapes
            val midX = size.width / 2f
            val midY = size.height / 2f - 40.dp.toPx()
            
            val linkWidth = 60.dp.toPx()
            val linkHeight = 25.dp.toPx()

            // Left link
            drawRoundRect(
                color = currentGlowColor.copy(alpha = 0.3f),
                topLeft = Offset(midX - linkWidth - 10.dp.toPx() + (amberToCyanProgress.value * 10.dp.toPx()), midY),
                size = Size(linkWidth, linkHeight),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
            )
            // Right link
            drawRoundRect(
                color = currentGlowColor.copy(alpha = 0.3f),
                topLeft = Offset(midX + 10.dp.toPx() - (amberToCyanProgress.value * 10.dp.toPx()), midY),
                size = Size(linkWidth, linkHeight),
                cornerRadius = CornerRadius(8.dp.toPx()),
                style = Stroke(width = 4.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(transitionScale.value)
                .alpha(textAlpha.value)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            Text(
                text = "NEURAL_SIGNAL_RECOVERED",
                color = currentGlowColor,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleMedium.copy(
                    shadow = Shadow(color = currentGlowColor, blurRadius = 15f)
                )
            )
            Text(
                text = "ADHD_GRACE_BUFFER_RESTORATION // STREAK_INTACT",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Momentum maintained: ${event.streakCount} days",
                color = currentGlowColor.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun MissionCompleteCelebration(event: DopamineEvent, onDismiss: () -> Unit) {
    val scale = remember { Animatable(0.3f) }
    val opacity = remember { Animatable(0f) }
    val shards = remember { mutableStateListOf<CyberParticle>() }

    LaunchedEffect(Unit) {
        repeat(50) {
            shards.add(
                CyberParticle(
                    x = 0.5f,
                    y = 0.4f,
                    vx = ((Math.random() - 0.5f) * 20f).toFloat(),
                    vy = ((Math.random() - 0.7f) * 22f).toFloat(),
                    color = if (Math.random() > 0.5) NeonPink else NeonCyan,
                    size = (Math.random() * 10 + 5).toFloat(),
                    alpha = 1f,
                    life = (Math.random() * 40 + 30).toInt()
                )
            )
        }

        launch {
            scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        launch {
            opacity.animateTo(1f, animationSpec = tween(500))
        }

        while (shards.isNotEmpty()) {
            withFrameMillis {
                val iterator = shards.listIterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    if (p.life <= 0) {
                        iterator.remove()
                    } else {
                        iterator.set(
                            p.copy(
                                x = p.x + p.vx * 0.01f,
                                y = p.y + p.vy * 0.01f + 0.003f, // Gravity effect
                                alpha = p.alpha * 0.95f,
                                life = p.life - 1
                            )
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(opacity.value),
        contentAlignment = Alignment.Center
    ) {
        // Neon shard background canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            shards.forEach { s ->
                drawRect(
                    color = s.color,
                    topLeft = Offset(s.x * size.width, s.y * size.height),
                    size = Size(s.size, s.size * 2f), // rectangular shards
                    alpha = s.alpha
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(scale.value)
                .padding(24.dp)
                .neonBorder(color = NeonPink, width = 2.dp, cornerRadius = 16.dp)
                .background(Color.Black.copy(alpha = 0.9f))
                .padding(24.dp)
        ) {
            Text(
                text = "[MISSION INTEGRATED]",
                color = NeonPink,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge.copy(
                    shadow = Shadow(color = NeonPink, blurRadius = 20f)
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (event.message ?: "CAMPAIGN COMPLETE").uppercase(),
                color = Color.White,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "New baseline integrated into core OS operating metrics.",
                color = Color.Gray,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "+${event.xpGained} XP",
                color = NeonPink,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    "DISMISS",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun DirectiveMilestoneCelebration(event: DopamineEvent) {
    val ringScale = remember { Animatable(0f) }
    val textScale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            ringScale.animateTo(5f, animationSpec = tween(2200, easing = FastOutSlowInEasing))
        }
        launch {
            textScale.animateTo(1.1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
            delay(2500)
            alpha.animateTo(0f, animationSpec = tween(800))
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = NeonPink,
                radius = 80.dp.toPx() * ringScale.value,
                style = Stroke(width = 3.dp.toPx()),
                alpha = (1.2f - ringScale.value / 5f).coerceIn(0f, 1f) * alpha.value
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .scale(textScale.value)
                .alpha(alpha.value)
                .padding(32.dp)
        ) {
            Text(
                text = ">>> DIRECTIVE SYNCHRONIZED <<<",
                color = NeonPink,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleLarge.copy(
                    shadow = Shadow(color = NeonPink, blurRadius = 25f)
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = (event.message ?: "CRITICAL GOAL MILESTONE").uppercase(),
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Global progress synchronization has updated Directive pathways.",
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "+${event.xpGained} XP",
                color = NeonPink,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun AscensionCelebration(
    event: DopamineEvent,
    onDismiss: () -> Unit,
    onActionClick: (() -> Unit)? = null
) {
    var glitchIntensity by remember { mutableFloatStateOf(0.1f) }
    val ringScale = remember { Animatable(0f) }
    val ringAlpha = remember { Animatable(1f) }
    
    LaunchedEffect(Unit) {
        glitchIntensity = 0.8f
        delay(200)
        glitchIntensity = 0.2f
        ringScale.animateTo(4f, animationSpec = tween(2000, easing = LinearOutSlowInEasing))
        ringAlpha.animateTo(0f, animationSpec = tween(2000))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = NeonPink,
                radius = 100.dp.toPx() * ringScale.value,
                style = Stroke(width = 4.dp.toPx()),
                alpha = ringAlpha.value
            )
        }
        
        Column(
            modifier = Modifier.align(Alignment.Center).cyberGlitch(glitchIntensity),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = ">>> ASCENSION_COMPLETE <<<",
                color = NeonPink,
                fontSize = 32.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black
            )
            Text(
                text = event.message ?: "PROTOCOL_MASTERED",
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "+${event.xpGained} XP",
                color = NeonPink,
                fontSize = 24.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            if (event.actionLabel != null && onActionClick != null) {
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = onActionClick,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPink, contentColor = Color.Black),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        event.actionLabel.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "DISMISS",
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable { onDismiss() }
            )
        }
    }
}

/**
 * Reusable neon glow border modifier.
 */
fun Modifier.neonBorder(
    color: Color = Color.Cyan,
    width: Dp = 4.dp,
    glowIntensity: Float = 1f,
    cornerRadius: Dp = 12.dp
) = this.drawWithContent {
    drawContent()

    val radiusPx = cornerRadius.toPx()
    val widthPx = width.toPx()
    val cr = CornerRadius(radiusPx)

    for (i in 0..6) {
        val f = i.toFloat()
        val alphaVal = ((0.25f - (f * 0.03f)).coerceAtLeast(0f) * glowIntensity).coerceIn(0f, 1f)
        if (alphaVal > 0f) {
            drawRoundRect(
                color = color.copy(alpha = alphaVal),
                cornerRadius = cr,
                style = Stroke(width = widthPx + f * 6f)
            )
        }
    }

    drawRoundRect(
        color = color,
        cornerRadius = cr,
        style = Stroke(width = widthPx + 2f)
    )

    drawRoundRect(
        color = Color.White.copy(alpha = (0.5f * glowIntensity).coerceIn(0f, 1f)),
        cornerRadius = cr,
        style = Stroke(width = 1.5f)
    )
}

/**
 * Amplified Glitch Effect Modifier.
 */
fun Modifier.cyberGlitch(
    intensity: Float = 0f
) = this.composed {
    if (intensity <= 0f) return@composed this

    var tick by remember { mutableLongStateOf(0L) }
    
    LaunchedEffect(intensity) {
        while (true) {
            withFrameNanos { tick = it }
            val frameDelay = when {
                intensity < 0.25f -> 300L 
                intensity < 0.5f -> 120L
                else -> 16L 
            }
            delay(frameDelay)
        }
    }

    this.drawWithContent {
        val random = Random(tick)
        val loadProb = (intensity.pow(3f) * 0.9f).coerceIn(0f, 0.95f)
        val isGlitching = random.nextFloat() < loadProb

        if (!isGlitching) {
            drawContent()
            return@drawWithContent
        }

        val visualIntensity = (intensity.pow(1.5f)).coerceAtLeast(0.05f)

        val shiftX = (random.nextFloat() - 0.5f) * 60f * visualIntensity
        val shiftY = (random.nextFloat() - 0.5f) * 20f * visualIntensity

        withTransform({ translate(left = shiftX, top = shiftY) }) {
            this@drawWithContent.drawContent()
            drawRect(color = Color.Cyan.copy(alpha = 0.4f * visualIntensity), blendMode = BlendMode.Screen)
        }

        withTransform({ translate(left = -shiftX, top = -shiftY) }) {
            this@drawWithContent.drawContent()
            drawRect(color = Color.Magenta.copy(alpha = 0.4f * visualIntensity), blendMode = BlendMode.Screen)
        }

        if (random.nextFloat() < visualIntensity * 1.8f) {
            repeat((3 * visualIntensity + 1).toInt()) {
                val sliceY = random.nextFloat() * size.height
                val sliceHeight = random.nextFloat() * 50.dp.toPx()
                val sliceShift = (random.nextFloat() - 0.5f) * 100f * visualIntensity
                
                withTransform({
                    translate(left = sliceShift)
                    clipRect(top = sliceY, bottom = sliceY + sliceHeight)
                }) {
                    this@drawWithContent.drawContent()
                }
            }
        }

        val jitterX = (random.nextFloat() - 0.5f) * 8f * visualIntensity
        withTransform({ translate(left = jitterX) }) {
            this@drawWithContent.drawContent()
        }
    }
}

@Composable
fun PerspectiveGrid(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "GridAnim")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Offset"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val gridSpacing = 50.dp.toPx()
        val color = Color(0xFF00FF9C).copy(alpha = 0.015f)
        
        val centerX = size.width / 2f
        val horizonY = size.height * 0.25f
        
        for (i in -15..15) {
            val f = i.toFloat()
            val xStart = centerX + f * gridSpacing * 0.5f
            drawLine(
                color = color,
                start = Offset(xStart, horizonY),
                end = Offset(centerX + f * gridSpacing * 4f, size.height),
                strokeWidth = 1f
            )
        }

        val movingOffset = offset * gridSpacing
        var y = horizonY + movingOffset
        while (y < size.height) {
            val ratio = ((y - horizonY) / (size.height - horizonY)).coerceIn(0f, 1f)
            drawLine(
                color = color.copy(alpha = 0.015f * ratio),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += (gridSpacing * ratio * 2.5f).coerceAtLeast(10f)
        }
    }
}

@Composable
fun StaticNoise(intensity: Float = 0.1f) {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(intensity) {
        while (true) {
            withFrameNanos { tick = it }
            val frameDelay = when {
                intensity < 0.2f -> 400L 
                intensity < 0.4f -> 150L
                else -> 16L
            }
            delay(frameDelay)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val random = Random(tick)
        val dotCount = (1200 * intensity.pow(2.5f)).toInt().coerceAtLeast(8)
        val opacity = (intensity.pow(2f) * 0.7f).coerceIn(0.02f, 0.9f)
        
        repeat(dotCount) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val w = random.nextFloat() * 4.dp.toPx() + 0.5.dp.toPx()
            
            val color = when(random.nextInt(12)) {
                0 -> Color.Cyan
                1 -> Color.Magenta
                else -> Color.White.copy(alpha = 0.6f)
            }
            
            drawRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(w, 1.5f),
                alpha = random.nextFloat() * opacity
            )
        }
    }
}

@Composable
fun Vignette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                center = center,
                radius = size.maxDimension / 1.1f
            )
        )
    }
}

@Composable
fun Scanlines(modifier: Modifier = Modifier, intensity: Float = 0.1f) {
    val infiniteTransition = rememberInfiniteTransition(label = "Scanlines")
    val scrollY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Scroll"
    )
    
    var flickerAlpha by remember { mutableFloatStateOf(0.02f) }
    LaunchedEffect(intensity) {
        while(true) {
            val baseFlicker = 0.02f + (intensity.pow(3) * 0.5f)
            flickerAlpha = (baseFlicker + Random.nextFloat() * 0.03f).coerceIn(0.02f, 0.6f)
            delay(Random.nextLong(60, 300))
        }
    }

    Canvas(modifier = modifier.fillMaxSize().alpha(flickerAlpha)) {
        val lineSpacing = 8.dp.toPx()
        var y = scrollY * lineSpacing
        while (y < size.height) {
            drawLine(
                color = Color.Black,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.5f
            )
            y += lineSpacing
        }
    }
}

@Composable
fun CyberGridBackground(color: Color = Color(0xFF00FF9C).copy(alpha = 0.03f)) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSpacing = 40.dp.toPx()
        
        var x = 0f
        while (x < size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += gridSpacing
        }
        
        var y = 0f
        while (y < size.height) {
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += gridSpacing
        }
    }
}

@Composable
fun GlitchOverlay(intensity: Float = 0.05f) {
    var glitchTrigger by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(intensity) {
        while(true) {
            val baseDelay = when {
                intensity < 0.3f -> 7000L
                intensity < 0.6f -> 3000L
                else -> 800L
            }
            delay(Random.nextLong(baseDelay, baseDelay * 2))
            glitchTrigger++
            delay(Random.nextLong(30, 80))
            glitchTrigger++
        }
    }

    if (glitchTrigger % 2 != 0) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val random = Random(intensity.hashCode())
            val count = (random.nextInt(3, 8) * (intensity.coerceAtLeast(0.15f) * 6f)).toInt().coerceAtLeast(1)
            repeat(count) {
                val ry = random.nextFloat() * size.height
                val rh = random.nextFloat() * 40f + 2f
                val rw = size.width * (random.nextFloat() * 0.7f + 0.05f)
                val rx = if (random.nextBoolean()) 0f else size.width - rw
                
                val color = when(random.nextInt(4)) {
                    0 -> Color(0xFF00FF9C)
                    1 -> Color(0xFFFF006E)
                    2 -> Color(0xFF00FFFF)
                    else -> Color.White
                }
                
                drawRect(
                    color = color,
                    topLeft = Offset(rx, ry),
                    size = Size(rw, rh),
                    alpha = 0.35f
                )
            }
        }
    }
}

@Composable
fun FloatingParticles(intensity: Float = 0.2f) {
    val infiniteTransition = rememberInfiniteTransition(label = "Particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Time"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha((0.1f + intensity * 0.2f).coerceIn(0f, 1f))) {
        val count = (15 + intensity * 60).toInt()
        repeat(count) { i ->
            val r = Random(i + 42)
            val xBase = r.nextFloat() * size.width
            val yBase = r.nextFloat() * size.height
            val speed = r.nextFloat() * 80f + 20f + intensity * 300f
            
            val x = xBase
            val y = (yBase - time * speed) % size.height
            val particleSize = r.nextFloat() * 6f + 1f
            
            drawRect(
                color = Color(0xFF00FF9C),
                topLeft = Offset(x, if (y < 0) y + size.height else y),
                size = Size(particleSize, particleSize),
                alpha = 0.25f
            )
        }
    }
}
