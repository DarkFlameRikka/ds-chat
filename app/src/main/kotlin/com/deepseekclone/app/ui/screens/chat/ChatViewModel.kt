package com.deepseekclone.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.deepseekclone.app.App
import com.deepseekclone.app.data.api.ApiException
import com.deepseekclone.app.data.api.StreamEvent
import com.deepseekclone.app.data.model.Attachment
import com.deepseekclone.app.data.model.ChatMessage
import com.deepseekclone.app.data.model.Conversation
import com.deepseekclone.app.data.model.MessageStatus
import com.deepseekclone.app.data.model.Role
import com.deepseekclone.app.data.model.ToolActivity
import com.deepseekclone.app.ui.Zh
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val conversation: Conversation? = null,
    val streaming: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
)

class ChatViewModel(
    private val app: App,
    private val conversationId: String,
) : ViewModel() {

    private val store = app.container.conversationStore
    private val client = app.container.chatClient
    private val settings = app.container.settingsRepository

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    /** deepseek-flash（快速）/ deepseek-v4-pro（专家）。 */
    val model = MutableStateFlow("deepseek-flash")
    /** 思考强度：none / low / high / max */
    val effort = MutableStateFlow("high")
    val search = MutableStateFlow(false)

    private var streamJob: Job? = null
    private var lastSavedAt = 0L

    init {
        viewModelScope.launch {
            store.load()
            _state.update { it.copy(conversation = store.get(conversationId) ?: newConversation(), loaded = true) }
        }
        viewModelScope.launch {
            model.value = normalizeModel(settings.modelFirst())
            effort.value = settings.effortFirst().ifBlank { "high" }
            search.value = settings.searchFirst()
        }
    }

    fun setModel(value: String) {
        val normalized = normalizeModel(value)
        model.value = normalized
        viewModelScope.launch { settings.setModel(normalized) }
    }

    fun setEffort(value: String) {
        effort.value = value
        viewModelScope.launch {
            settings.setEffort(value)
            settings.setThinkingEnabled(value != "none")
        }
    }

    fun setSearch(value: Boolean) {
        search.value = value
        viewModelScope.launch { settings.setSearchEnabled(value) }
    }

    fun send(text: String, attachments: List<Attachment> = emptyList()) {
        val content = text.trim()
        if ((content.isEmpty() && attachments.isEmpty()) || _state.value.streaming) return
        val conv = _state.value.conversation ?: newConversation()
        val now = System.currentTimeMillis()
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = Role.USER,
            content = content.ifEmpty { "（附件）" },
            attachments = attachments,
            createdAt = now,
            status = MessageStatus.DONE,
        )
        val assistant = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = Role.ASSISTANT,
            content = "",
            createdAt = now,
            status = MessageStatus.STREAMING,
        )
        val title = if (conv.messages.none { it.role == Role.USER }) {
            content.lineSequence().firstOrNull()?.take(24)?.ifBlank { "新对话" } ?: "新对话"
        } else {
            conv.title
        }
        val updated = conv.copy(title = title, messages = conv.messages + userMessage + assistant, updatedAt = now)
        _state.update { it.copy(conversation = updated, streaming = true, error = null) }
        persist(updated, force = true)
        startStream(updated, assistant.id)
    }

    /** 修改某条提问并重新生成：截断该条之后的消息，再用新文本发送。 */
    fun resendFrom(messageId: String, newText: String) {
        val conv = _state.value.conversation ?: return
        if (_state.value.streaming) return
        val index = conv.messages.indexOfFirst { it.id == messageId }
        if (index < 0) return
        val trimmed = conv.copy(
            messages = conv.messages.take(index),
            updatedAt = System.currentTimeMillis(),
        )
        _state.update { it.copy(conversation = trimmed) }
        send(newText)
    }

    fun stop() {
        streamJob?.cancel()
    }

    fun regenerate() {
        val conv = _state.value.conversation ?: return
        if (_state.value.streaming) return
        val lastUserIndex = conv.messages.indexOfLast { it.role == Role.USER }
        if (lastUserIndex < 0) return
        val assistant = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = Role.ASSISTANT,
            content = "",
            createdAt = System.currentTimeMillis(),
            status = MessageStatus.STREAMING,
        )
        val updated = conv.copy(
            messages = conv.messages.take(lastUserIndex + 1) + assistant,
            updatedAt = System.currentTimeMillis(),
        )
        _state.update { it.copy(conversation = updated, streaming = true, error = null) }
        persist(updated, force = true)
        startStream(updated, assistant.id)
    }

    fun delete() {
        val conv = _state.value.conversation ?: return
        viewModelScope.launch { store.delete(conv.id) }
    }

    /** 上传附件并返回可引用的 Attachment。 */
    suspend fun uploadAttachment(name: String, mimeType: String, bytes: ByteArray): Attachment {
        val uploaded = client.uploadFile(name, mimeType, bytes)
        val id = UUID.randomUUID().toString()
        val localPath = runCatching {
            com.deepseekclone.app.data.chat.AttachmentFiles.save(app, id, name, bytes)
        }.getOrNull()
        return Attachment(
            id = id,
            localPath = localPath,
            name = uploaded.name,
            mimeType = mimeType,
            fileId = uploaded.id,
            isImage = mimeType.startsWith("image/"),
            sizeBytes = bytes.size.toLong(),
        )
    }

    private fun startStream(conv: Conversation, assistantId: String) {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            val history = conv.messages.filter { it.id != assistantId }
            var thinkingStartedAt = 0L
            var thinkSeconds: Int? = null
            var turnInput = 0
            var turnCacheRead = 0
            var turnCacheCreation = 0
            try {
                client.streamChat(
                    messages = history,
                    model = model.value,
                    effort = effort.value,
                    webSearchEnabled = search.value,
                ).collect { event ->
                    when (event) {
                        is StreamEvent.Reasoning -> {
                            if (thinkingStartedAt == 0L) thinkingStartedAt = System.currentTimeMillis()
                            patch(assistantId) { it.copy(reasoning = (it.reasoning ?: "") + event.text) }
                            persistThrottled()
                        }

                        is StreamEvent.Content -> {
                            if (thinkingStartedAt != 0L && thinkSeconds == null) {
                                thinkSeconds = ((System.currentTimeMillis() - thinkingStartedAt) / 1000).toInt().coerceAtLeast(1)
                            }
                            patch(assistantId) { it.copy(content = it.content + event.text) }
                            persistThrottled()
                        }

                        is StreamEvent.CitationDelta -> {
                            patch(assistantId) { message ->
                                if (message.citations.any { it.url == event.citation.url }) message
                                else message.copy(citations = message.citations + event.citation)
                            }
                            persistThrottled()
                        }

                        is StreamEvent.ToolUse -> {
                            patch(assistantId) { message ->
                                message.copy(
                                    tools = message.tools + ToolActivity(
                                        id = event.id,
                                        kind = event.kind.name,
                                        label = labelFor(event.kind, event.label),
                                    ),
                                )
                            }
                            persistThrottled()
                        }

                        is StreamEvent.ToolResult -> {
                            patch(assistantId) { message ->
                                message.copy(
                                    tools = message.tools.map { tool ->
                                        if (tool.id != event.toolUseId) tool
                                        else tool.copy(
                                            done = true,
                                            resultCount = event.resultCount,
                                            label = finishedLabel(tool.kind, event),
                                            error = event.error,
                                        )
                                    },
                                )
                            }
                            persistThrottled()
                        }

                        is StreamEvent.Usage -> {
                            turnInput = maxOf(turnInput, event.inputTokens)
                            turnCacheRead = maxOf(turnCacheRead, event.cacheReadTokens)
                            turnCacheCreation = maxOf(turnCacheCreation, event.cacheCreationTokens)
                            val total = turnInput + turnCacheRead + turnCacheCreation
                            if (total > 0) {
                                val percent = turnCacheRead * 100f / total
                                _state.update { st ->
                                    val current = st.conversation ?: return@update st
                                    st.copy(conversation = current.copy(cacheHitPercent = percent))
                                }
                            }
                        }

                        is StreamEvent.Meta -> {
                            if (event.title != null) {
                                _state.update { st ->
                                    val current = st.conversation ?: return@update st
                                    st.copy(conversation = current.copy(title = event.title))
                                }
                            }
                            if (event.stopReason == "refusal") {
                                patch(assistantId) {
                                    it.copy(status = MessageStatus.ERROR, error = "内容被安全策略拦截（refusal）")
                                }
                            }
                        }

                        is StreamEvent.Finished -> {
                            patch(assistantId) {
                                it.copy(
                                    status = if (it.content.isBlank() && it.reasoning.isNullOrBlank()) MessageStatus.ERROR else MessageStatus.DONE,
                                    error = if (it.content.isBlank() && it.reasoning.isNullOrBlank()) "未收到有效回答，请重试" else it.error,
                                    thinkSeconds = thinkSeconds ?: it.thinkSeconds,
                                )
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                patch(assistantId) { it.copy(status = MessageStatus.STOPPED) }
                throw e
            } catch (e: ApiException) {
                patch(assistantId) {
                    it.copy(
                        status = MessageStatus.ERROR,
                        error = listOfNotNull(e.message, e.detail).joinToString("："),
                    )
                }
            } catch (e: Exception) {
                patch(assistantId) {
                    it.copy(status = MessageStatus.ERROR, error = e.message ?: Zh.operationFailed)
                }
            } finally {
                _state.update { it.copy(streaming = false) }
                _state.value.conversation?.let { persist(it, force = true) }
            }
        }
    }

    private fun labelFor(kind: com.deepseekclone.app.data.api.ToolKind, detail: String): String = when (kind) {
        com.deepseekclone.app.data.api.ToolKind.READ_LINK -> "正在阅读 $detail"
        com.deepseekclone.app.data.api.ToolKind.DEEP_SEARCH -> "正在深度搜索 $detail"
        com.deepseekclone.app.data.api.ToolKind.IMAGE_SEARCH -> "正在搜索图片 $detail"
        else -> "正在搜索 $detail"
    }

    private fun finishedLabel(kind: String, event: StreamEvent.ToolResult): String = when {
        event.error != null -> event.error
        kind == "READ_LINK" -> event.pageTitle?.let { "已阅读 $it" } ?: "已阅读网页"
        else -> if (event.resultCount > 0) "已阅读 ${event.resultCount} 个网页" else "未搜索到相关网页"
    }

    private fun patch(assistantId: String, transform: (ChatMessage) -> ChatMessage) {
        _state.update { st ->
            val conv = st.conversation ?: return@update st
            st.copy(
                conversation = conv.copy(
                    messages = conv.messages.map { if (it.id == assistantId) transform(it) else it },
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    private fun persistThrottled() {
        val now = System.currentTimeMillis()
        if (now - lastSavedAt < 800) return
        lastSavedAt = now
        _state.value.conversation?.let { conv -> app.appScope.launch { store.save(conv) } }
    }

    private fun persist(conv: Conversation, force: Boolean) {
        if (!force && System.currentTimeMillis() - lastSavedAt < 800) return
        lastSavedAt = System.currentTimeMillis()
        app.appScope.launch { store.save(conv) }
    }

    private fun newConversation(): Conversation {
        val now = System.currentTimeMillis()
        return Conversation(id = conversationId, title = "新对话", createdAt = now, updatedAt = now)
    }

    private fun normalizeModel(value: String): String = when (value) {
        "deepseek-chat", "chat", "quick", "default", "" -> "deepseek-flash"
        "deepseek-reasoner", "reasoner", "r1", "expert" -> "deepseek-v4-pro"
        else -> value
    }

    companion object {
        fun factory(app: App, conversationId: String) = viewModelFactory {
            initializer { ChatViewModel(app, conversationId) }
        }
    }
}
