package indi.likend.mobilekeypad.ui

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.annotation.IntRange
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import indi.likend.mobilekeypad.BluetoothHidViewModel
import indi.likend.mobilekeypad.model.ConnectionState
import indi.likend.mobilekeypad.model.KeyDescriptor
import indi.likend.mobilekeypad.model.KeypadLayout
import indi.likend.mobilekeypad.ui.screen.ConnectionScreen
import indi.likend.mobilekeypad.ui.screen.HomeScreen
import indi.likend.mobilekeypad.ui.screen.settings.MainSettingScreen
import indi.likend.mobilekeypad.ui.theme.MobileKeypadTheme
import indi.likend.mobilekeypad.utils.UiEvent
import indi.likend.mobilekeypad.utils.UiEventManager

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MobileKeypadApp() {
    val navController = rememberNavController()
    val bluetoothHidViewModel = viewModel<BluetoothHidViewModel>()

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Android 12+ 需要扫描和连接权限
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        // Android 11 及以下需要位置权限才能扫描蓝牙
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    val permissionsState = rememberMultiplePermissionsState(permissionsToRequest) { isGranted ->
        if (isGranted.all { it.value }) {
            bluetoothHidViewModel.initBluetooth()
        }
    }

    val connectionState = if (!permissionsState.allPermissionsGranted) {
        ConnectionState.PermissionRequire
    } else {
        bluetoothHidViewModel.connectionState
    }

    LaunchedEffect(Unit) {
        if (permissionsState.allPermissionsGranted) {
            bluetoothHidViewModel.initBluetooth()
        }
    }
    HandleUiEvent()
    RegisterReceiver(bluetoothHidViewModel)
    RegisterHidApp(bluetoothHidViewModel)

    MobileKeypadTheme {
        NavHost(
            navController = navController,
            startDestination = Route.Home,
            modifier = Modifier.background(MaterialTheme.colorScheme.background)
        ) {
            composable<Route.Home> {
                var selectedTab by remember { mutableIntStateOf(0) }
                val onChangeSelectedTab: (Int) -> Unit = { tabIndex ->
                    selectedTab = tabIndex
                    val prefixCode = 64 + tabIndex
                    bluetoothHidViewModel.onKeyDown(prefixCode)
                    bluetoothHidViewModel.onKeyUp(prefixCode)
                }
                HomeScreen(
                    navController = navController,
                    connectionState = connectionState,
                    onClickConnectionButton = {
                        when (connectionState) {
                            is ConnectionState.PermissionRequire -> permissionsState.launchMultiplePermissionRequest()
                            else -> navController.navigate(Route.Connection)
                        }
                    },
                    selectedTab = selectedTab,
                    onSelectedTab = onChangeSelectedTab,
                    keypadLayouts = keypadLayouts(bluetoothHidViewModel, onChangeSelectedTab)
                )
            }
            composable<Route.Settings> {
                MainSettingScreen()
            }
            composable<Route.Connection> {
                ConnectionScreen(
                    connectionState = connectionState,
                    lastConnectedDevice = bluetoothHidViewModel.lastConnectedDevice,
                    pairedDevices = bluetoothHidViewModel.pairedDevices,
                    availableDevices = bluetoothHidViewModel.availableDevices,
                    onConnectDevice = { device -> bluetoothHidViewModel.connect(device) },
                    disposableEffect = {
                        bluetoothHidViewModel.startScanning()
                        onDispose { bluetoothHidViewModel.stopScanning() }
                    }
                )
            }
        }
    }
}

@Composable
private fun HandleUiEvent() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        UiEventManager.events.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun RegisterHidApp(bluetoothHidViewModel: BluetoothHidViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    Log.d("HID", "应用切到后台")
                    bluetoothHidViewModel.connectedDevice?.let { bluetoothHidViewModel.disconnect(it) }
                    bluetoothHidViewModel.unregisterHidApp()
                }

                Lifecycle.Event.ON_RESUME -> {
                    Log.d("HID", "应用回到前台")
                    bluetoothHidViewModel.registerHidApp {
                        // 实现自动续连
                        bluetoothHidViewModel.lastConnectedDevice?.let { bluetoothHidViewModel.connect(it) }
                    }
                }

                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun RegisterReceiver(bluetoothHidViewModel: BluetoothHidViewModel) {
    val activity = LocalActivity.current
    DisposableEffect(Unit) {
        require(activity != null)
        bluetoothHidViewModel.registerReceiver(activity)
        onDispose {
            bluetoothHidViewModel.unregisterReceiver(activity)
        }
    }
}

private fun keypadLayout(
    bluetoothHidViewModel: BluetoothHidViewModel,
    @IntRange(0, 5) tabIndex: Int,
    onChangeTabIndex: (Int) -> Unit
): KeypadLayout {
    val prefixCode = 64 + tabIndex

    open class ShowKeyDescriptor(val text: String, val scanCode: Int) : KeyDescriptor {
        override val content: @Composable (() -> Unit)
            get() = { Text(text = text, fontSize = 12.sp, maxLines = 1) }

        @SuppressLint("MissingPermission")
        override fun pressDown() {
            bluetoothHidViewModel.onKeyDown(prefixCode)
            bluetoothHidViewModel.onKeyDown(scanCode)
        }

        @SuppressLint("MissingPermission")
        override fun pressUp() {
            bluetoothHidViewModel.onKeyUp(prefixCode)
            bluetoothHidViewModel.onKeyUp(scanCode)
        }
    }

    return object : KeypadLayout {
        override fun number(@IntRange(0, 9) number: Int): KeyDescriptor {
            val scanCode = when (number) {
                0 -> 0x62
                in 1..9 -> 0x58 + number
                else -> 0
            }
            return ShowKeyDescriptor(number.toString(), scanCode)
        }

        override val lock: KeyDescriptor
            get() = object : ShowKeyDescriptor("Lock", 0x29) { // Esc
                override fun pressDown() {
                    super.pressDown()
                    onChangeTabIndex(0) // reset to first tab
                }
            }

        override val add: KeyDescriptor
            get() = object : ShowKeyDescriptor("+", 0x57) {
                override fun pressDown() {
                    super.pressDown()
                    onChangeTabIndex((tabIndex + 1) % 6) // select next tab
                }
            }

        override val divide: KeyDescriptor get() = ShowKeyDescriptor("/", 0x54)
        override val multiply: KeyDescriptor get() = ShowKeyDescriptor("*", 0x55)
        override val subtract: KeyDescriptor get() = ShowKeyDescriptor("-", 0x56)
        override val enter: KeyDescriptor get() = ShowKeyDescriptor("Enter", 0x58)
        override val decimal: KeyDescriptor get() = ShowKeyDescriptor(".", 0x63)
    }
}

private fun keypadLayouts(bluetoothHidViewModel: BluetoothHidViewModel, onChangeTabIndex: (Int) -> Unit) = listOf(
    Pair("1", keypadLayout(bluetoothHidViewModel, 0, onChangeTabIndex)),
    Pair("2", keypadLayout(bluetoothHidViewModel, 1, onChangeTabIndex)),
    Pair("3", keypadLayout(bluetoothHidViewModel, 2, onChangeTabIndex)),
    Pair("4", keypadLayout(bluetoothHidViewModel, 3, onChangeTabIndex)),
    Pair("5", keypadLayout(bluetoothHidViewModel, 4, onChangeTabIndex)),
    Pair("6", keypadLayout(bluetoothHidViewModel, 5, onChangeTabIndex))
)
