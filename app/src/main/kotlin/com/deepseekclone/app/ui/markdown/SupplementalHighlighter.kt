package com.deepseekclone.app.ui.markdown

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import dev.snipme.highlights.model.SyntaxThemes

/**
 * 补位高亮器：`dev.snipme:highlights` 不支持的常见语言（SQL / JSON / YAML / HTML / XML / CSS）。
 * 配色直接复用 highlights 的主题色板，保证与其它语言视觉一致。
 */
internal object SupplementalHighlighter {

    private val languages = setOf("sql", "json", "yaml", "html", "xml", "css")

    fun supports(language: String?): Boolean = normalize(language) in languages

    fun highlight(code: String, language: String, dark: Boolean): AnnotatedString {
        val theme = SyntaxThemes.default(dark)
        val palette = Palette(
            keyword = theme.keyword,
            string = theme.string,
            literal = theme.literal,
            comment = theme.comment,
            meta = theme.metadata,
            punctuation = theme.punctuation,
        )
        return when (normalize(language)) {
            "sql" -> sql(code, palette)
            "json" -> json(code, palette)
            "yaml" -> yaml(code, palette)
            "html", "xml" -> markup(code, palette)
            "css" -> css(code, palette)
            else -> AnnotatedString(code)
        }
    }

    private fun normalize(language: String?): String = when (val raw = language?.trim()?.lowercase()) {
        null -> ""
        "yml" -> "yaml"
        "htm", "xhtml", "svg", "vue" -> "html"
        else -> raw
    }

    private data class Palette(
        val keyword: Int,
        val string: Int,
        val literal: Int,
        val comment: Int,
        val meta: Int,
        val punctuation: Int,
    )

    /** 规则：(正则, 分组序号, 颜色)。按顺序应用，后面的覆盖前面的。 */
    private data class Rule(val regex: Regex, val group: (Int) -> Int, val color: Int)

