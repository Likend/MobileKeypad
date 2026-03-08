package indi.likend.mobilekeypad.domain.model

import kotlinx.coroutines.flow.StateFlow

enum class DarkTheme {
    On,
    Off,
    FollowSystem
}

abstract class SettingItem<T : Any>(val key: String, val defaultValue: T) {
    abstract val stateFlow: StateFlow<T>
    abstract suspend fun store(value: T)
}
