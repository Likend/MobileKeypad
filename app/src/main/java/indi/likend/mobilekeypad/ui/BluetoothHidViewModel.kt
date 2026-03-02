package indi.likend.mobilekeypad.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import indi.likend.mobilekeypad.R
import indi.likend.mobilekeypad.model.BluetoothDeviceState
import indi.likend.mobilekeypad.model.ConnectionState
import indi.likend.mobilekeypad.model.toState
import indi.likend.mobilekeypad.utils.KeyboardHidReportBuilder
import indi.likend.mobilekeypad.utils.UiEventManager
import indi.likend.mobilekeypad.utils.bluetoothAdapterStateChangedFlow
import indi.likend.mobilekeypad.utils.bluetoothDeviceBondStateChangedFlow
import indi.likend.mobilekeypad.utils.bluetoothDeviceFoundFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BluetoothHidViewModel(application: Application) : AndroidViewModel(application) {
    private val _pairedDevices = MutableStateFlow(emptyList<BluetoothDeviceState>())
    val pairedDevices = _pairedDevices.asStateFlow()

    private val _availableDevices = MutableStateFlow(emptyList<BluetoothDeviceState>())
    val availableDevices = _availableDevices.asStateFlow()

    private val _lastConnectedDevice = MutableStateFlow<BluetoothDeviceState?>(null)
    val lastConnectedDevice = _lastConnectedDevice.asStateFlow()

    private val isBluetoothSupported = (adapter != null)
    private val isBluetoothEnabledStateFlow =
        bluetoothAdapterStateChangedFlow(getApplication<Application>().applicationContext)
            .filter { event -> event.state == BluetoothAdapter.STATE_ON || event.state == BluetoothAdapter.STATE_OFF }
            .map { event -> event.state == BluetoothAdapter.STATE_ON } // map state to boolean
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = adapter?.isEnabled ?: false
            )
    val hasPermissionStateFlow = MutableStateFlow(false)
    private val internalConnectionStateFlow = MutableStateFlow<ConnectionState.Valid>(ConnectionState.Unconnected)
    val connectionStateFlow = combine(
        hasPermissionStateFlow,
        isBluetoothEnabledStateFlow,
        internalConnectionStateFlow
    ) { hasPermission, isBluetoothEnabled, internalConnectionState ->
        when {
            !isBluetoothSupported -> ConnectionState.BluetoothUnsupported
            !hasPermission -> ConnectionState.PermissionRequire
            !isBluetoothEnabled -> ConnectionState.BluetoothTurnedOff
            else -> internalConnectionState
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ConnectionState.Unconnected
    )

    val connectedDevice get() = (connectionStateFlow.value as? ConnectionState.Connected)?.device

    private var hidDevice: BluetoothHidDevice? = null

    private val adapter: BluetoothAdapter?
        get() {
            val context = getApplication<Application>()
            val manager = ContextCompat.getSystemService(context, BluetoothManager::class.java)
            return manager?.adapter
        }

    init {
        @SuppressLint("MissingPermission")
        viewModelScope.launch {
            bluetoothDeviceBondStateChangedFlow(getApplication<Application>().applicationContext)
                .collect { updatePairedDevices() }
        }

        @SuppressLint("MissingPermission")
        viewModelScope.launch {
            bluetoothDeviceFoundFlow(getApplication<Application>().applicationContext).collect { event ->
                _availableDevices.update { currentList ->
                    val index = currentList.indexOfFirst { it.address == event.device.address }
                    if (index != -1) {
                        currentList.toMutableList().apply { this[index] = event.device.toState() }
                    } else {
                        currentList + event.device.toState()
                    }
                }
            }
        }

        @SuppressLint("MissingPermission")
        viewModelScope.launch {
            combine(hasPermissionStateFlow, isBluetoothEnabledStateFlow) { hasPermission, isBluetoothEnabled ->
                hasPermission && isBluetoothEnabled && isBluetoothSupported
            }.distinctUntilChanged().collect { ready ->
                Log.d(TAG, "ready state change $ready")
                if (ready) initBluetooth() else destroyBluetooth()
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun updatePairedDevices() {
        val adapter = adapter ?: return
        _pairedDevices.value = adapter.bondedDevices.map { it.toState() }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun initBluetooth() {
        val adapter = adapter ?: return

        updatePairedDevices()
        internalConnectionStateFlow.value = ConnectionState.Unconnected
        destroyBluetooth() // try destroy
        val result = adapter.getProfileProxy(
            getApplication(),
            object : BluetoothProfile.ServiceListener {
                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
                    hidDevice = proxy as BluetoothHidDevice
                    registerHidApp()
                }

                override fun onServiceDisconnected(profile: Int) {
                    hidDevice = null
                }
            },
            BluetoothProfile.HID_DEVICE
        )
        Log.d(TAG, "init Bluetooth $result")
    }

    fun destroyBluetooth() {
        hidDevice?.let {
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
            hidDevice = null
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun registerHidApp(onSuccess: () -> Unit = {}) {
        val sdp = BluetoothHidDeviceAppSdpSettings(
            "ComposeHID",
            "HID",
            "Vendor",
            BluetoothHidDevice.SUBCLASS1_KEYBOARD,
            HID_DESCRIPTOR
        )
        val result = hidDevice?.registerApp(
            sdp,
            null,
            null,
            { it.run() },
            object : BluetoothHidDevice.Callback() {
                override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                    Log.d(TAG, "Hid App registered status changed: $registered")
                    if (registered) {
                        onSuccess()
                    }
                }

                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
                    Log.d(TAG, "设备：${device?.address} 状态变更 -> $state")
                    val deviceState = device?.toState() ?: return

                    // 更新 Compose 观察的状态
                    internalConnectionStateFlow.value = when (state) {
                        BluetoothProfile.STATE_CONNECTED -> ConnectionState.Connected(deviceState)
                        BluetoothProfile.STATE_CONNECTING -> ConnectionState.Connecting
                        BluetoothProfile.STATE_DISCONNECTED -> ConnectionState.Unconnected
                        else -> ConnectionState.Unconnected
                    }

                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        UiEventManager.postToast(
                            getApplication<Application>().resources.getString(
                                R.string.connection_connect_to_device_success,
                                device.name ?: device.address
                            )
                        )
                        _lastConnectedDevice.value = deviceState
                    }
                }
            }
        ) ?: false
        Log.d(TAG, "HID App register $result")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun unregisterHidApp() {
        val result = hidDevice?.unregisterApp() ?: false
        Log.d(TAG, "HID App unregister $result")
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    fun connect(device: BluetoothDeviceState) {
        val originalDevice = device.originalDevice!!
        connectedDevice?.let {
            if (it.address == originalDevice.address) return
            hidDevice?.disconnect(it.originalDevice!!)
        }
        val result = hidDevice?.connect(originalDevice) ?: false
        if (result) {
            internalConnectionStateFlow.value = ConnectionState.Connecting
        } else {
            UiEventManager.postToast(
                getApplication<Application>().resources.getString(
                    R.string.connection_connect_to_device_failed,
                    originalDevice.name
                )
            )
        }
        Log.d(TAG, "Connect to device ${originalDevice.name} $result")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect(device: BluetoothDeviceState) {
        val result = hidDevice?.disconnect(device.originalDevice!!) ?: false
        Log.d(TAG, "Disconnect to device ${device.name} $result")
    }

    private val reportBuilder = KeyboardHidReportBuilder()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendKeyReport() {
        val device = connectedDevice?.originalDevice ?: return
        val report = reportBuilder.build()
        val result = hidDevice?.sendReport(device, 0, report)
        if (result == false) {
            UiEventManager.postToast(getApplication<Application>().resources.getString(R.string.key_report_sent_failed))
        }
        Log.d(
            TAG,
            "send report ${
                report.fold(StringBuilder()) { acc, next -> acc.append(next).append(" ") }
            }, $result"
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun onKeyDown(scanCode: Int) {
        reportBuilder.onKeyDown(scanCode)
        sendKeyReport()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun onKeyUp(scanCode: Int) {
        reportBuilder.onKeyUp(scanCode)
        sendKeyReport()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        val adapter = adapter ?: return
        // _availableDevices.value = emptyList()
        if (adapter.isDiscovering) adapter.cancelDiscovery()
        val result = adapter.startDiscovery()
        Log.d(TAG, "start scanning $result")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        val adapter = adapter ?: return
        val result = adapter.cancelDiscovery()
        Log.d(TAG, "stop scanning $result")
        _availableDevices.value = emptyList()
    }

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    override fun onCleared() {
        super.onCleared()
        adapter?.let { adapter -> if (adapter.isDiscovering) adapter.cancelDiscovery() }
        unregisterHidApp()
        destroyBluetooth()
    }

    companion object {
        private val TAG: String = BluetoothHidViewModel::class.simpleName!!

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
    }
}
