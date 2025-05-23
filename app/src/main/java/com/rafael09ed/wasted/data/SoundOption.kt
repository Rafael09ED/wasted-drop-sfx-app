package com.rafael09ed.wasted.data

import android.net.Uri

data class SoundOption(
    val id: String,
    val name: String,
    val uri: Uri? = null,
    val isBuiltIn: Boolean = false
) {
    companion object {
        fun getDefaultSound() = SoundOption(
            id = "built_in_wasted",
            name = "WASTED (Built-in)",
            uri = null,
            isBuiltIn = true
        )
    }
}