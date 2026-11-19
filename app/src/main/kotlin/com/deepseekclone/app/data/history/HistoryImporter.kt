package com.deepseekclone.app.data.history

import android.content.Context
import android.net.Uri
import com.deepseekclone.app.data.chat.AttachmentFiles
import com.deepseekclone.app.data.chat.ConversationStore
import java.io.InputStream

/**
 * 把 DeepSeek App 导出的历史记录 ZIP 导入本地会话库。
 * 已存在的会话（同 id）跳过，不覆盖本地改动。
 */
class HistoryImporter(
    private val context: Context,
    private val store: ConversationStore,
) {

    data class Result(
        val total: Int,
        val imported: Int,
        val duplicated: Int,
        val skipped: Int,
        val images: Int,
    )

    suspend fun import(input: InputStream): Result {
        val parsed = HistoryArchive().parse(input)
        store.load()
        val existing = store.conversations.value.map { it.id }.toSet()

        var imported = 0
        var duplicated = 0
        var images = 0

        parsed.conversations.forEach { conversation ->
            if (conversation.id in existing) {
                duplicated++
                return@forEach
            }
            val patched = conversation.copy(
                messages = conversation.messages.map { message ->
                    message.copy(
                        attachments = message.attachments.map { attachment ->
                            val bytes = parsed.imageBytes[attachment.id]
                            if (bytes != null) {
                                val path = runCatching {
                                    AttachmentFiles.save(context, attachment.id, attachment.name, bytes)
                                }.getOrNull()
                                if (path != null) images++
                                attachment.copy(localPath = path)
                            } else {
                                attachment
                            }
                        },
                    )
                },
            )
            store.save(patched)
            imported++
        }
        return Result(
            total = parsed.conversations.size,
            imported = imported,
            duplicated = duplicated,
            skipped = parsed.skipped,
            images = images,
        )
    }

    suspend fun import(uri: Uri): Result {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("无法读取所选文件")
        return stream.use { import(it) }
    }
}
