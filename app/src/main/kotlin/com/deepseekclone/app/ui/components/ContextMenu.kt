package com.deepseekclone.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.deepseekclone.app.ui.theme.LocalExtendedColors

/** 长按菜单的一项。 */
data class MenuAction(
    val label: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * 官方样式的长按菜单：浮层圆角卡片（浅紫底、大圆角、柔和阴影），
 * 每行「图标 + 文案」，行高 52dp。
 */
@Composable
fun SelectionMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    actions: List<MenuAction>,
    alignment: Alignment = Alignment.TopStart,
    offset: androidx.compose.ui.unit.IntOffset = androidx.compose.ui.unit.IntOffset.Zero,
) {
    if (!expanded) return
    val extended = LocalExtendedColors.current
    Popup(
        alignment = alignment,
        offset = offset,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = extended.menuBg,
            shadowElevation = 14.dp,
            modifier = Modifier.width(236.dp).padding(8.dp),
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                actions.forEach { action ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                action.onClick()
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        Icon(
                            action.icon,
                            contentDescription = null,
                            tint = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(21.dp),
                        )
                        Spacer(Modifier.width(20.dp))
                        Text(
                            text = action.label,
                            fontSize = 17.sp,
                            color = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
