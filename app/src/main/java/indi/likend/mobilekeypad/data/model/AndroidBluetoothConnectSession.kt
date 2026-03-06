package indi.likend.mobilekeypad.data.model

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothProfile
import android.util.Log
import androidx.annotation.RequiresPermission
import indi.likend.mobilekeypad.data.utils.bluetooth.BluetoothHidDeviceEvent
import indi.likend.mobilekeypad.domain.model.BluetoothConnectSession
import indi.likend.mobilekeypad.domain.model.BluetoothConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AndroidBluetoothConnectSession(
    coroutineScope: CoroutineScope,
    override val device: AndroidBluetoothDevice,
    private val hidProxy: BluetoothHidDevice,
    private val connectionStateChangedEventFlow: Flow<BluetoothHidDeviceEvent.ConnectionStateChanged>
) : BluetoothConnectSession(device) {
    /**
     * 用于控制扫描生命周期的 [SupervisorJob]。
     * 当需要停止扫描时，通过取消此 Job 来触发所有相关协程的取消。
     */
    private val sessionJob = SupervisorJob()

    private val originalDevice get() = device.originalData
    private val _state = MutableStateFlow(BluetoothConnectionState.DISCONNECTED)
    override val state = _state.asStateFlow()

    init {
        val sessionScope = CoroutineScope(coroutineScope.coroutineContext + sessionJob)
        sessionScope.launch {
            connectionStateChangedEventFlow.collect {
                if (it.device.address == device.address) {
                    val connectionState = when (it.state) {
                        BluetoothProfile.STATE_CONNECTED -> BluetoothConnectionState.CONNECTED
                        BluetoothProfile.STATE_CONNECTING -> BluetoothConnectionState.CONNECTING
                        BluetoothProfile.STATE_DISCONNECTED -> BluetoothConnectionState.DISCONNECTED
                        else -> BluetoothConnectionState.DISCONNECTED
                    }
                    _state.value = connectionState
                }
            }
        }

        @SuppressLint("MissingPermission")
        val result = hidProxy.connect(originalDevice)
        if (!result) Log.e(TAG, "hidProxy connect failed")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun sendReport(report: ByteArray) {
        require(_state.value == BluetoothConnectionState.CONNECTED)
        hidProxy.sendReport(originalDevice, 0, report)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun close() {
        if (_state.value != BluetoothConnectionState.DISCONNECTED) {
            val result = hidProxy.disconnect(originalDevice)
            if (!result) Log.e(TAG, "hidProxy disconnect failed")
            _state.value = BluetoothConnectionState.DISCONNECTED
            sessionJob.cancel()
        }
    }

    companion object {
        private const val TAG = "AndroidBluetoothConnectSession"
    }
}
