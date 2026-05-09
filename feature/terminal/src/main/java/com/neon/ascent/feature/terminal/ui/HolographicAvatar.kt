package com.neon.ascent.feature.terminal.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun HolographicAvatar(
    specialAttributes: Map<SpecialType, SpecialAttribute>,
    modifier: Modifier = Modifier,
    onLevelUp: (SpecialType) -> Unit = {}
) {
    val intelligence = specialAttributes[SpecialType.INTELLIGENCE] ?: SpecialAttribute(
        type = SpecialType.INTELLIGENCE,
        currentValue = 5,
        percentile = 50
    )
    val previousPercentile = remember { mutableStateOf(intelligence.percentile) }

    val glowIntensity by animateFloatAsState(
        targetValue = (intelligence.percentile ?: 50) / 100f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "glowIntensity"
    )

    // Level-up trigger
    LaunchedEffect(intelligence.percentile) {
        if (intelligence.percentile != null &&
            previousPercentile.value != null &&
            intelligence.percentile!! > previousPercentile.value!! + 5) {
            onLevelUp(SpecialType.INTELLIGENCE)
        }
        previousPercentile.value = intelligence.percentile
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Base holographic figure
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBaseHologram(glowIntensity)
        }

        // Particle system
        ParticleSystem(
            intensity = glowIntensity,
            modifier = Modifier.fillMaxSize()
        )

        // Level-up burst overlay
        LevelUpBurstOverlay(
            trigger = intelligence.percentile ?: 50,
            modifier = Modifier.fillMaxSize()
        )

        // Center Icon Placeholder
        Text(
            text = "◉",
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp),
            color = NeonCyan.copy(alpha = 0.4f + glowIntensity * 0.6f)
        )
    }
}

private fun DrawScope.drawBaseHologram(glowIntensity: Float) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 3

    // Inner glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(NeonCyan.copy(alpha = 0.2f * glowIntensity), Color.Transparent),
            center = center,
            radius = radius * 1.5f
        ),
        radius = radius * 1.5f,
        center = center
    )

    // Scanning ring
    drawCircle(
        color = NeonCyan.copy(alpha = 0.3f),
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )
}

// ==================== PARTICLE SYSTEM ====================

@Composable
private fun ParticleSystem(
    intensity: Float,
    modifier: Modifier = Modifier
) {
    val particles = remember { mutableStateListOf<Particle>() }
    val density = (intensity * 45).toInt().coerceIn(8, 45)

    LaunchedEffect(density) {
        while (true) {
            if (particles.size < density) {
                particles.add(Particle.random())
            }
            delay(16) // ~60fps
        }
    }

    Canvas(modifier = modifier) {
        particles.forEach { particle ->
            particle.update()
            drawParticle(particle)
        }

        // Remove dead particles
        particles.removeAll { it.alpha <= 0f }
    }
}

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    var size: Float,
    val color: Color
) {
    companion object {
        fun random(): Particle {
            return Particle(
                x = Random.nextFloat() * 800f - 200f, // Random start X
                y = Random.nextFloat() * 600f,        // Random start Y
                vx = Random.nextFloat() * 1.2f - 0.6f,
                vy = -Random.nextFloat() * 2.5f - 0.5f, // upward drift
                alpha = Random.nextFloat() * 0.8f + 0.4f,
                size = Random.nextFloat() * 4f + 2f,
                color = listOf(NeonCyan, NeonPink, NeonBlue).random()
            )
        }
    }

    fun update() {
        x += vx
        y += vy
        vy += 0.08f // gravity-like pull down
        alpha -= 0.018f
        size *= 0.985f
    }
}

private fun DrawScope.drawParticle(p: Particle) {
    drawCircle(
        color = p.color.copy(alpha = p.alpha),
        radius = p.size,
        center = Offset(p.x, p.y)
    )
    // Optional neon glow layer
    drawCircle(
        color = p.color.copy(alpha = p.alpha * 0.3f),
        radius = p.size * 2.2f,
        center = Offset(p.x, p.y)
    )
}

@Composable
private fun LevelUpBurstOverlay(
    trigger: Int,
    modifier: Modifier = Modifier
) {
    var burstProgress by remember { mutableFloatStateOf(0f) }
    var showBurst by remember { mutableStateOf(false) }

    LaunchedEffect(trigger) {
        if (trigger > 0 && trigger % 10 == 0) { // every 10 percentile jump
            showBurst = true
            burstProgress = 0f
            // Animate burst progress
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(1000)
            ) { value, _ ->
                burstProgress = value
            }
            showBurst = false
        }
    }

    if (showBurst) {
        Canvas(modifier = modifier) {
            val center = Offset(size.width / 2, size.height / 2)

            // Energy ring
            drawCircle(
                color = NeonCyan.copy(alpha = (1 - burstProgress) * 0.6f),
                radius = 80f + burstProgress * 200f,
                center = center,
                style = Stroke(width = 6f)
            )

            // Particle explosion burst
            for (i in 0..24) {
                val angle = (i * 15f)
                val dist = 40f + burstProgress * 250f
                drawCircle(
                    color = NeonPink.copy(alpha = (1 - burstProgress) * 0.9f),
                    radius = 4f,
                    center = Offset(
                        center.x + dist * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat(),
                        center.y + dist * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
                    )
                )
            }
        }
    }
}
