package top.yogiczy.mytv.tv.ui.screen.settings.subcategories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.ListItem
import androidx.tv.material3.ListItemDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import top.yogiczy.mytv.tv.ui.rememberChildPadding
import top.yogiczy.mytv.tv.ui.screen.components.AppScreen
import top.yogiczy.mytv.tv.ui.utils.Configs
import top.yogiczy.mytv.tv.ui.utils.handleKeyEvents

@Composable
fun SettingsMpvDecoderModeScreen(
    modifier: Modifier = Modifier,
    modeProvider: () -> Configs.MpvDecoderMode = { Configs.MpvDecoderMode.SOFTWARE },
    onModeChanged: (Configs.MpvDecoderMode) -> Unit = {},
    onBackPressed: () -> Unit = {},
) {
    val childPadding = rememberChildPadding()

    AppScreen(
        modifier = modifier.padding(top = 10.dp),
        header = { Text("设置 / 播放器 / MPV 解码模式") },
        canBack = true,
        onBackPressed = onBackPressed,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = childPadding.copy(top = 10.dp).paddingValues,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(Configs.MpvDecoderMode.entries) { mode ->
                ListItem(
                    modifier = Modifier.handleKeyEvents(onSelect = { onModeChanged(mode) }),
                    headlineContent = { Text(mode.label) },
                    supportingContent = {
                        Text(
                            when (mode) {
                                Configs.MpvDecoderMode.SOFTWARE -> "CPU 解码，兼容性高但老电视可能卡顿"
                                Configs.MpvDecoderMode.HARDWARE_COPY -> "硬件解码并兼容老电视显示驱动"
                                Configs.MpvDecoderMode.HARDWARE -> "直接硬解，适合较新的 64 位电视"
                            }
                        )
                    },
                    trailingContent = {
                        if (modeProvider() == mode) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(0.1f),
                    ),
                    selected = false,
                    onClick = {},
                )
            }
        }
    }
}
