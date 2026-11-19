package com.deepseekclone.app.data.api

import com.deepseekclone.app.data.model.ChatMessage
import com.deepseekclone.app.data.model.Citation
import com.deepseekclone.app.data.model.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * DeepSeek 官方 API 客户端（Anthropic 兼容 Messages API，与官方 chat-demo 同款协议）。
 *
 * - 对话：POST {origin}/anthropic/v1/messages（SSE）
 *   声明 `deepseek_web_search` / `deepseek_read_url` 工具即可联网搜索、边想边搜、阅读链接；
 *   `safety_check` 开启基础风控；`title` 让服务端顺便生成会话标题。
 * - 文件：POST {origin}/anthropic/v1/files（需 anthropic-beta: files-api-2025-04-14）
 * - 语音：POST {origin}/v1/tools/asr、/v1/tools/tts
 *
 * 文档：https://api-docs.deepseek.com/zh-cn/api/create-message
 */
class DeepSeekClient(
    private val apiKeyProvider: suspend () -> String,
    private val baseUrlProvider: suspend () -> String,
) : ChatEngine {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val mediaType = "application/json; charset=utf-8".toMediaType()

    override fun streamChat(
        messages: List<ChatMessage>,
        model: String,
        effort: String,
        webSearchEnabled: Boolean,
    ): Flow<StreamEvent> = flow {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) {
            throw ApiException(401, "尚未配置 API Key", "请先在「设置 → API Key」中填入你的 DeepSeek API Key")
        }
        val origin = normalizeOrigin(baseUrlProvider())
        val body = buildRequestBody(messages, model, effort, webSearchEnabled)
        val hasFiles = body.toString().contains("\"file_id\"")
        val request = Request.Builder()
            .url("$origin/anthropic/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("Authorization", "Bearer $apiKey")
            .apply { if (hasFiles) addHeader("anthropic-beta", FILES_BETA) }
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "text/event-stream")
            .post(json.encodeToString(JsonObject.serializer(), body).toRequestBody(mediaType))
            .build()

        val call = http.newCall(request)
        currentCoroutineContext().job.invokeOnCompletion { call.cancel() }

        val response = try {
            call.execute()
        } catch (e: IOException) {
            throw ApiException(-1, friendlyNetworkMessage(e), e.message)
        }

        response.use { resp ->
            if (!resp.isSuccessful) {
                throw mapHttpError(resp.code, runCatching { resp.body?.string() }.getOrNull().orEmpty())
            }
            val source = resp.body?.source() ?: throw ApiException(-1, "服务端返回空响应", null)
            var finishReason: String? = null
            var stopReason: String? = null
            var ended = false
            while (!ended) {
                val line = source.readUtf8Line() ?: break
                if (line.isBlank() || line.startsWith(":") || line.startsWith("event:")) continue
                if (!line.startsWith("data:")) continue
                val payload = line.removePrefix("data:").trim()
                if (payload.isEmpty() || payload == "[DONE]") continue

                val root = runCatching { json.parseToJsonElement(payload).jsonObject }.getOrNull()
                    ?: continue
                root["error"]?.let { err ->
                    val message = err.jsonObject["message"]?.jsonPrimitive?.contentOrNull
                    throw ApiException(-1, "生成失败", message)
                }
                when (root.string("type")) {
                    "message_start" -> {
                        root["message"]?.jsonObject?.get("usage")?.jsonObject?.let { usage ->
                            emit(parseUsage(usage))
                        }
                    }
                    "content_block_start" -> {
                        val block = root["content_block"]?.jsonObject ?: continue
                        when (block.string("type")) {
                            "text" -> block.string("text")?.takeIf { it.isNotEmpty() }
                                ?.let { emit(StreamEvent.Content(it)) }

                            "thinking" -> block.string("thinking")?.takeIf { it.isNotEmpty() }
                                ?.let { emit(StreamEvent.Reasoning(it)) }

                            "server_tool_use" -> {
                                val id = block.string("id") ?: continue
                                val name = block.string("name").orEmpty()
                                val kind = decodeKind(block.string("kind"))
                                val input = block["input"]?.jsonObject
                                val label = when {
                                    input?.containsKey("queries") == true -> {
                                        val queries = input["queries"]?.jsonArray?.mapNotNull {
                                            it.jsonPrimitive.contentOrNull
                                        }.orEmpty()
                                        queries.joinToString("、")
                                    }
                                    input?.containsKey("pattern") == true ->
                                        ((input["url"]?.jsonPrimitive?.contentOrNull ?: "") + " · " + (input["pattern"]?.jsonPrimitive?.contentOrNull ?: ""))
                                    else -> input?.get("url")?.jsonPrimitive?.contentOrNull ?: name
                                }
                                emit(StreamEvent.ToolUse(id, kind, label))
                            }

                            "web_search_tool_result" -> {
                                val toolUseId = block.string("tool_use_id").orEmpty()
                                val kind = decodeKind(block.string("kind"))
                                val items = block["content"]?.jsonArray.orEmpty()
                                val failed = items.any { it.jsonObject.string("type") == "web_search_tool_result_error" }
                                val count = items.count { it.jsonObject.string("type") == "web_search_result" }
                                emit(
                                    StreamEvent.ToolResult(
                                        toolUseId = toolUseId,
                                        kind = kind,
                                        resultCount = count,
                                        pageTitle = null,
                                        error = if (failed) "联网搜索不可用" else null,
                                    ),
                                )
                            }

                            "web_fetch_tool_result" -> {
                                val toolUseId = block.string("tool_use_id").orEmpty()
                                val kind = decodeKind(block.string("kind"))
                                val content = block["content"]?.jsonObject
                                val failed = content?.string("type") == "web_fetch_tool_result_error"
                                val title = content?.get("content")?.jsonObject?.let {
                                    it["title"]?.jsonPrimitive?.contentOrNull
                                }
                                emit(
                                    StreamEvent.ToolResult(
                                        toolUseId = toolUseId,
                                        kind = kind,
                                        resultCount = 0,
                                        pageTitle = title,
                                        error = if (failed) "网页无法访问" else null,
                                    ),
                                )
                            }
                        }
                    }

                    "content_block_delta" -> {
                        val delta = root["delta"]?.jsonObject ?: continue
                        when (delta.string("type")) {
                            "text_delta" -> delta.string("text")?.let { emit(StreamEvent.Content(it)) }
                            "thinking_delta" -> delta.string("thinking")?.let { emit(StreamEvent.Reasoning(it)) }
                            "citations_delta" -> {
                                val c = delta["citation"]?.jsonObject ?: continue
                                val url = c.string("url") ?: continue
                                emit(
                                    StreamEvent.CitationDelta(
                                        Citation(
                                            url = url,
                                            title = c.string("title"),
                                            citedText = c.string("cited_text").orEmpty(),
                                        ),
                                    ),
                                )
                            }
                        }
                    }

                    "message_delta" -> {
                        root["usage"]?.jsonObject?.let { usage -> emit(parseUsage(usage)) }
                        val delta = root["delta"]?.jsonObject
                        stopReason = delta?.string("stop_reason")
                        val title = delta?.string("title")
                        if (title != null || stopReason != null) {
                            emit(StreamEvent.Meta(title = title, stopReason = stopReason))
                        }
                        finishReason = stopReason
                    }

                    "message_stop" -> {
                        ended = true
                    }
                }
            }
            emit(StreamEvent.Finished(finishReason))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun uploadFile(name: String, mimeType: String, bytes: ByteArray): UploadedFile =
        withContext(Dispatchers.IO) {
            val apiKey = requireKey()
            val origin = normalizeOrigin(baseUrlProvider())
            val mime = mimeType.ifBlank { "application/octet-stream" }
            val builder = MultipartBody.Builder().setType(MultipartBody.FORM)
            purposeFor(name, mime)?.let { builder.addFormDataPart("purpose", it) }
            builder.addFormDataPart("file", name, bytes.toRequestBody(mime.toMediaTypeOrNull()))
            val request = Request.Builder()
                .url("$origin/anthropic/v1/files")
                .addHeader("x-api-key", apiKey)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("anthropic-beta", FILES_BETA)
                .post(builder.build())
                .build()
            http.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw mapHttpError(resp.code, raw)
                val dto = json.decodeFromString(FileObjectDto.serializer(), raw)
                if (dto.id.isBlank()) throw ApiException(-1, "文件上传失败", raw.take(200))
                UploadedFile(dto.id, dto.filename.ifBlank { name })
            }
        }

    override suspend fun transcribe(audio: ByteArray, fileName: String): String = withContext(Dispatchers.IO) {
        val apiKey = requireKey()
        val origin = normalizeOrigin(baseUrlProvider())
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, audio.toRequestBody("audio/wav".toMediaTypeOrNull()))
            .build()
        val request = Request.Builder()
            .url("$origin/v1/tools/asr")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
        http.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw mapHttpError(resp.code, raw)
            json.decodeFromString(AsrResponse.serializer(), raw).text
        }
    }

    override suspend fun synthesize(text: String, voice: String): ByteArray = withContext(Dispatchers.IO) {
        val apiKey = requireKey()
        val origin = normalizeOrigin(baseUrlProvider())
        val payload = buildJsonObject {
            put("text", text)
            put("voice_id", voice)
            put("format", "pcm")
            put("filter_markdown", true)
        }
        val request = Request.Builder()
            .url("$origin/v1/tools/tts")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.encodeToString(JsonObject.serializer(), payload).toRequestBody(mediaType))
            .build()
        http.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw mapHttpError(resp.code, resp.body?.string().orEmpty())
            resp.body?.bytes() ?: throw ApiException(-1, "语音合成返回为空", null)
        }
    }

    override suspend fun listModels(): List<ModelInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("${normalizeOrigin(baseUrlProvider())}/models")
                .addHeader("Authorization", "Bearer ${apiKeyProvider()}")
                .get()
                .build()
            http.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw mapHttpError(resp.code, raw)
                json.decodeFromString(ModelsResponse.serializer(), raw).data
                    .map { ModelInfo(it.id, displayName(it.id), description(it.id)) }
                    .ifEmpty { defaultModels() }
            }
        }.getOrElse { defaultModels() }
    }

    override suspend fun balance(): BalanceInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("${normalizeOrigin(baseUrlProvider())}/user/balance")
            .addHeader("Authorization", "Bearer ${apiKeyProvider()}")
            .get()
            .build()
        http.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) throw mapHttpError(resp.code, raw)
            val parsed = json.decodeFromString(BalanceResponse.serializer(), raw)
            val item = parsed.balanceInfos.firstOrNull()
            BalanceInfo(
                currency = item?.currency?.uppercase() ?: "CNY",
                totalBalance = item?.totalBalance ?: "0",
                available = parsed.isAvailable,
            )
        }
    }

    override suspend fun validateKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext false
        val request = Request.Builder()
            .url("${normalizeOrigin(baseUrlProvider())}/models")
            .addHeader("Authorization", "Bearer $apiKey")
            .get()
            .build()
        try {
            http.newCall(request).execute().use { resp ->
                when {
                    resp.isSuccessful -> true
                    resp.code == 401 || resp.code == 403 -> false
                    else -> throw mapHttpError(resp.code, resp.body?.string().orEmpty())
                }
            }
        } catch (e: IOException) {
            throw ApiException(-1, "网络异常，请检查你的网络状况", e.message)
        }
    }

    private suspend fun requireKey(): String {
        val key = apiKeyProvider()
        if (key.isBlank()) throw ApiException(401, "尚未配置 API Key", null)
        return key
    }

    private fun buildRequestBody(
        messages: List<ChatMessage>,
        model: String,
        effort: String,
        webSearch: Boolean,
    ): JsonObject {
        val turns = normalizeTurns(messages)
        val normalizedEffort = normalizeEffort(effort)
        return buildJsonObject {
            put("model", normalizeModel(model))
            put("system", SYSTEM_PROMPT)
            put("thinking", buildJsonObject {
                put("type", if (normalizedEffort == "none") "disabled" else "enabled")
            })
            // Anthropic 格式：思考强度走 output_config.effort（none 表示关闭思考）
            put("output_config", buildJsonObject { put("effort", normalizedEffort) })
            put("safety_check", buildJsonObject { put("level", "high") })
            put("title", buildJsonObject { put("enabled", true) })
            putJsonArray("tools") {
                if (webSearch) {
                    add(buildJsonObject {
                        put("type", "deepseek_web_search")
                        put("name", "deepseek_web_search")
                    })
                }
                add(buildJsonObject {
                    put("type", "deepseek_read_url")
                    put("name", "deepseek_read_url")
                })
            }
            put("messages", buildJsonArray {
                turns.forEach { message ->
                    add(buildJsonObject {
                        put("role", message.first)
                        putJsonArray("content") {
                            message.second.forEach { block -> add(block) }
                        }
                    })
                }
            })
            put("stream", true)
        }
    }

    /**
     * 只保留 user / assistant 两侧交替的轮次：
     * - 空回答（失败/停止）整体丢弃，避免出现连续同角色消息；
     * - 相邻同角色消息合并成一条，满足 Messages API 的交替要求。
     */
    private fun normalizeTurns(messages: List<ChatMessage>): List<Pair<String, List<JsonObject>>> {
        val result = mutableListOf<Pair<String, MutableList<JsonObject>>>()
        messages.filter { it.role != Role.SYSTEM }.forEach { message ->
            val role = if (message.role == Role.USER) "user" else "assistant"
            val blocks = mutableListOf<JsonObject>()
            if (message.content.isNotBlank()) {
                blocks += buildJsonObject {
                    put("type", "text")
                    put("text", message.content)
                }
            }
            if (message.role == Role.ASSISTANT && blocks.isEmpty()) return@forEach
            if (message.role == Role.USER) {
                message.attachments.filter { it.fileId != null }.forEach { attachment ->
                    blocks += buildJsonObject {
                        put("type", if (attachment.isImage) "image" else "document")
                        put(
                            "source",
                            buildJsonObject {
                                put("type", "file")
                                put("file_id", attachment.fileId)
                            },
                        )
                    }
                }
            }
            if (blocks.isEmpty()) return@forEach
            val last = result.lastOrNull()
            if (last != null && last.first == role) {
                last.second += blocks
            } else {
                result += role to blocks
            }
        }
        return result.map { (role, blocks) ->
            role to blocks.map { block ->
                if (role == "assistant" && block.string("type") == "thinking") block else block
            }
        }
    }

    /** 区分「证书被拦截」与普通断网，给出可操作提示。 */
    private fun friendlyNetworkMessage(e: IOException): String {
        val text = "${e.message} ${e.cause?.message}".lowercase()
        val certIssue = e is javax.net.ssl.SSLException ||
            e.cause is java.security.cert.CertPathValidatorException ||
            text.contains("trust anchor") || text.contains("certpath") || text.contains("certificate")
        return if (certIssue) {
            "HTTPS 证书校验失败：网络中存在 TLS 拦截（常见于 Kaspersky 等杀毒软件、雷神/SteamTools 等加速器或公司代理），请放行 api.deepseek.com 或关闭 HTTPS 扫描后重试"
        } else {
            "网络异常，请检查你的网络状况"
        }
    }

    private fun mapHttpError(code: Int, raw: String): ApiException {
        val serverMessage = runCatching {
            json.decodeFromString(ErrorEnvelope.serializer(), raw).error?.message
        }.getOrNull()
        val friendly = when (code) {
            400 -> "请求参数有误"
            401 -> "API Key 无效，请检查后重试"
            402 -> "账户余额不足，请前往 DeepSeek 开放平台充值"
            422 -> "请求参数校验失败"
            429 -> "请求过于频繁，请稍后再试"
            500, 502, 503, 504 -> "当前服务异常，请稍后重试"
            else -> "请求失败（HTTP $code）"
        }
        return ApiException(code, friendly, serverMessage ?: raw.take(200).ifBlank { null })
    }

    private fun parseUsage(usage: JsonObject): StreamEvent.Usage = StreamEvent.Usage(
        inputTokens = usage.int("input_tokens"),
        cacheReadTokens = usage.int("cache_read_input_tokens"),
        cacheCreationTokens = usage.int("cache_creation_input_tokens"),
    )

    private fun JsonObject.int(key: String): Int =
        (this[key] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0

    private fun decodeKind(raw: String?): ToolKind = when (raw) {
        "deep_search" -> ToolKind.DEEP_SEARCH
        "read_link" -> ToolKind.READ_LINK
        "image_search" -> ToolKind.IMAGE_SEARCH
        "deep_image_search" -> ToolKind.DEEP_IMAGE_SEARCH
        else -> ToolKind.SEARCH
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())

    companion object {
        const val SYSTEM_PROMPT = "该助手为DeepSeek，由深度求索公司创造。"
        const val FILES_BETA = "files-api-2025-04-14"
        private val LAYOUT_RECOGNITION_EXTS = setOf("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx")

        /** 兼容旧的 base_url 写法（末尾带 /v1 时去掉）。 */
        fun normalizeOrigin(raw: String): String {
            var url = raw.trim().ifBlank { "https://api.deepseek.com" }
            while (url.endsWith("/")) url = url.dropLast(1)
            if (url.endsWith("/v1")) url = url.dropLast(3)
            return url
        }

        /** none / low / high / max（兼容官方别名）。 */
        fun normalizeEffort(effort: String): String = when (effort.lowercase()) {
            "none", "off", "disabled" -> "none"
            "minimal", "low" -> "low"
            "medium", "xhigh", "high", "" -> "high"
            "ultra", "max" -> "max"
            else -> "high"
        }

        /** 输入框上的模型徽标文案。 */
        fun effortLabel(effort: String): String = when (normalizeEffort(effort)) {
            "none" -> "不思考"
            "low" -> "快速思考"
            "max" -> "深度思考"
            else -> "标准思考"
        }

        fun normalizeModel(model: String): String = when (model) {
            "deepseek-chat", "chat", "quick", "default" -> "deepseek-flash"
            "deepseek-reasoner", "reasoner", "r1", "expert" -> "deepseek-v4-pro"
            "" -> "deepseek-flash"
            else -> model
        }

        fun displayName(id: String): String = when {
            id.startsWith("deepseek-v4-pro") -> "专家模式"
            id.startsWith("deepseek-flash") -> "快速模式"
            id.contains("vl") || id.contains("vision") -> "识图模式"
            else -> id
        }

        fun description(id: String): String = when {
            id.startsWith("deepseek-v4-pro") -> "适合复杂问题，推理更深入"
            id.startsWith("deepseek-flash") -> "日常对话，响应更快，支持思考与联网"
            else -> ""
        }

        fun defaultModels(): List<ModelInfo> = listOf(
            ModelInfo("deepseek-flash", "快速模式", "日常对话，响应更快，支持思考与联网"),
            ModelInfo("deepseek-v4-pro", "专家模式", "适合复杂问题，推理更深入"),
        )

        fun purposeFor(fileName: String, mimeType: String): String? {
            if (mimeType.startsWith("image/")) return null
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return if (ext in LAYOUT_RECOGNITION_EXTS) "rich_layout_recognition" else "user_data"
        }
    }
}
