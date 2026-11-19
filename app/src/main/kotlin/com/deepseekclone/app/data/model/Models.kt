package com.deepseekclone.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Role {
    @SerialName("user") USER,
    @SerialName("assistant") ASSISTANT,
    @SerialName("system") SYSTEM,
}

@Serializable
enum class MessageStatus { SENDING, STREAMING, DONE, ERROR, STOPPED }

/** 正文引用（服务端把回答里的引用标记还原成来源）。 */
@Serializable
data class Citation(
    val url: String,
    val title: String? = null,
    val citedText: String = "",
)

/** 一次服务端工具调用（搜索 / 阅读链接），用于消息里展示「正在搜索…」。 */
@Serializable
data class ToolActivity(
    val id: String,
    val kind: String,
    val label: String,
    val done: Boolean = false,
    val resultCount: Int = 0,
    val error: String? = null,
)

/** Files API 上传过的附件。 */
@Serializable
data class Attachment(
    val id: String,
    val name: String,
    val mimeType: String = "",
    val fileId: String? = null,
    val isImage: Boolean = false,
    val sizeBytes: Long = 0,
    /** 本地副本路径（图片用于气泡预览；上传后仍保留） */
    val localPath: String? = null,
)

@Serializable
data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String = "",
    /** 深度思考模型的思维链（reasoning_content），普通模型为 null。 */
    val reasoning: String? = null,
    val createdAt: Long = 0L,
    val status: MessageStatus = MessageStatus.DONE,
    val error: String? = null,
    /** 深度思考耗时（秒），用于「已思考（用时 N 秒）」。 */
    val thinkSeconds: Int? = null,
    /** 附件（Files API 上传后引用）。 */
    val attachments: List<Attachment> = emptyList(),
    /** 引用的来源网页。 */
    val citations: List<Citation> = emptyList(),
    /** 服务端工具调用轨迹（搜索/阅读）。 */
    val tools: List<ToolActivity> = emptyList(),
)

@Serializable
data class Conversation(
    val id: String,
    val title: String = "新对话",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val pinned: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    /** 最近一轮请求的缓存命中率（0-100），无数据时为 null。 */
    val cacheHitPercent: Float? = null,
)

/** 会话分组：今天 / 7 天内 / 30 天内 / 更早 */
enum class ConversationGroup(val label: String) {
    TODAY("今天"),
    LAST_7_DAYS("7 天内"),
    LAST_30_DAYS("30 天内"),
    EARLIER("更早"),
}

fun Conversation.group(now: Long = System.currentTimeMillis()): ConversationGroup {
    val dayMs = 24L * 60 * 60 * 1000
    val startOfToday = now - (now % dayMs) // 粗略划分即可
    return when {
        updatedAt >= startOfToday -> ConversationGroup.TODAY
        updatedAt >= now - 7 * dayMs -> ConversationGroup.LAST_7_DAYS
        updatedAt >= now - 30 * dayMs -> ConversationGroup.LAST_30_DAYS
        else -> ConversationGroup.EARLIER
    }
}
