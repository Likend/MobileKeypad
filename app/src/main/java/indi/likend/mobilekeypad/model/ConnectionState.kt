package indi.likend.mobilekeypad.model

sealed interface ConnectionState {
    sealed class Valid : ConnectionState
    sealed class Invalid : ConnectionState

    data class Connected(val device: BluetoothDeviceState) : Valid()
    object Connecting : Valid()
    object Unconnected : Valid()
    object PermissionRequire : Invalid()
    object BluetoothUnsupported : Invalid()
    object BluetoothTurnedOff : Invalid()
}
