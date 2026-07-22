package com.example.chessapp.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

class ChessGame {
    private val _board = MutableStateFlow(ChessBoard.initial())
    val board: StateFlow<ChessBoard> = _board.asStateFlow()

    private val _currentTurn = MutableStateFlow(PieceColor.WHITE)
    val currentTurn: StateFlow<PieceColor> = _currentTurn.asStateFlow()

    private val _status = MutableStateFlow<GameStatus>(GameStatus.Active)
    val status: StateFlow<GameStatus> = _status.asStateFlow()

    private data class GameSnapshot(
        val board: ChessBoard,
        val currentTurn: PieceColor,
        val status: GameStatus,
        val moveHistory: List<Move>
    )

    private val historyStack = mutableListOf<GameSnapshot>()
    private val moveHistory = mutableListOf<Move>()
    val moves: List<Move> get() = moveHistory.toList()

    fun copy(): ChessGame {
        val newGame = ChessGame()
        newGame._board.value = this.board.value
        newGame._currentTurn.value = this.currentTurn.value
        newGame._status.value = this.status.value
        newGame.moveHistory.addAll(this.moveHistory)
        return newGame
    }

    fun makeMove(move: Move): Boolean {
        if (_status.value !is GameStatus.Active) return false
        if (move.piece.color != _currentTurn.value) return false
        
        val legalMoves = getLegalMoves(move.from)
        val actualMove = legalMoves.find { it.to == move.to && it.promotion == move.promotion } ?: return false

        historyStack.add(GameSnapshot(_board.value, _currentTurn.value, _status.value, ArrayList(moveHistory)))

        _board.value = _board.value.applyMove(actualMove)
        moveHistory.add(actualMove)
        _currentTurn.value = _currentTurn.value.opposite()

        updateGameStatus()
        return true
    }

    fun undoLastMove(): Boolean {
        if (historyStack.isEmpty()) return false
        val lastState = historyStack.removeAt(historyStack.size - 1)
        _board.value = lastState.board
        _currentTurn.value = lastState.currentTurn
        _status.value = lastState.status
        moveHistory.clear()
        moveHistory.addAll(lastState.moveHistory)
        return true
    }

    fun getLegalMoves(position: Position): List<Move> {
        val piece = _board.value.getPiece(position) ?: return emptyList()
        if (piece.color != _currentTurn.value) return emptyList()
        if (_status.value !is GameStatus.Active) return emptyList()

        val pseudoLegalMoves = generatePseudoLegalMoves(_board.value, position)
        
        // Filter out moves that leave the king in check
        val legalMoves = pseudoLegalMoves.filter { move ->
            val nextBoard = _board.value.applyMove(move)
            !isKingInCheck(nextBoard, piece.color)
        }

        return legalMoves
    }

