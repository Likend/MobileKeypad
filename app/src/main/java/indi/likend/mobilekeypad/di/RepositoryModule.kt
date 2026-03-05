package indi.likend.mobilekeypad.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import indi.likend.mobilekeypad.data.repository.AndroidBluetoothRepository
import indi.likend.mobilekeypad.domain.repository.BluetoothRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    // 告诉 Hilt，当有人要 BluetoothRepository 接口时，给他 AndroidBluetoothRepository 实现类
    @Binds
    @Singleton
    abstract fun bindBluetoothRepository(impl: AndroidBluetoothRepository): BluetoothRepository
}
