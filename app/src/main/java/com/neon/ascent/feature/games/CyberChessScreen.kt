package com.neon.ascent.feature.games

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neon.ascent.feature.charactercreation.CyberButtonShape
import com.neon.ascent.feature.charactercreation.CyberGridBackground
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

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

    if (gameMode == null) {
        ModeSelectionScreen(onModeSelect = { gameMode = it }, onBack = onBack)
    } else {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            CyberGridBackground()
            
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { gameMode = null; board = initialBoard(); turn = PieceColor.CYAN; gameResult = null; lastMove = null; lastPawnDoubleJump = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF00FF9C))
                    }
                    Column {
                        Text(
                            "CYBER_CHESS",
                            color = Color(0xFF00FF9C),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black
                        )
                        if (gameMode == GameMode.SINGLE_PLAYER) {
                            Text(
                                "VS $viName // ELO: $eloScore",
                                color = Color(0xFFFF006E),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                "LOCAL_LINK // MULTI_PLAYER",
                                color = Color(0xFF00FFFF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Turn/Result Indicator
                Text(
                    text = gameResult ?: if (turn == PieceColor.CYAN) "PLAYER_TURN // CYAN" else if (gameMode == GameMode.SINGLE_PLAYER) "VI_THINKING // $viName" else "OPPONENT_TURN // MAGENTA",
                    color = if (gameResult != null) Color.White else if (turn == PieceColor.CYAN) Color.Cyan else Color(0xFFFF006E),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Chess Board
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .border(2.dp, Color(0xFF00FF9C).copy(alpha = 0.5f))
                        .background(Color(0xFF0A0A0A))
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
                            .border(1.dp, Color(0xFF00FF9C), CyberButtonShape),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A))
                    ) {
                        Text("REBOOT_SESSION", color = Color(0xFF00FF9C), fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // Console output
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, Color(0xFF00FF9C).copy(alpha = 0.2f))
                        .background(Color(0xFF050505))
                        .padding(8.dp)
                ) {
                    Column {
                        systemLogs.take(5).forEach { log ->
                            Text(
                                text = log,
                                color = Color(0xFF00FF9C).copy(alpha = 0.7f),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Elo and Difficulty Scaling ---

fun getViElo(playerElo: Int): Int {
    return when {
        playerElo < 800 -> 800
        playerElo < 1200 -> 1200
        playerElo < 1600 -> 1600
        else -> 2000
    }
}

fun calculateNewElo(oldElo: Int, opponentElo: Int, score: Double): Int {
    val kFactor = 32
    val expectedScore = 1.0 / (1.0 + 10.0.pow((opponentElo - oldElo).toDouble() / 400.0))
    val newElo = oldElo + (kFactor * (score - expectedScore)).toInt()
    return max(100, newElo)
}

fun getAdaptiveDepth(playerElo: Int): Int {
    return when {
        playerElo < 800 -> 1
        playerElo < 1200 -> 2
        playerElo < 1600 -> 3
        else -> 4
    }
}

// --- UI Components ---

@Composable
fun ModeSelectionScreen(onModeSelect: (GameMode) -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        CyberGridBackground()
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("SELECT_LINK_PROTOCOL", color = Color(0xFF00FF9C), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            
            ModeButton(label = "SINGLE_PLAYER // VS AETHER_NULL", color = Color(0xFFFF006E)) {
                onModeSelect(GameMode.SINGLE_PLAYER)
            }
            
            ModeButton(label = "TWO_PLAYER // LOCAL_LINK", color = Color(0xFF00FFFF)) {
                onModeSelect(GameMode.TWO_PLAYER)
            }
            
            Spacer(Modifier.height(16.dp))
            
            TextButton(onClick = onBack) {
                Text("TERMINATE_SESSION", color = Color.Gray)
            }
        }
    }
}

@Composable
fun ModeButton(label: String, color: Color, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .height(56.dp)
            .clip(CyberButtonShape)
            .border(1.dp, color, CyberButtonShape),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A0A0A))
    ) {
        Text(label, color = color, fontWeight = FontWeight.Black, fontSize = 12.sp)
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
                                    isSelected -> Color(0xFF00FF9C).copy(alpha = 0.3f)
                                    isLastMove -> Color.White.copy(alpha = 0.1f)
                                    isDark -> Color(0xFF111111)
                                    else -> Color(0xFF050505)
                                }
                            )
                            .border(0.5.dp, Color(0xFF00FF9C).copy(alpha = 0.05f))
                            .clickable { onSquareClick(r, c) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPossibleMove) {
                            val dotColor = if (board[r][c] != null) Color(0xFFFF006E).copy(alpha = 0.5f) else Color(0xFF00FF9C).copy(alpha = 0.4f)
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(dotColor))
                        }
                        
                        board[r][c]?.let { piece ->
                            CyberPiece(piece)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CyberPiece(piece: ChessPiece) {
    val color = if (piece.color == PieceColor.CYAN) Color.Cyan else Color(0xFFFF006E)
    val infiniteTransition = rememberInfiniteTransition(label = "PiecePulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
        label = "Alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize(0.7f).alpha(alpha)) {
        when (piece.type) {
            PieceType.PAWN -> drawCyberPawn(color)
            PieceType.ROOK -> drawCyberRook(color)
            PieceType.KNIGHT -> drawCyberKnight(color)
            PieceType.BISHOP -> drawCyberBishop(color)
            PieceType.QUEEN -> drawCyberQueen(color)
            PieceType.KING -> drawCyberKing(color)
        }
    }
}

// --- Engine Logic ---

fun coord(r: Int, c: Int): String = "${('A' + c)}${8 - r}"

fun performMove(sr: Int, sc: Int, dr: Int, dc: Int, board: List<List<ChessPiece?>>, lastPawnDoubleJump: Pair<Int, Int>?): List<List<ChessPiece?>> {
    val newBoard = board.map { it.toMutableList() }
    val piece = newBoard[sr][sc] ?: return board
    
    // En Passant capture
    if (piece.type == PieceType.PAWN && sc != dc && newBoard[dr][dc] == null) {
        newBoard[sr][dc] = null
    }
    
    // Castling rook move
    if (piece.type == PieceType.KING && abs(dc - sc) == 2) {
        val rookSc = if (dc > sc) 7 else 0
        val rookDc = if (dc > sc) 5 else 3
        newBoard[sr][rookDc] = newBoard[sr][rookSc]?.copy(hasMoved = true)
        newBoard[sr][rookSc] = null
    }

    newBoard[dr][dc] = piece.copy(hasMoved = true)
    newBoard[sr][sc] = null
    
    // Pawn neural-uplink (promotion)
    if (piece.type == PieceType.PAWN) {
        if ((piece.color == PieceColor.CYAN && dr == 0) || (piece.color == PieceColor.MAGENTA && dr == 7)) {
            newBoard[dr][dc] = piece.copy(type = PieceType.QUEEN, hasMoved = true)
        }
    }
    
    return newBoard.map { it.toList() }
}

fun isInCheck(board: List<List<ChessPiece?>>, color: PieceColor, lastPawnDoubleJump: Pair<Int, Int>?): Boolean {
    var kingPos: Pair<Int, Int>? = null
    for (r in 0 until 8) {
        for (c in 0 until 8) {
            val p = board[r][c]
            if (p?.type == PieceType.KING && p.color == color) {
                kingPos = r to c
                break
            }
        }
    }
    if (kingPos == null) return false
    
    val oppColor = if (color == PieceColor.CYAN) PieceColor.MAGENTA else PieceColor.CYAN
    for (r in 0 until 8) {
        for (c in 0 until 8) {
            if (board[r][c]?.color == oppColor) {
                if (isValidMove(r, c, kingPos.first, kingPos.second, board, lastPawnDoubleJump, checkingAttacksOnly = true)) return true
            }
        }
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

fun isValidMove(
    sr: Int, sc: Int, dr: Int, dc: Int, 
    board: List<List<ChessPiece?>>, 
    lastPawnDoubleJump: Pair<Int, Int>?,
    checkingAttacksOnly: Boolean = false
): Boolean {
    val piece = board[sr][sc] ?: return false
    val destPiece = board[dr][dc]
    if (destPiece?.color == piece.color) return false
    
    val drAbs = abs(dr - sr)
    val dcAbs = abs(dc - sc)
    
    return when (piece.type) {
        PieceType.PAWN -> {
            val dir = if (piece.color == PieceColor.CYAN) -1 else 1
            if (dc == sc) {
                if (checkingAttacksOnly) false
                else if (dr == sr + dir) destPiece == null
                else if (dr == sr + 2 * dir && !piece.hasMoved && sr == (if (piece.color == PieceColor.CYAN) 6 else 1)) {
                    destPiece == null && board[sr + dir][sc] == null
                } else false
            } else if (dcAbs == 1 && dr == sr + dir) {
                if (destPiece != null) true
                else {
                    // En Passant
                    lastPawnDoubleJump != null && lastPawnDoubleJump.first == sr && lastPawnDoubleJump.second == dc
                }
            } else false
        }
        PieceType.ROOK -> (sr == dr || sc == dc) && isPathClear(sr, sc, dr, dc, board)
        PieceType.KNIGHT -> (drAbs == 2 && dcAbs == 1) || (drAbs == 1 && dcAbs == 2)
        PieceType.BISHOP -> drAbs == dcAbs && isPathClear(sr, sc, dr, dc, board)
        PieceType.QUEEN -> (drAbs == dcAbs || sr == dr || sc == dc) && isPathClear(sr, sc, dr, dc, board)
        PieceType.KING -> {
            if (drAbs <= 1 && dcAbs <= 1) true
            else if (!checkingAttacksOnly && !piece.hasMoved && dr == sr && dcAbs == 2) {
                val rookSc = if (dc > sc) 7 else 0
                val rook = board[sr][rookSc]
                if (rook?.type == PieceType.ROOK && !rook.hasMoved && isPathClear(sr, sc, sr, rookSc, board)) {
                    val step = if (dc > sc) 1 else -1
                    !isInCheck(board, piece.color, lastPawnDoubleJump) && 
                    !wouldBeInCheck(sr, sc, sr, sc + step, board, piece.color, lastPawnDoubleJump) &&
                    !wouldBeInCheck(sr, sc, sr, sc + 2 * step, board, piece.color, lastPawnDoubleJump)
                } else false
            } else false
        }
    }
}

fun isPathClear(sr: Int, sc: Int, dr: Int, dc: Int, board: List<List<ChessPiece?>>): Boolean {
    val rDir = if (dr > sr) 1 else if (dr < sr) -1 else 0
    val cDir = if (dc > sc) 1 else if (dc < sc) -1 else 0
    var currR = sr + rDir
    var currC = sc + cDir
    if (currR == dr && currC == dc) return true
    while (currR != dr || currC != dc) {
        if (board[currR][currC] != null) return false
        currR += rDir
        currC += cDir
    }
    return true
}

fun getAllValidMoves(board: List<List<ChessPiece?>>, color: PieceColor, lastPawnDoubleJump: Pair<Int, Int>?): List<Pair<Pair<Int, Int>, Pair<Int, Int>>> {
    val moves = mutableListOf<Pair<Pair<Int, Int>, Pair<Int, Int>>>()
    for (r in 0 until 8) {
        for (c in 0 until 8) {
            if (board[r][c]?.color == color) {
                for (dr in 0 until 8) {
                    for (dc in 0 until 8) {
                        if (isValidMove(r, c, dr, dc, board, lastPawnDoubleJump)) {
                            moves.add((r to c) to (dr to dc))
                        }
                    }
                }
            }
        }
    }
    return moves
}

// AI Engine
fun findBestMove(board: List<List<ChessPiece?>>, color: PieceColor, depth: Int, lastPawnDoubleJump: Pair<Int, Int>?): Pair<Pair<Int, Int>, Pair<Int, Int>>? {
    val moves = getAllValidMoves(board, color, lastPawnDoubleJump).filter { !wouldBeInCheck(it.first.first, it.first.second, it.second.first, it.second.second, board, color, lastPawnDoubleJump) }
    if (moves.isEmpty()) return null
    
    val sortedMoves = moves.sortedByDescending { move ->
        val target = board[move.second.first][move.second.second]
        target?.type?.ordinal ?: -1
    }
    
    var bestScore = if (color == PieceColor.MAGENTA) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
    var bestMove = sortedMoves.first()
    
    for (move in sortedMoves) {
        val nextBoard = performMove(move.first.first, move.first.second, move.second.first, move.second.second, board, lastPawnDoubleJump)
        val score = minimax(nextBoard, depth - 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, color != PieceColor.MAGENTA, lastPawnDoubleJump)
        
        if (color == PieceColor.MAGENTA) {
            if (score > bestScore) {
                bestScore = score
                bestMove = move
            }
        } else {
            if (score < bestScore) {
                bestScore = score
                bestMove = move
            }
        }
    }
    return bestMove
}

fun minimax(board: List<List<ChessPiece?>>, depth: Int, alpha: Double, beta: Double, isMaximizing: Boolean, lastPawnDoubleJump: Pair<Int, Int>?): Double {
    if (depth == 0) return evaluateBoard(board)
    
    var currentAlpha = alpha
    var currentBeta = beta
    
    if (isMaximizing) {
        var maxEval = Double.NEGATIVE_INFINITY
        val moves = getAllValidMoves(board, PieceColor.MAGENTA, lastPawnDoubleJump).filter { !wouldBeInCheck(it.first.first, it.first.second, it.second.first, it.second.second, board, PieceColor.MAGENTA, lastPawnDoubleJump) }
        if (moves.isEmpty()) return if (isInCheck(board, PieceColor.MAGENTA, lastPawnDoubleJump)) -10000.0 else 0.0
        
        for (move in moves) {
            val eval = minimax(performMove(move.first.first, move.first.second, move.second.first, move.second.second, board, lastPawnDoubleJump), depth - 1, currentAlpha, currentBeta, false, null)
            maxEval = max(maxEval, eval)
            currentAlpha = max(currentAlpha, eval)
            if (currentBeta <= currentAlpha) break
        }
        return maxEval
    } else {
        var minEval = Double.POSITIVE_INFINITY
        val moves = getAllValidMoves(board, PieceColor.CYAN, lastPawnDoubleJump).filter { !wouldBeInCheck(it.first.first, it.first.second, it.second.first, it.second.second, board, PieceColor.CYAN, lastPawnDoubleJump) }
        if (moves.isEmpty()) return if (isInCheck(board, PieceColor.CYAN, lastPawnDoubleJump)) 10000.0 else 0.0
        
        for (move in moves) {
            val eval = minimax(performMove(move.first.first, move.first.second, move.second.first, move.second.second, board, lastPawnDoubleJump), depth - 1, currentAlpha, currentBeta, true, null)
            minEval = min(minEval, eval)
            currentBeta = min(currentBeta, eval)
            if (currentBeta <= currentAlpha) break
        }
        return minEval
    }
}

fun evaluateBoard(board: List<List<ChessPiece?>>): Double {
    var score = 0.0
    for (r in 0 until 8) {
        for (c in 0 until 8) {
            board[r][c]?.let { piece ->
                var valWeight = when (piece.type) {
                    PieceType.PAWN -> 10.0
                    PieceType.KNIGHT -> 32.0
                    PieceType.BISHOP -> 33.0
                    PieceType.ROOK -> 50.0
                    PieceType.QUEEN -> 90.0
                    PieceType.KING -> 900.0
                }
                val distFromCenter = (abs(3.5 - r) + abs(3.5 - c))
                valWeight += (10.0 - distFromCenter) * 0.5
                if (piece.color == PieceColor.MAGENTA) score += valWeight else score -= valWeight
            }
        }
    }
    return score
}

// --- Drawing Utils ---

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCyberPawn(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.2f)
        lineTo(w * 0.7f, h * 0.5f)
        lineTo(w * 0.8f, h * 0.8f)
        lineTo(w * 0.2f, h * 0.8f)
        lineTo(w * 0.3f, h * 0.5f)
        close()
    }
    drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
    drawCircle(color.copy(alpha = 0.3f), radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.4f))
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCyberRook(color: Color) {
    val w = size.width
    val h = size.height
    drawRect(color, Offset(w * 0.2f, h * 0.2f), Size(w * 0.6f, h * 0.6f), style = Stroke(width = 2.dp.toPx()))
    drawLine(color, Offset(w * 0.2f, h * 0.4f), Offset(w * 0.8f, h * 0.4f), strokeWidth = 1.dp.toPx())
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCyberKnight(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.3f, h * 0.8f)
        lineTo(w * 0.3f, h * 0.4f)
        lineTo(w * 0.7f, h * 0.2f)
        lineTo(w * 0.8f, h * 0.4f)
        lineTo(w * 0.5f, h * 0.5f)
        lineTo(w * 0.7f, h * 0.8f)
        close()
    }
    drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCyberBishop(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.1f)
        lineTo(w * 0.8f, h * 0.5f)
        lineTo(w * 0.5f, h * 0.9f)
        lineTo(w * 0.2f, h * 0.5f)
        close()
    }
    drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
    drawLine(color, Offset(w * 0.5f, h * 0.1f), Offset(w * 0.5f, h * 0.9f), strokeWidth = 1.dp.toPx())
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCyberQueen(color: Color) {
    val w = size.width
    val h = size.height
    drawCircle(color, radius = w * 0.4f, center = center, style = Stroke(width = 2.dp.toPx()))
    drawCircle(color, radius = w * 0.2f, center = center)
}

fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCyberKing(color: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.1f)
        lineTo(w * 0.9f, h * 0.3f)
        lineTo(w * 0.9f, h * 0.7f)
        lineTo(w * 0.5f, h * 0.9f)
        lineTo(w * 0.1f, h * 0.7f)
        lineTo(w * 0.1f, h * 0.3f)
        close()
    }
    drawPath(path, color, style = Stroke(width = 2.dp.toPx()))
    drawRect(color, Offset(w * 0.4f, h * 0.4f), Size(w * 0.2f, h * 0.2f))
}

