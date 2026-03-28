package com.neon.ascent.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Reusable neon glow border modifier.
 * Stack multiple Stroke calls with decreasing alpha and increasing width.
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

    // 1. Multiple outer glow layers
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

    // 2. Main bright border
    drawRoundRect(
        color = color,
        cornerRadius = cr,
        style = Stroke(width = widthPx + 2f)
    )

    // 3. Inner highlight
    drawRoundRect(
        color = Color.White.copy(alpha = (0.5f * glowIntensity).coerceIn(0f, 1f)),
        cornerRadius = cr,
        style = Stroke(width = 1.5f)
    )
}

@Composable
fun PerspectiveGrid(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "GridAnim")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Offset"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val gridSpacing = 40.dp.toPx()
        val color = Color(0xFF00FF9C).copy(alpha = 0.05f)
        
        val centerX = size.width / 2f
        val horizonY = size.height * 0.2f
        
        // Vertical perspective lines
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

        // Horizontal moving lines
        val movingOffset = offset * gridSpacing
        var y = horizonY + movingOffset
        while (y < size.height) {
            val ratio = ((y - horizonY) / (size.height - horizonY)).coerceIn(0f, 1f)
            drawLine(
                color = color.copy(alpha = 0.05f * ratio),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += (gridSpacing * ratio * 2f).coerceAtLeast(10f)
        }
    }
}

@Composable
fun StaticNoise(intensity: Float = 0.1f) {
    val infiniteTransition = rememberInfiniteTransition(label = "Static")
    val seed by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Seed"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha((intensity * 0.3f).coerceIn(0f, 1f))) {
        val random = Random((seed * 1000f).toInt())
        
        // Random dots/short lines
        val count = (150 * intensity).toInt().coerceAtLeast(30)
        repeat(count) {
            val x = random.nextFloat() * size.width
            val y = random.nextFloat() * size.height
            val w = random.nextFloat() * (50f + intensity * 100f)
            drawRect(
                color = Color.White.copy(alpha = random.nextFloat() * 0.4f),
                topLeft = Offset(x, y),
                size = Size(w, 1f)
            )
        }
        
        // Occasional horizontal bands
        if (random.nextFloat() < intensity) {
            val bandY = random.nextFloat() * size.height
            val bandHeight = random.nextFloat() * 20f + 2f
            drawRect(
                color = Color.White.copy(alpha = 0.05f * intensity),
                topLeft = Offset(0f, bandY),
                size = Size(size.width, bandHeight)
            )
        }
    }
}

@Composable
fun Vignette(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                center = center,
                radius = size.maxDimension / 1.1f
            )
        )
    }
}

@Composable
fun Scanlines(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "Scanlines")
    val scrollY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Scroll"
    )

    Canvas(modifier = modifier.fillMaxSize().alpha(0.12f)) {
        val lineSpacing = 4.dp.toPx()
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
fun CyberGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val gridSpacing = 30.dp.toPx()
        val color = Color(0xFF00FF9C).copy(alpha = 0.08f)
        
        // Vertical lines
        var x = 0f
        while (x < size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += gridSpacing
        }
        
        // Horizontal lines
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
            val baseDelay = (2500 / (intensity * 10f).coerceAtLeast(1f)).toLong()
            delay(Random.nextLong(baseDelay, baseDelay * 2))
            glitchTrigger++
            delay(Random.nextLong(50, 200))
            glitchTrigger++
        }
    }

    if (glitchTrigger % 2 != 0) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = (Random.nextInt(4, 10) * (1f + intensity * 6f)).toInt()
            repeat(count) {
                val y = Random.nextFloat() * size.height
                val height = Random.nextFloat() * 30f + 2f
                val width = size.width * (Random.nextFloat() * 0.6f + 0.2f)
                val x = if (Random.nextBoolean()) 0f else size.width - width
                
                val color = when(Random.nextInt(3)) {
                    0 -> Color(0xFF00FF9C).copy(alpha = 0.5f + intensity * 0.4f)
                    1 -> Color(0xFFFF006E).copy(alpha = 0.5f + intensity * 0.4f)
                    else -> Color.White.copy(alpha = 0.4f + intensity * 0.4f)
                }
                
                drawRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(width, height)
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
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Time"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha((0.25f + intensity * 0.4f).coerceIn(0f, 1f))) {
        val count = (25 + intensity * 60).toInt()
        repeat(count) { i ->
            val r = Random(i + 42)
            val xBase = r.nextFloat() * size.width
            val yBase = r.nextFloat() * size.height
            val speed = r.nextFloat() * 120f + 40f + intensity * 250f
            
            val x = xBase
            val y = (yBase - time * speed) % size.height
            val particleSize = r.nextFloat() * 5f + 1f
            
            drawRect(
                color = Color(0xFF00FF9C).copy(alpha = 0.5f),
                topLeft = Offset(x, if (y < 0) y + size.height else y),
                size = Size(particleSize, particleSize)
            )
        }
    }
}