    private fun generatePseudoLegalMoves(board: ChessBoard, pos: Position, checkCastling: Boolean = true): List<Move> {
        val piece = board.getPiece(pos) ?: return emptyList()
        val moves = mutableListOf<Move>()

        when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (piece.color == PieceColor.WHITE) -1 else 1
                val startRow = if (piece.color == PieceColor.WHITE) 6 else 1

                // Move forward 1
                val forward1 = Position(pos.row + dir, pos.col)
                if (forward1.isValid() && !board.isOccupied(forward1)) {
                    addPawnMove(moves, pos, forward1, piece, board)
                    
                    // Move forward 2
                    val forward2 = Position(pos.row + dir * 2, pos.col)
                    if (pos.row == startRow && !board.isOccupied(forward2)) {
                        moves.add(Move(pos, forward2, piece))
                    }
                }

                // Captures
                val captures = listOf(Position(pos.row + dir, pos.col - 1), Position(pos.row + dir, pos.col + 1))
                for (cap in captures) {
                    if (cap.isValid()) {
                        if (board.isOccupiedByColor(cap, piece.color.opposite())) {
                            addPawnMove(moves, pos, cap, piece, board)
                        } else if (isEnPassant(pos, cap, piece.color)) {
                            moves.add(Move(pos, cap, piece, isEnPassant = true))
                        }
                    }
                }
            }
            PieceType.KNIGHT -> {
                val knightMoves = listOf(
                    Position(pos.row - 2, pos.col - 1), Position(pos.row - 2, pos.col + 1),
                    Position(pos.row - 1, pos.col - 2), Position(pos.row - 1, pos.col + 2),
                    Position(pos.row + 1, pos.col - 2), Position(pos.row + 1, pos.col + 2),
                    Position(pos.row + 2, pos.col - 1), Position(pos.row + 2, pos.col + 1)
                )
                for (m in knightMoves) {
                    if (m.isValid() && !board.isOccupiedByColor(m, piece.color)) {
                        moves.add(Move(pos, m, piece, capturedPiece = board.getPiece(m)))
                    }
                }
            }
            PieceType.BISHOP -> generateLineMoves(board, pos, piece, moves, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1))
            PieceType.ROOK -> generateLineMoves(board, pos, piece, moves, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1))
            PieceType.QUEEN -> generateLineMoves(board, pos, piece, moves, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1, -1 to 0, 1 to 0, 0 to -1, 0 to 1))
            PieceType.KING -> {
                val kingMoves = listOf(
                    Position(pos.row - 1, pos.col - 1), Position(pos.row - 1, pos.col), Position(pos.row - 1, pos.col + 1),
                    Position(pos.row, pos.col - 1), Position(pos.row, pos.col + 1),
                    Position(pos.row + 1, pos.col - 1), Position(pos.row + 1, pos.col), Position(pos.row + 1, pos.col + 1)
                )
                for (m in kingMoves) {
                    if (m.isValid() && !board.isOccupiedByColor(m, piece.color)) {
                        moves.add(Move(pos, m, piece, capturedPiece = board.getPiece(m)))
                    }
                }
                
                // Castling
                if (checkCastling && !piece.hasMoved && !isKingInCheck(board, piece.color)) {
                    // Queenside
                    if (canCastle(board, pos, 0, piece.color)) {
                        moves.add(Move(pos, Position(pos.row, 2), piece, isCastling = true))
                    }
                    // Kingside
                    if (canCastle(board, pos, 7, piece.color)) {
                        moves.add(Move(pos, Position(pos.row, 6), piece, isCastling = true))
                    }
                }
            }
        }
        return moves
    }

    private fun generateLineMoves(board: ChessBoard, pos: Position, piece: Piece, moves: MutableList<Move>, directions: List<Pair<Int, Int>>) {
        for ((dr, dc) in directions) {
            var r = pos.row + dr
            var c = pos.col + dc
            while (r in 0..7 && c in 0..7) {
                val currentPos = Position(r, c)
                if (board.isOccupied(currentPos)) {
                    if (board.getPiece(currentPos)?.color != piece.color) {
                        moves.add(Move(pos, currentPos, piece, capturedPiece = board.getPiece(currentPos)))
                    }
                    break
                }
                moves.add(Move(pos, currentPos, piece))
                r += dr
                c += dc
            }
        }
    }

    private fun addPawnMove(moves: MutableList<Move>, from: Position, to: Position, piece: Piece, board: ChessBoard) {
        val promotionRow = if (piece.color == PieceColor.WHITE) 0 else 7
        if (to.row == promotionRow) {
            moves.add(Move(from, to, piece, capturedPiece = board.getPiece(to), promotion = PieceType.QUEEN))
            moves.add(Move(from, to, piece, capturedPiece = board.getPiece(to), promotion = PieceType.ROOK))
            moves.add(Move(from, to, piece, capturedPiece = board.getPiece(to), promotion = PieceType.BISHOP))
            moves.add(Move(from, to, piece, capturedPiece = board.getPiece(to), promotion = PieceType.KNIGHT))
        } else {
            moves.add(Move(from, to, piece, capturedPiece = board.getPiece(to)))
        }
    }

    private fun isEnPassant(from: Position, to: Position, color: PieceColor): Boolean {
        if (moveHistory.isEmpty()) return false
        val lastMove = moveHistory.last()
        val dir = if (color == PieceColor.WHITE) -1 else 1
        return lastMove.piece.type == PieceType.PAWN &&
                abs(lastMove.from.row - lastMove.to.row) == 2 &&
                lastMove.to.row == from.row &&
                lastMove.to.col == to.col &&
                to.row == from.row + dir
    }

    private fun canCastle(board: ChessBoard, kingPos: Position, rookCol: Int, color: PieceColor): Boolean {
        val rookPos = Position(kingPos.row, rookCol)
        val rook = board.getPiece(rookPos)
        if (rook == null || rook.type != PieceType.ROOK || rook.hasMoved || rook.color != color) return false

        val minCol = minOf(kingPos.col, rookCol)
        val maxCol = maxOf(kingPos.col, rookCol)

        // Check path is clear
        for (c in (minCol + 1) until maxCol) {
            if (board.isOccupied(Position(kingPos.row, c))) return false
        }

        // Check king doesn't pass through check
        val dir = if (rookCol > kingPos.col) 1 else -1
        val nextPos = Position(kingPos.row, kingPos.col + dir)
        val nextBoard = board.applyMove(Move(kingPos, nextPos, board.getPiece(kingPos)!!))
        if (isKingInCheck(nextBoard, color)) return false
        
        return true
    }

    private fun isKingInCheck(board: ChessBoard, color: PieceColor): Boolean {
        var kingPos: Position? = null
        for ((pos, piece) in board.pieces) {
            if (piece.type == PieceType.KING && piece.color == color) {
                kingPos = pos
                break
            }
        }
        if (kingPos == null) return false // Should not happen in a valid game

        for ((pos, piece) in board.pieces) {
            if (piece.color != color) {
                val moves = generatePseudoLegalMoves(board, pos, checkCastling = false)
                if (moves.any { it.to == kingPos }) {
                    return true
                }
            }
        }
        return false
    }

    private fun updateGameStatus() {
        // Insufficient material check: Bare Kings (only 2 kings remain on the board)
        if (_board.value.pieces.size <= 2) {
            _status.value = GameStatus.Stalemate
            return
        }

        var hasLegalMoves = false
        for ((pos, piece) in _board.value.pieces) {
            if (piece.color == _currentTurn.value) {
                if (generatePseudoLegalMoves(_board.value, pos).any { move ->
                        !isKingInCheck(_board.value.applyMove(move), piece.color)
                    }) {
                    hasLegalMoves = true
                    break
                }
            }
        }

        if (!hasLegalMoves) {
            if (isKingInCheck(_board.value, _currentTurn.value)) {
                _status.value = GameStatus.Checkmate(_currentTurn.value.opposite())
            } else {
                _status.value = GameStatus.Stalemate
            }
        }
    }
}
