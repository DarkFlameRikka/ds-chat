package com.deepseekclone.app.data.update

import com.deepseekclone.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    /** 远端最新版本号（已去掉前导 v，如 1.0.0） */
    val latestVersion: String,
    /** Release 页面地址 */
    val releaseUrl: String,
    /** Release 说明 */
    val notes: String,
    /** 是否比当前版本新 */
    val hasUpdate: Boolean,
    /** 当前安装版本 */
    val currentVersion: String,
)

/**
 * 检查更新：读取 GitHub Releases 的最新 tag，与本地 versionName 比较。
 * 仓库是公开的，无需任何 token（未鉴权 API 限 60 次/小时/IP，够用）。
 */
class UpdateChecker(
    private val currentVersion: String = BuildConfig.VERSION_NAME,
    private val repository: String = DEFAULT_REPO,
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun check(): UpdateInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repository/releases/latest")
            .addHeader("Accept", "application/vnd.github+json")
            .addHeader("User-Agent", "DS-Chat-Android")
            .get()
            .build()
        val body = try {
            http.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException(
                        when (response.code) {
                            404 -> "仓库还没有发布版本"
                            403 -> "检查更新过于频繁，请稍后再试"
                            else -> "检查更新失败（HTTP ${response.code}）"
                        },
                    )
                }
                text
            }
        } catch (e: IOException) {
            throw IOException(e.message ?: "网络异常，请检查网络后重试", e)
        }
        val dto = json.decodeFromString(ReleaseDto.serializer(), body)
        val latest = dto.tagName.trim().removePrefix("v")
        UpdateInfo(
            latestVersion = latest.ifBlank { currentVersion },
            releaseUrl = dto.htmlUrl,
            notes = dto.body.orEmpty(),
            hasUpdate = compareVersion(latest, currentVersion) > 0,
            currentVersion = currentVersion,
        )
    }

    internal fun compareVersion(a: String, b: String): Int {
        fun parts(v: String) = v.split('.', '-', '+')
            .map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val pa = parts(a)
        val pb = parts(b)
        val size = maxOf(pa.size, pb.size)
        for (i in 0 until size) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    @Serializable
    private data class ReleaseDto(
        @kotlinx.serialization.SerialName("tag_name") val tagName: String = "",
        @kotlinx.serialization.SerialName("html_url") val htmlUrl: String = "",
        val body: String? = null,
    )

    companion object {
        const val DEFAULT_REPO = "DarkFlameRikka/ds-chat"
    }
}
