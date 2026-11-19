package com.deepseekclone.app.ui

/**
 * 界面文案。全部取自官方 APK 的 res/values-b+zh+Hans/strings.xml
 * （见 docs/strings-zh.md），保持 1:1。
 */
object Zh {
    // 首页
    const val greeting = "今天有什么可以帮到你？"
    const val newChat = "开启新对话"
    const val newChatShort = "新建对话"
    const val inputPlaceholder = "发消息"
    const val inputPlaceholderVoice = "发消息或按住说话"
    const val disclaimer = "回答由 AI 生成，仅供参考"
    const val send = "发送"
    const val sending = "发送中"
    const val generating = "生成中"
    const val stop = "停止"
    const val attachFiles = "上传文件"
    const val voiceInput = "按住说话"
    const val thinking = "深度思考"
    const val webSearch = "智能搜索"

    // 消息
    const val thinkingNow = "正在思考"
    const val searching = "正在搜索"
    const val searchDone = "已阅读 %1\$d 个网页"
    const val copy = "复制"
    const val copyOk = "已复制"
    const val regenerate = "重新生成"
    const val like = "喜欢"
    const val dislike = "不喜欢"
    const val share = "分享"
    const val goToBottom = "转至底部"
    const val thinkDuration = "已思考（用时 %1\$d 秒）"
    const val stopped = "已停止"

    // 侧边栏
    const val openSidebar = "打开侧边栏"
    const val closeSidebar = "关闭侧边栏"
    const val searchHint = "搜索对话内容..."
    const val emptyTitle = "暂无对话"
    const val emptyDescription = "你与 DeepSeek 的对话\n将展示在这里"
    const val pinned = "置顶"
    const val unpin = "取消置顶"
    const val rename = "重命名"
    const val delete = "删除"
    const val cancel = "取消"
    const val confirm = "确认"
    const val settings = "设置"

    // 分组
    const val groupToday = "今天"
    const val groupYesterday = "昨天"
    const val group7Days = "7 天内"
    const val group30Days = "30 天内"
    const val groupEarlier = "更早"

    // 模型
    const val modelPickerTitle = "选择模型"
    const val modelQuick = "快速模式"
    const val modelQuickDesc = "适合日常对话，即时响应"
    const val modelExpert = "专家模式"
    const val modelExpertDesc = "擅长复杂问题，高峰需等待"
    const val modelVision = "识图模式"
    const val modelVisionDesc = "擅长理解图片内容"
    const val modelWelcome = "使用%1\$s开始对话"

    // 首页推荐提示词
    val suggestions = listOf("总结", "解释一下", "分析一下", "怎么办", "这是什么", "答案是什么")

    // 错误
    const val networkError = "网络异常，请检查你的网络状况"
    const val serverError = "当前服务异常，请稍后重试"
    const val tooManyRequests = "请求过于频繁，请稍后再试"
    const val emptyMessage = "不能发送空白消息"
    const val operationFailed = "操作失败，请稍后重试"

    // 设置
    const val sectionAccount = "账户"
    const val sectionApp = "应用"
    const val sectionAbout = "关于"
    const val appearance = "外观"
    const val themeSystem = "系统"
    const val themeLight = "浅色"
    const val themeDark = "深色"
    const val defaultModel = "默认模型"
    const val language = "语言"
    const val fontSize = "字体大小"
    const val ttsVoice = "朗读音色"
    const val checkUpdate = "检查更新"
    const val sectionVoice = "语音"
    const val version = "版本"
    const val disclaimerLong =
        "输出内容由 AI 生成，医疗、法律、金融等专业领域的内容不构成任何诊疗、法律或投资建议，请注意甄别。"
    const val thirdPartyNote = "DS Chat · DeepSeek API 客户端"

    // 本项目增量：API Key
    const val apiKey = "API Key"
    const val apiKeyConfigured = "已配置"
    const val apiKeyMissing = "未配置"
    const val apiKeyTitle = "接入 DeepSeek API"
    const val apiKeyLocalOnly = "API Key 仅保存在本机，不会上传到任何服务器。"
    const val apiKeyHint = "sk-..."
    const val baseUrl = "Base URL"
    const val advanced = "高级选项"
    const val testConnection = "测试连接"
    const val testing = "正在测试..."
    const val testSuccess = "连接成功，API Key 可用"
    const val testFailed = "连接失败：%1\$s"
    const val save = "保存"
    const val getApiKey = "获取 API Key：platform.deepseek.com/api_keys"

    // 新 API 能力（全家桶）
    const val deepSearching = "正在深度搜索"
    const val reading = "正在阅读"
    const val searchedPages = "已阅读 %1\$d 个网页"
    const val references = "参考来源"
    const val uploading = "上传中..."
    const val uploadFailed = "上传失败：%1\$s"
    const val attachTitle = "添加附件"
    const val recording = "录音中，点击停止"
    const val transcribing = "正在识别语音..."
    const val readAloud = "朗读"
    const val stopReading = "停止朗读"
    const val voiceUnavailableOld = "语音输入需要接入第三方语音识别服务，当前版本未启用。"

    // 第三方 API 能力边界提示
    const val searchUnavailable =
        "DeepSeek 开放 API 暂不提供联网搜索。此开关仅保留界面位置；如需联网，可在设置中接入第三方搜索服务（待扩展）。"
    const val visionUnavailable = "DeepSeek 开放 API 暂未提供识图模型，识图模式暂不可用。"
    const val voiceUnavailable = "语音输入需要接入第三方语音识别服务，当前版本未启用。"
}
