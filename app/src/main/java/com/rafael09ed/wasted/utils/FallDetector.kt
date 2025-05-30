package com.rafael09ed.wasted.utils

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import kotlin.math.sqrt

class FallDetector : SensorEventListener {
    
    interface FallDetectionListener {
        fun onFallDetected()
    }
    
    private var fallDetectionListener: FallDetectionListener? = null
    private var lastUpdate = 0L
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    
    // Thresholds for fall detection
    private val fallThreshold = 15.0f // Sudden change threshold
    private val stopThreshold = 2.0f  // Near-zero movement threshold
    private val timeWindow = 2000L    // 2 seconds to detect stop after fall
    
    private var fallDetected = false
    private var fallTime = 0L
    
    // Buffer for processing batched events
    private val sensorDataBuffer = mutableListOf<Triple<Float, Float, Float>>()
    
    fun setFallDetectionListener(listener: FallDetectionListener) {
        this.fallDetectionListener = listener
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        
        val currentTime = System.currentTimeMillis()
        
        // When using sensor batching, we might receive multiple events at once
        // Process them in order to maintain temporal accuracy
        processAccelerometerData(event.values[0], event.values[1], event.values[2], currentTime)
    }
    
    private fun processAccelerometerData(x: Float, y: Float, z: Float, currentTime: Long) {
        // Skip if too soon since last update (throttle to ~50Hz)
        if (currentTime - lastUpdate < 20) return
        
        if (lastUpdate != 0L) {
            // Calculate change in acceleration
            val deltaX = kotlin.math.abs(x - lastX)
            val deltaY = kotlin.math.abs(y - lastY)
            val deltaZ = kotlin.math.abs(z - lastZ)
            
            val totalDelta = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
            
            // Detect sudden movement (potential fall)
            if (totalDelta > fallThreshold && !fallDetected) {
                fallDetected = true
                fallTime = currentTime
            }
            
            // Check for stop after fall
            if (fallDetected && currentTime - fallTime < timeWindow) {
                val currentMovement = sqrt(x * x + y * y + z * z)
                
                if (currentMovement < stopThreshold) {
                    // Fall followed by stop detected!
                    fallDetectionListener?.onFallDetected()
                    resetFallDetection()
                }
            } else if (fallDetected && currentTime - fallTime >= timeWindow) {
                // Reset if no stop detected within time window
                resetFallDetection()
            }
        }
        
        lastUpdate = currentTime
        lastX = x
        lastY = y
        lastZ = z
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this implementation
    }
    
    private fun resetFallDetection() {
        fallDetected = false
        fallTime = 0L
    }
}