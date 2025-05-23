package com.rafael09ed.wasted.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.rafael09ed.wasted.R

class SoundPlayer(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    
    fun playSound(soundUri: Uri? = null) {
        try {
            // Release any existing player
            mediaPlayer?.release()
            
            // Create new MediaPlayer and play the sound
            mediaPlayer = if (soundUri != null) {
                // Play custom sound from URI
                MediaPlayer.create(context, soundUri)
            } else {
                // Play default built-in sound
                MediaPlayer.create(context, R.raw.wasted)
            }
            
            mediaPlayer?.setOnCompletionListener { player ->
                player.release()
                mediaPlayer = null
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            // If custom sound fails, try to play default sound as fallback
            if (soundUri != null) {
                playSound(null)
            }
        }
    }
    
    // Keep the old method for backward compatibility
    fun playWastedSound() {
        playSound(null)
    }
    
    fun stopSound() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
            mediaPlayer = null
        }
    }
    
    fun cleanup() {
        stopSound()
    }
}