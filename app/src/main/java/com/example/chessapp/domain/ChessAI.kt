package com.example.chessapp.domain

import com.example.chessapp.ui.AiDifficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ChessAI {

    suspend fun getBestMove(game: ChessGame, difficulty: AiDifficulty): Move? = withContext(Dispatchers.Default) {
        val color = game.currentTurn.value
        val legalMoves = getAllLegalMoves(game, color)
        if (legalMoves.isEmpty()) return@withContext null

        val depth = difficulty.depth

        // For beginner balance:
        // Easy AI: 35% chance to play a random non-blundering move to simulate a beginner human player
        if (difficulty == AiDifficulty.EASY && Random.nextFloat() < 0.35f) {
            val nonBlunderMoves = legalMoves.filter { move ->
                val nextGame = game.copy()
                nextGame.makeMove(move)
                val eval = evaluateBoard(nextGame)
                if (color == PieceColor.WHITE) eval > -300 else eval < 300
            }
            if (nonBlunderMoves.isNotEmpty()) {
                return@withContext nonBlunderMoves.random()
            }
            return@withContext legalMoves.random()
        }

        // Medium AI: 10% chance of picking from top 3 moves
        var bestMove: Move? = null
        var bestValue = if (color == PieceColor.WHITE) Int.MIN_VALUE else Int.MAX_VALUE
        val scoredMoves = mutableListOf<Pair<Move, Int>>()

        for (move in legalMoves) {
            val nextGame = game.copy()
            nextGame.makeMove(move)
            
            val value = minimax(nextGame, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, color.opposite())
            scoredMoves.add(Pair(move, value))

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

        if (difficulty == AiDifficulty.MEDIUM && Random.nextFloat() < 0.15f) {
            val sorted = if (color == PieceColor.WHITE) scoredMoves.sortedByDescending { it.second } else scoredMoves.sortedBy { it.second }
            val top3 = sorted.take(minOf(3, sorted.size))
            return@withContext top3.random().first
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
        for ((pos, piece) in game.board.value.pieces) {
            var value = getPieceValue(piece.type)
            
            // Positional bonus: Center control (d4, d5, e4, e5)
            if (pos.row in 3..4 && pos.col in 3..4) {
                value += 3
            }

            if (piece.color == PieceColor.WHITE) {
                score += value
            } else {
                score -= value
            }
        }
        return score
    }

    // ─── CAMPAIGN AI ─────────────────────────────────────────────────────────────
    //
    // A dedicated move engine for Campaign Mode that scales across all 10 levels.
    // Completely separate from the Normal Match AI (getBestMove).
    //
    // Design principles:
    //   • Objective-Aware: Lower levels bias away from directly blocking objectives.
    //   • Controlled Errors: Error rate creates human-like sub-optimal play.
    //   • Progressive Difficulty: Each level step meaningfully increases strength.

    suspend fun getCampaignMove(game: ChessGame, levelNumber: Int): Move? = withContext(Dispatchers.Default) {
        val color = game.currentTurn.value
        val legalMoves = getAllLegalMoves(game, color)
        if (legalMoves.isEmpty()) return@withContext null

        val (depth, errorRate) = getCampaignParams(levelNumber)

        // Score every legal move with minimax at the calibrated depth
        val scoredMoves = mutableListOf<Pair<Move, Int>>()
        for (move in legalMoves) {
            val nextGame = game.copy()
            nextGame.makeMove(move)
            val value = minimax(nextGame, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, color.opposite())
            scoredMoves.add(Pair(move, value))
        }

        // Sort moves from best to worst for the AI (assuming Black AI)
        val sorted = if (color == PieceColor.WHITE)
            scoredMoves.sortedByDescending { it.second }
        else
            scoredMoves.sortedBy { it.second }

        val bestValue = sorted.first().second

        // Controlled error injection based on level parameter
        if (Random.nextFloat() < errorRate) {
            // For levels 1-2, allow a wider range of casual moves (within 350 centipawns)
            // For levels 3-5, within 250 centipawns
            val maxDelta = if (levelNumber <= 2) 350 else if (levelNumber <= 5) 250 else 150
            val acceptableMoves = sorted.filter { (_, value) ->
                if (color == PieceColor.WHITE) value >= bestValue - maxDelta
                else value <= bestValue + maxDelta
            }

            val choices = if (acceptableMoves.isNotEmpty()) acceptableMoves else sorted.take(minOf(3, sorted.size))

            // Level-specific objective balancing:
            when (levelNumber) {
                1 -> {
                    // Level 1: Pawn Academy – avoid aggressive pawn captures to let player learn promotion!
                    val nonPawnCaptures = choices.filter { (m, _) ->
                        !(m.capturedPiece?.type == PieceType.PAWN && Random.nextFloat() < 0.7f)
                    }
                    if (nonPawnCaptures.isNotEmpty()) return@withContext nonPawnCaptures.random().first
                }
                2 -> {
                    // Level 2: Knight's Grove – avoid immediately capturing or trading Knights
                    val nonKnightCaptures = choices.filter { (m, _) ->
                        !(m.capturedPiece?.type == PieceType.KNIGHT && Random.nextFloat() < 0.6f)
                    }
                    if (nonKnightCaptures.isNotEmpty()) return@withContext nonKnightCaptures.random().first
                }
                3 -> {
                    // Level 3: Bishop's Sanctuary – leave diagonals open where possible
                    val diagonalMoves = choices.filter { (m, _) -> m.piece.type != PieceType.PAWN }
                    if (diagonalMoves.isNotEmpty() && Random.nextFloat() < 0.5f) {
                        return@withContext diagonalMoves.random().first
                    }
                }
            }

            return@withContext choices.random().first
        }

        // Play best move
        return@withContext sorted.first().first
    }

    private fun getCampaignParams(levelNumber: Int): Pair<Int, Float> = when (levelNumber) {
        1  -> Pair(1, 0.65f)  // Pawn Academy       – depth 1, 65% forgiving
        2  -> Pair(1, 0.55f)  // Knight's Grove      – depth 1, 55% forgiving
        3  -> Pair(1, 0.45f)  // Bishop's Sanctuary  – depth 1, 45% forgiving
        4  -> Pair(2, 0.35f)  // Rook's Citadel      – depth 2, 35% error
        5  -> Pair(2, 0.25f)  // Queen's Court        – depth 2, 25% error
        6  -> Pair(2, 0.18f)  // King's Guard         – depth 2, 18% error
        7  -> Pair(3, 0.12f)  // Cyberpunk Trial      – depth 3, 12% error
        8  -> Pair(3, 0.08f)  // Tactical Master      – depth 3,  8% error
        9  -> Pair(3, 0.03f)  // Grandmaster's Lair   – depth 3,  3% error
        10 -> Pair(3, 0.00f)  // The Chess Tickler    – depth 3,  0% error (boss)
        else -> Pair(2, 0.20f)
    }

    private fun getPieceValue(type: PieceType): Int {
        return when (type) {
            PieceType.PAWN   -> 10
            PieceType.KNIGHT -> 30
            PieceType.BISHOP -> 30
            PieceType.ROOK   -> 50
            PieceType.QUEEN  -> 90
            PieceType.KING   -> 900
        }
    }
}
