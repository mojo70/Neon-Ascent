package com.neon.ascent.feature.loading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.feature.biohacking.AiType
import com.neon.ascent.ui.*
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalTextApi::class)
@Composable
fun LoadingScreen(
    onLoadingFinished: () -> Unit,
    viewModel: LoadingViewModel = hiltViewModel()
) {
    val progress = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    val aiType by viewModel.activeAiType.collectAsState()
    val saying by viewModel.randomSaying.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "LoadingAtmosphere")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    var flicker by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(Unit) {
        while(true) {
            flicker = if (Random.nextFloat() > 0.95f) 0.7f else 1f
            delay(Random.nextLong(50, 150))
        }
    }

    LaunchedEffect(Unit) {
        // Step 1: Initialize AI
        progress.animateTo(0.2f, tween(800, easing = LinearOutSlowInEasing))
        viewModel.initializeAi()
        
        // Step 2: More loading
        progress.animateTo(0.6f, tween(2500, easing = LinearOutSlowInEasing))
        
        // Finalize
        progress.animateTo(1.0f, tween(1500, easing = LinearOutSlowInEasing))
        delay(1000)
        onLoadingFinished()
    }

    val statusText = when {
        progress.value < 0.2f -> "SCANNING_NEURAL_HARDWARE..."
        progress.value < 0.5f -> {
            if (aiType == AiType.LOCAL) "LOCAL_AI_CORE_READY (GEMINI_NANO)" 
            else if (aiType == AiType.CLOUD) "CLOUD_LINK_ESTABLISHED (GEMINI_FLASH)" 
            else "CHECKING_AI_CAPABILITIES..."
        }
        progress.value < 0.8f -> "SYNCING_BIOMETRIC_DATA..."
        progress.value < 0.95f -> "STABILIZING_NEURAL_LINK..."
        else -> "NEURAL_LINK_COMPLETE [OK]"
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
        StaticNoise(intensity = displayLoad * 0.5f)
        Vignette()
        
        // 2. VERTICAL NEON TEXT
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cyanColor = Color(0xFF00FF9C)
            val magentaColor = Color(0xFFFF006E)
            
            val textStyle = TextStyle(
                fontSize = 52.sp, 
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 36.sp,
                fontStyle = FontStyle.Italic
            )

            val layoutTop = textMeasurer.measure("NEON", textStyle.copy(color = cyanColor))
            val layoutBottom = textMeasurer.measure("ASCENT", textStyle.copy(color = magentaColor))
            
            val spacingPx = 50.dp.toPx()
            val totalTextWidth = layoutTop.size.width + spacingPx + layoutBottom.size.width
            val xOffset = 70.dp.toPx()
            val centerY = size.height / 2f

            withTransform({
                translate(left = xOffset, top = centerY)
                rotate(-90f, pivot = Offset.Zero)
            }) {
                val startX = -totalTextWidth / 2f
                val startY = -layoutTop.size.height / 2f

                // --- NEON DRAWING ---
                for (i in 0..6) {
                    val f = i.toFloat()
                    drawText(
                        textLayoutResult = layoutTop,
                        color = cyanColor.copy(alpha = (0.12f - f * 0.02f).coerceAtLeast(0f) * pulse * flicker),
                        topLeft = Offset(startX, startY),
                        drawStyle = Stroke(width = 1f + f * 3f)
                    )
                }
                drawText(
                    textLayoutResult = layoutTop,
                    color = magentaColor.copy(alpha = 0.05f * pulse),
                    topLeft = Offset(startX, startY),
                    drawStyle = Stroke(width = 25f)
                )
                drawText(layoutTop, color = Color.White.copy(alpha = 0.95f * flicker), topLeft = Offset(startX, startY))
                drawText(layoutTop, color = cyanColor.copy(alpha = 0.4f), topLeft = Offset(startX, startY))

                // --- ASCENT DRAWING ---
                val secondPartX = startX + layoutTop.size.width + spacingPx
                for (i in 0..6) {
                    val f = i.toFloat()
                    drawText(
                        textLayoutResult = layoutBottom,
                        color = magentaColor.copy(alpha = (0.12f - f * 0.02f).coerceAtLeast(0f) * pulse * flicker),
                        topLeft = Offset(secondPartX, startY),
                        drawStyle = Stroke(width = 1f + f * 3f)
                    )
                }
                drawText(
                    textLayoutResult = layoutBottom,
                    color = cyanColor.copy(alpha = 0.05f * pulse),
                    topLeft = Offset(secondPartX, startY),
                    drawStyle = Stroke(width = 25f)
                )
                drawText(layoutBottom, color = Color.White.copy(alpha = 0.95f * flicker), topLeft = Offset(secondPartX, startY))
                drawText(layoutBottom, color = magentaColor.copy(alpha = 0.4f), topLeft = Offset(secondPartX, startY))
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

                val startY = size.height + 100.dp.toPx()
                val targetY = portRect.bottom - 10.dp.toPx()
                val currentPlugY = startY - (startY - targetY) * (progress.value * 1.1f).coerceIn(0f, 1f)
                
                val plugWidth = 50.dp.toPx()
                val plugHeight = 80.dp.toPx()
                val plugRect = Rect(center.x - plugWidth / 2, currentPlugY, center.x + plugWidth / 2, currentPlugY + plugHeight)

                val cablePath = Path().apply {
                    moveTo(center.x, plugRect.bottom)
                    quadraticBezierTo(center.x + 20.dp.toPx() * (1f - progress.value), size.height, center.x, size.height + 200f)
                }
                drawPath(cablePath, colorSecondary, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                
                if (progress.value > 0.5f) {
                    drawPath(cablePath, colorSecondary.copy(alpha = 0.3f * pulse), style = Stroke(width = 20.dp.toPx()))
                }

                drawRect(colorSecondary, plugRect.topLeft, plugRect.size, style = Fill)
                drawRect(Color.White.copy(alpha = 0.4f), plugRect.topLeft, plugRect.size, style = Stroke(width = 2f))

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

        // 4. RANDOM SAYING & JOURNAL SAVING
        AnimatedVisibility(
            visible = progress.value > 0.3f,
            enter = fadeIn(tween(1000)),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 40.dp)
        ) {
            saying?.let { s ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "\"${s.text}\"",
                        color = Color(0xFF00CCFF),
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontStyle = FontStyle.Italic,
                        textAlign = TextAlign.Center,
                        lineHeight = 26.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "— ${s.category.uppercase()}",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    if (!isSaved) {
                        Button(
                            onClick = { viewModel.saveToJournal() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            modifier = Modifier.border(1.dp, Color(0xFF00FFAA))
                        ) {
                            Text("💾 SAVE_TO_JOURNAL", color = Color(0xFF00FFAA), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    } else {
                        Text("ENTRY_SAVED // [OK]", color = Color(0xFF00FF9C), fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }

        // 5. TYPING STATUS TEXT
        val typingCount = (progress.value * statusText.length * 1.5f).toInt().coerceIn(0, statusText.length)
        val currentStatus = statusText.take(typingCount)

        Column(
            modifier = Modifier.fillMaxSize().padding(bottom = 60.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val statusColor = if (progress.value > 0.9f) Color(0xFF00FF9C) else Color(0xFFFF006E)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currentStatus,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = statusColor,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 1.sp,
                        shadow = Shadow(
                            color = statusColor.copy(alpha = 0.8f),
                            blurRadius = 8f * pulse
                        )
                    )
                )
                
                Spacer(Modifier.width(8.dp))
                
                Text(
                    text = "${(progress.value * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = statusColor.copy(alpha = 0.6f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            // Disguised progress bar
            Box(
                modifier = Modifier
                    .width(240.dp)
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.value)
                        .fillMaxHeight()
                        .background(statusColor)
                        .neonBorder(statusColor, width = 1.dp, cornerRadius = 0.dp)
                )
            }
        }
        
        // Final Glitch Burst
        if (progress.value > 0.95f) {
            GlitchOverlay(intensity = 0.8f)
        }
    }
}
