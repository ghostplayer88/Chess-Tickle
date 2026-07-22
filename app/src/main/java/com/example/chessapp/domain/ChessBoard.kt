package com.example.chessapp.domain

class ChessBoard(val pieces: Map<Position, Piece>) {

    fun getPiece(position: Position): Piece? = pieces[position]

    fun isOccupied(position: Position): Boolean = pieces.containsKey(position)

    fun isOccupiedByColor(position: Position, color: PieceColor): Boolean =
        pieces[position]?.color == color

    // Creates a new board by applying a move
    fun applyMove(move: Move): ChessBoard {
        val newPieces = pieces.toMutableMap()
        newPieces.remove(move.from)
        
        if (move.isEnPassant) {
            val captureRow = move.from.row
            val captureCol = move.to.col
            newPieces.remove(Position(captureRow, captureCol))
        }
        
        if (move.isCastling) {
            // Move the rook as well
            val row = move.from.row
            if (move.to.col > move.from.col) {
                // Kingside castling
                val rook = newPieces.remove(Position(row, 7))!!
                newPieces[Position(row, 5)] = rook.copy(hasMoved = true)
            } else {
                // Queenside castling
                val rook = newPieces.remove(Position(row, 0))!!
                newPieces[Position(row, 3)] = rook.copy(hasMoved = true)
            }
        }
        
        val movedPiece = move.promotion?.let { 
            Piece(it, move.piece.color, true) 
        } ?: move.piece.copy(hasMoved = true)
        
        newPieces[move.to] = movedPiece
        
        return ChessBoard(newPieces)
    }

    companion object {
        fun initial(): ChessBoard {
            val pieces = mutableMapOf<Position, Piece>()
            
            // Pawns
            for (col in 0..7) {
                pieces[Position(1, col)] = Piece(PieceType.PAWN, PieceColor.BLACK)
                pieces[Position(6, col)] = Piece(PieceType.PAWN, PieceColor.WHITE)
            }
            
            val order = listOf(
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
            )
            
            for (col in 0..7) {
                pieces[Position(0, col)] = Piece(order[col], PieceColor.BLACK)
                pieces[Position(7, col)] = Piece(order[col], PieceColor.WHITE)
            }
            
            return ChessBoard(pieces)
        }
    }
}
