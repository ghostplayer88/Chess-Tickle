package com.example.chessapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chessapp.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color

enum class GameMode { PVP, PVAI }
enum class AiDifficulty(val depth: Int) { EASY(1), MEDIUM(2), HARD(3) }
enum class AppScreen { MENU, MODE_SELECTION, GAME, TUTORIAL, CAMPAIGN, ACHIEVEMENTS }
enum class BoardTheme(val displayName: String, val lightColor: Color, val darkColor: Color) {
    WOOD("Classic Wood", Color(0xFFF0D9B5), Color(0xFFB58863)),
    EMERALD("Emerald", Color(0xFFE8EDF9), Color(0xFF769656)),
    NEON("Cyberpunk Neon", Color(0xFF34495E), Color(0xFF1ABC9C))
}

class ChessViewModel(application: Application) : AndroidViewModel(application) {
    private var game = ChessGame()
    private val ai = ChessAI()

    val achievementManager = AchievementManager(application)
    val campaignManager = CampaignManager(application)
    private val soundManager = SoundManager()

    private val _currentScreen = MutableStateFlow(AppScreen.MENU)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _unlockedToast = MutableStateFlow<Achievement?>(null)
    val unlockedToast: StateFlow<Achievement?> = _unlockedToast.asStateFlow()

    private val _activeCampaignLevel = MutableStateFlow<CampaignLevel?>(null)
    val activeCampaignLevel: StateFlow<CampaignLevel?> = _activeCampaignLevel.asStateFlow()

    private val _board = MutableStateFlow(game.board.value)
    val board: StateFlow<ChessBoard> = _board.asStateFlow()

    private val _currentTurn = MutableStateFlow(game.currentTurn.value)
    val currentTurn: StateFlow<PieceColor> = _currentTurn.asStateFlow()

    private val _status = MutableStateFlow(game.status.value)
    val status: StateFlow<GameStatus> = _status.asStateFlow()

    private val _selectedPosition = MutableStateFlow<Position?>(null)
    val selectedPosition: StateFlow<Position?> = _selectedPosition.asStateFlow()

    private val _legalMoves = MutableStateFlow<List<Move>>(emptyList())
    val legalMoves: StateFlow<List<Move>> = _legalMoves.asStateFlow()

    private val _promotionPending = MutableStateFlow<Move?>(null)
    val promotionPending: StateFlow<Move?> = _promotionPending.asStateFlow()

    private val _gameMode = MutableStateFlow(GameMode.PVP)
    val gameMode: StateFlow<GameMode> = _gameMode.asStateFlow()

    private val _aiColor = MutableStateFlow(PieceColor.BLACK)
    val aiColor: StateFlow<PieceColor> = _aiColor.asStateFlow()

    private val _aiDifficulty = MutableStateFlow(AiDifficulty.MEDIUM)
    val aiDifficulty: StateFlow<AiDifficulty> = _aiDifficulty.asStateFlow()

    private val _boardTheme = MutableStateFlow(BoardTheme.WOOD)
    val boardTheme: StateFlow<BoardTheme> = _boardTheme.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    private val _hintMove = MutableStateFlow<Move?>(null)
    val hintMove: StateFlow<Move?> = _hintMove.asStateFlow()

    fun setBoardTheme(theme: BoardTheme) {
        _boardTheme.value = theme
        triggerAchievement("change_theme")
    }

    fun goToModeSelection() {
        _currentScreen.value = AppScreen.MODE_SELECTION
    }

    fun goToTutorial() {
        _currentScreen.value = AppScreen.TUTORIAL
        triggerAchievement("view_tutorial")
    }

    fun goToCampaign() {
        _currentScreen.value = AppScreen.CAMPAIGN
    }

    fun goToAchievements() {
        _currentScreen.value = AppScreen.ACHIEVEMENTS
    }

    fun startCampaignLevel(level: CampaignLevel) {
        _activeCampaignLevel.value = level
        setBoardTheme(level.theme)
        startGame(GameMode.PVAI, PieceColor.BLACK, level.difficulty)
    }

    fun dismissToast() {
        _unlockedToast.value = null
    }

    fun triggerAchievement(id: String, increment: Int = 1) {
        val newlyUnlocked = achievementManager.unlockOrProgress(id, increment)
        if (newlyUnlocked != null) {
            _unlockedToast.value = newlyUnlocked
            soundManager.playAchievementSound()
        }
    }

    fun undoMove() {
        if (_isAiThinking.value) return
        _hintMove.value = null
        triggerAchievement("use_undo")
        soundManager.playMoveSound()
        if (_gameMode.value == GameMode.PVAI) {
            game.undoLastMove()
            game.undoLastMove()
        } else {
            game.undoLastMove()
        }
        clearSelection()
        updateFlows()
    }

    fun requestHint() {
        if (_isAiThinking.value || _status.value !is GameStatus.Active) return
        triggerAchievement("use_hint")
        soundManager.playMoveSound()
        viewModelScope.launch {
            _hintMove.value = ai.getBestMove(game, AiDifficulty.HARD)
        }
    }

