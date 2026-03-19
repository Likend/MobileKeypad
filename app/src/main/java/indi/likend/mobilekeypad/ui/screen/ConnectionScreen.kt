package indi.likend.mobilekeypad.ui.screen

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Laptop
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffectResult
import androidx.compose.runtime.DisposableEffectScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import indi.likend.mobilekeypad.R
import indi.likend.mobilekeypad.domain.model.BluetoothDevice
import indi.likend.mobilekeypad.domain.model.BluetoothDeviceType
import indi.likend.mobilekeypad.ui.ConnectionState
import indi.likend.mobilekeypad.ui.component.CostumeScaffold
import indi.likend.mobilekeypad.ui.component.SessionTitle
import indi.likend.mobilekeypad.ui.connectedDevice
import indi.likend.mobilekeypad.ui.previewDevice
import indi.likend.mobilekeypad.ui.theme.CostumeColorScheme
import indi.likend.mobilekeypad.ui.theme.MobileKeypadTheme

@SuppressLint("MissingPermission")
@Composable
fun ConnectionScreen(
    connectionState: ConnectionState.Valid,
    lastConnectedDevice: BluetoothDevice? = null,
    pairedDevices: List<BluetoothDevice> = emptyList(),
    availableDevices: List<BluetoothDevice> = emptyList(),
    onConnectDevice: (BluetoothDevice) -> Unit = {},
    disposableEffect: DisposableEffectScope.() -> DisposableEffectResult = { onDispose { } }
) {
    DisposableEffect(Unit) { disposableEffect() }

    val connectedDevice: BluetoothDevice? = connectionState.connectedDevice()

    CostumeScaffold(title = stringResource(R.string.connection_heading)) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item { StatusCard(connectionState) }

            if (lastConnectedDevice == null && pairedDevices.isEmpty() && availableDevices.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.connection_not_found), modifier = Modifier.padding(16.dp))
                    }
                }
            }

            fun deviceList(devices: List<BluetoothDevice>) {
                itemsIndexed(devices) { index, device ->
                    if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                    DeviceRow(
                        device = device,
                        isConnected = connectedDevice == device,
                        onClick = { onConnectDevice(device) }
                    )
                }
            }

            lastConnectedDevice?.let { device ->
                item { SessionTitle(stringResource(R.string.connection_history_records)) }
                deviceList(listOf(device))
            }

            if (!pairedDevices.isEmpty()) {
                item { SessionTitle(stringResource(R.string.connection_paired_devices)) }
                deviceList(pairedDevices)
            }

            if (!availableDevices.isEmpty()) {
                item { SessionTitle(stringResource(R.string.connection_available_devices)) }
                deviceList(availableDevices)
            }
        }
    }
}

@Preview
@Composable
private fun PreviewConnectionScreenUnconnected() {
    ConnectionScreen(
        connectionState = ConnectionState.Disconnected
    )
}

@Preview
@Composable
private fun PreviewConnectionScreenConnected() {
    ConnectionScreen(
        connectionState = ConnectionState.Connected(previewDevice),
        lastConnectedDevice = previewDevice,
        pairedDevices = listOf(previewDevice),
        availableDevices = listOf(previewDevice)
    )
}

@Preview
@Composable
private fun PreviewConnectionScreenConnecting() {
    ConnectionScreen(
        connectionState = ConnectionState.Connecting,
        pairedDevices = listOf(previewDevice, previewDevice),
        availableDevices = listOf(previewDevice)
    )
}

@Composable
private fun StatusCard(connectionState: ConnectionState.Valid) {
    val colors = connectionState.cardColors
    ListItem(
        headlineContent = { Text(text = connectionState.statusText, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(text = connectionState.descriptionText) },
        leadingContent = {
            Icon(
                imageVector = connectionState.iconVector,
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = 0.7f)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium),
        colors = ListItemDefaults.colors(
            containerColor = colors.containerColor,
            headlineColor = colors.contentColor,
            supportingColor = colors.contentColor,
            leadingIconColor = colors.contentColor
        )
    )
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
    MobileKeypadTheme { StatusCard(ConnectionState.Disconnected) }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PreviewStatusCardConnected() {
    MobileKeypadTheme { StatusCard(ConnectionState.Connected(previewDevice)) }
}

@Composable
private fun DeviceRow(device: BluetoothDevice, isConnected: Boolean, onClick: () -> Unit) {
    val contentColor = if (isConnected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Unspecified
    }

    ListItem(
        headlineContent = {
            Text(
                text = device.name,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis
            )
        },
        supportingContent = { Text(text = device.address) },
        leadingContent = {
            Icon(
                imageVector = device.deviceType.iconVector,
                contentDescription = null
            )
        },
        trailingContent = {
            if (isConnected) {
                Text(
                    text = stringResource(R.string.connection_current),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.medium)
            .applyIf(isConnected) {
                border(
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.medium
                )
            }
            .clickable(enabled = true, onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
            headlineColor = contentColor,
            supportingColor = contentColor,
            leadingIconColor = contentColor,
            trailingIconColor = contentColor
        ),
        tonalElevation = if (isConnected) 4.dp else 1.dp
    )
}

private inline fun <T> T.applyIf(condition: Boolean, block: T.() -> T): T = if (condition) block() else this

private inline val BluetoothDeviceType.iconVector
    get() = when (this) {
        BluetoothDeviceType.COMPUTER -> Icons.Default.Laptop
        BluetoothDeviceType.PHONE -> Icons.Default.Smartphone
        BluetoothDeviceType.AUDIO_VIDEO -> Icons.Default.Headset
        BluetoothDeviceType.PERIPHERAL -> Icons.Default.Keyboard
        BluetoothDeviceType.WEARABLE -> Icons.Default.Watch
        else -> Icons.Default.Bluetooth
    }

private inline val ConnectionState.Valid.cardColors
    @Composable get() = when (this) {
        is ConnectionState.Connected -> CardDefaults.cardColors(
            containerColor = CostumeColorScheme.successContainer,
            contentColor = CostumeColorScheme.onSuccessContainer
        )

        is ConnectionState.Connecting -> CardDefaults.cardColors(
            containerColor = CostumeColorScheme.warningContainer,
            contentColor = CostumeColorScheme.onWarningContainer
        )

        is ConnectionState.Disconnected -> CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }

private inline val ConnectionState.Valid.statusText
    @Composable get() = when (this) {
        is ConnectionState.Connected -> stringResource(R.string.connection_connected_to_device, device.name)
        is ConnectionState.Connecting -> stringResource(R.string.connection_state_connecting)
        is ConnectionState.Disconnected -> stringResource(R.string.connection_unconnected)
    }

private inline val ConnectionState.Valid.descriptionText
    @Composable get() = when (this) {
        is ConnectionState.Connected -> stringResource(R.string.connection_connected_description)
        else -> stringResource(R.string.connection_unconnected_description)
    }

private inline val ConnectionState.Valid.iconVector
    get() = when (this) {
        is ConnectionState.Connected -> Icons.Default.CheckCircle
        else -> Icons.Default.Bluetooth
    }
