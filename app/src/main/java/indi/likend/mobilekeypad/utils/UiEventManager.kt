package indi.likend.mobilekeypad.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
}

object UiEventManager {
    private val _events =
        MutableSharedFlow<UiEvent>(
            replay = 0,
            extraBufferCapacity = 64
        )
    val events = _events.asSharedFlow()

    fun postEvent(event: UiEvent) {
        _events.tryEmit(event)
    }

    fun postToast(message: String) {
        postEvent(UiEvent.ShowToast(message))
    }
}
