package com.example.chessapp.domain

import android.content.Context
import android.content.SharedPreferences

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val category: String,
    val maxProgress: Int = 1,
    var currentProgress: Int = 0,
    var isUnlocked: Boolean = false
)

class AchievementManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("chess_achievements_prefs", Context.MODE_PRIVATE)

    private val _achievements = mutableListOf(
        Achievement("first_move", "First Steps", "Make your very first move.", "♟️", "Beginners"),
        Achievement("first_capture", "First Blood", "Capture an enemy piece.", "⚔️", "Combat"),
        Achievement("promote_pawn", "Pawn Power", "Promote a pawn into a higher piece.", "👑", "Special"),
        Achievement("castle", "Fortress", "Perform Castling (Kingside or Queenside).", "🏰", "Defense"),
        Achievement("en_passant", "Sneaky En Passant", "Execute an En Passant capture.", "⚡", "Special"),
        Achievement("first_check", "First Check", "Put the opponent's King in check.", "🎯", "Combat"),
        Achievement("win_checkmate", "Checkmate Master", "Win a game by Checkmate.", "🏆", "Victory"),
        Achievement("win_easy_ai", "AI Defeater", "Win a game against Easy AI.", "🌱", "PvE"),
        Achievement("win_med_ai", "Tactical Genius", "Win a game against Medium AI.", "⚡", "PvE"),
        Achievement("win_hard_ai", "Grandmaster Defeater", "Win a game against Hard AI.", "🔥", "PvE"),
        Achievement("play_pvp", "Local Champion", "Complete a 2-Player local match.", "👥", "PvP"),
        Achievement("two_queens", "Queen Collector", "Have 2 Queens on the board simultaneously.", "👑", "Mastery"),
        Achievement("use_hint", "Guided Move", "Use the Hint button during a match.", "💡", "QoL"),
        Achievement("use_undo", "Time Traveler", "Use the Undo button 5 times.", "↩️", "QoL", maxProgress = 5),
        Achievement("change_theme", "Theme Enthusiast", "Change your board theme in setup.", "🎨", "Customization"),
        Achievement("campaign_lvl1", "Campaign Rookie", "Complete Level 1 of Campaign Mode.", "🗺️", "Campaign"),
        Achievement("campaign_10stars", "Star Collector", "Earn 10 total stars in Campaign Mode.", "⭐", "Campaign", maxProgress = 10),
        Achievement("campaign_complete", "Campaign Champion", "Complete all 10 Campaign levels.", "👑", "Campaign"),
        Achievement("castle_3x", "Double Castle", "Castle in 3 separate matches.", "🏰", "Defense", maxProgress = 3),
        Achievement("knight_captures", "Knight Rider", "Capture 5 enemy pieces using Knights.", "🐴", "Combat", maxProgress = 5),
        Achievement("bishop_sniper", "Sniper Bishop", "Capture a piece from 4+ squares away.", "🏹", "Combat"),
        Achievement("rook_rampage", "Rook Rampage", "Capture 3 pieces with a single Rook.", "🏯", "Combat", maxProgress = 3),
        Achievement("queens_gambit", "Queen's Gambit", "Move your Queen out before move 5.", "👸", "Strategy"),
        Achievement("double_promotion", "Pawn Rush", "Promote 2 pawns in a single match.", "🏃", "Mastery", maxProgress = 2),
        Achievement("stalemate", "Stalemate Survivor", "End a match in Stalemate.", "🤝", "Endgame"),
        Achievement("win_streak_3", "Winning Streak", "Win 3 games in a row.", "🏆", "Mastery", maxProgress = 3),
        Achievement("view_tutorial", "Scholar", "Open and read the Rules & Tutorial.", "🎓", "Discovery"),
        Achievement("fast_win", "Fast Win", "Win a game in under 20 total turns.", "⏱️", "Speed"),
        Achievement("perfect_queen", "Perfect Protection", "Win a match without losing your Queen.", "🛡️", "Mastery"),
        Achievement("chess_legend", "Chess Legend", "Unlock 15 other achievements.", "🌟", "Ultimate", maxProgress = 15)
    )

    val achievements: List<Achievement> get() = _achievements

    init {
        loadProgress()
    }

    private fun loadProgress() {
        for (ach in _achievements) {
            ach.isUnlocked = prefs.getBoolean("${ach.id}_unlocked", false)
            ach.currentProgress = prefs.getInt("${ach.id}_progress", 0)
        }
    }

    fun unlockOrProgress(id: String, increment: Int = 1): Achievement? {
        val ach = _achievements.find { it.id == id } ?: return null
        if (ach.isUnlocked) return null

        ach.currentProgress = (ach.currentProgress + increment).coerceAtMost(ach.maxProgress)
        prefs.edit().putInt("${ach.id}_progress", ach.currentProgress).apply()

        if (ach.currentProgress >= ach.maxProgress) {
            ach.isUnlocked = true
            prefs.edit().putBoolean("${ach.id}_unlocked", true).apply()
            
            // Check meta achievement
            if (id != "chess_legend") {
                val unlockedCount = _achievements.count { it.isUnlocked && it.id != "chess_legend" }
                unlockOrProgress("chess_legend", unlockedCount - getAchievement("chess_legend")!!.currentProgress)
            }
            
            return ach
        }
        return null
    }

    fun getAchievement(id: String): Achievement? = _achievements.find { it.id == id }
    fun getUnlockedCount(): Int = _achievements.count { it.isUnlocked }
}
