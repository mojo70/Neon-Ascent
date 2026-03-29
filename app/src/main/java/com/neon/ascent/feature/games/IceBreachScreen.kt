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
    // Implement a simple pattern match or hex grid selection
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("PHASE 2: NODAL BYPASS", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        
        val grid = state.grid
        val target = state.targetIndices
        
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (r in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0 until 4) {
                        val index = r * 4 + c
                        val isSelected = state.selectedIndices.contains(index)
                        val isTarget = target.contains(index)
                        
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .border(1.dp, if (isSelected) Color(0xFFFF006E) else Color(0xFF00FF9C).copy(alpha = 0.3f))
                                .background(if (isSelected) Color(0xFFFF006E).copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { viewModel.toggleNode(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(grid[index], color = Color(0xFF00FF9C), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { viewModel.submitPhase2() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier = Modifier.border(1.dp, Color(0xFF00FF9C), RoundedCornerShape(4.dp))
        ) {
            Text("BYPASS NODES", color = Color(0xFF00FF9C))
        }
    }
}

@Composable
fun Phase3Screen(state: IceBreachUiState.Phase3, viewModel: IceBreachViewModel) {
    var text by remember { mutableStateOf("") }
    
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("PHASE 3: SEMANTIC OVERRIDE", color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            state.phrase.uppercase(),
            color = Color(0xFF00FF9C),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            modifier = Modifier.padding(8.dp).border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.5f)).padding(16.dp)
        )
        Spacer(Modifier.height(24.dp))
        
        BasicTextField(
            value = text,
            onValueChange = { 
                text = it
                if (it.equals(state.phrase, ignoreCase = true)) {
                    viewModel.submitPhase3(it)
                }
            },
            textStyle = TextStyle(color = Color(0xFFFF006E), fontFamily = FontFamily.Monospace, fontSize = 20.sp, textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f))
                .padding(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            cursorBrush = SolidColor(Color(0xFFFF006E))
        )
        
        Text(
            "REPLICATE ENCRYPTION KEY",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 8.dp)
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
    data class Phase2(val grid: List<String>, val targetIndices: Set<Int>, val selectedIndices: Set<Int>) : IceBreachUiState()
    data class Phase3(val phrase: String) : IceBreachUiState()
    data class Success(val xp: Int, val eddies: Int) : IceBreachUiState()
    data class Failed(val reason: String) : IceBreachUiState()
}
