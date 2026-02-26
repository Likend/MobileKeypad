package indi.likend.mobilekeypad.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SuccessGreenLight = Color(0xFFE8F5E9) // 极浅绿
private val OnSuccessGreenLight = Color(0xFF00210B) // 深绿文字
private val SuccessGreenDark = Color(0xFF00210B) // 极深绿背景
private val OnSuccessGreenDark = Color(0xFFE8F5E9) // 亮绿文字

private val WarningYellowLight = Color(0xFFFFF3E0) // 极浅黄
private val OnWarningYellowLight = Color(0xFF291800) // 深橙文字
private val WarningYellowDark = Color(0xFF29201D) // 极深橙背景
private val OnWarningYellowDark = Color(0xFFFFF3E0) // 亮橙文字

object CostumeColorScheme {
    val successContainer: Color
        @Composable get() = if (isSystemInDarkTheme()) SuccessGreenDark else SuccessGreenLight
    val onSuccessContainer: Color
        @Composable get() = if (isSystemInDarkTheme()) OnSuccessGreenDark else OnSuccessGreenLight
    val warningContainer: Color
        @Composable get() = if (isSystemInDarkTheme()) WarningYellowDark else WarningYellowLight
    val onWarningContainer: Color
        @Composable get() = if (isSystemInDarkTheme()) OnWarningYellowDark else OnWarningYellowLight
}
