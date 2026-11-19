package com.deepseekclone.app.ui.screens.chat

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepseekclone.app.App
import com.deepseekclone.app.data.AppContainer
import com.deepseekclone.app.data.model.Attachment
import com.deepseekclone.app.data.model.ChatMessage
import com.deepseekclone.app.data.model.MessageStatus
import com.deepseekclone.app.data.model.Role
import com.deepseekclone.app.ui.Zh
import com.deepseekclone.app.ui.components.ChatInputBar
import com.deepseekclone.app.ui.components.TranscribingOverlay
import com.deepseekclone.app.ui.components.VoiceGradientOverlay
import com.deepseekclone.app.ui.components.ChatTopBar
import com.deepseekclone.app.ui.voice.PcmPlayer
import com.deepseekclone.app.ui.voice.VoiceRecorder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    container: AppContainer,
    conversationId: String,
    initialText: String?,
    startVoiceMode: Boolean = false,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onDeleted: () -> Unit,
) {
    val app = LocalContext.current.applicationContext as App
    val vm: ChatViewModel = viewModel(
        key = "chat_$conversationId",
        factory = ChatViewModel.factory(app, conversationId),
    )
    val state by vm.state.collectAsState()
    val model by vm.model.collectAsState()
    val effort by vm.effort.collectAsState()
    val search by vm.search.collectAsState()
    val autoRead by container.settingsRepository.autoReadAloud.collectAsState(initial = false)
    var lastSpokenId by remember { mutableStateOf<String?>(null) }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val recorder = remember { VoiceRecorder() }
    var input by rememberSaveable(conversationId) { mutableStateOf("") }
    var showModelPicker by remember { mutableStateOf(false) }
    var voiceMode by remember { mutableStateOf(startVoiceMode) }
    var holding by remember { mutableStateOf(false) }
    var finger by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var cancelHold by remember { mutableStateOf(false) }
    var holdStartY by remember { mutableStateOf(0f) }
    var transcribing by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<com.deepseekclone.app.data.model.ChatMessage?>(null) }
    var editDraft by remember { mutableStateOf("") }
    var showMore by remember { mutableStateOf(false) }
    var sentInitial by rememberSaveable(conversationId) { mutableStateOf(false) }
    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    var uploading by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var showAttachSheet by remember { mutableStateOf(false) }

    fun toast(msg: String) {
        scope.launch { snackbar.showSnackbar(msg) }
    }

    var pendingVoiceStart by remember { mutableStateOf(false) }

    fun beginHold(origin: androidx.compose.ui.geometry.Offset) {
        finger = origin
        holdStartY = origin.y
        cancelHold = false
        if (recorder.start()) {
            holding = true
        } else {
            toast("无法启动录音")
        }
    }

    fun finishHold(cancelled: Boolean) {
        holding = false
        val wav = recorder.stop()
        if (cancelled || cancelHold) {
            cancelHold = false
            return
        }
        if (wav == null) {
            toast("没有录到有效音频")
            return
        }
        scope.launch {
            transcribing = true
            try {
                val text = container.chatClient.transcribe(wav, "voice.wav")
                if (text.isNotBlank()) vm.send(text) else toast("没有识别到内容")
            } catch (e: Exception) {
                toast("语音识别失败：${e.message}")
            } finally {
                transcribing = false
            }
        }
    }

    val recordPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            pendingVoiceStart = false
        } else {
            toast("需要麦克风权限才能语音输入")
            voiceMode = false
            pendingVoiceStart = false
        }
    }

    val startUpload: (Uri) -> Unit = { uri ->
        scope.launch {
            uploading = true
            try {
                val name = queryDisplayName(context, uri) ?: "附件"
                val mime = context.contentResolver.getType(uri) ?: mimeFromName(name)
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("无法读取文件")
                require(bytes.size <= 30 * 1024 * 1024) { "文件过大（上限 30MB）" }
                val attachment = vm.uploadAttachment(name, mime, bytes)
                attachments = attachments + attachment
            } catch (e: Exception) {
                toast("上传失败：${e.message}")
            } finally {
                uploading = false
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? -> if (uri != null) startUpload(uri) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? -> if (uri != null) startUpload(uri) }

    // 自动朗读：一轮回答结束后朗读最新一条 AI 回复
    LaunchedEffect(state.streaming, autoRead) {
        if (!autoRead || state.streaming) return@LaunchedEffect
        val last = state.conversation?.messages
            ?.lastOrNull { it.role == com.deepseekclone.app.data.model.Role.ASSISTANT && it.status == com.deepseekclone.app.data.model.MessageStatus.DONE }
        if (last != null && last.id != lastSpokenId && last.content.isNotBlank()) {
            lastSpokenId = last.id
            scope.launch {
                val voice = container.settingsRepository.ttsVoiceFirst()
                runCatching { container.chatClient.synthesize(last.content, voice) }
                    .onSuccess { PcmPlayer.play(it) }
                    .onFailure { toast("自动朗读失败：${it.message}") }
            }
        }
    }

    LaunchedEffect(state.loaded) {
        if (!state.loaded) return@LaunchedEffect
        container.pendingAttachment?.let { pending ->
            attachments = listOf(pending)
            container.pendingAttachment = null
        }
        if (!sentInitial && !initialText.isNullOrBlank()) {
            sentInitial = true
            vm.send(initialText, attachments)
            attachments = emptyList()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ChatLayout(
            title = state.conversation?.title ?: Zh.newChatShort,
            messages = state.conversation?.messages.orEmpty(),
            streaming = state.streaming,
            input = input,
            onInputChange = { input = it },
            attachments = attachments,
            onRemoveAttachment = { target -> attachments = attachments.filterNot { it.id == target.id } },
            uploading = uploading,
            model = model,
            effort = effort,
            onOpenModelPicker = { showModelPicker = true },
            search = search,
            onSearchChange = { vm.setSearch(!search) },
            editingTitle = if (editTarget != null) "修改输入" else null,
            editDraft = editDraft,
            onEditDraftChange = { editDraft = it },
            onCancelEdit = { editTarget = null },
            onSend = {
                val target = editTarget
                if (target != null) {
                    val text = editDraft.trim()
                    if (text.isEmpty()) {
                        toast(Zh.emptyMessage)
                    } else {
                        editTarget = null
                        vm.resendFrom(target.id, text)
                    }
                } else if (input.isBlank() && attachments.isEmpty()) {
                    toast(Zh.emptyMessage)
                } else {
                    vm.send(input, attachments)
                    input = ""
                    attachments = emptyList()
                }
            },
            onStop = { vm.stop() },
            onAttach = { showAttachSheet = true },
            onVoice = { voiceMode = true },
            autoReadAloud = autoRead,
            cacheHitPercent = state.conversation?.cacheHitPercent,
            onToggleAutoRead = {
                scope.launch { container.settingsRepository.setAutoReadAloud(!autoRead) }
            },
            voiceMode = voiceMode,
            onVoiceModeChange = { enabled ->
                if (enabled) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO,
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        voiceMode = true
                    } else {
                        pendingVoiceStart = true
                        recordPermission.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            },
            onHoldStart = { origin ->
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    beginHold(origin)
                } else {
                    recordPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            onHoldMove = { origin ->
                finger = origin
                cancelHold = origin.y < holdStartY - 140f
            },
            onHoldEnd = { cancelled -> finishHold(cancelled) },
            transcribing = transcribing,
            onOpenDrawer = onOpenDrawer,
            onNewChat = onNewChat,
            onMore = { showMore = true },
            onCopy = { message ->
                clipboard.setText(AnnotatedString(message.content))
            },
            onRegenerate = { vm.regenerate() },
            onLike = { toast("感谢反馈：喜欢") },
            onDislike = { toast("感谢反馈：不喜欢") },
            onShare = { message ->
                runCatching {
                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, message.content)
                    }
                    context.startActivity(android.content.Intent.createChooser(send, Zh.share))
                }
            },
            onEditUserMessage = { message ->
                editTarget = message
                editDraft = message.content
            },
            onReadAloud = { message ->
                scope.launch {
                    val voice = container.settingsRepository.ttsVoiceFirst()
                    runCatching { container.chatClient.synthesize(message.content, voice) }
                        .onSuccess { PcmPlayer.play(it) }
                        .onFailure { toast("朗读失败：${it.message}") }
                }
            },
        )

        if (holding) {
            VoiceGradientOverlay(
                finger = finger,
                cancelled = cancelHold,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (transcribing) {
            TranscribingOverlay()
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 150.dp),
        )

        if (showAttachSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAttachSheet = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                Column(modifier = Modifier.padding(bottom = 24.dp)) {
                    MenuRow("从相册选择图片") {
                        showAttachSheet = false
                        photoPicker.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                            ),
                        )
                    }
                    MenuRow("选择文件（PDF / Office / 文本）") {
                        showAttachSheet = false
                        filePicker.launch(arrayOf("*/*"))
                    }
                }
            }
        }

        if (showModelPicker) {
            ModelPickerSheet(
                currentModel = model,
                currentEffort = effort,
                onSelectModel = { id -> vm.setModel(id) },
                onSelectEffort = { level -> vm.setEffort(level) },
                onDismiss = { showModelPicker = false },
            )
        }

        if (showMore) {
            MoreSheet(
                onDismiss = { showMore = false },
                onPickModel = {
                    showMore = false
                    showModelPicker = true
                },
                onOpenSettings = {
                    showMore = false
                    onOpenSettings()
                },
                onNewChat = {
                    showMore = false
                    onNewChat()
                },
                onDelete = {
                    showMore = false
                    vm.delete()
                    onDeleted()
                },
            )
        }
    }
}

