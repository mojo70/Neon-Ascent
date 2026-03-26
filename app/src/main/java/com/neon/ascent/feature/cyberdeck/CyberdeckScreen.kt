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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.feature.charactercreation.CyberFrame
import com.neon.ascent.feature.charactercreation.CyberGridBackground
import kotlin.math.cos
import kotlin.math.sin

val HexagonShape = GenericShape { size, _ ->
    val radius = size.width / 2f
    val centerX = size.width / 2f
    val centerY = size.height / 2f
    for (i in 0..5) {
        val angle = (60 * i - 30) * (Math.PI / 180f).toFloat()
        val x = centerX + radius * cos(angle)
        val y = centerY + radius * sin(angle)
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
fun CyberdeckScreen(onWalletClick: () -> Unit, tickerMessages: List<String> = emptyList()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020202))
    ) {
        CyberGridBackground()
        
        // Background Haze/Smoke effect
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
                Column(modifier = Modifier.fillMaxWidth().height(120.dp).padding(8.dp)) {
                    TerminalLine("> INITIALIZING NEURAL_LINK...", Color(0xFF00FF9C))
                    TerminalLine("> CONNECTION ESTABLISHED.", Color(0xFF00FF9C))
                    TerminalLine("> WELCOME BACK, EDGE-RUNNER.", Color.White)
                    TerminalLine("> 3 NEW MESSAGES FROM 'THE_FIXER'", Color(0xFFFF006E))
                    TerminalLine("> SOLANA_CHAIN: SYNCED", Color(0xFF00FFFF))
                    TerminalLine("> SCANNING FOR VULNERABILITIES...", Color(0xFF00FF9C))
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
        
        // Decorative neon borders like the image
        NeonFrameBorders()
    }
}

@Composable
fun HazeEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "HazeAnim")
    val hazeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize().blur(40.dp)) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00FFFF).copy(alpha = hazeAlpha), Color.Transparent),
                center = center,
                radius = size.maxDimension / 1.5f
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color(0xFFFF006E).copy(alpha = hazeAlpha * 0.5f)),
                startY = size.height * 0.7f,
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
                color = Color(0xFF00FF9C),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(
                "CONNECTED_VIA // NEURAL_GATE_01",
                color = Color(0xFF00FF9C).copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(HexagonShape)
                .background(Color(0xFFFF006E).copy(alpha = 0.1f))
                .border(1.dp, Color(0xFFFF006E), HexagonShape),
            contentAlignment = Alignment.Center
        ) {
            Text("SEC", color = Color(0xFFFF006E), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HexagonalNodeGrid(onWalletClick: () -> Unit) {
    Box(modifier = Modifier.size(340.dp)) {
        // Decorative Connecting Lines (Circuit Theme)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cyan = Color(0xFF00FFFF).copy(alpha = 0.2f)
            val magenta = Color(0xFFFF006E).copy(alpha = 0.2f)
            
            // Draw circuit lines between node positions
            drawCircle(cyan, radius = 120.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
            
            // X lines
            drawLine(magenta, start = Offset(0f, 0f), end = Offset(size.width, size.height), strokeWidth = 1f)
            drawLine(magenta, start = Offset(size.width, 0f), end = Offset(0f, size.height), strokeWidth = 1f)
        }

        // Center node
        HexNode(
            label = "CORE_OS", 
            color = Color(0xFF00FFFF), 
            modifier = Modifier.align(Alignment.Center).size(110.dp),
            isPulse = true
        )
        
        // Top Node
        HexNode(
            label = "NETWORK", 
            color = Color(0xFF00FF9C), 
            modifier = Modifier.align(Alignment.TopCenter).size(85.dp).offset(y = (-5).dp)
        )
        
        // Bottom Node
        HexNode(
            label = "DATABASE", 
            color = Color(0xFF00FFFF), 
            modifier = Modifier.align(Alignment.BottomCenter).size(85.dp).offset(y = 5.dp)
        )
        
        // Left Node
        HexNode(
            label = "EXPLOITS", 
            color = Color(0xFFFF006E), 
            modifier = Modifier.align(Alignment.CenterStart).size(85.dp).offset(x = (-5).dp)
        )
        
        // Right Node (Wallet)
        HexNode(
            label = "WALLET", 
            color = Color(0xFF00FFFF), 
            modifier = Modifier.align(Alignment.CenterEnd).size(85.dp).offset(x = 5.dp),
            onClick = onWalletClick
        )
        
        // Top Left Accents
        HexNode(
            label = "LOGS",
            color = Color(0xFF00FF9C).copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.TopStart).size(60.dp).offset(x = 20.dp, y = 20.dp)
        )
        
        // Top Right Accents
        HexNode(
            label = "SYNC",
            color = Color(0xFF00FF9C).copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.TopEnd).size(60.dp).offset(x = (-20).dp, y = 20.dp)
        )
    }
}

@Composable
fun HexNode(
    label: String, 
    color: Color, 
    modifier: Modifier = Modifier, 
    isPulse: Boolean = false,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HexPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isPulse) 0.9f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulse) 1.08f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(HexagonShape)
            .background(color.copy(alpha = 0.05f))
            .border(1.dp, color.copy(alpha = alpha), HexagonShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Decorative background patterns inside hex
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.1f)) {
            repeat(3) { i ->
                drawCircle(color, radius = (size.width / 2) * (i + 1) / 3f, style = Stroke(1f))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label, 
                color = color, 
                fontSize = 9.sp, 
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(modifier = Modifier.size(24.dp, 1.dp).background(color.copy(alpha = alpha)))
        }
        
        // Scanning effect
        val scanY = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Scan"
        )
        
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.3f)) {
            val y = size.height * scanY.value
            drawLine(
                color = color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
fun TerminalLine(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
fun NeonFrameBorders() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val magenta = Color(0xFFFF006E)
        val cyan = Color(0xFF00FFFF)
        
        // Left neon bar
        drawLine(
            magenta,
            start = Offset(6.dp.toPx(), 120.dp.toPx()),
            end = Offset(6.dp.toPx(), size.height - 120.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Right neon bar
        drawLine(
            cyan,
            start = Offset(size.width - 6.dp.toPx(), 120.dp.toPx()),
            end = Offset(size.width - 6.dp.toPx(), size.height - 120.dp.toPx()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        
        // Decorative top corners
        val cornerSize = 40.dp.toPx()
        drawPath(
            path = Path().apply {
                moveTo(20.dp.toPx(), 4.dp.toPx())
                lineTo(20.dp.toPx() + cornerSize, 4.dp.toPx())
                moveTo(size.width - 20.dp.toPx(), 4.dp.toPx())
                lineTo(size.width - 20.dp.toPx() - cornerSize, 4.dp.toPx())
            },
            color = magenta,
            style = Stroke(width = 2.dp.toPx())
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
            color = Color(0xFF00FF9C),
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
