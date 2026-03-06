package indi.likend.mobilekeypad.ui

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import indi.likend.mobilekeypad.R
import indi.likend.mobilekeypad.domain.model.BluetoothConnectionState
import indi.likend.mobilekeypad.domain.model.BluetoothDevice
import indi.likend.mobilekeypad.domain.model.BluetoothScanSession
import indi.likend.mobilekeypad.domain.repository.BluetoothRepository
import indi.likend.mobilekeypad.utils.combineStates
import indi.likend.mobilekeypad.utils.mapState
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MobileKeypadAppViewModel @Inject constructor(
    application: Application,
    private val repository: BluetoothRepository
) : AndroidViewModel(application) {
    val pairedDevices = repository.pairedDevices

    val lastConnectedDevice: StateFlow<BluetoothDevice?> = repository.lastSession.mapState { it?.device }

    val hasPermissionStateFlow = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val internalConnectionStateFlow: StateFlow<ConnectionState.Valid> =
        repository.lastSession.flatMapLatest { it?.state ?: flowOf(BluetoothConnectionState.DISCONNECTED) }.map {
            when (it) {
                BluetoothConnectionState.CONNECTED -> ConnectionState.Connected(lastConnectedDevice.value!!)
                BluetoothConnectionState.CONNECTING -> ConnectionState.Connecting
                BluetoothConnectionState.DISCONNECTED -> ConnectionState.Unconnected
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ConnectionState.Unconnected
        )

    val connectionState: StateFlow<ConnectionState> = combineStates(
        hasPermissionStateFlow,
        repository.isBluetoothEnable,
        internalConnectionStateFlow
    ) { hasPermission, isBluetoothEnabled, internalConnectionState ->
        when {
            !hasPermission -> ConnectionState.PermissionRequire
            !isBluetoothEnabled -> ConnectionState.BluetoothTurnedOff
            else -> internalConnectionState
        }
    }

    init {
        viewModelScope.launch {
            connectionState.collect {
                when (it) {
                    is ConnectionState.BluetoothTurnedOff -> Unit

                    is ConnectionState.BluetoothUnsupported -> Unit

                    is ConnectionState.PermissionRequire -> Unit

                    is ConnectionState.Connected -> UiEventManager.postToast(
                        application.resources.getString(R.string.connection_connected_to_device, it.device.name)
                    )

                    is ConnectionState.Connecting -> Unit

                    is ConnectionState.Unconnected -> Unit
                }
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        viewModelScope.launch {
            repository.connect(device)
        }
    }

    fun disconnect() {
        repository.lastSession.value?.close()
    }

    fun onKeyDown(scanCode: Int) {
        repository.lastSession.value?.onKeyDown(scanCode)
    }

    fun onKeyUp(scanCode: Int) {
        repository.lastSession.value?.onKeyUp(scanCode)
    }

    private var lastScanSession = MutableStateFlow<BluetoothScanSession?>(null)

    fun startScanning() {
        lastScanSession.value = repository.startScan()
    }

    fun stopScanning() {
        lastScanSession.value?.close()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val availableDevices: StateFlow<List<BluetoothDevice>> =
        lastScanSession.flatMapLatest { it?.devices ?: flowOf(emptyList()) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    companion object {
        val permissionsToRequest = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ 需要扫描和连接权限
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                // Android 11 及以下需要位置权限才能扫描蓝牙
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }
}

sealed interface ConnectionState {
    sealed interface Valid : ConnectionState
    sealed interface Invalid : ConnectionState

    data class Connected(val device: BluetoothDevice) : Valid
    object Connecting : Valid
    object Unconnected : Valid
    object PermissionRequire : Invalid
    object BluetoothUnsupported : Invalid
    object BluetoothTurnedOff : Invalid
}
