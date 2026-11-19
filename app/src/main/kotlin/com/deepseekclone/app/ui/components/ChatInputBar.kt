package com.deepseekclone.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseekclone.app.data.api.DeepSeekClient
import com.deepseekclone.app.data.model.Attachment
import com.deepseekclone.app.ui.Zh
import com.deepseekclone.app.ui.theme.LocalExtendedColors

/**
 * 底部输入区（对齐官方 2.8.x）：
 * - 文本模式：白卡片 + 圆角；左侧「模型 ▾ / 智能搜索」胶囊，右侧 ＋ 与 发送/语音
 * - 语音模式：卡片内改为「按住说话」，右下角切换成键盘图标
 */
@Composable
fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    model: String,
    effort: String,
    onOpenModelPicker: () -> Unit,
    search: Boolean,
    onSearchChange: (Boolean) -> Unit,
    isStreaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttach: () -> Unit,
    onVoice: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = Zh.inputPlaceholderVoice,
    attachments: List<Attachment> = emptyList(),
    onRemoveAttachment: (Attachment) -> Unit = {},
    uploading: Boolean = false,
    voiceMode: Boolean = false,
    onVoiceModeChange: (Boolean) -> Unit = {},
    onHoldStart: (Offset) -> Unit = {},
    onHoldMove: (Offset) -> Unit = {},
    onHoldEnd: (cancelled: Boolean) -> Unit = {},
    transcribing: Boolean = false,
    showDisclaimer: Boolean = false,
    editingTitle: String? = null,
    onCancelEdit: () -> Unit = {},
    cacheHitPercent: Float? = null,
) {
    val extended = LocalExtendedColors.current
    val primary = MaterialTheme.colorScheme.primary
    var barOrigin by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        if (attachments.isNotEmpty() || uploading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp),
            ) {
                attachments.forEach { attachment ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = extended.cardBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, extended.cardBorder),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 6.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        ) {
                            if (attachment.isImage && attachment.localPath != null) {
                                LocalImage(
                                    path = attachment.localPath,
                                    modifier = Modifier.size(26.dp),
                                    cornerRadius = 6,
                                )
                                Spacer(Modifier.width(6.dp))
                            } else {
                                Icon(
                                    Icons.Outlined.Description,
                                    contentDescription = null,
                                    tint = extended.textSecondary,
                                    modifier = Modifier.size(14.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = attachment.name,
                                fontSize = 12.sp,
                                color = extended.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 110.dp),
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clickable { onRemoveAttachment(attachment) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "移除附件",
                                    tint = extended.textSecondary,
                                    modifier = Modifier.size(13.dp),
                                )
                            }
                        }
                    }
                }
                if (uploading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = primary)
                        Spacer(Modifier.width(6.dp))
                        Text(Zh.uploading, fontSize = 12.sp, color = extended.textSecondary)
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = extended.cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, extended.cardBorder),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { barOrigin = it.positionInRoot() },
        ) {
            Column(modifier = Modifier.padding(start = 18.dp, end = 12.dp, top = if (editingTitle != null) 10.dp else 14.dp, bottom = 12.dp)) {
                if (editingTitle != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = editingTitle,
                            fontSize = 17.sp,
                            color = extended.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(extended.inputBar)
                                .clickable(onClick = onCancelEdit),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "取消修改",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                if (voiceMode) {
                    HoldToTalkArea(
                        recording = transcribing,
                        onPressStart = { local ->
                            onHoldStart(barOrigin + local)
                        },
                        onMove = { local -> onHoldMove(barOrigin + local) },
                        onRelease = { cancelled, local ->
                            onHoldEnd(cancelled)
                        },
                    )
                } else {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                            lineHeight = 24.sp,
                        ),
                        cursorBrush = SolidColor(primary),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp, max = 160.dp),
                        decorationBox = { inner ->
                            Box {
                                if (value.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        fontSize = 17.sp,
                                        color = extended.textSecondary,
                                    )
                                }
                                inner()
                            }
                        },
                    )
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ModelChip(
                        model = model,
                        effort = effort,
                        onClick = onOpenModelPicker,
                    )
                    FeatureChip(
                        label = Zh.webSearch,
                        selected = search,
                        icon = Icons.Outlined.Language,
                        onClick = { onSearchChange(!search) },
                    )
                    Spacer(Modifier.weight(1f))
                    CircleIconButton(
                        icon = Icons.Outlined.Add,
                        description = Zh.attachFiles,
                        filled = false,
                        tint = extended.iconTint,
                        onClick = onAttach,
                    )
                    when {
                        isStreaming -> CircleIconButton(
                            icon = Icons.Filled.Stop,
                            description = Zh.stop,
                            filled = true,
                            tint = Color.White,
                            onClick = onStop,
                        )

                        voiceMode -> CircleIconButton(
                            icon = Icons.Outlined.Keyboard,
                            description = "切换至键盘",
                            filled = false,
                            tint = extended.iconTint,
                            onClick = { onVoiceModeChange(false) },
                        )

                        value.isNotBlank() || attachments.isNotEmpty() -> CircleIconButton(
                            icon = Icons.Outlined.ArrowUpward,
                            description = Zh.send,
                            filled = true,
                            tint = Color.White,
                            onClick = onSend,
                        )

                        else -> CircleIconButton(
                            icon = Icons.Outlined.Mic,
                            description = Zh.voiceInput,
                            filled = false,
                            tint = extended.iconTint,
                            onClick = { onVoiceModeChange(true) },
                        )
                    }
                }
            }
        }
        if (showDisclaimer) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                // 免责声明保持居中；缓存命中率固定在左侧，不改变居中文案的位置
                Text(
                    text = Zh.disclaimer,
                    fontSize = 12.sp,
                    color = extended.textSecondary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center),
                )
                if (cacheHitPercent != null) {
                    Text(
                        text = String.format("缓存命中率 %.1f%%", cacheHitPercent),
                        fontSize = 12.sp,
                        color = extended.textSecondary.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp),
                    )
                }
            }
        }
    }
}

