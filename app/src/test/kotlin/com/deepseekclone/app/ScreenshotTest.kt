package com.deepseekclone.app

import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.deepseekclone.app.data.model.Attachment
import com.deepseekclone.app.data.model.ChatMessage
import com.deepseekclone.app.data.model.Citation
import com.deepseekclone.app.data.model.ToolActivity
import com.deepseekclone.app.data.model.Conversation
import com.deepseekclone.app.data.model.MessageStatus
import com.deepseekclone.app.data.model.Role
import com.deepseekclone.app.ui.components.ChatInputBar
import com.deepseekclone.app.ui.screens.chat.ChatLayout
import com.deepseekclone.app.ui.screens.history.HistoryDrawerContent
import com.deepseekclone.app.ui.screens.home.HomeContent
import com.deepseekclone.app.ui.theme.DeepSeekTheme
import org.junit.Rule
import org.junit.Test

/**
 * JVM 端截图回归（Paparazzi）：本机无 KVM，跑不了模拟器，
 * 用 layoutlib 渲染 Compose 界面来肉眼校验视觉效果。
 */
class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.Light.NoActionBar",
        showSystemUi = false,
    )

    private val userMessage = ChatMessage(
        id = "u1",
        role = Role.USER,
        content = "用 Kotlin 写一个快速排序，并简单说明时间复杂度",
        createdAt = 1L,
        status = MessageStatus.DONE,
    )

    private val aiMessage = ChatMessage(
        id = "a1",
        role = Role.ASSISTANT,
        reasoning = "用户想要一个 Kotlin 的快排实现。先给出简洁的递归写法，再说明平均/最坏复杂度，最后补充稳定性。",
        content = "可以用分治递归实现：\n\n" +
            "```kotlin\n" +
            "fun quickSort(list: List<Int>): List<Int> =\n" +
            "    if (list.size <= 1) list\n" +
            "    else {\n" +
            "        val pivot = list.first()\n" +
            "        quickSort(list.drop(1).filter { it < pivot }) +\n" +
            "            pivot +\n" +
            "            quickSort(list.drop(1).filter { it >= pivot })\n" +
            "    }\n" +
            "```\n\n" +
            "时间复杂度：\n" +
            "- 平均情况：**O(n log n)**\n" +
            "- 最坏情况：**O(n²)**，可通过随机化 pivot 规避\n\n" +
            "> 注意：快速排序不稳定，且递归深度较大时建议改用迭代或三路划分。",
        createdAt = 2L,
        status = MessageStatus.DONE,
        thinkSeconds = 7,
        tools = listOf(
            ToolActivity(id = "t1", kind = "SEARCH", label = "已阅读 5 个网页", done = true, resultCount = 5),
            ToolActivity(id = "t2", kind = "READ_LINK", label = "已阅读 深入理解快速排序", done = true),
        ),
        citations = listOf(
            Citation(url = "https://example.com/quicksort", title = "深入理解快速排序 - 算法笔记"),
            Citation(url = "https://example.com/complexity", title = "时间复杂度速查表"),
        ),
    )

    @Test
    fun homeLight() {
        paparazzi.snapshot {
            DeepSeekTheme(darkTheme = false) {
                HomeContent(
                    input = "",
                    onInputChange = {},
                    model = "deepseek-flash",
                    effort = "high",
                    onOpenModelPicker = {},
                    search = false,
                    onSearchToggle = {},
                    onSend = {},
                    onAttach = {},
                    onVoice = {},
                    onOpenDrawer = {},
                )
            }
        }
    }

    @Test
    fun homeDark() {
        paparazzi.snapshot {
            DeepSeekTheme(darkTheme = true) {
                HomeContent(
                    input = "帮我总结一下这篇文章",
                    onInputChange = {},
                    model = "deepseek-flash",
                    effort = "high",
                    onOpenModelPicker = {},
                    search = false,
                    onSearchToggle = {},
                    onSend = {},
                    onAttach = {},
                    onVoice = {},
                    onOpenDrawer = {},
                )
            }
        }
    }

    @Test
    fun chatLight() {
        paparazzi.snapshot {
            DeepSeekTheme(darkTheme = false) {
                ChatLayout(
                    title = "快速排序的时间复杂度",
                    messages = listOf(
                        userMessage.copy(
                            content = "用 Kotlin 写一个快速排序，并参考我上传的笔记",
                            attachments = listOf(
                                Attachment(id = "f1", name = "算法笔记.pdf", mimeType = "application/pdf", fileId = "file_1"),
                            ),
                        ),
                        aiMessage,
                    ),
                    streaming = false,
                    input = "",
                    onInputChange = {},
                    model = "deepseek-flash",
                    effort = "high",
                    onOpenModelPicker = {},
                    search = false,
                    onSearchChange = {},
                    onSend = {},
                    onStop = {},
                    onAttach = {},
                    onVoice = {},
                    onOpenDrawer = {},
                    onNewChat = {},
                    onMore = {},
                    onCopy = {},
                    onRegenerate = {},
                    onLike = {},
                    onDislike = {},
                    onShare = {},
                )
            }
        }
    }

    @Test
    fun chatStreamingDark() {
        paparazzi.snapshot {
            DeepSeekTheme(darkTheme = true) {
                ChatLayout(
                    title = "快速排序的时间复杂度",
                    messages = listOf(
                        userMessage,
                        aiMessage.copy(
                            id = "a2",
                            content = "可以用分治递归实现：\n\n```kotlin\nfun quickSort(list: List<Int>)",
                            status = MessageStatus.STREAMING,
                            thinkSeconds = null,
                        ),
                    ),
                    streaming = true,
                    input = "",
                    onInputChange = {},
                    model = "deepseek-flash",
                    effort = "high",
                    onOpenModelPicker = {},
                    search = false,
                    onSearchChange = {},
                    onSend = {},
                    onStop = {},
                    onAttach = {},
                    onVoice = {},
                    onOpenDrawer = {},
                    onNewChat = {},
                    onMore = {},
                    onCopy = {},
                    onRegenerate = {},
                    onLike = {},
                    onDislike = {},
                    onShare = {},
                )
            }
        }
    }

    @Test
    fun chatCitationsLight() {
        paparazzi.snapshot {
            DeepSeekTheme(darkTheme = false) {
                ChatLayout(
                    title = "今天 AI 圈有什么新闻",
                    messages = listOf(
                        ChatMessage(id = "u9", role = Role.USER, content = "今天 AI 圈有什么新闻？", createdAt = 1L, status = MessageStatus.DONE),
                        ChatMessage(
                            id = "a9",
                            role = Role.ASSISTANT,
                            content = "今天主要有两条：DeepSeek 开放了全家桶应用 API，以及几家厂商更新了模型。",
                            tools = listOf(ToolActivity(id = "t9", kind = "SEARCH", label = "已阅读 3 个网页", done = true, resultCount = 3)),
                            citations = listOf(
                                Citation(url = "https://example.com/1", title = "DeepSeek 开放全家桶应用 API"),
                                Citation(url = "https://example.com/2", title = "模型更新速览"),
                            ),
                            createdAt = 2L,
                            status = MessageStatus.DONE,
                        ),
                    ),
                    streaming = false,
                    input = "",
                    onInputChange = {},
                    model = "deepseek-flash",
                    effort = "low",
                    onOpenModelPicker = {},
                    search = true,
                    onSearchChange = {},
                    onSend = {},
                    onStop = {},
                    onAttach = {},
                    onVoice = {},
                    onOpenDrawer = {},
                    onNewChat = {},
                    onMore = {},
                    onCopy = {},
                    onRegenerate = {},
                    onLike = {},
                    onDislike = {},
                    onShare = {},
                )
            }
        }
    }

    @Test
    fun drawerLight() {
        val now = System.currentTimeMillis()
        val conversations = listOf(
            Conversation(id = "1", title = "快速排序的时间复杂度", updatedAt = now - 60_000, createdAt = now),
            Conversation(id = "2", title = "帮我把这段 Python 改成 Kotlin", updatedAt = now - 3 * 86_400_000L, createdAt = now),
            Conversation(id = "3", title = "周报怎么写更有条理", updatedAt = now - 10 * 86_400_000L, createdAt = now, pinned = true),
            Conversation(id = "4", title = "解释一下 Transformer 的注意力机制", updatedAt = now - 45 * 86_400_000L, createdAt = now),
        )
        paparazzi.snapshot {
            DeepSeekTheme(darkTheme = false) {
                androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.width(300.dp)) {
                    HistoryDrawerContent(
                        conversations = conversations,
                        currentConversationId = "1",
                        onSelect = {},
                        onNewChat = {},
                        onOpenSettings = {},
                        onDelete = {},
                        onPin = { _, _ -> },
                        onRename = { _, _ -> },
                    )
                }
            }
        }
    }

    @Test
    fun inputBarStates() {
        paparazzi.snapshot {
            DeepSeekTheme(darkTheme = false) {
                androidx.compose.foundation.layout.Column {
                    ChatInputBar(
                        value = "",
                        onValueChange = {},
                        model = "deepseek-flash",
                        effort = "high",
                        onOpenModelPicker = {},
                        search = false,
                        onSearchChange = {},
                        isStreaming = false,
                        onSend = {},
                        onStop = {},
                        onAttach = {},
                        onVoice = {},
                    )
                    ChatInputBar(
                        value = "帮我写一个 Ktor 的鉴权拦截器",
                        onValueChange = {},
                        model = "deepseek-flash",
                        effort = "high",
                        onOpenModelPicker = {},
                        search = true,
                        onSearchChange = {},
                        isStreaming = true,
                        onSend = {},
                        onStop = {},
                        onAttach = {},
                        onVoice = {},
                    )
                }
            }
        }
    }
}
