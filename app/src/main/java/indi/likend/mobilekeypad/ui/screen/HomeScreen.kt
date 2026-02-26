package indi.likend.mobilekeypad.ui.screen

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFrom
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import indi.likend.mobilekeypad.R
import indi.likend.mobilekeypad.model.ConnectionState
import indi.likend.mobilekeypad.model.KeypadLayout
import indi.likend.mobilekeypad.ui.DeviceResponsivePreviews
import indi.likend.mobilekeypad.ui.Route
import indi.likend.mobilekeypad.ui.component.Keypad
import indi.likend.mobilekeypad.ui.previewDevice
import indi.likend.mobilekeypad.ui.previewKeypadLayouts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavHostController,
    connectionState: ConnectionState = ConnectionState.Unconnected,
    onClickConnectionButton: () -> Unit = {},
    keypadLayouts: List<Pair<String, KeypadLayout>>,
    selectedTab: Int = 0,
    onSelectedTab: (Int) -> Unit = {}
) {
    val activity = LocalActivity.current
    DisposableEffect(Unit) {
        activity?.let { activity ->
            val previousOrientation = activity.requestedOrientation
            @SuppressLint("SourceLockedOrientationActivity")
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            onDispose { activity.requestedOrientation = previousOrientation }
        } ?: onDispose { }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val spacing = 8.dp
        val tabRowAreaHeight = 64.dp
        val gridSizeW = (maxWidth - (spacing * 3) - 32.dp) / 4
        val gridSizeH =
            (maxHeight - (spacing * 4) - tabRowAreaHeight - TopAppBarDefaults.LargeAppBarCollapsedHeight) / 5
        val gridSize = minOf(gridSizeW, gridSizeH)

        val keypadActualWidth = gridSize * 4 + spacing * 3
        val keypadActualHeight = gridSize * 5 + spacing * 4

        val showTitle =
            maxHeight >= keypadActualHeight + tabRowAreaHeight + TopAppBarDefaults.LargeAppBarExpandedHeight

        Scaffold(topBar = {
            HomeScreenTopBar(
                navController = navController,
                connectionState = connectionState,
                onClickConnectionButton = onClickConnectionButton,
                showTitle = showTitle
            )
        }) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (showTitle) Arrangement.Top else Arrangement.Bottom
            ) {
                Column(
                    modifier = Modifier.width(keypadActualWidth)
                ) {
                    SecondaryTabRow(selectedTabIndex = selectedTab, tabs = {
                        keypadLayouts.map { it.first }.forEachIndexed { index, title ->
                            Tab(selected = selectedTab == index, onClick = { onSelectedTab(index) }) {
                                Text(title, modifier = Modifier.padding(vertical = 12.dp))
                            }
                        }
                    })
                    Spacer(modifier = Modifier.height(16.dp))
                    Keypad(layout = keypadLayouts[selectedTab].second, gridSize = gridSize, spacing = spacing)
                }
            }
        }
    }
}

@Composable
private fun HomeScreenTopBar(
    navController: NavHostController,
    connectionState: ConnectionState = ConnectionState.Unconnected,
    onClickConnectionButton: () -> Unit = {},
    showTitle: Boolean = true
) {
    Column(
        modifier = Modifier
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopAppBarDefaults.LargeAppBarCollapsedHeight)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Button(
                    onClick = onClickConnectionButton,
                    colors = connectionState.buttonColors,
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .height(40.dp)
                ) {
                    Icon(connectionState.iconVector, null, Modifier.size(18.dp))
                    Text(
                        text = connectionState.text,
                        maxLines = 1,
                        overflow = TextOverflow.MiddleEllipsis,
                        // fill = false 确保短文本时按钮收缩，长文本时才占据 Box 全宽
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .padding(start = 8.dp)
                    )
                }
            }

            // 设置图标
            IconButton(onClick = { navController.navigate(Route.Settings) }) {
                Icon(Icons.Filled.Settings, stringResource(R.string.settings_heading))
            }
        }

        if (showTitle) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TopAppBarDefaults.LargeAppBarExpandedHeight - TopAppBarDefaults.LargeAppBarCollapsedHeight),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(
                    text = "Mobile Keypad",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .paddingFrom(LastBaseline, after = 28.dp)
                )
            }
        }
    }
}

@Composable
private fun PreviewHomeScreenTemplate(connectionState: ConnectionState) {
    HomeScreen(
        navController = rememberNavController(),
        connectionState = connectionState,
        keypadLayouts = previewKeypadLayouts
    )
}

@Composable
@DeviceResponsivePreviews
private fun PreviewHomeScreenUnconnected() {
    PreviewHomeScreenTemplate(ConnectionState.Unconnected)
}

@Composable
@DeviceResponsivePreviews
private fun PreviewHomeScreenConnected() {
    PreviewHomeScreenTemplate(ConnectionState.Connected(previewDevice))
}

@Composable
@DeviceResponsivePreviews
private fun PreviewHomeScreenPermissionRequire() {
    PreviewHomeScreenTemplate(ConnectionState.PermissionRequire)
}
