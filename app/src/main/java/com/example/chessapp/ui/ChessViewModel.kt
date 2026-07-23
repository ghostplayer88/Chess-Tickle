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
import com.google.firebase.auth.FirebaseUser

enum class GameMode { PVP, PVAI, ONLINE, POWERUP_PVP, POWERUP_PVAI, POWERUP_ONLINE }
enum class AiDifficulty(val depth: Int) { EASY(1), MEDIUM(2), HARD(3) }
enum class AppScreen { MENU, MODE_SELECTION, GAME, TUTORIAL, CAMPAIGN, ACHIEVEMENTS, ONLINE_LOBBY, SETTINGS }
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
    val onlineRepository = OnlineGameRepository()
    val authManager = AuthManager(application)

    private val settingsPrefs = application.getSharedPreferences("chess_settings_prefs", android.content.Context.MODE_PRIVATE)

    val currentUser: StateFlow<FirebaseUser?> = authManager.currentUser

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

    private val _moveCount = MutableStateFlow(0)
    val moveCount: StateFlow<Int> = _moveCount.asStateFlow()

    private val _onlineRoomCode = MutableStateFlow<String?>(null)
    val onlineRoomCode: StateFlow<String?> = _onlineRoomCode.asStateFlow()

    private val _isWaitingForGuest = MutableStateFlow(false)
    val isWaitingForGuest: StateFlow<Boolean> = _isWaitingForGuest.asStateFlow()

    private val _onlineErrorMessage = MutableStateFlow<String?>(null)
    val onlineErrorMessage: StateFlow<String?> = _onlineErrorMessage.asStateFlow()

    private val _myOnlineColor = MutableStateFlow(PieceColor.WHITE)
    val myOnlineColor: StateFlow<PieceColor> = _myOnlineColor.asStateFlow()

    private val _soundVolume = MutableStateFlow(settingsPrefs.getInt("sound_volume", 80))
    val soundVolume: StateFlow<Int> = _soundVolume.asStateFlow()

    private val _selectedAvatarId = MutableStateFlow(settingsPrefs.getInt("selected_avatar_id", 1))
    val selectedAvatarId: StateFlow<Int> = _selectedAvatarId.asStateFlow()

    private val _selectedPowerUp = MutableStateFlow<PowerUpType?>(null)
    val selectedPowerUp: StateFlow<PowerUpType?> = _selectedPowerUp.asStateFlow()

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

        // Enforce Strict Online Turn Rule: Cannot move or interact when it's opponent's turn
        val isOnline = _gameMode.value == GameMode.ONLINE || _gameMode.value == GameMode.POWERUP_ONLINE
        if (isOnline && _currentTurn.value != _myOnlineColor.value) {
            return
        }

        if (_selectedPowerUp.value != null) {
            activatePowerUpOnSquare(position)
            return
        }

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

            // If Online mode, push move to Firebase
            if (_gameMode.value == GameMode.ONLINE && move.piece.color == _myOnlineColor.value) {
                _onlineRoomCode.value?.let { code ->
                    onlineRepository.pushMove(code, move)
                }
            }

            checkAiTurn()
        }
    }

    private fun checkAiTurn() {
        if (_gameMode.value == GameMode.PVAI && _currentTurn.value == _aiColor.value && _status.value is GameStatus.Active) {
            _isAiThinking.value = true
            viewModelScope.launch {
                delay(1000)
                val campaignLevel = _activeCampaignLevel.value
                val bestMove = if (campaignLevel != null) {
                    // Campaign match: use dedicated level-scaled Campaign AI
                    ai.getCampaignMove(game, campaignLevel.levelNumber)
                } else {
                    // Normal match: use standard difficulty AI
                    ai.getBestMove(game, _aiDifficulty.value)
                }
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
        _moveCount.value = game.moves.size

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

    // ─── ONLINE MULTIPLAYER METHODS ──────────────────────────────────────────────

    fun navigateToOnlineLobby() {
        _onlineErrorMessage.value = null
        _isWaitingForGuest.value = false
        _currentScreen.value = AppScreen.ONLINE_LOBBY
    }

    fun hostOnlineGame(hostColor: PieceColor) {
        _onlineErrorMessage.value = null
        val code = onlineRepository.generateRoomCode()
        _onlineRoomCode.value = code
        _myOnlineColor.value = hostColor
        _isWaitingForGuest.value = true

        onlineRepository.createGame(code, hostColor) { success, errorMsg ->
            if (success) {
                onlineRepository.listenForStatus(code) { status ->
                    if (status == "active" && _isWaitingForGuest.value) {
                        _isWaitingForGuest.value = false
                        startOnlineMatch(code, hostColor)
                    }
                }
            } else {
                _isWaitingForGuest.value = false
                _onlineErrorMessage.value = errorMsg ?: "Failed to create room. Check Firebase Rules."
            }
        }
    }

    fun joinOnlineGame(code: String) {
        _onlineErrorMessage.value = null
        val formattedCode = code.uppercase().trim()
        onlineRepository.joinGame(formattedCode) { success, guestColor ->
            if (success && guestColor != null) {
                _onlineRoomCode.value = formattedCode
                _myOnlineColor.value = guestColor
                _isWaitingForGuest.value = false
                startOnlineMatch(formattedCode, guestColor)
            } else {
                _onlineErrorMessage.value = "Invalid room code or game is already active."
            }
        }
    }

    private fun startOnlineMatch(gameId: String, myColor: PieceColor) {
        game = ChessGame()
        _gameMode.value = GameMode.ONLINE
        _aiColor.value = myColor.opposite() // Not used for AI, but set for consistency
        _promotionPending.value = null
        _hintMove.value = null
        clearSelection()
        updateFlows()
        _currentScreen.value = AppScreen.GAME

        // Listen for opponent's incoming moves
        onlineRepository.listenForMoves(gameId) { dto ->
            val move = dto.toMove(game.board.value)
            if (move != null && move.piece.color != _myOnlineColor.value) {
                viewModelScope.launch {
                    if (game.makeMove(move)) {
                        soundManager.playMoveSound()
                        updateFlows()
                    }
                }
            }
        }
    }

    fun leaveOnlineLobby() {
        onlineRepository.cleanup()
        _onlineRoomCode.value = null
        _isWaitingForGuest.value = false
        _onlineErrorMessage.value = null
        _currentScreen.value = AppScreen.MENU
    }

    // ─── SETTINGS & POWER-UP METHODS ─────────────────────────────────────────────

    fun goToSettings() {
        _currentScreen.value = AppScreen.SETTINGS
    }

    fun updateVolume(volume: Int) {
        _soundVolume.value = volume
        settingsPrefs.edit().putInt("sound_volume", volume).apply()
        soundManager.setVolume(volume)
    }

    fun selectAvatar(avatarId: Int) {
        _selectedAvatarId.value = avatarId
        settingsPrefs.edit().putInt("selected_avatar_id", avatarId).apply()
    }

    fun selectPowerUp(type: PowerUpType) {
        if (_selectedPowerUp.value == type) {
            _selectedPowerUp.value = null
        } else {
            _selectedPowerUp.value = type
        }
    }

    fun activatePowerUpOnSquare(pos: Position) {
        val type = _selectedPowerUp.value ?: return
        val success = game.activatePowerUp(type, pos, currentTurn.value)
        if (success) {
            soundManager.playPowerUpSound()
            _selectedPowerUp.value = null
            updateFlows()
        }
    }
}
