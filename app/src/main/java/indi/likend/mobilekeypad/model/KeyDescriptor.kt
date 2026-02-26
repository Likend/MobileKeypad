package indi.likend.mobilekeypad.model

import androidx.compose.runtime.Composable

interface KeyDescriptor {
    val content: @Composable () -> Unit
    fun pressDown() {}
    fun pressUp() {}
}