fun initialBoard(): List<List<ChessPiece?>> {
    val board = MutableList(8) { MutableList<ChessPiece?>(8) { null } }
    val magentaColor = PieceColor.MAGENTA
    board[0][0] = ChessPiece(PieceType.ROOK, magentaColor)
    board[0][1] = ChessPiece(PieceType.KNIGHT, magentaColor)
    board[0][2] = ChessPiece(PieceType.BISHOP, magentaColor)
    board[0][3] = ChessPiece(PieceType.QUEEN, magentaColor)
    board[0][4] = ChessPiece(PieceType.KING, magentaColor)
    board[0][5] = ChessPiece(PieceType.BISHOP, magentaColor)
    board[0][6] = ChessPiece(PieceType.KNIGHT, magentaColor)
    board[0][7] = ChessPiece(PieceType.ROOK, magentaColor)
    for (i in 0 until 8) board[1][i] = ChessPiece(PieceType.PAWN, magentaColor)
    
    val cyanColor = PieceColor.CYAN
    board[7][0] = ChessPiece(PieceType.ROOK, cyanColor)
    board[7][1] = ChessPiece(PieceType.KNIGHT, cyanColor)
    board[7][2] = ChessPiece(PieceType.BISHOP, cyanColor)
    board[7][3] = ChessPiece(PieceType.QUEEN, cyanColor)
    board[7][4] = ChessPiece(PieceType.KING, cyanColor)
    board[7][5] = ChessPiece(PieceType.BISHOP, cyanColor)
    board[7][6] = ChessPiece(PieceType.KNIGHT, cyanColor)
    board[7][7] = ChessPiece(PieceType.ROOK, cyanColor)
    for (i in 0 until 8) board[6][i] = ChessPiece(PieceType.PAWN, cyanColor)
    
    return board.map { it.toList() }
}
