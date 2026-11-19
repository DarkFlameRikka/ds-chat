package com.deepseekclone.app.data.api

import com.deepseekclone.app.data.model.ChatMessage
import com.deepseekclone.app.data.model.Role
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepSeekClientTest {

    private val server = MockWebServer()

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun client(): DeepSeekClient {
        server.start()
        return DeepSeekClient(
            apiKeyProvider = { "sk-test" },
            baseUrlProvider = { server.url("/").toString() },
        )
    }

    private val userMessage = ChatMessage(id = "1", role = Role.USER, content = "今天有什么新闻？")

    private fun sse(body: String) = MockResponse()
        .setHeader("Content-Type", "text/event-stream")
        .setBody(body)

    @Test
    fun `解析 thinking 搜索 引用 标题 的完整事件流`() = runTest {
        server.enqueue(
            sse(
                "event: message_start\n" +
                    "data: {\"type\":\"message_start\",\"message\":{\"id\":\"m1\",\"model\":\"deepseek-flash\"}}\n\n" +
                    "event: content_block_start\n" +
                    "data: {\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"thinking\",\"thinking\":\"\"}}\n\n" +
                    "event: content_block_delta\n" +
                    "data: {\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"thinking_delta\",\"thinking\":\"先想一下\"}}\n\n" +
                    "event: content_block_start\n" +
                    "data: {\"type\":\"content_block_start\",\"index\":1,\"content_block\":{\"type\":\"server_tool_use\",\"id\":\"srvtoolu_1\",\"name\":\"deepseek_web_search\",\"input\":{\"action\":\"search\",\"queries\":[\"DeepSeek API\"]},\"kind\":\"search\"}}\n\n" +
                    "event: content_block_start\n" +
                    "data: {\"type\":\"content_block_start\",\"index\":2,\"content_block\":{\"type\":\"web_search_tool_result\",\"tool_use_id\":\"srvtoolu_1\",\"kind\":\"search\",\"content\":[{\"type\":\"web_search_result\",\"url\":\"https://a.com\",\"title\":\"A 新闻\",\"snippet\":\"摘要\"}]}}\n\n" +
                    "event: content_block_start\n" +
                    "data: {\"type\":\"content_block_start\",\"index\":3,\"content_block\":{\"type\":\"text\",\"text\":\"\"}}\n\n" +
                    "event: content_block_delta\n" +
                    "data: {\"type\":\"content_block_delta\",\"index\":3,\"delta\":{\"type\":\"text_delta\",\"text\":\"今天有\"}}\n\n" +
                    "event: content_block_delta\n" +
                    "data: {\"type\":\"content_block_delta\",\"index\":3,\"delta\":{\"type\":\"text_delta\",\"text\":\"一条新闻\"}}\n\n" +
                    "event: content_block_delta\n" +
                    "data: {\"type\":\"content_block_delta\",\"index\":3,\"delta\":{\"type\":\"citations_delta\",\"citation\":{\"type\":\"web_search_result_location\",\"url\":\"https://a.com\",\"title\":\"A 新闻\",\"cited_text\":\"今天有一条新闻\"}}}\n\n" +
                    "event: message_delta\n" +
                    "data: {\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"end_turn\",\"stop_sequence\":null,\"title\":\"今日新闻\"}}\n\n" +
                    "event: message_stop\n" +
                    "data: {\"type\":\"message_stop\"}\n\n",
            ),
        )

        val events = client().streamChat(
            messages = listOf(userMessage),
            model = "deepseek-flash",
            effort = "high",
            webSearchEnabled = true,
        ).toList()

        assertEquals("先想一下", events.filterIsInstance<StreamEvent.Reasoning>().joinToString("") { it.text })
        assertEquals("今天有一条新闻", events.filterIsInstance<StreamEvent.Content>().joinToString("") { it.text })
        val toolUse = events.filterIsInstance<StreamEvent.ToolUse>().single()
        assertEquals("srvtoolu_1", toolUse.id)
        assertEquals("DeepSeek API", toolUse.label)
        val toolResult = events.filterIsInstance<StreamEvent.ToolResult>().single()
        assertEquals(1, toolResult.resultCount)
        assertEquals(1, events.filterIsInstance<StreamEvent.CitationDelta>().size)
        assertEquals("https://a.com", events.filterIsInstance<StreamEvent.CitationDelta>().first().citation.url)
        assertEquals("今日新闻", events.filterIsInstance<StreamEvent.Meta>().first().title)
        assertTrue(events.last() is StreamEvent.Finished)
        assertEquals("end_turn", (events.last() as StreamEvent.Finished).finishReason)
    }

    @Test
    fun `请求体包含官方同款参数与工具声明`() = runTest {
        server.enqueue(sse("event: message_stop\ndata: {\"type\":\"message_stop\"}\n\n"))
        client().streamChat(
            messages = listOf(userMessage),
            model = "deepseek-chat",
            effort = "max",
            webSearchEnabled = true,
        ).toList()

        val recorded = server.takeRequest()
        assertEquals("/anthropic/v1/messages", recorded.path)
        assertEquals("sk-test", recorded.getHeader("x-api-key"))
        val body = recorded.body.readUtf8()
        assertTrue(body.contains("\"deepseek-flash\""))            // 旧模型名归一化
        assertTrue(body.contains("\"deepseek_web_search\""))
        assertTrue(body.contains("\"deepseek_read_url\""))
        assertTrue(body.contains("\"safety_check\""))
        assertTrue(body.contains("\"title\""))
        assertTrue(body.contains("该助手为DeepSeek，由深度求索公司创造。"))
        assertTrue(body.contains("\"thinking\":{\"type\":\"enabled\"}"))
        assertTrue(body.contains("\"output_config\":{\"effort\":\"max\"}"))
    }

    @Test
    fun `401 映射为可读的 ApiException`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(401)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"error\":{\"message\":\"Authentication Fails\"}}"),
        )
        val error = runCatching {
            client().streamChat(listOf(userMessage), "deepseek-flash", "none", false).toList()
        }.exceptionOrNull()
        assertTrue(error is ApiException)
        assertEquals(401, (error as ApiException).code)
        assertEquals("API Key 无效，请检查后重试", error.message)
    }

    @Test
    fun `listModels 失败时回退到内置模型列表`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
        val models = client().listModels()
        assertEquals(listOf("deepseek-flash", "deepseek-v4-pro"), models.map { it.id })
    }

    @Test
    fun `文件用途映射符合官方规则`() {
        assertEquals("rich_layout_recognition", DeepSeekClient.purposeFor("报告.pdf", "application/pdf"))
        assertEquals("rich_layout_recognition", DeepSeekClient.purposeFor("a.docx", "application/octet-stream"))
        assertEquals("user_data", DeepSeekClient.purposeFor("main.kt", "text/plain"))
        assertEquals(null, DeepSeekClient.purposeFor("photo.jpg", "image/jpeg"))
    }
}
