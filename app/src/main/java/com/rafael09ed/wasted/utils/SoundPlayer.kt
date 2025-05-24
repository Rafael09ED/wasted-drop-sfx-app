package com.rafael09ed.wasted.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri

class SoundPlayer(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    
    fun playSound(soundUri: Uri?) {
        if (soundUri == null) return
        
        try {
            // Release any existing player
            mediaPlayer?.release()
            
            // Create new MediaPlayer and play the custom sound from URI
            mediaPlayer = MediaPlayer.create(context, soundUri)
            
            mediaPlayer?.setOnCompletionListener { player ->
                player.release()
                mediaPlayer = null
            }
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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