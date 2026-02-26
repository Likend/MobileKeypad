package indi.likend.mobilekeypad.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import indi.likend.mobilekeypad.R
import indi.likend.mobilekeypad.ui.theme.CostumeColorScheme

sealed interface ConnectionState {
    val iconVector: ImageVector
    val text: String @Composable get() = ""
    val buttonColors: ButtonColors @Composable get() = ButtonDefaults.buttonColors()
    val cardColors: CardColors @Composable get() = CardDefaults.cardColors()

    data class Connected(val device: BluetoothDeviceState) : ConnectionState {
        override val iconVector: ImageVector get() = Icons.Default.Link
        override val text: String @Composable get() = device.name
        override val buttonColors: ButtonColors @Composable get() = ButtonDefaults.filledTonalButtonColors()
        override val cardColors: CardColors
            @Composable get() = CardDefaults.cardColors(
                containerColor = CostumeColorScheme.successContainer,
                contentColor = CostumeColorScheme.onSuccessContainer
            )
    }

    object Connecting : ConnectionState {
        override val iconVector: ImageVector get() = Icons.Default.LinkOff
        override val text: String @Composable get() = stringResource(R.string.connection_state_connecting)
        override val buttonColors: ButtonColors @Composable get() = ButtonDefaults.buttonColors()
        override val cardColors: CardColors
            @Composable get() = CardDefaults.cardColors(
                containerColor = CostumeColorScheme.warningContainer,
                contentColor = CostumeColorScheme.onWarningContainer
            )
    }

    object Unconnected : ConnectionState {
        override val iconVector: ImageVector get() = Icons.Default.LinkOff
        override val text: String @Composable get() = stringResource(R.string.connection_unconnected)
        override val buttonColors: ButtonColors @Composable get() = ButtonDefaults.buttonColors()
        override val cardColors: CardColors
            @Composable get() = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
    }

    object PermissionRequire : ConnectionState {
        override val iconVector: ImageVector get() = Icons.Default.Warning
        override val text: String @Composable get() = stringResource(R.string.permission_required)
        override val buttonColors: ButtonColors
            @Composable get() = ButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
    }
}
