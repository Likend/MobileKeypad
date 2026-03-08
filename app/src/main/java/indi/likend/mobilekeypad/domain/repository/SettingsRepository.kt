package indi.likend.mobilekeypad.domain.repository

import indi.likend.mobilekeypad.domain.model.DarkTheme
import indi.likend.mobilekeypad.domain.model.SettingItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(private val settingItemProvider: SettingItemProviderRepository) {
    private fun <T : Any> provideSettingItem(key: String, defaultValue: T): SettingItem<T> =
        settingItemProvider.provide(key, defaultValue)

    val enableDynamicTheme = provideSettingItem("enable_dynamic_theme", false)
    val darkTheme = provideSettingItem("dark_theme", DarkTheme.FollowSystem)
    val pureBlack = provideSettingItem("pure_black", false)
}
