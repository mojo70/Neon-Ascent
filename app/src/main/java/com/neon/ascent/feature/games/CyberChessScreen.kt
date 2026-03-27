package com.neon.ascent.feature.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.feature.charactercreation.CyberButtonShape
import com.neon.ascent.feature.charactercreation.CyberGridBackground
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

val CyberChessColors = darkColorScheme(
    background = Color(0xFF0A0F14),   // Deep void black
    surface = Color(0xFF12181F),
    primary = Color(0xFF00F5FF),      // Bright cyan (player)
    secondary = Color(0xFFFF0088),    // Hot magenta/pink (opponent)
    tertiary = Color(0xFF00FF9F)      // Accent green for highlights
)

enum class PieceType { PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING }
enum class PieceColor { CYAN, MAGENTA }
enum class GameMode { SINGLE_PLAYER, TWO_PLAYER }

data class ChessPiece(val type: PieceType, val color: PieceColor, val hasMoved: Boolean = false)

@Composable
fun CyberChessScreen(onBack: () -> Unit, userLevel: Int = 1) {
    var gameMode by remember { mutableStateOf<GameMode?>(null) }
    var board by remember { mutableStateOf(initialBoard()) }
    var selectedSquare by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var turn by remember { mutableStateOf(PieceColor.CYAN) }
    var lastMove by remember { mutableStateOf<Pair<Pair<Int, Int>, Pair<Int, Int>>?>(null) }
    var lastPawnDoubleJump by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var eloScore by remember { mutableIntStateOf(1000) }
    var gameResult by remember { mutableStateOf<String?>(null) }
    
    val viName = "AETHER_NULL // V.2.4"
    val systemLogs = remember { mutableStateListOf("> INITIALIZING NEURAL_CHESS_ENGINE...") }

    val possibleMoves = remember(selectedSquare, board, turn, lastPawnDoubleJump) {
        selectedSquare?.let { (r, c) ->
            val moves = mutableListOf<Pair<Int, Int>>()
            for (dr in 0 until 8) {
                for (dc in 0 until 8) {
                    if (isValidMove(r, c, dr, dc, board, lastPawnDoubleJump) && !wouldBeInCheck(r, c, dr, dc, board, turn, lastPawnDoubleJump)) {
                        moves.add(dr to dc)
                    }
                }
            }
            moves
        } ?: emptyList()
    }

    MaterialTheme(colorScheme = CyberChessColors) {
        if (gameMode == null) {
            ModeSelectionScreen(onModeSelect = { gameMode = it }, onBack = onBack)
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                CyberGridBackground()
                FullBackgroundGrid()
                AtmosphericHaze()
                
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    ChessHeader(
                        gameMode = gameMode!!,
                        viName = viName,
                        eloScore = eloScore,
                        onResetMode = { 
                            gameMode = null
                            board = initialBoard()
                            turn = PieceColor.CYAN
                            gameResult = null
                            lastMove = null
                            lastPawnDoubleJump = null
                            selectedSquare = null
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Turn/Result Indicator
                    StatusIndicator(gameResult = gameResult, turn = turn, viName = viName, gameMode = gameMode!!)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chess Board with Neon Upgrade
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
                            onSquareClick = { r, c ->
                                if (gameResult != null) return@ChessBoardGrid
                                if (gameMode == GameMode.SINGLE_PLAYER && turn == PieceColor.MAGENTA) return@ChessBoardGrid
                                
                                val piece = board[r][c]
                                if (selectedSquare == null) {
                                    if (piece?.color == turn) {
                                        selectedSquare = r to c
                                    }
                                } else {
                                    val (sr, sc) = selectedSquare!!
                                    if (isValidMove(sr, sc, r, c, board, lastPawnDoubleJump) && !wouldBeInCheck(sr, sc, r, c, board, turn, lastPawnDoubleJump)) {
                                        val isPawnDoubleJump = board[sr][sc]?.type == PieceType.PAWN && abs(r - sr) == 2
                                        
                                        board = performMove(sr, sc, r, c, board, lastPawnDoubleJump)
                                        lastMove = (sr to sc) to (r to c)
                                        lastPawnDoubleJump = if (isPawnDoubleJump) r to c else null
                                        selectedSquare = null
                                        systemLogs.add(0, "> MOVE_EXECUTED: ${coord(sr, sc)} TO ${coord(r, c)}")
                                        
                                        val nextTurn = if (turn == PieceColor.CYAN) PieceColor.MAGENTA else PieceColor.CYAN
                                        if (isCheckmate(board, nextTurn, lastPawnDoubleJump)) {
                                            gameResult = "CHECKMATE // PLAYER WINS"
                                            eloScore = calculateNewElo(eloScore, getViElo(eloScore), 1.0)
                                            systemLogs.add(0, "> VI_DEFEATED: ELO_UPGRADE_SYNCED")
                                        } else if (isInCheck(board, nextTurn, lastPawnDoubleJump)) {
                                            systemLogs.add(0, "> WARNING: KING_UNDER_ATTACK")
                                            turn = nextTurn
                                        } else if (isDraw(board, nextTurn, lastPawnDoubleJump)) {
                                            gameResult = "STALEMATE // DRAW"
                                            eloScore = calculateNewElo(eloScore, getViElo(eloScore), 0.5)
                                            systemLogs.add(0, "> DRAW_DETECTED: ELO_CALIBRATED")
                                            turn = nextTurn
                                        } else {
                                            turn = nextTurn
                                        }
                                    } else {
                                        if (piece?.color == turn) {
                                            selectedSquare = r to c
                                        } else {
                                            selectedSquare = null
                                        }
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // AI Turn Logic
                    if (gameResult == null && gameMode == GameMode.SINGLE_PLAYER && turn == PieceColor.MAGENTA) {
                        LaunchedEffect(turn) {
                            delay(1500)
                            val depth = getAdaptiveDepth(eloScore)
                            val aiMove = findBestMove(board, PieceColor.MAGENTA, depth, lastPawnDoubleJump)
                            if (aiMove != null) {
                                val (from, to) = aiMove
                                val isPawnDoubleJump = board[from.first][from.second]?.type == PieceType.PAWN && abs(to.first - from.first) == 2
                                
                                board = performMove(from.first, from.second, to.first, to.second, board, lastPawnDoubleJump)
                                lastMove = from to to
                                lastPawnDoubleJump = if (isPawnDoubleJump) to.first to to.second else null
                                systemLogs.add(0, "> $viName EXECUTED: ${coord(from.first, from.second)} TO ${coord(to.first, to.second)}")
                                
                                if (isCheckmate(board, PieceColor.CYAN, lastPawnDoubleJump)) {
                                    gameResult = "CHECKMATE // $viName WINS"
                                    eloScore = calculateNewElo(eloScore, getViElo(eloScore), 0.0)
                                    systemLogs.add(0, "> NEURAL_LINK_SEVERED: YOU LOSE")
                                } else if (isInCheck(board, PieceColor.CYAN, lastPawnDoubleJump)) {
                                    systemLogs.add(0, "> HAZARD: NEURAL_CORE_THREATENED")
                                } else if (isDraw(board, PieceColor.CYAN, lastPawnDoubleJump)) {
                                    gameResult = "STALEMATE // DRAW"
                                    eloScore = calculateNewElo(eloScore, getViElo(eloScore), 0.5)
                                    systemLogs.add(0, "> BUFFER_OVERFLOW: GAME DRAWN")
                                }
                            }
                            turn = PieceColor.CYAN
                        }
                    }

                    if (gameResult != null) {
                        Button(
                            onClick = { 
                                board = initialBoard()
                                turn = PieceColor.CYAN
                                gameResult = null
                                lastMove = null
                                selectedSquare = null
                                lastPawnDoubleJump = null
                                systemLogs.add(0, "> REBOOTING_GAME_PROTOCOL...")
                            },
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

// --- Engine Logic ---

fun coord(r: Int, c: Int): String = "${('A' + c)}${8 - r}"

fun performMove(sr: Int, sc: Int, dr: Int, dc: Int, board: List<List<ChessPiece?>>, lastPawnDoubleJump: Pair<Int, Int>?): List<List<ChessPiece?>> {
    val newBoard = board.map { it.toMutableList() }
    val piece = newBoard[sr][sc] ?: return board
    if (piece.type == PieceType.PAWN && sc != dc && newBoard[dr][dc] == null) newBoard[sr][dc] = null
    if (piece.type == PieceType.KING && abs(dc - sc) == 2) {
        val rookSc = if (dc > sc) 7 else 0; val rookDc = if (dc > sc) 5 else 3
        newBoard[sr][rookDc] = newBoard[sr][rookSc]?.copy(hasMoved = true); newBoard[sr][rookSc] = null
    }
    newBoard[dr][dc] = piece.copy(hasMoved = true); newBoard[sr][sc] = null
    if (piece.type == PieceType.PAWN && ((piece.color == PieceColor.CYAN && dr == 0) || (piece.color == PieceColor.MAGENTA && dr == 7))) {
        newBoard[dr][dc] = piece.copy(type = PieceType.QUEEN, hasMoved = true)
    }
    return newBoard.map { it.toList() }
}

fun isInCheck(board: List<List<ChessPiece?>>, color: PieceColor, lastPawnDoubleJump: Pair<Int, Int>?): Boolean {
    var kingPos: Pair<Int, Int>? = null
    for (r in 0 until 8) for (c in 0 until 8) {
        val p = board[r][c]
        if (p?.type == PieceType.KING && p.color == color) { kingPos = r to c; break }
    }
    if (kingPos == null) return false
    val oppColor = if (color == PieceColor.CYAN) PieceColor.MAGENTA else PieceColor.CYAN
    for (r in 0 until 8) for (c in 0 until 8) {
        if (board[r][c]?.color == oppColor && isValidMove(r, c, kingPos.first, kingPos.second, board, lastPawnDoubleJump, checkingAttacksOnly = true)) return true
    }
    return false
}

fun wouldBeInCheck(sr: Int, sc: Int, dr: Int, dc: Int, board: List<List<ChessPiece?>>, color: PieceColor, lastPawnDoubleJump: Pair<Int, Int>?): Boolean {
    val simulatedBoard = performMove(sr, sc, dr, dc, board, lastPawnDoubleJump)
    return isInCheck(simulatedBoard, color, lastPawnDoubleJump)
}

fun isCheckmate(board: List<List<ChessPiece?>>, color: PieceColor, lastPawnDoubleJump: Pair<Int, Int>?): Boolean {
    if (!isInCheck(board, color, lastPawnDoubleJump)) return false
    return getAllValidMoves(board, color, lastPawnDoubleJump).none { !wouldBeInCheck(it.first.first, it.first.second, it.second.first, it.second.second, board, color, lastPawnDoubleJump) }
}

fun isDraw(board: List<List<ChessPiece?>>, color: PieceColor, lastPawnDoubleJump: Pair<Int, Int>?): Boolean {
    if (!isInCheck(board, color, lastPawnDoubleJump) && getAllValidMoves(board, color, lastPawnDoubleJump).none { !wouldBeInCheck(it.first.first, it.first.second, it.second.first, it.second.second, board, color, lastPawnDoubleJump) }) return true
    var pieceCount = 0
    for (r in 0 until 8) for (c in 0 until 8) if (board[r][c] != null) pieceCount++
    if (pieceCount == 2) return true
    return false
}

fun isValidMove(sr: Int, sc: Int, dr: Int, dc: Int, board: List<List<ChessPiece?>>, lastPawnDoubleJump: Pair<Int, Int>?, checkingAttacksOnly: Boolean = false): Boolean {
    val piece = board[sr][sc] ?: return false
    val destPiece = board[dr][dc]
    if (destPiece?.color == piece.color) return false
    val drAbs = abs(dr - sr); val dcAbs = abs(dc - sc)
    return when (piece.type) {
        PieceType.PAWN -> {
            val dir = if (piece.color == PieceColor.CYAN) -1 else 1
            if (dc == sc) {
                if (checkingAttacksOnly) false
                else if (dr == sr + dir) destPiece == null
                else if (dr == sr + 2 * dir && !piece.hasMoved && sr == (if (piece.color == PieceColor.CYAN) 6 else 1)) destPiece == null && board[sr + dir][sc] == null
                else false
            } else if (dcAbs == 1 && dr == sr + dir) destPiece != null || (lastPawnDoubleJump != null && lastPawnDoubleJump.first == sr && lastPawnDoubleJump.second == dc)
            else false
        }
        PieceType.ROOK -> (sr == dr || sc == dc) && isPathClear(sr, sc, dr, dc, board)
        PieceType.KNIGHT -> (drAbs == 2 && dcAbs == 1) || (drAbs == 1 && dcAbs == 2)
        PieceType.BISHOP -> drAbs == dcAbs && isPathClear(sr, sc, dr, dc, board)
        PieceType.QUEEN -> (drAbs == dcAbs || sr == dr || sc == dc) && isPathClear(sr, sc, dr, dc, board)
        PieceType.KING -> {
            if (drAbs <= 1 && dcAbs <= 1) true
            else if (!checkingAttacksOnly && !piece.hasMoved && dr == sr && dcAbs == 2) {
                val rookSc = if (dc > sc) 7 else 0; val rook = board[sr][rookSc]
                if (rook?.type == PieceType.ROOK && !rook.hasMoved && isPathClear(sr, sc, sr, rookSc, board)) {
                    val step = if (dc > sc) 1 else -1
                    !isInCheck(board, piece.color, lastPawnDoubleJump) && !wouldBeInCheck(sr, sc, sr, sc + step, board, piece.color, lastPawnDoubleJump) && !wouldBeInCheck(sr, sc, sr, sc + 2 * step, board, piece.color, lastPawnDoubleJump)
                } else false
            } else false
        }
    }
}

fun isPathClear(sr: Int, sc: Int, dr: Int, dc: Int, board: List<List<ChessPiece?>>): Boolean {
    val rDir = if (dr > sr) 1 else if (dr < sr) -1 else 0
    val cDir = if (dc > sc) 1 else if (dc < sc) -1 else 0
    var currR = sr + rDir; var currC = sc + cDir
    if (currR == dr && currC == dc) return true
    while (currR != dr || currC != dc) {
        if (board[currR][currC] != null) return false
        currR += rDir; currC += cDir
    }
    return true
}

fun getAllValidMoves(board: List<List<ChessPiece?>>, color: PieceColor, lastPawnDoubleJump: Pair<Int, Int>? = null): List<Pair<Pair<Int, Int>, Pair<Int, Int>>> {
    val moves = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
    for (r in 0 until 8) for (c in 0 until 8) {
        if (board[r][c]?.color == color) {
            for (dr in 0 until 8) for (dc in 0 until 8) {
                if (isValidMove(r, c, dr, dc, board, lastPawnDoubleJump)) moves.add((r to c) to (dr to dc))
            }
        }
    }
    return moves
}

fun findBestMove(board: List<List<ChessPiece?>>, color: PieceColor, depth: Int, lastPawnDoubleJump: Pair<Int, Int>?): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
    val moves = getAllValidMoves(board, color, lastPawnDoubleJump).filter { !wouldBeInCheck(it.first.first, it.first.second, it.second.first, it.second.second, board, color, lastPawnDoubleJump) }
    if (moves.isEmpty()) return null
    val sortedMoves = moves.sortedByDescending { move -> board[move.second.first][move.second.second]?.type?.ordinal ?: -1 }
    var bestScore = if (color == PieceColor.MAGENTA) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
    var bestMove = sortedMoves.first()
    for (move in sortedMoves) {
        val nextBoard = performMove(move.first.first, move.first.second, move.second.first, move.second.second, board, lastPawnDoubleJump)
        val score = minimax(nextBoard, depth - 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, color != PieceColor.MAGENTA, lastPawnDoubleJump)
        if (color == PieceColor.MAGENTA) { if (score > bestScore) { bestScore = score; bestMove = move } } else { if (score < bestScore) { bestScore = score; bestMove = move } }
    }
    return bestMove
}

fun minimax(board: List<List<ChessPiece?>>, depth: Int, alpha: Double, beta: Double, isMaximizing: Boolean, lastPawnDoubleJump: Pair<Int, Int>?): Double {
    if (depth == 0) return evaluateBoard(board)
    var curAlpha = alpha; var curBeta = beta
    if (isMaximizing) {
        var maxEval = Double.NEGATIVE_INFINITY
        val moves = getAllValidMoves(board, PieceColor.MAGENTA, lastPawnDoubleJump).filter { !wouldBeInCheck(it.first.first, it.first.second, it.second.first, it.second.second, board, PieceColor.MAGENTA, lastPawnDoubleJump) }
        if (moves.isEmpty()) return if (isInCheck(board, PieceColor.MAGENTA, lastPawnDoubleJump)) -10000.0 else 0.0
        for (move in moves) {
            val eval = minimax(performMove(move.first.first, move.first.second, move.second.first, move.second.second, board, lastPawnDoubleJump), depth - 1, curAlpha, curBeta, false, null)
            maxEval = max(maxEval, eval); curAlpha = max(curAlpha, eval)
            if (curBeta <= curAlpha) break
        }
        return maxEval
    } else {
        var minEval = Double.POSITIVE_INFINITY
        val moves = getAllValidMoves(board, PieceColor.CYAN, lastPawnDoubleJump).filter { !wouldBeInCheck(it.first.first, it.first.second, it.second.first, it.second.second, board, PieceColor.CYAN, lastPawnDoubleJump) }
        if (moves.isEmpty()) return if (isInCheck(board, PieceColor.CYAN, lastPawnDoubleJump)) 10000.0 else 0.0
        for (move in moves) {
            val eval = minimax(performMove(move.first.first, move.first.second, move.second.first, move.second.second, board, lastPawnDoubleJump), depth - 1, curAlpha, curBeta, true, null)
            minEval = min(minEval, eval); curBeta = min(curBeta, eval)
            if (curBeta <= curAlpha) break
        }
        return minEval
    }
}

fun evaluateBoard(board: List<List<ChessPiece?>>): Double {
    var score = 0.0
    for (r in 0 until 8) for (c in 0 until 8) {
        board[r][c]?.let { piece ->
            var valWeight = when (piece.type) { PieceType.PAWN -> 10.0; PieceType.KNIGHT -> 32.0; PieceType.BISHOP -> 33.0; PieceType.ROOK -> 50.0; PieceType.QUEEN -> 90.0; PieceType.KING -> 900.0 }
            valWeight += (10.0 - (abs(3.5 - r) + abs(3.5 - c))) * 0.5
            if (piece.color == PieceColor.MAGENTA) score += valWeight else score -= valWeight
        }
    }
    return score
}

fun getViElo(playerElo: Int): Int = when { playerElo < 800 -> 800; playerElo < 1200 -> 1200; playerElo < 1600 -> 1600; else -> 2000 }
fun calculateNewElo(oldElo: Int, opponentElo: Int, score: Double): Int { val kFactor = 32; val expectedScore = 1.0 / (1.0 + 10.0.pow((opponentElo - oldElo).toDouble() / 400.0)); return (oldElo + kFactor * (score - expectedScore)).toInt().coerceAtLeast(100) }
fun getAdaptiveDepth(playerElo: Int): Int = when { playerElo < 800 -> 1; playerElo < 1200 -> 2; playerElo < 1600 -> 3; else -> 4 }

fun initialBoard(): List<List<ChessPiece?>> {
    val board = MutableList(8) { MutableList<ChessPiece?>(8) { null } }
    val magenta = PieceColor.MAGENTA; val cyan = PieceColor.CYAN
    board[0][0] = ChessPiece(PieceType.ROOK, magenta); board[0][1] = ChessPiece(PieceType.KNIGHT, magenta); board[0][2] = ChessPiece(PieceType.BISHOP, magenta); board[0][3] = ChessPiece(PieceType.QUEEN, magenta); board[0][4] = ChessPiece(PieceType.KING, magenta); board[0][5] = ChessPiece(PieceType.BISHOP, magenta); board[0][6] = ChessPiece(PieceType.KNIGHT, magenta); board[0][7] = ChessPiece(PieceType.ROOK, magenta)
    for (i in 0 until 8) board[1][i] = ChessPiece(PieceType.PAWN, magenta)
    board[7][0] = ChessPiece(PieceType.ROOK, cyan); board[7][1] = ChessPiece(PieceType.KNIGHT, cyan); board[7][2] = ChessPiece(PieceType.BISHOP, cyan); board[7][3] = ChessPiece(PieceType.QUEEN, cyan); board[7][4] = ChessPiece(PieceType.KING, cyan); board[7][5] = ChessPiece(PieceType.BISHOP, cyan); board[7][6] = ChessPiece(PieceType.KNIGHT, cyan); board[7][7] = ChessPiece(PieceType.ROOK, cyan)
    for (i in 0 until 8) board[6][i] = ChessPiece(PieceType.PAWN, cyan)
    return board.map { it.toList() }
}

@Composable
fun ModeSelectionScreen(onModeSelect: (GameMode) -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
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
