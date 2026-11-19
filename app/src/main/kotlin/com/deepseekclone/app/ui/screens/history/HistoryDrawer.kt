package com.deepseekclone.app.ui.screens.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseekclone.app.data.model.Conversation
import com.deepseekclone.app.data.model.ConversationGroup
import com.deepseekclone.app.data.model.group
import com.deepseekclone.app.ui.Zh
import com.deepseekclone.app.ui.theme.LocalExtendedColors

/**
 * 侧边栏：搜索 + 分组会话列表 + 多选 + 设置入口。
 * 层次：会话标题（一级）> 分组标签（二级）> 行内操作（三级，hover/常驻弱化）。
 */
@Composable
fun HistoryDrawerContent(
    conversations: List<Conversation>,
    currentConversationId: String?,
    onSelect: (String) -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onDelete: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
    onRename: (String, String) -> Unit,
    onBatchDelete: (List<String>) -> Unit = {},
) {
    val extended = LocalExtendedColors.current
    var query by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }
    var selectionMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateOf(setOf<String>()) }

    val filtered = conversations
        .filter { it.messages.isNotEmpty() } // 隐藏还没发过消息的空会话
        .filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
    val pinned = filtered.filter { it.pinned }
    val grouped = filtered.filterNot { it.pinned }
        .groupBy { it.group() }
        .toList()
        .sortedBy { it.first.ordinal }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = extended.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) {
                                Text(
                                    text = Zh.searchHint,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = extended.textSecondary,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
        }

        if (conversations.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(Zh.emptyTitle, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    Zh.emptyDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extended.textSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                var headerIndex = 0
                if (pinned.isNotEmpty()) {
                    item {
                        GroupHeader(
                            label = Zh.pinned,
                            showMultiSelect = headerIndex++ == 0,
                            onMultiSelect = { selectionMode = true },
                        )
                    }
                    items(pinned, key = { "p_${it.id}" }) { conv ->
                        SessionRow(
                            conversation = conv,
                            selected = conv.id == currentConversationId,
                            selectionMode = selectionMode,
                            checked = conv.id in selected.value,
                            onToggleCheck = { toggle(selected, conv.id) },
                            onSelect = onSelect,
                            onPin = onPin,
                            onRename = { renameTarget = it },
                            onDelete = { deleteTarget = it },
                            onMultiSelect = {
                                selectionMode = true
                                selected.value = selected.value + conv.id
                            },
                        )
                    }
                }
                grouped.forEach { (group, list) ->
                    item {
                        GroupHeader(
                            label = groupLabel(group),
                            showMultiSelect = headerIndex++ == 0,
                            onMultiSelect = { selectionMode = true },
                        )
                    }
                    items(list, key = { it.id }) { conv ->
                        SessionRow(
                            conversation = conv,
                            selected = conv.id == currentConversationId,
                            selectionMode = selectionMode,
                            checked = conv.id in selected.value,
                            onToggleCheck = { toggle(selected, conv.id) },
                            onSelect = onSelect,
                            onPin = onPin,
                            onRename = { renameTarget = it },
                            onDelete = { deleteTarget = it },
                            onMultiSelect = {
                                selectionMode = true
                                selected.value = selected.value + conv.id
                            },
                        )
                    }
                }
            }
        }

        if (selectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "已选 ${selected.value.size} 项",
                    fontSize = 13.sp,
                    color = extended.textSecondary,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "取消",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .clickable {
                            selectionMode = false
                            selected.value = emptySet()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
                Text(
                    text = "删除",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .clickable(enabled = selected.value.isNotEmpty()) {
                            onBatchDelete(selected.value.toList())
                            selectionMode = false
                            selected.value = emptySet()
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenSettings)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = null,
                tint = extended.textSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(Zh.settings, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.weight(1f))
        }
        Text(
            text = Zh.thirdPartyNote,
            style = MaterialTheme.typography.labelMedium,
            color = extended.textSecondary.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }

    renameTarget?.let { target ->
        var text by remember(target.id) { mutableStateOf(target.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(Zh.rename) },
            text = {
                OutlinedTextField(
                    value = androidx.compose.ui.text.input.TextFieldValue(
                        text = text,
                        selection = androidx.compose.ui.text.TextRange(text.length),
                    ),
                    onValueChange = { text = it.text },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) onRename(target.id, text.trim())
                    renameTarget = null
                }) { Text(Zh.confirm) }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text(Zh.cancel) } },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("${Zh.delete}「${target.title}」？") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleteTarget = null
                }) { Text(Zh.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(Zh.cancel) } },
        )
    }
}

