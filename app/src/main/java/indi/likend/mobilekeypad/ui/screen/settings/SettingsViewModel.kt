package indi.likend.mobilekeypad.ui.screen.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import indi.likend.mobilekeypad.domain.model.SettingItem
import indi.likend.mobilekeypad.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(settingsRepository: SettingsRepository) : ViewModel() {
    private fun <T : Any> SettingItem<T>.monitor(): MutableState<T> {
        val state = mutableStateOf(stateFlow.value)

        // 当 Flow 变化时，自动同步给 State (由外向内)
        viewModelScope.launch {
            stateFlow.collect { state.value = it }
        }

        return object : MutableState<T> by state {
            override var value: T
                get() = state.value
                set(value) {
                    viewModelScope.launch { store(value) }
                }
        }
    }

    val enableDynamicTheme = settingsRepository.enableDynamicTheme.monitor()
    val darkTheme = settingsRepository.darkTheme.monitor()
    val pureBlack = settingsRepository.pureBlack.monitor()
}
