package com.neon.ascent.feature.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.feature.charactercreation.CyberGridBackground
import com.neon.ascent.feature.charactercreation.GlitchOverlay
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(onLoadingFinished: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "LoadingAnimation")
    
    // Animation for the plug jacking in
    val plugOffset by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PlugOffset"
    )

    // Simulate loading time
    LaunchedEffect(Unit) {
        delay(3500) // Show animation for 3.5 seconds
        onLoadingFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020202)),
        contentAlignment = Alignment.Center
    ) {
        CyberGridBackground()
        GlitchOverlay()

        // Sideways Title
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "NEON ASCENT",
                modifier = Modifier.rotate(-90f),
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = Color(0xFF00FF9C),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 12.sp,
                    shadow = Shadow(
                        color = Color(0xFF00FF9C).copy(alpha = 0.7f),
                        blurRadius = 25f
                    )
                )
            )
        }

        // Jack-in Animation
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val colorPrimary = Color(0xFF00FF9C)
                    val colorSecondary = Color(0xFFFF006E)
                    val strokeWidth = 3.dp.toPx()

                    // Port (Stationary)
                    val portSize = Size(60.dp.toPx(), 40.dp.toPx())
                    val portOffset = Offset(center.x - portSize.width / 2, center.y - portSize.height / 2)
                    
                    drawRect(
                        color = colorPrimary,
                        topLeft = portOffset,
                        size = portSize,
                        style = Stroke(width = strokeWidth)
                    )
                    
                    // Internal pins of the port
                    repeat(3) { i ->
                        drawLine(
                            color = colorPrimary.copy(alpha = 0.5f),
                            start = Offset(portOffset.x + (i + 1) * (portSize.width / 4), portOffset.y + 10f),
                            end = Offset(portOffset.x + (i + 1) * (portSize.width / 4), portOffset.y + portSize.height - 10f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Plug (Animated)
                    val plugSize = Size(40.dp.toPx(), 25.dp.toPx())
                    val currentPlugY = center.y + plugOffset.dp.toPx()
                    val plugOffsetFinal = Offset(center.x - plugSize.width / 2, currentPlugY)

                    // Plug body
                    drawRect(
                        color = colorSecondary,
                        topLeft = plugOffsetFinal,
                        size = plugSize
                    )
                    
                    // Cord
                    drawLine(
                        color = colorSecondary.copy(alpha = 0.7f),
                        start = Offset(center.x, plugOffsetFinal.y + plugSize.height),
                        end = Offset(center.x, size.height),
                        strokeWidth = 6.dp.toPx()
                    )
                    
                    // Connection spark when "jacking in"
                    if (plugOffset > -5f) {
                        drawCircle(
                            color = Color.White,
                            radius = (5f + plugOffset).coerceAtLeast(0f) * 2f,
                            center = Offset(center.x, portOffset.y + portSize.height / 2),
                            alpha = (1f + (plugOffset / 5f)).coerceIn(0f, 1f)
                        )
                    }
                }
            }
            
            Text(
                text = "ESTABLISHING NEURAL LINK...",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFFF006E),
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.padding(bottom = 60.dp)
            )
        }
    }
}
