package com.rafael09ed.wasted.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.rafael09ed.wasted.MainActivity
import com.rafael09ed.wasted.R
import com.rafael09ed.wasted.utils.FallDetector
import com.rafael09ed.wasted.utils.SoundPlayer

class AccelerometerService : Service(), FallDetector.FallDetectionListener {
    
    companion object {
        const val CHANNEL_ID = "ACCELEROMETER_SERVICE_CHANNEL"
        const val NOTIFICATION_ID = 1
        const val ACTION_START_MONITORING = "START_MONITORING"
        const val ACTION_STOP_MONITORING = "STOP_MONITORING"
    }
    
    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null
    private lateinit var fallDetector: FallDetector
    private lateinit var soundPlayer: SoundPlayer
    private lateinit var sharedPrefs: SharedPreferences
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize shared preferences
        sharedPrefs = getSharedPreferences("fall_detection_prefs", Context.MODE_PRIVATE)
        
        // Initialize sensor manager and accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        // Initialize fall detector and sound player
        fallDetector = FallDetector() // Pass context for SharedPreferences access
        fallDetector.setFallDetectionListener(this)
        soundPlayer = SoundPlayer(this)
        
        // Create notification channel
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopSelf()
        }
        
        return START_STICKY // Restart service if killed
    }
    
    private fun startMonitoring() {
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Register accelerometer listener with batching for power efficiency
        accelerometerSensor?.let { sensor ->
            val samplingPeriodUs = 20_000 // 50Hz (20ms intervals)
            val maxReportLatencyUs = 1_000_000 // 1 second batching
            
            sensorManager.registerListener(
                fallDetector,
                sensor,
                samplingPeriodUs,
                maxReportLatencyUs // This enables FIFO batching
            )
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Fall Detection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors accelerometer for fall detection"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fall Detection Active")
            .setContentText("Monitoring accelerometer for falls...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
    
    override fun onFallDetected() {
        // Get the selected sound from preferences and play it
        val soundUriString = sharedPrefs.getString("sound_uri", null)
        val soundUri = soundUriString?.let { Uri.parse(it) }
        
        // Only play sound if a custom sound is selected
        if (soundUri != null) {
            soundPlayer.playSound(soundUri)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Unregister sensor listener
        sensorManager.unregisterListener(fallDetector)
        
        // Clean up sound player
        soundPlayer.cleanup()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}