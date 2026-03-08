package indi.likend.mobilekeypad.utils

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
private class DerivedStateFlow<T>(private val getValue: () -> T, private val flow: Flow<T>) : StateFlow<T> {
    override val value: T
        get() = getValue()

    override val replayCache: List<T>
        get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<T>): Nothing {
        coroutineScope {
            flow.distinctUntilChanged()
                .stateIn(this)
                .collect(collector)
        }
    }
}

fun <T, R> StateFlow<T>.mapState(transform: (a: T) -> R): StateFlow<R> = DerivedStateFlow(
    getValue = { transform(this.value) },
    flow = this.map { a -> transform(a) }
)

fun <T1, T2, R> combineStates(flow: StateFlow<T1>, flow2: StateFlow<T2>, transform: (a: T1, b: T2) -> R): StateFlow<R> =
    DerivedStateFlow(
        getValue = { transform(flow.value, flow2.value) },
        flow = combine(flow, flow2) { a, b -> transform(a, b) }
    )

fun <T1, T2, T3, R> combineStates(
    flow: StateFlow<T1>,
    flow2: StateFlow<T2>,
    flow3: StateFlow<T3>,
    transform: (a: T1, b: T2, c: T3) -> R
): StateFlow<R> = DerivedStateFlow(
    getValue = { transform(flow.value, flow2.value, flow3.value) },
    flow = combine(flow, flow2, flow3) { a, b, c -> transform(a, b, c) }
)
