package com.neon.ascent.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    accentColor: Color = Color(0xFFFF006E),
    borderColor: Color = Color(0xFF00FF9C),
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp, 16.dp)
                    .background(accentColor)
                    .neonBorder(accentColor, width = 1.dp, glowIntensity = 0.5f, cornerRadius = 0.dp)
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.labelLarge.copy(
                    color = borderColor,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .neonBorder(borderColor.copy(alpha = 0.6f), width = 1.dp, cornerRadius = 2.dp)
                .background(borderColor.copy(alpha = 0.02f))
                .padding(16.dp)
        ) {
            content()
        }
    }
}
