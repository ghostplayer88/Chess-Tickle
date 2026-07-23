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

    private var halfMoveClock = 0
    private val positionHistory = mutableMapOf<String, Int>()

    val activeEffects = mutableListOf<ActivePowerUpEffect>()
    val whiteInventory = PowerUpInventory()
    val blackInventory = PowerUpInventory()
    var isExtraTurnActive = false

    fun copy(): ChessGame {
        val newGame = ChessGame()
        newGame._board.value = this.board.value
        newGame._currentTurn.value = this.currentTurn.value
        newGame._status.value = this.status.value
        newGame.moveHistory.addAll(this.moveHistory)
        newGame.halfMoveClock = this.halfMoveClock
        newGame.positionHistory.putAll(this.positionHistory)
        return newGame
    }

    fun makeMove(move: Move): Boolean {
        if (_status.value !is GameStatus.Active) return false
        if (move.piece.color != _currentTurn.value) return false
        
        val legalMoves = getLegalMoves(move.from)
        val actualMove = legalMoves.find { it.to == move.to && it.promotion == move.promotion } ?: return false

        historyStack.add(GameSnapshot(_board.value, _currentTurn.value, _status.value, ArrayList(moveHistory)))

        // Handle Divine Shield capture prevention
        if (actualMove.capturedPiece != null) {
            val isShielded = activeEffects.any { it.type == PowerUpType.DIVINE_SHIELD && it.targetPosition == actualMove.to }
            if (isShielded) {
                // Cannot capture a shielded piece
                return false
            }
        }

        // Handle Frost Freeze check
        val isFrozen = activeEffects.any { it.type == PowerUpType.FROST_FREEZE && it.targetPosition == actualMove.from }
        if (isFrozen) {
            return false
        }

        _board.value = _board.value.applyMove(actualMove)

        // Handle Bomb Pawn explosion: if captured piece was a Bomb Pawn, destroy the attacker too!
        if (actualMove.capturedPiece != null) {
            val isBombPawn = activeEffects.any { it.type == PowerUpType.BOMB_PAWN && it.targetPosition == actualMove.to }
            if (isBombPawn) {
                _board.value = _board.value.removePiece(actualMove.to)
            }
        }

        moveHistory.add(actualMove)

        // Decrement effect turns
        activeEffects.forEach { it.turnsRemaining-- }
        activeEffects.removeAll { it.turnsRemaining <= 0 }

        // Double Step: don't flip turn if double step is active
        if (isExtraTurnActive) {
            isExtraTurnActive = false
        } else {
            _currentTurn.value = _currentTurn.value.opposite()
        }

        // Track Threefold Repetition
        val posKey = getPositionKey()
        positionHistory[posKey] = (positionHistory[posKey] ?: 0) + 1

        updateGameStatus()
        return true
    }

    fun activatePowerUp(type: PowerUpType, targetPos: Position, color: PieceColor): Boolean {
        if (_currentTurn.value != color) return false
        val inventory = if (color == PieceColor.WHITE) whiteInventory else blackInventory
        if (!inventory.available.contains(type)) return false

        when (type) {
            PowerUpType.DIVINE_SHIELD -> {
                val piece = _board.value.getPiece(targetPos) ?: return false
                if (piece.color != color) return false
                activeEffects.add(ActivePowerUpEffect(type, targetPos, color, turnsRemaining = 2))
            }
            PowerUpType.QUANTUM_LEAP -> {
                val piece = _board.value.getPiece(targetPos) ?: return false
                if (piece.color != color) return false
                activeEffects.add(ActivePowerUpEffect(type, targetPos, color, turnsRemaining = 1))
            }
            PowerUpType.BOMB_PAWN -> {
                val piece = _board.value.getPiece(targetPos) ?: return false
                if (piece.type != PieceType.PAWN || piece.color != color) return false
                activeEffects.add(ActivePowerUpEffect(type, targetPos, color, turnsRemaining = 10))
            }
            PowerUpType.DOUBLE_STEP -> {
                isExtraTurnActive = true
            }
            PowerUpType.FROST_FREEZE -> {
                val piece = _board.value.getPiece(targetPos) ?: return false
                if (piece.color == color) return false // target enemy piece
                activeEffects.add(ActivePowerUpEffect(type, targetPos, color, turnsRemaining = 2))
            }
        }

        inventory.available.remove(type)
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

    private fun getPositionKey(): String {
        val piecesStr = _board.value.pieces.entries.sortedBy { it.key.row * 8 + it.key.col }
            .joinToString(";") { "${it.key.row},${it.key.col}:${it.value.color}${it.value.type}" }
        return "$piecesStr|_currentTurn:${_currentTurn.value}"
    }

    private fun isInsufficientMaterial(): Boolean {
        val pieces = _board.value.pieces.values.toList()
        if (pieces.size == 2) return true // King vs King
        
        if (pieces.size == 3) {
            val nonKings = pieces.filter { it.type != PieceType.KING }
            if (nonKings.size == 1 && (nonKings[0].type == PieceType.KNIGHT || nonKings[0].type == PieceType.BISHOP)) {
                return true // King + Knight vs King OR King + Bishop vs King
            }
        }

        if (pieces.size == 4) {
            val whitePieces = pieces.filter { it.color == PieceColor.WHITE && it.type != PieceType.KING }
            val blackPieces = pieces.filter { it.color == PieceColor.BLACK && it.type != PieceType.KING }
            if (whitePieces.size == 1 && blackPieces.size == 1 &&
                whitePieces[0].type == PieceType.BISHOP && blackPieces[0].type == PieceType.BISHOP) {
                val whiteBishopPos = _board.value.pieces.entries.find { it.value == whitePieces[0] }?.key
                val blackBishopPos = _board.value.pieces.entries.find { it.value == blackPieces[0] }?.key
                if (whiteBishopPos != null && blackBishopPos != null) {
                    val whiteSquareColor = (whiteBishopPos.row + whiteBishopPos.col) % 2
                    val blackSquareColor = (blackBishopPos.row + blackBishopPos.col) % 2
                    if (whiteSquareColor == blackSquareColor) return true // Same color bishops
                }
            }
        }

        return false
    }

    private fun updateGameStatus() {
        // 0. Check if any King has been captured/destroyed
        val whiteHasKing = _board.value.pieces.values.any { it.type == PieceType.KING && it.color == PieceColor.WHITE }
        val blackHasKing = _board.value.pieces.values.any { it.type == PieceType.KING && it.color == PieceColor.BLACK }

        if (!whiteHasKing && !blackHasKing) {
            _status.value = GameStatus.Stalemate
            return
        }
        if (!whiteHasKing) {
            _status.value = GameStatus.Checkmate(PieceColor.BLACK)
            return
        }
        if (!blackHasKing) {
            _status.value = GameStatus.Checkmate(PieceColor.WHITE)
            return
        }

        // 1. FIDE Insufficient material check
        if (isInsufficientMaterial()) {
            _status.value = GameStatus.Stalemate
            return
        }

        // 2. FIDE 50-Move Rule (100 half-moves)
        if (halfMoveClock >= 100) {
            _status.value = GameStatus.Stalemate
            return
        }

        // 3. FIDE Threefold Repetition Rule
        val currentPosKey = getPositionKey()
        if ((positionHistory[currentPosKey] ?: 0) >= 3) {
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
