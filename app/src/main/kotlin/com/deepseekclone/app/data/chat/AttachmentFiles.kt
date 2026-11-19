package com.deepseekclone.app.data.chat

import android.content.Context
import java.io.File

/** 附件本地副本：上传后保留一份，便于在消息气泡里展示图片缩略图。 */
object AttachmentFiles {

    fun save(context: Context, id: String, fileName: String, bytes: ByteArray): String {
        val dir = File(context.filesDir, "attachments").apply { mkdirs() }
        val ext = fileName.substringAfterLast('.', "").lowercase().ifBlank { "bin" }
        val file = File(dir, "$id.$ext")
        file.writeBytes(bytes)
        return file.absolutePath
    }
}
