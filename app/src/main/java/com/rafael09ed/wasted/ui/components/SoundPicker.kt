package com.rafael09ed.wasted.ui.components

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
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
import com.rafael09ed.wasted.data.SoundOption
import com.rafael09ed.wasted.utils.SoundPlayer

@Composable
fun SoundPicker(
    selectedSound: SoundOption,
    onSoundSelected: (SoundOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val soundPlayer = remember { SoundPlayer(context) }
    
    // Ringtone picker launcher - uses system's ringtone picker UI
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            uri?.let {
                // Get the ringtone title
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
    
    fun openRingtonePickerUI() {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Sound for Fall Detection")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            
            // Set currently selected ringtone
            if (!selectedSound.isBuiltIn && selectedSound.uri != null) {
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedSound.uri)
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
            
            // Built-in sound option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedSound.isBuiltIn,
                        onClick = { onSoundSelected(SoundOption.getDefaultSound()) }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedSound.isBuiltIn,
                    onClick = { onSoundSelected(SoundOption.getDefaultSound()) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "WASTED (Built-in)")
                    Text(
                        text = "Default \"wasted\" sound effect",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { soundPlayer.playSound(null) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Test")
                }
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // System ringtone/notification sound option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = !selectedSound.isBuiltIn,
                        onClick = { 
                            if (!selectedSound.isBuiltIn) {
                                // If already selected custom, allow changing it
                                openRingtonePickerUI()
                            } else {
                                openRingtonePickerUI()
                            }
                        }
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = !selectedSound.isBuiltIn,
                    onClick = { openRingtonePickerUI() }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!selectedSound.isBuiltIn) 
                            selectedSound.name 
                        else 
                            "System Sound"
                    )
                    Text(
                        text = if (!selectedSound.isBuiltIn) 
                            "Selected from system sounds" 
                        else 
                            "Choose from ringtones, notifications, or music",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (!selectedSound.isBuiltIn) {
                    OutlinedButton(
                        onClick = { soundPlayer.playSound(selectedSound.uri) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Test")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Browse button for system sounds
            Button(
                onClick = { openRingtonePickerUI() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose System Sound")
            }
            
            if (!selectedSound.isBuiltIn) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Currently selected: ${selectedSound.name}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}