package com.example.chessapp.domain

data class OnlineMoveDto(
    val fromRow: Int = 0,
    val fromCol: Int = 0,
    val toRow: Int = 0,
    val toCol: Int = 0,
    val pieceType: String = "",
    val pieceColor: String = "",
    val promotionType: String? = null
) {
    fun toMove(board: ChessBoard): Move? {
        val fromPos = Position(fromRow, fromCol)
        val toPos = Position(toRow, toCol)
        val piece = board.getPiece(fromPos) ?: return null
        val capturedPiece = board.getPiece(toPos)
        val promo = promotionType?.let { PieceType.valueOf(it) }

        return Move(
            from = fromPos,
            to = toPos,
            piece = piece,
            capturedPiece = capturedPiece,
            promotion = promo
        )
    }

    companion object {
        fun fromMove(move: Move): OnlineMoveDto {
            return OnlineMoveDto(
                fromRow = move.from.row,
                fromCol = move.from.col,
                toRow = move.to.row,
                toCol = move.to.col,
                pieceType = move.piece.type.name,
                pieceColor = move.piece.color.name,
                promotionType = move.promotion?.name
            )
        }
    }
}
