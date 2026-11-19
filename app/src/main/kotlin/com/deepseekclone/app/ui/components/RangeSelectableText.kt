package com.deepseekclone.app.ui.components

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

/**
 * 自定义「整段选中 + 可拖手柄」的可选文本：
 * 进入即全选，两端各一个圆形手柄，拖动即可调整选区；上方浮出「复制 / 全选 / 分享」。
 *
 * 自己画的原因：Compose 1.7 的 SelectionContainer 只能长按进入选文态，
 * 程序化选区不显示手柄（受控 Selection API 是 internal）。
 */
@Composable
fun RangeSelectableText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val highlight = primary.copy(alpha = 0.28f)
    var layout by remember(text) { mutableStateOf<TextLayoutResult?>(null) }
    var range by remember(text) { mutableStateOf(0 to text.length) }
    var toolbarVisible by remember { mutableStateOf(true) }

    val annotated: AnnotatedString = remember(range, text, highlight) {
        buildAnnotatedString {
            append(text)
            val start = range.first.coerceIn(0, text.length)
            val end = range.second.coerceIn(0, text.length)
            if (end > start) addStyle(SpanStyle(background = highlight), start, end)
        }
    }

    var dragTarget by remember { mutableStateOf(HandleTarget.NONE) }
    var dragTotal by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .pointerInput(text) {
                detectDragGestures(
                    onDragStart = { start ->
                        val lr = layout
                        dragTotal = Offset.Zero
                        dragTarget = if (lr != null) {
                            val boxes = handleBoxes(lr, text.length, range)
                            val startPos = boxes?.first?.let { Offset(it.left, it.top) }
                            val endPos = boxes?.second?.let { Offset(it.right, it.bottom) }
                            val dStart = startPos?.let { (it - start).getDistance() } ?: Float.MAX_VALUE
                            val dEnd = endPos?.let { (it - start).getDistance() } ?: Float.MAX_VALUE
                            if (dStart <= dEnd) HandleTarget.START else HandleTarget.END
                        } else HandleTarget.NONE
                        toolbarVisible = false
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        dragTotal += amount
                        val lr = layout ?: return@detectDragGestures
                        val boxes = handleBoxes(lr, text.length, range) ?: return@detectDragGestures
                        when (dragTarget) {
                            HandleTarget.START -> {
                                val pos = Offset(boxes.first.left + dragTotal.x, boxes.first.top + dragTotal.y)
                                val offset = lr.getOffsetForPosition(pos).coerceIn(0, range.second)
                                range = offset to range.second
                            }
                            HandleTarget.END -> {
                                val pos = Offset(boxes.second.right + dragTotal.x, boxes.second.bottom + dragTotal.y)
                                val offset = lr.getOffsetForPosition(pos).coerceIn(range.first, text.length)
                                range = range.first to offset
                            }
                            HandleTarget.NONE -> Unit
                        }
                    },
                )
            },
    ) {
        Text(
            text = annotated,
            style = style,
            onTextLayout = { layout = it },
            modifier = Modifier.drawWithContent {
                drawContent()
                val lr = layout ?: return@drawWithContent
                val boxes = handleBoxes(lr, text.length, range) ?: return@drawWithContent
                val r = 9.dp.toPx()
                // 起始手柄在选区左上，结束手柄在选区右下
                drawCircle(color = primary, radius = r, center = Offset(boxes.first.left, boxes.first.top))
                drawCircle(color = primary, radius = r, center = Offset(boxes.second.right, boxes.second.bottom))
            },
        )

        val lr = layout
        if (lr != null && text.isNotEmpty()) {
            val startOffset = range.first.coerceIn(0, text.length - 1)
            val endOffset = (range.second - 1).coerceIn(0, text.length - 1)
            val startBox = runCatching { lr.getBoundingBox(startOffset) }.getOrNull()
            val endBox = runCatching { lr.getBoundingBox(endOffset) }.getOrNull()

            if (toolbarVisible && startBox != null && endBox != null) {
                val anchor = Rect(
                    left = startBox.left,
                    top = startBox.top,
                    right = endBox.right,
                    bottom = endBox.bottom,
                )
                Popup(
                    popupPositionProvider = RangeToolbarPositionProvider(anchor),
                    onDismissRequest = { toolbarVisible = false },
                    properties = PopupProperties(focusable = false),
                ) {
                    ToolbarCardImpl(
                        onCopy = {
                            copyToClipboard(context, selectedText(text, range))
                            toolbarVisible = false
                        },
                        onSelectAll = { range = 0 to text.length },
                        onShare = {
                            shareText(context, selectedText(text, range))
                            toolbarVisible = false
                        },
                    )
                }
            }
        }
    }
}

private enum class HandleTarget { NONE, START, END }

/** 返回选区起点与终点字符的包围盒（px，相对文本左上角）。 */
private fun handleBoxes(
    layout: TextLayoutResult,
    textLength: Int,
    range: Pair<Int, Int>,
): Pair<androidx.compose.ui.geometry.Rect, androidx.compose.ui.geometry.Rect>? = runCatching {
    val start = range.first.coerceIn(0, textLength - 1)
    val end = (range.second - 1).coerceIn(0, textLength - 1)
    layout.getBoundingBox(start) to layout.getBoundingBox(end)
}.getOrNull()

private fun selectedText(text: String, range: Pair<Int, Int>): String {
    val start = range.first.coerceIn(0, text.length)
    val end = range.second.coerceIn(start, text.length)
    return text.substring(start, end)
}

internal fun copyToClipboard(context: Context, text: String) {
    runCatching {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(android.content.ClipData.newPlainText("text", text))
    }
}

internal fun shareText(context: Context, text: String) {
    runCatching {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(send, "分享"))
    }
}

private class RangeToolbarPositionProvider(
    private val anchor: Rect,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: androidx.compose.ui.unit.IntRect,
        windowSize: IntSize,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val absLeft = anchorBounds.left + anchor.left
        val absTop = anchorBounds.top + anchor.top
        val absBottom = anchorBounds.top + anchor.bottom
        val x = (absLeft + (anchor.width / 2f) - popupContentSize.width / 2f)
            .roundToInt()
            .coerceIn(8, (windowSize.width - popupContentSize.width - 8).coerceAtLeast(8))
        val above = (absTop - popupContentSize.height - 12f).roundToInt()
        val below = (absBottom + 12f).roundToInt()
        val y = (if (above >= 8) above else below)
            .coerceIn(8, (windowSize.height - popupContentSize.height - 8).coerceAtLeast(8))
        return IntOffset(x, y)
    }
}
