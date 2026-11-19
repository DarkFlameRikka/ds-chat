package com.deepseekclone.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseekclone.app.BuildConfig
import com.deepseekclone.app.ui.Zh
import com.deepseekclone.app.ui.components.SimpleTopBar
import com.deepseekclone.app.ui.theme.LocalExtendedColors
import kotlinx.coroutines.launch

/** 设置页：对齐官方 2.8.x 的分组卡片 + 图标行样式。 */
@Composable
fun SettingsScreen(
    settings: com.deepseekclone.app.data.settings.SettingsRepository,
    onBack: () -> Unit,
    onEditApiKey: () -> Unit,
    onOpenFontSize: () -> Unit = {},
    onOpenApiKeyGuide: () -> Unit = {},
    container: com.deepseekclone.app.data.AppContainer? = null,
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showImportGuide by remember { mutableStateOf(false) }
    var checkingUpdate by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<com.deepseekclone.app.data.update.UpdateInfo?>(null) }
    var updateError by remember { mutableStateOf<String?>(null) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    val importPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val importer = container?.historyImporter ?: return@rememberLauncherForActivityResult
        scope.launch {
            importing = true
            importMessage = null
            runCatching { importer.import(uri) }
                .onSuccess { result ->
                    importMessage = "导入完成：新增 ${result.imported} 个会话" +
                        (if (result.duplicated > 0) "，已存在 ${result.duplicated} 个" else "") +
                        (if (result.skipped > 0) "，跳过 ${result.skipped} 个" else "")
                }
                .onFailure { importMessage = "导入失败：${it.message}" }
            importing = false
        }
    }
    val apiKey by settings.apiKey.collectAsState(initial = "")
    val model by settings.model.collectAsState(initial = "deepseek-flash")
    val theme by settings.darkTheme.collectAsState(initial = "light")
    val fontStep by settings.fontStep.collectAsState(initial = "2")
    val voice by settings.ttsVoice.collectAsState(initial = "stella")
    val autoRead by settings.autoReadAloud.collectAsState(initial = false)
    var showLanguage by remember { mutableStateOf(false) }
    var showVoice by remember { mutableStateOf(false) }
    var showDisclaimer by remember { mutableStateOf(false) }

    val extended = LocalExtendedColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        SimpleTopBar(title = Zh.settings, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            SectionHeader(Zh.sectionAccount)
            SettingsCard {
                SettingRow(
                    icon = Icons.Outlined.VpnKey,
                    title = Zh.apiKey,
                    value = if (apiKey.isBlank()) Zh.apiKeyMissing else Zh.apiKeyConfigured,
                    valueColor = if (apiKey.isBlank()) MaterialTheme.colorScheme.error else extended.textSecondary,
                    onClick = onEditApiKey,
                )
                RowDivider()
                SettingRow(
                    icon = Icons.Outlined.Add,
                    title = "申请 API Key",
                    value = "图文教程",
                    onClick = onOpenApiKeyGuide,
                )
                RowDivider()
                SettingRow(
                    icon = Icons.Outlined.FileDownload,
                    title = "导入历史记录",
                    value = if (importing) "导入中..." else null,
                    onClick = { showImportGuide = true },
                )
            }

            SectionHeader(Zh.sectionApp)
            SettingsCard {
                SettingRow(
                    icon = Icons.Outlined.Language,
                    title = Zh.language,
                    value = "简体中文",
                    onClick = { showLanguage = true },
                )
                RowDivider()
                SettingRow(
                    icon = Icons.Outlined.LightMode,
                    title = Zh.appearance,
                    value = when (theme) {
                        "light" -> Zh.themeLight
                        "dark" -> Zh.themeDark
                        else -> Zh.themeSystem
                    },
                    onClick = null,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (theme) {
                                    "light" -> Zh.themeLight
                                    "dark" -> Zh.themeDark
                                    else -> Zh.themeSystem
                                },
                                fontSize = 16.sp,
                                color = extended.textSecondary,
                            )
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Icon(
                                    Icons.Outlined.KeyboardArrowUp,
                                    contentDescription = "切换",
                                    tint = extended.textSecondary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            scope.launch { settings.setDarkTheme(previousTheme(theme)) }
                                        },
                                )
                                Icon(
                                    Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = "切换",
                                    tint = extended.textSecondary,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable {
                                            scope.launch { settings.setDarkTheme(nextTheme(theme)) }
                                        },
                                )
                            }
                        }
                    },
                )
                RowDivider()
                SettingRow(
                    icon = Icons.Outlined.TextFields,
                    title = Zh.fontSize,
                    value = fontLabelOf(fontStep),
                    onClick = onOpenFontSize,
                )
            }

            SectionHeader(Zh.sectionVoice)
            SettingsCard {
                SettingRow(
                    icon = Icons.Outlined.VolumeUp,
                    title = "自动朗读",
                    value = if (autoRead) "已开启" else "已关闭",
                    onClick = null,
                    trailing = {
                        androidx.compose.material3.Switch(
                            checked = autoRead,
                            onCheckedChange = { enabled -> scope.launch { settings.setAutoReadAloud(enabled) } },
                        )
                    },
                )
                RowDivider()
                SettingRow(
                    icon = Icons.Outlined.VolumeUp,
                    title = Zh.ttsVoice,
                    value = if (voice == "tide") "Tide" else "Stella",
                    onClick = { showVoice = true },
                )
            }

            SectionHeader(Zh.sectionAbout)
            SettingsCard {
                SettingRow(
                    icon = Icons.Outlined.Info,
                    title = Zh.checkUpdate,
                    value = BuildConfig.VERSION_NAME,
                    onClick = {
                        val checker = container?.updateChecker
                        if (checker == null) {
                            updateError = "当前环境不支持检查更新"
                            showUpdateDialog = true
                            return@SettingRow
                        }
                        showUpdateDialog = true
                        checkingUpdate = true
                        updateResult = null
                        updateError = null
                        scope.launch {
                            runCatching { checker.check() }
                                .onSuccess { updateResult = it }
                                .onFailure { updateError = it.message ?: Zh.networkError }
                            checkingUpdate = false
                        }
                    },
                )
                RowDivider()
                SettingRow(
                    icon = Icons.Outlined.Info,
                    title = "免责声明",
                    onClick = { showDisclaimer = true },
                )
            }

            Text(
                text = Zh.thirdPartyNote + " · 非官方应用，仅供个人学习使用",
                fontSize = 12.sp,
                color = extended.textSecondary.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
            )
        }
    }

    if (showLanguage) {
        RadioDialog(
            title = Zh.language,
            options = listOf("zh" to "简体中文"),
            current = "zh",
            onDismiss = { showLanguage = false },
            onPick = { showLanguage = false },
        )
    }

    if (showVoice) {
        RadioDialog(
            title = Zh.ttsVoice,
            options = listOf("stella" to "Stella（女声）", "tide" to "Tide（男声）"),
            current = voice,
            onDismiss = { showVoice = false },
            onPick = { key ->
                scope.launch { settings.setTtsVoice(key) }
                showVoice = false
            },
        )
    }

    if (showUpdateDialog) {
        val info = updateResult
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text(Zh.checkUpdate) },
            text = {
                when {
                    checkingUpdate -> Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("正在检查更新…")
                    }

                    updateError != null -> Text(updateError!!)

                    info != null && !info.hasUpdate -> Text(
                        "当前已是最新版本\n\n当前版本：${info.currentVersion}\n最新版本：v${info.latestVersion}",
                    )

                    info != null -> Text(
                        "发现新版本 v${info.latestVersion}（当前 ${info.currentVersion}）" +
                            if (info.notes.isNotBlank()) "\n\n${info.notes.take(400)}" else "",
                    )

                    else -> Text("暂无更新信息")
                }
            },
            confirmButton = {
                val openUrl: (String) -> Unit = { url ->
                    runCatching {
                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(url),
                        )
                        context.startActivity(intent)
                    }
                }
                when {
                    info != null && info.hasUpdate -> TextButton(onClick = {
                        showUpdateDialog = false
                        openUrl(info.releaseUrl)
                    }) { Text("前往下载") }

                    else -> TextButton(onClick = { showUpdateDialog = false }) { Text("知道了") }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUpdateDialog = false
                    checkingUpdate = false
                }) { Text(Zh.cancel) }
            },
        )
    }

    if (showImportGuide) {
        AlertDialog(
            onDismissRequest = { showImportGuide = false },
            title = { Text("导入历史记录") },
            text = {
                Text(
                    "支持导入 DeepSeek App 导出的历史对话 ZIP 文件（deepseek_data-日期.zip）。\n\n" +
                        "导出方法：\n" +
                        "1. 打开 DeepSeek App\n" +
                        "2. 进入「设置」→「数据管理」\n" +
                        "3. 点击「导出所有历史对话」→「导出」\n" +
                        "4. 复制下载链接，下载完成后即可导入",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImportGuide = false
                    importPicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                }) { Text("选择 ZIP 文件") }
            },
            dismissButton = {
                TextButton(onClick = { showImportGuide = false }) { Text(Zh.cancel) }
            },
        )
    }

    importMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { importMessage = null },
            title = { Text("导入历史记录") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { importMessage = null }) { Text("知道了") } },
        )
    }

    if (showDisclaimer) {
        AlertDialog(
            onDismissRequest = { showDisclaimer = false },
            title = { Text("免责声明") },
            text = { Text(Zh.disclaimerLong, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { TextButton(onClick = { showDisclaimer = false }) { Text("知道了") } },
        )
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = LocalExtendedColors.current.textSecondary,
        modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 8.dp),
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LocalExtendedColors.current.cardBg,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, LocalExtendedColors.current.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column { content() }
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(
        color = LocalExtendedColors.current.divider,
        thickness = 0.6.dp,
        modifier = Modifier.padding(start = 56.dp),
    )
}

@Composable
fun SettingRow(
    title: String,
    value: String? = null,
    valueColor: Color = LocalExtendedColors.current.textSecondary,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val extended = LocalExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = extended.iconTint, modifier = Modifier.size(21.dp))
            Spacer(Modifier.width(18.dp))
        }
        Text(title, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            trailing()
        } else {
            if (value != null) {
                Text(value, fontSize = 16.sp, color = valueColor)
                Spacer(Modifier.width(6.dp))
            }
            if (onClick != null) {
                Icon(
                    Icons.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = extended.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun RadioDialog(
    title: String,
    options: List<Pair<String, String>>,
    current: String,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                options.forEach { (key, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(key) }
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(selected = current == key, onClick = { onPick(key) })
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(Zh.cancel) } },
    )
}

private fun nextTheme(current: String): String = when (current) {
    "system" -> "light"
    "light" -> "dark"
    else -> "system"
}

private fun previousTheme(current: String): String = when (current) {
    "system" -> "dark"
    "dark" -> "light"
    else -> "system"
}
