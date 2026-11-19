package com.deepseekclone.app.ui.markdown

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 轻量 Markdown 渲染（对话场景够用）：
 * 标题、段落、粗体/斜体/行内代码/链接、围栏代码块（语法高亮 + 复制）、
 * 有序/无序列表、引用、分割线、行内公式 $...$ 与块级公式 $$...$$。
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
                    style = if (block is MdBlock.Code) {
                        style.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp)
                    } else {
                        style
                    },
                )
                return@forEachIndexed
            }
            val content: @Composable () -> Unit = {
                when (block) {
                    is MdBlock.Heading -> RichText(
                        text = block.text,
                        style = style.copy(
                            fontSize = when (block.level) {
                                1 -> 22.sp
                                2 -> 19.sp
                                else -> 17.sp
                            },
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )

                    is MdBlock.Paragraph -> RichText(text = block.text, style = style)

                    is MdBlock.Quote -> Row {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(if (block.text.length > 40) 40.dp else 20.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    RoundedCornerShape(2.dp),
                                ),
                        )
                        Spacer(Modifier.width(8.dp))
                        RichText(
                            text = block.text,
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
                        RichText(text = block.text, style = style)
                    }

                    MdBlock.Rule -> HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    is MdBlock.Code -> CodeBlock(language = block.language, code = block.code)

                    is MdBlock.Math -> MathBlock(latex = block.latex)
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

/** 块级公式：居中渲染；渲染失败时回退为等宽源码。 */
@Composable
private fun MathBlock(latex: String) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val color = MaterialTheme.colorScheme.onSurface.toArgb()
    val bitmap = remember(latex, color) {
        LatexRenderer.render(context, latex, 18f * density.density, color, display = true)
    }
    if (bitmap != null) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = latex,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
    } else {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "$$latex$$",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

/**
 * 支持行内公式的文本：普通行内 Markdown（粗体/斜体/代码/链接）+ $...$ 公式图片。
 */
@Composable
private fun RichText(text: String, style: TextStyle) {
    if (!text.contains('$')) {
        Text(text = inline(text), style = style)
        return
    }
    val context = LocalContext.current
    val density = LocalDensity.current
    val color = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary
    val codeBg = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    val segments = remember(text) { splitInlineMath(text) }
    val inlineContents = LinkedHashMap<String, InlineTextContent>()
    val annotated = buildAnnotatedString {
        segments.forEachIndexed { index, segment ->
            when (segment) {
                is InlineSegment.Plain -> append(buildInline(segment.text, linkColor, codeBg))
                is InlineSegment.Math -> {
                    val bitmap = LatexRenderer.render(
                        context,
                        segment.latex,
                        16f * density.density,
                        color,
                        display = false,
                    )
                    if (bitmap == null) {
                        append(segment.raw)
                    } else {
                        val id = "inline_math_$index"
                        appendInlineContent(id, segment.raw)
                        inlineContents[id] = InlineTextContent(
                            placeholder = Placeholder(
                                width = (bitmap.width / density.density).sp,
                                height = (bitmap.height / density.density).sp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                            ),
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = segment.latex,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }
            }
        }
    }
    Text(text = annotated, style = style, inlineContent = inlineContents)
}

private sealed interface InlineSegment {
    data class Plain(val text: String) : InlineSegment
    data class Math(val latex: String, val raw: String) : InlineSegment
}

/** 拆分出行内公式与普通文本（跳过 $$ 块级标记）。 */
private fun splitInlineMath(text: String): List<InlineSegment> {
    val result = mutableListOf<InlineSegment>()
    val sb = StringBuilder()
    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == '$' && i + 1 < text.length && text[i + 1] == '$') {
            // $$...$$ 出现在行内时整体当作块级公式的残留，原样保留
            sb.append("$$")
            i += 2
            continue
        }
        if (c == '$' || (c == '\\' && i + 1 < text.length && text[i + 1] == '(')) {
            val isParen = c == '\\'
            val start = if (isParen) i + 2 else i + 1
            var end = start
            var found = -1
            while (end < text.length) {
                if (isParen) {
                    if (text[end] == '\\' && end + 1 < text.length && text[end + 1] == ')') {
                        found = end
                        break
                    }
                } else if (text[end] == '$') {
                    found = end
                    break
                }
                end++
            }
            if (found > start) {
                if (sb.isNotEmpty()) {
                    result += InlineSegment.Plain(sb.toString())
                    sb.clear()
                }
                val latex = text.substring(start, found)
                val rawEnd = if (isParen) found + 2 else found + 1
                result += InlineSegment.Math(latex, text.substring(i, rawEnd))
                i = rawEnd
                continue
            }
        }
        sb.append(c)
        i++
    }
    if (sb.isNotEmpty()) result += InlineSegment.Plain(sb.toString())
    return result.ifEmpty { listOf(InlineSegment.Plain(text)) }
}

/** 代码块：语法高亮 + 语言标签 + 复制。 */
@Composable
private fun CodeBlock(language: String?, code: String) {
    val clipboard = LocalClipboardManager.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val highlighted = remember(code, language, isDark) {
        SyntaxHighlighter.highlight(code, language, isDark)
    }
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
                    text = highlighted,
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
                        SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg, fontSize = 13.sp),
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
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, end)) }
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
    data class Math(val latex: String) : MdBlock
    data object Rule : MdBlock
}

/** 块的可选中纯文本（代码块取代码内容，公式取原始 LaTeX）。 */
private fun plainTextOf(block: MdBlock): String = when (block) {
    is MdBlock.Heading -> block.text
    is MdBlock.Paragraph -> block.text
    is MdBlock.Quote -> block.text
    is MdBlock.Bullet -> block.text
    is MdBlock.Code -> block.code
    is MdBlock.Math -> block.latex
    MdBlock.Rule -> ""
}

private val inlineMathMarker = Regex("^\\$\\$(.+)\\$\\$$")
private val blockMathStart = Regex("^\\$\\$")
private val bracketMathStart = Regex("^\\\\\\\\\\[")
private val bracketMathEnd = Regex("\\\\\\\\\\]$")

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
                i++
                blocks += MdBlock.Code(lang, code.toString().trimEnd('\n'))
            }

            // 单行 $$...$$
            inlineMathMarker.matches(trimmed) -> {
                val latex = inlineMathMarker.matchEntire(trimmed)!!.groupValues[1].trim()
                blocks += MdBlock.Math(latex)
                i++
            }

            // 多行 $$ ... $$
            trimmed.startsWith("$$") || bracketMathStart.containsMatchIn(trimmed) -> {
                val isBracket = bracketMathStart.containsMatchIn(trimmed)
                val opening = if (isBracket) "\\[" else "$$"
                val closing = if (isBracket) "\\]" else "$$"
                val code = StringBuilder(trimmed.removePrefix(opening).trim())
                i++
                while (i < lines.size && !lines[i].trim().endsWith(closing)) {
                    code.append('\n').append(lines[i])
                    i++
                }
                if (i < lines.size) {
                    val tail = lines[i].trim().removeSuffix(closing).trim()
                    if (tail.isNotEmpty()) code.append('\n').append(tail)
                    i++
                }
                blocks += MdBlock.Math(code.toString().trim())
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
                        next.startsWith(">") || next.startsWith("- ") || next.startsWith("* ") ||
                        next.startsWith("$$")
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
