package indi.likend.mobilekeypad.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object CoroutineScopeModule {
    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope {
        // 创建一个全局的协程作用域供 Repository 使用
        return CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
