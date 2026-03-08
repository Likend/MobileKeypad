package indi.likend.mobilekeypad.ui.screen.settings

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import indi.likend.mobilekeypad.R
import indi.likend.mobilekeypad.domain.model.DarkTheme
import indi.likend.mobilekeypad.ui.component.CostumeScaffold
import indi.likend.mobilekeypad.ui.component.SessionDivider
import indi.likend.mobilekeypad.ui.component.SessionTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingScreen() {
    CostumeScaffold(title = stringResource(R.string.settings_heading)) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(state = rememberScrollState())
        ) {
            // --- 主题模式部分 ---
            AppearanceSettings()
            SessionDivider()
        }
    }
}

@Composable
private fun AppearanceSettings() {
    val settings: SettingsViewModel = hiltViewModel()
    var darkTheme by settings.darkTheme
    var pureBlack by settings.pureBlack
    var enableDynamicTheme by settings.enableDynamicTheme

    SessionTitle("主题模式")

    Box {
        var menuExpanded by remember(darkTheme) { mutableStateOf(false) }

        ListItem(
            headlineContent = { Text("暗色模式") },
            supportingContent = { Text(darkTheme.label) },
            modifier = Modifier.clickable { menuExpanded = !menuExpanded }
        )

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            offset = DpOffset(x = 16.dp, y = 0.dp)
        ) {
            DarkTheme.entries.forEach {
                DropdownMenuItem(
                    text = { Text(text = it.label) },
                    onClick = { darkTheme = it },
                    leadingIcon = {
                        if (it == darkTheme) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        }
    }

    ListItem(
        headlineContent = { Text("纯黑") },
        supportingContent = { Text("启用 AMOLED 主题颜色") },
        trailingContent = {
            Switch(
                checked = pureBlack,
                onCheckedChange = { pureBlack = it }
            )
        },
        modifier = Modifier.clickable { pureBlack = !pureBlack }
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ListItem(
            headlineContent = { Text("动态颜色") },
            supportingContent = { Text("启用系统动态颜色支持") },
            trailingContent = {
                Switch(
                    checked = enableDynamicTheme,
                    onCheckedChange = { enableDynamicTheme = it }
                )
            },
            modifier = Modifier.clickable { enableDynamicTheme = !enableDynamicTheme }
        )
    }
}

private val DarkTheme.label
    get() = when (this) {
        DarkTheme.On -> "开"
        DarkTheme.Off -> "关"
        DarkTheme.FollowSystem -> "跟随系统"
    }
