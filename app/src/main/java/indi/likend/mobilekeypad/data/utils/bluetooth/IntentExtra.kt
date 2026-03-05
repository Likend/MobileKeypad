package indi.likend.mobilekeypad.data.utils.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Build
import android.os.Parcelable

val Intent.device get() = getParcelableExtraCompact<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)!!

val Intent.deviceBondState get() = getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)

val Intent.adapterState get() = getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

private inline fun <reified T : Parcelable> Intent.getParcelableExtraCompact(name: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra<T>(name, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra<T>(name)
    }
