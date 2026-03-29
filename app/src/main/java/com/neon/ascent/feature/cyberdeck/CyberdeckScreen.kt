package com.neon.ascent.feature.cyberdeck

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.feature.charactercreation.CyberFrame
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CyberdeckScreen(onWalletClick: () -> Unit, tickerMessages: List<String> = emptyList()) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulsePhase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05080A))
    ) {
        // 1. Grid Background (Restored)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGrid()
        }

        Column(modifier = Modifier.fillMaxSize()) {
            TopStatusBar()

            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val w = constraints.maxWidth.toFloat()
                val h = constraints.maxHeight.toFloat()
                
                val paths = remember(w, h) {
                    createCircuitPaths(w, h)
                }

                // 2. Triple Base Wires + Traffic Pulses
                Canvas(modifier = Modifier.fillMaxSize()) {
                    paths.forEach { (path, color) ->
                        // Draw three parallel wires for each path
                        val offsets = listOf(-5f, 0f, 5f)
                        offsets.forEachIndexed { index, offset ->
                            withTransform({
                                translate(left = offset, top = offset)
                            }) {
                                // Faint static wire
                                drawPath(
                                    path = path,
                                    color = color.copy(alpha = 0.1f),
                                    style = Stroke(width = 1.5f)
                                )
                                // Glowing traffic pulses - staggered for a "data stream" look
                                val staggeredPhase = (pulsePhase + index * 0.15f) % 1f
                                drawTrafficPulse(
                                    path = path, 
                                    color = color, 
                                    phase = staggeredPhase,
                                    width = if (index == 1) 3f else 2f
                                )
                            }
                        }
                    }
                }

                // 3. Hexagon Cores
                CoreLayout(onWalletClick)
            }

            // 4. Live Console (Blinking cursor restored)
            CyberFrame(label = "CONSOLE_OUTPUT") {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp).padding(8.dp)) {
                    LiveConsole()
                }
            }
        }

        // Decorative accents
        Box(Modifier.width(4.dp).fillMaxHeight().background(Color(0xFFFF0088)).align(Alignment.CenterStart))
        Box(Modifier.width(4.dp).fillMaxHeight().background(Color(0xFF00CCFF)).align(Alignment.CenterEnd))
    }
}

