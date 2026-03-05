package indi.likend.mobilekeypad.data.utils

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingCommand
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.transformLatest

@Singleton
class ForegroundSharingStarted @Inject constructor(private val monitor: ActivityStateMonitor) : SharingStarted {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun command(subscriptionCount: StateFlow<Int>): Flow<SharingCommand> =
        monitor.isAppForeground.transformLatest { isForeground ->
            if (isForeground) {
                emit(SharingCommand.START)
            } else {
                emit(SharingCommand.STOP_AND_RESET_REPLAY_CACHE)
            }
        }.distinctUntilChanged() // 双重保险
}
