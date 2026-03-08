package indi.likend.mobilekeypad.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.IntRange
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import indi.likend.mobilekeypad.domain.model.DarkTheme
import indi.likend.mobilekeypad.ui.screen.ConnectionScreen
import indi.likend.mobilekeypad.ui.screen.HomeScreen
import indi.likend.mobilekeypad.ui.screen.settings.MainSettingScreen
import indi.likend.mobilekeypad.ui.screen.settings.SettingsViewModel
import indi.likend.mobilekeypad.ui.theme.MobileKeypadTheme

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MobileKeypadApp() {
    val viewModel: MobileKeypadAppViewModel = hiltViewModel()

    val navController = rememberNavController()

    val permissionsState = rememberMultiplePermissionsState(MobileKeypadAppViewModel.permissionsToRequest) { result ->
        if (result.map { it.value }.all { it }) {
            navController.navigate(Route.Connection)
        }
    }
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        viewModel.hasPermission.value = permissionsState.allPermissionsGranted
    }

    val enableBluetoothLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                navController.navigate(Route.Connection)
            }
        }

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val lastConnectedDevice by viewModel.lastConnectedDevice.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()

    HandleUiEvent()

    val settings: SettingsViewModel = hiltViewModel()
    val darkTheme = when (settings.darkTheme.value) {
        DarkTheme.On -> true
        DarkTheme.Off -> false
        DarkTheme.FollowSystem -> isSystemInDarkTheme()
    }
    val pureBlack by settings.pureBlack
    val enableDynamicTheme by settings.enableDynamicTheme

    MobileKeypadTheme(darkTheme = darkTheme, dynamicColor = enableDynamicTheme, pureBlack = pureBlack) {
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
                    viewModel.onKeyDown(prefixCode)
                    viewModel.onKeyUp(prefixCode)
                }

                HomeScreen(
                    navController = navController,
                    connectionState = connectionState,
                    onClickConnectionButton = {
                        when (connectionState) {
                            is ConnectionState.PermissionRequire -> permissionsState.launchMultiplePermissionRequest()

                            is ConnectionState.BluetoothUnsupported -> Unit

                            is ConnectionState.BluetoothTurnedOff -> enableBluetoothLauncher.launch(
                                Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                            )

                            is ConnectionState.Valid -> navController.navigate(Route.Connection)
                        }
                    },
                    selectedTab = selectedTab,
                    onSelectedTab = onChangeSelectedTab,
                    keypadLayouts = keypadLayouts(
                        onKeyDown = viewModel::onKeyDown,
                        onKeyUp = viewModel::onKeyUp,
                        onChangeTabIndex = onChangeSelectedTab
                    )
                )
            }
            composable<Route.Settings> {
                MainSettingScreen()
            }
            composable<Route.Connection> {
                val state = connectionState
                if (state is ConnectionState.Valid) {
                    ConnectionScreen(
                        connectionState = state,
                        lastConnectedDevice = lastConnectedDevice,
                        pairedDevices = pairedDevices,
                        availableDevices = availableDevices,
                        onConnectDevice = { device -> viewModel.connect(device) },
                        disposableEffect = {
                            viewModel.startScanning()
                            onDispose { viewModel.stopScanning() }
                        }
                    )
                } else { // 可以在这里处理错误状态，或者自动返回上一页
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
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

private fun keypadLayout(
    onKeyDown: (Int) -> Unit,
    onKeyUp: (Int) -> Unit,
    @IntRange(0, 5) tabIndex: Int,
    onChangeTabIndex: (Int) -> Unit
): KeypadLayout {
    val prefixCode = 64 + tabIndex

    open class ShowKeyDescriptor(val text: String, val scanCode: Int) : KeyDescriptor {
        override val content: @Composable (() -> Unit)
            get() = { Text(text = text, fontSize = 12.sp, maxLines = 1) }

        @SuppressLint("MissingPermission")
        override fun pressDown() {
            onKeyDown(prefixCode)
            onKeyDown(scanCode)
        }

        @SuppressLint("MissingPermission")
        override fun pressUp() {
            onKeyUp(prefixCode)
            onKeyUp(scanCode)
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

private fun keypadLayouts(onKeyDown: (Int) -> Unit, onKeyUp: (Int) -> Unit, onChangeTabIndex: (Int) -> Unit) = listOf(
    Pair("1", keypadLayout(onKeyDown, onKeyUp, 0, onChangeTabIndex)),
    Pair("2", keypadLayout(onKeyDown, onKeyUp, 1, onChangeTabIndex)),
    Pair("3", keypadLayout(onKeyDown, onKeyUp, 2, onChangeTabIndex)),
    Pair("4", keypadLayout(onKeyDown, onKeyUp, 3, onChangeTabIndex)),
    Pair("5", keypadLayout(onKeyDown, onKeyUp, 4, onChangeTabIndex)),
    Pair("6", keypadLayout(onKeyDown, onKeyUp, 5, onChangeTabIndex))
)
