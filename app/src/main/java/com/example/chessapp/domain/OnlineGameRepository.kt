package com.example.chessapp.domain

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.random.Random

class OnlineGameRepository {
    private val db = try {
        FirebaseDatabase.getInstance("https://chess-tickle-default-rtdb.europe-west1.firebasedatabase.app")
    } catch (e: Exception) {
        FirebaseDatabase.getInstance()
    }
    private val gamesRef = db.getReference("games")

    private var moveListener: ChildEventListener? = null
    private var statusListener: ValueEventListener? = null
    private var currentGameId: String? = null

    fun generateRoomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars.random() }.joinToString("")
    }

    fun createGame(gameId: String, hostColor: PieceColor, isPowerUpMode: Boolean, onCreated: (Boolean, String?) -> Unit) {
        currentGameId = gameId
        val gameData = mapOf(
            "status" to "waiting",
            "hostColor" to hostColor.name,
            "isPowerUpMode" to isPowerUpMode,
            "createdAt" to System.currentTimeMillis()
        )

        gamesRef.child(gameId).setValue(gameData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                onCreated(true, null)
            } else {
                val errorMsg = task.exception?.localizedMessage ?: "Permission denied or network failure."
                onCreated(false, errorMsg)
            }
        }
    }

    fun joinGame(gameId: String, onJoined: (Boolean, PieceColor?, Boolean) -> Unit) {
        currentGameId = gameId
        gamesRef.child(gameId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    onJoined(false, null, false)
                    return
                }

                val status = snapshot.child("status").getValue(String::class.java)
                if (status != "waiting") {
                    onJoined(false, null, false)
                    return
                }

                val hostColorStr = snapshot.child("hostColor").getValue(String::class.java) ?: "WHITE"
                val hostColor = PieceColor.valueOf(hostColorStr)
                val guestColor = hostColor.opposite()
                val isPowerUpMode = snapshot.child("isPowerUpMode").getValue(Boolean::class.java) ?: false

                // Mark game active
                gamesRef.child(gameId).child("status").setValue("active").addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        onJoined(true, guestColor, isPowerUpMode)
                    } else {
                        onJoined(false, null, false)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                onJoined(false, null, false)
            }
        })
    }

    fun pushMove(gameId: String, move: Move) {
        val dto = OnlineMoveDto.fromMove(move)
        gamesRef.child(gameId).child("moves").push().setValue(dto)
    }

    fun listenForMoves(gameId: String, onMoveReceived: (OnlineMoveDto) -> Unit) {
        removeListeners()
        currentGameId = gameId

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val dto = snapshot.getValue(OnlineMoveDto::class.java)
                if (dto != null) {
                    onMoveReceived(dto)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        moveListener = listener
        gamesRef.child(gameId).child("moves").addChildEventListener(listener)
    }

    fun listenForStatus(gameId: String, onStatusChanged: (String) -> Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status != null) {
                    onStatusChanged(status)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        statusListener = listener
        gamesRef.child(gameId).child("status").addValueEventListener(listener)
    }

    fun setGameStatus(gameId: String, status: String) {
        gamesRef.child(gameId).child("status").setValue(status)
    }

    fun cleanup() {
        removeListeners()
        currentGameId = null
    }

    private fun removeListeners() {
        val gameId = currentGameId ?: return
        moveListener?.let { gamesRef.child(gameId).child("moves").removeEventListener(it) }
        statusListener?.let { gamesRef.child(gameId).child("status").removeEventListener(it) }
        moveListener = null
        statusListener = null
    }
}
