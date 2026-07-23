package com.example.chessapp.domain

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

class SoundManager {
    private var toneGenerator: ToneGenerator? = null
    private var currentVolume: Int = 80

    init {
        initGenerator(currentVolume)
    }

    private fun initGenerator(volume: Int) {
        try {
            toneGenerator?.release()
            currentVolume = volume.coerceIn(0, 100)
            if (currentVolume > 0) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, currentVolume)
            } else {
                toneGenerator = null
            }
        } catch (e: Exception) {
            Log.e("SoundManager", "Error initializing ToneGenerator: ${e.message}")
        }
    }

    fun setVolume(volume: Int) {
        initGenerator(volume)
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

    fun playPowerUpSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 220)
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing power-up sound: ${e.message}")
        }
    }
}
