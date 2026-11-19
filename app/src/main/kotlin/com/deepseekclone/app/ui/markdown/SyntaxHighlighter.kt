package com.deepseekclone.app.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

/**
 * 轻量代码高亮：正则分词（注释 / 字符串 / 数字 / 关键字 / 类型 / 注解），
 * 不引入额外依赖，覆盖常见语言。
 */
object SyntaxHighlighter {

    private data class Palette(
        val keyword: Color,
        val string: Color,
        val number: Color,
        val comment: Color,
        val type: Color,
        val annotation: Color,
    )

    private val Light = Palette(
        keyword = Color(0xFF0550AE),
        string = Color(0xFF0A7D33),
        number = Color(0xFFB35900),
        comment = Color(0xFF6E7781),
        type = Color(0xFF8250DF),
        annotation = Color(0xFF8250DF),
    )
    private val Dark = Palette(
        keyword = Color(0xFF79C0FF),
        string = Color(0xFF7EE787),
        number = Color(0xFFFFA657),
        comment = Color(0xFF8B949E),
        type = Color(0xFFD2A8FF),
        annotation = Color(0xFFD2A8FF),
    )

    private val keywords: Map<String, Set<String>> = mapOf(
        "kotlin" to setOf(
            "fun", "val", "var", "if", "else", "when", "for", "while", "do", "return", "class", "object",
            "interface", "data", "sealed", "enum", "companion", "override", "private", "public", "protected",
            "internal", "suspend", "import", "package", "in", "is", "as", "try", "catch", "finally", "throw",
            "null", "true", "false", "this", "super", "it", "by", "lateinit", "init", "const", "typealias",
            "inline", "reified", "vararg", "operator", "infix", "out", "where", "break", "continue", "abstract",
        ),
        "java" to setOf(
            "public", "private", "protected", "class", "interface", "enum", "extends", "implements", "static",
            "final", "void", "int", "long", "double", "float", "boolean", "char", "byte", "short", "new",
            "return", "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "try", "catch",
            "finally", "throw", "throws", "import", "package", "this", "super", "null", "true", "false",
            "abstract", "synchronized", "volatile", "transient", "instanceof", "record", "var", "var",
        ),
        "python" to setOf(
            "def", "class", "return", "if", "elif", "else", "for", "while", "break", "continue", "import",
            "from", "as", "with", "try", "except", "finally", "raise", "pass", "lambda", "yield", "global",
            "nonlocal", "assert", "del", "in", "is", "not", "and", "or", "None", "True", "False", "async",
            "await", "self", "print",
        ),
        "js" to setOf(
            "function", "const", "let", "var", "return", "if", "else", "for", "while", "do", "switch", "case",
            "break", "continue", "try", "catch", "finally", "throw", "new", "class", "extends", "super", "this",
            "async", "await", "import", "export", "default", "from", "of", "in", "typeof", "instanceof",
            "null", "undefined", "true", "false", "yield", "static", "get", "set",
        ),
        "bash" to setOf(
            "if", "then", "else", "elif", "fi", "for", "while", "do", "done", "case", "esac", "function",
            "echo", "cd", "export", "local", "return", "exit", "source", "sudo", "curl", "grep", "sed", "awk",
            "cat", "ls", "mkdir", "rm", "chmod", "set", "unset", "read",
        ),
        "sql" to setOf(
            "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE",
            "TABLE", "ALTER", "DROP", "INDEX", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "ON", "GROUP", "BY",
            "ORDER", "HAVING", "LIMIT", "OFFSET", "AS", "AND", "OR", "NOT", "NULL", "PRIMARY", "KEY",
            "FOREIGN", "REFERENCES", "DISTINCT", "COUNT", "SUM", "AVG", "MIN", "MAX",
        ),
        "c" to setOf(
            "int", "char", "float", "double", "void", "long", "short", "unsigned", "signed", "struct",
            "typedef", "static", "const", "extern", "return", "if", "else", "for", "while", "do", "switch",
            "case", "break", "continue", "sizeof", "include", "define", "ifdef", "ifndef", "endif", "NULL",
        ),
        "cpp" to setOf(
            "int", "char", "float", "double", "void", "long", "short", "unsigned", "signed", "class", "struct",
            "public", "private", "protected", "virtual", "override", "template", "typename", "namespace",
            "using", "const", "constexpr", "return", "if", "else", "for", "while", "do", "switch", "case",
            "break", "continue", "new", "delete", "this", "nullptr", "auto", "static", "include", "define",
        ),
        "go" to setOf(
            "func", "package", "import", "var", "const", "type", "struct", "interface", "map", "chan", "go",
            "defer", "select", "case", "switch", "if", "else", "for", "range", "return", "break", "continue",
            "nil", "true", "false", "make", "new", "len", "cap", "append", "string", "int", "int64", "float64",
            "bool", "byte", "error",
        ),
        "rust" to setOf(
            "fn", "let", "mut", "const", "static", "struct", "enum", "trait", "impl", "for", "while", "loop",
            "if", "else", "match", "return", "break", "continue", "use", "mod", "pub", "crate", "self", "Self",
            "super", "where", "async", "await", "move", "ref", "type", "unsafe", "as", "in", "true", "false",
            "Some", "None", "Ok", "Err",
        ),
        "json" to setOf("true", "false", "null"),
        "yaml" to setOf("true", "false", "null"),
    )

