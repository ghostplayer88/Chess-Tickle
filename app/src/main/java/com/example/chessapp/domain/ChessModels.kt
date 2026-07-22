package com.example.chessapp.domain

enum class PieceType {
    PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
}

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

data class Piece(val type: PieceType, val color: PieceColor, val hasMoved: Boolean = false)

data class Position(val row: Int, val col: Int) {
    fun isValid() = row in 0..7 && col in 0..7
}

data class Move(
    val from: Position,
    val to: Position,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val promotion: PieceType? = null,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false
)

sealed class GameStatus {
    object Active : GameStatus()
    data class Checkmate(val winner: PieceColor) : GameStatus()
    object Stalemate : GameStatus()
}
