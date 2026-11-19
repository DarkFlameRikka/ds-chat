package com.deepseekclone.app.ui.components

import androidx.compose.runtime.compositionLocalOf

/**
 * 全局「清除文本选中」信号：聊天页任意空白处轻点后自增，
 * 各个 SelectionContainer 监听它并把自己置空（Android 原生行为也是这样）。
 */
val LocalSelectionClearKey = compositionLocalOf { 0 }
