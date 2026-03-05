package indi.likend.mobilekeypad.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import indi.likend.mobilekeypad.data.utils.ForegroundSharingStarted
import indi.likend.mobilekeypad.di.qualifiers.ForegroundStrategy
import javax.inject.Singleton
import kotlinx.coroutines.flow.SharingStarted

@Module
@InstallIn(SingletonComponent::class)
object SharingStartedModule {
    @Provides
    @Singleton
    @ForegroundStrategy
    fun provideForegroundSharingStarted(impl: ForegroundSharingStarted): SharingStarted = impl
}
