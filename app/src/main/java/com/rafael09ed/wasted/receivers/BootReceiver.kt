package com.rafael09ed.wasted.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.rafael09ed.wasted.services.AccelerometerService

class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Check if fall detection was enabled before reboot
            val sharedPrefs = context.getSharedPreferences("fall_detection_prefs", Context.MODE_PRIVATE)
            val isEnabled = sharedPrefs.getBoolean("fall_detection_enabled", false)
            
            if (isEnabled) {
                // Restart the accelerometer service
                val serviceIntent = Intent(context, AccelerometerService::class.java).apply {
                    action = AccelerometerService.ACTION_START_MONITORING
                }
                context.startForegroundService(serviceIntent)
            }
        }
    }
}