private fun toggle(state: androidx.compose.runtime.MutableState<Set<String>>, id: String) {
    state.value = if (id in state.value) state.value - id else state.value + id
}

@Composable
private fun GroupHeader(
    label: String,
    showMultiSelect: Boolean,
    onMultiSelect: () -> Unit,
) {
    val extended = LocalExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = extended.textSecondary,
        )
        Spacer(Modifier.weight(1f))
        if (showMultiSelect) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .semantics { contentDescription = "多选" }
                    .clickable(onClick = onMultiSelect),
                contentAlignment = Alignment.Center,
            ) {
                MultiSelectIcon(tint = extended.textSecondary)
            }
        }
    }
}

/** 官方「多选」图标：左侧两个圆点 + 右侧两条横线。 */
@Composable
private fun MultiSelectIcon(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val r = 2.dp.toPx()
        val stroke = 1.6.dp.toPx()
        val gap = size.height * 0.36f
        drawCircle(tint, r, Offset(r + 1.dp.toPx(), size.height / 2 - gap / 2))
        drawCircle(tint, r, Offset(r + 1.dp.toPx(), size.height / 2 + gap / 2))
        val x1 = size.width * 0.42f
        val x2 = size.width
        drawLine(tint, Offset(x1, size.height / 2 - gap / 2), Offset(x2, size.height / 2 - gap / 2), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(tint, Offset(x1, size.height / 2 + gap / 2), Offset(x2, size.height / 2 + gap / 2), stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
private fun SessionRow(
    conversation: Conversation,
    selected: Boolean,
    selectionMode: Boolean,
    checked: Boolean,
    onToggleCheck: () -> Unit,
    onSelect: (String) -> Unit,
    onPin: (String, Boolean) -> Unit,
    onRename: (Conversation) -> Unit,
    onDelete: (Conversation) -> Unit,
    onMultiSelect: () -> Unit,
) {
    val extended = LocalExtendedColors.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                RoundedCornerShape(10.dp),
            )
            .clickable { if (selectionMode) onToggleCheck() else onSelect(conversation.id) }
            .padding(start = 8.dp, end = 4.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.5.dp, if (checked) MaterialTheme.colorScheme.primary else extended.textSecondary, CircleShape)
                    .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (checked) {
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = conversation.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (!selectionMode) {
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.MoreHoriz,
                        contentDescription = "更多操作",
                        tint = extended.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(Zh.rename) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            onRename(conversation)
                            menuOpen = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (conversation.pinned) Zh.unpin else Zh.pinned) },
                        leadingIcon = { Icon(Icons.Outlined.PushPin, null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            onPin(conversation.id, !conversation.pinned)
                            menuOpen = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("多选") },
                        leadingIcon = { MultiSelectIcon(tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        onClick = {
                            onMultiSelect()
                            menuOpen = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(Zh.delete, color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            onDelete(conversation)
                            menuOpen = false
                        },
                    )
                }
            }
        }
    }
}

private fun groupLabel(group: ConversationGroup): String = when (group) {
    ConversationGroup.TODAY -> Zh.groupToday
    ConversationGroup.LAST_7_DAYS -> Zh.group7Days
    ConversationGroup.LAST_30_DAYS -> Zh.group30Days
    ConversationGroup.EARLIER -> Zh.groupEarlier
}
