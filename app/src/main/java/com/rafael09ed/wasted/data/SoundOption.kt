package com.rafael09ed.wasted.data

import android.net.Uri

data class SoundOption(
    val id: String,
    val name: String,
    val uri: Uri,
    val isBuiltIn: Boolean = false
) {
    companion object {
        fun getDefaultSound(): SoundOption? = null
    }
}