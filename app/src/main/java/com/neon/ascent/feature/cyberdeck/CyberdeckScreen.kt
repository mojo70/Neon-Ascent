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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.feature.biohacking.AiType
import com.neon.ascent.ui.CyberFrame
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun CyberdeckScreen(
    onWalletClick: () -> Unit,
    onDatabaseClick: () -> Unit,
    onIceBreachClick: () -> Unit,
    onCoreClick: () -> Unit,
    onExploitsClick: () -> Unit = {},
    tickerMessages: List<String> = emptyList(),
    viewModel: CyberdeckViewModel = hiltViewModel()
) {
    val aiType by viewModel.aiType.collectAsState()
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

    // Sync external ticker messages to the ViewModel's combined feed
    LaunchedEffect(tickerMessages) {
        viewModel.setExternalFeeds(tickerMessages)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020508)) // Darker for high contrast neon
    ) {
        // 1. Grid Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawGrid()
        }

        // 2. Ambient Haze
        AtmosphericHaze()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            TopStatusBar(onIceBreachClick)

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

                // 3. Triple Base Wires + Traffic Pulse
                Canvas(modifier = Modifier.fillMaxSize()) {
                    paths.forEach { (path, color) ->
                        val offsets = listOf(-5f, 0f, 5f)
                        offsets.forEachIndexed { index, offset ->
                            withTransform({
                                translate(left = offset, top = offset)
                            }) {
                                drawPath(
                                    path = path,
                                    color = color.copy(alpha = 0.15f),
                                    style = Stroke(width = 1.5f)
                                )
                                if (index == 1) {
                                    drawTrafficPulse(path, color, pulsePhase, width = 3.5f)
                                }
                            }
                        }
                    }
                }

                // 4. Hexagon Cores (Enhanced 3D Neon)
                CoreLayout(onWalletClick, onDatabaseClick, onCoreClick, onExploitsClick, aiType)
            }

            // 5. Live Console
            CyberFrame(label = "CYBERDECK_TERMINAL_FEED") {
                Box(modifier = Modifier.fillMaxWidth().height(160.dp).padding(8.dp)) {
                    LiveConsole(viewModel)
                }
            }
        }

        // Decorative accents
        Box(Modifier.width(4.dp).fillMaxHeight().background(Color(0xFFFF0088)).align(Alignment.CenterStart))
        Box(Modifier.width(4.dp).fillMaxHeight().background(Color(0xFF00CCFF)).align(Alignment.CenterEnd))
    }
}

@Composable
fun AtmosphericHaze() {
    val infiniteTransition = rememberInfiniteTransition(label = "Haze")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFF00CCFF).copy(alpha = alpha), Color.Transparent),
                center = center,
                radius = size.maxDimension / 1.5f
            )
        )
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.Transparent, Color(0xFFFF0088).copy(alpha = alpha * 0.5f)),
                startY = size.height * 0.6f,
                endY = size.height
            )
        )
    }
}

@Composable
private fun TopStatusBar(onIceBreachClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("CYBERDECK_TERMINAL", color = Color(0xFF00FFAA), fontSize = 18.sp, fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
            Text("CONNECTED_VIA // NEURAL_GATE_01", color = Color(0xFF00FFAA).copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }

        // SEC Hex with ICE effect
        SecCoreWithIce(onClick = onIceBreachClick)
    }
}

@Composable
private fun SecCoreWithIce(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "SecIce")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(modifier = Modifier.size(64.dp).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.width * 0.32f
            val iceColor = Color(0xFFFF0088)

            // ICE Protective Barriers (Rotating Rings)
            rotate(rotation) {
                drawArc(
                    color = iceColor.copy(alpha = 0.3f),
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Butt),
                    topLeft = Offset(center.x - baseRadius * 1.4f, center.y - baseRadius * 1.4f),
                    size = Size(baseRadius * 2.8f, baseRadius * 2.8f)
                )
                drawArc(
                    color = iceColor.copy(alpha = 0.3f),
                    startAngle = 180f,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Butt),
                    topLeft = Offset(center.x - baseRadius * 1.4f, center.y - baseRadius * 1.4f),
                    size = Size(baseRadius * 2.8f, baseRadius * 2.8f)
                )
            }

            rotate(-rotation * 0.7f) {
                drawArc(
                    color = iceColor.copy(alpha = 0.2f),
                    startAngle = 45f,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 10f))),
                    topLeft = Offset(center.x - baseRadius * 1.7f, center.y - baseRadius * 1.7f),
                    size = Size(baseRadius * 3.4f, baseRadius * 3.4f)
                )
            }

            // Main Core Hexagon
            drawNeonHexagon(
                center = center,
                radius = baseRadius * pulse,
                color = iceColor
            )
        }
        Text(
            text = "SEC",
            modifier = Modifier.align(Alignment.Center).graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            },
            color = Color(0xFFFF0088),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun CoreLayout(
    onWalletClick: () -> Unit,
    onDatabaseClick: () -> Unit,
    onCoreClick: () -> Unit,
    onExploitsClick: () -> Unit,
    aiType: AiType
) {
    Box(Modifier.fillMaxSize()) {
        // NETWORK
        HexCore("NETWORK",  Color(0xFF00FF99), Modifier.align(Alignment.TopCenter).padding(top = 80.dp))
        // EXPLOITS
        HexCore("EXPLOITS", Color(0xFFFF0088), Modifier.align(Alignment.CenterStart).padding(start = 32.dp, bottom = 40.dp), onClick = onExploitsClick)
        
        // CORE_OS / AI Status
        val coreLabel = when (aiType) {
            AiType.LOCAL -> "LOCAL AI"
            AiType.CLOUD -> "CLOUD AI"
            AiType.NONE -> "OFFLINE"
        }
        val coreColor = when (aiType) {
            AiType.LOCAL -> Color(0xFFFFFF00)
            AiType.CLOUD -> Color(0xFF00CCFF)
            AiType.NONE -> Color.Red
        }
        HexCore(
            label = coreLabel,
            color = coreColor,
            modifier = Modifier.align(Alignment.Center).padding(bottom = 40.dp),
            aiType = aiType,
            onClick = onCoreClick
        )

        // WALLET
        HexCore("WALLET",   Color(0xFF00CCFF), Modifier.align(Alignment.CenterEnd).padding(end = 32.dp, bottom = 40.dp), onClick = onWalletClick)
        // DATABASE
        HexCore("DATABASE", Color(0xFF00CCFF), Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp), onClick = onDatabaseClick)
    }
}

