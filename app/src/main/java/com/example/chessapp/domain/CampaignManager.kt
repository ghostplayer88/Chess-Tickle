package com.example.chessapp.domain

import android.content.Context
import android.content.SharedPreferences
import com.example.chessapp.ui.AiDifficulty
import com.example.chessapp.ui.BoardTheme

data class CampaignLevel(
    val levelNumber: Int,
    val title: String,
    val description: String,
    val difficulty: AiDifficulty,
    val theme: BoardTheme,
    var starsEarned: Int = 0,
    var isUnlocked: Boolean = false
)

class CampaignManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("chess_campaign_prefs", Context.MODE_PRIVATE)

    private val _levels = mutableListOf(
        CampaignLevel(1, "🐣 Pawn Academy", "Basic match against Easy AI.", AiDifficulty.EASY, BoardTheme.WOOD),
        CampaignLevel(2, "🐴 Knight's Grove", "Easy AI with aggressive knight maneuvers.", AiDifficulty.EASY, BoardTheme.WOOD),
        CampaignLevel(3, "♗ Bishop's Sanctuary", "Medium AI focusing on long diagonal lines.", AiDifficulty.MEDIUM, BoardTheme.EMERALD),
        CampaignLevel(4, "🏰 Rook's Citadel", "Medium AI heavy piece endgame focus.", AiDifficulty.MEDIUM, BoardTheme.EMERALD),
        CampaignLevel(5, "👸 Queen's Court", "Medium AI with high-pressure tactical offense.", AiDifficulty.MEDIUM, BoardTheme.WOOD),
        CampaignLevel(6, "🛡️ King's Guard", "Hard AI playing a tight defensive structure.", AiDifficulty.HARD, BoardTheme.EMERALD),
        CampaignLevel(7, "🌌 Cyberpunk Trial", "Hard AI match on the Cyberpunk Neon board.", AiDifficulty.HARD, BoardTheme.NEON),
        CampaignLevel(8, "🧩 Tactical Master", "Hard AI testing your tactical discipline.", AiDifficulty.HARD, BoardTheme.NEON),
        CampaignLevel(9, "⚔️ Grandmaster's Lair", "Hard AI searching deep into future outcomes.", AiDifficulty.HARD, BoardTheme.EMERALD),
        CampaignLevel(10, "👑 The Chess Tickler", "Ultimate Boss Fight! Win to claim the Crown!", AiDifficulty.HARD, BoardTheme.NEON)
    )

    val levels: List<CampaignLevel> get() = _levels

    init {
        loadCampaignProgress()
    }

    private fun loadCampaignProgress() {
        for (lvl in _levels) {
            if (lvl.levelNumber == 1) {
                lvl.isUnlocked = true // Level 1 always unlocked
            } else {
                lvl.isUnlocked = prefs.getBoolean("campaign_lvl_${lvl.levelNumber}_unlocked", false)
            }
            lvl.starsEarned = prefs.getInt("campaign_lvl_${lvl.levelNumber}_stars", 0)
        }
    }

    fun completeLevel(levelNumber: Int, turnsTaken: Int, lostQueen: Boolean): Pair<Int, Boolean> {
        val lvl = _levels.find { it.levelNumber == levelNumber } ?: return Pair(0, false)

        var stars = 1 // 1 star for winning
        if (turnsTaken <= 30) stars++ // 2nd star for speed
        if (!lostQueen) stars++ // 3rd star for clean win

        val isNewRecord = stars > lvl.starsEarned
        if (isNewRecord) {
            lvl.starsEarned = stars
            prefs.edit().putInt("campaign_lvl_${lvl.levelNumber}_stars", stars).apply()
        }

        // Unlock next level
        val nextLevelNumber = levelNumber + 1
        val nextLvl = _levels.find { it.levelNumber == nextLevelNumber }
        if (nextLvl != null && !nextLvl.isUnlocked) {
            nextLvl.isUnlocked = true
            prefs.edit().putBoolean("campaign_lvl_${nextLevelNumber}_unlocked", true).apply()
        }

        return Pair(stars, isNewRecord)
    }

    fun getTotalStars(): Int = _levels.sumOf { it.starsEarned }
    fun isCampaignComplete(): Boolean = _levels.all { it.starsEarned > 0 }
}
