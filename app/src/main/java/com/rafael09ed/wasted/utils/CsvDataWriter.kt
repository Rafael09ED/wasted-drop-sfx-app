package com.rafael09ed.wasted.utils

import android.content.Context
import android.hardware.SensorEvent
import kotlinx.coroutines.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

data class AccelerometerDataPoint(
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val z: Float
)

data class DropEvent(
    val timestamp: Long,
    val eventType: String,
    val notes: String
)

class CsvDataWriter(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val accelerometerBuffer = mutableListOf<AccelerometerDataPoint>()
    private val dropEventBuffer = mutableListOf<DropEvent>()
    
    private var accelerometerFile: File? = null
    private var dropEventFile: File? = null
    
    private val batchSize = 100 // Write every 100 data points
    private val flushInterval = 5000L // Or every 5 seconds
    
    private var lastFlushTime = 0L
    
    fun startRecording() {
        createFiles()
        lastFlushTime = System.currentTimeMillis()
    }
    
    fun stopRecording() {
        // Flush remaining data
        scope.launch {
            flushBuffers()
        }
        scope.cancel()
    }
    
    fun addAccelerometerData(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        val dataPoint = AccelerometerDataPoint(
            timestamp = System.currentTimeMillis(),
            x = x,
            y = y,
            z = z
        )
        
        synchronized(accelerometerBuffer) {
            accelerometerBuffer.add(dataPoint)
            
            // Check if we should flush
            if (shouldFlush()) {
                scope.launch {
                    flushBuffers()
                }
            }
        }
    }
    
    fun addDropEvent(notes: String = "user_marked") {
        val dropEvent = DropEvent(
            timestamp = System.currentTimeMillis(),
            eventType = "manual_drop",
            notes = notes
        )
        
        synchronized(dropEventBuffer) {
            dropEventBuffer.add(dropEvent)
            
            // Flush drop events immediately
            scope.launch {
                flushDropEvents()
            }
        }
    }
    
    private fun shouldFlush(): Boolean {
        return accelerometerBuffer.size >= batchSize || 
               (System.currentTimeMillis() - lastFlushTime) >= flushInterval
    }
    
    private suspend fun flushBuffers() {
        flushAccelerometerData()
        flushDropEvents()
        lastFlushTime = System.currentTimeMillis()
    }
    
    private suspend fun flushAccelerometerData() {
        val dataToWrite = synchronized(accelerometerBuffer) {
            val copy = accelerometerBuffer.toList()
            accelerometerBuffer.clear()
            copy
        }
        
        if (dataToWrite.isNotEmpty()) {
            accelerometerFile?.let { file ->
                withContext(Dispatchers.IO) {
                    FileWriter(file, true).use { writer ->
                        dataToWrite.forEach { data ->
                            writer.appendLine("${data.timestamp},${data.x},${data.y},${data.z}")
                        }
                    }
                }
            }
        }
    }
    
    private suspend fun flushDropEvents() {
        val eventsToWrite = synchronized(dropEventBuffer) {
            val copy = dropEventBuffer.toList()
            dropEventBuffer.clear()
            copy
        }
        
        if (eventsToWrite.isNotEmpty()) {
            dropEventFile?.let { file ->
                withContext(Dispatchers.IO) {
                    FileWriter(file, true).use { writer ->
                        eventsToWrite.forEach { event ->
                            writer.appendLine("${event.timestamp},${event.eventType},${event.notes}")
                        }
                    }
                }
            }
        }
    }
    
    private fun createFiles() {
        val recordingsDir = File(context.getExternalFilesDir(null), "recordings")
        recordingsDir.mkdirs()
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        
        accelerometerFile = File(recordingsDir, "accelerometer_data_$timestamp.csv").apply {
            if (!exists()) {
                createNewFile()
                // Write CSV header
                FileWriter(this).use { writer ->
                    writer.appendLine("timestamp,x,y,z")
                }
            }
        }
        
        dropEventFile = File(recordingsDir, "drop_events_$timestamp.csv").apply {
            if (!exists()) {
                createNewFile()
                // Write CSV header
                FileWriter(this).use { writer ->
                    writer.appendLine("timestamp,event_type,notes")
                }
            }
        }
    }
    
    fun getRecordingStats(): RecordingStats {
        val recordingsDir = File(context.getExternalFilesDir(null), "recordings")
        
        val allFiles = recordingsDir.listFiles() ?: emptyArray()
        val accelerometerFiles = allFiles.filter { it.name.startsWith("accelerometer_data_") }
        val eventFiles = allFiles.filter { it.name.startsWith("drop_events_") }
        
        val totalAccelerometerSize = accelerometerFiles.sumOf { it.length() }
        val totalEventSize = eventFiles.sumOf { it.length() }
        
        return RecordingStats(
            accelerometerFileCount = accelerometerFiles.size,
            eventFileCount = eventFiles.size,
            totalAccelerometerSizeBytes = totalAccelerometerSize,
            totalEventSizeBytes = totalEventSize,
            bufferedDataPoints = synchronized(accelerometerBuffer) { accelerometerBuffer.size },
            bufferedEvents = synchronized(dropEventBuffer) { dropEventBuffer.size }
        )
    }
}

data class RecordingStats(
    val accelerometerFileCount: Int,
    val eventFileCount: Int,
    val totalAccelerometerSizeBytes: Long,
    val totalEventSizeBytes: Long,
    val bufferedDataPoints: Int,
    val bufferedEvents: Int
) {
    fun getTotalSizeFormatted(): String {
        val totalBytes = totalAccelerometerSizeBytes + totalEventSizeBytes
        return formatFileSize(totalBytes)
    }
    
    fun getAccelerometerSizeFormatted(): String = formatFileSize(totalAccelerometerSizeBytes)
    fun getEventSizeFormatted(): String = formatFileSize(totalEventSizeBytes)
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }
}