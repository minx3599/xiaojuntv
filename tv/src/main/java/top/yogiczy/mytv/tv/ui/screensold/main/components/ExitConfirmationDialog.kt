package top.yogiczy.mytv.tv.ui.screensold.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.tv.material3.WideButton
import top.yogiczy.mytv.tv.ui.material.PopupContent
import top.yogiczy.mytv.tv.ui.utils.focusOnLaunched

@Composable
fun ExitConfirmationDialog(
    visibleProvider: () -> Boolean,
    onDismissRequest: () -> Unit,
    onConfirmExit: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    PopupContent(
        visibleProvider = visibleProvider,
        onDismissRequest = onDismissRequest,
        withBackground = true,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(420.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 32.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = "退出小骏TV？",
                    style = MaterialTheme.typography.headlineSmall,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    WideButton(
                        modifier = Modifier
                            .weight(1f)
                            .focusOnLaunched(),
                        onClick = onConfirmExit,
                    ) {
                        Text("确定退出")
                    }

                    WideButton(
                        modifier = Modifier.weight(1f),
                        onClick = onOpenSettings,
                    ) {
                        Text("进入设置")
                    }
                }
            }
        }
    }
}
