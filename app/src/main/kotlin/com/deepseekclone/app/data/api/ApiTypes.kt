package com.deepseekclone.app.data.api

import com.deepseekclone.app.data.model.ChatMessage
import com.deepseekclone.app.data.model.Citation
import kotlinx.coroutines.flow.Flow

/** 服务端联网能力种类（对应 API 的 kind 字段）。 */
enum class ToolKind {
    SEARCH, DEEP_SEARCH, READ_LINK, IMAGE_SEARCH, DEEP_IMAGE_SEARCH
}

/** Messages API 流式事件。 */
sealed interface StreamEvent {
    /** 思考过程增量（thinking_delta）。 */
    data class Reasoning(val text: String) : StreamEvent

    /** 正文增量（text_delta）。 */
    data class Content(val text: String) : StreamEvent

    /** 正文引用（citations_delta）。 */
    data class CitationDelta(val citation: Citation) : StreamEvent

    /** 服务端发起工具调用（搜索 / 阅读链接 / 页内查找）。 */
    data class ToolUse(val id: String, val kind: ToolKind, val label: String) : StreamEvent

    /** 工具返回结果。 */
    data class ToolResult(
        val toolUseId: String,
        val kind: ToolKind,
        val resultCount: Int,
        val pageTitle: String?,
        val error: String?,
    ) : StreamEvent

    /** message_delta 中的会话标题与结束原因。 */
    data class Meta(val title: String?, val stopReason: String?) : StreamEvent

    /** 本轮的 token 用量（用于计算缓存命中率）。 */
    data class Usage(
        val inputTokens: Int,
        val cacheReadTokens: Int,
        val cacheCreationTokens: Int,
    ) : StreamEvent

    data class Finished(val finishReason: String?) : StreamEvent
}

data class ModelInfo(
    val id: String,
    val displayName: String,
    val description: String,
)

data class BalanceInfo(
    val currency: String,
    val totalBalance: String,
    val available: Boolean,
)

data class UploadedFile(
    val id: String,
    val name: String,
)

class ApiException(
    val code: Int,
    override val message: String,
    val detail: String? = null,
) : Exception(message)

/**
 * 聊天引擎抽象：UI 只依赖本接口。
 * 官方 App 同款能力（联网搜索、文件输入、链接阅读、思考、标题）由 [DeepSeekClient] 实现。
 */
interface ChatEngine {
    fun streamChat(
        messages: List<ChatMessage>,
        model: String,
        effort: String,
        webSearchEnabled: Boolean,
    ): Flow<StreamEvent>

    /** Files API：上传附件，返回 file_id 供消息引用。 */
    suspend fun uploadFile(name: String, mimeType: String, bytes: ByteArray): UploadedFile

    suspend fun listModels(): List<ModelInfo>

    suspend fun balance(): BalanceInfo

    suspend fun validateKey(apiKey: String): Boolean

    /** ASR：语音转文字（WAV/OGG，≤120s，≤8MiB）。 */
    suspend fun transcribe(audio: ByteArray, fileName: String): String

    /** TTS：返回裸 PCM（24kHz 单声道 16bit 小端）。 */
    suspend fun synthesize(text: String, voice: String = "stella"): ByteArray
}
