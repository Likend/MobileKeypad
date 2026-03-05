package indi.likend.mobilekeypad.domain.model

import java.io.Closeable
import kotlinx.coroutines.flow.StateFlow

interface BluetoothScanSession : Closeable {
    val devices: StateFlow<List<BluetoothDevice>>
    override fun close()
}
