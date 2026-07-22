package com.example.chessapp.domain

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

class ChessAI {

    suspend fun getBestMove(game: ChessGame, depth: Int = 3): Move? = withContext(Dispatchers.Default) {
        val color = game.currentTurn.value
        var bestMove: Move? = null
        var bestValue = if (color == PieceColor.WHITE) Int.MIN_VALUE else Int.MAX_VALUE

        val legalMoves = getAllLegalMoves(game, color)
        
        for (move in legalMoves) {
            val nextGame = game.copy()
            nextGame.makeMove(move)
            
            val value = minimax(nextGame, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, color.opposite())
            
            if (color == PieceColor.WHITE) {
                if (value > bestValue || bestMove == null) {
                    bestValue = value
                    bestMove = move
                }
            } else {
                if (value < bestValue || bestMove == null) {
                    bestValue = value
                    bestMove = move
                }
            }
        }
        
        return@withContext bestMove
    }

    private fun minimax(game: ChessGame, depth: Int, alpha: Int, beta: Int, turn: PieceColor): Int {
        if (depth == 0 || game.status.value !is GameStatus.Active) {
            return evaluateBoard(game, depth)
        }

        val legalMoves = getAllLegalMoves(game, turn)
        if (legalMoves.isEmpty()) {
            return evaluateBoard(game, depth)
        }

        var currentAlpha = alpha
        var currentBeta = beta

        if (turn == PieceColor.WHITE) {
            var maxEval = Int.MIN_VALUE
            for (move in legalMoves) {
                val nextGame = game.copy()
                nextGame.makeMove(move)
                val eval = minimax(nextGame, depth - 1, currentAlpha, currentBeta, PieceColor.BLACK)
                maxEval = max(maxEval, eval)
                currentAlpha = max(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in legalMoves) {
                val nextGame = game.copy()
                nextGame.makeMove(move)
                val eval = minimax(nextGame, depth - 1, currentAlpha, currentBeta, PieceColor.WHITE)
                minEval = min(minEval, eval)
                currentBeta = min(currentBeta, eval)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }

    private fun getAllLegalMoves(game: ChessGame, color: PieceColor): List<Move> {
        val allMoves = mutableListOf<Move>()
        for (row in 0..7) {
            for (col in 0..7) {
                val pos = Position(row, col)
                val piece = game.board.value.getPiece(pos)
                if (piece != null && piece.color == color) {
                    allMoves.addAll(game.getLegalMoves(pos))
                }
            }
        }
        // Basic move ordering: captures first to improve alpha-beta pruning
        return allMoves.sortedByDescending { it.capturedPiece != null }
    }

    private fun evaluateBoard(game: ChessGame, depth: Int = 0): Int {
        val status = game.status.value
        if (status is GameStatus.Checkmate) {
            return if (status.winner == PieceColor.WHITE) 100000 + depth else -100000 - depth
        }
        if (status is GameStatus.Stalemate) {
            return 0
        }

        var score = 0
        for ((_, piece) in game.board.value.pieces) {
            val value = getPieceValue(piece.type)
            if (piece.color == PieceColor.WHITE) {
                score += value
            } else {
                score -= value
            }
        }
        return score
    }

    private fun getPieceValue(type: PieceType): Int {
        return when (type) {
            PieceType.PAWN -> 10
            PieceType.KNIGHT -> 30
            PieceType.BISHOP -> 30
            PieceType.ROOK -> 50
            PieceType.QUEEN -> 90
            PieceType.KING -> 900 // High value so AI doesn't sacrifice it if it could (though rules prevent this, it's good practice)
        }
    }
}
