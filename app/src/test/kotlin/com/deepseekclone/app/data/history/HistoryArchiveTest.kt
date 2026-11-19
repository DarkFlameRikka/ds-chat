package com.deepseekclone.app.data.history

import com.deepseekclone.app.data.model.Role
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

class HistoryArchiveTest {

    private fun parse(): HistoryArchive.Parsed {
        val zip = File("src/test/resources/history_export.zip")
        assumeTrue("缺少测试用导出数据：${zip.absolutePath}", zip.exists())
        return zip.inputStream().use { HistoryArchive().parse(it) }
    }

    @Test
    fun `解析 DeepSeek 导出的历史记录`() {
        val parsed = parse()
        assertEquals(3, parsed.version)
        assertEquals(3, parsed.conversations.size)
        assertEquals(0, parsed.skipped)
        assertTrue(parsed.conversations.all { it.messages.size == 2 })
        assertTrue(parsed.conversations.any { it.title == "简单问候" })
    }

    @Test
    fun `消息角色与正文正确`() {
        val conversation = parse().conversations.first { it.title == "询问数字2含义" }
        val user = conversation.messages.first { it.role == Role.USER }
        val assistant = conversation.messages.first { it.role == Role.ASSISTANT }
        assertEquals("2", user.content)
        assertTrue(assistant.content.contains("你好"))
        assertTrue(conversation.updatedAt > 0)
    }
}
