package com.example.chessapp.domain

import android.content.Context
import android.content.SharedPreferences
import com.example.chessapp.ui.AiDifficulty
import com.example.chessapp.ui.BoardTheme

data class CampaignLevel(
    val levelNumber: Int,
    val title: String,
    val description: String,
    val lessonTip: String,
    val difficulty: AiDifficulty,
    val theme: BoardTheme,
    val goal1: String = "⭐ Win the match",
    val goal2: String = "⭐⭐ Win in 30 turns or fewer",
    val goal3: String = "⭐⭐⭐ Win without losing your Queen",
    var starsEarned: Int = 0,
    var isUnlocked: Boolean = false
)

class CampaignManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("chess_campaign_prefs", Context.MODE_PRIVATE)

    private val _levels = mutableListOf(
        CampaignLevel(
            levelNumber = 1,
            title = "🐣 Pawn Academy",
            description = "Master pawn advancement, pawn structure, and promotion.",
            lessonTip = "💡 LESSON: Pawns are the backbone of chess! Control the center and push pawns to the 8th row for promotion.",
            difficulty = AiDifficulty.EASY,
            theme = BoardTheme.WOOD,
            goal1 = "⭐ Defeat Pawn Academy",
            goal2 = "⭐⭐ Promote a Pawn into a Queen",
            goal3 = "⭐⭐⭐ Win in 30 turns or fewer"
        ),
        CampaignLevel(
            levelNumber = 2,
            title = "🐴 Knight's Grove",
            description = "Master L-shaped knight jumps, central outposts, and forks.",
            lessonTip = "💡 LESSON: Knights excel in closed positions! Jump over obstacles and fork two enemy pieces at once.",
            difficulty = AiDifficulty.EASY,
            theme = BoardTheme.WOOD,
            goal1 = "⭐ Defeat Knight's Grove",
            goal2 = "⭐⭐ Capture 2 enemy pieces with Knights",
            goal3 = "⭐⭐⭐ Win without losing any Knight"
        ),
        CampaignLevel(
            levelNumber = 3,
            title = "♗ Bishop's Sanctuary",
            description = "Master long-range diagonal attacks and bishop pairs.",
            lessonTip = "💡 LESSON: Bishops thrive on long open diagonals! Keep your diagonal paths clear for snipers.",
            difficulty = AiDifficulty.MEDIUM,
            theme = BoardTheme.EMERALD,
            goal1 = "⭐ Defeat Bishop's Sanctuary",
            goal2 = "⭐⭐ Capture a piece from 4+ squares away",
            goal3 = "⭐⭐⭐ Win in 30 turns or fewer"
        ),
        CampaignLevel(
            levelNumber = 4,
            title = "🏰 Rook's Citadel",
            description = "Master open files, heavy piece control, and castling safety.",
            lessonTip = "💡 LESSON: Rooks belong on open vertical files! Castle early to connect your Rooks for heavy pressure.",
            difficulty = AiDifficulty.MEDIUM,
            theme = BoardTheme.EMERALD,
            goal1 = "⭐ Defeat Rook's Citadel",
            goal2 = "⭐⭐ Perform Castling (Kingside or Queenside)",
            goal3 = "⭐⭐⭐ Win without losing a Rook"
        ),
        CampaignLevel(
            levelNumber = 5,
            title = "👸 Queen's Court",
            description = "Master powerful queen offense while protecting your piece.",
            lessonTip = "💡 LESSON: The Queen is your strongest piece! Launch fierce attacks while guarding her against traps.",
            difficulty = AiDifficulty.MEDIUM,
            theme = BoardTheme.WOOD,
            goal1 = "⭐ Defeat Queen's Court",
            goal2 = "⭐⭐ Win without losing your Queen",
            goal3 = "⭐⭐⭐ Win in 25 turns or fewer"
        ),
        CampaignLevel(
            levelNumber = 6,
            title = "🛡️ King's Guard",
            description = "Master king safety, pawn shields, and escaping check.",
            lessonTip = "💡 LESSON: Protect your King! Keep a pawn shield in front of your King and avoid exposed King walks.",
            difficulty = AiDifficulty.HARD,
            theme = BoardTheme.EMERALD,
            goal1 = "⭐ Defeat King's Guard",
            goal2 = "⭐⭐ Deliver Checkmate with your King safe",
            goal3 = "⭐⭐⭐ Win losing fewer than 4 pieces"
        ),
        CampaignLevel(
            levelNumber = 7,
            title = "🌌 Cyberpunk Trial",
            description = "Master tactical calculation on neon battlegrounds.",
            lessonTip = "💡 LESSON: Calculate multiple turns ahead! Use tactical vision to spot weaknesses in enemy defenses.",
            difficulty = AiDifficulty.HARD,
            theme = BoardTheme.NEON,
            goal1 = "⭐ Defeat Cyberpunk Trial",
            goal2 = "⭐⭐ Use the AI Hint system for guidance",
            goal3 = "⭐⭐⭐ Win in 30 turns or fewer"
        ),
        CampaignLevel(
            levelNumber = 8,
            title = "🧩 Tactical Master",
            description = "Master pins, skewers, and tactical combinations.",
            lessonTip = "💡 LESSON: Pin enemy pieces to higher value targets so they cannot move without massive losses!",
            difficulty = AiDifficulty.HARD,
            theme = BoardTheme.NEON,
            goal1 = "⭐ Defeat Tactical Master",
            goal2 = "⭐⭐ Promote a Pawn during the match",
            goal3 = "⭐⭐⭐ Win without using Undo"
        ),
        CampaignLevel(
            levelNumber = 9,
            title = "⚔️ Grandmaster's Lair",
            description = "Master endgame technique against deep AI calculation.",
            lessonTip = "💡 LESSON: Convert minor material advantages into guaranteed endgame victories with precision.",
            difficulty = AiDifficulty.HARD,
            theme = BoardTheme.EMERALD,
            goal1 = "⭐ Defeat Grandmaster's Lair",
            goal2 = "⭐⭐ Defeat Hard AI Engine",
            goal3 = "⭐⭐⭐ Win without losing your Queen"
        ),
        CampaignLevel(
            levelNumber = 10,
            title = "👑 The Chess Tickler",
            description = "The Ultimate Boss Fight! Combine all chess lessons!",
            lessonTip = "💡 LESSON: Combine Pawns, Knights, Bishops, Rooks, and Queen into one unstoppable winning army!",
            difficulty = AiDifficulty.HARD,
            theme = BoardTheme.NEON,
            goal1 = "⭐ Defeat The Chess Tickler Boss",
            goal2 = "⭐⭐ Win in 35 turns or fewer",
            goal3 = "⭐⭐⭐ Earn 3 Stars to claim the Crown!"
        )
    )

    val levels: List<CampaignLevel> get() = _levels

    init {
        loadCampaignProgress()
    }

    private fun loadCampaignProgress() {
        for (lvl in _levels) {
            if (lvl.levelNumber == 1) {
                lvl.isUnlocked = true
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
