package indi.likend.mobilekeypad.domain.model

import indi.likend.mobilekeypad.domain.utils.KeyboardHidReportBuilder
import java.io.Closeable
import kotlinx.coroutines.flow.StateFlow

abstract class BluetoothConnectSession(val device: BluetoothDevice) : Closeable {
    abstract val state: StateFlow<BluetoothConnectionState>

    protected abstract fun sendReport(report: ByteArray)

    private val reportBuilder = KeyboardHidReportBuilder()

    private fun sendKeyReport() {
        val report = reportBuilder.build()
        sendReport(report)
    }

    fun onKeyDown(scanCode: Int) {
        reportBuilder.onKeyDown(scanCode)
        sendKeyReport()
    }

    fun onKeyUp(scanCode: Int) {
        reportBuilder.onKeyUp(scanCode)
        sendKeyReport()
    }

    abstract override fun close()
}

enum class BluetoothConnectionState {
    CONNECTED,
    CONNECTING,
    DISCONNECTED
}
