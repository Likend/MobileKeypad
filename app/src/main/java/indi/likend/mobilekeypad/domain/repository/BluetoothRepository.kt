package indi.likend.mobilekeypad.domain.repository

import indi.likend.mobilekeypad.domain.model.BluetoothConnectSession
import indi.likend.mobilekeypad.domain.model.BluetoothDevice
import indi.likend.mobilekeypad.domain.model.BluetoothScanSession
import kotlinx.coroutines.flow.StateFlow

interface BluetoothRepository {
    val pairedDevices: StateFlow<List<BluetoothDevice>>
    val isBluetoothEnable: StateFlow<Boolean>

    val lastSession: StateFlow<BluetoothConnectSession?>

    suspend fun connect(device: BluetoothDevice): BluetoothConnectSession

    fun startScan(): BluetoothScanSession
}