@Composable
private fun HexCore(label: String, color: Color, modifier: Modifier, aiType: AiType? = null, onClick: () -> Unit = {}) {
    val infiniteTransition = rememberInfiniteTransition(label = "HexEffect")
    
    // Local AI Swirl
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SwirlRotation"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // Offline Glitch
    var glitchOffset by remember { mutableStateOf(Offset.Zero) }
    if (aiType == AiType.NONE) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(Random.nextLong(50, 250))
                glitchOffset = if (Random.nextFloat() > 0.85f) {
                    Offset(Random.nextFloat() * 6 - 3, Random.nextFloat() * 4 - 2)
                } else {
                    Offset.Zero
                }
            }
        }
    }

    Box(
        modifier = modifier
            .size(110.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width * 0.46f
            val path = createHexagonPath(center, radius)
            
            // 3D Inner Shading
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    listOf(color.copy(alpha = 0.15f), Color.Transparent, color.copy(alpha = 0.05f)),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                ),
                style = Fill
            )
            
            // Local AI Swirling Feature
            if (aiType == AiType.LOCAL) {
                rotate(rotation) {
                    drawArc(
                        color = Color(0xFFFFFF00).copy(alpha = pulseAlpha),
                        startAngle = 0f,
                        sweepAngle = 120f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(center.x - radius * 1.25f, center.y - radius * 1.25f),
                        size = Size(radius * 2.5f, radius * 2.5f)
                    )
                    drawArc(
                        color = Color(0xFFFFFF00).copy(alpha = pulseAlpha),
                        startAngle = 180f,
                        sweepAngle = 120f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(center.x - radius * 1.25f, center.y - radius * 1.25f),
                        size = Size(radius * 2.5f, radius * 2.5f)
                    )
                }
            }

            // Enhanced Neon Hexagon Border
            drawNeonHexagon(center, radius, color)
        }
        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(glitchOffset.x.dp, glitchOffset.y.dp)
                .graphicsLayer {
                    if (aiType == AiType.NONE) {
                        alpha = if (Random.nextFloat() > 0.92f) 0.4f else 1f
                    }
                },
            color = if (aiType == AiType.NONE) Color.Red else Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            fontWeight = if (aiType != null) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun LiveConsole(viewModel: CyberdeckViewModel) {
    val combinedFeeds by viewModel.combinedFeeds.collectAsState()
    
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
        combinedFeeds.reversed().forEach { line ->
            val color = when {
                line.contains("[CRITICAL]") -> Color.Red
                line.contains("[MEDIUM]") -> Color(0xFFFFCC00)
                line.contains("SUBJECT:") -> Color(0xFF00CCFF)
                line.contains("LOCAL_CONDITIONS:") -> Color(0xFFFFFF00)
                else -> Color(0xFF00FF99)
            }
            Text(
                line,
                color = color, 
                fontSize = 10.sp, 
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 2.dp)
            )
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

private fun createHexagonPath(center: Offset, radius: Float): Path {
    return Path().apply {
        for (i in 0..5) {
            val angle = i * 60f * (PI.toFloat() / 180f) - (PI.toFloat() / 2)
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

private fun DrawScope.drawNeonHexagon(center: Offset, radius: Float, color: Color) {
    val path = createHexagonPath(center, radius)
    
    // Outer glow layers (halo effect on the hexagon path)
    for (i in 0..8) {
        val f = i.toFloat()
        val alphaVal = (0.15f - f * 0.015f).coerceAtLeast(0f)
        if (alphaVal > 0f) {
            drawPath(
                path = path,
                color = color.copy(alpha = alphaVal),
                style = Stroke(width = 4f + f * 8f, cap = StrokeCap.Round)
            )
        }
    }

    // Main sharp neon highlight
    drawPath(path, color, style = Stroke(width = 4f, cap = StrokeCap.Round))
    
    // Inner white highlight for depth and "etched light" feel
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.5f),
        style = Stroke(width = 1.5f, cap = StrokeCap.Round)
    )
}
