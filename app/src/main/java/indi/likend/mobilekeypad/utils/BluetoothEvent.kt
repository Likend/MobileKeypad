package indi.likend.mobilekeypad.utils

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed interface BluetoothEvent {
    data class DeviceFound(val device: BluetoothDevice) : BluetoothEvent
    data class DeviceBondStateChanged(val device: BluetoothDevice, val bondState: Int) : BluetoothEvent
    data object AdapterDiscoveryFinished : BluetoothEvent
    data class AdapterStateChanged(val state: Int) : BluetoothEvent
}

private fun <E> broadcastReceiverFlow(
    context: Context,
    action: String,
    block: SendChannel<E>.(intent: Intent) -> Unit
): Flow<E> = callbackFlow {
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == action) {
                block(intent)
            }
        }
    }
    context.registerReceiver(receiver, IntentFilter(action))
    awaitClose {
        context.unregisterReceiver(receiver)
    }
}

fun bluetoothDeviceFoundFlow(context: Context) =
    broadcastReceiverFlow(context, BluetoothDevice.ACTION_FOUND) { intent ->
        trySend(BluetoothEvent.DeviceFound(intent.device))
    }

fun bluetoothDeviceBondStateChangedFlow(context: Context) =
    broadcastReceiverFlow(context, BluetoothDevice.ACTION_BOND_STATE_CHANGED) { intent ->
        trySend(
            BluetoothEvent.DeviceBondStateChanged(
                intent.device,
                intent.deviceBondState
            )
        )
    }

fun bluetoothAdapterStateChangedFlow(context: Context) =
    broadcastReceiverFlow(context, BluetoothAdapter.ACTION_STATE_CHANGED) { intent ->
        trySend(BluetoothEvent.AdapterStateChanged(intent.adapterState))
    }

private val Intent.device
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }!!

private val Intent.deviceBondState get() = getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)

private val Intent.adapterState get() = getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
