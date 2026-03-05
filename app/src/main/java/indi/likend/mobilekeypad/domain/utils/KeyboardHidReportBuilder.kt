package indi.likend.mobilekeypad.domain.utils

class KeyboardHidReportBuilder {
    private val pressedKeys = LinkedHashSet<Int>(6)

    fun build() = ByteArray(8).apply {
        synchronized(pressedKeys) {
            require(pressedKeys.size <= 6)
            pressedKeys.forEachIndexed { index, keyCode ->
                this[index + 2] = keyCode.toByte()
            }
        }
    }

    fun onKeyDown(scanCode: Int) {
        synchronized(pressedKeys) {
            pressedKeys.remove(scanCode)
            if (pressedKeys.size == 6) {
                val oldestKey = pressedKeys.iterator()
                oldestKey.next()
                oldestKey.remove()
            }
            require(pressedKeys.size < 6)
            pressedKeys.add(scanCode)
        }
    }

    fun onKeyUp(scanCode: Int) {
        synchronized(pressedKeys) {
            pressedKeys.remove(scanCode)
        }
    }
}