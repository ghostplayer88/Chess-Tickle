package com.example.chessapp.domain

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists the full game state to SharedPreferences so the app can resume
 * after being closed. Online games are excluded (they live in Firebase).
 */
object GameSaveManager {

    private const val PREFS_NAME = "chess_save_state"
    private const val KEY_SAVED = "has_saved_game"
    private const val KEY_BOARD = "board"
    private const val KEY_TURN = "turn"
    private const val KEY_MODE = "game_mode"
    private const val KEY_DIFFICULTY = "ai_difficulty"
    private const val KEY_MOVE_COUNT = "move_count"
    private const val KEY_EFFECTS = "active_effects"
    private const val KEY_WHITE_INV = "white_inventory"
    private const val KEY_BLACK_INV = "black_inventory"
    private const val KEY_CAMPAIGN_LEVEL = "campaign_level"
    private const val KEY_EXTRA_TURN = "extra_turn_active"

    fun hasSavedGame(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_SAVED, false)

    fun saveGame(
        context: Context,
        game: ChessGame,
        gameMode: String,
        aiDifficulty: String,
        moveCount: Int,
        campaignLevelNumber: Int?
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val boardJson = JSONArray()
        game.board.value.pieces.forEach { (pos, piece) ->
            boardJson.put(JSONObject().apply {
                put("row", pos.row); put("col", pos.col)
                put("type", piece.type.name); put("color", piece.color.name)
                put("hasMoved", piece.hasMoved)
            })
        }

        val effectsJson = JSONArray()
        game.activeEffects.forEach { effect ->
            effectsJson.put(JSONObject().apply {
                put("type", effect.type.name)
                put("row", effect.targetPosition.row); put("col", effect.targetPosition.col)
                put("ownerColor", effect.ownerColor.name); put("turns", effect.turnsRemaining)
            })
        }

        val whiteInvJson = JSONArray().apply { game.whiteInventory.available.forEach { put(it.name) } }
        val blackInvJson = JSONArray().apply { game.blackInventory.available.forEach { put(it.name) } }

        editor.putBoolean(KEY_SAVED, true)
        editor.putString(KEY_BOARD, boardJson.toString())
        editor.putString(KEY_TURN, game.currentTurn.value.name)
        editor.putString(KEY_MODE, gameMode)
        editor.putString(KEY_DIFFICULTY, aiDifficulty)
        editor.putInt(KEY_MOVE_COUNT, moveCount)
        editor.putString(KEY_EFFECTS, effectsJson.toString())
        editor.putString(KEY_WHITE_INV, whiteInvJson.toString())
        editor.putString(KEY_BLACK_INV, blackInvJson.toString())
        editor.putBoolean(KEY_EXTRA_TURN, game.isExtraTurnActive)
        if (campaignLevelNumber != null) editor.putInt(KEY_CAMPAIGN_LEVEL, campaignLevelNumber)
        else editor.remove(KEY_CAMPAIGN_LEVEL)
        editor.apply()
    }

    fun loadGame(context: Context): SavedGameState? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_SAVED, false)) return null
        return try {
            val boardJson = JSONArray(prefs.getString(KEY_BOARD, "[]")!!)
            val pieces = mutableMapOf<Position, Piece>()
            for (i in 0 until boardJson.length()) {
                val obj = boardJson.getJSONObject(i)
                val pos = Position(obj.getInt("row"), obj.getInt("col"))
                val piece = Piece(
                    type = PieceType.valueOf(obj.getString("type")),
                    color = PieceColor.valueOf(obj.getString("color")),
                    hasMoved = obj.getBoolean("hasMoved")
                )
                pieces[pos] = piece
            }

            val effectsJson = JSONArray(prefs.getString(KEY_EFFECTS, "[]")!!)
            val effects = mutableListOf<ActivePowerUpEffect>()
            for (i in 0 until effectsJson.length()) {
                val obj = effectsJson.getJSONObject(i)
                effects.add(ActivePowerUpEffect(
                    type = PowerUpType.valueOf(obj.getString("type")),
                    targetPosition = Position(obj.getInt("row"), obj.getInt("col")),
                    ownerColor = PieceColor.valueOf(obj.getString("ownerColor")),
                    turnsRemaining = obj.getInt("turns")
                ))
            }

            val whiteInvJson = JSONArray(prefs.getString(KEY_WHITE_INV, "[]")!!)
            val whiteInv = mutableListOf<PowerUpType>().apply {
                for (i in 0 until whiteInvJson.length()) add(PowerUpType.valueOf(whiteInvJson.getString(i)))
            }
            val blackInvJson = JSONArray(prefs.getString(KEY_BLACK_INV, "[]")!!)
            val blackInv = mutableListOf<PowerUpType>().apply {
                for (i in 0 until blackInvJson.length()) add(PowerUpType.valueOf(blackInvJson.getString(i)))
            }

            SavedGameState(
                board = ChessBoard(pieces),
                currentTurn = PieceColor.valueOf(prefs.getString(KEY_TURN, "WHITE")!!),
                gameMode = prefs.getString(KEY_MODE, "PVP")!!,
                aiDifficulty = prefs.getString(KEY_DIFFICULTY, "MEDIUM")!!,
                moveCount = prefs.getInt(KEY_MOVE_COUNT, 0),
                activeEffects = effects,
                whiteInventory = whiteInv,
                blackInventory = blackInv,
                isExtraTurnActive = prefs.getBoolean(KEY_EXTRA_TURN, false),
                campaignLevelNumber = if (prefs.contains(KEY_CAMPAIGN_LEVEL)) prefs.getInt(KEY_CAMPAIGN_LEVEL, -1) else null
            )
        } catch (e: Exception) {
            null
        }
    }

    fun clearSave(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}

data class SavedGameState(
    val board: ChessBoard,
    val currentTurn: PieceColor,
    val gameMode: String,
    val aiDifficulty: String,
    val moveCount: Int,
    val activeEffects: List<ActivePowerUpEffect>,
    val whiteInventory: List<PowerUpType>,
    val blackInventory: List<PowerUpType>,
    val isExtraTurnActive: Boolean,
    val campaignLevelNumber: Int?
)
