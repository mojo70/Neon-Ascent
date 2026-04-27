package com.neon.ascent.feature.games

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun IceBreachScreen(
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
        BreachBackground()

        if (uiState !is IceBreachUiState.Success && uiState !is IceBreachUiState.Failed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.TopStart
            ) {
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
                Text("INITIALIZING BREACH PROTOCOL...", color = Color(0xFF00FF9C), fontFamily = FontFamily.Monospace)
            }
            is IceBreachUiState.Phase1 -> {
                Phase1Screen(state, viewModel)
            }
            is IceBreachUiState.Phase2 -> {
                Phase2Screen(state, viewModel)
            }
            is IceBreachUiState.Phase3 -> {
                Phase3Screen(state, viewModel)
            }
            is IceBreachUiState.Success -> {
                SuccessScreen(state, onBreachSuccess)
            }
            is IceBreachUiState.Failed -> {
                FailedScreen(state, onCancel)
            }
        }

        if (userCharacter?.iceLevel ?: 0 >= 8 && uiState !is IceBreachUiState.Success && uiState !is IceBreachUiState.Failed) {
            QuickHackButton(
                eddies = userCharacter?.eddies ?: 0,
                onQuickHack = { viewModel.triggerQuickHack() }
            )
        }
    }
}

@Composable
fun Phase1Screen(state: IceBreachUiState.Phase1, viewModel: IceBreachViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("PHASE 1: FREQUENCY MATCH", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2 - 20f
                drawWave(center, radius, state.targetFreq, Color(0xFF00FF9C).copy(alpha = 0.5f))
                drawWave(center, radius, state.currentFreq, Color(0xFFFF006E))
            }
        }
        Slider(
            value = state.currentFreq,
            onValueChange = { viewModel.updateFrequency(it) },
            valueRange = 0.1f..5f,
            modifier = Modifier.width(250.dp),
            colors = SliderDefaults.colors(thumbColor = Color(0xFFFF006E), activeTrackColor = Color(0xFFFF006E).copy(alpha = 0.5f))
        )
        Button(
            onClick = { viewModel.submitPhase1() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier.border(1.dp, Color(0xFF00FF9C), RoundedCornerShape(4.dp))
        ) {
            Text("LOCK FREQUENCY", color = Color(0xFF00FF9C))
        }
    }
}

