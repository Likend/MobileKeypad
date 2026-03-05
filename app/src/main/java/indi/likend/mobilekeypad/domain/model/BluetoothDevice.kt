package indi.likend.mobilekeypad.domain.model

open class BluetoothDevice(val name: String, val address: String, val deviceType: BluetoothDeviceType) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BluetoothDevice) return false
        return address == other.address
    }

    override fun hashCode(): Int = address.hashCode()

    override fun toString(): String = "BluetoothDevice($name [$address])"
}

enum class BluetoothDeviceType {
    COMPUTER,
    PHONE,
    AUDIO_VIDEO,
    PERIPHERAL,
    WEARABLE,
    UNCATEGORIZED
}
