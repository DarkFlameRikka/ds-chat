package com.deepseekclone.app.data.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** 真实导出包（含图片、且整包套了一层目录）解析校验。 */
class HistoryArchiveImageTest {

    private fun parse(): HistoryArchive.Parsed {
        val zip = File("src/test/resources/history_export_with_image.zip")
        assumeTrue("缺少测试数据：${zip.absolutePath}", zip.exists())
        return zip.inputStream().use { HistoryArchive().parse(it) }
    }

    @Test
    fun `兼容套目录的导出包并解析图片附件`() {
        val parsed = parse()
        assertEquals(3, parsed.version)
        assertTrue("应至少解析出 4 个会话", parsed.conversations.size >= 3)
        assertTrue("应解析出图片原件", parsed.imageBytes.isNotEmpty())
        assertTrue(parsed.imageBytes.values.all { it.size > 1000 })
    }
}
