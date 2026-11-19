package com.deepseekclone.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseekclone.app.data.api.ApiException
import com.deepseekclone.app.data.api.ChatEngine
import com.deepseekclone.app.data.settings.SettingsRepository
import com.deepseekclone.app.ui.Zh
import com.deepseekclone.app.ui.components.SimpleTopBar
import kotlinx.coroutines.launch

/** 增量功能：配置 DeepSeek API Key（官方 App 走账号登录，本项目走 API Key）。 */
@Composable
fun ApiKeyScreen(
    settings: SettingsRepository,
    client: ChatEngine,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var key by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("https://api.deepseek.com") }
    var visible by remember { mutableStateOf(false) }
    var advanced by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    LaunchedEffect(Unit) {
        key = settings.apiKeyFirst()
        baseUrl = settings.baseUrlFirst()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
    ) {
        SimpleTopBar(title = Zh.apiKeyTitle, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = Zh.apiKeyLocalOnly,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = key,
                onValueChange = { key = it.trim() },
                label = { Text(Zh.apiKey) },
                placeholder = { Text(Zh.apiKeyHint) },
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = null,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = Zh.getApiKey,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (advanced) "收起" else Zh.advanced,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { advanced = !advanced }
                    .padding(vertical = 6.dp),
            )
            if (advanced) {
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it.trim() },
                    label = { Text(Zh.baseUrl) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "默认 https://api.deepseek.com，兼容自建/代理网关",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = {
                        if (key.isBlank()) {
                            status = false to "请先填写 API Key"
                            return@OutlinedButton
                        }
                        testing = true
                        status = null
                        scope.launch {
                            try {
                                val ok = client.validateKey(key)
                                status = ok to if (ok) Zh.testSuccess else "API Key 无效"
                            } catch (e: ApiException) {
                                status = false to String.format(Zh.testFailed, e.message ?: "未知错误")
                            } catch (e: Exception) {
                                status = false to String.format(Zh.testFailed, e.message ?: "未知错误")
                            } finally {
                                testing = false
                            }
                        }
                    },
                ) {
                    if (testing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(Zh.testConnection)
                    }
                }
                Spacer(Modifier.weight(1f))
            }
            status?.let { (ok, message) ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch {
                        settings.setApiKey(key.trim())
                        settings.setBaseUrl(baseUrl.trim())
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(Zh.save, fontSize = 16.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "提示：DeepSeek「全家桶应用 API」（2026-11-13 开放）已支持联网搜索、文件输入、" +
                    "链接阅读、语音（ASR/TTS）等能力。注意这些工具按次/按时长额外计费：" +
                    "搜索与抓取约 5 元/千次，ASR 约 0.39 元/小时，TTS 约 1.5 元/万字符（以官方价格页为准）。",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
