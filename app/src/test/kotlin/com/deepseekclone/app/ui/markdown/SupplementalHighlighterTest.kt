package com.deepseekclone.app.ui.markdown

import org.junit.Assert.assertTrue
import org.junit.Test

/** 补位高亮：库不支持的语言（SQL / JSON / YAML / HTML / CSS）也要有颜色区间。 */
class SupplementalHighlighterTest {

    private fun colored(code: String, language: String): Set<String> {
        val annotated = SupplementalHighlighter.highlight(code, language, dark = false)
        val ranges = annotated.spanStyles.map { it.start to it.end }
        return ranges.map { (s, e) -> code.substring(s, e) }.toSet()
    }

    @Test
    fun `sql 关键字与字符串着色`() {
        val code = "SELECT name, COUNT(*) FROM users WHERE age > 18 -- 注释\nAND `x` = 'abc';"
        val colored = colored(code, "sql")
        assertTrue("关键字 SELECT 应着色: $colored", colored.any { it.equals("SELECT", true) })
        assertTrue("关键字 FROM 应着色: $colored", colored.any { it.equals("FROM", true) })
        assertTrue("字符串应着色: $colored", colored.any { it.contains("abc") })
        assertTrue("注释应着色: $colored", colored.any { it.startsWith("--") })
    }

    @Test
    fun `json 键与字面量着色`() {
        val code = """{"name": "deepseek", "ok": true, "n": 42}"""
        val colored = colored(code, "json")
        assertTrue("键 name 应着色: $colored", colored.any { it.contains("name") })
        assertTrue("字符串值应着色: $colored", colored.any { it.contains("deepseek") })
        assertTrue("true 应着色: $colored", colored.contains("true"))
        assertTrue("数字应着色: $colored", colored.contains("42"))
    }

    @Test
    fun `yaml 键与布尔着色`() {
        val code = "name: demo\nenabled: true\ncount: 3  # 注释"
        val colored = colored(code, "yaml")
        assertTrue("键 name 应着色: $colored", colored.contains("name"))
        assertTrue("true 应着色: $colored", colored.contains("true"))
        assertTrue("注释应着色: $colored", colored.any { it.contains("注释") })
    }

    @Test
    fun `html 标签与属性着色`() {
        val code = """<div class="box" id="a"><!-- c --><span>hi</span></div>"""
        val colored = colored(code, "html")
        assertTrue("标签应着色: $colored", colored.any { it.contains("div") })
        assertTrue("属性名应着色: $colored", colored.contains("class"))
        assertTrue("属性值应着色: $colored", colored.any { it.contains("box") })
        assertTrue("注释应着色: $colored", colored.any { it.startsWith("<!--") })
    }

    @Test
    fun `css 选择器与属性着色`() {
        val code = ".card { color: red; margin: 8px; }"
        val colored = colored(code, "css")
        assertTrue("选择器应着色: $colored", colored.any { it.contains("card") })
        assertTrue("属性应着色: $colored", colored.contains("color"))
        assertTrue("数值应着色: $colored", colored.contains("8px"))
    }

    @Test
    fun `支持的语言判定`() {
        listOf("sql", "json", "yaml", "yml", "html", "xml", "css", "SQL", "Json").forEach {
            assertTrue("应支持 $it", SupplementalHighlighter.supports(it))
        }
        listOf("kotlin", "python", null).forEach {
            assertTrue("不应接管 $it", !SupplementalHighlighter.supports(it))
        }
    }
}