/** 按住说话：按下/移动/抬起，位置回传给全屏渐变遮罩。 */
@Composable
private fun HoldToTalkArea(
    recording: Boolean,
    onPressStart: (Offset) -> Unit,
    onMove: (Offset) -> Unit,
    onRelease: (cancelled: Boolean, local: Offset) -> Unit,
) {
    val extended = LocalExtendedColors.current
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onPressStart(down.position)
                    var cancelled = false
                    var last = down.position
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        last = change.position
                        if (!change.pressed) break
                        onMove(last)
                        if (last.y < -140f) cancelled = true
                        change.consume()
                    }
                    pressed = false
                    onRelease(cancelled, last)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (recording) Zh.transcribing else if (pressed) "松开 发送" else "按住说话",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = if (pressed) MaterialTheme.colorScheme.primary else extended.iconTint,
        )
    }
}

/** 输入框里的模型/思考等级徽标。 */
@Composable
fun ModelChip(
    model: String,
    effort: String,
    onClick: () -> Unit,
) {
    val extended = LocalExtendedColors.current
    val modelLabel = if (model.startsWith("deepseek-v4-pro")) "专家" else "快速"
    val effortLabel = DeepSeekClient.effortLabel(effort)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(extended.chipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            text = "$modelLabel · $effortLabel",
            fontSize = 13.sp,
            color = extended.chipFg,
            fontWeight = FontWeight.Medium,
        )
    }
}

/** 浅蓝胶囊（智能搜索）。 */
@Composable
fun FeatureChip(
    label: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val extended = LocalExtendedColors.current
    val fg = if (selected) extended.chipFg else MaterialTheme.colorScheme.onSurface
    val bg = if (selected) extended.chipBg else Color.Transparent
    val border = if (selected) Color.Transparent else extended.cardBorder
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        Text(text = label, fontSize = 14.sp, color = fg, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    filled: Boolean,
    tint: Color,
    onClick: () -> Unit,
) {
    val extended = LocalExtendedColors.current
    val bg = if (filled) MaterialTheme.colorScheme.primary else Color.Transparent
    val border = if (filled) Color.Transparent else extended.iconTint.copy(alpha = 0.55f)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.2.dp, border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}
