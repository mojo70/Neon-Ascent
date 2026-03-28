package com.neon.ascent.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.random.Random

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
        val alphaVal = ((0.25f - f * 0.03f).coerceAtLeast(0f) * glowIntensity).coerceIn(0f, 1f)
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
 * - Reactive: Glitch violence scales exponentially with intensity.
 * - Silent: Completely stops when intensity is 0.
 */
fun Modifier.cyberGlitch(
    intensity: Float = 0f
) = this.composed {
    if (intensity <= 0f) return@composed this

    var tick by remember { mutableLongStateOf(0L) }
    
    // Tick control: Frequency increases with intensity
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
        
        // Load-based Stress: Probability scales cubed
        val loadProb = (intensity.pow(3f) * 0.9f).coerceIn(0f, 0.95f)
        val isGlitching = random.nextFloat() < loadProb

        if (!isGlitching) {
            drawContent()
            return@drawWithContent
        }

        // Glitch magnitude
        val visualIntensity = (intensity.pow(1.5f)).coerceAtLeast(0.05f)

        // --- APPLY GLITCH TRANSFORMS ---
        val shiftX = (random.nextFloat() - 0.5f) * 60f * visualIntensity
        val shiftY = (random.nextFloat() - 0.5f) * 20f * visualIntensity

        // 1. Color splits
        withTransform({ translate(left = shiftX, top = shiftY) }) {
            this@drawWithContent.drawContent()
            drawRect(color = Color.Cyan.copy(alpha = 0.4f * visualIntensity), blendMode = BlendMode.Screen)
        }

        withTransform({ translate(left = -shiftX, top = -shiftY) }) {
            this@drawWithContent.drawContent()
            drawRect(color = Color.Magenta.copy(alpha = 0.4f * visualIntensity), blendMode = BlendMode.Screen)
        }

        // 2. Horizontal Slice Distortion
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

        // 3. Overall HUD Jitter
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
        // Baseline noise always present but sparse at low load
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
        
        // Random background "pop" bands occur rarely even at low load
        if (random.nextFloat() < 0.015f + (intensity * 0.1f)) {
             drawRect(
                color = Color.White,
                topLeft = Offset(0f, random.nextFloat() * size.height),
                size = Size(size.width, 2.dp.toPx()),
                alpha = 0.05f.coerceAtLeast(intensity * 0.1f)
            )
        }

        // Heavy bands at high stress
        if (intensity > 0.6f && random.nextFloat() < (intensity - 0.5f) * 1.5f) {
            repeat(((intensity - 0.5f) * 10).toInt().coerceAtLeast(1)) {
                val bandY = random.nextFloat() * size.height
                val bandHeight = random.nextFloat() * 10.dp.toPx() + 1.dp.toPx()
                val bandAlpha = random.nextFloat() * intensity * 0.2f
                drawRect(
                    color = Color.White,
                    topLeft = Offset(0f, bandY),
                    size = Size(size.width, bandHeight),
                    alpha = bandAlpha
                )
            }
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
        
        if (intensity > 0.8f && Random.nextFloat() < (intensity - 0.75f)) {
            drawRect(
                color = Color.Black,
                topLeft = Offset(0f, Random.nextFloat() * size.height),
                size = Size(size.width, 60.dp.toPx()),
                alpha = (0.15f + intensity * 0.2f).coerceIn(0f, 0.5f)
            )
        }
    }
}

@Composable
fun CyberGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSpacing = 40.dp.toPx()
        val color = Color(0xFF00FF9C).copy(alpha = 0.03f)
        
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
            // Ambient pops trigger even at low load, but density scales
            val baseDelay = when {
                intensity < 0.3f -> 7000L // 7s avg pop at low load
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
