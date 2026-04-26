package com.neon.ascent.feature.games

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class CyberChessViewModel @Inject constructor() : ViewModel() {
    private val _gameMode = MutableStateFlow<GameMode?>(null)
    val gameMode: StateFlow<GameMode?> = _gameMode.asStateFlow()

    private val _board = MutableStateFlow(ChessEngine.initialBoard())
    val board: StateFlow<List<List<ChessPiece?>>> = _board.asStateFlow()

    private val _selectedSquare = MutableStateFlow<Pair<Int, Int>?>(null)
    val selectedSquare: StateFlow<Pair<Int, Int>?> = _selectedSquare.asStateFlow()

    private val _turn = MutableStateFlow(PieceColor.CYAN)
    val turn: StateFlow<PieceColor> = _turn.asStateFlow()

    private val _lastMove = MutableStateFlow<Pair<Pair<Int, Int>, Pair<Int, Int>>?>(null)
    val lastMove: StateFlow<Pair<Pair<Int, Int>, Pair<Int, Int>>?> = _lastMove.asStateFlow()

    private val _lastPawnDoubleJump = MutableStateFlow<Pair<Int, Int>?>(null)
    val lastPawnDoubleJump: StateFlow<Pair<Int, Int>?> = _lastPawnDoubleJump.asStateFlow()

    private val _eloScore = MutableStateFlow(1000)
    val eloScore: StateFlow<Int> = _eloScore.asStateFlow()

    private val _gameResult = MutableStateFlow<String?>(null)
    val gameResult: StateFlow<String?> = _gameResult.asStateFlow()

    val systemLogs = mutableStateListOf("> INITIALIZING NEURAL_CHESS_ENGINE...")

    private val viName = "AETHER_NULL // V.2.4"

    fun selectMode(mode: GameMode) {
        _gameMode.value = mode
    }

    fun resetGame() {
        _gameMode.value = null
        _board.value = ChessEngine.initialBoard()
        _turn.value = PieceColor.CYAN
        _gameResult.value = null
        _lastMove.value = null
        _lastPawnDoubleJump.value = null
        _selectedSquare.value = null
        systemLogs.clear()
        systemLogs.add("> REBOOTING_GAME_PROTOCOL...")
    }

    fun onSquareClick(r: Int, c: Int) {
        if (_gameResult.value != null) return
        if (_gameMode.value == GameMode.SINGLE_PLAYER && _turn.value == PieceColor.MAGENTA) return

        val currentBoard = _board.value
        val currentTurn = _turn.value
        val selected = _selectedSquare.value
        val piece = currentBoard[r][c]

        if (selected == null) {
            if (piece?.color == currentTurn) {
                _selectedSquare.value = r to c
            }
        } else {
            val (sr, sc) = selected
            if (ChessEngine.isValidMove(sr, sc, r, c, currentBoard, _lastPawnDoubleJump.value) && 
                !ChessEngine.wouldBeInCheck(sr, sc, r, c, currentBoard, currentTurn, _lastPawnDoubleJump.value)) {
                
                executeMove(sr, sc, r, c)
            } else {
                if (piece?.color == currentTurn) {
                    _selectedSquare.value = r to c
                } else {
                    _selectedSquare.value = null
                }
            }
        }
    }

    private fun executeMove(sr: Int, sc: Int, dr: Int, dc: Int) {
        val currentBoard = _board.value
        val isPawnDoubleJump = currentBoard[sr][sc]?.type == PieceType.PAWN && abs(dr - sr) == 2
        
        val nextBoard = ChessEngine.performMove(sr, sc, dr, dc, currentBoard, _lastPawnDoubleJump.value)
        _board.value = nextBoard
        _lastMove.value = (sr to sc) to (dr to dc)
        _lastPawnDoubleJump.value = if (isPawnDoubleJump) dr to dc else null
        _selectedSquare.value = null
        
        systemLogs.add(0, "> MOVE_EXECUTED: ${ChessEngine.coord(sr, sc)} TO ${ChessEngine.coord(dr, dc)}")
        
        val nextTurn = if (_turn.value == PieceColor.CYAN) PieceColor.MAGENTA else PieceColor.CYAN
        checkGameState(nextBoard, nextTurn)
    }

    private fun checkGameState(board: List<List<ChessPiece?>>, nextTurn: PieceColor) {
        if (ChessEngine.isCheckmate(board, nextTurn, _lastPawnDoubleJump.value)) {
            val winner = if (nextTurn == PieceColor.MAGENTA) "PLAYER WINS" else "$viName WINS"
            _gameResult.value = "CHECKMATE // $winner"
            
            val winScore = if (nextTurn == PieceColor.MAGENTA) 1.0 else 0.0
            _eloScore.value = ChessEngine.calculateNewElo(_eloScore.value, ChessEngine.getViElo(_eloScore.value), winScore)
            
            if (nextTurn == PieceColor.MAGENTA) {
                systemLogs.add(0, "> VI_DEFEATED: ELO_UPGRADE_SYNCED")
            } else {
                systemLogs.add(0, "> NEURAL_LINK_SEVERED: YOU LOSE")
            }
        } else if (ChessEngine.isDraw(board, nextTurn, _lastPawnDoubleJump.value)) {
            _gameResult.value = "STALEMATE // DRAW"
            _eloScore.value = ChessEngine.calculateNewElo(_eloScore.value, ChessEngine.getViElo(_eloScore.value), 0.5)
            systemLogs.add(0, "> DRAW_DETECTED: ELO_CALIBRATED")
        } else {
            if (ChessEngine.isInCheck(board, nextTurn, _lastPawnDoubleJump.value)) {
                systemLogs.add(0, if (nextTurn == PieceColor.CYAN) "> HAZARD: NEURAL_CORE_THREATENED" else "> WARNING: KING_UNDER_ATTACK")
            }
            _turn.value = nextTurn
            
            if (_gameMode.value == GameMode.SINGLE_PLAYER && nextTurn == PieceColor.MAGENTA) {
                triggerAiMove()
            }
        }
    }

    private fun triggerAiMove() {
        viewModelScope.launch {
            delay(1500)
            val bestMove = withContext(Dispatchers.Default) {
                val depth = ChessEngine.getAdaptiveDepth(_eloScore.value)
                ChessEngine.findBestMove(_board.value, PieceColor.MAGENTA, depth, _lastPawnDoubleJump.value)
            }
            
            bestMove?.let { (from, to) ->
                executeMove(from.first, from.second, to.first, to.second)
            }
        }
    }
}