    private val hashCommentLangs = setOf("python", "bash", "yaml", "ruby", "perl", "shell", "sh", "dockerfile")
    private val slashCommentLangs = setOf(
        "kotlin", "java", "js", "ts", "typescript", "javascript", "c", "cpp", "c++", "go", "rust", "swift",
        "scala", "dart", "php", "cs", "csharp", "json",
    )

    fun highlight(code: String, language: String?, dark: Boolean): AnnotatedString {
        val lang = normalize(language)
        val palette = if (dark) Dark else Light
        val kw = keywords[lang].orEmpty()
        val allowSlashComment = lang.isEmpty() || lang in slashCommentLangs
        val allowHashComment = lang in hashCommentLangs

        val commentPattern = buildString {
            if (allowSlashComment) append("//[^\\n]*|/\\*[\\s\\S]*?\\*/")
            if (allowSlashComment && allowHashComment) append("|")
            if (allowHashComment) append("#[^\\n]*")
        }.ifEmpty { "(?!)" }

        val pattern = Regex(
            "($commentPattern)" +                                     // 1 comment
                "|(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`)" + // 2 string
                "|(@[A-Za-z_][A-Za-z0-9_]*)" +                        // 3 annotation
                "|\\b(0[xX][0-9a-fA-F]+|\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?[fFlLdD]?)\\b" + // 4 number
                "|\\b([A-Za-z_][A-Za-z0-9_]*)\\b",                    // 5 identifier
        )

        return buildAnnotatedString {
            var last = 0
            pattern.findAll(code).forEach { match ->
                val start = match.range.first
                if (start > last) append(code.substring(last, start))
                val text = match.value
                val style = when {
                    match.groups[1] != null -> SpanStyle(color = palette.comment)
                    match.groups[2] != null -> SpanStyle(color = palette.string)
                    match.groups[3] != null -> SpanStyle(color = palette.annotation)
                    match.groups[4] != null -> SpanStyle(color = palette.number)
                    text in kw -> SpanStyle(color = palette.keyword)
                    text.firstOrNull()?.isUpperCase() == true && text.length > 1 -> SpanStyle(color = palette.type)
                    else -> null
                }
                if (style != null) withStyle(style) { append(text) } else append(text)
                last = match.range.last + 1
            }
            if (last < code.length) append(code.substring(last))
        }
    }

    private fun normalize(language: String?): String = when (val raw = language?.trim()?.lowercase()) {
        null, "" -> ""
        "kt", "kts" -> "kotlin"
        "js", "javascript", "node" -> "js"
        "ts", "typescript", "tsx", "jsx" -> "js"
        "py", "python3" -> "python"
        "sh", "shell", "zsh", "console" -> "bash"
        "c++", "cc", "cxx", "h", "hpp" -> "cpp"
        "cs", "csharp", "c#" -> "java"
        "yml" -> "yaml"
        else -> raw
    }
}
