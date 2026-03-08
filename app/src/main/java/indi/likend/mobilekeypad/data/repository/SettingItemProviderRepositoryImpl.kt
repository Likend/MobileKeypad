package indi.likend.mobilekeypad.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import indi.likend.mobilekeypad.domain.model.SettingItem
import indi.likend.mobilekeypad.domain.repository.SettingItemProviderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Singleton
class SettingItemProviderRepositoryImpl @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val dataStore: DataStore<Preferences>
) : SettingItemProviderRepository {
    private fun <T> Flow<T>.stateInDefaultScope(initialValue: T) = stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5.seconds),
        initialValue = initialValue
    )

    inner class BoolSettingItemImpl(key: String, defaultValue: Boolean) : SettingItem<Boolean>(key, defaultValue) {
        private val preferencesKey = booleanPreferencesKey(key)

        override val stateFlow = dataStore.get(preferencesKey, defaultValue).stateInDefaultScope(defaultValue)

        override suspend fun store(value: Boolean) {
            Log.d("BoolSettingItemImpl", "store")
            dataStore.set(preferencesKey, value)
        }
    }

    inner class EnumSettingItemImpl<T : Any>(key: String, defaultValue: T) : SettingItem<T>(key, defaultValue) {
        private val preferencesKey = stringPreferencesKey(key)

        // 2. 安全获取枚举常量。
        // 使用 defaultValue.javaClass 获取。
        private val constants: Array<out Any>? = defaultValue.javaClass.enumConstants

        override val stateFlow =
            dataStore
                .getMapOrDefault(
                    preferencesKey,
                    { savedName ->
                        // 3. 通过遍历常量数组，匹配 name 找回枚举实例，完全替代 Enum.valueOf
                        @Suppress("UNCHECKED_CAST")
                        constants?.firstOrNull { (it as Enum<*>).name == savedName } as? T
                    },
                    defaultValue
                )
                .stateInDefaultScope(defaultValue)

        override suspend fun store(value: T) {
            dataStore.set(preferencesKey, (value as Enum<*>).name)
        }
    }

    override fun <T : Any> provide(key: String, defaultValue: T): SettingItem<T> {
        val item: SettingItem<*> = when (defaultValue) {
            is Boolean -> BoolSettingItemImpl(key, defaultValue)

            is Enum<*> ->
                @Suppress("UNCHECKED_CAST")
                EnumSettingItemImpl(key, defaultValue)

            else -> throw IllegalArgumentException("Unsupported type: ${defaultValue::class}")
        }

        @Suppress("UNCHECKED_CAST")
        return item as SettingItem<T>
    }
}

/** Returns a pre-saved preferences or `default` if it doesn't exist. */
private fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>, default: T): Flow<T> =
    data.map { preferences -> preferences[key] ?: default }

/**
 * Returns a pre-saved preferences after applying a function or `default`
 * if it doesn't exist.
 */
private inline fun <T, U> DataStore<Preferences>.getMapOrDefault(
    key: Preferences.Key<T>,
    crossinline map: (T) -> U?,
    default: U
): Flow<U> = data.map { preferences -> preferences[key]?.let(map) ?: default }

/** Sets a preferences or updates if it already exists .*/
private suspend fun <T> DataStore<Preferences>.set(key: Preferences.Key<T>, value: T) {
    edit { preferences -> preferences[key] = value }
}