@Composable
fun Phase2Screen(state: IceBreachUiState.Phase2, viewModel: IceBreachViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PHASE 2: NODAL BYPASS", color = Color.White, fontWeight = FontWeight.Bold)
            Text(
                "TRACE: ${state.remainingTime}s",
                color = if (state.remainingTime < 5) Color.Red else Color(0xFFFFCC00),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.border(1.dp, if (state.remainingTime < 5) Color.Red else Color(0xFFFFCC00)).padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.targetSequences.forEachIndexed { index, sequence ->
                val isCompleted = isSequenceCompleted(state.selectedIndices.map { state.grid[it] }, sequence)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("V${index + 1}:", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        sequence.forEach { code ->
                            Text(
                                code,
                                color = if (isCompleted) Color(0xFF00FF9C) else Color(0xFF00FF9C).copy(alpha = 0.7f),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .border(1.dp, if (isCompleted) Color(0xFF00FF9C) else Color(0xFF00FF9C).copy(alpha = 0.3f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .background(if (isCompleted) Color(0xFF00FF9C).copy(alpha = 0.1f) else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("BUFFER [", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            repeat(state.bufferSize) { i ->
                val code = state.selectedIndices.getOrNull(i)?.let { state.grid[it] } ?: "__"
                Text(" $code ", color = if (code == "__") Color.Gray else Color(0xFFFF006E), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            Text("]", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(24.dp))
        
        val activeIdx = state.activeIndex
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (r in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0 until 4) {
                        val index = r * 4 + c
                        val isSelected = state.selectedIndices.contains(index)
                        val isSelectable = if (activeIdx == null) r == 0 else {
                            if (state.isRowSelection) r == activeIdx / 4 else c == activeIdx % 4
                        }
                        Box(
                            modifier = Modifier.size(50.dp).border(1.dp, when {
                                isSelected -> Color(0xFFFF006E)
                                isSelectable -> Color(0xFF00FF9C)
                                else -> Color(0xFF00FF9C).copy(alpha = 0.1f)
                            }).background(when {
                                isSelected -> Color(0xFFFF006E).copy(alpha = 0.4f)
                                isSelectable -> Color(0xFF00FF9C).copy(alpha = 0.1f)
                                else -> Color.Transparent
                            }).clickable(enabled = isSelectable && !isSelected) { viewModel.selectNode(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(state.grid[index], color = if (isSelectable || isSelected) Color(0xFF00FF9C) else Color(0xFF00FF9C).copy(alpha = 0.2f), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { viewModel.resetPhase2() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), modifier = Modifier.border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(4.dp))) {
            Text("RESET MATRIX", color = Color.Red.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}

private fun isSequenceCompleted(selected: List<String>, target: List<String>): Boolean {
    if (target.isEmpty()) return true
    for (i in 0..selected.size - target.size) {
        if (selected.subList(i, i + target.size) == target) return true
    }
    return false
}

@Composable
fun Phase3Screen(state: IceBreachUiState.Phase3, viewModel: IceBreachViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("PHASE 3: SEMANTIC OVERRIDE", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text("SELECT DECRYPTION KEY", color = Color(0xFF00FF9C), textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.padding(bottom = 24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.options.forEach { option ->
                Box(modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.5f), RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.05f)).clickable { viewModel.submitPhase3(option) }.padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(option.uppercase(), color = Color(0xFFFF006E), fontFamily = FontFamily.Monospace, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("HINT: ${state.phrase.uppercase().take(8)}...", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun SuccessScreen(state: IceBreachUiState.Success, onBreachSuccess: () -> Unit) {
    var showRewards by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(2000)
        showRewards = true
        delay(2500)
        onBreachSuccess()
    }
    if (!showRewards) CyberSuccessEffect() else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ICE BROKEN", color = Color(0xFF00FF9C), fontSize = 32.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(16.dp))
            Text("XP REWARD: +${state.xp}", color = Color.White)
            Text("EDDIES EARNED: €${state.eddies}", color = Color(0xFFFFCC00))
            Spacer(Modifier.height(32.dp))
            Text("REDIRECTING TO CORE_OS...", color = Color(0xFF00FF9C).copy(alpha = 0.5f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun CyberSuccessEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "Success")
    val scanlineOffset by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(200, easing = LinearEasing)), label = "Scanline")
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ACCESS GRANTED", color = Color(0xFF00FF9C), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
            Text("DECRYPTING CORE DATA...", color = Color(0xFF00FF9C).copy(alpha = 0.7f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val glitchLineY = (scanlineOffset * size.height)
            drawLine(color = Color(0xFF00FF9C).copy(alpha = 0.3f), start = Offset(0f, glitchLineY), end = Offset(size.width, glitchLineY), strokeWidth = 4f)
            repeat(10) {
                val x = Random.nextFloat() * size.width
                val y = Random.nextFloat() * size.height
                drawRect(color = Color(0xFF00FF9C).copy(alpha = 0.2f), topLeft = Offset(x, y), size = Size(Random.nextFloat() * 100, Random.nextFloat() * 20))
            }
        }
    }
}

@Composable
fun FailedScreen(state: IceBreachUiState.Failed, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("BREACH FAILED", color = Color.Red, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Text(state.reason, color = Color.White, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
            Text("DISCONNECT", color = Color.White)
        }
    }
}

@Composable
fun QuickHackButton(eddies: Int, onQuickHack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomEnd) {
        Button(onClick = onQuickHack, enabled = eddies >= 20, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C).copy(alpha = 0.2f), disabledContainerColor = Color.Gray.copy(alpha = 0.1f)), border = BorderStroke(1.dp, if (eddies >= 20) Color(0xFF00FF9C) else Color.Gray), shape = RoundedCornerShape(4.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("QUICKHACK", fontSize = 10.sp, color = if (eddies >= 20) Color(0xFF00FF9C) else Color.Gray)
                Text("20 ED", fontSize = 8.sp, color = if (eddies >= 20) Color(0xFFFFCC00) else Color.Gray)
            }
        }
    }
}

@Composable
fun BreachBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "Background")
    val alpha by infiniteTransition.animateFloat(initialValue = 0.05f, targetValue = 0.15f, animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "Alpha")
    val density = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = with(density) { 40.dp.toPx() }
        for (x in 0..size.width.toInt() step step.toInt()) drawLine(Color(0xFF00FF9C).copy(alpha = alpha), Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1f)
        for (y in 0..size.height.toInt() step step.toInt()) drawLine(Color(0xFF00FF9C).copy(alpha = alpha), Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
    }
}

private fun DrawScope.drawWave(center: Offset, radius: Float, freq: Float, color: Color) {
    val path = Path()
    val points = 100
    for (i in 0..points) {
        val angle = (i.toFloat() / points) * 2 * PI.toFloat()
        val r = radius + sin(angle * freq * 5) * 15f
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
}
