package com.deepseekclone.app.ui.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 轻量 Markdown 渲染（对话场景够用）：
 * 标题、段落、粗体/斜体/行内代码/链接、围栏代码块（带语言标签与复制）、
 * 有序/无序列表、引用、分割线。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    selectedBlockIndex: Int? = null,
    onBlockLongPress: ((index: Int, plainText: String) -> Unit)? = null,
) {
    val blocks = remember(text) { parseBlocks(text) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEachIndexed { index, block ->
            val plain = plainTextOf(block)
            val selectableHere = selectedBlockIndex == index && plain.isNotBlank()
            if (selectableHere) {
                com.deepseekclone.app.ui.components.RangeSelectableText(
                    text = plain,
                    style = if (block is MdBlock.Code) style.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp) else style,
                )
                return@forEachIndexed
            }
            val content: @Composable () -> Unit = {
            when (block) {
                is MdBlock.Heading -> run { Text(
                    text = inline(block.text),
                    style = style.copy(
                        fontSize = when (block.level) {
                            1 -> 22.sp
                            2 -> 19.sp
                            else -> 17.sp
                        },
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) }

                is MdBlock.Paragraph -> run { Text(text = inline(block.text), style = style) }

                is MdBlock.Quote -> Row {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(if (block.text.length > 40) 40.dp else 20.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(2.dp)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = inline(block.text),
                        style = style.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    )
                }

                is MdBlock.Bullet -> Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = if (block.ordered) "${block.index}." else "•",
                        style = style,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.widthIn(min = 16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    run { Text(text = inline(block.text), style = style) }
                }

                MdBlock.Rule -> HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                )

                is MdBlock.Code -> CodeBlock(language = block.language, code = block.code)
            }
            }
            if (onBlockLongPress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { if (plain.isNotBlank()) onBlockLongPress(index, plain) },
                        ),
                ) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}

/** 块的可选中纯文本（代码块取代码内容）。 */
private fun plainTextOf(block: MdBlock): String = when (block) {
    is MdBlock.Heading -> block.text
    is MdBlock.Paragraph -> block.text
    is MdBlock.Quote -> block.text
    is MdBlock.Bullet -> block.text
    is MdBlock.Code -> block.code
    MdBlock.Rule -> ""
}

@Composable
private fun CodeBlock(language: String?, code: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember(code) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(1500)
            copied = false
        }
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = language?.ifBlank { "text" } ?: "text",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            clipboard.setText(AnnotatedString(code))
                            copied = true
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Icon(
                        if (copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                        contentDescription = if (copied) "已复制" else "复制代码",
                        tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = if (copied) "已复制" else "复制",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun inline(text: String): AnnotatedString {
    val primary = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    return remember(text, primary, codeBg) { buildInline(text, primary, codeBg) }
}

private fun buildInline(text: String, linkColor: Color, codeBg: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            text.startsWith("**", i) -> {
                val end = text.indexOf("**", i + 2)
                if (end > i + 2) {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(text.substring(i + 2, end)) }
                    i = end + 2
                } else {
                    append(c); i++
                }
            }

            c == '`' -> {
                val end = text.indexOf('`', i + 1)
                if (end > i + 1) {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = codeBg,
                            fontSize = 13.sp,
                        ),
                    ) { append(text.substring(i + 1, end)) }
                    i = end + 1
                } else {
                    append(c); i++
                }
            }

            c == '[' -> {
                val close = text.indexOf(']', i + 1)
                val open = if (close > 0) text.indexOf('(', close + 1) else -1
                val closeParen = if (open > 0) text.indexOf(')', open + 1) else -1
                if (close > i + 1 && open == close + 1 && closeParen > open) {
                    withStyle(SpanStyle(color = linkColor)) { append(text.substring(i + 1, close)) }
                    i = closeParen + 1
                } else {
                    append(c); i++
                }
            }

            c == '*' -> {
                val end = text.indexOf('*', i + 1)
                if (end > i + 1) {
                    withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                } else {
                    append(c); i++
                }
            }

            else -> {
                append(c); i++
            }
        }
    }
}

private sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    data class Bullet(val text: String, val ordered: Boolean, val index: Int) : MdBlock
    data class Code(val language: String?, val code: String) : MdBlock
    data object Rule : MdBlock
}

private fun parseBlocks(source: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = source.replace("\r\n", "\n").split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> i++

            trimmed.startsWith("```") -> {
                val lang = trimmed.removePrefix("```").trim().ifBlank { null }
                val code = StringBuilder()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    code.append(lines[i]).append('\n')
                    i++
                }
                i++ // 跳过结束围栏
                blocks += MdBlock.Code(lang, code.toString().trimEnd('\n'))
            }

            trimmed.startsWith("#") -> {
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
                blocks += MdBlock.Heading(level, trimmed.drop(level).trim())
                i++
            }

            trimmed.startsWith(">") -> {
                blocks += MdBlock.Quote(trimmed.removePrefix(">").trim())
                i++
            }

            trimmed == "---" || trimmed == "***" -> {
                blocks += MdBlock.Rule
                i++
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ") -> {
                blocks += MdBlock.Bullet(trimmed.drop(2).trim(), ordered = false, index = 0)
                i++
            }

            Regex("^\\d+\\.\\s").containsMatchIn(trimmed) -> {
                val num = trimmed.takeWhile { it.isDigit() }.toIntOrNull() ?: 1
                blocks += MdBlock.Bullet(trimmed.dropWhile { it.isDigit() || it == '.' || it == ' ' }, ordered = true, index = num)
                i++
            }

            else -> {
                val sb = StringBuilder(trimmed)
                i++
                while (i < lines.size) {
                    val next = lines[i].trim()
                    if (next.isEmpty() || next.startsWith("```") || next.startsWith("#") ||
                        next.startsWith(">") || next.startsWith("- ") || next.startsWith("* ")
                    ) break
                    sb.append('\n').append(next)
                    i++
                }
                blocks += MdBlock.Paragraph(sb.toString())
            }
        }
    }
    return blocks
}
