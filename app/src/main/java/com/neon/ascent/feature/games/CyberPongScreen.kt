package com.neon.ascent.feature.games

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.ui.GlitchOverlay
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun CyberPongScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    
    // Force Landscape orientation
    DisposableEffect(context) {
        val activity = context.findActivity()
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = originalOrientation
        }
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var paddleY by remember { mutableStateOf(0f) }
    var aiPaddleY by remember { mutableStateOf(0f) }
    var ballPos by remember { mutableStateOf(Offset.Zero) }
    var ballVelocity by remember { mutableStateOf(Offset.Zero) }
    var score by remember { mutableStateOf(0) }
    var aiScore by remember { mutableStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }
    var gameStarted by remember { mutableStateOf(false) }
    var isSandevistanEnabled by remember { mutableStateOf(false) }

    val paddleWidth = 8.dp
    val paddleHeight = 60.dp
    val ballRadius = 6.dp

    // Reset ball to center
    fun resetBall() {
        if (canvasSize != IntSize.Zero) {
            ballPos = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
            // Re-tuned speeds for a better challenge
            val speed = if (isSandevistanEnabled) 110f else 35f
            val angle = if (Random.nextBoolean()) 1f else -1f
            ballVelocity = Offset(speed * angle, (Random.nextFloat() - 0.5f) * speed * 1.5f)
        }
    }

    LaunchedEffect(canvasSize) {
        if (canvasSize != IntSize.Zero && !gameStarted) {
            paddleY = canvasSize.height / 2f - 120f
            aiPaddleY = canvasSize.height / 2f - 120f
            resetBall()
        }
    }

    LaunchedEffect(gameStarted, gameOver, isSandevistanEnabled) {
        if (gameStarted && !gameOver && canvasSize != IntSize.Zero) {
            while (true) {
                delay(16) // ~60 FPS
                
                // Update Ball Position
                ballPos += ballVelocity

                // Wall collision (top/bottom)
                if (ballPos.y <= ballRadius.value || ballPos.y >= canvasSize.height - ballRadius.value) {
                    ballVelocity = Offset(ballVelocity.x, -ballVelocity.y)
                }

                // AI Paddle Logic (System)
                // Center the AI paddle logic better on its size
                val aiTargetY = ballPos.y - (paddleHeight.value * 2)
                val aiSpeed = if (isSandevistanEnabled) 90f else 22f
                if (aiPaddleY < aiTargetY) aiPaddleY += aiSpeed
                if (aiPaddleY > aiTargetY) aiPaddleY -= aiSpeed
                aiPaddleY = aiPaddleY.coerceIn(0f, canvasSize.height - paddleHeight.value * 4)

                // Player Paddle collision (Left)
                val paddleTop = paddleY
                val paddleBottom = paddleY + paddleHeight.value * 4
                if (ballPos.x <= 50f + paddleWidth.value && ballPos.y in paddleTop..paddleBottom) {
                    if (ballVelocity.x < 0) {
                        // Reflect and slightly increase speed/add vertical variance
                        ballVelocity = Offset(-ballVelocity.x * 1.05f, ballVelocity.y + (Random.nextFloat() - 0.5f) * 15f)
                    }
                } else if (ballPos.x < 0f) {
                    aiScore++
                    resetBall()
                    if (aiScore >= 5) gameOver = true
                }

                // AI Paddle collision (Right)
                val aiPaddleTop = aiPaddleY
                val aiPaddleBottom = aiPaddleY + paddleHeight.value * 4
                if (ballPos.x >= canvasSize.width - 50f - paddleWidth.value && ballPos.y in aiPaddleTop..aiPaddleBottom) {
                    if (ballVelocity.x > 0) {
                        ballVelocity = Offset(-ballVelocity.x * 1.05f, ballVelocity.y + (Random.nextFloat() - 0.5f) * 15f)
                    }
                } else if (ballPos.x > canvasSize.width) {
                    score++
                    resetBall()
                    if (score >= 5) gameOver = true
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF010501))
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    // Offset paddle touch to feel more natural
                    paddleY = (change.position.y - 120f).coerceIn(0f, canvasSize.height.toFloat() - 240f)
                }
            }
    ) {
        CircuitBoardBackground()
        GlitchOverlay()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val colorPrimary = Color(0xFF00FF9C)
            val colorSecondary = Color(0xFFFF006E)
            val colorSystem = Color(0xFF00FFFF)

            // Electron Ball (with glow)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, colorPrimary, Color.Transparent),
                    center = ballPos,
                    radius = ballRadius.toPx() * 4
                ),
                radius = ballRadius.toPx() * 3,
                center = ballPos
            )
            drawCircle(
                color = Color.White,
                radius = ballRadius.toPx() * 0.8f,
                center = ballPos
            )

            // Player Paddle (Magenta)
            drawRect(
                color = colorSecondary,
                topLeft = Offset(40f, paddleY),
                size = Size(paddleWidth.toPx(), paddleHeight.toPx() * 2)
            )
            
            // AI Paddle (Cyan)
            drawRect(
                color = colorSystem,
                topLeft = Offset(size.width - 40f - paddleWidth.toPx(), aiPaddleY),
                size = Size(paddleWidth.toPx(), paddleHeight.toPx() * 2)
            )
        }

        // UI Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FF9C))
                    }
                    Text("//CYBER_PONG_OS", color = Color(0xFF00FF9C), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SANDEVISTAN_MODE", color = if (isSandevistanEnabled) Color(0xFFFF006E) else Color.Gray, fontSize = 10.sp)
                    Checkbox(
                        checked = isSandevistanEnabled,
                        onCheckedChange = { isSandevistanEnabled = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF006E))
                    )
                    Spacer(Modifier.width(16.dp))
                    Text("USER: $score | SYSTEM: $aiScore", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (!gameStarted) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                Button(onClick = { gameStarted = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF006E))) {
                    Text("INITIALIZE_STREAM", color = Color.White, fontWeight = FontWeight.Black)
                }
            }
        }

        if (gameOver) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (score > aiScore) "SYSTEM_OVERRIDE_SUCCESS" else "KERNEL_PANIC", color = if (score > aiScore) Color(0xFF00FF9C) else Color.Red, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { score = 0; aiScore = 0; gameOver = false; gameStarted = false; resetBall() }, colors = ButtonDefaults.buttonColors(containerColor = Color.White)) {
                        Text("REBOOT_SESSION", color = Color.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun CircuitBoardBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val color = Color(0xFF00FF9C).copy(alpha = 0.15f)
        val strokeWidth = 1.dp.toPx()
        repeat(20) {
            val startX = Random.nextFloat() * size.width
            val startY = Random.nextFloat() * size.height
            val length = Random.nextFloat() * 300f + 100f
            if (Random.nextBoolean()) {
                drawLine(color, Offset(startX, startY), Offset(startX, startY + length), strokeWidth)
                drawCircle(color, 4f, Offset(startX, startY))
            } else {
                drawLine(color, Offset(startX, startY), Offset(startX + length, startY), strokeWidth)
                drawCircle(color, 4f, Offset(startX, startY))
            }
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}
