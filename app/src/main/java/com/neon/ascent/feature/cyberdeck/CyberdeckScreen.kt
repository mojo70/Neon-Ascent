package com.neon.ascent.feature.cyberdeck

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.feature.charactercreation.CyberFrame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val CyberpunkColors = darkColorScheme(
    background = Color(0xFF0A0F0A),     // Deep near-black
    surface = Color(0xFF121A12),
    primary = Color(0xFF00FF9F),        // Neon cyan/green
    secondary = Color(0xFFFF00AA),      // Hot magenta/pink
    tertiary = Color(0xFF00CCFF)        // Bright cyan
)

@Composable
fun CyberdeckScreen(onWalletClick: () -> Unit, tickerMessages: List<String> = emptyList()) {
    MaterialTheme(colorScheme = CyberpunkColors) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Faint Grid Background
            GridBackground()
            
            // Atmospheric Haze/Fog
            HazeEffect()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                HeaderSection()
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Hexagonal Hub Section (Immersive Command Center)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    HexagonalNodeGrid(onWalletClick)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                CyberFrame(label = "CONSOLE_OUTPUT") {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp).padding(8.dp)) {
                        ConsoleOutput()
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp)) // Space for ticker
            }

            // Ticker Tape at the bottom
            if (tickerMessages.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(Color(0xFF0A0A0A).copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.3f))
                ) {
                    TickerTape(messages = tickerMessages)
                }
            }
            
            // Decorative neon borders
            NeonFrameBorders()
        }
    }
}

@Composable
fun GridBackground() {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
        val gridSize = 40.dp.toPx()
        val color = Color(0xFF00FF9F)
        
        for (x in 0 until size.width.toInt() step gridSize.toInt()) {
            drawLine(
                color = color,
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), size.height),
                strokeWidth = 1f
            )
        }
        for (y in 0 until size.height.toInt() step gridSize.toInt()) {
            drawLine(
                color = color,
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
fun HazeEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "HazeAnim")
    val hazeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize().blur(60.dp)) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00CCFF).copy(alpha = hazeAlpha), Color.Transparent),
                center = center,
                radius = size.maxDimension / 1.2f
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xFFFF00AA).copy(alpha = hazeAlpha * 0.4f)),
                startY = size.height * 0.6f,
                endY = size.height
            )
        )
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "CYBERDECK_TERMINAL",
                color = Color(0xFF00FF9F),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            Text(
                "CONNECTED_VIA // NEURAL_GATE_01",
                color = Color(0xFF00FF9F).copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        
        NeonHexagon(
            label = "SEC",
            color = Color(0xFFFF00AA),
            size = 48.dp,
            fontSize = 10.sp
        )
    }
}

@Composable
fun HexagonalNodeGrid(onWalletClick: () -> Unit) {
    Box(modifier = Modifier.size(360.dp)) {
        // Center node
        NeonHexagon(
            label = "CORE_OS", 
            color = Color(0xFF00CCFF), 
            modifier = Modifier.align(Alignment.Center).size(120.dp),
            isPulse = true
        )
        
        // Surrounding Nodes
        NeonHexagon(
            label = "NETWORK", 
            color = Color(0xFF00FF9F), 
            modifier = Modifier.align(Alignment.TopCenter).size(90.dp).offset(y = (-10).dp)
        )
        
        NeonHexagon(
            label = "DATABASE", 
            color = Color(0xFF00CCFF), 
            modifier = Modifier.align(Alignment.BottomCenter).size(90.dp).offset(y = 10.dp)
        )
        
        NeonHexagon(
            label = "EXPLOITS", 
            color = Color(0xFFFF00AA), 
            modifier = Modifier.align(Alignment.CenterStart).size(90.dp).offset(x = (-10).dp)
        )
        
        NeonHexagon(
            label = "WALLET", 
            color = Color(0xFF00CCFF), 
            modifier = Modifier.align(Alignment.CenterEnd).size(90.dp).offset(x = 10.dp),
            onClick = onWalletClick
        )
    }
}

@Composable
fun NeonHexagon(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 11.sp,
    isPulse: Boolean = false,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HexPulse")
    val glowIntensity by if (isPulse) {
        infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowIntensity"
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val path = createHexagonPath(size.toPx() / 2.2f) // Leave space for glow

            // Layer 1: Strong outer glow (blur simulation via multiple thick strokes)
            for (i in 0..8) {
                drawPath(
                    path = path,
                    color = color.copy(alpha = (0.15f / (i + 1)) * glowIntensity),
                    style = Stroke(width = (12f + i * 4f), cap = StrokeCap.Round)
                )
            }

            // Layer 2: Main bright border
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            // Layer 3: Inner highlight for depth
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.4f * glowIntensity),
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )
        }

        Text(
            label,
            color = Color.White,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp,
            modifier = Modifier.alpha(0.9f)
        )
    }
}

private fun DrawScope.createHexagonPath(radius: Float): Path {
    val path = Path()
    val centerX = size.width / 2
    val centerY = size.height / 2
    for (i in 0..5) {
        val angle = (i * 60f) * (PI.toFloat() / 180f) - (PI.toFloat() / 2)
        val x = centerX + radius * cos(angle)
        val y = centerY + radius * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

@Composable
fun ConsoleOutput() {
    val lines = listOf(
        "> INITIALIZING NEURAL_LINK...",
        "> CONNECTION ESTABLISHED.",
        "> WELCOME BACK, EDGE-RUNNER.",
        "> 3 NEW MESSAGES FROM 'THE_FIXER'",
        "> SOLANA_CHAIN: SYNCED",
        "> SCANNING FOR VULNERABILITIES..."
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "Cursor")
    val cursorVisible by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CursorAlpha"
    )

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(lines) { line ->
            TerminalLine(line, Color(0xFF00FF9F))
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "> ",
                    color = Color(0xFF00FF9F),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Box(
                    modifier = Modifier
                        .size(8.dp, 14.dp)
                        .background(Color(0xFF00FF9F).copy(alpha = cursorVisible))
                )
            }
        }
    }
}

@Composable
fun TerminalLine(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
fun NeonFrameBorders() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val magenta = Color(0xFFFF00AA)
        val cyan = Color(0xFF00CCFF)
        
        // Decorative neon bars
        drawLine(
            magenta,
            start = Offset(6.dp.toPx(), 120.dp.toPx()),
            end = Offset(6.dp.toPx(), size.height - 120.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        drawLine(
            cyan,
            start = Offset(size.width - 6.dp.toPx(), 120.dp.toPx()),
            end = Offset(size.width - 6.dp.toPx(), size.height - 120.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun TickerTape(messages: List<String>) {
    val tickerText = messages.joinToString("   [ // ]   ")
    var textWidth by remember { mutableStateOf(0) }
    var containerWidth by remember { mutableStateOf(0) }
    
    val infiniteTransition = rememberInfiniteTransition(label = "TickerTransition")
    val xOffsetPercent by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (textWidth > 0) (textWidth + containerWidth) * 5 else 10000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "TickerOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerWidth = it.width },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = tickerText,
            color = Color(0xFF00FF9F),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .onSizeChanged { textWidth = it.width }
                .offset {
                    val startX = containerWidth.toFloat()
                    val endX = -textWidth.toFloat()
                    val currentX = startX + (endX - startX) * ((1f - xOffsetPercent) / 2f)
                    IntOffset(currentX.toInt(), 0)
                }
        )
    }
}
