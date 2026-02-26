package indi.likend.mobilekeypad.ui.screen.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import indi.likend.mobilekeypad.R
import indi.likend.mobilekeypad.ui.component.CostumeScaffold

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingScreen() {
    CostumeScaffold(title = stringResource(R.string.settings_heading)) {
    }
}
