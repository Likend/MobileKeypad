package indi.likend.mobilekeypad.model

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission

data class BluetoothDeviceState(
    val name: String,
    val address: String,
    val bluetoothClass: BluetoothClass,
    val originalDevice: BluetoothDevice? = null
)

@RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
fun BluetoothDevice.toState() = BluetoothDeviceState(
    name = this.name ?: this.address,
    address = this.address,
    bluetoothClass = this.bluetoothClass,
    originalDevice = this
)
