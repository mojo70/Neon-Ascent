package com.neon.ascent.feature.terminal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.neon.ascent.core.domain.model.CognitiveItem
import com.neon.ascent.core.common.NeonCyan
import com.neon.ascent.core.common.NeonPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatrixQuestionView(
    item: CognitiveItem,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit
) {
    val pattern = remember(item.id) {
        val targetIndex = when (item.correctAnswer.uppercase()) {
            "A" -> 0
            "B" -> 1
            "C" -> 2
            "D" -> 3
            else -> 2
        }
        MatrixSolvabilityTester.generateValidSolvablePattern(item.difficulty, targetIndex)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = item.question,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Main 3x3 Grid
        Canvas(
            modifier = Modifier
                .size(280.dp)
                .padding(8.dp)
        ) {
            drawMatrix(pattern)
        }

        Spacer(Modifier.height(32.dp))

        // Answer Options (as smaller grids)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("A", "B", "C", "D").forEachIndexed { index, option ->
                val isSelected = selectedAnswer == option
                Card(
                    onClick = { onAnswerSelected(option) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) NeonCyan.copy(alpha = 0.3f) else Color(0xFF1A0033)
                    ),
                    modifier = Modifier.size(68.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawMiniMatrix(pattern, index)
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawMatrix(pattern: MatrixPattern) {
    val cellSize = size.width / 3f
    val padding = 8f

    for (row in 0..2) {
        for (col in 0..2) {
            val index = row * 3 + col
            val x = col * cellSize
            val y = row * cellSize

            // Draw cell border
            drawRect(
                color = NeonCyan.copy(alpha = 0.4f),
                topLeft = Offset(x + padding, y + padding),
                size = Size(cellSize - 2 * padding, cellSize - 2 * padding),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )

            if (row == 2 && col == 2) {
                // Missing cell - draw a dashed cybernetic outline
                drawRect(
                    color = NeonPink.copy(alpha = 0.6f),
                    topLeft = Offset(x + padding + 4f, y + padding + 4f),
                    size = Size(cellSize - 2 * padding - 8f, cellSize - 2 * padding - 8f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2f,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )
            } else {
                val cell = pattern.cells.getOrNull(index)
                if (cell != null) {
                    drawCellShape(cell, Offset(x + cellSize / 2f, y + cellSize / 2f), cellSize - 2 * padding - 16f)
                }
            }
        }
    }
}

private fun DrawScope.drawCellShape(cell: CellPattern, center: Offset, maxAvailableSize: Float) {
    val shapeSize = maxAvailableSize * 0.6f * cell.size
    
    withTransform({
        rotate(cell.rotation.toFloat(), center)
    }) {
        when (cell.shape) {
            ShapeType.CIRCLE -> {
                drawCircle(
                    color = cell.color,
                    radius = shapeSize / 2f,
                    center = center
                )
            }
            ShapeType.SQUARE -> {
                drawRect(
                    color = cell.color,
                    topLeft = Offset(center.x - shapeSize / 2f, center.y - shapeSize / 2f),
                    size = Size(shapeSize, shapeSize)
                )
            }
            ShapeType.TRIANGLE -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(center.x, center.y - shapeSize / 2f)
                    lineTo(center.x - shapeSize / 2f, center.y + shapeSize / 2f)
                    lineTo(center.x + shapeSize / 2f, center.y + shapeSize / 2f)
                    close()
                }
                drawPath(path = path, color = cell.color)
            }
            ShapeType.STAR -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    val outerRadius = shapeSize / 2f
                    val innerRadius = outerRadius * 0.4f
                    for (i in 0 until 10) {
                        val angle = i * Math.PI / 5
                        val radius = if (i % 2 == 0) outerRadius else innerRadius
                        val px = center.x + radius * Math.cos(angle)
                        val py = center.y + radius * Math.sin(angle)
                        if (i == 0) moveTo(px.toFloat(), py.toFloat()) else lineTo(px.toFloat(), py.toFloat())
                    }
                    close()
                }
                drawPath(path = path, color = cell.color)
            }
            ShapeType.HEXAGON -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    val radius = shapeSize / 2f
                    for (i in 0 until 6) {
                        val angle = i * Math.PI / 3
                        val px = center.x + radius * Math.cos(angle)
                        val py = center.y + radius * Math.sin(angle)
                        if (i == 0) moveTo(px.toFloat(), py.toFloat()) else lineTo(px.toFloat(), py.toFloat())
                    }
                    close()
                }
                drawPath(path = path, color = cell.color)
            }
        }
    }
}

private fun DrawScope.drawMiniMatrix(pattern: MatrixPattern, optionIndex: Int) {
    val sizeMin = minOf(size.width, size.height)

    // Background border of the card options
    drawRect(
        color = NeonCyan.copy(alpha = 0.2f),
        topLeft = Offset(2f, 2f),
        size = Size(size.width - 4f, size.height - 4f),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
    )

    val cellToDraw = pattern.options.getOrNull(optionIndex)
    if (cellToDraw != null) {
        drawCellShape(cellToDraw, Offset(size.width / 2f, size.height / 2f), sizeMin - 16f)
    }
}
