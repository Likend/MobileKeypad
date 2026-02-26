package indi.likend.mobilekeypad

import android.Manifest
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.AndroidViewModel
import indi.likend.mobilekeypad.model.BluetoothDeviceState
import indi.likend.mobilekeypad.model.ConnectionState
import indi.likend.mobilekeypad.model.toState
import indi.likend.mobilekeypad.utils.UiEventManager

class BluetoothHidViewModel(application: Application) : AndroidViewModel(application) {
    var pairedDevices by mutableStateOf(emptyList<BluetoothDeviceState>())
        private set
    var availableDevices by mutableStateOf(emptyList<BluetoothDeviceState>())
        private set
    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Unconnected)
        private set
    val connectedDevice: BluetoothDeviceState?
        get() = if (connectionState is ConnectionState.Connected) {
            (connectionState as ConnectionState.Connected).device
        } else {
            null
        }
    var lastConnectedDevice by mutableStateOf<BluetoothDeviceState?>(null)
        private set
    private var hidDevice: BluetoothHidDevice? = null

    private val adapter: BluetoothAdapter
        inline get() {
            val context = getApplication<Application>()
            val manager = getSystemService(context, BluetoothManager::class.java)
            require(manager != null)
            return manager.adapter
        }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun initBluetooth() {
        pairedDevices = adapter.bondedDevices.map { it.toState() }

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

    fun unInitBluetooth() {
        hidDevice?.let {
            adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
        }
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // 发现新设备
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }

                    device?.let {
                        // 过滤掉没有名字的设备，并更新列表
                        if (it.name != null && !availableDevices.any { d -> d.address == it.address }) {
                            availableDevices = availableDevices + it.toState()
                        }
                    }
                    Log.d(TAG, "发现新设备 ${device?.name} (${device?.address})")
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.d(TAG, "扫描结束")
                }
            }
        }
    }

    fun registerReceiver(activity: Activity) {
        // 准备 IntentFilter，监听扫描发现和扫描完成
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        activity.registerReceiver(discoveryReceiver, filter)
    }

    fun unregisterReceiver(activity: Activity) {
        activity.unregisterReceiver(discoveryReceiver)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        availableDevices = emptyList()
        if (adapter.isDiscovering) adapter.cancelDiscovery()
        val result = adapter.startDiscovery()
        Log.d(TAG, "start scanning $result")
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        val result = adapter.cancelDiscovery()
        Log.d(TAG, "stop scanning $result")
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
                    Log.d(TAG, "设备: ${device?.address} 状态变更 -> $state")
                    val deviceState = device?.toState() ?: return

                    // 更新 Compose 观察的状态
                    connectionState = when (state) {
                        BluetoothProfile.STATE_CONNECTED -> ConnectionState.Connected(deviceState)
                        BluetoothProfile.STATE_CONNECTING -> ConnectionState.Connecting
                        BluetoothProfile.STATE_DISCONNECTED -> ConnectionState.Unconnected
                        else -> ConnectionState.Unconnected
                    }

                    if (state == BluetoothProfile.STATE_CONNECTED) {
                        UiEventManager.postToast(
                            getApplication<Application>().resources.getString(
                                R.string.connection_connected_to_device_success,
                                device.name ?: device.address
                            )
                        )
                        lastConnectedDevice = deviceState
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
            connectionState = ConnectionState.Connecting
        } else {
            UiEventManager.postToast(
                getApplication<Application>().resources.getString(
                    R.string.connection_connected_to_device_failed,
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

    private val pressedKeys = LinkedHashSet<Int>(6)

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendKeyReport() {
        require(pressedKeys.size <= 6)
        val report = ByteArray(8).apply {
            pressedKeys.forEachIndexed { index, keyCode ->
                this[index + 2] = keyCode.toByte()
            }
        }
        val device = connectedDevice?.originalDevice ?: return
        val result = hidDevice?.sendReport(device, 0, report)
        if (result == false) {
            UiEventManager.postToast(getApplication<Application>().resources.getString(R.string.key_report_sent_failed))
        }
        Log.d(
            TAG,
            "send report ${
                report.fold(StringBuilder()) { acc, next -> acc.append(next).append(" ") }
            }, pressed keys : $pressedKeys, $result"
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun onKeyDown(scanCode: Int) {
        synchronized(pressedKeys) {
            pressedKeys.remove(scanCode)
            if (pressedKeys.size == 6) {
                val oldestKey = pressedKeys.iterator()
                oldestKey.next()
                oldestKey.remove()
            }
            require(pressedKeys.size < 6)
            pressedKeys.add(scanCode)
            sendKeyReport()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun onKeyUp(scanCode: Int) {
        synchronized(pressedKeys) {
            pressedKeys.remove(scanCode)
            sendKeyReport()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCleared() {
        super.onCleared()
        unregisterHidApp()
        unInitBluetooth()
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
            0x81.toByte(), 0x03.toByte(), //    Input (Const,Var,Abs): 作为输入信号，常量模式 (不可更改，通常为0)

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
