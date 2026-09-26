package top.yogiczy.mytv.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import top.yogiczy.mytv.core.data.entities.iptvsource.IptvSource.Companion.needExternalStoragePermission
import top.yogiczy.mytv.tv.ui.material.Padding
import top.yogiczy.mytv.tv.ui.material.PopupHandleableApplication
import top.yogiczy.mytv.tv.ui.material.SnackbarUI
import top.yogiczy.mytv.tv.ui.material.Visibility
import top.yogiczy.mytv.tv.ui.screen.main.MainScreen
import top.yogiczy.mytv.tv.ui.screen.monitor.MonitorPopup
import top.yogiczy.mytv.tv.ui.screen.settings.SettingsViewModel
import top.yogiczy.mytv.tv.ui.screen.settings.settingsVM
import top.yogiczy.mytv.tv.ui.theme.DESIGN_WIDTH
import top.yogiczy.mytv.tv.ui.theme.SAFE_AREA_HORIZONTAL_PADDING
import top.yogiczy.mytv.tv.ui.theme.SAFE_AREA_VERTICAL_PADDING
import top.yogiczy.mytv.tv.ui.tooling.PreviewWithLayoutGrids
import top.yogiczy.mytv.tv.ui.utils.rememberReadExternalStoragePermission

@Composable
fun App(
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = settingsVM,
    onBackPressed: () -> Unit = {},
) {
    val configuration = LocalConfiguration.current

    BackHandler(onBack = onBackPressed)

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = LocalDensity.current.density * when (settingsViewModel.uiDensityScaleRatio) {
                0f -> configuration.screenWidthDp.toFloat() / DESIGN_WIDTH
                else -> settingsViewModel.uiDensityScaleRatio
            },
            fontScale = LocalDensity.current.fontScale * settingsViewModel.uiFontScaleRatio,
        ),
    ) {
        PopupHandleableApplication {
            MainScreen(
                modifier = modifier,
                onBackPressed = onBackPressed,
            )
        }

        SnackbarUI()
        Visibility({ settingsViewModel.debugShowFps }) { MonitorPopup() }
        Visibility({ settingsViewModel.debugShowLayoutGrids }) { PreviewWithLayoutGrids { } }
    }

    if (settingsViewModel.iptvSourceCurrent.needExternalStoragePermission()) {
        val (hasPermission, requestPermission) = rememberReadExternalStoragePermission()
        LaunchedEffect(Unit) { if (!hasPermission) requestPermission() }
    }
}

val ParentPadding = PaddingValues(
    vertical = SAFE_AREA_VERTICAL_PADDING.dp,
    horizontal = SAFE_AREA_HORIZONTAL_PADDING.dp,
)

@Composable
fun rememberChildPadding(direction: LayoutDirection = LocalLayoutDirection.current) =
    remember {
        Padding(
            start = ParentPadding.calculateStartPadding(direction),
            top = ParentPadding.calculateTopPadding(),
            end = ParentPadding.calculateEndPadding(direction),
            bottom = ParentPadding.calculateBottomPadding()
        )
    }
