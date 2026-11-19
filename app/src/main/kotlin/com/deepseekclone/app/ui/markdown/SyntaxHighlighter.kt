package com.deepseekclone.app.ui.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.CodeHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxThemes

/**
 * 代码语法高亮：使用 dev.snipme:highlights（Kotlin Multiplatform，Apache-2.0，
 * mikepenz compose-markdown-renderer 等项目的底层实现），按库自带的 light/dark 主题上色。
 */
object SyntaxHighlighter {

    fun highlight(code: String, language: String?, dark: Boolean): AnnotatedString {
        if (code.isBlank()) return AnnotatedString(code)
        val result = runCatching {
            Highlights.Builder()
                .code(code)
                .language(toSyntaxLanguage(language))
                .theme(SyntaxThemes.default(dark))
                .build()
                .getHighlights()
        }.getOrNull() ?: return AnnotatedString(code)

        return buildAnnotatedString {
            append(code)
            result.forEach { highlight ->
                if (highlight is ColorHighlight) {
                    val start = highlight.location.start.coerceIn(0, code.length)
                    val end = highlight.location.end.coerceIn(start, code.length)
                    if (end > start) {
                        addStyle(
                            SpanStyle(color = Color(0xFF000000L or highlight.rgb.toLong())),
                            start,
                            end,
                        )
                    }
                }
            }
        }
    }

    private fun toSyntaxLanguage(language: String?): SyntaxLanguage {
        val raw = language?.trim()?.lowercase().orEmpty()
        return when (raw) {
            "kotlin", "kt", "kts" -> SyntaxLanguage.KOTLIN
            "java" -> SyntaxLanguage.JAVA
            "python", "py", "python3" -> SyntaxLanguage.PYTHON
            "javascript", "js", "node", "jsx" -> SyntaxLanguage.JAVASCRIPT
            "typescript", "ts", "tsx" -> SyntaxLanguage.TYPESCRIPT
            "bash", "sh", "shell", "zsh", "console" -> SyntaxLanguage.SHELL
            "c" -> SyntaxLanguage.C
            "cpp", "c++", "cc", "cxx", "h", "hpp" -> SyntaxLanguage.CPP
            "csharp", "cs", "c#" -> SyntaxLanguage.CSHARP
            "go", "golang" -> SyntaxLanguage.GO
            "rust", "rs" -> SyntaxLanguage.RUST
            "swift" -> SyntaxLanguage.SWIFT
            "ruby", "rb" -> SyntaxLanguage.RUBY
            "php" -> SyntaxLanguage.PHP
            "dart" -> SyntaxLanguage.DART
            "perl" -> SyntaxLanguage.PERL
            "coffeescript" -> SyntaxLanguage.COFFEESCRIPT
            else -> SyntaxLanguage.DEFAULT
        }
    }
}
