package com.deepseekclone.app.data.history

import com.deepseekclone.app.data.model.Attachment
import com.deepseekclone.app.data.model.ChatMessage
import com.deepseekclone.app.data.model.Citation
import com.deepseekclone.app.data.model.Conversation
import com.deepseekclone.app.data.model.MessageStatus
import com.deepseekclone.app.data.model.Role
import com.deepseekclone.app.data.model.ToolActivity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.InputStream
import java.time.Instant
import java.util.zip.ZipInputStream

/**
 * DeepSeek App 导出的历史记录 ZIP（export_format_version = 3）解析器。
 *
 * 归档结构（官方 chat-demo 同款格式）：
 * ```
 * manifest.json                                        {"export_format_version":3}
 * user.json                                            账号信息（跳过）
 * chat_sessions/{session_id}.json                      会话与消息树
 * chat_session_index.json                              会话索引
 * chat_sessions/{session_id}_files/{file_id}_{name}    图片原件（可选）
 * ```
 * 纯 JVM 实现（只依赖 InputStream），便于单测与服务端复用。
 */
class HistoryArchive {

    data class Parsed(
        val conversations: List<Conversation>,
        val skipped: Int,
        /** 附件 id → 图片原件字节（导入时由 Android 层落盘） */
        val imageBytes: Map<String, ByteArray>,
        val version: Int,
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(input: InputStream): Parsed {
        var entries = LinkedHashMap<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        // 兼容「整包套一层目录」的导出（deepseek_data-日期/xxx.json）
        if (!entries.containsKey("manifest.json")) {
            val prefix = entries.keys
                .firstOrNull { it.endsWith("/manifest.json") }
                ?.substringBeforeLast("/manifest.json")
                ?.plus("/")
            if (prefix != null) {
                entries = entries
                    .filterKeys { it.startsWith(prefix) }
                    .mapValues { it.value }
                    .entries
                    .associateTo(LinkedHashMap()) { it.key.removePrefix(prefix) to it.value }
            }
        }
        val manifest = entries["manifest.json"]
            ?: error("不是 DeepSeek 导出的历史记录：缺少 manifest.json")
        val version = runCatching {
            json.parseToJsonElement(manifest.decodeToString()).jsonObject["export_format_version"]
                ?.jsonPrimitive?.intOrNull
        }.getOrNull() ?: error("manifest.json 格式不正确")
        require(version == 3) { "暂不支持的历史记录格式版本：$version（需要 3）" }

        val images = entries.filterKeys { it.contains("_files/") }

        val sessionFiles = entries.keys
            .filter { it.startsWith("chat_sessions/") && it.endsWith(".json") && !it.contains("_files/") }
            .sorted()

        val conversations = mutableListOf<Conversation>()
        var skipped = 0
        sessionFiles.forEach { name ->
            val raw = entries[name] ?: return@forEach
            val parsed = runCatching { json.parseToJsonElement(raw.decodeToString()).jsonObject }
                .getOrNull()
            if (parsed == null) {
                skipped++
                return@forEach
            }
            val conversation = runCatching { toConversation(parsed, name, images, entries) }.getOrNull()
            if (conversation == null || conversation.messages.isEmpty()) {
                skipped++
            } else {
                conversations += conversation
            }
        }
        // 会话里引用到的图片原件：attachment.id → bytes
        val imageById = mutableMapOf<String, ByteArray>()
        conversations.flatMap { it.messages }.flatMap { it.attachments }.forEach { attachment ->
            if (!attachment.isImage) return@forEach
            val name = attachment.name
            val match = images.entries.firstOrNull { (key, _) ->
                key.endsWith("/${attachment.id}_$name") || key.endsWith("/${attachment.id}_")
            } ?: images.entries.firstOrNull { it.key.substringAfterLast('/').endsWith(name) }
            match?.let { imageById[attachment.id] = it.value }
        }

        return Parsed(
            conversations = conversations.sortedByDescending { it.updatedAt },
            skipped = skipped,
            imageBytes = imageById,
            version = version,
        )
    }

    private fun toConversation(
        session: JsonObject,
        fileName: String,
        images: Map<String, ByteArray>,
        allEntries: Map<String, ByteArray>,
    ): Conversation? {
        val id = session.str("id") ?: fileName.substringAfterLast('/').removeSuffix(".json")
        val messagesJson = session["messages"] as? JsonArray ?: JsonArray(emptyList())
        val messages = messagesJson.mapIndexedNotNull { index, element ->
            toMessage(element.jsonObject, index, id, images)
        }
        return Conversation(
            id = id,
            title = session.str("title")?.ifBlank { "未命名对话" } ?: "未命名对话",
            createdAt = parseTime(session.str("inserted_at")) ?: System.currentTimeMillis(),
            updatedAt = parseTime(session.str("updated_at")) ?: System.currentTimeMillis(),
            pinned = session["pinned"]?.jsonPrimitive?.contentOrNull == "true",
            messages = messages,
        )
    }

    private fun toMessage(
        message: JsonObject,
        index: Int,
        sessionId: String,
        images: Map<String, ByteArray>,
    ): ChatMessage? {
        val roleValue = message.str("role") ?: return null
        val role = if (roleValue.equals("USER", true)) Role.USER else Role.ASSISTANT
        val fragments = (message["fragments"] as? JsonArray)?.map { it.jsonObject } ?: emptyList()

        var content = ""
        var reasoning: String? = null
        var thinkSeconds: Int? = null
        val citations = mutableListOf<Citation>()
        val tools = mutableListOf<ToolActivity>()
        val attachments = mutableListOf<Attachment>()

        fragments.forEach { fragment ->
            when (fragment.str("type")) {
                "REQUEST", "RESPONSE", "TEMPLATE_RESPONSE" ->
                    content += fragment.str("content").orEmpty()

                "THINK" -> {
                    if (!fragment.str("content").isNullOrBlank()) {
                        reasoning = (reasoning ?: "") + fragment.str("content")
                    }
                    thinkSeconds = (fragment["elapsed_secs"] as? JsonPrimitive)?.intOrNull ?: thinkSeconds
                }

                "TOOL_SEARCH" -> {
                    val queries = (fragment["queries"] as? JsonArray)
                        ?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty()
                    val results = (fragment["results"] as? JsonArray)?.map { it.jsonObject }.orEmpty()
                    results.forEach { result ->
                        val url = result.str("url") ?: return@forEach
                        if (citations.none { it.url == url }) {
                            citations += Citation(url, result.str("title"), "")
                        }
                    }
                    tools += ToolActivity(
                        id = "$sessionId-$index-search",
                        kind = "SEARCH",
                        label = if (results.isEmpty()) "搜索 ${queries.joinToString("、")}" else "已阅读 ${results.size} 个网页",
                        done = true,
                        resultCount = results.size,
                    )
                }

                "TOOL_OPEN" -> {
                    val url = fragment.str("url")
                    val title = fragment.str("title")
                    if (url != null) {
                        tools += ToolActivity(
                            id = "$sessionId-$index-open-${tools.size}",
                            kind = "READ_LINK",
                            label = "已阅读 ${title ?: url}",
                            done = true,
                        )
                    }
                }

                "READ_LINK" -> {
                    (fragment["results"] as? JsonArray)?.forEach { item ->
                        val obj = item.jsonObject
                        val url = obj.str("url") ?: return@forEach
                        if (citations.none { it.url == url }) {
                            citations += Citation(url, obj.str("title"), "")
                        }
                    }
                }

                "FILE" -> {
                    (fragment["files"] as? JsonArray)?.forEach { item ->
                        val file = item.jsonObject
                        val fileId = file.str("id") ?: return@forEach
                        val name = file.str("file_name") ?: "附件"
                        val isImage = file["is_image"]?.jsonPrimitive?.contentOrNull == "true"
                        val exportPath = file.str("export_path")
                        val imageEntry = exportPath?.let { path -> images[path] ?: images.entries.firstOrNull { it.key.endsWith(path.substringAfterLast('/')) }?.value }
                        attachments += Attachment(
                            id = fileId,
                            name = name,
                            fileId = fileId,
                            isImage = isImage,
                            sizeBytes = (file["file_size"] as? JsonPrimitive)?.intOrNull?.toLong() ?: 0L,
                            localPath = null,
                        )
                    }
                }
            }
        }

        val status = when (message.str("status")) {
            "FINISHED" -> MessageStatus.DONE
            "INCOMPLETE", "WIP" -> MessageStatus.STOPPED
            "CONTENT_FILTER" -> MessageStatus.ERROR
            "FAILED", "TIMEOUT", "CONTEXT_LENGTH_EXCEEDED" -> MessageStatus.ERROR
            else -> MessageStatus.DONE
        }

        if (content.isBlank() && reasoning.isNullOrBlank() && attachments.isEmpty() && citations.isEmpty()) return null

        return ChatMessage(
            id = "$sessionId-${message.str("id") ?: index}",
            role = role,
            content = content,
            reasoning = reasoning,
            thinkSeconds = thinkSeconds,
            citations = citations,
            tools = tools,
            attachments = attachments,
            createdAt = parseTime(message.str("inserted_at")) ?: 0L,
            status = status,
        )
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun parseTime(value: String?): Long? = runCatching {
        value?.let { Instant.parse(it).toEpochMilli() }
    }.getOrNull()
}
