package indi.likend.mobilekeypad.data.model

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import indi.likend.mobilekeypad.data.utils.bluetooth.device
import indi.likend.mobilekeypad.domain.model.BluetoothDevice
import indi.likend.mobilekeypad.domain.model.BluetoothScanSession
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Android 平台上的 [BluetoothScanSession] 实现。
 *
 * 该类通过注册 [android.bluetooth.BluetoothDevice.ACTION_FOUND] 广播接收器，
 * 监听蓝牙设备发现事件，并将发现的设备以 [StateFlow] 形式对外暴露。
 * 扫描操作在 [devices] 流被收集时自动启动。。
 *
 * **需要权限**：
 * - `BLUETOOTH_SCAN`（Android 12+）或 `BLUETOOTH` + `ACCESS_FINE_LOCATION`（旧版本）
 *
 * 调用 [close] 方法可停止扫描并释放资源。
 *
 * @property context 应用程序级 Context，用于注册广播和获取系统服务
 * @property coroutineScope 用于创建内部协程作用域的父级作用域
 * @property adapter BluetoothAdapter 实例，用于执行实际的扫描操作
 */
@SuppressLint("MissingPermission")
class AndroidBluetoothScanSession @Inject constructor(
    @param:ApplicationContext private val context: Context,
    coroutineScope: CoroutineScope,
    private val adapter: BluetoothAdapter
) : BluetoothScanSession {
    /**
     * 用于控制扫描生命周期的 [SupervisorJob]。
     * 当需要停止扫描时，通过取消此 Job 来触发所有相关协程的取消。
     */
    private val sessionJob = SupervisorJob()

    /**
     * 专用于该扫描会话的 [CoroutineScope]。
     * 它继承自传入的 [coroutineScope] 上下文，并添加了 [sessionJob] 作为父 `Job`，
     * 以便统一管理该会话中的所有协程。
     */
    private val sessionScope = CoroutineScope(coroutineScope.coroutineContext + sessionJob)

    /**
     * 提供已发现蓝牙设备列表的 [StateFlow]。
     *
     * 内部通过 [callbackFlow] 构建一个基于广播接收器的数据流：
     * - 注册 `ACTION_FOUND` 广播接收器。
     * - 调用 [BluetoothAdapter.startDiscovery] 开始扫描。
     * - 每当有新设备被发现时，将其添加到 [LinkedHashSet] 中（自动去重），
     *   并通过 [kotlinx.coroutines.channels.SendChannel.trySend] 发射最新的设备列表。
     * - 当流被取消（例如 [sessionJob] 被取消）时，通过 [awaitClose] 回调
     *   停止扫描并注销广播接收器。
     *
     * 扫描会在 [AndroidBluetoothScanSession] 实例创建时自动启动。
     */
    override val devices: StateFlow<List<BluetoothDevice>> = callbackFlow {
        val foundedDevicesSet = LinkedHashSet<AndroidBluetoothDevice>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                require(intent.action == ACTION)
                val device = intent.device
                foundedDevicesSet.add(AndroidBluetoothDevice(device))
                trySend(foundedDevicesSet.toList())
                Log.d(TAG, "receive device $device")
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(ACTION), ContextCompat.RECEIVER_EXPORTED)
        val result = adapter.startDiscovery()
        Log.d(TAG, "adapter.startDiscovery() $result adapter state: ${adapter.state}")

        awaitClose {
            val result = adapter.cancelDiscovery()
            Log.d(TAG, "adapter.cancelDiscovery() $result")
            context.unregisterReceiver(receiver)
        }
    }.stateIn(
        scope = sessionScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    /**
     * 关闭当前扫描会话，停止蓝牙发现并释放资源。
     *
     * 该方法的实现通过取消 [sessionJob] 来触发 [devices] 流内部的 [awaitClose] 代码块，
     * 从而自动调用 [BluetoothAdapter.cancelDiscovery] 并注销广播接收器。
     */
    override fun close() {
        sessionJob.cancel()
    }

    companion object {
        private const val TAG = "AndroidBluetoothScanSession"
        private const val ACTION = android.bluetooth.BluetoothDevice.ACTION_FOUND
    }
}