/** 对话页布局（无状态，便于截图回归测试）。 */
@Composable
fun ChatLayout(
    title: String,
    messages: List<ChatMessage>,
    streaming: Boolean,
    input: String,
    onInputChange: (String) -> Unit,
    model: String,
    effort: String,
    onOpenModelPicker: () -> Unit,
    search: Boolean,
    onSearchChange: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onAttach: () -> Unit,
    onVoice: () -> Unit,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onMore: () -> Unit,
    onCopy: (ChatMessage) -> Unit,
    onRegenerate: () -> Unit,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onShare: (ChatMessage) -> Unit,
    modifier: Modifier = Modifier,
    showDisclaimer: Boolean = true,
    attachments: List<Attachment> = emptyList(),
    onRemoveAttachment: (Attachment) -> Unit = {},
    uploading: Boolean = false,
    onReadAloud: ((ChatMessage) -> Unit)? = null,
    onEditUserMessage: (ChatMessage) -> Unit = {},
    editingTitle: String? = null,
    editDraft: String = "",
    onEditDraftChange: (String) -> Unit = {},
    onCancelEdit: () -> Unit = {},
    autoReadAloud: Boolean = false,
    onToggleAutoRead: () -> Unit = {},
    cacheHitPercent: Float? = null,
    voiceMode: Boolean = false,
    onVoiceModeChange: (Boolean) -> Unit = {},
    onHoldStart: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    onHoldMove: (androidx.compose.ui.geometry.Offset) -> Unit = {},
    onHoldEnd: (Boolean) -> Unit = {},
    transcribing: Boolean = false,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    var selectionClearKey by remember { mutableStateOf(0) }
    val last = messages.lastOrNull()
    // 自动跟随底部：用户一旦手动滚动就暂停，回到底部再恢复（避免流式输出时和手指抢滚动）
    var autoFollow by remember { mutableStateOf(true) }
    var atBottom by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) autoFollow = false
        }
    }
    LaunchedEffect(listState) {
        snapshotFlow { !listState.canScrollForward }.collect { bottom ->
            atBottom = bottom
            if (bottom) autoFollow = true
        }
    }
    // 滚到真正的底部：最后一条消息可能高度超过一屏，仅 scrollToItem 只能对齐其顶部
    suspend fun scrollToBottom() {
        if (messages.isEmpty()) return
        listState.scrollToItem(messages.lastIndex)
        var guard = 0
        while (listState.canScrollForward && guard++ < 60) {
            listState.scrollBy(2000f)
        }
    }
    LaunchedEffect(messages.size, last?.content?.length ?: 0, last?.reasoning?.length ?: 0) {
        if (messages.isNotEmpty() && autoFollow) scrollToBottom()
    }

    androidx.compose.runtime.CompositionLocalProvider(
        com.deepseekclone.app.ui.components.LocalSelectionClearKey provides selectionClearKey,
    ) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ChatTopBar(
            title = title,
            onMenu = onOpenDrawer,
            onNewChat = onNewChat,
            onMore = onMore,
            autoReadAloud = autoReadAloud,
            onToggleAutoRead = onToggleAutoRead,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    // 聊天区轻点：清除文本选中并收起键盘（长按/滑动不触发）
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        val startTime = down.uptimeMillis
                        var maxDistance = 0f
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            maxDistance = maxOf(maxDistance, (change.position - down.position).getDistance())
                            if (!change.pressed) {
                                if (change.uptimeMillis - startTime < 350 && maxDistance < 24f) {
                                    selectionClearKey++
                                    focusManager.clearFocus()
                                }
                                break
                            }
                        }
                    }
                },
        ) {
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = Zh.emptyDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    items(messages, key = { it.id }) { message ->
                        when (message.role) {
                            Role.USER -> UserMessageBubble(
                                message = message,
                                onEdit = onEditUserMessage,
                                onCopy = { onCopy(it) },
                                onShare = { onShare(it) },
                            )
                            else -> AssistantMessage(
                                message = message,
                                isStreaming = streaming && message.status == MessageStatus.STREAMING,
                                onCopy = { onCopy(message) },
                                onRegenerate = onRegenerate,
                                onLike = onLike,
                                onDislike = onDislike,
                                onShare = { onShare(message) },
                                onReadAloud = onReadAloud?.let { callback -> { callback(message) } },
                            )
                        }
                    }
                }
            }
            if (messages.isNotEmpty() && !atBottom) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(34.dp)
                        .clickable {
                            autoFollow = true
                            scope.launch { scrollToBottom() }
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.KeyboardArrowDown,
                            contentDescription = Zh.goToBottom,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        ChatInputBar(
            value = if (editingTitle != null) editDraft else input,
            onValueChange = { if (editingTitle != null) onEditDraftChange(it) else onInputChange(it) },
            model = model,
            effort = effort,
            onOpenModelPicker = onOpenModelPicker,
            search = search,
            onSearchChange = { onSearchChange() },
            isStreaming = streaming,
            onSend = onSend,
            onStop = onStop,
            onAttach = onAttach,
            onVoice = onVoice,
            modifier = Modifier.navigationBarsPadding(),
            attachments = attachments,
            onRemoveAttachment = onRemoveAttachment,
            uploading = uploading,
            showDisclaimer = showDisclaimer,
            cacheHitPercent = cacheHitPercent,
            voiceMode = voiceMode,
            onVoiceModeChange = onVoiceModeChange,
            onHoldStart = onHoldStart,
            onHoldMove = onHoldMove,
            onHoldEnd = onHoldEnd,
            transcribing = transcribing,
            editingTitle = editingTitle,
            onCancelEdit = onCancelEdit,
        )
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreSheet(
    onDismiss: () -> Unit,
    onPickModel: () -> Unit,
    onOpenSettings: () -> Unit,
    onNewChat: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            MenuRow(Zh.modelPickerTitle, onClick = onPickModel)
            MenuRow(Zh.settings, onClick = onOpenSettings)
            MenuRow(Zh.newChatShort, onClick = onNewChat)
            MenuRow(Zh.delete, destructive = true, onClick = onDelete)
        }
    }
}

@Composable
private fun MenuRow(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
}.getOrNull()

private fun mimeFromName(name: String): String = when (name.substringAfterLast('.', "").lowercase()) {
    "png" -> "image/png"
    "jpg", "jpeg" -> "image/jpeg"
    "webp" -> "image/webp"
    "gif" -> "image/gif"
    "pdf" -> "application/pdf"
    "doc" -> "application/msword"
    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    "ppt" -> "application/vnd.ms-powerpoint"
    "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    "xls" -> "application/vnd.ms-excel"
    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    "txt" -> "text/plain"
    "md" -> "text/markdown"
    "json" -> "application/json"
    "csv" -> "text/csv"
    else -> "application/octet-stream"
}
