package com.example.chessapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.TextUnit
import com.example.chessapp.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chessapp.domain.*

@Composable
fun ChessAppScreen(viewModel: ChessViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val unlockedToast by viewModel.unlockedToast.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            AppScreen.MENU -> MenuScreen(viewModel)
            AppScreen.MODE_SELECTION -> ModeSelectionScreen(viewModel)
            AppScreen.GAME -> GameScreen(viewModel)
            AppScreen.TUTORIAL -> TutorialScreen(viewModel)
            AppScreen.CAMPAIGN -> CampaignScreen(viewModel)
            AppScreen.ACHIEVEMENTS -> AchievementsScreen(viewModel)
        }

        if (unlockedToast != null) {
            AchievementToast(achievement = unlockedToast!!, onDismiss = { viewModel.dismissToast() })
        }
    }
}

@Composable
fun MenuScreen(viewModel: ChessViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.chess_bg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)))
        
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Chess-Tickle",
                fontSize = 50.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            CartoonButton(
                text = "🗺️ CAMPAIGN MODE",
                onClick = { viewModel.goToCampaign() },
                modifier = Modifier.fillMaxWidth(0.85f),
                backgroundColor = Color(0xFFAB47BC),
                shadowColor = Color(0xFF7B1FA2),
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            CartoonButton(
                text = "🤖 VS AI (SINGLE PLAYER)",
                onClick = { viewModel.goToModeSelection() },
                modifier = Modifier.fillMaxWidth(0.85f),
                backgroundColor = Color(0xFFFF9800),
                shadowColor = Color(0xFFE65100),
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            CartoonButton(
                text = "👥 VS PLAYER (LOCAL 2P)",
                onClick = { viewModel.startGame(GameMode.PVP, PieceColor.BLACK, AiDifficulty.MEDIUM) },
                modifier = Modifier.fillMaxWidth(0.85f),
                backgroundColor = Color(0xFF4CAF50),
                shadowColor = Color(0xFF2E7D32),
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            CartoonButton(
                text = "🏆 ACHIEVEMENTS",
                onClick = { viewModel.goToAchievements() },
                modifier = Modifier.fillMaxWidth(0.85f),
                backgroundColor = Color(0xFFFFB300),
                shadowColor = Color(0xFFC67C00),
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            CartoonButton(
                text = "📖 RULES & TUTORIAL",
                onClick = { viewModel.goToTutorial() },
                modifier = Modifier.fillMaxWidth(0.85f),
                backgroundColor = Color(0xFF0288D1),
                shadowColor = Color(0xFF01579B),
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun ModeSelectionScreen(viewModel: ChessViewModel) {
    var selectedMode by remember { mutableStateOf(GameMode.PVAI) }
    var selectedPlayerColor by remember { mutableStateOf(PieceColor.WHITE) }
    var selectedDifficulty by remember { mutableStateOf(AiDifficulty.MEDIUM) }
    val currentTheme by viewModel.boardTheme.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.chess_bg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Game Setup",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text("Game Mode", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CartoonButton(
                    text = "👥 vs Player",
                    onClick = { selectedMode = GameMode.PVP },
                    backgroundColor = if (selectedMode == GameMode.PVP) Color(0xFFFFB300) else Color(0xFF37474F),
                    shadowColor = if (selectedMode == GameMode.PVP) Color(0xFFC67C00) else Color(0xFF263238),
                    fontSize = 14.sp
                )
                CartoonButton(
                    text = "🤖 vs AI",
                    onClick = { selectedMode = GameMode.PVAI },
                    backgroundColor = if (selectedMode == GameMode.PVAI) Color(0xFFFFB300) else Color(0xFF37474F),
                    shadowColor = if (selectedMode == GameMode.PVAI) Color(0xFFC67C00) else Color(0xFF263238),
                    fontSize = 14.sp
                )
            }

            if (selectedMode == GameMode.PVAI) {
                Text("Your Color", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CartoonButton(
                        text = "♔ White",
                        onClick = { selectedPlayerColor = PieceColor.WHITE },
                        backgroundColor = if (selectedPlayerColor == PieceColor.WHITE) Color(0xFF00ACC1) else Color(0xFF37474F),
                        shadowColor = if (selectedPlayerColor == PieceColor.WHITE) Color(0xFF00838F) else Color(0xFF263238),
                        fontSize = 14.sp
                    )
                    CartoonButton(
                        text = "♚ Black",
                        onClick = { selectedPlayerColor = PieceColor.BLACK },
                        backgroundColor = if (selectedPlayerColor == PieceColor.BLACK) Color(0xFF00ACC1) else Color(0xFF37474F),
                        shadowColor = if (selectedPlayerColor == PieceColor.BLACK) Color(0xFF00838F) else Color(0xFF263238),
                        fontSize = 14.sp
                    )
                }

                Text("AI Difficulty", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CartoonButton(
                        text = "🌱 Easy",
                        onClick = { selectedDifficulty = AiDifficulty.EASY },
                        backgroundColor = if (selectedDifficulty == AiDifficulty.EASY) Color(0xFF81C784) else Color(0xFF37474F),
                        shadowColor = if (selectedDifficulty == AiDifficulty.EASY) Color(0xFF388E3C) else Color(0xFF263238),
                        fontSize = 13.sp
                    )
                    CartoonButton(
                        text = "⚡ Med",
                        onClick = { selectedDifficulty = AiDifficulty.MEDIUM },
                        backgroundColor = if (selectedDifficulty == AiDifficulty.MEDIUM) Color(0xFFFFB74D) else Color(0xFF37474F),
                        shadowColor = if (selectedDifficulty == AiDifficulty.MEDIUM) Color(0xFFF57C00) else Color(0xFF263238),
                        fontSize = 13.sp
                    )
                    CartoonButton(
                        text = "🔥 Hard",
                        onClick = { selectedDifficulty = AiDifficulty.HARD },
                        backgroundColor = if (selectedDifficulty == AiDifficulty.HARD) Color(0xFFE57373) else Color(0xFF37474F),
                        shadowColor = if (selectedDifficulty == AiDifficulty.HARD) Color(0xD32F2F) else Color(0xFF263238),
                        fontSize = 13.sp
                    )
                }
            }

            Text("Board Theme", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BoardTheme.values().forEach { theme ->
                    CartoonButton(
                        text = theme.displayName,
                        onClick = { viewModel.setBoardTheme(theme) },
                        backgroundColor = if (currentTheme == theme) Color(0xFFAB47BC) else Color(0xFF37474F),
                        shadowColor = if (currentTheme == theme) Color(0xFF7B1FA2) else Color(0xFF263238),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            CartoonButton(
                text = "🚀 START GAME",
                onClick = { 
                    val aiColor = selectedPlayerColor.opposite()
                    viewModel.startGame(selectedMode, aiColor, selectedDifficulty) 
                },
                modifier = Modifier.fillMaxWidth(0.75f),
                backgroundColor = Color(0xFF4CAF50),
                shadowColor = Color(0xFF2E7D32),
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            CartoonButton(
                text = "⬅️ Back",
                onClick = { viewModel.backToMenu() },
                modifier = Modifier.fillMaxWidth(0.4f),
                backgroundColor = Color(0xFFE53935),
                shadowColor = Color(0xFFB71C1C),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun GameScreen(viewModel: ChessViewModel) {
    val board by viewModel.board.collectAsState()
    val currentTurn by viewModel.currentTurn.collectAsState()
    val selectedPosition by viewModel.selectedPosition.collectAsState()
    val legalMoves by viewModel.legalMoves.collectAsState()
    val status by viewModel.status.collectAsState()
    val promotionPending by viewModel.promotionPending.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val gameMode by viewModel.gameMode.collectAsState()
    val aiColor by viewModel.aiColor.collectAsState()
    val boardTheme by viewModel.boardTheme.collectAsState()
    val activeCampaignLevel by viewModel.activeCampaignLevel.collectAsState()
    val hintMove by viewModel.hintMove.collectAsState()
    
    val userColor = if (gameMode == GameMode.PVAI) aiColor.opposite() else PieceColor.WHITE

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.chess_bg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.15f)))

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CartoonButton(
                    text = "🛑 Quit",
                    onClick = { viewModel.backToMenu() },
                    backgroundColor = Color(0xFFE53935),
                    shadowColor = Color(0xFFB71C1C),
                    fontSize = 14.sp
                )
                Text(
                    text = "Turn: ${if (currentTurn == PieceColor.WHITE) "White" else "Black"}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD700)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CartoonButton(
                        text = "↩️ Undo",
                        onClick = { viewModel.undoMove() },
                        backgroundColor = Color(0xFFFF9800),
                        shadowColor = Color(0xFFE65100),
                        fontSize = 14.sp
                    )
                    if (gameMode == GameMode.PVAI) {
                        CartoonButton(
                            text = "💡 Hint",
                            onClick = { viewModel.requestHint() },
                            backgroundColor = Color(0xFF00ACC1),
                            shadowColor = Color(0xFF00838F),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (activeCampaignLevel != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A237E).copy(alpha = 0.9f))
                ) {
                    Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🗺️ Lvl ${activeCampaignLevel!!.levelNumber}: ${activeCampaignLevel!!.title}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Text(
                            text = activeCampaignLevel!!.lessonTip,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFE082),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Text(
                            text = "Goals: ${activeCampaignLevel!!.goal1} | ${activeCampaignLevel!!.goal2} | ${activeCampaignLevel!!.goal3}",
                            fontSize = 11.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            if (isAiThinking) {
                Text("AI is thinking...", color = Color.LightGray, modifier = Modifier.padding(bottom = 4.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            ChessBoardView(
                board = board,
                selectedPosition = selectedPosition,
                legalMoves = legalMoves,
                hintMove = hintMove,
                boardTheme = boardTheme,
                userColor = userColor,
                onSquareClicked = { pos -> viewModel.onSquareClicked(pos) }
            )

            if (promotionPending != null) {
                PromotionDialog(onPromotionSelected = { type -> viewModel.onPromotionSelected(type) })
            }

            when (val currentStatus = status) {
                is GameStatus.Checkmate -> {
                    Text(
                        text = "Checkmate! ${if (currentStatus.winner == PieceColor.WHITE) "White" else "Black"} wins!",
                        fontSize = 24.sp,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                is GameStatus.Stalemate -> {
                    Text(
                        text = "Stalemate! It's a draw.",
                        fontSize = 24.sp,
                        color = Color.Blue,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                is GameStatus.Active -> {}
            }
        }
    }
}

@Composable
fun ChessBoardView(
    board: ChessBoard,
    selectedPosition: Position?,
    legalMoves: List<Move>,
    hintMove: Move? = null,
    boardTheme: BoardTheme = BoardTheme.WOOD,
    userColor: PieceColor = PieceColor.WHITE,
    onSquareClicked: (Position) -> Unit
) {
    val rows = if (userColor == PieceColor.WHITE) 0..7 else 7 downTo 0
    val cols = if (userColor == PieceColor.WHITE) 0..7 else 7 downTo 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(2.dp, Color.Black)
    ) {
        for (row in rows) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                for (col in cols) {
                    val pos = Position(row, col)
                    val piece = board.getPiece(pos)
                    val isLightSquare = (row + col) % 2 != 0
                    val backgroundColor = if (isLightSquare) boardTheme.lightColor else boardTheme.darkColor

                    val isSelected = selectedPosition == pos
                    val isLegalMove = legalMoves.any { it.to == pos }
                    val isHintSquare = hintMove != null && (hintMove.from == pos || hintMove.to == pos)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                when {
                                    isHintSquare -> Color(0xFF00E5FF) // Bright cyan for hint
                                    isSelected -> Color(0xFFFFF59D)
                                    isLegalMove -> Color(0xFFA5D6A7)
                                    else -> backgroundColor
                                }
                            )
                            .clickable { onSquareClicked(pos) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (piece != null) {
                            Text(
                                text = piece.getUnicode(),
                                fontSize = 42.sp,
                                textAlign = TextAlign.Center,
                                color = if (piece.color == PieceColor.WHITE) Color.White else Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionDialog(onPromotionSelected: (PieceType) -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { /* Must select */ },
        title = { Text("👑 Promote Pawn", fontWeight = FontWeight.Bold) },
        text = {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                val types = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
                types.forEach { type ->
                    CartoonButton(
                        text = type.name.take(1),
                        onClick = { onPromotionSelected(type) },
                        backgroundColor = Color(0xFF00ACC1),
                        shadowColor = Color(0xFF00838F),
                        fontSize = 18.sp
                    )
                }
            }
        },
        confirmButton = {}
    )
}

private fun Piece.getUnicode(): String {
    return when (color) {
        PieceColor.WHITE -> when (type) {
            PieceType.KING -> "♔"
            PieceType.QUEEN -> "♕"
            PieceType.ROOK -> "♖"
            PieceType.BISHOP -> "♗"
            PieceType.KNIGHT -> "♘"
            PieceType.PAWN -> "♙"
        }
        PieceColor.BLACK -> when (type) {
            PieceType.KING -> "♚"
            PieceType.QUEEN -> "♛"
            PieceType.ROOK -> "♜"
            PieceType.BISHOP -> "♝"
            PieceType.KNIGHT -> "♞"
            PieceType.PAWN -> "♟"
        }
    }
}

@Composable
fun TutorialScreen(viewModel: ChessViewModel) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.chess_bg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tutorial & Rules",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFD700)
                )
                CartoonButton(
                    text = "🏠 Menu",
                    onClick = { viewModel.backToMenu() },
                    backgroundColor = Color(0xFF0288D1),
                    shadowColor = Color(0xFF01579B),
                    fontSize = 15.sp
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TutorialCard(
                    title = "🎯 Game Objective",
                    content = "The goal of Chess-Tickle is to Checkmate your opponent's King. This means placing the enemy King under attack ('Check') with no legal move to escape."
                )

                TutorialCard(
                    title = "♟️ Piece Movements",
                    content = "• Pawn (♙/♟): Moves 1 square forward (or 2 on its first move). Captures 1 square diagonally forward.\n• Knight (♘/♞): Moves in an 'L' shape (2 squares one way, 1 square perpendicular). Can jump over pieces!\n• Bishop (♗/♝): Moves diagonally any distance across open squares.\n• Rook (♖/♜): Moves horizontally or vertically any distance across open squares.\n• Queen (♕/♛): Moves in any direction any distance across open squares.\n• King (♔/♚): Moves 1 square in any direction. Protect your King!"
                )

                TutorialCard(
                    title = "✨ Special Mechanics",
                    content = "• 👑 Pawn Promotion: Reaching the opponent's back row transforms your Pawn into a Queen, Rook, Bishop, or Knight!\n• 🏰 Castling: Move your King 2 squares towards a Rook to safety (if neither has moved and path is clear/safe).\n• ⚔️ En Passant: Capture an adjacent enemy Pawn that just jumped 2 squares forward as if it moved only 1!"
                )

                TutorialCard(
                    title = "⚖️ Win & Draw Conditions",
                    content = "• Checkmate: King is under attack and has no escape -> Attacker Wins!\n• Stalemate: Player has no legal moves left and King is NOT in check -> Draw!"
                )

                TutorialCard(
                    title = "↩️ Undo & 💡 Hints",
                    content = "• ↩️ Undo Move: Reverts your last move. When playing against the AI, it automatically rolls back both your turn and the AI's turn so you get control back instantly!\n• 💡 AI Hint System: Tap the Hint button during any match to calculate and highlight the top recommended move in glowing cyan on the board."
                )

                TutorialCard(
                    title = "🎨 Custom Themes & Controls",
                    content = "• 🎨 Board Themes: Select your favorite board aesthetic in Game Setup (Classic Wood, Emerald, or Cyberpunk Neon).\n• ♔/♚ Color Alignment: Select White or Black. Your chosen color always defaults to the bottom of the screen!\n• ⚡ AI Difficulty: Choose between Easy, Medium, or Hard AI engines."
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun TutorialCard(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C).copy(alpha = 0.9f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD700),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = content,
                fontSize = 15.sp,
                color = Color.White,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun CartoonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFFFFB300),
    shadowColor: Color = Color(0xFFC67C00),
    textColor: Color = Color.White,
    enabled: Boolean = true,
    fontSize: TextUnit = 18.sp
) {
    val alpha = if (enabled) 1f else 0.5f
    val currentBg = if (enabled) backgroundColor else Color(0xFF546E7A)
    val currentShadow = if (enabled) shadowColor else Color(0xFF37474F)

    Box(
        modifier = modifier
            .alpha(alpha)
            .clickable(enabled = enabled) { onClick() }
            .background(currentShadow, shape = androidx.compose.foundation.shape.RoundedCornerShape(50))
            .padding(bottom = 4.dp)
            .background(currentBg, shape = androidx.compose.foundation.shape.RoundedCornerShape(50))
            .border(2.5.dp, Color(0xFF2A1B0E), shape = androidx.compose.foundation.shape.RoundedCornerShape(50))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CampaignScreen(viewModel: ChessViewModel) {
    val scrollState = rememberScrollState()
    val levels = viewModel.campaignManager.levels
    val totalStars = viewModel.campaignManager.getTotalStars()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.chess_bg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Campaign Mode", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700))
                    Text("Total Stars: ⭐ $totalStars / 30", fontSize = 14.sp, color = Color.White)
                }
                CartoonButton(
                    text = "🏠 Menu",
                    onClick = { viewModel.backToMenu() },
                    backgroundColor = Color(0xFF0288D1),
                    shadowColor = Color(0xFF01579B),
                    fontSize = 14.sp
                )
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                levels.forEach { lvl ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (lvl.isUnlocked) Color(0xFF2C2C2C).copy(alpha = 0.9f) else Color(0xFF1A1A1A).copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lvl ${lvl.levelNumber}: ${lvl.title}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lvl.isUnlocked) Color(0xFFFFD700) else Color.Gray
                                )
                                Text(
                                    text = lvl.description,
                                    fontSize = 13.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = lvl.lessonTip,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFFFE082),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "Goals: ${lvl.goal1} | ${lvl.goal2} | ${lvl.goal3}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF81D4FA),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                Text(
                                    text = "Stars: " + "⭐".repeat(lvl.starsEarned) + "☆".repeat(3 - lvl.starsEarned),
                                    fontSize = 14.sp,
                                    color = Color(0xFFFFB300),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                            CartoonButton(
                                text = if (lvl.isUnlocked) "▶ PLAY" else "🔒 LOCKED",
                                onClick = { if (lvl.isUnlocked) viewModel.startCampaignLevel(lvl) },
                                backgroundColor = if (lvl.isUnlocked) Color(0xFF4CAF50) else Color(0xFF546E7A),
                                shadowColor = if (lvl.isUnlocked) Color(0xFF2E7D32) else Color(0xFF37474F),
                                enabled = lvl.isUnlocked,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AchievementsScreen(viewModel: ChessViewModel) {
    val scrollState = rememberScrollState()
    val achievements = viewModel.achievementManager.achievements
    val unlockedCount = viewModel.achievementManager.getUnlockedCount()

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.chess_bg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Achievements", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD700))
                    Text("Unlocked: $unlockedCount / 30", fontSize = 14.sp, color = Color.White)
                }
                CartoonButton(
                    text = "🏠 Menu",
                    onClick = { viewModel.backToMenu() },
                    backgroundColor = Color(0xFF0288D1),
                    shadowColor = Color(0xFF01579B),
                    fontSize = 14.sp
                )
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                achievements.forEach { ach ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (ach.isUnlocked) Color(0xFF1B5E20).copy(alpha = 0.85f) else Color(0xFF2C2C2C).copy(alpha = 0.85f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = ach.icon, fontSize = 32.sp, modifier = Modifier.padding(end = 12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ach.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (ach.isUnlocked) Color(0xFFFFD700) else Color.White
                                )
                                Text(
                                    text = ach.description,
                                    fontSize = 13.sp,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                if (ach.maxProgress > 1) {
                                    Text(
                                        text = "Progress: ${ach.currentProgress} / ${ach.maxProgress}",
                                        fontSize = 12.sp,
                                        color = Color(0xFF81D4FA),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            if (ach.isUnlocked) {
                                Text("✅ UNLOCKED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD700))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun AchievementToast(achievement: Achievement, onDismiss: () -> Unit) {
    LaunchedEffect(achievement) {
        delay(3000L)
        onDismiss()
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(top = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).clickable { onDismiss() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = achievement.icon, fontSize = 36.sp, modifier = Modifier.padding(end = 12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("🏆 ACHIEVEMENT UNLOCKED!", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF3E2723))
                    Text(achievement.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(achievement.description, fontSize = 13.sp, color = Color(0xFF4E342E))
                }
            }
        }
    }
}
