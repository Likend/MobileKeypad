package indi.likend.mobilekeypad.domain.repository

import indi.likend.mobilekeypad.domain.model.SettingItem

interface SettingItemProviderRepository {
    fun <T : Any> provide(key: String, defaultValue: T): SettingItem<T>
}
