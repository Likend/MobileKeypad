package indi.likend.mobilekeypad.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import indi.likend.mobilekeypad.data.repository.BluetoothRepositoryImpl
import indi.likend.mobilekeypad.data.repository.SettingItemProviderRepositoryImpl
import indi.likend.mobilekeypad.domain.repository.BluetoothRepository
import indi.likend.mobilekeypad.domain.repository.SettingItemProviderRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindBluetoothRepository(impl: BluetoothRepositoryImpl): BluetoothRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingItemProviderRepositoryImpl): SettingItemProviderRepository
}
