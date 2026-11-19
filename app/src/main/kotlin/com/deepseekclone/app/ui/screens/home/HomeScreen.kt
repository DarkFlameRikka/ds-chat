package com.deepseekclone.app.ui.screens.home

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.deepseekclone.app.R
import com.deepseekclone.app.data.AppContainer
import com.deepseekclone.app.data.model.Attachment
import com.deepseekclone.app.data.model.Conversation
import com.deepseekclone.app.ui.Zh
import com.deepseekclone.app.ui.components.ChatInputBar
import com.deepseekclone.app.ui.components.RoundSquareButton
import com.deepseekclone.app.ui.components.TwoLineGlyph
import com.deepseekclone.app.ui.theme.LocalExtendedColors
import kotlinx.coroutines.launch
import java.util.UUID

/** 首页：问候语 + 提示词胶囊 + 输入框（官方 App 的冷启动页）。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenDrawer: () -> Unit,
    onStartChat: (conversationId: String, text: String, voice: Boolean) -> Unit,
) {
    val settings = container.settingsRepository
    val store = container.conversationStore
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val model by settings.model.collectAsState(initial = "deepseek-flash")
    val effort by settings.effort.collectAsState(initial = "high")
    val search by settings.searchEnabled.collectAsState(initial = false)
    var showModelPicker by remember { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }
    var showAttachSheet by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var attachment by remember { mutableStateOf<Attachment?>(null) }

    fun toast(msg: String) {
        scope.launch { snackbar.showSnackbar(msg) }
    }

    fun startUpload(uri: Uri) {
        scope.launch {
            uploading = true
            try {
                val name = queryDisplayName(context, uri) ?: "附件"
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("无法读取文件")
                require(bytes.size <= 30 * 1024 * 1024) { "文件过大（上限 30MB）" }
                attachment = container.chatClient.uploadFile(name, mime, bytes).let { uploaded ->
                    val id = UUID.randomUUID().toString()
                    Attachment(
                        id = id,
                        localPath = runCatching {
                            com.deepseekclone.app.data.chat.AttachmentFiles.save(context, id, name, bytes)
                        }.getOrNull(),
                        name = uploaded.name,
                        mimeType = mime,
                        fileId = uploaded.id,
                        isImage = mime.startsWith("image/"),
                        sizeBytes = bytes.size.toLong(),
                    )
                }
            } catch (e: Exception) {
                toast("上传失败：${e.message}")
            } finally {
                uploading = false
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) startUpload(uri)
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) startUpload(uri)
    }

    fun startChat(voice: Boolean) {
        val text = input.trim()
        val current = attachment
        if (text.isEmpty() && current == null && !voice) {
            toast(Zh.emptyMessage)
            return
        }
        val now = System.currentTimeMillis()
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            title = text.lineSequence().firstOrNull()?.take(24)?.ifBlank { "新对话" } ?: "新对话",
            createdAt = now,
            updatedAt = now,
        )
        container.pendingAttachment = current
        input = ""
        attachment = null
        scope.launch {
            store.save(conversation)
            onStartChat(
                conversation.id,
                if (voice) "" else if (text.isEmpty()) "请分析这个附件" else text,
                voice,
            )
        }
    }

    fun send() {
        val text = input.trim()
        val current = attachment
        if (text.isEmpty() && current == null) {
            toast(Zh.emptyMessage)
            return
        }
        val now = System.currentTimeMillis()
        val conversation = Conversation(
            id = UUID.randomUUID().toString(),
            title = text.lineSequence().firstOrNull()?.take(24)?.ifBlank { "新对话" } ?: "新对话",
            createdAt = now,
            updatedAt = now,
        )
        container.pendingAttachment = current
        input = ""
        attachment = null
        scope.launch {
            store.save(conversation)
            onStartChat(conversation.id, if (text.isEmpty()) "请分析这个附件" else text, false)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeContent(
            input = input,
            onInputChange = { input = it },
            model = model,
            effort = effort,
            onOpenModelPicker = { showModelPicker = true },
            search = search,
            onSearchToggle = { scope.launch { settings.setSearchEnabled(!search) } },
            onSend = { send() },
            onAttach = { showAttachSheet = true },
            onVoice = { startChat(voice = true) },
            voiceMode = false,
            onVoiceModeChange = { enabled -> if (enabled) startChat(voice = true) },
            onOpenDrawer = onOpenDrawer,
            uploading = uploading,
            attachment = attachment,
            onRemoveAttachment = { attachment = null },
        )

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 170.dp),
        )

        if (showModelPicker) {
            com.deepseekclone.app.ui.screens.chat.ModelPickerSheet(
                currentModel = model,
                currentEffort = effort,
                onSelectModel = { id -> scope.launch { settings.setModel(id) } },
                onSelectEffort = { level -> scope.launch { settings.setEffort(level) } },
                onDismiss = { showModelPicker = false },
            )
        }

        if (showAttachSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAttachSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    SheetRow("从相册选择图片") {
                        showAttachSheet = false
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    SheetRow("选择文件（PDF / Office / 文本）") {
                        showAttachSheet = false
                        filePicker.launch(arrayOf("*/*"))
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

/** 无状态版本，便于截图回归测试。 */
@Composable
fun HomeContent(
    input: String,
    onInputChange: (String) -> Unit,
    model: String,
    effort: String,
    onOpenModelPicker: () -> Unit,
    search: Boolean,
    onSearchToggle: () -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    onVoice: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    voiceMode: Boolean = false,
    onVoiceModeChange: (Boolean) -> Unit = {},
    greeting: String = Zh.greeting,
    suggestions: List<String> = Zh.suggestions,
    uploading: Boolean = false,
    attachment: Attachment? = null,
    onRemoveAttachment: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RoundSquareButton(onClick = onOpenDrawer) {
                TwoLineGlyph(tint = LocalExtendedColors.current.iconTint)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.chat_welcome_logo),
                contentDescription = null,
                modifier = Modifier.height(64.dp),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = greeting,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(suggestions) { suggestion ->
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.clickable { onInputChange(suggestion) },
                ) {
                    Text(
                        text = suggestion,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ChatInputBar(
            value = input,
            onValueChange = onInputChange,
            model = model,
            effort = effort,
            onOpenModelPicker = onOpenModelPicker,
            search = search,
            onSearchChange = { onSearchToggle() },
            isStreaming = false,
            onSend = onSend,
            onStop = {},
            onAttach = onAttach,
            onVoice = onVoice,
            modifier = Modifier.navigationBarsPadding(),
            attachments = listOfNotNull(attachment),
            onRemoveAttachment = { onRemoveAttachment() },
            uploading = uploading,
            showDisclaimer = true,
            voiceMode = voiceMode,
            onVoiceModeChange = onVoiceModeChange,
        )
    }
}

internal fun queryDisplayName(context: android.content.Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
}.getOrNull()
