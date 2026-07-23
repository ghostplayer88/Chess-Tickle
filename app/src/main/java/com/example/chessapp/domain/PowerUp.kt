package com.example.chessapp.domain

enum class PowerUpType(val displayName: String, val icon: String, val description: String) {
    DIVINE_SHIELD("Divine Shield", "🛡️", "Protects a piece from capture for 1 turn"),
    QUANTUM_LEAP("Quantum Leap", "🌀", "Allows any piece to make a Knight jump"),
    BOMB_PAWN("Bomb Pawn", "💣", "Explodes when captured, destroying the attacker"),
    DOUBLE_STEP("Double Step", "⏩", "Grants an extra consecutive move in 1 turn"),
    FROST_FREEZE("Frost Freeze", "❄️", "Freezes an enemy piece so it cannot move for 2 turns")
}

data class ActivePowerUpEffect(
    val type: PowerUpType,
    val targetPosition: Position,
    val ownerColor: PieceColor,
    var turnsRemaining: Int = 1
)

data class PowerUpInventory(
    val available: MutableList<PowerUpType> = mutableListOf(
        PowerUpType.DIVINE_SHIELD,
        PowerUpType.QUANTUM_LEAP,
        PowerUpType.BOMB_PAWN
    )
)
