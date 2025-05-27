package com.rafael09ed.wasted.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.rafael09ed.wasted.MainActivity
import com.rafael09ed.wasted.R
import com.rafael09ed.wasted.utils.CsvDataWriter

class DataRecordingService : Service(), SensorEventListener {
    
    companion object {
        const val CHANNEL_ID = "DATA_RECORDING_SERVICE_CHANNEL"
        const val NOTIFICATION_ID = 2
        const val ACTION_START_RECORDING = "START_RECORDING"
        const val ACTION_STOP_RECORDING = "STOP_RECORDING"
        const val ACTION_MARK_DROP = "MARK_DROP"
    }
    
    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null
    private lateinit var csvDataWriter: CsvDataWriter
    private lateinit var sharedPrefs: SharedPreferences
    private var wakeLock: PowerManager.WakeLock? = null
    
    private var startTime: Long = 0
    private var dataPointCount: Long = 0
    
    private val dropEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_MARK_DROP) {
                csvDataWriter.addDropEvent("user_marked")
                showDropMarkedNotification()
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize shared preferences
        sharedPrefs = getSharedPreferences("data_recording_prefs", Context.MODE_PRIVATE)
        
        // Initialize sensor manager and accelerometer
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        // Initialize CSV data writer
        csvDataWriter = CsvDataWriter(this)
        
        // Create notification channel
        createNotificationChannel()
        
        // Register broadcast receiver for drop marking
        val filter = IntentFilter(ACTION_MARK_DROP)
        registerReceiver(dropEventReceiver, filter, RECEIVER_NOT_EXPORTED)
        
        // Acquire wake lock to keep CPU running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WastedApp::DataRecordingWakeLock"
        )
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_MARK_DROP -> {
                csvDataWriter.addDropEvent("user_marked")
                showDropMarkedNotification()
            }
        }
        
        return START_STICKY
    }
    
    private fun startRecording() {
        startTime = System.currentTimeMillis()
        dataPointCount = 0
        
        // Start CSV recording
        csvDataWriter.startRecording()
        
        // Start foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Acquire wake lock
        wakeLock?.acquire()
        
        // Register accelerometer listener with batching
        accelerometerSensor?.let { sensor ->
            val samplingPeriodUs = 20_000 // 50Hz (20ms intervals)
            val maxReportLatencyUs = 1_000_000 // 1 second batching
            
            sensorManager.registerListener(
                this,
                sensor,
                samplingPeriodUs,
                maxReportLatencyUs
            )
        }
        
        // Update preferences
        sharedPrefs.edit()
            .putBoolean("data_recording_enabled", true)
            .apply()
    }
    
    private fun stopRecording() {
        // Stop CSV recording
        csvDataWriter.stopRecording()
        
        // Unregister sensor listener
        sensorManager.unregisterListener(this)
        
        // Release wake lock
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        
        // Update preferences
        sharedPrefs.edit()
            .putBoolean("data_recording_enabled", false)
            .apply()
        
        stopSelf()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Data Recording Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Records accelerometer data for analysis"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val markDropIntent = Intent(ACTION_MARK_DROP)
        val markDropPendingIntent = PendingIntent.getBroadcast(
            this, 1, markDropIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val stopRecordingIntent = Intent(this, DataRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopRecordingPendingIntent = PendingIntent.getService(
            this, 2, stopRecordingIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val recordingDuration = if (startTime > 0) {
            val durationMs = System.currentTimeMillis() - startTime
            val minutes = durationMs / 60000
            val seconds = (durationMs % 60000) / 1000
            String.format("%02d:%02d", minutes, seconds)
        } else "00:00"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Accelerometer Data")
            .setContentText("Duration: $recordingDuration | Data points: $dataPointCount")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                R.mipmap.ic_launcher,
                "Mark Drop",
                markDropPendingIntent
            )
            .addAction(
                R.mipmap.ic_launcher,
                "Stop",
                stopRecordingPendingIntent
            )
            .build()
    }
    
    private fun showDropMarkedNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Drop Event Marked")
            .setContentText("Drop event recorded at ${System.currentTimeMillis()}")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(999, notification)
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        
        // Add data to CSV writer
        csvDataWriter.addAccelerometerData(event)
        dataPointCount++
        
        // Update notification every 100 data points to avoid too frequent updates
        if (dataPointCount % 100 == 0L) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Unregister sensor listener
        sensorManager.unregisterListener(this)
        
        // Stop CSV recording
        csvDataWriter.stopRecording()
        
        // Release wake lock
        wakeLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        
        // Unregister broadcast receiver
        try {
            unregisterReceiver(dropEventReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered
        }
        
        // Update preferences
        sharedPrefs.edit()
            .putBoolean("data_recording_enabled", false)
            .apply()
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}