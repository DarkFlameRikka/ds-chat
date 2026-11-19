package com.deepseekclone.app.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.deepseekclone.app.ui.theme.LocalExtendedColors
import kotlin.math.roundToInt

/**
 * 选中文本后的工具条，对齐官方样式：白色圆角浮层，横排「复制 / 全选 / 分享」，
 * 每项是「图标 + 文案」。通过替换 LocalTextToolbar 实现（Compose 默认工具条样式偏系统）。
 */
@Composable
fun OfficialSelectionToolbar(
    shareFallbackText: String,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var state by remember { mutableStateOf<ToolbarState?>(null) }
    val toolbar = remember {
        object : TextToolbar {
            override var status: TextToolbarStatus = TextToolbarStatus.Hidden

            override fun showMenu(
                rect: Rect,
                onCopyRequested: (() -> Unit)?,
                onPasteRequested: (() -> Unit)?,
                onCutRequested: (() -> Unit)?,
                onSelectAllRequested: (() -> Unit)?,
            ) {
                status = TextToolbarStatus.Shown
                state = ToolbarState(rect, onCopyRequested, onSelectAllRequested)
            }

            override fun hide() {
                status = TextToolbarStatus.Hidden
                state = null
            }
        }
    }
    CompositionLocalProvider(LocalTextToolbar provides toolbar) {
        content()
    }
    state?.let { current ->
        Popup(
            popupPositionProvider = SelectionToolbarPositionProvider(current.rect),
            onDismissRequest = { state = null },
            properties = PopupProperties(focusable = false),
        ) {
            ToolbarCardImpl(
                onCopy = {
                    current.onCopy?.invoke()
                    state = null
                },
                onSelectAll = {
                    current.onSelectAll?.invoke()
                    state = null
                },
                onShare = {
                    current.onCopy?.invoke() // 先把选中内容复制，再读取剪贴板分享
                    val text = readClipboardText(context) ?: shareFallbackText
                    state = null
                    runCatching {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(send, "分享"))
                    }
                },
            )
        }
    }
}

private data class ToolbarState(
    val rect: Rect,
    val onCopy: (() -> Unit)?,
    val onSelectAll: (() -> Unit)?,
)

@Composable
internal fun ToolbarCardImpl(
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
) {
    val extended = LocalExtendedColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = extended.cardBg,
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ToolbarItem(Icons.Outlined.ContentCopy, "复制", onCopy)
            ToolbarItem(Icons.Outlined.CheckBox, "全选", onSelectAll)
            ToolbarItem(Icons.AutoMirrored.Outlined.Reply, "分享", onShare)
        }
    }
}

@Composable
private fun ToolbarItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(84.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** 工具条定位：显示在选区上方居中，贴边时自动收进窗口内。 */
private class SelectionToolbarPositionProvider(
    private val selectionRect: Rect,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect,
        windowSize: IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val centerX = selectionRect.center.x
        val x = (centerX - popupContentSize.width / 2f)
            .roundToInt()
            .coerceIn(8, (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8))
        val above = (selectionRect.top - popupContentSize.height - 12f).roundToInt()
        val y = if (above >= 8) above else (selectionRect.bottom + 12f).roundToInt()
        return IntOffset(x, y)
    }
}

private fun readClipboardText(context: Context): String? = runCatching {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
}.getOrNull()
