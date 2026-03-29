package com.neon.ascent.feature.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun BlackIceBreachScreen(
    onBreachSuccess: () -> Unit,
    onCancel: () -> Unit,
    viewModel: IceBreachViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userCharacter by viewModel.userCharacter.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Matrix Rain Background
        MatrixRainBackground()

        // Abort Button
        if (uiState !is IceBreachUiState.Success && uiState !is IceBreachUiState.Failed) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.TopStart) {
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Abort", modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("ABORT", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        when (val state = uiState) {
            is IceBreachUiState.Initializing -> {
                Text("CONNECTING TO BLACK_ICE...", color = Color(0xFF00FF9C), fontFamily = FontFamily.Monospace)
            }
            is IceBreachUiState.Phase1 -> {
                BlackIcePhase1(state, viewModel)
            }
            is IceBreachUiState.Phase2 -> {
                BlackIcePhase2(state, viewModel)
            }
            is IceBreachUiState.Phase3 -> {
                Phase3Screen(state, viewModel) // Reuse Phase 3 for consistency
            }
            is IceBreachUiState.Success -> {
                SuccessScreen(state, onBreachSuccess)
            }
            is IceBreachUiState.Failed -> {
                FailedScreen(state, onCancel)
            }
        }

        // Quick-hack Button
        if (userCharacter?.iceLevel ?: 0 >= 8 && uiState !is IceBreachUiState.Success && uiState !is IceBreachUiState.Failed) {
            QuickHackButton(
                eddies = userCharacter?.eddies ?: 0,
                onQuickHack = { viewModel.triggerQuickHack() }
            )
        }
    }
}

@Composable
fun BlackIcePhase1(state: IceBreachUiState.Phase1, viewModel: IceBreachViewModel) {
    // V3 Phase 1: TRACE (Falling Nodes)
    var nodes by remember { mutableStateOf(listOf<FallingNode>()) }
    var caughtCount by remember { mutableIntStateOf(0) }
    val targetCount = 10
    
    LaunchedEffect(Unit) {
        while (caughtCount < targetCount) {
            delay(800)
            nodes = nodes + FallingNode(
                id = Random.nextInt(),
                x = Random.nextFloat(),
                y = 0f,
                speed = 0.01f + (Random.nextFloat() * 0.01f)
            )
        }
    }

    LaunchedEffect(nodes) {
        while (nodes.any { it.y < 1f }) {
            delay(16)
            nodes = nodes.map { it.copy(y = it.y + it.speed) }.filter { it.y < 1.1f }
            if (nodes.any { it.y > 1f && it.y < 1.05f }) {
                // Node missed firewall
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
        Text("PHASE 1: TRACE_BYPASS", color = Color(0xFF00FF9C), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 32.dp))
        Text("NODES CAUGHT: $caughtCount / $targetCount", color = Color.White, fontSize = 12.sp)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            nodes.forEach { node ->
                Box(
                    modifier = Modifier
                        .offset(x = (node.x * 300).dp, y = (node.y * 500).dp)
                        .size(30.dp)
                        .background(Color(0xFF00FF9C).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF00FF9C), RoundedCornerShape(4.dp))
                        .clickable {
                            caughtCount++
                            nodes = nodes.filter { it.id != node.id }
                            if (caughtCount >= targetCount) {
                                viewModel.submitPhase1() // Hijack existing logic to move to next phase
                            }
                        }
                )
            }
        }
    }
}

@Composable
fun BlackIcePhase2(state: IceBreachUiState.Phase2, viewModel: IceBreachViewModel) {
    // V3 Phase 2: EXPLOIT (Node Connection / Pattern)
    // For now, we reuse the Nodal Bypass but with a different visual flair
    Phase2Screen(state, viewModel) 
}

@Composable
fun MatrixRainBackground() {
    val matrixChars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜｦﾝ"
    val columnCount = 25
    val fontSize = 16.sp
    val density = LocalDensity.current
    val fontSizePx = with(density) { fontSize.toPx() }

    val infiniteTransition = rememberInfiniteTransition(label = "MatrixRain")
    
    // We'll create multiple "drops" for each column to make it look more like a rain of characters
    val columns = remember {
        List(columnCount) { index ->
            MatrixColumn(
                xPercent = index.toFloat() / columnCount,
                speed = Random.nextFloat() * 0.02f + 0.01f,
                delay = Random.nextInt(0, 5000)
            )
        }
    }

    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Time"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#00FF9C")
            this.textSize = fontSizePx
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
        }

        columns.forEach { col ->
            val x = size.width * col.xPercent
            // Calculate current y position based on time and col speed/delay
            val totalDistance = size.height + 1000f // Extra distance to reset
            val currentY = ((time * 10000 * col.speed * 50) + col.delay) % totalDistance
            
            // Draw a trail of characters
            val trailLength = 15
            for (i in 0 until trailLength) {
                val charY = currentY - (i * fontSizePx)
                if (charY < 0 || charY > size.height) continue

                // Fade out the trail
                val alpha = (1f - (i.toFloat() / trailLength)).coerceIn(0f, 1f)
                
                // Variable glow: top character is bright white-green, others fade to dark green
                val charColor = if (i == 0) {
                    android.graphics.Color.argb((alpha * 255).toInt(), 200, 255, 230) // Bright head
                } else {
                    android.graphics.Color.argb((alpha * 255).toInt(), 0, 255, 156) // Green trail
                }
                
                paint.color = charColor
                
                // Add glow effect for the head
                if (i == 0) {
                    paint.setShadowLayer(10f, 0f, 0f, charColor)
                } else {
                    paint.setShadowLayer(0f, 0f, 0f, 0)
                }

                val randomChar = matrixChars[Random.nextInt(matrixChars.length)].toString()
                drawContext.canvas.nativeCanvas.drawText(randomChar, x, charY, paint)
            }
        }
    }
}

data class MatrixColumn(val xPercent: Float, val speed: Float, val delay: Int)

data class FallingNode(val id: Int, val x: Float, val y: Float, val speed: Float)
