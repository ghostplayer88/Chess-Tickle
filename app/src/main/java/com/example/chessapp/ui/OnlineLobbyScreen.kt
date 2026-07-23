package com.example.chessapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessapp.R
import com.example.chessapp.domain.PieceColor

@Composable
fun OnlineLobbyScreen(
    onHostGame: (PieceColor) -> Unit,
    onJoinGame: (String) -> Unit,
    onBack: () -> Unit,
    roomCode: String?,
    isWaitingForGuest: Boolean,
    errorMessage: String?
) {
    var joinCodeInput by remember { mutableStateOf("") }
    var selectedHostColor by remember { mutableStateOf(PieceColor.WHITE) }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.chess_bg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🌐 Online 1v1 Lobby",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (isWaitingForGuest && roomCode != null) {
                // Host Waiting View
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2746).copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Room Created!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Share this Room Code with your friend:",
                            fontSize = 14.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = roomCode,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFFD700),
                            letterSpacing = 4.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        CircularProgressIndicator(color = Color(0xFFFFD700))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Waiting for player to join...",
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            } else {
                // Host / Join Options
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2746).copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "👑 Host a New Game",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Play as:", color = Color.White, fontWeight = FontWeight.SemiBold)
                            FilterChip(
                                selected = selectedHostColor == PieceColor.WHITE,
                                onClick = { selectedHostColor = PieceColor.WHITE },
                                label = { Text("⚪ White") }
                            )
                            FilterChip(
                                selected = selectedHostColor == PieceColor.BLACK,
                                onClick = { selectedHostColor = PieceColor.BLACK },
                                label = { Text("⚫ Black") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        CartoonButton(
                            text = "🚀 Create Room",
                            onClick = { onHostGame(selectedHostColor) },
                            backgroundColor = Color(0xFF4CAF50),
                            shadowColor = Color(0xFF2E7D32)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2746).copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔑 Join Game with Code",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = joinCodeInput,
                            onValueChange = { if (it.length <= 6) joinCodeInput = it.uppercase() },
                            label = { Text("Enter 6-char Code") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFFD700),
                                unfocusedBorderColor = Color.LightGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        CartoonButton(
                            text = "⚔️ Join Game",
                            onClick = {
                                if (joinCodeInput.length == 6) {
                                    onJoinGame(joinCodeInput)
                                }
                            },
                            backgroundColor = Color(0xFF2196F3),
                            shadowColor = Color(0xFF1565C0)
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = Color(0xFFFF5252),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            CartoonButton(
                text = "⬅️ Back to Menu",
                onClick = onBack,
                backgroundColor = Color(0xFF757575),
                shadowColor = Color(0xFF424242)
            )
        }
    }
}
