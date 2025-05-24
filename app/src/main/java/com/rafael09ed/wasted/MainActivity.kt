package com.rafael09ed.wasted

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rafael09ed.wasted.data.SoundOption
import com.rafael09ed.wasted.services.AccelerometerService
import com.rafael09ed.wasted.ui.components.SoundPicker
import com.rafael09ed.wasted.ui.theme.WastedTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var sharedPrefs: SharedPreferences
    
    // Track notification permission state
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationPermissionGranted.value = isGranted
        if (!isGranted) {
            showNotificationPermissionDialog.value = true
        }
    }
    
    // State for notification permission handling
    private val notificationPermissionGranted = mutableStateOf(false)
    private val showNotificationPermissionDialog = mutableStateOf(false)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        sharedPrefs = getSharedPreferences("fall_detection_prefs", Context.MODE_PRIVATE)
        
        // Check initial notification permission state
        checkNotificationPermission()
        
        setContent {
            WastedTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FallDetectionApp(
                        modifier = Modifier.padding(innerPadding),
                        sharedPrefs = sharedPrefs,
                        notificationPermissionGranted = notificationPermissionGranted.value,
                        showNotificationPermissionDialog = showNotificationPermissionDialog.value,
                        onDismissPermissionDialog = { showNotificationPermissionDialog.value = false },
                        onRequestNotificationPermission = { requestNotificationPermission() }
                    )
                }
            }
        }
    }
    
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionGranted.value = ContextCompat.checkSelfPermission(
                this, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            // Request permission if not granted
            if (!notificationPermissionGranted.value) {
                requestNotificationPermission()
            }
        } else {
            // For devices below API 33, notifications don't require explicit permission
            notificationPermissionGranted.value = true
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun FallDetectionApp(
    modifier: Modifier = Modifier,
    sharedPrefs: SharedPreferences,
    notificationPermissionGranted: Boolean,
    showNotificationPermissionDialog: Boolean,
    onDismissPermissionDialog: () -> Unit,
    onRequestNotificationPermission: () -> Unit
) {
    val context = LocalContext.current
    var isEnabled by remember { 
        mutableStateOf(sharedPrefs.getBoolean("fall_detection_enabled", false))
    }
    
    // Load selected sound from preferences
    var selectedSound by remember {
        mutableStateOf(loadSelectedSound(sharedPrefs))
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            Text(
                text = "WASTED",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Drop Detection App",
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Show notification permission warning if not granted
            if (!notificationPermissionGranted) {
                NotificationPermissionWarning(
                    onRequestPermission = onRequestNotificationPermission
                )
            }
            
            // Sound picker section
            SoundPicker(
                selectedSound = selectedSound,
                onSoundSelected = { sound ->
                    selectedSound = sound
                    saveSelectedSound(sharedPrefs, sound)
                }
            )
            
            // Fall detection control section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isEnabled) "Fall Detection Active" else "Fall Detection Inactive",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isEnabled) MaterialTheme.colorScheme.primary 
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isEnabled) {
                        Text(
                            text = "Monitoring accelerometer for falls in the background",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Tap the button below to start monitoring",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            if (isEnabled) {
                                // Stop the service
                                val intent = Intent(context, AccelerometerService::class.java).apply {
                                    action = AccelerometerService.ACTION_STOP_MONITORING
                                }
                                context.startService(intent)
                                
                                // Update preferences
                                sharedPrefs.edit()
                                    .putBoolean("fall_detection_enabled", false)
                                    .apply()
                                
                                isEnabled = false
                            } else if (selectedSound != null) {
                                // Start the service only if a sound is selected
                                val intent = Intent(context, AccelerometerService::class.java).apply {
                                    action = AccelerometerService.ACTION_START_MONITORING
                                }
                                context.startForegroundService(intent)
                                
                                // Update preferences
                                sharedPrefs.edit()
                                    .putBoolean("fall_detection_enabled", true)
                                    .apply()
                                
                                isEnabled = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isEnabled || selectedSound != null, // Disable button if no sound selected
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isEnabled) MaterialTheme.colorScheme.error
                                           else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isEnabled) "Disable Fall Detection" 
                                  else if (selectedSound != null) "Enable Fall Detection"
                                  else "Select Sound First",
                            fontSize = 16.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
            
            Text(
                text = "When enabled, the app will monitor your device's accelerometer for sudden movements followed by stops, indicating a potential fall. When detected, it will play your selected sound effect.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        
        // Notification permission dialog
        if (showNotificationPermissionDialog) {
            NotificationPermissionDialog(
                onDismiss = onDismissPermissionDialog,
                onRetry = {
                    onDismissPermissionDialog()
                    onRequestNotificationPermission()
                }
            )
        }
    }
}

@Composable
fun NotificationPermissionWarning(
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⚠️ Notifications Disabled",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "For best functionality, please enable notifications to receive fall detection alerts.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Enable Notifications")
            }
        }
    }
}

@Composable
fun NotificationPermissionDialog(
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.notification_permission_title))
        },
        text = {
            Text(text = stringResource(R.string.notification_permission_message))
        },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.notification_permission_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notification_permission_dismiss))
            }
        }
    )
}

private fun loadSelectedSound(sharedPrefs: SharedPreferences): SoundOption? {
    val isBuiltIn = sharedPrefs.getBoolean("sound_is_built_in", true)
    return if (isBuiltIn) {
        // No more built-in sound, return null
        null
    } else {
        val soundName = sharedPrefs.getString("sound_name", null)
        val soundUriString = sharedPrefs.getString("sound_uri", null)
        val soundUri = soundUriString?.let { Uri.parse(it) }
        
        if (soundName != null && soundUri != null) {
            SoundOption(
                id = "custom_saved",
                name = soundName,
                uri = soundUri,
                isBuiltIn = false
            )
        } else {
            null
        }
    }
}

private fun saveSelectedSound(sharedPrefs: SharedPreferences, sound: SoundOption) {
    sharedPrefs.edit()
        .putBoolean("sound_is_built_in", false)
        .putString("sound_name", sound.name)
        .putString("sound_uri", sound.uri.toString())
        .apply()
}