    private fun render(code: String, rules: List<Rule>): AnnotatedString {
        val styles = mutableListOf<Triple<Int, Int, Int>>()
        rules.forEach { rule ->
            rule.regex.findAll(code).forEach { match ->
                val group = rule.group(match.groupValues.size)
                if (group in 1 until match.groupValues.size && match.groups[group] != null) {
                    val start = match.groups[group]!!.range.first
                    val end = match.groups[group]!!.range.last + 1
                    if (end > start) styles += Triple(start, end, rule.color)
                }
            }
        }
        return buildAnnotatedString {
            append(code)
            styles.forEach { (start, end, color) ->
                if (start >= 0 && end <= code.length && end > start) {
                    addStyle(SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF000000L or color.toLong())), start, end)
                }
            }
        }
    }

    // ---- SQL ----
    private val sqlKeywords = listOf(
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE",
        "TABLE", "ALTER", "DROP", "INDEX", "VIEW", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "FULL",
        "CROSS", "ON", "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "AS", "AND", "OR", "NOT",
        "NULL", "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "UNIQUE", "DISTINCT", "UNION", "ALL",
        "CASE", "WHEN", "THEN", "ELSE", "END", "LIKE", "IN", "IS", "BETWEEN", "EXISTS", "DEFAULT",
        "ADD", "COLUMN", "CONSTRAINT", "CASCADE", "TRUNCATE", "BEGIN", "COMMIT", "ROLLBACK", "WITH",
        "RECURSIVE", "OVER", "PARTITION", "ASC", "DESC", "IF", "EXPLAIN", "ANALYZE", "VACUUM",
    )

    private fun sql(code: String, p: Palette): AnnotatedString {
        val keywords = sqlKeywords.joinToString("|")
        val pattern = Regex(
            "(--[^\\n]*|/\\*[\\s\\S]*?\\*/)" +                          // 1 comment
                "|('(?:''|[^'])*'|\"(?:\"\"|[^\"])*\")" +                // 2 string
                "|\\b(" + keywords + ")\\b" +                             // 3 keyword
                "|\\b(\\d+(?:\\.\\d+)?)\\b",                         // 4 number
            setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE),
        )
        return paint(
            code,
            pattern,
            listOf(
                1 to p.comment,
                2 to p.string,
                3 to p.keyword,
                4 to p.literal,
            ),
        )
    }

    // ---- 通用实现：一次扫描 + 分组着色 ----
    private fun paint(code: String, pattern: Regex, colors: List<Pair<Int, Int>>): AnnotatedString {
        val styles = mutableListOf<Triple<Int, Int, Int>>()
        pattern.findAll(code).forEach { match ->
            colors.forEach { (group, color) ->
                val g = match.groups[group]
                if (g != null) {
                    styles += Triple(g.range.first, g.range.last + 1, color)
                }
            }
        }
        return buildAnnotatedString {
            append(code)
            styles.sortedBy { it.first }.forEach { (start, end, color) ->
                if (start >= 0 && end <= code.length && end > start) {
                    addStyle(
                        SpanStyle(color = androidx.compose.ui.graphics.Color(0xFF000000L or color.toLong())),
                        start,
                        end,
                    )
                }
            }
        }
    }

    private fun json(code: String, p: Palette): AnnotatedString {
        val pattern = Regex(
            "(\"(?:\\\\.|[^\"\\\\])*\")(?=\\s*:)" +                 // 1 key
                "|(\"(?:\\\\.|[^\"\\\\])*\")" +                     // 2 string
                "|\\b(true|false|null)\\b" +                        // 3 literal
                "|(-?\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b)" +   // 4 number
                "|([{}\\[\\],:])",                                  // 5 punctuation
        )
        return paint(
            code,
            pattern,
            listOf(
                1 to p.meta,
                2 to p.string,
                3 to p.literal,
                4 to p.literal,
                5 to p.punctuation,
            ),
        )
    }

    private fun yaml(code: String, p: Palette): AnnotatedString {
        val pattern = Regex(
            "(#[^\\n]*)" +                                          // 1 comment
                "|^([ \\t]*)([A-Za-z0-9_.\\-\\/]+)(:)" +             // 2/3/4 key
                "|(\"(?:\\\\.|[^\"\\\\])*\"|'(?:''|[^'])*')" +       // 5 string
                "|\\b(true|false|null|yes|no|on|off|~)\\b" +        // 6 literal
                "|(-?\\b\\d+(?:\\.\\d+)?\\b)" +                     // 7 number
                "|^(---|\\.\\.\\.|[-?]\\s)",                          // 8 indicators
            setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE),
        )
        return paint(
            code,
            pattern,
            listOf(
                1 to p.comment,
                3 to p.meta,
                4 to p.punctuation,
                5 to p.string,
                6 to p.literal,
                7 to p.literal,
                8 to p.punctuation,
            ),
        )
    }

    private fun markup(code: String, p: Palette): AnnotatedString {
        val pattern = Regex(
            "(<!--[\\s\\S]*?-->)" +                                         // 1 comment
                "|(<!DOCTYPE[^>]*>)" +                                      // 2 doctype
                "|(</?[A-Za-z][\\w:.-]*)" +                                 // 3 tag
                "|([A-Za-z_:][\\w:.-]*)(?=\\s*=)" +                         // 4 attribute
                "|(\"[^\"]*\"|'[^']*')" +                                   // 5 attr value
                "|(&[a-zA-Z#0-9]+;)",                                        // 6 entity
            setOf(RegexOption.IGNORE_CASE),
        )
        return paint(
            code,
            pattern,
            listOf(
                1 to p.comment,
                2 to p.meta,
                3 to p.keyword,
                4 to p.meta,
                5 to p.string,
                6 to p.literal,
            ),
        )
    }

    private fun css(code: String, p: Palette): AnnotatedString {
        val pattern = Regex(
            "(/\\*[\\s\\S]*?\\*/)" +                                    // 1 comment
                "|(\"[^\"]*\"|'[^']*')" +                               // 2 string
                "|([.#:]?[A-Za-z][\\w-]*)(?=\\s*\\{)" +                 // 3 selector
                "|([A-Za-z-]+)(?=\\s*:)" +                              // 4 property
                "|(-?\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|s|ms|deg)?)" + // 5 number
                "|(@[A-Za-z-]+)",                                        // 6 at-rule
        )
        return paint(
            code,
            pattern,
            listOf(
                1 to p.comment,
                2 to p.string,
                3 to p.keyword,
                4 to p.meta,
                5 to p.literal,
                6 to p.keyword,
            ),
        )
    }
}
