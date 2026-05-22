package com.neon.ascent.feature.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neon.ascent.ui.CyberButtonShape
import com.neon.ascent.core.common.CyberGridBackground
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val CyberChessColors = darkColorScheme(
    background = Color(0xFF0A0F14),   // Deep void black
    surface = Color(0xFF12181F),
    primary = Color(0xFF00F5FF),      // Bright cyan (player)
    secondary = Color(0xFFFF0088),    // Hot magenta/pink (opponent)
    tertiary = Color(0xFF00FF9F)      // Accent green for highlights
)

@Composable
fun CyberChessScreen(
    onBack: () -> Unit,
    userLevel: Int = 1,
    viewModel: CyberChessViewModel = hiltViewModel()
) {
    val gameMode by viewModel.gameMode.collectAsState()
    val board by viewModel.board.collectAsState()
    val selectedSquare by viewModel.selectedSquare.collectAsState()
    val turn by viewModel.turn.collectAsState()
    val lastMove by viewModel.lastMove.collectAsState()
    val lastPawnDoubleJump by viewModel.lastPawnDoubleJump.collectAsState()
    val eloScore by viewModel.eloScore.collectAsState()
    val gameResult by viewModel.gameResult.collectAsState()
    
    val viName = "AETHER_NULL // V.2.4"
    val systemLogs = viewModel.systemLogs

    val possibleMoves = remember(selectedSquare, board, turn, lastPawnDoubleJump) {
        selectedSquare?.let { (r, c) ->
            val moves = mutableListOf<Pair<Int, Int>>()
            for (dr in 0 until 8) {
                for (dc in 0 until 8) {
                    if (ChessEngine.isValidMove(r, c, dr, dc, board, lastPawnDoubleJump) && 
                        !ChessEngine.wouldBeInCheck(r, c, dr, dc, board, turn, lastPawnDoubleJump)) {
                        moves.add(dr to dc)
                    }
                }
            }
            moves
        } ?: emptyList()
    }

    MaterialTheme(colorScheme = CyberChessColors) {
        if (gameMode == null) {
            ModeSelectionScreen(onModeSelect = { viewModel.selectMode(it) }, onBack = onBack)
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                CyberGridBackground()
                FullBackgroundGrid()
                AtmosphericHaze()
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    ChessHeader(
                        gameMode = gameMode!!,
                        viName = viName,
                        eloScore = eloScore,
                        onResetMode = { viewModel.resetGame() }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Turn/Result Indicator
                    StatusIndicator(gameResult = gameResult, turn = turn, viName = viName, gameMode = gameMode!!)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chess Board
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(4.dp)
                    ) {
                        ChessBoardGrid(
                            board = board,
                            selectedSquare = selectedSquare,
                            possibleMoves = possibleMoves,
                            lastMove = lastMove,
                            onSquareClick = { r, c -> viewModel.onSquareClick(r, c) }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (gameResult != null) {
                        Button(
                            onClick = { viewModel.resetGame() },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(48.dp)
                                .clip(CyberButtonShape)
                                .border(1.dp, MaterialTheme.colorScheme.tertiary, CyberButtonShape),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A))
                        ) {
                            GlowingText("REBOOT_SESSION", MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    ConsoleTerminal(systemLogs)
                }
            }
        }
    }
}

@Composable
fun FullBackgroundGrid() {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.08f)) {
        val gridSize = 40.dp.toPx()
        val color = Color(0xFF00F5FF)
        for (x in 0 until size.width.toInt() step gridSize.toInt()) {
            drawLine(color, start = Offset(x.toFloat(), 0f), end = Offset(x.toFloat(), size.height), strokeWidth = 1f)
        }
        for (y in 0 until size.height.toInt() step gridSize.toInt()) {
            drawLine(color, start = Offset(0f, y.toFloat()), end = Offset(size.width, y.toFloat()), strokeWidth = 1f)
        }
    }
}

@Composable
fun AtmosphericHaze() {
    Canvas(modifier = Modifier.fillMaxSize().blur(100.dp).alpha(0.15f)) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF00F5FF).copy(alpha = 0.3f), Color.Transparent),
                center = center,
                radius = size.maxDimension / 1.2f
            )
        )
    }
}

