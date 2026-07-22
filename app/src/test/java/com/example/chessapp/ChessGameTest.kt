package com.example.chessapp

import com.example.chessapp.domain.*
import org.junit.Test
import org.junit.Assert.*

class ChessGameTest {
    @Test
    fun testInitialBoard() {
        val game = ChessGame()
        val board = game.board.value
        
        // Check a white pawn
        val whitePawn = board.getPiece(Position(6, 0))
        assertNotNull(whitePawn)
        assertEquals(PieceType.PAWN, whitePawn!!.type)
        assertEquals(PieceColor.WHITE, whitePawn.color)
        
        // Check black king
        val blackKing = board.getPiece(Position(0, 4))
        assertNotNull(blackKing)
        assertEquals(PieceType.KING, blackKing!!.type)
        assertEquals(PieceColor.BLACK, blackKing.color)
        
        // White moves first
        assertEquals(PieceColor.WHITE, game.currentTurn.value)
    }

    @Test
    fun testPawnDoubleMove() {
        val game = ChessGame()
        
        // White pawn moves double
        val moves = game.getLegalMoves(Position(6, 0))
        val doubleMove = moves.find { it.to == Position(4, 0) }
        assertNotNull("Pawn should be able to move double on first turn", doubleMove)
        
        assertTrue(game.makeMove(doubleMove!!))
        assertEquals(PieceColor.BLACK, game.currentTurn.value)
        assertNotNull(game.board.value.getPiece(Position(4, 0)))
        assertNull(game.board.value.getPiece(Position(6, 0)))
    }

    @Test
    fun testKnightMove() {
        val game = ChessGame()
        val moves = game.getLegalMoves(Position(7, 1)) // White Knight on b1
        val move = moves.find { it.to == Position(5, 2) } // to c3
        assertNotNull("Knight should be able to jump over pieces", move)
        assertTrue(game.makeMove(move!!))
    }
}
