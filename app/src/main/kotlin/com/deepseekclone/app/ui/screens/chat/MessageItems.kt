package com.deepseekclone.app.ui.screens.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import com.deepseekclone.app.ui.components.LocalImage
import com.deepseekclone.app.ui.components.rememberImageRatio
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseekclone.app.data.model.ChatMessage
import com.deepseekclone.app.data.model.Citation
import com.deepseekclone.app.data.model.MessageStatus
import com.deepseekclone.app.data.model.ToolActivity
import com.deepseekclone.app.ui.Zh
import com.deepseekclone.app.ui.markdown.MarkdownText
import com.deepseekclone.app.ui.theme.LocalExtendedColors

/**
 * 用户消息：右侧浅蓝气泡。
 * 长按弹出「修改 / 复制 / 选择文本 / 分享」菜单（与官方一致）。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun UserMessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier,
    onEdit: (ChatMessage) -> Unit = {},
    onCopy: (ChatMessage) -> Unit = {},
    onShare: (ChatMessage) -> Unit = {},
) {
    val extended = LocalExtendedColors.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var menuOpen by remember { mutableStateOf(false) }
    var selectable by remember { mutableStateOf(false) }
    val images = message.attachments.filter { it.isImage && it.localPath != null }
    val files = message.attachments.filterNot { it.isImage && it.localPath != null }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = { if (!selectable) menuOpen = true }),
            horizontalArrangement = Arrangement.End,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = extended.bubbleUser,
                modifier = Modifier.widthIn(max = 300.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
                    when {
                        images.size == 1 -> {
                            val ratio = rememberImageRatio(images.first().localPath)
                            LocalImage(
                                path = images.first().localPath,
                                modifier = Modifier
                                    .widthIn(max = 240.dp)
                                    .heightIn(max = 240.dp)
                                    .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier),
                                contentScale = ContentScale.Fit,
                            )
                        }

                        images.size > 1 -> Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.widthIn(max = 260.dp),
                        ) {
                            images.take(3).forEach { image ->
                                LocalImage(
                                    path = image.localPath,
                                    modifier = Modifier.size(80.dp),
                                    cornerRadius = 10,
                                )
                            }
                        }
                    }
                    files.forEach { attachment ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = if (images.isNotEmpty()) 6.dp else 0.dp, bottom = 4.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = attachment.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    if (message.content.isNotBlank()) {
                        if (selectable) {
                            com.deepseekclone.app.ui.components.RangeSelectableText(
                                text = message.content,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        } else {
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        com.deepseekclone.app.ui.components.SelectionMenu(
            expanded = menuOpen,
            onDismiss = { menuOpen = false },
            actions = listOf(
                com.deepseekclone.app.ui.components.MenuAction("修改", Icons.Outlined.Edit) { onEdit(message) },
                com.deepseekclone.app.ui.components.MenuAction("复制", Icons.Outlined.ContentCopy) {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(message.content))
                    onCopy(message)
                },
                com.deepseekclone.app.ui.components.MenuAction("选择文本", TextSelectIcon) { selectable = true },
                com.deepseekclone.app.ui.components.MenuAction("分享", Icons.AutoMirrored.Outlined.Reply) { onShare(message) },
            ),
            alignment = Alignment.BottomStart,
        )
    }
}


/** AI 消息：工具轨迹 + 思考过程 + Markdown 正文 + 参考来源 + 操作行。 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AssistantMessage(
    message: ChatMessage,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onShare: () -> Unit,
    onReadAloud: (() -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var selectedBlock by remember { mutableStateOf<Int?>(null) }
    var pressedBlock by remember { mutableStateOf(0) }
    val clearKey = com.deepseekclone.app.ui.components.LocalSelectionClearKey.current
    LaunchedEffect(clearKey) {
        if (clearKey > 0) {
            selectedBlock = null
            menuOpen = false
        }
    }
    Box(modifier = modifier.fillMaxWidth()) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!message.reasoning.isNullOrBlank()) {
            ReasoningSection(
                reasoning = message.reasoning,
                isThinking = message.status == MessageStatus.STREAMING && message.content.isBlank(),
                durationSeconds = message.thinkSeconds,
            )
        }
        val visibleTools = message.tools.filterNot { tool ->
            tool.done && tool.resultCount == 0 && tool.error == null && tool.kind != "READ_LINK"
        }
        if (visibleTools.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 6.dp)) {
                visibleTools.forEach { ToolRow(it) }
            }
        }
        when {
            message.status == MessageStatus.ERROR -> {
                Text(
                    text = message.error ?: Zh.serverError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }

            message.content.isBlank() && isStreaming -> {
                Text(
                    text = Zh.thinkingNow,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            else -> {
                val display = if (isStreaming) message.content + " ▌" else message.content
                MarkdownText(
                    text = display,
                    modifier = Modifier.padding(top = 6.dp),
                    selectedBlockIndex = selectedBlock,
                    onBlockLongPress = { index, _ ->
                        pressedBlock = index
                        menuOpen = true
                    },
                )
            }
        }
        if (message.citations.isNotEmpty()) {
            CitationsRow(message.citations)
        }
        if (!isStreaming && message.status == MessageStatus.DONE && message.content.isNotBlank()) {
            MessageActions(
                onCopy = onCopy,
                onRegenerate = onRegenerate,
                onLike = onLike,
                onDislike = onDislike,
                onShare = onShare,
                onReadAloud = onReadAloud,
            )
        }
    }

    com.deepseekclone.app.ui.components.SelectionMenu(
        expanded = menuOpen,
        onDismiss = { menuOpen = false },
        actions = listOf(
            com.deepseekclone.app.ui.components.MenuAction("复制", Icons.Outlined.ContentCopy) { onCopy() },
            com.deepseekclone.app.ui.components.MenuAction("选择文本", TextSelectIcon) { selectedBlock = pressedBlock },
            com.deepseekclone.app.ui.components.MenuAction("重新生成", Icons.Outlined.Refresh) { onRegenerate() },
            com.deepseekclone.app.ui.components.MenuAction("分享", Icons.AutoMirrored.Outlined.Reply) { onShare() },
        ),
    )
    }
}

/** 「选择文本」图标：竖线 + 上下虚线，贴近官方的 I 形光标图标。 */
val TextSelectIcon: androidx.compose.ui.graphics.vector.ImageVector
    get() = Icons.Outlined.TextFields

