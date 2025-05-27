package com.rafael09ed.wasted.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafael09ed.wasted.services.DataRecordingService
import com.rafael09ed.wasted.utils.CsvDataWriter
import com.rafael09ed.wasted.utils.RecordingStats
import kotlinx.coroutines.delay

@Composable
fun DataRecordingScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sharedPrefs = remember { 
        context.getSharedPreferences("data_recording_prefs", Context.MODE_PRIVATE) 
    }
    
    var isRecording by remember { 
        mutableStateOf(sharedPrefs.getBoolean("data_recording_enabled", false))
    }
    
    var recordingStats by remember { mutableStateOf(RecordingStats(0, 0, 0, 0, 0, 0)) }
    
    // Update stats periodically
    LaunchedEffect(Unit) {
        while (true) {
            val csvWriter = CsvDataWriter(context)
            recordingStats = csvWriter.getRecordingStats()
            delay(2000) // Update every 2 seconds
        }
    }
    
    // Listen for recording state changes
    LaunchedEffect(Unit) {
        while (true) {
            val currentState = sharedPrefs.getBoolean("data_recording_enabled", false)
            if (currentState != isRecording) {
                isRecording = currentState
            }
            delay(1000)
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        
        Text(
            text = "DATA RECORDING",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "Accelerometer Data Collection",
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Recording Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.primaryContainer
                               else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRecording) "Recording Active" else "Recording Inactive",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isRecording) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                if (isRecording) {
                    Text(
                        text = "📊 Collecting accelerometer data in background\n" +
                               "📱 Use notification to mark drop events",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Text(
                        text = "Tap the button below to start data collection",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = {
                        if (isRecording) {
                            // Stop recording
                            val intent = Intent(context, DataRecordingService::class.java).apply {
                                action = DataRecordingService.ACTION_STOP_RECORDING
                            }
                            context.startService(intent)
                        } else {
                            // Start recording
                            val intent = Intent(context, DataRecordingService::class.java).apply {
                                action = DataRecordingService.ACTION_START_RECORDING
                            }
                            context.startForegroundService(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = if (isRecording) "Stop Recording" else "Start Recording",
                        fontSize = 16.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        
        // File Statistics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "File Statistics",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Accelerometer Files:")
                        Text("Event Files:")
                        Text("Total Size:")
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${recordingStats.accelerometerFileCount} (${recordingStats.getAccelerometerSizeFormatted()})")
                        Text("${recordingStats.eventFileCount} (${recordingStats.getEventSizeFormatted()})")
                        Text(recordingStats.getTotalSizeFormatted(), fontWeight = FontWeight.Medium)
                    }
                }
                
                if (isRecording) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Current Session",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Buffered Data Points:")
                            Text("Buffered Events:")
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${recordingStats.bufferedDataPoints}")
                            Text("${recordingStats.bufferedEvents}")
                        }
                    }
                }
            }
        }
        
        // Instructions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "How to Use",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "1. Start recording to collect accelerometer data\n" +
                           "2. Use the 'Mark Drop' button in the notification when you drop your phone\n" +
                           "3. Data is saved to CSV files in app storage\n" +
                           "4. Files are automatically organized by date and time",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 14.sp
                )
            }
        }
        
        // File Location Info
        Text(
            text = "Files saved to: Android/data/com.rafael09ed.wasted/files/recordings/",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}