package com.example.chessapp.domain

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

class SoundManager {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error initializing ToneGenerator: ${e.message}")
        }
    }

    fun playMoveSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 30)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing move sound: ${e.message}")
        }
    }

    fun playCaptureSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 60)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing capture sound: ${e.message}")
        }
    }

    fun playCheckSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 120)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing check sound: ${e.message}")
        }
    }

    fun playVictorySound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing victory sound: ${e.message}")
        }
    }

    fun playAchievementSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing achievement sound: ${e.message}")
        }
    }
}
