package com.neon.ascent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CyberButtonShape = GenericShape { size, _ ->
    moveTo(0f, 12f)
    lineTo(12f, 0f)
    lineTo(size.width - 24f, 0f)
    lineTo(size.width, 24f)
    lineTo(size.width, size.height - 12f)
    lineTo(size.width - 12f, size.height)
    lineTo(24f, size.height)
    lineTo(0f, size.height - 24f)
    close()
}

@Composable
fun CyberFrame(
    label: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFFF006E),
    borderColor: Color = Color(0xFF00FF9C),
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Label with side indicator and glow
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Box(
                Modifier
                    .size(4.dp, 16.dp)
                    .background(accentColor)
                    .neonBorder(accentColor, width = 1.dp, glowIntensity = 0.8f, cornerRadius = 0.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = borderColor,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
            Spacer(Modifier.width(8.dp))
            // Decorative line
            Box(
                Modifier
                    .height(1.dp)
                    .weight(1f)
                    .background(borderColor.copy(alpha = 0.3f))
            )
        }
        
        // Main container with layered neon border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .neonBorder(borderColor.copy(alpha = 0.6f), width = 1.dp, cornerRadius = 4.dp)
                .background(Color.Black.copy(alpha = 0.2f))
                .padding(12.dp)
        ) {
            content()
        }
    }
}

@Composable
fun HudCornerAccents(color: Color) {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
        val lineLen = 24.dp.toPx()
        val thickness = 2.dp.toPx()
        
        // Top Left
        drawLine(color, Offset(0f, 0f), Offset(lineLen, 0f), thickness)
        drawLine(color, Offset(0f, 0f), Offset(0f, lineLen), thickness)
        
        // Top Right
        drawLine(color, Offset(size.width, 0f), Offset(size.width - lineLen, 0f), thickness)
        drawLine(color, Offset(size.width, 0f), Offset(size.width, lineLen), thickness)
        
        // Bottom Left
        drawLine(color, Offset(0f, size.height), Offset(lineLen, size.height), thickness)
        drawLine(color, Offset(0f, size.height), Offset(0f, size.height - lineLen), thickness)
        
        // Bottom Right
        drawLine(color, Offset(size.width, size.height), Offset(size.width - lineLen, size.height), thickness)
        drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - lineLen), thickness)
    }
}
