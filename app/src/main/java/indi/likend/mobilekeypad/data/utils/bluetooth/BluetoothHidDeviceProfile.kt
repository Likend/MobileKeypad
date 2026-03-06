package indi.likend.mobilekeypad.data.utils.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.util.Log
import java.io.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * @see android.bluetooth.BluetoothHidDevice.Callback
 */
sealed class BluetoothHidDeviceEvent(open val device: BluetoothDevice?) {
    /**
     * @param device object which represents host that currently
     *     has Virtual Cable established with device. Only valid when application is registered,
     *     can be `null`.
     * @param registered `true` if application is registered, `false` otherwise.
     * @see android.bluetooth.BluetoothHidDevice.Callback.onAppStatusChanged
     */
    class AppStatusChanged(device: BluetoothDevice?, val registered: Boolean) : BluetoothHidDeviceEvent(device) {
        override fun toString(): String = "AppStatusChanged(device=$device, registered=$registered)"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AppStatusChanged

            if (device != other.device) return false
            if (registered != other.registered) return false

            return true
        }

        override fun hashCode(): Int = device.hashCode() * 31 + registered.hashCode()
    }

    /**
     * @param device object representing host device which connection state was changed.
     * @param state Connection state as defined in [android.bluetooth.BluetoothProfile].
     * @see android.bluetooth.BluetoothHidDevice.Callback.onConnectionStateChanged
     */
    data class ConnectionStateChanged(override val device: BluetoothDevice, val state: Int) :
        BluetoothHidDeviceEvent(device) {
        override fun toString(): String = "ConnectionStateChanged(device=$device, state=${
            when (state) {
                BluetoothProfile.STATE_DISCONNECTED -> "STATE_DISCONNECTED"
                BluetoothProfile.STATE_CONNECTING -> "STATE_CONNECTING"
                BluetoothProfile.STATE_CONNECTED -> "STATE_CONNECTED"
                BluetoothProfile.STATE_DISCONNECTING -> "STATE_DISCONNECTING"
                else -> "UNKNOWN"
            }
        })"
    }

    /**
     * @param type Requested Report Type.
     * @param id Requested Report ID, can be 0 if no Report ID are defined in descriptor.
     * @param bufferSize Requested buffer size, application shall respond with at least given
     *     number of bytes.
     *  @see android.bluetooth.BluetoothHidDevice.Callback.onGetReport
     */
    data class GetReport(override val device: BluetoothDevice, val type: Byte, val id: Byte, val bufferSize: Int) :
        BluetoothHidDeviceEvent(device)

    /**
     * @param type Report Type.
     * @param id Report Id.
     * @param data Report data.
     * @see android.bluetooth.BluetoothHidDevice.Callback.onSetReport
     */
    data class SetReport(override val device: BluetoothDevice, val type: Byte, val id: Byte, val data: ByteArray) :
        BluetoothHidDeviceEvent(device) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SetReport

            if (type != other.type) return false
            if (id != other.id) return false
            if (device != other.device) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = type.toInt()
            result = 31 * result + id
            result = 31 * result + device.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    /**
     * @param protocol Protocol Mode.
     * @see android.bluetooth.BluetoothHidDevice.Callback.onSetProtocol
     */
    data class SetProtocol(override val device: BluetoothDevice, val protocol: Byte) : BluetoothHidDeviceEvent(device)

    /**
     * @param reportId Report Id.
     * @param data Report data.
     * @see android.bluetooth.BluetoothHidDevice.Callback.onInterruptData
     */
    data class InterruptData(override val device: BluetoothDevice, val reportId: Byte, val data: ByteArray) :
        BluetoothHidDeviceEvent(device) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as InterruptData

            if (reportId != other.reportId) return false
            if (device != other.device) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = reportId.toInt()
            result = 31 * result + device.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    /**
     * @see android.bluetooth.BluetoothHidDevice.Callback.onVirtualCableUnplug
     */
    data class VirtualCableUnplug(override val device: BluetoothDevice) : BluetoothHidDeviceEvent(device)
}

@SuppressLint("MissingPermission")
class HidAppSession internal constructor(
    private val hidDevice: BluetoothHidDevice,
    sdp: BluetoothHidDeviceAppSdpSettings,
    inQos: BluetoothHidDeviceAppQosSettings? = null,
    outQos: BluetoothHidDeviceAppQosSettings? = null,
    callback: BluetoothHidDevice.Callback
) : Closeable {
    init {
        val result = hidDevice.registerApp(sdp, inQos, outQos, { it.run() }, callback)
        Log.d(TAG, "hidDevice.registerApp $result")
    }

    override fun close() {
        hidDevice.unregisterApp()
    }

    companion object {
        private const val TAG = "HidAppSession"
    }
}

class HidAppRegister(
    private val sdp: BluetoothHidDeviceAppSdpSettings,
    private val inQos: BluetoothHidDeviceAppQosSettings? = null,
    private val outQos: BluetoothHidDeviceAppQosSettings? = null
) {
    private val _events = MutableSharedFlow<BluetoothHidDeviceEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    init {
        // 监控订阅者数量
        CoroutineScope(Dispatchers.Default).launch {
            _events.subscriptionCount.collect { count ->
                Log.d(TAG, "HidAppRegister subscriptionCount: $count")
            }
        }
    }

    val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            val result = _events.tryEmit(BluetoothHidDeviceEvent.AppStatusChanged(pluggedDevice, registered))
            Log.d(TAG, "onAppStatusChanged tryEmit $result")
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            val result = _events.tryEmit(BluetoothHidDeviceEvent.ConnectionStateChanged(device, state))
            Log.d(TAG, "onConnectionStateChanged tryEmit $result")
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            _events.tryEmit(BluetoothHidDeviceEvent.GetReport(device, type, id, bufferSize))
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            _events.tryEmit(BluetoothHidDeviceEvent.SetReport(device, type, id, data.copyOf()))
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            _events.tryEmit(BluetoothHidDeviceEvent.SetProtocol(device, protocol))
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            _events.tryEmit(BluetoothHidDeviceEvent.InterruptData(device, reportId, data.copyOf()))
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            _events.tryEmit(BluetoothHidDeviceEvent.VirtualCableUnplug(device))
        }
    }

    fun open(hidDevice: BluetoothHidDevice): HidAppSession = HidAppSession(hidDevice, sdp, inQos, outQos, callback)

    companion object {
        private const val TAG = "HidAppRegister"
    }
}