@Composable
private fun ToolRow(tool: ToolActivity) {
    val icon: ImageVector = when (tool.kind) {
        "READ_LINK" -> Icons.Outlined.Language
        else -> Icons.Outlined.Search
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp),
    ) {
        if (!tool.done) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = tool.label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = if (tool.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CitationsRow(citations: List<Citation>) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = Zh.references,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.padding(top = 4.dp)) {
            citations.take(8).forEachIndexed { index, citation ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { runCatching { uriHandler.openUri(citation.url) } }
                        .padding(vertical = 3.dp),
                ) {
                    Text(
                        text = "${index + 1}.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(18.dp),
                    )
                    Text(
                        text = citation.title ?: citation.url,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReasoningSection(
    reasoning: String,
    isThinking: Boolean,
    durationSeconds: Int?,
) {
    var expanded by remember { mutableStateOf(isThinking) }
    LaunchedEffect(isThinking) { if (!isThinking) expanded = false }
    val label = when {
        isThinking -> Zh.thinkingNow
        durationSeconds != null && durationSeconds > 0 -> String.format(Zh.thinkDuration, durationSeconds)
        else -> Zh.thinkingNow
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            if (expanded) {
                Text(
                    text = reasoning,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageActions(
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onShare: () -> Unit,
    onReadAloud: (() -> Unit)?,
) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionIcon(
            icon = if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
            label = if (copied) Zh.copyOk else Zh.copy,
            onClick = {
                onCopy()
                copied = true
            },
            tint = if (copied) MaterialTheme.colorScheme.primary else null,
        )
        ActionIcon(Icons.Outlined.ThumbUp, Zh.like, onLike)
        ActionIcon(Icons.Outlined.ThumbDown, Zh.dislike, onDislike)
        if (onReadAloud != null) {
            ActionIcon(Icons.Outlined.VolumeUp, Zh.readAloud, onReadAloud)
        }
        ActionIcon(Icons.AutoMirrored.Outlined.Reply, Zh.share, onShare)
        Spacer(Modifier.weight(1f))
        ActionIcon(Icons.Outlined.Refresh, Zh.regenerate, onRegenerate)
    }
}

@Composable
private fun MenuItemText(label: String, onClick: () -> Unit) {
    androidx.compose.material3.DropdownMenuItem(
        text = { Text(label, fontSize = 15.sp) },
        onClick = onClick,
    )
}

@Composable
private fun ActionIcon(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color? = null,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(
            icon,
            contentDescription = label,
            tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
        )
    }
}
