package com.neon.ascent.feature.games

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class PieceType { PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING }
enum class PieceColor { CYAN, MAGENTA }
enum class GameMode { SINGLE_PLAYER, TWO_PLAYER }

data class ChessPiece(val type: PieceType, val color: PieceColor, val hasMoved: Boolean = false)

object ChessEngine {
    fun initialBoard(): List<List<ChessPiece?>> {
        val board = MutableList(8) { MutableList<ChessPiece?>(8) { null } }
        val magenta = PieceColor.MAGENTA; val cyan = PieceColor.CYAN
        board[0][0] = ChessPiece(PieceType.ROOK, magenta); board[0][1] = ChessPiece(PieceType.KNIGHT, magenta); board[0][2] = ChessPiece(PieceType.BISHOP, magenta); board[0][3] = ChessPiece(PieceType.QUEEN, magenta); board[0][4] = ChessPiece(PieceType.KING, magenta); board[0][5] = ChessPiece(PieceType.BISHOP, magenta); board[0][6] = ChessPiece(PieceType.KNIGHT, magenta); board[0][7] = ChessPiece(PieceType.ROOK, magenta)
        for (i in 0 until 8) board[1][i] = ChessPiece(PieceType.PAWN, magenta)
        board[7][0] = ChessPiece(PieceType.ROOK, cyan); board[7][1] = ChessPiece(PieceType.KNIGHT, cyan); board[7][2] = ChessPiece(PieceType.BISHOP, cyan); board[7][3] = ChessPiece(PieceType.QUEEN, cyan); board[7][4] = ChessPiece(PieceType.KING, cyan); board[7][5] = ChessPiece(PieceType.BISHOP, cyan); board[7][6] = ChessPiece(PieceType.KNIGHT, cyan); board[7][7] = ChessPiece(PieceType.ROOK, cyan)
        for (i in 0 until 8) board[6][i] = ChessPiece(PieceType.PAWN, cyan)
        return board.map { it.toList() }
    }

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
}
