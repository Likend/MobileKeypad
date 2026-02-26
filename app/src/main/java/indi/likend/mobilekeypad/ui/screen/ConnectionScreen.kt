package indi.likend.mobilekeypad.ui.screen

import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import indi.likend.mobilekeypad.R
import indi.likend.mobilekeypad.model.BluetoothDeviceState
import indi.likend.mobilekeypad.model.ConnectionState
import indi.likend.mobilekeypad.ui.component.CostumeScaffold
import indi.likend.mobilekeypad.ui.previewDevice
import indi.likend.mobilekeypad.ui.theme.MobileKeypadTheme
import kotlin.collections.listOf

@SuppressLint("MissingPermission")
@Composable
fun ConnectionScreen(
    connectionState: ConnectionState,
    lastConnectedDevice: BluetoothDeviceState? = null,
    pairedDevices: List<BluetoothDeviceState> = emptyList(),
    availableDevices: List<BluetoothDeviceState> = emptyList(),
    onConnectDevice: (BluetoothDeviceState) -> Unit = {},
    disposableEffect: DisposableEffectScope.() -> DisposableEffectResult = { onDispose { } }
) {
    DisposableEffect(Unit) { disposableEffect() }

    CostumeScaffold(title = stringResource(R.string.connection_heading)) {
        StatusCard(connectionState)

        Spacer(modifier = Modifier.height(12.dp))

        if (lastConnectedDevice == null && pairedDevices.isEmpty() && availableDevices.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.connection_not_found))
            }
        }

        lastConnectedDevice?.let { device ->
            Text(
                text = stringResource(R.string.connection_history_records),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            DeviceRow(
                device = device,
                isConnected =
                    connectionState is ConnectionState.Connected &&
                        device.address == connectionState.device.address,
                onClick = { onConnectDevice(device) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (!pairedDevices.isEmpty()) {
            Text(
                text = stringResource(R.string.connection_paired_devices),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            pairedDevices.forEach { device ->
                DeviceRow(
                    device = device,
                    isConnected =
                        connectionState is ConnectionState.Connected &&
                            device.address == connectionState.device.address,
                    onClick = { onConnectDevice(device) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))
        }

        if (!availableDevices.isEmpty()) {
            Text(
                text = stringResource(R.string.connection_available_devices),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            availableDevices.forEach { device ->
                DeviceRow(
                    device = device,
                    isConnected =
                        connectionState is ConnectionState.Connected &&
                            device.address == connectionState.device.address,
                    onClick = { onConnectDevice(device) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Preview
@Composable
fun PreviewConnectionScreenUnconnected() {
    ConnectionScreen(
        connectionState = ConnectionState.Unconnected
    )
}

@Preview
@Composable
fun PreviewConnectionScreenConnected() {
    ConnectionScreen(
        connectionState = ConnectionState.Connected(previewDevice),
        lastConnectedDevice = previewDevice,
        pairedDevices = listOf(previewDevice),
        availableDevices = listOf(previewDevice)
    )
}

@Preview
@Composable
fun PreviewConnectionScreenConnecting() {
    ConnectionScreen(
        connectionState = ConnectionState.Connecting,
        pairedDevices = listOf(previewDevice, previewDevice),
        availableDevices = listOf(previewDevice)
    )
}

@Composable
fun StatusCard(connectionState: ConnectionState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = connectionState.cardColors
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (connectionState is ConnectionState.Connected) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Default.Bluetooth
                },
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = 0.7f)
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                val statusText = when (connectionState) {
                    is ConnectionState.Connected ->
                        stringResource(
                            R.string.connection_connected_to_device,
                            connectionState.device.name
                        )

                    is ConnectionState.Connecting,
                    is ConnectionState.Unconnected -> connectionState.text

                    else -> ""
                }
                Text(text = statusText, fontWeight = FontWeight.Medium)
                Text(
                    text = stringResource(
                        if (connectionState is ConnectionState.Connected) {
                            R.string.connection_connected_description
                        } else {
                            R.string.connection_unconnected_description
                        }
                    ),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PreviewStatusCardConnecting() {
    MobileKeypadTheme { StatusCard(ConnectionState.Connecting) }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PreviewStatusCardUnconnected() {
    MobileKeypadTheme { StatusCard(ConnectionState.Unconnected) }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PreviewStatusCardConnected() {
    MobileKeypadTheme { StatusCard(ConnectionState.Connected(previewDevice)) }
}

@Composable
fun DeviceRow(device: BluetoothDeviceState, isConnected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = if (isConnected) 4.dp else 1.dp,
        border = if (isConnected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val contentColor = if (isConnected) {
                MaterialTheme.colorScheme.primary
            } else {
                LocalContentColor.current
            }
            Icon(
                imageVector = device.bluetoothClass.iconVector,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.7f)
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = device.name,
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.MiddleEllipsis
                )
                Text(text = device.address, color = contentColor, style = MaterialTheme.typography.bodySmall)
            }
            if (isConnected) {
                Text(
                    "当前连接",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
    }
}

private val BluetoothClass.iconVector
    get() = when (majorDeviceClass) {
        BluetoothClass.Device.Major.COMPUTER -> Icons.Default.Laptop
        BluetoothClass.Device.Major.PHONE -> Icons.Default.Smartphone
        BluetoothClass.Device.Major.AUDIO_VIDEO -> Icons.Default.Headset
        BluetoothClass.Device.Major.PERIPHERAL -> Icons.Default.Keyboard
        BluetoothClass.Device.Major.WEARABLE -> Icons.Default.Watch
        else -> Icons.Default.Bluetooth
    }