    fun startGame(mode: GameMode, aiPlaysAs: PieceColor, difficulty: AiDifficulty) {
        game = ChessGame()
        _gameMode.value = mode
        _aiColor.value = aiPlaysAs
        _aiDifficulty.value = difficulty
        _isAiThinking.value = false
        _promotionPending.value = null
        _hintMove.value = null
        clearSelection()
        updateFlows()
        _currentScreen.value = AppScreen.GAME
        checkAiTurn()
    }

    fun backToMenu() {
        _activeCampaignLevel.value = null
        _currentScreen.value = AppScreen.MENU
    }

    fun onSquareClicked(position: Position) {
        if (_promotionPending.value != null || _isAiThinking.value) return 

        _hintMove.value = null
        val selected = _selectedPosition.value
        
        if (selected != null) {
            val moves = _legalMoves.value
            val moveToMake = moves.find { it.to == position }
            
            if (moveToMake != null) {
                if (moveToMake.promotion != null) {
                    _promotionPending.value = moveToMake 
                    return
                } else {
                    executeMove(moveToMake)
                    return
                }
            }
        }
        
        val piece = board.value.getPiece(position)
        if (piece != null && piece.color == currentTurn.value) {
            _selectedPosition.value = position
            _legalMoves.value = game.getLegalMoves(position)
        } else {
            clearSelection()
        }
    }

    fun onPromotionSelected(promotionType: PieceType) {
        val pendingMove = _promotionPending.value ?: return
        
        val moves = game.getLegalMoves(pendingMove.from)
        val move = moves.find { it.to == pendingMove.to && it.promotion == promotionType }
        
        if (move != null) {
            executeMove(move)
            triggerAchievement("promote_pawn")
        }
        
        _promotionPending.value = null
    }

    private fun executeMove(move: Move) {
        triggerAchievement("first_move")
        if (move.capturedPiece != null) {
            triggerAchievement("first_capture")
            soundManager.playCaptureSound()
        } else {
            soundManager.playMoveSound()
        }
        if (move.isCastling) {
            triggerAchievement("castle")
            triggerAchievement("castle_3x")
        }
        if (move.isEnPassant) {
            triggerAchievement("en_passant")
        }
        if (game.makeMove(move)) {
            clearSelection()
            updateFlows()
            checkAiTurn()
        }
    }

    private fun checkAiTurn() {
        if (_gameMode.value == GameMode.PVAI && _currentTurn.value == _aiColor.value && _status.value is GameStatus.Active) {
            _isAiThinking.value = true
            viewModelScope.launch {
                delay(1000)
                val bestMove = ai.getBestMove(game, _aiDifficulty.value)
                if (bestMove != null) {
                    executeMove(bestMove)
                }
                _isAiThinking.value = false
            }
        }
    }

    private fun updateFlows() {
        _board.value = game.board.value
        _currentTurn.value = game.currentTurn.value
        _status.value = game.status.value

        when (val currentStatus = _status.value) {
            is GameStatus.Checkmate -> {
                soundManager.playVictorySound()
                triggerAchievement("win_checkmate")
                val userColor = if (_gameMode.value == GameMode.PVAI) _aiColor.value.opposite() else PieceColor.WHITE
                if (currentStatus.winner == userColor) {
                    if (_gameMode.value == GameMode.PVAI) {
                        when (_aiDifficulty.value) {
                            AiDifficulty.EASY -> triggerAchievement("win_easy_ai")
                            AiDifficulty.MEDIUM -> triggerAchievement("win_med_ai")
                            AiDifficulty.HARD -> triggerAchievement("win_hard_ai")
                        }
                    } else {
                        triggerAchievement("play_pvp")
                    }

                    // Check campaign completion
                    val campaignLvl = _activeCampaignLevel.value
                    if (campaignLvl != null) {
                        val turnsTaken = game.moves.size
                        val (stars, _) = campaignManager.completeLevel(campaignLvl.levelNumber, turnsTaken, false)
                        
                        if (campaignLvl.levelNumber == 1) triggerAchievement("campaign_lvl1")
                        if (campaignLvl.levelNumber == 1 && stars == 3) triggerAchievement("campaign_lvl1_3stars")
                        if (campaignLvl.levelNumber == 10 && stars == 3) triggerAchievement("campaign_boss_3stars")

                        val totalStars = campaignManager.getTotalStars()
                        triggerAchievement("campaign_10stars", totalStars)
                        triggerAchievement("campaign_20stars", totalStars)
                        triggerAchievement("campaign_30stars", totalStars)

                        if (turnsTaken <= 30) triggerAchievement("campaign_speed_star")
                        triggerAchievement("campaign_queen_star")
                    }
                }
            }
            is GameStatus.Stalemate -> {
                triggerAchievement("stalemate")
            }
            is GameStatus.Active -> {}
        }
    }

    private fun clearSelection() {
        _selectedPosition.value = null
        _legalMoves.value = emptyList()
    }
}
