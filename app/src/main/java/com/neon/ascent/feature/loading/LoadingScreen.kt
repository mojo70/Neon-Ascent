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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.ui.*
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.random.Random

@OptIn(ExperimentalTextApi::class)
@Composable
fun LoadingScreen(onLoadingFinished: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    
    val infiniteTransition = rememberInfiniteTransition(label = "LoadingAtmosphere")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(4500, easing = LinearOutSlowInEasing)
        )
        delay(500)
        onLoadingFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010203)),
        contentAlignment = Alignment.Center
    ) {
        // 1. ATMOSPHERIC BACKGROUND
        PerspectiveGrid()
        val displayLoad = progress.value * 0.8f
        Scanlines(intensity = displayLoad)
        StaticNoise(intensity = displayLoad)
        Vignette()
        
        // 2. VERTICAL NEON TEXT
        Canvas(modifier = Modifier.fillMaxSize()) {
            val primaryColor = Color(0xFF00FF9C)
            val secondaryColor = Color(0xFFFF006E)
            
            val textStyle = TextStyle(
                fontSize = 84.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 16.sp
            )

            val layoutTop = textMeasurer.measure("NEON", textStyle.copy(color = primaryColor))
            val layoutBottom = textMeasurer.measure("ASCENT", textStyle.copy(color = secondaryColor))
            
            val spacingPx = 60.dp.toPx()
            val totalTextWidth = layoutTop.size.width.toFloat() + spacingPx + layoutBottom.size.width.toFloat()
            val xPadding = 80.dp.toPx()
            val centerY = size.height / 2f

            withTransform({
                // Move to left side with padding, vertically centered
                translate(left = xPadding, top = centerY)
                // Rotate to make text vertical (pointing UP)
                rotate(-90f, pivot = Offset.Zero)
            }) {
                // In this local coordinate system, text is drawn along the X-axis (screen UP).
                // Center the text block horizontally on the screen's vertical center.
                val startX = -totalTextWidth / 2f
                val startY = -layoutTop.size.height.toFloat() / 2f

                // Layered Bloom for "NEON"
                for (i in 0..8) {
                    val f = i.toFloat()
                    drawText(
                        textLayoutResult = layoutTop,
                        color = primaryColor.copy(alpha = (0.15f - f * 0.015f).coerceAtLeast(0f) * pulse),
                        topLeft = Offset(startX, startY),
                        drawStyle = Stroke(width = 12f + f * 6f)
                    )
                }
                drawText(layoutTop, topLeft = Offset(startX, startY))
                
                // Layered Bloom for "ASCENT"
                val secondPartX = startX + layoutTop.size.width.toFloat() + spacingPx
                for (i in 0..8) {
                    val f = i.toFloat()
                    drawText(
                        textLayoutResult = layoutBottom,
                        color = secondaryColor.copy(alpha = (0.15f - f * 0.015f).coerceAtLeast(0f) * pulse),
                        topLeft = Offset(secondPartX, startY),
                        drawStyle = Stroke(width = 12f + f * 6f)
                    )
                }
                drawText(layoutBottom, topLeft = Offset(secondPartX, startY))
            }
        }

        // 3. ANIMATED NEURAL JACK
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Canvas(modifier = Modifier.size(300.dp)) {
                val colorPrimary = Color(0xFF00FF9C)
                val colorSecondary = Color(0xFFFF006E)
                val strokeWidth = 3.dp.toPx()
                
                val portY = center.y - 40.dp.toPx()
                val portWidth = 80.dp.toPx()
                val portHeight = 50.dp.toPx()
                val portRect = Rect(center.x - portWidth / 2, portY - portHeight / 2, center.x + portWidth / 2, portY + portHeight / 2)

                // PORT GLOW
                for (i in 0..6) {
                    val f = i.toFloat()
                    drawRoundRect(
                        color = colorPrimary.copy(alpha = (0.2f - f * 0.03f).coerceAtLeast(0f) * pulse),
                        topLeft = Offset(portRect.left - f * 4, portRect.top - f * 4),
                        size = Size(portRect.width + f * 8, portRect.height + f * 8),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx() + f * 3f)
                    )
                }
                drawRoundRect(colorPrimary, portRect.topLeft, portRect.size, CornerRadius(4.dp.toPx()), Stroke(strokeWidth))

                // PLUG MOVEMENT
                val startY = size.height + 100.dp.toPx()
                val targetY = portRect.bottom - 10.dp.toPx()
                val currentPlugY = startY - (startY - targetY) * (progress.value * 1.1f).coerceIn(0f, 1f)
                
                val plugWidth = 50.dp.toPx()
                val plugHeight = 80.dp.toPx()
                val plugRect = Rect(center.x - plugWidth / 2, currentPlugY, center.x + plugWidth / 2, currentPlugY + plugHeight)

                // CABLE (Dynamic flow)
                val cablePath = Path().apply {
                    moveTo(center.x, plugRect.bottom)
                    quadraticBezierTo(center.x + 20.dp.toPx() * (1f - progress.value), size.height, center.x, size.height + 200f)
                }
                drawPath(cablePath, colorSecondary, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                
                // Cable glow
                if (progress.value > 0.5f) {
                    drawPath(cablePath, colorSecondary.copy(alpha = 0.3f * pulse), style = Stroke(width = 20.dp.toPx()))
                }

                // PLUG BODY
                drawRect(colorSecondary, plugRect.topLeft, plugRect.size, style = Fill)
                drawRect(Color.White.copy(alpha = 0.4f), plugRect.topLeft, plugRect.size, style = Stroke(width = 2f))

                // CONNECTION BURST
                if (progress.value > 0.85f) {
                    val burstProgress = (progress.value - 0.85f) / 0.15f
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.White, colorPrimary.copy(alpha = 0.5f), Color.Transparent),
                            center = portRect.center,
                            radius = burstProgress * 150.dp.toPx()
                        ),
                        radius = burstProgress * 150.dp.toPx(),
                        center = portRect.center,
                        alpha = (1f - burstProgress).coerceIn(0f, 1f)
                    )
                    
                    // Particles
                    val r = Random(42)
                    repeat(15) {
                        val angle = r.nextFloat() * 360f
                        val dist = burstProgress * 120.dp.toPx() * r.nextFloat()
                        val px = portRect.center.x + kotlin.math.cos(angle) * dist
                        val py = portRect.center.y + kotlin.math.sin(angle) * dist
                        drawCircle(colorPrimary, radius = 2f, center = Offset(px, py), alpha = 1f - burstProgress)
                    }
                }
            }
        }

        // 4. TYPING STATUS TEXT
        val fullStatusText = "ESTABLISHING NEURAL LINK... [OK]"
        val typingCount = (progress.value * fullStatusText.length * 1.5f).toInt().coerceIn(0, fullStatusText.length)
        val currentStatus = fullStatusText.take(typingCount)

        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = currentStatus,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (progress.value > 0.9f) Color(0xFF00FF9C) else Color(0xFFFF006E),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    shadow = Shadow(
                        color = (if (progress.value > 0.9f) Color(0xFF00FF9C) else Color(0xFFFF006E)).copy(alpha = 0.5f),
                        blurRadius = 8f
                    )
                )
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Disguised progress bar
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                val progressColor = if (progress.value > 0.9f) Color(0xFF00FF9C) else Color(0xFFFF006E)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .fillMaxHeight()
                        .background(progressColor)
                        .neonBorder(progressColor, width = 1.dp, cornerRadius = 0.dp)
                )
            }
        }
        
        // Final Glitch Burst
        if (progress.value > 0.95f) {
            GlitchOverlay(intensity = 0.8f)
        }
    }
}
