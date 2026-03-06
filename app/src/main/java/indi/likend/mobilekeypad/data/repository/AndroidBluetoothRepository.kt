package indi.likend.mobilekeypad.data.repository

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED
import android.bluetooth.BluetoothDevice.ACTION_BOND_STATE_CHANGED
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import indi.likend.mobilekeypad.data.model.AndroidBluetoothDevice
import indi.likend.mobilekeypad.data.model.AndroidBluetoothScanSession
import indi.likend.mobilekeypad.data.utils.ActivityStateMonitor
import indi.likend.mobilekeypad.data.utils.bluetooth.BluetoothHidDeviceEvent
import indi.likend.mobilekeypad.data.utils.bluetooth.HidAppRegister
import indi.likend.mobilekeypad.data.utils.bluetooth.HidAppSession
import indi.likend.mobilekeypad.data.utils.bluetooth.adapterState
import indi.likend.mobilekeypad.data.utils.bluetooth.device
import indi.likend.mobilekeypad.data.utils.bluetooth.deviceBondState
import indi.likend.mobilekeypad.data.utils.bluetooth.getProfileProxyFlow
import indi.likend.mobilekeypad.domain.model.BluetoothConnectSession
import indi.likend.mobilekeypad.domain.model.BluetoothConnectionState
import indi.likend.mobilekeypad.domain.model.BluetoothDevice
import indi.likend.mobilekeypad.domain.model.BluetoothScanSession
import indi.likend.mobilekeypad.domain.repository.BluetoothRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@Singleton
class AndroidBluetoothRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val adapter: BluetoothAdapter,
    private val coroutineScope: CoroutineScope,
    private val activityStateMonitor: ActivityStateMonitor
) : BluetoothRepository {
    private sealed interface BluetoothEvent {
        data class AdapterStateChanged(val state: Int) : BluetoothEvent
        data class DeviceBondStateChanged(val device: AndroidBluetoothDevice, val bondState: Int) : BluetoothEvent
    }

    private val bluetoothEventsFlow: SharedFlow<BluetoothEvent> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    ACTION_BOND_STATE_CHANGED -> trySend(
                        BluetoothEvent.DeviceBondStateChanged(
                            AndroidBluetoothDevice(intent.device),
                            intent.deviceBondState
                        )
                    )

                    ACTION_STATE_CHANGED -> trySend(BluetoothEvent.AdapterStateChanged(intent.adapterState))
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_BOND_STATE_CHANGED)
            addAction(ACTION_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }.shareIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5000)
    )

    @SuppressLint("MissingPermission")
    private fun getPairedDevicesFromAdapter() = adapter.bondedDevices.map { AndroidBluetoothDevice(it) }
    override val pairedDevices: StateFlow<List<AndroidBluetoothDevice>> =
        bluetoothEventsFlow.map { getPairedDevicesFromAdapter() }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = getPairedDevicesFromAdapter()
        )

    override val isBluetoothEnable: StateFlow<Boolean> =
        bluetoothEventsFlow.filterIsInstance<BluetoothEvent.AdapterStateChanged>()
            .filter { event -> event.state == BluetoothAdapter.STATE_ON || event.state == BluetoothAdapter.STATE_OFF }
            .map { event -> event.state == BluetoothAdapter.STATE_ON }
            .stateIn(
                scope = coroutineScope,
                started = SharingStarted.Eagerly,
                initialValue = adapter.isEnabled
            )

    @SuppressLint("MissingPermission")
    override fun startScan(): BluetoothScanSession = AndroidBluetoothScanSession(context, coroutineScope, adapter)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val hidDeviceFlow: SharedFlow<BluetoothHidDevice?> = isBluetoothEnable.filter { it }.flatMapLatest {
        Log.d(TAG, "hidDeviceFlow flatMapLatest get update")
        adapter.getProfileProxyFlow<BluetoothHidDevice>(context)
            .catch { e -> Log.e(TAG, "getProfileProxyFlow error", e) }
    }.shareIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        replay = 1
    )

    private val hidAppRegister = HidAppRegister(sdp = SDP_SETTINGS)
    private var hidAppSession: HidAppSession? = null

    private val hidEventsFlow: SharedFlow<BluetoothHidDeviceEvent> = hidAppRegister.events.shareIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly
    )

    private val isRegistered =
        hidEventsFlow.filterIsInstance<BluetoothHidDeviceEvent.AppStatusChanged>()
            .map { it.registered }
            .shareIn(
                scope = coroutineScope,
                started = SharingStarted.Eagerly,
                replay = 1
            )

    init {
        coroutineScope.launch {
            val registerSuggestion = combine(
                hidDeviceFlow,
                activityStateMonitor.isAppForeground
            ) { hidDevice, isAppForeground ->
                Log.d(TAG, "registerSuggestion hidDevice=$hidDevice, isAppForeground=$isAppForeground")
                if (hidDevice != null && isAppForeground) hidDevice else null
            }.distinctUntilChanged()

            registerSuggestion.collect { hidDevice ->
                hidAppSession?.close()
                hidAppSession = hidDevice?.let { hidAppRegister.open(it) }
                Log.d(TAG, "registerSuggestion receive hidDevice=$hidDevice hidAppSession=$hidAppSession")
            }
        }

        coroutineScope.launch {
            hidEventsFlow.filterIsInstance<BluetoothHidDeviceEvent.ConnectionStateChanged>().collect {
                _lastSession.value?.let { currentSession ->
                    if (it.device.address == currentSession.device.address) {
                        val connectionState = when (it.state) {
                            BluetoothProfile.STATE_CONNECTED -> BluetoothConnectionState.CONNECTED
                            BluetoothProfile.STATE_CONNECTING -> BluetoothConnectionState.CONNECTING
                            BluetoothProfile.STATE_DISCONNECTED -> BluetoothConnectionState.DISCONNECTED
                            else -> BluetoothConnectionState.DISCONNECTED
                        }
                        currentSession.updateState(connectionState)
                    }
                }
            }
        }

        coroutineScope.launch {
            isRegistered.collect { registered ->
                Log.d(TAG, "registered $registered")
                if (registered) {
                    _lastSession.value?.let { lastSession ->
                        @Suppress("MissingPermission")
                        connect(lastSession.device)
                    }
                }
            }
        }

        coroutineScope.launch {
            hidDeviceFlow.collect {
                Log.d(TAG, "hidDeviceFlow $it")
            }
        }

        coroutineScope.launch {
            hidEventsFlow.collect {
                Log.d(TAG, "hidEventsFlow $it")
            }
        }
    }

    private suspend fun getHidProxy(): BluetoothHidDevice {
        val cached = hidDeviceFlow.replayCache.firstOrNull()
        if (cached != null) return cached
        return hidDeviceFlow.filterNotNull().first()
    }

    private class BluetoothConnectSessionImpl(
        device: AndroidBluetoothDevice,
        private val hidProxy: BluetoothHidDevice
    ) : BluetoothConnectSession(device) {
        private val originalDevice = device.originalData

        private val _state = MutableStateFlow(BluetoothConnectionState.CONNECTING)
        override val state = _state.asStateFlow()

        init {
            @SuppressLint("MissingPermission")
            val result = hidProxy.connect(originalDevice)
            if (!result) {
                Log.e(TAG, "hidProxy connect failed")
                _state.value = BluetoothConnectionState.DISCONNECTED
            }
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
            }
        }

        fun updateState(state: BluetoothConnectionState) {
            _state.value = state
        }
    }

    private var _lastSession = MutableStateFlow<BluetoothConnectSessionImpl?>(null)
    override val lastSession: StateFlow<BluetoothConnectSession?> = _lastSession.asStateFlow()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun connect(device: BluetoothDevice): BluetoothConnectSession {
        val device = device as AndroidBluetoothDevice
        _lastSession.value?.let { currentSession ->
            if (device == currentSession.device &&
                currentSession.state.value != BluetoothConnectionState.DISCONNECTED
            ) {
                Log.d(TAG, "connect reuse session")
                return currentSession
            }

            currentSession.close() // close old session
        }

        val hidProxy = getHidProxy()
        Log.d(TAG, "connect proxy got")

        // wait for register
        try {
            withTimeout(5000) {
                isRegistered.replayCache.firstOrNull { it } ?: isRegistered.first { it }
                Log.d(TAG, "connect app registered got")
            }
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("Bluetooth HID registration timed out", e)
        }

        val newSession = BluetoothConnectSessionImpl(device, hidProxy)

        _lastSession.value = newSession
        return newSession
    }

    companion object {
        private const val TAG: String = "AndroidBluetoothRepository"

        private val HID_DESCRIPTOR = byteArrayOf(
            // @formatter:off
            0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop): 使用通用桌面设备页面
            0x09.toByte(), 0x06.toByte(), // Usage (Keyboard): 具体设备类型为键盘
            0xa1.toByte(), 0x01.toByte(), // Collection (Application): 开始定义应用层数据包内容

            0x05.toByte(), 0x07.toByte(), //    Usage Page (Key Codes): 使用按键代码页面
            0x19.toByte(), 0xe0.toByte(), //    Usage Minimum (224): 起始键为左 Ctrl (0xE0)
            0x29.toByte(), 0xe7.toByte(), //    Usage Maximum (231): 结束键为右 GUI (0xE7)
            0x15.toByte(), 0x00.toByte(), //    Logical Minimum (0): 该位为 0 表示未按下
            0x25.toByte(), 0x01.toByte(), //    Logical Maximum (1): 该位为 1 表示按下

            // 输入报告 Report[0], 修饰键字节 (Modifier Byte)
            0x75.toByte(), 0x01.toByte(), //    Report Size (1): 每个字段占 1 bit
            0x95.toByte(), 0x08.toByte(), //    Report Count (8): 总共 8 个字段
            0x81.toByte(), 0x02.toByte(), //    Input (Data,Var,Abs): 作为输入信号，变量模式存储

            // 输入报告 Report[1], 保留字节 (Reserved Byte)
            0x95.toByte(), 0x01.toByte(), //    Report Count (1): 总共 1 个字段
            0x75.toByte(), 0x08.toByte(), //    Report Size (8): 每个字段占 8 bits (即 1 byte)
            0x81.toByte(), 0x03.toByte(), //    Input (Const,Var,Abs): 作为输入信号，常量模式 (不可更改，通常为 0)

            // 输入报告 Report[2..7], 普通键数组 (Key Array)
            0x95.toByte(), 0x06.toByte(), //    Report Count (6): 允许同时传输 6 个按键
            0x75.toByte(), 0x08.toByte(), //    Report Size (8): 每个按键代码占 8 bits (1 byte)
            0x15.toByte(), 0x00.toByte(), //    Logical Minimum (0): 键码最小值 0
            0x25.toByte(), 0x65.toByte(), //    Logical Maximum (101): 键码最大值 101 (0x65)
            0x05.toByte(), 0x07.toByte(), //    Usage Page (Key Codes): 使用按键代码页面
            0x19.toByte(), 0x00.toByte(), //    Usage Minimum (0): 代码搜索起始为 0
            0x29.toByte(), 0x65.toByte(), //    Usage Maximum (101): 代码搜索结束为 101
            0x81.toByte(), 0x00.toByte(), //    Input (Data,Ary,Abs): 作为输入信号，数组模式 (Array)

            0xc0.toByte()                 // End Collection (Application)
            // @formatter:on
        )

        private val SDP_SETTINGS = BluetoothHidDeviceAppSdpSettings(
            "MobileKeypad",
            "MobileKeypad App",
            "Likend",
            BluetoothHidDevice.SUBCLASS1_KEYBOARD,
            HID_DESCRIPTOR
        )
    }
}
