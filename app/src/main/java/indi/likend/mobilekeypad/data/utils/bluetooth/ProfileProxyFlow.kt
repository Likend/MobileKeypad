package indi.likend.mobilekeypad.data.utils.bluetooth

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothCsipSetCoordinator
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothHearingAid
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothLeAudio
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import kotlin.reflect.KClass
import kotlin.reflect.cast
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * 对 [BluetoothAdapter.getProfileProxy] 和 [BluetoothAdapter.closeProfileProxy] 的封装，
 * 使用 Kotlin [Flow] 实现异步获取蓝牙配置文件代理对象。
 *
 * @param BP 蓝牙配置文件类型，必须是 [BluetoothProfile] 的子类型
 * @param context Android 上下文
 * @return 返回一个 [Flow]，发射获取到的代理对象，连接断开时发射 `null` 并关闭流
 * @throws IllegalArgumentException 当传入的配置文件类型不被支持时抛出
 * @throws IllegalStateException 当无法启动代理获取时抛出
 */
inline fun <reified BP : BluetoothProfile> BluetoothAdapter.getProfileProxyFlow(context: Context): Flow<BP?> =
    getProfileProxyFlow(
        context = context,
        profileClass = BP::class
    )

/**
 * 蓝牙配置文件类与其对应整型常量的映射表。
 * 根据 Android API 级别动态添加支持的配置文件类型。
 *
 * @see [BluetoothAdapter.getProfileProxy]
 */
private val profileIntMap = buildMap {
    put(BluetoothHeadset::class, BluetoothProfile.HEADSET)
    put(BluetoothA2dp::class, BluetoothProfile.A2DP)

    put(BluetoothHidDevice::class, BluetoothProfile.HID_DEVICE) // API 28

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // API 29
        put(BluetoothHearingAid::class, BluetoothProfile.HEARING_AID)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33
        put(BluetoothLeAudio::class, BluetoothProfile.LE_AUDIO)
        put(BluetoothCsipSetCoordinator::class, BluetoothProfile.CSIP_SET_COORDINATOR)
    }
}

/**
 * 获取指定蓝牙配置文件的代理对象流。
 *
 * @param BP 蓝牙配置文件类型，必须是 [BluetoothProfile] 的子类型
 * @param context Android 上下文
 * @param profileClass 蓝牙配置文件类的 [KClass] 对象
 * @return 返回一个 [Flow]，发射获取到的代理对象，连接断开时发射 `null` 并关闭流
 */
fun <BP : BluetoothProfile> BluetoothAdapter.getProfileProxyFlow(
    context: Context,
    profileClass: KClass<BP>
): Flow<BP?> {
    // 从映射表中查找配置文件对应的整型常量
    val profileInt = profileIntMap[profileClass]
        ?: throw IllegalArgumentException("Unsupported Bluetooth profile: ${profileClass.simpleName}")

    return callbackFlow {
        var proxyRef: BluetoothProfile? = null // 保存代理对象的引用，用于后续清理

        // 创建服务连接监听器
        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == profileInt) {
                    proxyRef = proxy
                    trySend(profileClass.cast(proxy)) // 将代理对象发送到流中
                }
            }

            override fun onServiceDisconnected(profile: Int) {
                if (profile == profileInt) {
                    proxyRef = null
                    trySend(null) // 发送 null 表示连接断开
                    close() // 关闭流
                }
            }
        }

        // 尝试启动获取代理对象
        val started = getProfileProxy(context, listener, profileInt)
        if (!started) {
            // 如果启动失败，立即关闭流并抛出异常
            close(IllegalStateException("Failed to start profile proxy"))
            return@callbackFlow
        }

        // 注册流关闭时的清理回调
        awaitClose {
            // 如果代理对象存在，则关闭它
            proxyRef?.let {
                closeProfileProxy(profileInt, it)
            }
        }
    }
}
