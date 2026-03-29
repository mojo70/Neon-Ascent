package com.neon.ascent.feature.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.data.local.SayingsDao
import com.neon.ascent.data.local.UserCharacterDao
import com.neon.ascent.model.UserCharacter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
        // Background Matrix/Grid Effect
        BreachBackground()

        when (val state = uiState) {
            is IceBreachUiState.Initializing -> {
                Text(
                    "INITIALIZING BREACH PROTOCOL...",
                    color = Color(0xFF00FF9C),
                    fontFamily = FontFamily.Monospace
                )
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
fun Phase1Screen(state: IceBreachUiState.Phase1, viewModel: IceBreachViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("PHASE 1: FREQUENCY MATCH", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        
        Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2 - 20f
                
                // Target Wave
                drawWave(center, radius, state.targetFreq, Color(0xFF00FF9C).copy(alpha = 0.5f))
                
                // Current Wave
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
        Text("PHASE 2: NODAL BYPASS", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        // Code Sequence to match
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.targetSequence.forEach { code ->
                Text(
                    code,
                    color = Color(0xFF00FF9C),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
                    modifier = Modifier.border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.5f)).padding(4.dp)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Buffer display
        Row(modifier = Modifier.height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("BUFFER [", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            repeat(state.bufferSize) { i ->
                val code = state.selectedIndices.getOrNull(i)?.let { state.grid[it] } ?: "__"
                Text(
                    " $code ",
                    color = if (code == "__") Color.Gray else Color(0xFFFF006E),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text("]", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(Modifier.height(24.dp))
        
        val grid = state.grid
        val activeIdx = state.activeIndex
        val activeRow = activeIdx?.let { it / 4 }
        val activeCol = activeIdx?.let { it % 4 }
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (r in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0 until 4) {
                        val index = r * 4 + c
                        val isSelected = state.selectedIndices.contains(index)
                        
                        val isSelectable = if (activeIdx == null) {
                            r == 0 // Initial selection usually from first row
                        } else {
                            if (state.isRowSelection) r == activeRow else c == activeCol
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .border(
                                    1.dp, 
                                    when {
                                        isSelected -> Color(0xFFFF006E)
                                        isSelectable -> Color(0xFF00FF9C)
                                        else -> Color(0xFF00FF9C).copy(alpha = 0.1f)
                                    }
                                )
                                .background(
                                    when {
                                        isSelected -> Color(0xFFFF006E).copy(alpha = 0.4f)
                                        isSelectable -> Color(0xFF00FF9C).copy(alpha = 0.1f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(enabled = isSelectable && !isSelected) { 
                                    viewModel.selectNode(index) 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                grid[index], 
                                color = if (isSelectable || isSelected) Color(0xFF00FF9C) else Color(0xFF00FF9C).copy(alpha = 0.2f), 
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.resetPhase2() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier.border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
        ) {
            Text("RESET MATRIX", color = Color.Red.copy(alpha = 0.5f), fontSize = 10.sp)
        }
    }
}

@Composable
fun Phase3Screen(state: IceBreachUiState.Phase3, viewModel: IceBreachViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("PHASE 3: SEMANTIC OVERRIDE", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            "SELECT DECRYPTION KEY",
            color = Color(0xFF00FF9C),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.options.forEach { option ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { viewModel.submitPhase3(option) }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        option.uppercase(),
                        color = Color(0xFFFF006E),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
        Text(
            "HINT: ${state.phrase.uppercase().take(8)}...",
            color = Color.White.copy(alpha = 0.3f),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun SuccessScreen(state: IceBreachUiState.Success, onBreachSuccess: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("ICE BROKEN", color = Color(0xFF00FF9C), fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Text("XP REWARD: +${state.xp}", color = Color.White)
        Text("EDDIES EARNED: €${state.eddies}", color = Color(0xFFFFCC00))
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onBreachSuccess,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF9C))
        ) {
            Text("ENTER CORE DASHBOARD", color = Color.Black)
        }
    }
}

@Composable
fun FailedScreen(state: IceBreachUiState.Failed, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("BREACH FAILED", color = Color.Red, fontSize = 32.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Text(state.reason, color = Color.White)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("JACK OUT", color = Color.White)
        }
    }
}

@Composable
fun QuickHackButton(eddies: Int, onQuickHack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.BottomCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("QUICK-HACK AVAILABLE (LVL 8+)", color = Color(0xFF00CCFF), fontSize = 10.sp)
            Button(
                onClick = onQuickHack,
                enabled = eddies >= 20,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00CCFF).copy(alpha = 0.2f)),
                modifier = Modifier.border(1.dp, Color(0xFF00CCFF), RoundedCornerShape(4.dp))
            ) {
                Text("GHOST ICE (-20 EDDIES)", color = Color(0xFF00CCFF))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWave(center: Offset, radius: Float, freq: Float, color: Color) {
    val path = Path()
    val points = 100
    for (i in 0..points) {
        val angle = (i.toFloat() / points) * 2 * PI.toFloat()
        val offset = sin(angle * freq * 5) * 20f
        val r = radius + offset
        val x = center.x + r * cos(angle)
        val y = center.y + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, style = Stroke(width = 3f))
}

@Composable
fun BreachBackground() {
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing))
    )
    
    Canvas(Modifier.fillMaxSize()) {
        val color = Color(0xFF00FF9C).copy(alpha = 0.05f)
        val step = 40.dp.toPx()
        for (x in -step.toInt()..size.width.toInt() step step.toInt()) {
            drawLine(color, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1f)
        }
        for (y in -step.toInt()..size.height.toInt() step step.toInt()) {
            val yPos = (y + offset) % size.height
            drawLine(color, Offset(0f, yPos), Offset(size.width, yPos), 1f)
        }
    }
}

sealed class IceBreachUiState {
    object Initializing : IceBreachUiState()
    data class Phase1(val targetFreq: Float, val currentFreq: Float) : IceBreachUiState()
    data class Phase2(
        val grid: List<String>, 
        val targetSequence: List<String>, 
        val selectedIndices: List<Int>,
        val bufferSize: Int,
        val isRowSelection: Boolean,
        val activeIndex: Int?
    ) : IceBreachUiState()
    data class Phase3(val phrase: String, val options: List<String>) : IceBreachUiState()
    data class Success(val xp: Int, val eddies: Int) : IceBreachUiState()
    data class Failed(val reason: String) : IceBreachUiState()
}
