package com.neon.ascent.feature.terminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun SpecialAttributeCard(
    attribute: SpecialAttribute,
    glowAlpha: Float,
    onClick: () -> Unit
) {
    val neonColor = getNeonColorForAttribute(attribute.type)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.15f),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F001A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            neonColor.copy(alpha = 0.08f * glowAlpha),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Attribute Name + Icon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = attribute.type.getIcon(),
                        style = MaterialTheme.typography.titleLarge,
                        color = neonColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = attribute.type.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Value + Percentile
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = attribute.currentValue.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = neonColor,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${attribute.percentile ?: "--"}th percentile",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                // XP Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { (attribute.currentValue / 10f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                        color = neonColor,
                        trackColor = Color.DarkGray.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${attribute.totalXp} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

fun getNeonColorForAttribute(type: SpecialType): Color = when (type) {
    SpecialType.STRENGTH -> NeonRed
    SpecialType.PERCEPTION -> NeonPurple
    SpecialType.ENDURANCE -> NeonGreen
    SpecialType.CHARISMA -> NeonPink
    SpecialType.INTELLIGENCE -> NeonCyan
    SpecialType.AGILITY -> NeonBlue
    SpecialType.LUCK -> NeonYellow
}
