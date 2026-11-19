package com.deepseekclone.app.data.chat

import android.content.Context
import com.deepseekclone.app.data.model.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 本地会话仓库：全部会话以 JSON 落盘到 filesDir/conversations.json。
 * 目前是单用户本地存储，足够支撑第三方客户端的历史会话需求。
 */
class ConversationStore(context: Context) {

    private val file = File(context.filesDir, "conversations.json")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private val mutex = Mutex()
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        mutex.withLock {
            _conversations.value = runCatching {
                if (!file.exists()) emptyList()
                else json.decodeFromString<List<Conversation>>(file.readText())
            }.getOrDefault(emptyList()).sortedByDescending { it.updatedAt }
        }
    }

    fun get(id: String): Conversation? = _conversations.value.firstOrNull { it.id == id }

    suspend fun save(conversation: Conversation) = mutate { list ->
        list.filterNot { it.id == conversation.id } + conversation
    }

    suspend fun delete(id: String) = mutate { list -> list.filterNot { it.id == id } }

    suspend fun clearAll() = mutate { emptyList() }

    suspend fun setPinned(id: String, pinned: Boolean) = mutate { list ->
        list.map { if (it.id == id) it.copy(pinned = pinned) else it }
    }

    suspend fun rename(id: String, title: String) = mutate { list ->
        list.map { if (it.id == id) it.copy(title = title, updatedAt = System.currentTimeMillis()) else it }
    }

    private suspend fun mutate(transform: (List<Conversation>) -> List<Conversation>) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val next = transform(_conversations.value).sortedByDescending { it.updatedAt }
                _conversations.value = next
                runCatching { file.writeText(json.encodeToString(ListSerializer(Conversation.serializer()), next)) }
            }
        }
}
