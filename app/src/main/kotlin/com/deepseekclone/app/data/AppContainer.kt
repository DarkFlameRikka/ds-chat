package com.deepseekclone.app.data

import android.content.Context
import com.deepseekclone.app.data.api.ChatEngine
import com.deepseekclone.app.data.api.DeepSeekClient
import com.deepseekclone.app.data.chat.ConversationStore
import com.deepseekclone.app.data.settings.SettingsRepository

/** 手动依赖注入容器：App 级单例。 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val settingsRepository = SettingsRepository(appContext)

    val conversationStore = ConversationStore(appContext)

    /** 首页选择附件后暂存，进入对话页时消费一次。 */
    var pendingAttachment: com.deepseekclone.app.data.model.Attachment? = null

    val historyImporter = com.deepseekclone.app.data.history.HistoryImporter(appContext, conversationStore)

    val chatClient: ChatEngine = DeepSeekClient(
        apiKeyProvider = { settingsRepository.apiKeyFirst() },
        baseUrlProvider = { settingsRepository.baseUrlFirst() },
    )
}
