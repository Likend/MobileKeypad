package indi.likend.mobilekeypad.data.model

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import indi.likend.mobilekeypad.domain.model.BluetoothDevice
import indi.likend.mobilekeypad.domain.model.BluetoothDeviceType

@SuppressLint("MissingPermission")
class AndroidBluetoothDevice(val originalData: android.bluetooth.BluetoothDevice) :
    BluetoothDevice(
        name = originalData.name,
        address = originalData.address,
        deviceType = originalData.bluetoothClass.toBluetoothDeviceType
    )

private val BluetoothClass.toBluetoothDeviceType
    inline get() = when (majorDeviceClass) {
        BluetoothClass.Device.Major.COMPUTER -> BluetoothDeviceType.COMPUTER
        BluetoothClass.Device.Major.PHONE -> BluetoothDeviceType.PHONE
        BluetoothClass.Device.Major.AUDIO_VIDEO -> BluetoothDeviceType.AUDIO_VIDEO
        BluetoothClass.Device.Major.PERIPHERAL -> BluetoothDeviceType.PERIPHERAL
        BluetoothClass.Device.Major.WEARABLE -> BluetoothDeviceType.WEARABLE
        else -> BluetoothDeviceType.UNCATEGORIZED
    }
