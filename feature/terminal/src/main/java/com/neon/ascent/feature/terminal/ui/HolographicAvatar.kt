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
    onLevelUp: (SpecialType, Int) -> Unit = { _, _ -> }
) {
    val levelUpService = rememberLevelUpService()
    val previousValues = remember { mutableStateMapOf<SpecialType, Int>() }

    // Detect level-ups across all attributes
    specialAttributes.forEach { (type, current) ->
        val previous = previousValues[type]
        val currentPercentile = current.percentile ?: 50
        if (previous != null && currentPercentile > previous + 7) {
            levelUpService.triggerLevelUp(currentPercentile - previous)
            onLevelUp(type, currentPercentile)
        }
        previousValues[type] = currentPercentile
    }

    val totalPower = if (specialAttributes.isEmpty()) 0.5f else {
        specialAttributes.values.sumOf { it.percentile ?: 50 }.toFloat() / (specialAttributes.size * 100f)
    }
    
    val globalIntensity by animateFloatAsState(
        targetValue = totalPower.coerceIn(0.3f, 1f),
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "globalIntensity"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Base holographic figure
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBaseHologram(globalIntensity)
        }

        // Multi-attribute reactive particles
        MultiAttributeParticleSystem(
            attributes = specialAttributes,
            globalIntensity = globalIntensity,
            modifier = Modifier.fillMaxSize()
        )

        // Center Icon Placeholder (Reactive to dominant attribute)
        val dominantType = specialAttributes.maxByOrNull { it.value.percentile ?: 0 }?.key ?: SpecialType.INTELLIGENCE
        val neonColor = getNeonColorForAttribute(dominantType)
        
        Text(
            text = dominantType.getIcon(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 60.sp),
            color = neonColor.copy(alpha = 0.4f + globalIntensity * 0.6f)
        )
    }
}

private fun DrawScope.drawBaseHologram(intensity: Float) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.minDimension / 3

    // Inner glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(NeonCyan.copy(alpha = 0.2f * intensity), Color.Transparent),
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

@Composable
private fun MultiAttributeParticleSystem(
    attributes: Map<SpecialType, SpecialAttribute>,
    globalIntensity: Float,
    modifier: Modifier = Modifier
) {
    val particles = remember { mutableStateListOf<AttributeParticle>() }

    LaunchedEffect(globalIntensity) {
        while (true) {
            val targetCount = (globalIntensity * 45).toInt().coerceIn(12, 60)
            if (particles.size < targetCount) {
                // Pick a type to represent based on its percentile weight
                val types = attributes.keys.toList().ifEmpty { listOf(SpecialType.INTELLIGENCE) }
                val randomType = types.random()
                particles.add(AttributeParticle.random(randomType))
            }
            delay(16)
        }
    }

    Canvas(modifier = modifier) {
        particles.forEach { p ->
            p.update()
            drawAttributeParticle(p)
        }
        particles.removeAll { it.alpha <= 0.05f }
    }
}

private data class AttributeParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    var size: Float,
    val color: Color
) {
    companion object {
        fun random(type: SpecialType): AttributeParticle {
            return AttributeParticle(
                x = Random.nextFloat() * 800f - 200f,
                y = Random.nextFloat() * 600f,
                vx = Random.nextFloat() * 1.5f - 0.75f,
                vy = -Random.nextFloat() * 2.5f - 0.5f,
                alpha = Random.nextFloat() * 0.8f + 0.4f,
                size = Random.nextFloat() * 4f + 2f,
                color = getNeonColorForAttribute(type)
            )
        }
    }

    fun update() {
        x += vx
        y += vy
        vy += 0.07f
        alpha -= 0.015f
        size *= 0.985f
    }
}

private fun DrawScope.drawAttributeParticle(p: AttributeParticle) {
    drawCircle(
        color = p.color.copy(alpha = p.alpha),
        radius = p.size,
        center = Offset(p.x, p.y)
    )
    drawCircle(
        color = p.color.copy(alpha = p.alpha * 0.3f),
        radius = p.size * 2.2f,
        center = Offset(p.x, p.y)
    )
}
