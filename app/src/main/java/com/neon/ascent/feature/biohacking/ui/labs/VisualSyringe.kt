package com.neon.ascent.feature.biohacking.ui.labs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.core.domain.labs.ReconstitutionCalculator
import java.math.BigDecimal

@Composable
fun VisualSyringe(
    units: BigDecimal?,
    barrel: ReconstitutionCalculator.SyringeBarrel,
    modifier: Modifier = Modifier,
    cyan: Color = Color(0xFF00F5FF),
    magenta: Color = Color(0xFFFF0088),
    darkBg: Color = Color(0xFF0F1722)
) {
    val capacityUnits = barrel.capacityUnits.toFloat()
    val drawUnits = units?.toFloat() ?: 0f
    val isOverflow = units != null && barrel.isOverflow(units)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "VISUAL_SYRINGE // ${barrel.label}",
                color = cyan,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            if (units != null) {
                Text(
                    text = "${units.toPlainString()} U / ${barrel.capacityUnits.toPlainString()} U",
                    color = if (isOverflow) magenta else cyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            val width = size.width
            val height = size.height

            val needleWidth = 24f
            val barrelLeft = needleWidth + 16f
            val barrelRight = width - 36f
            val barrelWidth = barrelRight - barrelLeft
            val barrelTop = height * 0.25f
            val barrelBottom = height * 0.75f
            val barrelHeight = barrelBottom - barrelTop

            // 1. Needle & Hub on Left
            val needleY = height * 0.5f
            drawLine(
                color = Color.LightGray,
                start = Offset(0f, needleY),
                end = Offset(needleWidth, needleY),
                strokeWidth = 3f
            )
            val hubPath = Path().apply {
                moveTo(needleWidth, needleY - 8f)
                lineTo(barrelLeft, barrelTop + 2f)
                lineTo(barrelLeft, barrelBottom - 2f)
                lineTo(needleWidth, needleY + 8f)
                close()
            }
            drawPath(path = hubPath, color = Color.Gray)

            // 2. Barrel Inner Background
            drawRoundRect(
                color = darkBg,
                topLeft = Offset(barrelLeft, barrelTop),
                size = Size(barrelWidth, barrelHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // 3. Fluid Fill
            val fillFraction = (drawUnits / capacityUnits).coerceIn(0f, 1f)
            val fillWidth = barrelWidth * fillFraction
            if (fillWidth > 0f) {
                val fillColor = if (isOverflow) magenta.copy(alpha = 0.85f) else cyan.copy(alpha = 0.65f)
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(barrelLeft, barrelTop + 2f),
                    size = Size(fillWidth, barrelHeight - 4f),
                    cornerRadius = CornerRadius(2f, 2f)
                )
            }

            // 4. Plunger Stopper & Rod
            val plungerX = barrelLeft + fillWidth
            val stopperWidth = 10f
            drawRoundRect(
                color = if (isOverflow) magenta else cyan,
                topLeft = Offset(plungerX, barrelTop - 2f),
                size = Size(stopperWidth, barrelHeight + 4f),
                cornerRadius = CornerRadius(2f, 2f)
            )

            // Plunger Rod extending to right
            drawLine(
                color = Color.Gray,
                start = Offset(plungerX + stopperWidth, needleY),
                end = Offset(width - 4f, needleY),
                strokeWidth = 6f
            )
            // Plunger flange
            drawLine(
                color = Color.Gray,
                start = Offset(width - 4f, barrelTop - 6f),
                end = Offset(width - 4f, barrelBottom + 6f),
                strokeWidth = 8f
            )

            // 5. Barrel Outer Frame
            drawRoundRect(
                color = cyan,
                topLeft = Offset(barrelLeft, barrelTop),
                size = Size(barrelWidth, barrelHeight),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = 2f)
            )

            // 6. Graduation Tick Marks
            val numTicks = 10
            for (i in 0..numTicks) {
                val tickFraction = i.toFloat() / numTicks
                val tickX = barrelLeft + (barrelWidth * tickFraction)

                drawLine(
                    color = cyan.copy(alpha = 0.8f),
                    start = Offset(tickX, barrelTop),
                    end = Offset(tickX, barrelTop + 10f),
                    strokeWidth = 2f
                )

                if (i < numTicks) {
                    val minorX = barrelLeft + (barrelWidth * (tickFraction + 0.05f))
                    drawLine(
                        color = cyan.copy(alpha = 0.35f),
                        start = Offset(minorX, barrelTop),
                        end = Offset(minorX, barrelTop + 5f),
                        strokeWidth = 1f
                    )
                }
            }
        }
    }
}
