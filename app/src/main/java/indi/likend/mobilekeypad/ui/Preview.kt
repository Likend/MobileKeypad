package indi.likend.mobilekeypad.ui

import android.bluetooth.BluetoothClass
import androidx.annotation.IntRange
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import indi.likend.mobilekeypad.model.BluetoothDeviceState
import indi.likend.mobilekeypad.model.KeyDescriptor
import indi.likend.mobilekeypad.model.KeypadLayout

private fun previewKeyDescriptor(text: String): KeyDescriptor = object : KeyDescriptor {
    override val content: @Composable (() -> Unit)
        get() = { Text(text = text, fontSize = 12.sp, maxLines = 1) }
}

val previewKeypadLayout = object : KeypadLayout {
    override fun number(@IntRange(0, 9) number: Int): KeyDescriptor = previewKeyDescriptor(number.toString())

    override val lock: KeyDescriptor get() = previewKeyDescriptor("Lock")
    override val divide: KeyDescriptor get() = previewKeyDescriptor("/")
    override val multiply: KeyDescriptor get() = previewKeyDescriptor("*")
    override val subtract: KeyDescriptor get() = previewKeyDescriptor("-")
    override val add: KeyDescriptor get() = previewKeyDescriptor("+")
    override val decimal: KeyDescriptor get() = previewKeyDescriptor(".")
    override val enter: KeyDescriptor get() = previewKeyDescriptor("Enter")
}

val previewKeypadLayouts = listOf(
    Pair("1", previewKeypadLayout),
    Pair("2", previewKeypadLayout),
    Pair("3", previewKeypadLayout),
    Pair("4", previewKeypadLayout),
    Pair("5", previewKeypadLayout)
)

private fun createMockBluetoothClass(classInt: Int): BluetoothClass {
    val constructor = BluetoothClass::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType)
    constructor.isAccessible = true
    return constructor.newInstance(classInt) as BluetoothClass
}

val previewDevice =
    BluetoothDeviceState(name = "LAPTOP-A12BC3D4", address = "AA:BB:CC:DD:EE:FF", createMockBluetoothClass(0x010C))

@Repeatable // 允许在同一个地方多次使用（虽然通常不需要）
@Preview(
    name = "1. 标准竖屏 (Standard)",
    group = "Responsive",
    widthDp = 360,
    heightDp = 800,
    showBackground = true
)
@Preview(
    name = "2. 高度受限 (Short)",
    group = "Responsive",
    widthDp = 360,
    heightDp = 450, // 测试 gridSizeH 触发缩放
    showBackground = true
)
@Preview(
    name = "3. 横屏模式 (Landscape)",
    group = "Responsive",
    widthDp = 800,
    heightDp = 400, // 测试水平居中逻辑
    showBackground = true
)
annotation class DeviceResponsivePreviews
