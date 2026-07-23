package com.example.chessapp.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chessapp.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseUser

data class AvatarOption(val id: Int, val emoji: String, val name: String)

val AVATAR_OPTIONS = listOf(
    AvatarOption(1, "♟️", "Pawn Hero"),
    AvatarOption(2, "🐴", "Knight Commander"),
    AvatarOption(3, "♗", "Bishop Sniper"),
    AvatarOption(4, "🏰", "Rook Fortress"),
    AvatarOption(5, "👸", "Cyber Queen"),
    AvatarOption(6, "👑", "Royal King"),
    AvatarOption(7, "🤪", "Tickler Boss"),
    AvatarOption(8, "🧙‍♂️", "Arcane Wizard"),
    AvatarOption(9, "🤖", "Cyber Bot"),
    AvatarOption(10, "🐉", "Dragon Master")
)

@Composable
fun SettingsScreen(
    viewModel: ChessViewModel,
    onBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val soundVolume by viewModel.soundVolume.collectAsState()
    val selectedAvatarId by viewModel.selectedAvatarId.collectAsState()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        viewModel.authManager.handleSignInResult(task) { success, _ ->
            if (success) {
                viewModel.triggerAchievement("first_move")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.chess_bg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚙️ Settings & Profile",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFD700),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
            )

            // 1. GOOGLE ACCOUNT SIGN-IN SECTION
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2746).copy(alpha = 0.95f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌐 Account Sync & Backup",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (currentUser != null) {
                        Text(
                            text = "Signed in as: ${viewModel.authManager.getUserDisplayName()}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CartoonButton(
                            text = "Sign Out",
                            onClick = { viewModel.authManager.signOut() },
                            backgroundColor = Color(0xFFE53935),
                            shadowColor = Color(0xFFB71C1C)
                        )
                    } else {
                        Text(
                            text = "Sign in with Google to sync stats & play online",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CartoonButton(
                            text = "🌐 Sign in with Google",
                            onClick = {
                                val intent = viewModel.authManager.getSignInIntent()
                                googleSignInLauncher.launch(intent)
                            },
                            backgroundColor = Color(0xFF4285F4),
                            shadowColor = Color(0xFF1A73E8)
                        )
                    }
                }
            }

            // 2. VOLUME CONTROL SECTION
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2746).copy(alpha = 0.95f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔊 Sound Effects Volume",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Volume: $soundVolume%",
                        fontSize = 14.sp,
                        color = Color.White
                    )

                    Slider(
                        value = soundVolume.toFloat(),
                        onValueChange = { viewModel.updateVolume(it.toInt()) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFD700),
                            activeTrackColor = Color(0xFFFFD700),
                            inactiveTrackColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 3. 10 USER AVATAR SELECTION GRID
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2746).copy(alpha = 0.95f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎭 Choose Your Avatar",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val avatarRows = AVATAR_OPTIONS.chunked(5)
                        for (row in avatarRows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (avatar in row) {
                                    val isSelected = selectedAvatarId == avatar.id
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) Color(0xFFFFD700) else Color(0xFF2C3E50)
                                            )
                                            .border(
                                                width = if (isSelected) 3.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable { viewModel.selectAvatar(avatar.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = avatar.emoji, fontSize = 28.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            CartoonButton(
                text = "⬅️ Back to Menu",
                onClick = onBack,
                backgroundColor = Color(0xFF757575),
                shadowColor = Color(0xFF424242)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
