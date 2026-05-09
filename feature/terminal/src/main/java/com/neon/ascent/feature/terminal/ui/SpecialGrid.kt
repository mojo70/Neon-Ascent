package com.neon.ascent.feature.terminal.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neon.ascent.core.common.*
import com.neon.ascent.core.domain.model.SpecialAttribute
import com.neon.ascent.core.domain.model.SpecialType

@Composable
fun SpecialGrid(
    specialAttributes: Map<SpecialType, SpecialAttribute>,
    modifier: Modifier = Modifier,
    onAttributeClick: (SpecialType) -> Unit = {}
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(SpecialType.entries) { type ->
            val attribute = specialAttributes[type] ?: defaultAttribute(type)
            val glowAlpha by animateFloatAsState(
                targetValue = (attribute.percentile ?: 50) / 100f,
                animationSpec = tween(600),
                label = "glowAlpha"
            )

            SpecialAttributeCard(
                attribute = attribute,
                glowAlpha = glowAlpha,
                onClick = { onAttributeClick(type) }
            )
        }
    }
}

private fun defaultAttribute(type: SpecialType) = SpecialAttribute(
    type = type,
    currentValue = 5,
    percentile = 50
)
