package com.neon.ascent.feature.terminal.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.unit.dp
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType

@Composable
fun HolographicAvatar(
    specialAttributes: Map<SpecialType, SpecialAttribute>,
    modifier: Modifier = Modifier
) {
    val intelligence = specialAttributes[SpecialType.INTELLIGENCE] ?: SpecialAttribute(
        type = SpecialType.INTELLIGENCE,
        currentValue = 5,
        percentile = 50
    )
    
    val glowIntensity by animateFloatAsState(
        targetValue = (intelligence.percentile ?: 50) / 100f,
        animationSpec = tween(800),
        label = "glowIntensity"
    )

    Box(modifier = modifier) {
        // Base avatar (placeholder for neon outline + particle system)
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.Center)
                .drawWithCache {
                    onDrawBehind {
                        // TODO: Add actual neon drawing logic here
                    }
                }
        ) {
            Text(
                text = "◉",
                style = MaterialTheme.typography.displayLarge,
                color = NeonCyan.copy(alpha = 0.4f + glowIntensity * 0.6f),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // Floating S.P.E.C.I.A.L. badges
        SpecialFloatingBadges(specialAttributes)
    }
}

@Composable
fun SpecialFloatingBadges(specialAttributes: Map<SpecialType, SpecialAttribute>) {
    // Placeholder for floating badges logic
}
