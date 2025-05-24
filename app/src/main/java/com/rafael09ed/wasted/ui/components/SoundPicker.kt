package com.rafael09ed.wasted.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rafael09ed.wasted.data.SoundOption
import com.rafael09ed.wasted.utils.SoundPlayer

@Composable
fun SoundPicker(
    selectedSound: SoundOption?,
    onSoundSelected: (SoundOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundPlayer = remember { SoundPlayer(context) }
    
    // State for file permission
    var hasFilePermission by remember {
        mutableStateOf(checkFilePermission(context))
    }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    
    // File picker launcher for MP3 files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            // Take persistable URI permission
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                
                // Get the file name
                val fileName = getFileName(context, it) ?: "Selected Audio File"
                
                val customSound = SoundOption(
                    id = "file_${System.currentTimeMillis()}",
                    name = fileName,
                    uri = it,
                    isBuiltIn = false
                )
                onSoundSelected(customSound)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Alternative file picker using GetContent (fallback)
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Get the file name
            val fileName = getFileName(context, it) ?: "Selected Audio File"
            
            val customSound = SoundOption(
                id = "file_${System.currentTimeMillis()}",
                name = fileName,
                uri = it,
                isBuiltIn = false
            )
            onSoundSelected(customSound)
        }
    }
    
    // Ringtone picker launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let {
                val ringtone = RingtoneManager.getRingtone(context, it)
                val title = ringtone?.getTitle(context) ?: "Selected Ringtone"
                
                val customSound = SoundOption(
                    id = "ringtone_${System.currentTimeMillis()}",
                    name = title,
                    uri = it,
                    isBuiltIn = false
                )
                onSoundSelected(customSound)
            }
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasFilePermission = isGranted
        if (!isGranted) {
            showPermissionDeniedDialog = true
        } else {
            filePickerLauncher.launch(arrayOf("audio/*"))
        }
    }
    
    fun requestFilePermissionAndOpenPicker() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        
        if (hasFilePermission) {
            // Try with specific MIME types first
            val mimeTypes = arrayOf(
                "audio/mpeg",      // MP3
                "audio/mp3",       // MP3 alternative
                "audio/x-mpeg",    // MP3 alternative
                "audio/wav",       // WAV
                "audio/x-wav",     // WAV alternative
                "audio/mp4",       // M4A
                "audio/aac",       // AAC
                "audio/ogg",       // OGG
                "audio/flac",      // FLAC
                "audio/*"          // Fallback for all audio
            )
            filePickerLauncher.launch(mimeTypes)
        } else {
            permissionLauncher.launch(permission)
        }
    }
    
    fun openRingtonePickerUI() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Sound for Fall Detection")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            
            selectedSound?.uri?.let {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it)
            }
        }
        ringtonePickerLauncher.launch(intent)
    }
    
    DisposableEffect(Unit) {
        onDispose {
            soundPlayer.cleanup()
        }
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sound Selection",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (selectedSound == null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "⚠️ No sound selected! Please choose a sound file to enable fall detection.",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Current selection display
            if (selectedSound != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Current Selection:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = selectedSound.name,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Custom sound file",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { soundPlayer.playSound(selectedSound.uri) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Test")
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            // Option 1: Browse MP3/Audio Files
            Text(
                text = "Option 1: Browse Audio Files",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Button(
                onClick = { requestFilePermissionAndOpenPicker() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Browse MP3/Audio Files")
            }
            
            Text(
                text = "Select MP3, WAV, or other audio files from your device storage",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Option 2: System Sounds
            Text(
                text = "Option 2: System Sounds",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Button(
                onClick = { openRingtonePickerUI() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Choose System Sound")
            }
            
            Text(
                text = "Choose from ringtones, notifications, alarms, or media sounds",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
    
    // Permission denied dialog
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Storage Permission Required") },
            text = { 
                Text("To browse and select audio files from your device, please grant storage permission. You can still use system sounds without this permission.")
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showPermissionDeniedDialog = false
                        requestFilePermissionAndOpenPicker()
                    }
                ) {
                    Text("Try Again")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDeniedDialog = false }
                ) {
                    Text("Use System Sounds")
                }
            }
        )
    }
}

private fun checkFilePermission(context: Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}

private fun getFileName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                if (nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else null
            } else null
        }
    } catch (e: Exception) {
        null
    }
}