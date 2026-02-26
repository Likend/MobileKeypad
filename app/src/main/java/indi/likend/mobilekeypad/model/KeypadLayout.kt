package indi.likend.mobilekeypad.model

import androidx.annotation.IntRange
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

/**
 * Number pad layout interface
 *
 * Defines access methods for each key on the number pad, including number keys,
 * operator keys, and function keys. All keys are described via [KeyDescriptor]
 * to represent specific key behaviors.
 */
interface KeypadLayout {

    /**
     * Gets a number key on the number pad
     *
     * @param number The value of the number key, ranging from 0 to 9 (inclusive)
     * @return The [KeyDescriptor] corresponding to the number key
     *
     * Example:
     * number(5) represents the "5" key on the number pad
     */
    fun number(@IntRange(0, 9) number: Int): KeyDescriptor

    /**
     * The number lock key on the Number Pad
     */
    val lock: KeyDescriptor

    /**
     * The division operator (/) key on the number pad.
     */
    val divide: KeyDescriptor

    /**
     * The multiplication operator (*) key on the number pad.
     */
    val multiply: KeyDescriptor

    /**
     * The subtraction operator (-) key on the number pad.
     */
    val subtract: KeyDescriptor

    /**
     * The addition operator (+) key on the number pad.
     */
    val add: KeyDescriptor

    /**
     * The decimal point (.) key on the number pad.
     */
    val decimal: KeyDescriptor

    /**
     * The enter key on the number pad.
     */
    val enter: KeyDescriptor
}
