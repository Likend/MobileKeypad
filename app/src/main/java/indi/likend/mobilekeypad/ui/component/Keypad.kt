package indi.likend.mobilekeypad.ui.component

import android.R.attr.maxHeight
import android.R.attr.maxWidth
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import indi.likend.mobilekeypad.model.KeyDescriptor
import indi.likend.mobilekeypad.model.KeypadLayout
import indi.likend.mobilekeypad.ui.previewKeypadLayout

/**
 * 小键盘布局示意图：
 * ```
 * ┌───┬───┬───┬───┐
 * │Loc│ / │ * │ - │
 * ├───┼───┼───┼───┤
 * │ 7 │ 8 │ 9 │   │
 * ├───┼───┼───┤ + │
 * │ 4 │ 5 │ 6 │   │
 * ├───┼───┼───┼───┤
 * │ 1 │ 2 │ 3 │   │
 * ├───┴───┼───┤Ent│
 * │   0   │ . │   │
 * └───────┴───┴───┘
 * ```
 */
@Composable
fun Keypad(layout: KeypadLayout = previewKeypadLayout, gridSize: Dp = 60.dp, spacing: Dp = 8.dp) {
    val doubleGridSize = (gridSize * 2) + spacing

    val baseKeyModifier = Modifier.size(gridSize)
    val wideKeyModifier = Modifier
        .width(doubleGridSize)
        .height(gridSize)
    val tallKeyModifier = Modifier
        .width(gridSize)
        .height(doubleGridSize)
    val arrangement = Arrangement.spacedBy(spacing)

    Row(horizontalArrangement = arrangement) {
        Column(verticalArrangement = arrangement) {
            Row(horizontalArrangement = arrangement) {
                KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.lock)
                KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.divide)
                KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.multiply)
            }
            Row(horizontalArrangement = arrangement) {
                KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.number(7))
                KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.number(8))
                KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.number(9))
            }
            Row(horizontalArrangement = arrangement) {
                KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.number(4))
                KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.number(5))
                KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.number(6))
            }
            Row(horizontalArrangement = arrangement) {
                KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.number(1))
                KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.number(2))
                KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.number(3))
            }
            Row(horizontalArrangement = arrangement) {
                KeypadButton(modifier = wideKeyModifier, keyDescriptor = layout.number(0))
                KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.decimal)
            }
        }
        Column(verticalArrangement = arrangement) {
            KeypadButton(modifier = baseKeyModifier, keyDescriptor = layout.subtract)
            KeypadButton(modifier = tallKeyModifier, keyDescriptor = layout.add)
            KeypadButton(modifier = tallKeyModifier, keyDescriptor = layout.enter)
        }
    }
}

@Composable
private fun KeypadButton(modifier: Modifier = Modifier, keyDescriptor: KeyDescriptor) {
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource, keyDescriptor) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                // Press down
                is PressInteraction.Press -> keyDescriptor.pressDown()

                // Press up
                is PressInteraction.Release,
                is PressInteraction.Cancel -> keyDescriptor.pressUp()
            }
        }
    }

    ElevatedButton(
        onClick = {}, // 留空即可，已在 interactionSource 中处理
        interactionSource = interactionSource,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        contentPadding = PaddingValues(0.dp)
    ) {
        keyDescriptor.content()
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewKeypad() {
    Keypad(layout = previewKeypadLayout)
}
