package top.yogiczy.mytv.tv.ui.screen.network

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.WideButton
import top.yogiczy.mytv.tv.ui.material.CircularProgressIndicator
import top.yogiczy.mytv.tv.ui.theme.colors
import top.yogiczy.mytv.tv.ui.utils.focusOnLaunched

enum class NetworkStatus {
    OFFLINE,
    SLOW,
    SOURCE_TIMEOUT,
    PLAYBACK_FAILED,
    RECOVERED,
    RETRYING,
}

@Composable
fun NetworkStatusPanel(
    modifier: Modifier = Modifier,
    status: NetworkStatus,
    detail: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val copy = when (status) {
        NetworkStatus.OFFLINE -> "暂无网络连接" to "请检查电视的网络连接后重试"
        NetworkStatus.SLOW -> "网络连接较慢" to "直播源响应时间较长，请稍候"
        NetworkStatus.SOURCE_TIMEOUT -> "直播源加载超时" to "当前直播源暂时没有响应"
        NetworkStatus.PLAYBACK_FAILED -> "当前频道暂时无法播放" to "可以重试当前线路，或返回选择其他频道"
        NetworkStatus.RECOVERED -> "网络已恢复" to "可以继续加载直播内容"
        NetworkStatus.RETRYING -> "正在重试" to "正在重新连接，请稍候"
    }

    Column(
        modifier = modifier
            .widthIn(min = 360.dp, max = 560.dp)
            .background(
                MaterialTheme.colors.surfaceContainer,
                RoundedCornerShape(12.dp),
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.outlineVariant.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 28.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(copy.first, style = MaterialTheme.typography.headlineSmall)
        Text(
            detail?.takeIf { it.isNotBlank() } ?: copy.second,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (status == NetworkStatus.RETRYING) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 3.dp,
            )
        } else if (onPrimaryAction != null || onSecondaryAction != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                onPrimaryAction?.let { action ->
                    WideButton(
                        modifier = Modifier
                            .weight(1f)
                            .focusOnLaunched(),
                        onClick = action,
                    ) {
                        Text(
                            when (status) {
                                NetworkStatus.PLAYBACK_FAILED -> "重试播放"
                                NetworkStatus.SOURCE_TIMEOUT -> "重新加载"
                                NetworkStatus.RECOVERED -> "继续播放"
                                else -> "重试"
                            }
                        )
                    }
                }

                onSecondaryAction?.let { action ->
                    WideButton(
                        modifier = Modifier.weight(1f),
                        onClick = action,
                    ) {
                        Text(
                            when (status) {
                                NetworkStatus.PLAYBACK_FAILED -> "返回选台"
                                NetworkStatus.SOURCE_TIMEOUT -> "切换直播源"
                                NetworkStatus.RETRYING -> "取消重试"
                                NetworkStatus.RECOVERED -> "关闭"
                                else -> "返回"
                            }
                        )
                    }
                }
            }
        }
    }
}