@Composable
private fun TopStatusBar() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("CYBERDECK_TERMINAL", color = Color(0xFF00FFAA), fontSize = 18.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            Text("CONNECTED_VIA // NEURAL_GATE_01", color = Color(0xFF00FFAA).copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }

        // SEC Hex
        Box(modifier = Modifier.size(48.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawHexagon(Offset(size.width / 2, size.height / 2), size.width * 0.45f, Color(0xFFFF0088), 3f)
            }
            Text("SEC", Modifier.align(Alignment.Center), color = Color(0xFFFF0088), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun CoreLayout(onWalletClick: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        HexCore("NETWORK",  Color(0xFF00FF99), Modifier.align(Alignment.TopCenter).padding(top = 80.dp))
        HexCore("EXPLOITS", Color(0xFFFF0088), Modifier.align(Alignment.CenterStart).padding(start = 32.dp, bottom = 40.dp))
        HexCore("CORE_OS",  Color(0xFF00CCFF), Modifier.align(Alignment.Center).padding(bottom = 40.dp))
        HexCore("WALLET",   Color(0xFF00CCFF), Modifier.align(Alignment.CenterEnd).padding(end = 32.dp, bottom = 40.dp), onClick = onWalletClick)
        HexCore("DATABASE", Color(0xFF00CCFF), Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
    }
}

@Composable
private fun HexCore(label: String, color: Color, modifier: Modifier, onClick: () -> Unit = {}) {
    Box(modifier = modifier.size(110.dp).clickable { onClick() }) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width * 0.46f
            drawHexagon(center, radius, color.copy(alpha = 0.2f), 12f)
            drawHexagon(center, radius, color, 4f)
        }
        Text(
            text = label,
            modifier = Modifier.align(Alignment.Center),
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LiveConsole() {
    val allLogs = listOf(
        "> INITIALIZING NEURAL_LINK...",
        "> SYNCING DATA_STREAM...",
        "> CONNECTION ESTABLISHED.",
        "> TARGET ACQUIRED: NEURAL_GATE_01",
        "> SCANNING FOR VULNERABILITIES...",
        "> BYPASSING FIREWALL...",
        "> ACCESS GRANTED.",
        "> SOLANA_CHAIN: SYNCED",
        "> 3 NEW MESSAGES FROM 'THE_FIXER'",
        "> UPLINK STABLE.",
        "> WAITING FOR COMMAND..."
    )
    val lines = remember { mutableStateListOf<String>() }
    
    LaunchedEffect(Unit) {
        for (log in allLogs) {
            lines.add(log)
            delay(1200)
            if (lines.size > 7) lines.removeAt(0)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        lines.forEach { line ->
            Text(line, color = Color(0xFF00FF99), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("> ", color = Color(0xFF00FF99), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Box(Modifier.size(8.dp, 16.dp).background(Color(0xFF00FF99).copy(alpha = cursorAlpha)))
        }
    }
}

private fun createCircuitPaths(w: Float, h: Float): List<Pair<Path, Color>> {
    return listOf(
        Path().apply { moveTo(w * 0.5f, h * 0.2f); lineTo(w * 0.5f, h * 0.45f) } to Color(0xFF00FF99),
        Path().apply { moveTo(w * 0.22f, h * 0.45f); lineTo(w * 0.5f, h * 0.45f) } to Color(0xFFFF0088),
        Path().apply { moveTo(w * 0.5f, h * 0.45f); lineTo(w * 0.78f, h * 0.45f) } to Color(0xFF00CCFF),
        Path().apply { moveTo(w * 0.5f, h * 0.45f); lineTo(w * 0.5f, h * 0.72f) } to Color(0xFF00CCFF),
        Path().apply { moveTo(w * 0.22f, h * 0.45f); lineTo(w * 0.22f, h * 0.72f); lineTo(w * 0.5f, h * 0.72f) } to Color(0xFF00CCFF),
        Path().apply { moveTo(w * 0.88f, h * 0.05f); lineTo(w * 0.88f, h) } to Color(0xFFFF0044)
    )
}

private fun DrawScope.drawTrafficPulse(path: Path, color: Color, phase: Float, width: Float) {
    val pm = android.graphics.PathMeasure(path.asAndroidPath(), false)
    val len = pm.length
    if (len <= 0f) return
    
    val pulseLen = 60f
    val currentStart = len * phase
    
    val segment = android.graphics.Path()
    if (currentStart + pulseLen > len) {
        pm.getSegment(currentStart, len, segment, true)
        pm.getSegment(0f, (currentStart + pulseLen) % len, segment, true)
    } else {
        pm.getSegment(currentStart, currentStart + pulseLen, segment, true)
    }
    
    drawPath(segment.asComposePath(), color.copy(alpha = 0.8f), style = Stroke(width = width, cap = StrokeCap.Round))
    drawPath(segment.asComposePath(), color.copy(alpha = 0.2f), style = Stroke(width = width * 3, cap = StrokeCap.Round))
}

private fun DrawScope.drawGrid() {
    val color = Color(0xFF1A2A3A).copy(alpha = 0.4f)
    val step = 40.dp.toPx()
    for (x in 0..size.width.toInt() step step.toInt()) drawLine(color, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1f)
    for (y in 0..size.height.toInt() step step.toInt()) drawLine(color, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
}

private fun DrawScope.drawHexagon(center: Offset, radius: Float, color: Color, strokeWidth: Float) {
    val path = Path().apply {
        for (i in 0..5) {
            val angle = i * 60f * (PI.toFloat() / 180f) - (PI.toFloat() / 2)
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, color, style = Stroke(strokeWidth))
}
