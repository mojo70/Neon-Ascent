package com.neon.ascent.feature.terminal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.neon.ascent.core.domain.model.CognitiveItem
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpatialRotationView(
    item: CognitiveItem,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = item.question,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Original Shape
        Text("ORIGINAL SHAPE", color = NeonCyan, style = MaterialTheme.typography.labelMedium)
        Canvas(modifier = Modifier.size(120.dp)) {
            drawShape(Offset(size.width / 2, size.height / 2), 0f)
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "WHICH MATCHES AFTER 90° CLOCKWISE ROTATION?", 
            color = NeonPink,
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(Modifier.height(24.dp))

        // Answer Options
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            listOf("A", "B", "C", "D").forEach { option ->
                val isSelected = selectedAnswer == option
                Card(
                    onClick = { onAnswerSelected(option) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) NeonCyan.copy(alpha = 0.3f) else Color(0xFF1A0033)
                    ),
                    modifier = Modifier.size(92.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val rotation = when (option) {
                            "A" -> 90f
                            "B" -> 180f
                            "C" -> 270f
                            else -> 0f
                        }
                        drawShape(Offset(size.width / 2, size.height / 2), rotation)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawShape(center: Offset, rotationDegrees: Float) {
    rotate(rotationDegrees, center) {
        // Draw a distinctive L-shaped polyomino (good for rotation tests)
        val path = Path().apply {
            moveTo(center.x - 35f, center.y - 35f)
            lineTo(center.x + 35f, center.y - 35f)
            lineTo(center.x + 35f, center.y - 10f)
            lineTo(center.x - 10f, center.y - 10f)
            lineTo(center.x - 10f, center.y + 35f)
            lineTo(center.x - 35f, center.y + 35f)
            close()
        }

        drawPath(path, color = NeonCyan)
        drawPath(path, color = NeonPink, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
    }
}
