package com.neon.ascent.feature.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing))
    )

    Canvas(Modifier.fillMaxSize()) {
        val color = Color(0xFF00FF9C).copy(alpha = 0.1f)
        val chars = "0123456789ABCDEFHIJKLMNOPQRSTUVWXYZ"
        // Simulate rain with drawing text or just lines for performance
        for (i in 0..20) {
            val x = (size.width / 20) * i
            val yStart = (offset + (i * 100)) % size.height
            drawLine(color, Offset(x, yStart), Offset(x, yStart + 200f), strokeWidth = 2f)
        }
    }
}

data class FallingNode(val id: Int, val x: Float, val y: Float, val speed: Float)