@Composable
fun ChessHeader(gameMode: GameMode, viName: String, eloScore: Int, onResetMode: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onResetMode) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
        }
        Column {
            GlowingText("CYBER_CHESS", MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.headlineSmall)
            if (gameMode == GameMode.SINGLE_PLAYER) {
                Text(
                    "VS $viName // ELO: $eloScore",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            } else {
                Text(
                    "LOCAL_LINK // MULTI_PLAYER",
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun StatusIndicator(gameResult: String?, turn: PieceColor, viName: String, gameMode: GameMode) {
    val infiniteTransition = rememberInfiniteTransition(label = "StatusPulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "Alpha"
    )

    val text = gameResult ?: if (turn == PieceColor.CYAN) "PLAYER_TURN // CYAN" else if (gameMode == GameMode.SINGLE_PLAYER) "VI_THINKING // $viName" else "OPPONENT_TURN // MAGENTA"
    val color = if (gameResult != null) Color.White else if (turn == PieceColor.CYAN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

    Text(
        text = text,
        color = color.copy(alpha = alpha),
        fontWeight = FontWeight.Black,
        fontSize = 14.sp,
        fontFamily = FontFamily.Monospace,
        letterSpacing = 2.sp
    )
}

@Composable
fun GlowingText(text: String, color: Color, style: androidx.compose.ui.text.TextStyle = LocalTextStyle.current) {
    Box(contentAlignment = Alignment.Center) {
        for (i in 0..4) {
            Text(
                text = text,
                color = color.copy(alpha = (0.15f / (i + 1))),
                style = style.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.blur((i * 2 + 1).dp)
            )
        }
        Text(
            text = text,
            color = color,
            style = style.copy(
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        )
    }
}

@Composable
fun ConsoleTerminal(logs: List<String>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            .background(Color(0xFF050505))
            .padding(8.dp)
    ) {
        Column {
            logs.take(5).forEach { log ->
                Text(
                    text = log,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("> ", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                val infiniteTransition = rememberInfiniteTransition(label = "Cursor")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                    label = "Alpha"
                )
                Box(modifier = Modifier.size(6.dp, 12.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)))
            }
        }
    }
}

@Composable
fun ChessBoardGrid(
    board: List<List<ChessPiece?>>,
    selectedSquare: Pair<Int, Int>?,
    possibleMoves: List<Pair<Int, Int>>,
    lastMove: Pair<Pair<Int, Int>, Pair<Int, Int>>?,
    onSquareClick: (Int, Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boardSize = size.minDimension
            val squareSize = boardSize / 8f
            for (i in 0..6) {
                drawRect(
                    color = Color(0xFF00F5FF).copy(alpha = (0.2f / (i + 1))),
                    style = Stroke(width = (10f + i * 6f))
                )
            }
            drawRect(
                color = Color(0xFF00F5FF),
                style = Stroke(width = 4f)
            )
            for (i in 1..7) {
                val pos = i * squareSize
                drawLine(
                    color = Color(0xFF00F5FF).copy(alpha = 0.2f),
                    start = Offset(0f, pos),
                    end = Offset(boardSize, pos),
                    strokeWidth = 1f
                )
                drawLine(
                    color = Color(0xFF00F5FF).copy(alpha = 0.2f),
                    start = Offset(pos, 0f),
                    end = Offset(pos, boardSize),
                    strokeWidth = 1f
                )
            }
        }

        Column {
            for (r in 0 until 8) {
                Row(modifier = Modifier.weight(1f)) {
                    for (c in 0 until 8) {
                        val isDark = (r + c) % 2 != 0
                        val isSelected = selectedSquare?.first == r && selectedSquare?.second == c
                        val isPossibleMove = possibleMoves.any { it.first == r && it.second == c }
                        val isLastMove = lastMove?.first == (r to c) || lastMove?.second == (r to c)
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                                        isLastMove -> Color.White.copy(alpha = 0.08f)
                                        isDark -> Color(0xFF0D1218).copy(alpha = 0.9f)
                                        else -> Color(0xFF080B0F).copy(alpha = 0.9f)
                                    }
                                )
                                .border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                .clickable { onSquareClick(r, c) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPossibleMove) {
                                val isCapture = board[r][c] != null
                                val ringColor = if (isCapture) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary
                                val infiniteTransition = rememberInfiniteTransition(label = "MoveIndicator")
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 0.8f,
                                    targetValue = 1.3f,
                                    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                                    label = "Scale"
                                )
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 0.7f,
                                    animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
                                    label = "Alpha"
                                )
                                Canvas(modifier = Modifier.fillMaxSize(0.6f)) {
                                    drawCircle(
                                        color = ringColor.copy(alpha = alpha),
                                        radius = (size.minDimension / 4f) * scale,
                                        style = Stroke(width = 2.dp.toPx())
                                    )
                                    if (!isCapture) {
                                        drawCircle(
                                            color = ringColor.copy(alpha = alpha * 0.5f),
                                            radius = 4.dp.toPx(),
                                            style = Fill
                                        )
                                    }
                                }
                            }
                            
                            board[r][c]?.let { piece ->
                                NeonPieceContainer(piece)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NeonPieceContainer(piece: ChessPiece) {
    val baseColor = if (piece.color == PieceColor.CYAN) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val materialAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        materialAnim.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    Box(modifier = Modifier
        .fillMaxSize(0.8f)
        .graphicsLayer { 
            scaleX = materialAnim.value
            scaleY = materialAnim.value
            alpha = materialAnim.value
        }
    ) {
        NeonPiece(type = piece.type, color = baseColor)
    }
}

@Composable
fun NeonPiece(type: PieceType, color: Color, glowIntensity: Float = 1.3f) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = createPiecePath(type, size)
        for (i in 0..8) {
            drawPath(
                path = path,
                color = color.copy(alpha = (0.25f - i * 0.03f).coerceAtLeast(0f) * glowIntensity),
                style = Stroke(width = (12f + i * 5f), cap = StrokeCap.Round)
            )
        }
        drawPath(path, color, style = Stroke(width = 5f, cap = StrokeCap.Round))
        drawPath(path, Color.White.copy(alpha = 0.6f), style = Stroke(width = 2f, cap = StrokeCap.Round))
        drawPath(path, color.copy(alpha = 0.15f), style = Fill)
    }
}

private fun createPiecePath(type: PieceType, size: Size): Path {
    val w = size.width
    val h = size.height
    val path = Path()
    val cx = w / 2f
    val cy = h / 2f
    when (type) {
        PieceType.PAWN -> {
            path.moveTo(cx, h * 0.15f); path.lineTo(w * 0.85f, h * 0.85f); path.lineTo(w * 0.15f, h * 0.85f); path.close()
        }
        PieceType.ROOK -> {
            path.moveTo(w * 0.2f, h * 0.2f); path.lineTo(w * 0.35f, h * 0.2f); path.lineTo(w * 0.35f, h * 0.35f); path.lineTo(w * 0.45f, h * 0.35f); path.lineTo(w * 0.45f, h * 0.2f); path.lineTo(w * 0.55f, h * 0.2f); path.lineTo(w * 0.55f, h * 0.35f); path.lineTo(w * 0.65f, h * 0.35f); path.lineTo(w * 0.65f, h * 0.2f); path.lineTo(w * 0.8f, h * 0.2f); path.lineTo(w * 0.8f, h * 0.85f); path.lineTo(w * 0.2f, h * 0.85f); path.close()
        }
        PieceType.KNIGHT -> {
            path.moveTo(w * 0.25f, h * 0.85f); path.lineTo(w * 0.25f, h * 0.45f); path.lineTo(w * 0.65f, h * 0.15f); path.lineTo(w * 0.85f, h * 0.45f); path.lineTo(w * 0.55f, h * 0.55f); path.lineTo(w * 0.75f, h * 0.85f); path.close()
        }
        PieceType.BISHOP -> {
            path.moveTo(cx, h * 0.1f); path.lineTo(w * 0.85f, cy); path.lineTo(cx, h * 0.9f); path.lineTo(w * 0.15f, cy); path.close()
            path.moveTo(cx, h * 0.15f); path.lineTo(cx, h * 0.85f)
        }
        PieceType.QUEEN -> {
            val centerX = w / 2; val centerY = h / 2
            for (i in 0 until 8) {
                val angle = (i * 45f) * (PI.toFloat() / 180f)
                val outerX = centerX + (w * 0.45f) * cos(angle); val outerY = centerY + (h * 0.45f) * sin(angle)
                val innerX = centerX + (w * 0.15f) * cos(angle + 22.5f * (PI.toFloat() / 180f)); val innerY = centerY + (h * 0.15f) * sin(angle + 22.5f * (PI.toFloat() / 180f))
                if (i == 0) path.moveTo(outerX, outerY) else path.lineTo(outerX, outerY)
                path.lineTo(innerX, innerY)
            }
            path.close()
        }
        PieceType.KING -> {
            val centerX = w / 2; val centerY = h / 2
            for (i in 0 until 6) {
                val angle = (i * 60f) * (PI.toFloat() / 180f) - (PI.toFloat() / 2)
                val x = centerX + (w * 0.45f) * cos(angle); val y = centerY + (h * 0.45f) * sin(angle)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            path.addOval(Rect(centerX - w * 0.12f, centerY - h * 0.12f, centerX + w * 0.12f, cy + h * 0.12f))
        }
    }
    return path
}

@Composable
fun ModeSelectionScreen(onModeSelect: (GameMode) -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GlowingText("SELECT_LINK_PROTOCOL", Color(0xFF00FF9F), style = MaterialTheme.typography.headlineMedium)
            ModeButton(label = "SINGLE_PLAYER // VS AETHER_NULL", color = Color(0xFFFF0088)) { onModeSelect(GameMode.SINGLE_PLAYER) }
            ModeButton(label = "TWO_PLAYER // LOCAL_LINK", color = Color(0xFF00F5FF)) { onModeSelect(GameMode.TWO_PLAYER) }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBack) { Text("TERMINATE_SESSION", color = Color.Gray, fontFamily = FontFamily.Monospace) }
        }
    }
}

@Composable
fun ModeButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(0.8f).height(56.dp).clip(CyberButtonShape).border(1.dp, color, CyberButtonShape),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A))
    ) {
        Text(label, color = color, fontWeight = FontWeight.Black, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}
